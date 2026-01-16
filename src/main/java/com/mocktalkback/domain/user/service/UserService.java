package com.mocktalkback.domain.user.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.article.dto.ArticleResponse;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.mapper.ArticleMapper;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.comment.dto.CommentResponse;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.mapper.CommentMapper;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.file.repository.FileClassRepository;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.service.FileStorage;
import com.mocktalkback.domain.file.service.FileStorage.StoredFile;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.MediaKind;
import com.mocktalkback.domain.user.dto.UserProfileResponse;
import com.mocktalkback.domain.user.dto.UserProfileUpdateRequest;
import com.mocktalkback.domain.user.dto.UserDeleteRequest;
import com.mocktalkback.domain.user.dto.UserMentionResponse;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.entity.UserFileEntity;
import com.mocktalkback.domain.user.repository.UserFileRepository;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_MENTION_SIZE = 10;
    private static final Sort MY_CONTENT_SORT = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("updatedAt"),
        Sort.Order.desc("id")
    );

    private final UserRepository userRepository;
    private final UserFileRepository userFileRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;
    private final FileClassRepository fileClassRepository;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final FileMapper fileMapper;
    private final FileStorage fileStorage;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);
        FileResponse profileImage = findProfileImage(userId);
        return new UserProfileResponse(
            user.getId(),
            user.getLoginId(),
            user.getEmail(),
            user.getUserName(),
            user.getDisplayName(),
            user.getHandle(),
            user.getUserPoint(),
            profileImage
        );
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UserProfileUpdateRequest request) {
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);

        String userName = normalizeRequired(request.getUserName(), "이름");
        String email = normalizeRequired(request.getEmail(), "이메일");
        String displayName = normalizeOptional(request.getDisplayName(), userName);
        String handle = normalizeRequired(request.getHandle(), "핸들");

        requireMaxLength(userName, 32, "이름");
        requireMaxLength(email, 128, "이메일");
        requireMaxLength(displayName, 16, "닉네임");
        requireMaxLength(handle, 24, "핸들");

        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (!handle.equals(user.getHandle()) && userRepository.existsByHandle(handle)) {
            throw new IllegalArgumentException("이미 사용 중인 핸들입니다.");
        }

        user.updateProfile(userName, displayName, handle);
        user.updateEmail(email);

        String password = request.getPassword();
        if (StringUtils.hasText(password)) {
            updatePassword(user, password);
        }

        MultipartFile profileImage = request.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            updateProfileImage(user, profileImage);
        }

        return getMyProfile();
    }

    @Transactional
    public void deleteMyAccount(UserDeleteRequest request) {
        String confirmText = normalizeRequired(request.confirmText(), "재확인 문구");
        if (!"탈퇴".equals(confirmText)) {
            throw new IllegalArgumentException("재확인 문구가 올바르지 않습니다.");
        }
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);
        user.softDeleteAccount();
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getMyArticles(int page, int size) {
        Pageable pageable = createPageable(page, size);
        Long userId = currentUserService.getUserId();
        Page<ArticleEntity> result = articleRepository.findByUserId(userId, pageable);
        Page<ArticleResponse> mapped = result.map(articleMapper::toResponse);
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getMyComments(int page, int size) {
        Pageable pageable = createPageable(page, size);
        Long userId = currentUserService.getUserId();
        Page<CommentEntity> result = commentRepository.findByUserId(userId, pageable);
        Page<CommentResponse> mapped = result.map(commentMapper::toResponse);
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public List<UserMentionResponse> getMentionSuggestions(String keyword, int size) {
        String normalized = normalizeKeyword(keyword);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        int resolvedSize = normalizeMentionSize(size);
        Pageable pageable = PageRequest.of(0, resolvedSize, Sort.by(Sort.Order.asc("handle")));
        Page<UserEntity> result = userRepository.findByHandleContainingIgnoreCaseAndDeletedAtIsNull(
            normalized,
            pageable
        );
        return result.stream()
            .map(user -> new UserMentionResponse(
                user.getId(),
                user.getHandle(),
                user.getDisplayName(),
                findProfileImage(user.getId())
            ))
            .toList();
    }

    private void updateProfileImage(UserEntity user, MultipartFile profileImage) {
        StoredFile storedFile = fileStorage.store(
            FileClassCode.PROFILE_IMAGE,
            profileImage,
            user.getId()
        );
        FileClassEntity fileClass = getProfileImageClass();

        removeExistingProfileImages(user.getId());

        FileEntity fileEntity = FileEntity.builder()
            .fileClass(fileClass)
            .fileName(storedFile.fileName())
            .storageKey(storedFile.storageKey())
            .fileSize(storedFile.fileSize())
            .mimeType(storedFile.mimeType())
            .build();

        FileEntity savedFile = fileRepository.save(fileEntity);
        UserFileEntity link = UserFileEntity.builder()
            .user(user)
            .file(savedFile)
            .build();
        userFileRepository.save(link);
    }

    private FileResponse findProfileImage(Long userId) {
        PageRequest pageRequest = PageRequest.of(0, 1);
        List<UserFileEntity> files = userFileRepository.findLatestByUserIdAndFileClassCode(
            userId,
            FileClassCode.PROFILE_IMAGE,
            pageRequest
        );
        if (files.isEmpty()) {
            return null;
        }
        return fileMapper.toResponse(files.get(0).getFile());
    }

    private void removeExistingProfileImages(Long userId) {
        List<UserFileEntity> existing = userFileRepository.findAllByUserIdAndFileClassCode(
            userId,
            FileClassCode.PROFILE_IMAGE
        );
        for (UserFileEntity userFile : existing) {
            FileEntity file = userFile.getFile();
            userFileRepository.delete(userFile);
            file.softDelete();
        }
    }

    private FileClassEntity getProfileImageClass() {
        return fileClassRepository.findByCode(FileClassCode.PROFILE_IMAGE)
            .orElseGet(() -> fileClassRepository.save(
                FileClassEntity.builder()
                    .code(FileClassCode.PROFILE_IMAGE)
                    .name("프로필 이미지")
                    .description("사용자 프로필 이미지")
                    .mediaKind(MediaKind.IMAGE)
                    .build()
            ));
    }

    private Pageable createPageable(int page, int size) {
        int resolvedPage = normalizePage(page);
        int resolvedSize = normalizeSize(size);
        return PageRequest.of(resolvedPage, resolvedSize, MY_CONTENT_SORT);
    }

    private int normalizePage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        return page;
    }

    private int normalizeSize(int size) {
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        return size;
    }

    private int normalizeMentionSize(int size) {
        if (size <= 0) {
            return MAX_MENTION_SIZE;
        }
        return Math.min(size, MAX_MENTION_SIZE);
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.trim();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + "을(를) 입력해주세요.");
        }
        return value.trim();
    }

    private String normalizeOptional(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private void requireMaxLength(String value, int max, String fieldName) {
        if (value.length() > max) {
            throw new IllegalArgumentException(fieldName + "은 " + max + "자 이하이어야 합니다.");
        }
    }

    private void updatePassword(UserEntity user, String rawPassword) {
        if (rawPassword.length() < 8 || rawPassword.length() > 64) {
            throw new IllegalArgumentException("비밀번호는 8~64자 사이여야 합니다.");
        }
        user.changePassword(passwordEncoder.encode(rawPassword));
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }
}
