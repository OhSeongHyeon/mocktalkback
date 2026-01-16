package com.mocktalkback.domain.board.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.board.dto.BoardCreateRequest;
import com.mocktalkback.domain.board.dto.BoardDetailResponse;
import com.mocktalkback.domain.board.dto.BoardMemberStatusResponse;
import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.dto.BoardSubscribeStatusResponse;
import com.mocktalkback.domain.board.dto.BoardUpdateRequest;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardFileEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.entity.BoardSubscribeEntity;
import com.mocktalkback.domain.board.mapper.BoardMapper;
import com.mocktalkback.domain.board.repository.BoardFileRepository;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.repository.BoardSubscribeRepository;
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
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardService {

    private static final int BOARD_CREATE_POINT = 1000;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort BOARD_SORT = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("updatedAt"),
        Sort.Order.desc("id")
    );

    private final BoardRepository boardRepository;
    private final BoardFileRepository boardFileRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardSubscribeRepository boardSubscribeRepository;
    private final FileRepository fileRepository;
    private final FileClassRepository fileClassRepository;
    private final FileStorage fileStorage;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final BoardMapper boardMapper;
    private final FileMapper fileMapper;

    @Transactional
    public BoardResponse create(BoardCreateRequest request) {
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);
        if (user.getUserPoint() < BOARD_CREATE_POINT) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        BoardEntity entity = boardMapper.toEntity(request);
        BoardEntity saved = boardRepository.save(entity);
        user.changePoint(-BOARD_CREATE_POINT);

        BoardMemberEntity member = BoardMemberEntity.builder()
            .user(user)
            .board(saved)
            .grantedByUser(user)
            .boardRole(BoardRole.OWNER)
            .build();
        boardMemberRepository.save(member);
        return boardMapper.toResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public BoardDetailResponse findById(Long id) {
        BoardEntity entity = getBoard(id);
        Long userId = currentUserService.getOptionalUserId().orElse(null);
        if (userId == null) {
            if (entity.getVisibility() != BoardVisibility.PUBLIC) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
            }
            return toDetailResponse(entity, null);
        }

        UserEntity user = getUser(userId);
        if (!canReadBoard(entity, user, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
        }
        return toDetailResponse(entity, userId);
    }

    @Transactional(readOnly = true)
    public BoardDetailResponse findBySlug(String slug) {
        BoardEntity entity = getBoardBySlug(slug);
        Long userId = currentUserService.getOptionalUserId().orElse(null);
        if (userId == null) {
            if (entity.getVisibility() != BoardVisibility.PUBLIC) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
            }
            return toDetailResponse(entity, null);
        }

        UserEntity user = getUser(userId);
        if (!canReadBoard(entity, user, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
        }
        return toDetailResponse(entity, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<BoardResponse> findAll(int page, int size) {
        int resolvedPage = normalizePage(page);
        int resolvedSize = normalizeSize(size);
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, BOARD_SORT);

        Long userId = currentUserService.getOptionalUserId().orElse(null);
        if (userId == null) {
            Page<BoardEntity> result = boardRepository.findAllByVisibilityInAndDeletedAtIsNull(
                List.of(BoardVisibility.PUBLIC),
                pageable
            );
            return toPageResponse(result);
        }

        UserEntity user = getUser(userId);
        if (isManagerOrAdmin(user)) {
            Page<BoardEntity> result = boardRepository.findAllByDeletedAtIsNull(pageable);
            return toPageResponse(result);
        }

        List<BoardEntity> boards = boardRepository.findAllByDeletedAtIsNull(BOARD_SORT);
        Map<Long, BoardRole> membership = boardMemberRepository.findAllByUserId(userId).stream()
            .collect(Collectors.toMap(
                member -> member.getBoard().getId(),
                BoardMemberEntity::getBoardRole,
                (left, right) -> left
            ));

        List<BoardEntity> accessibleBoards = boards.stream()
            .filter(board -> canReadBoard(board, membership))
            .toList();

        return toPageResponse(accessibleBoards, resolvedPage, resolvedSize);
    }

    @Transactional
    public BoardResponse update(Long id, BoardUpdateRequest request) {
        BoardEntity entity = getBoard(id);
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);
        requireManagePermission(entity, user, userId);
        entity.update(request.boardName(), request.slug(), request.description(), request.visibility());
        return boardMapper.toResponse(entity, resolveBoardImage(entity.getId()));
    }

    @Transactional
    public void delete(Long id) {
        BoardEntity entity = getBoard(id);
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);
        requireManagePermission(entity, user, userId);
        entity.softDelete();
    }

    @Transactional
    public BoardResponse uploadBoardImage(Long boardId, MultipartFile boardImage) {
        BoardEntity board = getBoard(boardId);
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);
        requireManagePermission(board, user, userId);

        StoredFile storedFile = fileStorage.store(FileClassCode.BOARD_IMAGE, boardImage, userId);
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
    public BoardSubscribeStatusResponse subscribe(Long boardId) {
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);
        BoardEntity board = getBoard(boardId);
        if (!canReadBoard(board, user, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
        }
        BoardMemberEntity member = boardMemberRepository.findByUserIdAndBoardId(userId, boardId).orElse(null);
        if (member != null && member.getBoardRole() == BoardRole.BANNED) {
            throw new AccessDeniedException("구독 권한이 없습니다.");
        }
        if (boardSubscribeRepository.existsByUserIdAndBoardId(userId, boardId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 구독 중입니다.");
        }
        BoardSubscribeEntity entity = BoardSubscribeEntity.builder()
            .user(user)
            .board(board)
            .build();
        boardSubscribeRepository.save(entity);
        return new BoardSubscribeStatusResponse(boardId, true);
    }

    @Transactional
    public BoardSubscribeStatusResponse unsubscribe(Long boardId) {
        Long userId = currentUserService.getUserId();
        // BoardEntity board = getBoard(boardId);
        if (!boardSubscribeRepository.existsByUserIdAndBoardId(userId, boardId)) {
            return new BoardSubscribeStatusResponse(boardId, false);
        }
        boardSubscribeRepository.deleteByUserIdAndBoardId(userId, boardId);
        return new BoardSubscribeStatusResponse(boardId, false);
    }

    @Transactional
    public BoardMemberStatusResponse requestJoin(Long boardId) {
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);
        BoardEntity board = getBoard(boardId);

        if (!canReadBoard(board, user, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
        }

        if (board.getVisibility() == BoardVisibility.PRIVATE || board.getVisibility() == BoardVisibility.UNLISTED) {
            throw new AccessDeniedException("가입 요청이 허용되지 않습니다.");
        }

        BoardMemberEntity existing = boardMemberRepository.findByUserIdAndBoardId(userId, boardId).orElse(null);
        if (existing != null) {
            if (existing.getBoardRole() == BoardRole.BANNED) {
                throw new AccessDeniedException("가입 요청이 제한된 사용자입니다.");
            }
            if (existing.getBoardRole() == BoardRole.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입 요청이 존재합니다.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 사용자입니다.");
        }

        BoardMemberEntity member = BoardMemberEntity.builder()
            .user(user)
            .board(board)
            .grantedByUser(null)
            .boardRole(BoardRole.PENDING)
            .build();
        boardMemberRepository.save(member);

        return new BoardMemberStatusResponse(boardId, BoardRole.PENDING);
    }

    @Transactional
    public BoardMemberStatusResponse approveJoin(Long boardId, Long targetUserId) {
        Long approverId = currentUserService.getUserId();
        UserEntity approver = getUser(approverId);
        BoardEntity board = getBoard(boardId);
        requireApprovePermission(board, approver, approverId);

        BoardMemberEntity member = boardMemberRepository.findByUserIdAndBoardId(targetUserId, boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found"));
        if (member.getBoardRole() != BoardRole.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "승인 대기 상태가 아닙니다.");
        }
        member.approve(approver);
        return new BoardMemberStatusResponse(boardId, BoardRole.MEMBER);
    }

    @Transactional
    public void cancelOrRejectMember(Long boardId, Long targetUserId) {
        Long userId = currentUserService.getUserId();
        UserEntity user = getUser(userId);
        BoardEntity board = getBoard(boardId);

        if (!userId.equals(targetUserId)) {
            requireApprovePermission(board, user, userId);
        }

        BoardMemberEntity member = boardMemberRepository.findByUserIdAndBoardId(targetUserId, boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found"));
        boardMemberRepository.delete(member);
    }

    @Transactional
    public void cancelOwnMember(Long boardId) {
        Long userId = currentUserService.getUserId();
        cancelOrRejectMember(boardId, userId);
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }

    private BoardEntity getBoard(Long id) {
        return boardRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found"));
    }

    private BoardEntity getBoardBySlug(String slug) {
        return boardRepository.findBySlugAndDeletedAtIsNull(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found"));
    }

    private BoardDetailResponse toDetailResponse(BoardEntity board, Long userId) {
        FileResponse boardImage = resolveBoardImage(board.getId());
        BoardMemberEntity ownerMember = boardMemberRepository.findFirstByBoardIdAndBoardRole(board.getId(), BoardRole.OWNER)
            .orElse(null);
        String ownerDisplayName = formatOwnerDisplay(ownerMember == null ? null : ownerMember.getUser());

        BoardRole memberStatus = null;
        boolean subscribed = false;
        if (userId != null) {
            BoardMemberEntity member = boardMemberRepository.findByUserIdAndBoardId(userId, board.getId())
                .orElse(null);
            if (member != null) {
                memberStatus = member.getBoardRole();
            }
            subscribed = boardSubscribeRepository.existsByUserIdAndBoardId(userId, board.getId());
        }

        return boardMapper.toDetailResponse(board, boardImage, ownerDisplayName, memberStatus, subscribed);
    }

    private String formatOwnerDisplay(UserEntity user) {
        if (user == null) {
            return null;
        }
        String displayName = user.getDisplayName() == null ? "" : user.getDisplayName().trim();
        String handle = user.getHandle() == null ? "" : user.getHandle().trim();
        if (displayName.isEmpty() && handle.isEmpty()) {
            return null;
        }
        if (handle.isEmpty()) {
            return displayName;
        }
        if (displayName.isEmpty()) {
            return "@" + handle;
        }
        return displayName + "@" + handle;
    }

    private boolean canReadBoard(BoardEntity board, UserEntity user, Long userId) {
        if (isManagerOrAdmin(user)) {
            return true;
        }
        BoardVisibility visibility = board.getVisibility();
        if (visibility == BoardVisibility.PUBLIC) {
            return true;
        }
        BoardMemberEntity member = boardMemberRepository.findByUserIdAndBoardId(userId, board.getId())
            .orElse(null);
        if (member != null && member.getBoardRole() == BoardRole.BANNED) {
            return false;
        }
        if (visibility == BoardVisibility.GROUP) {
            return true;
        }
        if (visibility == BoardVisibility.PRIVATE) {
            return member != null && member.getBoardRole() == BoardRole.OWNER;
        }
        return false;
    }

    private boolean canReadBoard(BoardEntity board, Map<Long, BoardRole> membership) {
        BoardVisibility visibility = board.getVisibility();
        if (visibility == BoardVisibility.PUBLIC) {
            return true;
        }
        BoardRole role = membership.get(board.getId());
        if (role == BoardRole.BANNED) {
            return false;
        }
        if (visibility == BoardVisibility.GROUP) {
            return true;
        }
        if (visibility == BoardVisibility.PRIVATE) {
            return role == BoardRole.OWNER;
        }
        return false;
    }

    private PageResponse<BoardResponse> toPageResponse(Page<BoardEntity> page) {
        List<BoardResponse> items = mapBoardResponses(page.getContent());
        return new PageResponse<>(
            items,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    private PageResponse<BoardResponse> toPageResponse(
        List<BoardEntity> boards,
        int page,
        int size
    ) {
        int totalElements = boards.size();
        if (totalElements == 0) {
            return new PageResponse<>(List.of(), page, size, 0, 0, false, false);
        }

        int fromIndex = page * size;
        if (fromIndex >= totalElements) {
            return new PageResponse<>(List.of(), page, size, totalElements, calculateTotalPages(totalElements, size), false, page > 0);
        }

        int toIndex = Math.min(fromIndex + size, totalElements);
        List<BoardEntity> pageItems = boards.subList(fromIndex, toIndex);
        List<BoardResponse> items = mapBoardResponses(pageItems);
        int totalPages = calculateTotalPages(totalElements, size);
        boolean hasNext = page + 1 < totalPages;

        return new PageResponse<>(
            items,
            page,
            size,
            totalElements,
            totalPages,
            hasNext,
            page > 0
        );
    }

    private int calculateTotalPages(int totalElements, int size) {
        return (int) Math.ceil((double) totalElements / size);
    }

    private List<BoardResponse> mapBoardResponses(List<BoardEntity> boards) {
        Map<Long, FileResponse> boardImages = resolveBoardImages(boards);
        return boards.stream()
            .map(board -> boardMapper.toResponse(board, boardImages.get(board.getId())))
            .toList();
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

    private void requireManagePermission(BoardEntity board, UserEntity user, Long userId) {
        if (isManagerOrAdmin(user)) {
            return;
        }
        BoardMemberEntity member = boardMemberRepository.findByUserIdAndBoardId(userId, board.getId())
            .orElse(null);
        if (member == null || member.getBoardRole() != BoardRole.OWNER) {
            throw new AccessDeniedException("게시판 관리 권한이 없습니다.");
        }
    }

    private void requireApprovePermission(BoardEntity board, UserEntity user, Long userId) {
        if (isManagerOrAdmin(user)) {
            return;
        }
        BoardMemberEntity member = boardMemberRepository.findByUserIdAndBoardId(userId, board.getId())
            .orElse(null);
        if (member == null) {
            throw new AccessDeniedException("가입 승인 권한이 없습니다.");
        }
        BoardRole role = member.getBoardRole();
        if (role != BoardRole.OWNER && role != BoardRole.MODERATOR) {
            throw new AccessDeniedException("가입 승인 권한이 없습니다.");
        }
    }

    private boolean isManagerOrAdmin(UserEntity user) {
        String roleName = user.getRole().getRoleName();
        return RoleNames.MANAGER.equals(roleName) || RoleNames.ADMIN.equals(roleName);
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
}
