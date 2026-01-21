package com.mocktalkback.domain.moderation.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.moderation.dto.BoardMemberListItemResponse;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardMemberAdminService {

    private static final int MAX_PAGE_SIZE = 50;

    private final BoardMemberRepository boardMemberRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PageResponse<BoardMemberListItemResponse> findMembers(
        Long boardId,
        BoardRole status,
        int page,
        int size
    ) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);

        List<BoardRole> roles = status == null ? Arrays.asList(BoardRole.values()) : List.of(status);
        Pageable pageable = toPageable(page, size);
        Page<BoardMemberEntity> result = boardMemberRepository.findAllByBoardIdAndBoardRoleIn(boardId, roles, pageable);
        Page<BoardMemberListItemResponse> mapped = result.map(BoardMemberListItemResponse::from);
        return PageResponse.from(mapped);
    }

    @Transactional
    public BoardMemberListItemResponse approve(Long boardId, Long memberId) {
        BoardMemberEntity member = getBoardMember(boardId, memberId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, member.getBoard());

        if (member.getBoardRole() != BoardRole.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "승인 대기 상태가 아닙니다.");
        }
        member.approve(actor);
        return BoardMemberListItemResponse.from(member);
    }

    @Transactional
    public void reject(Long boardId, Long memberId) {
        BoardMemberEntity member = getBoardMember(boardId, memberId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, member.getBoard());

        if (member.getBoardRole() != BoardRole.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "승인 대기 상태가 아닙니다.");
        }
        boardMemberRepository.delete(member);
    }

    @Transactional
    public BoardMemberListItemResponse changeRole(Long boardId, Long memberId, BoardRole targetRole) {
        BoardMemberEntity member = getBoardMember(boardId, memberId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, member.getBoard());
        ensureNotOwner(member, actor);

        if (targetRole != BoardRole.MEMBER && targetRole != BoardRole.MODERATOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경할 역할이 올바르지 않습니다.");
        }
        if (member.getBoardRole() == BoardRole.PENDING || member.getBoardRole() == BoardRole.BANNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "역할 변경 대상이 아닙니다.");
        }
        member.changeRole(targetRole, actor);
        return BoardMemberListItemResponse.from(member);
    }

    @Transactional
    public BoardMemberListItemResponse changeStatus(Long boardId, Long memberId, BoardRole targetRole) {
        BoardMemberEntity member = getBoardMember(boardId, memberId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, member.getBoard());
        ensureNotOwner(member, actor);

        if (targetRole == BoardRole.BANNED) {
            if (member.getBoardRole() == BoardRole.BANNED) {
                return BoardMemberListItemResponse.from(member);
            }
            if (member.getBoardRole() == BoardRole.PENDING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인 대기 상태는 차단할 수 없습니다.");
            }
            member.changeRole(BoardRole.BANNED, actor);
            return BoardMemberListItemResponse.from(member);
        }
        if (targetRole == BoardRole.MEMBER) {
            if (member.getBoardRole() != BoardRole.BANNED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "차단 상태만 해제할 수 있습니다.");
            }
            member.changeRole(BoardRole.MEMBER, actor);
            return BoardMemberListItemResponse.from(member);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경할 상태가 올바르지 않습니다.");
    }

    private BoardMemberEntity getBoardMember(Long boardId, Long memberId) {
        BoardEntity board = getBoard(boardId);
        BoardMemberEntity member = boardMemberRepository.findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."));
        if (!board.getId().equals(member.getBoard().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판 멤버가 아닙니다.");
        }
        return member;
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

    private void ensureNotOwner(BoardMemberEntity member, UserEntity actor) {
        if (member.getBoardRole() == BoardRole.OWNER && !RoleNames.ADMIN.equals(actor.getRole().getRoleName())) {
            throw new AccessDeniedException("OWNER 권한은 변경할 수 없습니다.");
        }
    }

    private Pageable toPageable(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
