package com.mocktalkback.domain.moderation.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardFileEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.mapper.BoardMapper;
import com.mocktalkback.domain.board.repository.BoardFileRepository;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;
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
import com.mocktalkback.domain.moderation.dto.AdminBoardCreateRequest;
import com.mocktalkback.domain.moderation.dto.AdminBoardUpdateRequest;
import com.mocktalkback.domain.moderation.type.AdminBoardSortBy;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminBoardService {

    private static final int MAX_PAGE_SIZE = 50;
    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardFileRepository boardFileRepository;
    private final FileRepository fileRepository;
    private final FileClassRepository fileClassRepository;
    private final FileStorage fileStorage;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final BoardMapper boardMapper;
    private final FileMapper fileMapper;

    @Transactional(readOnly = true)
    public PageResponse<BoardResponse> findBoards(
        String keyword,
        BoardVisibility visibility,
        boolean includeDeleted,
        AdminBoardSortBy sortBy,
        boolean sortAsc,
        int page,
        int size
    ) {
        Pageable pageable = toPageable(page, size, sortAsc, sortBy);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        Page<BoardEntity> result = boardRepository.findAdminBoards(normalizedKeyword, visibility, includeDeleted, pageable);
        Map<Long, FileResponse> boardImages = resolveBoardImages(result.getContent());
        Page<BoardResponse> mapped = result.map(
            board -> boardMapper.toResponse(board, boardImages.get(board.getId()))
        );
        return PageResponse.from(mapped);
    }

    @Transactional
    public BoardResponse create(AdminBoardCreateRequest request) {
        UserEntity actor = getCurrentUser();
        String description = normalizeDescription(request.description());
        BoardEntity entity = BoardEntity.builder()
            .boardName(request.boardName())
            .slug(request.slug())
            .description(description)
            .visibility(request.visibility())
            .build();
        BoardEntity saved = boardRepository.save(entity);

        BoardMemberEntity owner = BoardMemberEntity.builder()
            .user(actor)
            .board(saved)
            .grantedByUser(actor)
            .boardRole(BoardRole.OWNER)
            .build();
        boardMemberRepository.save(owner);

        return boardMapper.toResponse(saved, null);
    }

    @Transactional
    public BoardResponse update(Long boardId, AdminBoardUpdateRequest request) {
        BoardEntity board = getBoard(boardId);
        String description = normalizeDescription(request.description());
        board.update(request.boardName(), request.slug(), description, request.visibility());
        return boardMapper.toResponse(board, resolveBoardImage(boardId));
    }

    @Transactional
    public void delete(Long boardId) {
        BoardEntity board = getBoard(boardId);
        board.softDelete();
    }

    @Transactional
    public BoardResponse uploadBoardImage(Long boardId, MultipartFile boardImage) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();

        StoredFile storedFile = fileStorage.store(FileClassCode.BOARD_IMAGE, boardImage, actor.getId());
        FileClassEntity fileClass = getBoardImageClass();

        removeExistingBoardImages(boardId);

        FileEntity fileEntity = FileEntity.builder()
            .fileClass(fileClass)
            .fileName(storedFile.fileName())
            .storageKey(storedFile.storageKey())
            .fileSize(storedFile.fileSize())
            .mimeType(storedFile.mimeType())
            .build();

        FileEntity savedFile = fileRepository.save(fileEntity);
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
        removeExistingBoardImages(boardId);
        return boardMapper.toResponse(board, null);
    }

    private BoardEntity getBoard(Long boardId) {
        return boardRepository.findById(boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다."));
    }

    private UserEntity getCurrentUser() {
        Long userId = currentUserService.getUserId();
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        return description.isBlank() ? null : description;
    }

    private Map<Long, FileResponse> resolveBoardImages(List<BoardEntity> boards) {
        if (boards.isEmpty()) {
            return Map.of();
        }
        List<Long> boardIds = boards.stream()
            .map(BoardEntity::getId)
            .toList();
        List<BoardFileEntity> files = boardFileRepository.findAllByBoardIdInOrderByCreatedAtDesc(boardIds);
        Map<Long, FileResponse> result = new HashMap<>();
        for (BoardFileEntity boardFile : files) {
            Long boardId = boardFile.getBoard().getId();
            if (result.containsKey(boardId)) {
                continue;
            }
            FileEntity file = boardFile.getFile();
            if (file.isDeleted()) {
                continue;
            }
            result.put(boardId, fileMapper.toResponse(file));
        }
        return result;
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
            file.softDelete();
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

    private Pageable toPageable(int page, int size, boolean sortAsc, AdminBoardSortBy sortBy) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        String sortField = sortBy == AdminBoardSortBy.UPDATED_AT ? "updatedAt" : "createdAt";
        Sort sort = sortAsc
            ? Sort.by(Sort.Order.asc(sortField), Sort.Order.asc("id"))
            : Sort.by(Sort.Order.desc(sortField), Sort.Order.desc("id"));
        return PageRequest.of(page, size, sort);
    }
}
