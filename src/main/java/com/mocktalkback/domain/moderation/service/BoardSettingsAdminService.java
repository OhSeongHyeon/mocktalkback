package com.mocktalkback.domain.moderation.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardFileEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.mapper.BoardMapper;
import com.mocktalkback.domain.board.repository.BoardFileRepository;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.entity.FileVariantEntity;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.file.repository.FileClassRepository;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.repository.FileVariantRepository;
import com.mocktalkback.domain.file.service.FileStorage;
import com.mocktalkback.domain.file.service.FileStorage.StoredFile;
import com.mocktalkback.domain.file.service.ImageOptimizationService;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.MediaKind;
import com.mocktalkback.domain.moderation.dto.BoardAdminSettingsUpdateRequest;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardSettingsAdminService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardFileRepository boardFileRepository;
    private final FileRepository fileRepository;
    private final FileClassRepository fileClassRepository;
    private final FileVariantRepository fileVariantRepository;
    private final FileStorage fileStorage;
    private final ImageOptimizationService imageOptimizationService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final BoardMapper boardMapper;
    private final FileMapper fileMapper;

    @Transactional(readOnly = true)
    public BoardResponse getSettings(Long boardId) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);
        return boardMapper.toResponse(board, resolveBoardImage(boardId));
    }

    @Transactional
    public BoardResponse updateSettings(Long boardId, BoardAdminSettingsUpdateRequest request) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);

        String description = request.description();
        if (description != null && description.isBlank()) {
            description = null;
        }

        board.update(request.boardName(), board.getSlug(), description, request.visibility());
        return boardMapper.toResponse(board, resolveBoardImage(boardId));
    }

    @Transactional
    public BoardResponse uploadBoardImage(Long boardId, MultipartFile boardImage, boolean preserveMetadata) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);

        StoredFile storedFile = fileStorage.store(FileClassCode.BOARD_IMAGE, boardImage, actor.getId());
        ImageOptimizationService.OriginalFileResult processed = imageOptimizationService
            .processOriginal(storedFile, preserveMetadata);
        FileClassEntity fileClass = getBoardImageClass();

        removeExistingBoardImages(boardId);

        FileEntity fileEntity = FileEntity.builder()
            .fileClass(fileClass)
            .fileName(storedFile.fileName())
            .storageKey(storedFile.storageKey())
            .fileSize(processed.fileSize())
            .mimeType(processed.mimeType())
            .metadataPreserved(processed.metadataPreserved())
            .build();

        FileEntity savedFile = fileRepository.save(fileEntity);
        imageOptimizationService.enqueueVariantGeneration(savedFile);
        BoardFileEntity mapping = BoardFileEntity.builder()
            .file(savedFile)
            .board(board)
            .build();
        boardFileRepository.save(mapping);

        return boardMapper.toResponse(board, fileMapper.toResponse(savedFile));
    }

    @Transactional
    public BoardResponse deleteBoardImage(Long boardId) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);

        removeExistingBoardImages(boardId);
        return boardMapper.toResponse(board, null);
    }

    private BoardEntity getBoard(Long boardId) {
        return boardRepository.findByIdAndDeletedAtIsNull(boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다."));
    }

    private UserEntity getCurrentUser() {
        Long userId = currentUserService.getUserId();
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private void requireBoardAdmin(UserEntity actor, BoardEntity board) {
        if (RoleNames.ADMIN.equals(actor.getRole().getRoleName())) {
            return;
        }
        BoardMemberEntity member = boardMemberRepository.findByUserIdAndBoardId(actor.getId(), board.getId())
            .orElse(null);
        if (member == null) {
            throw new AccessDeniedException("게시판 관리자 권한이 없습니다.");
        }
        BoardRole role = member.getBoardRole();
        if (role != BoardRole.OWNER && role != BoardRole.MODERATOR) {
            throw new AccessDeniedException("게시판 관리자 권한이 없습니다.");
        }
    }

    private FileResponse resolveBoardImage(Long boardId) {
        List<BoardFileEntity> files = boardFileRepository.findAllByBoardIdOrderByCreatedAtDesc(boardId);
        for (BoardFileEntity boardFile : files) {
            FileEntity file = boardFile.getFile();
            if (file.isDeleted()) {
                continue;
            }
            return fileMapper.toResponse(file);
        }
        return null;
    }

    private void removeExistingBoardImages(Long boardId) {
        List<BoardFileEntity> files = boardFileRepository.findAllByBoardIdOrderByCreatedAtDesc(boardId);
        for (BoardFileEntity boardFile : files) {
            FileEntity file = boardFile.getFile();
            boardFileRepository.delete(boardFile);
            softDeleteVariants(file.getId());
            file.softDelete();
        }
    }

    private void softDeleteVariants(Long fileId) {
        if (fileId == null) {
            return;
        }
        List<FileVariantEntity> variants = fileVariantRepository.findAllByFileIdAndDeletedAtIsNull(fileId);
        for (FileVariantEntity variant : variants) {
            variant.softDelete();
        }
    }

    private FileClassEntity getBoardImageClass() {
        return fileClassRepository.findByCode(FileClassCode.BOARD_IMAGE)
            .orElseGet(() -> fileClassRepository.save(
                FileClassEntity.builder()
                    .code(FileClassCode.BOARD_IMAGE)
                    .name("게시판 대표 이미지")
                    .description("게시판 대표 이미지")
                    .mediaKind(MediaKind.IMAGE)
                    .build()
            ));
    }
}
