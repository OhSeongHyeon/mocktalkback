package com.mocktalkback.domain.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.board.dto.BoardSubscribeItemResponse;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardSubscribeEntity;
import com.mocktalkback.domain.board.mapper.BoardMapper;
import com.mocktalkback.domain.board.repository.BoardFileRepository;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.repository.BoardSubscribeRepository;
import com.mocktalkback.domain.board.type.BoardArticleWritePolicy;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.common.policy.AuthorDisplayResolver;
import com.mocktalkback.domain.common.policy.BoardAccessPolicy;
import com.mocktalkback.domain.common.policy.PageNormalizer;
import com.mocktalkback.domain.common.policy.RoleEvaluator;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.file.repository.FileClassRepository;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.repository.FileVariantRepository;
import com.mocktalkback.domain.file.service.ImageOptimizationService;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardFileRepository boardFileRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private BoardSubscribeRepository boardSubscribeRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileClassRepository fileClassRepository;

    @Mock
    private FileVariantRepository fileVariantRepository;

    @Mock
    private ImageOptimizationService imageOptimizationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private FileMapper fileMapper;

    @Spy
    private RoleEvaluator roleEvaluator = new RoleEvaluator();

    @Spy
    private BoardAccessPolicy boardAccessPolicy = new BoardAccessPolicy(roleEvaluator);

    @Spy
    private PageNormalizer pageNormalizer = new PageNormalizer();

    @Spy
    private AuthorDisplayResolver authorDisplayResolver = new AuthorDisplayResolver();

    @InjectMocks
    private BoardService boardService;

    // 일반 사용자 구독 목록 조회는 현재 접근 가능한 게시판만 조회해야 한다.
    @Test
    void findSubscribes_uses_accessible_query_for_normal_user() {
        // Given: 일반 사용자와 접근 가능한 구독 목록
        UserEntity user = createUser(10L, "USER");
        BoardEntity board = createBoard(1L, BoardVisibility.PUBLIC);
        BoardSubscribeEntity subscribe = createSubscribe(100L, user, board);
        Page<BoardSubscribeEntity> page = new PageImpl<>(
            List.of(subscribe),
            PageRequest.of(0, 10),
            1L
        );

        when(currentUserService.getUserId()).thenReturn(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(boardSubscribeRepository.findAccessibleSubscribes(
            eq(10L),
            eq(List.of(BoardVisibility.PUBLIC, BoardVisibility.GROUP)),
            eq(BoardVisibility.PRIVATE),
            eq(BoardRole.OWNER),
            eq(BoardRole.BANNED),
            any()
        )).thenReturn(page);
        when(boardFileRepository.findAllByBoardIdInOrderByCreatedAtDesc(List.of(1L))).thenReturn(List.of());

        // When: 구독 목록을 조회하면
        PageResponse<BoardSubscribeItemResponse> response = boardService.findSubscribes(0, 10);

        // Then: 접근 가능한 구독 쿼리를 사용하고 응답을 반환한다.
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).boardId()).isEqualTo(1L);
        verify(boardSubscribeRepository).findAccessibleSubscribes(
            eq(10L),
            eq(List.of(BoardVisibility.PUBLIC, BoardVisibility.GROUP)),
            eq(BoardVisibility.PRIVATE),
            eq(BoardRole.OWNER),
            eq(BoardRole.BANNED),
            any()
        );
        verify(boardSubscribeRepository, never()).findAllByUserIdAndBoardDeletedAtIsNull(eq(10L), any());
    }

    // 관리자 구독 목록 조회는 전체 구독 목록을 유지해야 한다.
    @Test
    void findSubscribes_uses_all_subscribe_query_for_admin() {
        // Given: 관리자 사용자와 구독 목록
        UserEntity admin = createUser(20L, RoleNames.ADMIN);
        BoardEntity board = createBoard(2L, BoardVisibility.UNLISTED);
        BoardSubscribeEntity subscribe = createSubscribe(200L, admin, board);
        Page<BoardSubscribeEntity> page = new PageImpl<>(
            List.of(subscribe),
            PageRequest.of(0, 10),
            1L
        );

        when(currentUserService.getUserId()).thenReturn(20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(admin));
        when(boardSubscribeRepository.findAllByUserIdAndBoardDeletedAtIsNull(eq(20L), any())).thenReturn(page);
        when(boardFileRepository.findAllByBoardIdInOrderByCreatedAtDesc(List.of(2L))).thenReturn(List.of());

        // When: 관리자 구독 목록을 조회하면
        PageResponse<BoardSubscribeItemResponse> response = boardService.findSubscribes(0, 10);

        // Then: 전체 구독 쿼리를 사용하고 기존 동작을 유지한다.
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).boardId()).isEqualTo(2L);
        verify(boardSubscribeRepository).findAllByUserIdAndBoardDeletedAtIsNull(eq(20L), any());
        verify(boardSubscribeRepository, never()).findAccessibleSubscribes(
            eq(20L),
            any(),
            any(),
            any(),
            any(),
            any()
        );
    }

    private UserEntity createUser(Long id, String roleName) {
        RoleEntity role = RoleEntity.create(roleName, 0, "테스트 권한");
        UserEntity user = UserEntity.createLocal(
            role,
            "login-" + id,
            "user-" + id + "@test.com",
            "pw",
            "user-" + id,
            "display-" + id,
            "handle-" + id
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private BoardEntity createBoard(Long id, BoardVisibility visibility) {
        BoardEntity board = BoardEntity.builder()
            .boardName("board-" + id)
            .slug("board-" + id)
            .description("테스트 게시판")
            .visibility(visibility)
            .articleWritePolicy(BoardArticleWritePolicy.ALL_AUTHENTICATED)
            .build();
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    private BoardSubscribeEntity createSubscribe(Long id, UserEntity user, BoardEntity board) {
        BoardSubscribeEntity subscribe = BoardSubscribeEntity.builder()
            .user(user)
            .board(board)
            .build();
        ReflectionTestUtils.setField(subscribe, "id", id);
        ReflectionTestUtils.setField(subscribe, "createdAt", Instant.parse("2024-01-01T00:00:00Z"));
        return subscribe;
    }
}
