package com.mocktalkback.infra.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.file.service.FileStorage;
import com.mocktalkback.domain.file.service.FileStoragePathResolver;
import com.mocktalkback.domain.file.type.FileClassCode;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;

@Service
@Profile({"dev", "prod"})
public class ObjectStorageFileStorageService implements FileStorage {

    private static final int DEFAULT_PRESIGN_EXPIRE_SECONDS = 300;
    private static final int MAX_PRESIGN_EXPIRE_SECONDS = 604800;

    private final ObjectStorageProperties properties;
    private final FileStoragePathResolver pathResolver;
    private final MinioClient objectClient;
    private final MinioClient presignClient;

    public ObjectStorageFileStorageService(
        ObjectStorageProperties properties,
        FileStoragePathResolver pathResolver
    ) {
        this.properties = properties;
        this.pathResolver = pathResolver;
        validateProperties(properties);
        this.objectClient = createClient(properties.getEndpoint());
        this.presignClient = createClient(resolvePresignEndpoint(properties));
    }

    @Override
    public StoredFile store(String fileClassCode, MultipartFile file, Long ownerId) {
        validateFile(fileClassCode, file);
        if (ownerId == null) {
            throw new IllegalArgumentException("파일 소유자 식별자가 비어있습니다.");
        }
        String originalName = cleanFileName(file);
        String savedName = UUID.randomUUID().toString().replace("-", "") + "_" + originalName;
        String category = pathResolver.resolveCategory(fileClassCode);
        String storageKey = buildStorageKey(category, ownerId, savedName);
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(storageKey)
                .stream(inputStream, file.getSize(), -1);
            if (StringUtils.hasText(file.getContentType())) {
                builder.contentType(file.getContentType());
            }
            objectClient.putObject(builder.build());
        } catch (Exception ex) {
            throw new IllegalStateException("파일 저장에 실패했습니다.");
        }
        return new StoredFile(
            savedName,
            storageKey,
            file.getSize(),
            file.getContentType()
        );
    }

    @Override
    public byte[] read(String storageKey) {
        String normalizedKey = normalizeKey(storageKey);
        try (InputStream inputStream = objectClient.getObject(
            GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(normalizedKey)
                .build()
        )) {
            return inputStream.readAllBytes();
        } catch (Exception ex) {
            throw new IllegalStateException("파일 조회에 실패했습니다.");
        }
    }

    @Override
    public void write(String storageKey, byte[] bytes, String mimeType) {
        if (bytes == null) {
            throw new IllegalArgumentException("저장할 파일 바이트가 비어있습니다.");
        }
        String normalizedKey = normalizeKey(storageKey);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(normalizedKey)
                .stream(inputStream, bytes.length, -1);
            if (StringUtils.hasText(mimeType)) {
                builder.contentType(mimeType);
            }
            objectClient.putObject(builder.build());
        } catch (Exception ex) {
            throw new IllegalStateException("파일 저장에 실패했습니다.");
        }
    }

    @Override
    public void delete(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return;
        }
        String normalizedKey = normalizeKey(storageKey);
        try {
            objectClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(normalizedKey)
                    .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("파일 삭제에 실패했습니다.");
        }
    }

    @Override
    public String resolveViewUrl(String storageKey) {
        String normalizedKey = normalizeKey(storageKey);
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            String base = properties.getPublicBaseUrl().replaceAll("/+$", "");
            return base + "/" + normalizedKey;
        }
        int expireSeconds = normalizePresignExpireSeconds(properties.getPresignExpireSeconds());
        try {
            return presignClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucket())
                    .object(normalizedKey)
                    .expiry(expireSeconds)
                    .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("파일 조회 URL 생성에 실패했습니다.");
        }
    }

    private int normalizePresignExpireSeconds(long expireSeconds) {
        if (expireSeconds <= 0L) {
            return DEFAULT_PRESIGN_EXPIRE_SECONDS;
        }
        if (expireSeconds > MAX_PRESIGN_EXPIRE_SECONDS) {
            return MAX_PRESIGN_EXPIRE_SECONDS;
        }
        return (int) expireSeconds;
    }

    private MinioClient createClient(String endpoint) {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .region(properties.getRegion())
            .build();
    }

    private String resolvePresignEndpoint(ObjectStorageProperties props) {
        if (StringUtils.hasText(props.getPresignEndpoint())) {
            return props.getPresignEndpoint();
        }
        return props.getEndpoint();
    }

    private void validateProperties(ObjectStorageProperties props) {
        if (!StringUtils.hasText(props.getEndpoint())) {
            throw new IllegalStateException("오브젝트 스토리지 endpoint 설정이 비어있습니다.");
        }
        if (!StringUtils.hasText(props.getRegion())) {
            throw new IllegalStateException("오브젝트 스토리지 region 설정이 비어있습니다.");
        }
        if (!StringUtils.hasText(props.getBucket())) {
            throw new IllegalStateException("오브젝트 스토리지 bucket 설정이 비어있습니다.");
        }
        if (!StringUtils.hasText(props.getAccessKey())) {
            throw new IllegalStateException("오브젝트 스토리지 access-key 설정이 비어있습니다.");
        }
        if (!StringUtils.hasText(props.getSecretKey())) {
            throw new IllegalStateException("오브젝트 스토리지 secret-key 설정이 비어있습니다.");
        }
    }

    private void validateFile(String fileClassCode, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어있습니다.");
        }
        if (FileClassCode.PROFILE_IMAGE.equals(fileClassCode)
            || FileClassCode.BOARD_IMAGE.equals(fileClassCode)
            || FileClassCode.ARTICLE_CONTENT_IMAGE.equals(fileClassCode)) {
            validateImage(file);
            return;
        }
        if (FileClassCode.ARTICLE_CONTENT_VIDEO.equals(fileClassCode)) {
            validateVideo(file);
        }
    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private void validateVideo(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("영상 파일만 업로드할 수 있습니다.");
        }
        if (!"video/mp4".equals(contentType) && !"video/webm".equals(contentType)) {
            throw new IllegalArgumentException("MP4 또는 WebM 영상만 업로드할 수 있습니다.");
        }
    }

    private String cleanFileName(MultipartFile file) {
        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        String cleaned = StringUtils.cleanPath(original).replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!StringUtils.hasText(cleaned)) {
            return "file";
        }
        return cleaned;
    }

    private String buildStorageKey(String category, Long ownerId, String savedName) {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        String day = String.format("%02d", today.getDayOfMonth());
        String prefix = normalizePrefix(properties.getKeyPrefix());
        return prefix + "/" + category + "/" + ownerId + "/" + year + "/" + month + "/" + day + "/" + savedName;
    }

    private String normalizePrefix(String rawPrefix) {
        if (!StringUtils.hasText(rawPrefix)) {
            return "uploads";
        }
        String normalized = rawPrefix.trim().replace('\\', '/');
        normalized = normalized.replaceAll("^/+", "");
        normalized = normalized.replaceAll("/+$", "");
        if (!StringUtils.hasText(normalized)) {
            return "uploads";
        }
        return normalized;
    }

    private String normalizeKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new IllegalArgumentException("저장소 키가 비어있습니다.");
        }
        String normalized = storageKey.trim().replace('\\', '/');
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            URI uri = URI.create(normalized);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                throw new IllegalArgumentException("저장소 키 경로가 비어있습니다.");
            }
            normalized = path;
        }
        normalized = normalized.replaceAll("^/+", "");
        String bucketPrefix = properties.getBucket() + "/";
        if (normalized.startsWith(bucketPrefix)) {
            normalized = normalized.substring(bucketPrefix.length());
        }
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("저장소 키 경로가 비어있습니다.");
        }
        return normalized;
    }
}
