package com.mocktalkback.domain.file.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.entity.ArticleFileEntity;
import com.mocktalkback.domain.article.repository.ArticleFileRepository;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardFileEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.repository.BoardFileRepository;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.common.policy.BoardAccessPolicy;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;

@Service
public class FileAccessDecisionService {

    private static final Set<String> ALWAYS_PUBLIC_FILE_CLASSES = Set.of(
        FileClassCode.PROFILE_IMAGE,
        FileClassCode.PROFILE_BANNER,
        FileClassCode.AVATAR_IMAGE,
        FileClassCode.SITE_BANNER,
        FileClassCode.SITE_POPUP,
        FileClassCode.GALLERY_ITEM_IMAGE,
        FileClassCode.GALLERY_ITEM_THUMBNAIL
    );

    private final ArticleFileRepository articleFileRepository;
    private final BoardFileRepository boardFileRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final BoardAccessPolicy boardAccessPolicy;
    private final String keyPrefix;

    public FileAccessDecisionService(
        ArticleFileRepository articleFileRepository,
        BoardFileRepository boardFileRepository,
        BoardMemberRepository boardMemberRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        BoardAccessPolicy boardAccessPolicy,
        @Value("${app.object-storage.key-prefix:uploads}") String keyPrefix
    ) {
        this.articleFileRepository = articleFileRepository;
        this.boardFileRepository = boardFileRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.boardAccessPolicy = boardAccessPolicy;
        this.keyPrefix = keyPrefix;
    }

    public FileAccessDecision decide(FileEntity file) {
        if (file == null || file.getId() == null || file.getFileClass() == null || file.isDeleted()) {
            return FileAccessDecision.deny();
        }

        FileDeliveryMode deliveryMode = resolveDeliveryMode(file);
        if (deliveryMode == null) {
            return FileAccessDecision.deny();
        }

        Optional<Long> optionalUserId = currentUserService.getOptionalUserId();
        UserEntity currentUser = optionalUserId.flatMap(userRepository::findByIdWithRoleAndDeletedAtIsNull).orElse(null);

        if (file.isTemporary()) {
            return decideTemporaryFile(file, optionalUserId.orElse(null));
        }

        String fileClassCode = file.getFileClass().getCode();
        if (isArticleProtectedClass(fileClassCode)) {
            return decideArticleFile(file, currentUser);
        }
        if (FileClassCode.BOARD_IMAGE.equals(fileClassCode)) {
            return decideBoardImage(file, currentUser);
        }
        if (deliveryMode == FileDeliveryMode.PUBLIC) {
            return FileAccessDecision.publicAccess();
        }
        return FileAccessDecision.deny();
    }

    public FileDeliveryMode resolveDeliveryMode(FileEntity file) {
        if (file == null || file.getId() == null || file.getFileClass() == null || file.isDeleted()) {
            return null;
        }

        if (file.isTemporary()) {
            return FileDeliveryMode.PROTECTED;
        }

        String fileClassCode = file.getFileClass().getCode();
        if (isArticleProtectedClass(fileClassCode)) {
            return hasAccessibleArticleBinding(file) ? FileDeliveryMode.PROTECTED : null;
        }
        if (FileClassCode.BOARD_IMAGE.equals(fileClassCode)) {
            return resolveBoardImageDeliveryMode(file);
        }
        if (ALWAYS_PUBLIC_FILE_CLASSES.contains(fileClassCode)) {
            return FileDeliveryMode.PUBLIC;
        }
        return null;
    }

    private FileAccessDecision decideTemporaryFile(FileEntity file, Long currentUserId) {
        if (currentUserId == null) {
            return FileAccessDecision.deny();
        }
        Optional<Long> ownerId = extractOwnerId(file.getStorageKey());
        if (ownerId.isPresent() && ownerId.get().equals(currentUserId)) {
            return FileAccessDecision.protectedAccess();
        }
        return FileAccessDecision.deny();
    }

    private FileAccessDecision decideArticleFile(FileEntity file, UserEntity currentUser) {
        List<ArticleFileEntity> mappings = articleFileRepository.findAllByFileId(file.getId());
        if (mappings.isEmpty()) {
            return FileAccessDecision.deny();
        }
        for (ArticleFileEntity mapping : mappings) {
            ArticleEntity article = mapping.getArticle();
            if (article == null || article.isDeleted()) {
                continue;
            }
            BoardEntity board = article.getBoard();
            if (board == null || board.isDeleted()) {
                continue;
            }
            BoardMemberEntity member = resolveBoardMember(currentUser, board.getId());
            if (!boardAccessPolicy.canAccessBoard(board, currentUser, member)) {
                continue;
            }
            EnumSet<ContentVisibility> allowed = boardAccessPolicy.resolveAllowedVisibilities(board, currentUser, member);
            if (allowed.contains(article.getVisibility())) {
                return FileAccessDecision.protectedAccess();
            }
        }
        return FileAccessDecision.deny();
    }

    private boolean hasAccessibleArticleBinding(FileEntity file) {
        List<ArticleFileEntity> mappings = articleFileRepository.findAllByFileId(file.getId());
        if (mappings.isEmpty()) {
            return false;
        }
        for (ArticleFileEntity mapping : mappings) {
            ArticleEntity article = mapping.getArticle();
            if (article == null || article.isDeleted()) {
                continue;
            }
            BoardEntity board = article.getBoard();
            if (board == null || board.isDeleted()) {
                continue;
            }
            return true;
        }
        return false;
    }

    private FileAccessDecision decideBoardImage(FileEntity file, UserEntity currentUser) {
        List<BoardFileEntity> mappings = boardFileRepository.findAllByFileId(file.getId());
        if (mappings.isEmpty()) {
            return FileAccessDecision.deny();
        }
        for (BoardFileEntity mapping : mappings) {
            BoardEntity board = mapping.getBoard();
            if (board == null || board.isDeleted()) {
                continue;
            }
            BoardMemberEntity member = resolveBoardMember(currentUser, board.getId());
            if (!boardAccessPolicy.canAccessBoard(board, currentUser, member)) {
                continue;
            }
            if (board.getVisibility() == BoardVisibility.PUBLIC) {
                return FileAccessDecision.publicAccess();
            }
            return FileAccessDecision.protectedAccess();
        }
        return FileAccessDecision.deny();
    }

    private FileDeliveryMode resolveBoardImageDeliveryMode(FileEntity file) {
        List<BoardFileEntity> mappings = boardFileRepository.findAllByFileId(file.getId());
        if (mappings.isEmpty()) {
            return null;
        }
        for (BoardFileEntity mapping : mappings) {
            BoardEntity board = mapping.getBoard();
            if (board == null || board.isDeleted()) {
                continue;
            }
            if (board.getVisibility() == BoardVisibility.PUBLIC) {
                return FileDeliveryMode.PUBLIC;
            }
            return FileDeliveryMode.PROTECTED;
        }
        return null;
    }

    private BoardMemberEntity resolveBoardMember(UserEntity currentUser, Long boardId) {
        if (currentUser == null || currentUser.getId() == null || boardId == null) {
            return null;
        }
        return boardMemberRepository.findByUserIdAndBoardId(currentUser.getId(), boardId).orElse(null);
    }

    private boolean isArticleProtectedClass(String fileClassCode) {
        return FileClassCode.ARTICLE_CONTENT_IMAGE.equals(fileClassCode)
            || FileClassCode.ARTICLE_CONTENT_VIDEO.equals(fileClassCode)
            || FileClassCode.ARTICLE_ATTACHMENT.equals(fileClassCode)
            || FileClassCode.ARTICLE_THUMBNAIL.equals(fileClassCode);
    }

    private Optional<Long> extractOwnerId(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return Optional.empty();
        }
        String normalizedKey = storageKey.trim().replace('\\', '/').replaceAll("^/+", "");
        String normalizedPrefix = normalizePrefix(keyPrefix);
        String prefixWithSlash = normalizedPrefix + "/";
        if (!normalizedKey.startsWith(prefixWithSlash)) {
            return Optional.empty();
        }
        String remainder = normalizedKey.substring(prefixWithSlash.length());
        String[] parts = remainder.split("/");
        if (parts.length < 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(parts[1]));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
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
}
