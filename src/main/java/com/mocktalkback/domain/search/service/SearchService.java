package com.mocktalkback.domain.search.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.entity.QArticleEntity;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardFileEntity;
import com.mocktalkback.domain.board.entity.QBoardEntity;
import com.mocktalkback.domain.board.repository.BoardFileRepository;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.entity.QCommentEntity;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.search.dto.ArticleSearchResponse;
import com.mocktalkback.domain.search.dto.BoardSearchResponse;
import com.mocktalkback.domain.search.dto.CommentSearchResponse;
import com.mocktalkback.domain.search.dto.SearchResponse;
import com.mocktalkback.domain.search.dto.UserSearchResponse;
import com.mocktalkback.domain.search.type.SearchType;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.entity.QUserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.SliceResponse;
import com.mocktalkback.global.common.type.SortOrder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE = 0;

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final BoardFileRepository boardFileRepository;
    private final CommentRepository commentRepository;
    private final ArticleReactionRepository articleReactionRepository;
    private final FileMapper fileMapper;

    @Transactional(readOnly = true)
    public SearchResponse search(
        String keyword,
        SearchType type,
        SortOrder order,
        Integer page,
        Integer size,
        String boardSlug
    ) {
        String normalized = normalizeKeyword(keyword);
        int resolvedPage = normalizePage(page);
        int resolvedSize = normalizeSize(size);
        SearchUserContext context = resolveUserContext();
        SearchType resolvedType = type == null ? SearchType.ALL : type;
        SortOrder resolvedOrder = order == null ? SortOrder.LATEST : order;

        SliceResponse<BoardSearchResponse> boards = resolvedType == SearchType.ALL || resolvedType == SearchType.BOARD
            ? searchBoards(normalized, resolvedPage, resolvedSize, resolvedOrder, context)
            : emptySlice(resolvedPage, resolvedSize);
        SliceResponse<ArticleSearchResponse> articles = resolvedType == SearchType.ALL || resolvedType == SearchType.ARTICLE
            ? searchArticles(normalized, resolvedPage, resolvedSize, resolvedOrder, boardSlug, context)
            : emptySlice(resolvedPage, resolvedSize);
        SliceResponse<CommentSearchResponse> comments = resolvedType == SearchType.ALL || resolvedType == SearchType.COMMENT
            ? searchComments(normalized, resolvedPage, resolvedSize, resolvedOrder, boardSlug, context)
            : emptySlice(resolvedPage, resolvedSize);
        SliceResponse<UserSearchResponse> users = resolvedType == SearchType.ALL || resolvedType == SearchType.USER
            ? searchUsers(normalized, resolvedPage, resolvedSize, resolvedOrder)
            : emptySlice(resolvedPage, resolvedSize);

        return new SearchResponse(boards, articles, comments, users);
    }

    private SliceResponse<BoardSearchResponse> searchBoards(
        String keyword,
        int page,
        int size,
        SortOrder order,
        SearchUserContext context
    ) {
        long offset = (long) page * size;
        List<Long> ftsIds = fetchBoardIdsByFts(keyword, offset, size + 1, order, context);
        boolean hasNext = ftsIds.size() > size;
        Set<Long> idSet = new LinkedHashSet<>();
        for (int index = 0; index < Math.min(size, ftsIds.size()); index++) {
            idSet.add(ftsIds.get(index));
        }
        if (!hasNext && idSet.size() < size) {
            int remaining = size - idSet.size();
            List<Long> fallbackIds = fetchBoardIdsByIlike(keyword, offset, remaining + 1, order, context, List.copyOf(idSet));
            boolean fallbackHasNext = fallbackIds.size() > remaining;
            for (Long id : fallbackIds) {
                if (idSet.size() >= size) {
                    break;
                }
                idSet.add(id);
            }
            hasNext = fallbackHasNext;
        }
        List<Long> ids = new ArrayList<>(idSet);
        if (ids.isEmpty()) {
            return emptySlice(page, size);
        }
        List<BoardEntity> boards = loadBoards(ids);
        Map<Long, FileResponse> boardImages = resolveBoardImages(boards);
        List<BoardSearchResponse> items = boards.stream()
            .map(entity -> new BoardSearchResponse(
                entity.getId(),
                entity.getBoardName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getVisibility(),
                boardImages.get(entity.getId()),
                entity.getCreatedAt()
            ))
            .toList();

        return new SliceResponse<>(items, page, size, hasNext, page > 0);
    }

    private SliceResponse<ArticleSearchResponse> searchArticles(
        String keyword,
        int page,
        int size,
        SortOrder order,
        String boardSlug,
        SearchUserContext context
    ) {
        long offset = (long) page * size;
        List<Long> ftsIds = fetchArticleIdsByFts(keyword, offset, size + 1, order, boardSlug, context);
        boolean hasNext = ftsIds.size() > size;
        Set<Long> idSet = new LinkedHashSet<>();
        for (int index = 0; index < Math.min(size, ftsIds.size()); index++) {
            idSet.add(ftsIds.get(index));
        }
        if (!hasNext && idSet.size() < size) {
            int remaining = size - idSet.size();
            List<Long> fallbackIds = fetchArticleIdsByIlike(keyword, offset, remaining + 1, order, boardSlug, context, List.copyOf(idSet));
            boolean fallbackHasNext = fallbackIds.size() > remaining;
            for (Long id : fallbackIds) {
                if (idSet.size() >= size) {
                    break;
                }
                idSet.add(id);
            }
            hasNext = fallbackHasNext;
        }
        List<Long> ids = new ArrayList<>(idSet);
        if (ids.isEmpty()) {
            return emptySlice(page, size);
        }
        List<ArticleEntity> articles = loadArticles(ids);
        Map<Long, Long> commentCounts = loadCommentCounts(articles);
        Map<Long, ReactionCounts> reactionCounts = loadReactionCounts(articles);
        List<ArticleSearchResponse> items = articles.stream()
            .map(entity -> toArticleSearchResponse(entity, commentCounts, reactionCounts))
            .toList();

        return new SliceResponse<>(items, page, size, hasNext, page > 0);
    }

    private SliceResponse<CommentSearchResponse> searchComments(
        String keyword,
        int page,
        int size,
        SortOrder order,
        String boardSlug,
        SearchUserContext context
    ) {
        long offset = (long) page * size;
        List<Long> ftsIds = fetchCommentIdsByFts(keyword, offset, size + 1, order, boardSlug, context);
        boolean hasNext = ftsIds.size() > size;
        Set<Long> idSet = new LinkedHashSet<>();
        for (int index = 0; index < Math.min(size, ftsIds.size()); index++) {
            idSet.add(ftsIds.get(index));
        }
        if (!hasNext && idSet.size() < size) {
            int remaining = size - idSet.size();
            List<Long> fallbackIds = fetchCommentIdsByIlike(keyword, offset, remaining + 1, order, boardSlug, context, List.copyOf(idSet));
            boolean fallbackHasNext = fallbackIds.size() > remaining;
            for (Long id : fallbackIds) {
                if (idSet.size() >= size) {
                    break;
                }
                idSet.add(id);
            }
            hasNext = fallbackHasNext;
        }
        List<Long> ids = new ArrayList<>(idSet);
        if (ids.isEmpty()) {
            return emptySlice(page, size);
        }
        List<CommentEntity> comments = loadComments(ids);
        List<CommentSearchResponse> items = comments.stream()
            .map(this::toCommentSearchResponse)
            .toList();

        return new SliceResponse<>(items, page, size, hasNext, page > 0);
    }

    private SliceResponse<UserSearchResponse> searchUsers(
        String keyword,
        int page,
        int size,
        SortOrder order
    ) {
        long offset = (long) page * size;
        List<Long> ftsIds = fetchUserIdsByFts(keyword, offset, size + 1, order);
        boolean hasNext = ftsIds.size() > size;
        Set<Long> idSet = new LinkedHashSet<>();
        for (int index = 0; index < Math.min(size, ftsIds.size()); index++) {
            idSet.add(ftsIds.get(index));
        }
        if (!hasNext && idSet.size() < size) {
            int remaining = size - idSet.size();
            List<Long> fallbackIds = fetchUserIdsByIlike(keyword, offset, remaining + 1, order, List.copyOf(idSet));
            boolean fallbackHasNext = fallbackIds.size() > remaining;
            for (Long id : fallbackIds) {
                if (idSet.size() >= size) {
                    break;
                }
                idSet.add(id);
            }
            hasNext = fallbackHasNext;
        }
        List<Long> ids = new ArrayList<>(idSet);
        if (ids.isEmpty()) {
            return emptySlice(page, size);
        }
        List<UserEntity> users = loadUsers(ids);
        List<UserSearchResponse> items = users.stream()
            .map(entity -> new UserSearchResponse(
                entity.getId(),
                entity.getHandle(),
                entity.getDisplayName(),
                entity.getCreatedAt()
            ))
            .toList();

        return new SliceResponse<>(items, page, size, hasNext, page > 0);
    }

    private List<Long> fetchBoardIdsByFts(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        SearchUserContext context
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        sql.append("select b.board_id ");
        sql.append("from tb_boards b ");
        if (context.isAuthenticated() && !context.isManagerOrAdmin()) {
            sql.append("left join tb_board_members bm on bm.board_id = b.board_id and bm.user_id = :userId ");
            params.put("userId", context.userId());
        }
        sql.append("where b.deleted_at is null ");
        if (!context.isAuthenticated()) {
            sql.append("and b.visibility = 'PUBLIC' ");
        } else if (!context.isManagerOrAdmin()) {
            sql.append("and (bm.board_manager_id is null or bm.board_role <> 'BANNED') ");
            sql.append("and (b.visibility in ('PUBLIC','GROUP') ");
            sql.append("or (b.visibility = 'PRIVATE' and bm.board_role = 'OWNER')) ");
        }
        sql.append("and b.search_vector @@ plainto_tsquery('simple', :keyword) ");
        sql.append("order by ts_rank(b.search_vector, plainto_tsquery('simple', :keyword)) desc, ");
        sql.append(resolveBoardOrderBy(order));
        sql.append(" offset :offset rows fetch first :limit rows only");

        params.put("keyword", keyword);
        params.put("offset", offset);
        params.put("limit", limit);
        return fetchIds(sql.toString(), params);
    }

    private List<Long> fetchBoardIdsByIlike(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        SearchUserContext context,
        List<Long> excludeIds
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        sql.append("select b.board_id ");
        sql.append("from tb_boards b ");
        if (context.isAuthenticated() && !context.isManagerOrAdmin()) {
            sql.append("left join tb_board_members bm on bm.board_id = b.board_id and bm.user_id = :userId ");
            params.put("userId", context.userId());
        }
        sql.append("where b.deleted_at is null ");
        if (!context.isAuthenticated()) {
            sql.append("and b.visibility = 'PUBLIC' ");
        } else if (!context.isManagerOrAdmin()) {
            sql.append("and (bm.board_manager_id is null or bm.board_role <> 'BANNED') ");
            sql.append("and (b.visibility in ('PUBLIC','GROUP') ");
            sql.append("or (b.visibility = 'PRIVATE' and bm.board_role = 'OWNER')) ");
        }
        sql.append("and (b.board_name ilike :pattern ");
        sql.append("or b.slug ilike :pattern ");
        sql.append("or b.description ilike :pattern) ");
        if (!excludeIds.isEmpty()) {
            sql.append("and b.board_id not in (:excludeIds) ");
            params.put("excludeIds", excludeIds);
        }
        sql.append("order by greatest(");
        sql.append("coalesce(extensions.similarity(b.board_name, :keyword), 0), ");
        sql.append("coalesce(extensions.similarity(b.slug, :keyword), 0), ");
        sql.append("coalesce(extensions.similarity(b.description, :keyword), 0)) desc, ");
        sql.append(resolveBoardOrderBy(order));
        sql.append(" offset :offset rows fetch first :limit rows only");

        params.put("keyword", keyword);
        params.put("pattern", "%" + keyword + "%");
        params.put("offset", offset);
        params.put("limit", limit);
        return fetchIds(sql.toString(), params);
    }

    private List<Long> fetchArticleIdsByFts(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        String boardSlug,
        SearchUserContext context
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        sql.append("select a.article_id ");
        sql.append("from tb_articles a ");
        sql.append("join tb_boards b on b.board_id = a.board_id ");
        if (context.isAuthenticated() && !context.isManagerOrAdmin()) {
            sql.append("left join tb_board_members bm on bm.board_id = b.board_id and bm.user_id = :userId ");
            params.put("userId", context.userId());
        }
        sql.append("where a.deleted_at is null ");
        sql.append("and b.deleted_at is null ");
        if (StringUtils.hasText(boardSlug)) {
            sql.append("and b.slug = :boardSlug ");
            params.put("boardSlug", boardSlug);
        }
        appendArticleAccessFilter(sql, context);
        sql.append("and a.search_vector @@ plainto_tsquery('simple', :keyword) ");
        sql.append("order by ts_rank(a.search_vector, plainto_tsquery('simple', :keyword)) desc, ");
        sql.append(resolveArticleOrderBy(order));
        sql.append(" offset :offset rows fetch first :limit rows only");

        params.put("keyword", keyword);
        params.put("offset", offset);
        params.put("limit", limit);
        return fetchIds(sql.toString(), params);
    }

    private List<Long> fetchArticleIdsByIlike(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        String boardSlug,
        SearchUserContext context,
        List<Long> excludeIds
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        sql.append("select a.article_id ");
        sql.append("from tb_articles a ");
        sql.append("join tb_boards b on b.board_id = a.board_id ");
        if (context.isAuthenticated() && !context.isManagerOrAdmin()) {
            sql.append("left join tb_board_members bm on bm.board_id = b.board_id and bm.user_id = :userId ");
            params.put("userId", context.userId());
        }
        sql.append("where a.deleted_at is null ");
        sql.append("and b.deleted_at is null ");
        if (StringUtils.hasText(boardSlug)) {
            sql.append("and b.slug = :boardSlug ");
            params.put("boardSlug", boardSlug);
        }
        appendArticleAccessFilter(sql, context);
        sql.append("and (a.title ilike :pattern or a.content ilike :pattern) ");
        if (!excludeIds.isEmpty()) {
            sql.append("and a.article_id not in (:excludeIds) ");
            params.put("excludeIds", excludeIds);
        }
        sql.append("order by greatest(");
        sql.append("coalesce(extensions.similarity(a.title, :keyword), 0), ");
        sql.append("coalesce(extensions.similarity(a.content, :keyword), 0)) desc, ");
        sql.append(resolveArticleOrderBy(order));
        sql.append(" offset :offset rows fetch first :limit rows only");

        params.put("keyword", keyword);
        params.put("pattern", "%" + keyword + "%");
        params.put("offset", offset);
        params.put("limit", limit);
        return fetchIds(sql.toString(), params);
    }

    private List<Long> fetchCommentIdsByFts(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        String boardSlug,
        SearchUserContext context
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        sql.append("select c.comment_id ");
        sql.append("from tb_comments c ");
        sql.append("join tb_articles a on a.article_id = c.article_id ");
        sql.append("join tb_boards b on b.board_id = a.board_id ");
        if (context.isAuthenticated() && !context.isManagerOrAdmin()) {
            sql.append("left join tb_board_members bm on bm.board_id = b.board_id and bm.user_id = :userId ");
            params.put("userId", context.userId());
        }
        sql.append("where c.deleted_at is null ");
        sql.append("and a.deleted_at is null ");
        sql.append("and b.deleted_at is null ");
        if (StringUtils.hasText(boardSlug)) {
            sql.append("and b.slug = :boardSlug ");
            params.put("boardSlug", boardSlug);
        }
        appendArticleAccessFilter(sql, context);
        sql.append("and c.search_vector @@ plainto_tsquery('simple', :keyword) ");
        sql.append("order by ts_rank(c.search_vector, plainto_tsquery('simple', :keyword)) desc, ");
        sql.append(resolveCommentOrderBy(order));
        sql.append(" offset :offset rows fetch first :limit rows only");

        params.put("keyword", keyword);
        params.put("offset", offset);
        params.put("limit", limit);
        return fetchIds(sql.toString(), params);
    }

    private List<Long> fetchCommentIdsByIlike(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        String boardSlug,
        SearchUserContext context,
        List<Long> excludeIds
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        sql.append("select c.comment_id ");
        sql.append("from tb_comments c ");
        sql.append("join tb_articles a on a.article_id = c.article_id ");
        sql.append("join tb_boards b on b.board_id = a.board_id ");
        if (context.isAuthenticated() && !context.isManagerOrAdmin()) {
            sql.append("left join tb_board_members bm on bm.board_id = b.board_id and bm.user_id = :userId ");
            params.put("userId", context.userId());
        }
        sql.append("where c.deleted_at is null ");
        sql.append("and a.deleted_at is null ");
        sql.append("and b.deleted_at is null ");
        if (StringUtils.hasText(boardSlug)) {
            sql.append("and b.slug = :boardSlug ");
            params.put("boardSlug", boardSlug);
        }
        appendArticleAccessFilter(sql, context);
        sql.append("and c.content ilike :pattern ");
        if (!excludeIds.isEmpty()) {
            sql.append("and c.comment_id not in (:excludeIds) ");
            params.put("excludeIds", excludeIds);
        }
        sql.append("order by coalesce(extensions.similarity(c.content, :keyword), 0) desc, ");
        sql.append(resolveCommentOrderBy(order));
        sql.append(" offset :offset rows fetch first :limit rows only");

        params.put("keyword", keyword);
        params.put("pattern", "%" + keyword + "%");
        params.put("offset", offset);
        params.put("limit", limit);
        return fetchIds(sql.toString(), params);
    }

    private List<Long> fetchUserIdsByFts(
        String keyword,
        long offset,
        int limit,
        SortOrder order
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        sql.append("select u.user_id ");
        sql.append("from tb_users u ");
        sql.append("where u.deleted_at is null ");
        sql.append("and u.search_vector @@ plainto_tsquery('simple', :keyword) ");
        sql.append("order by ts_rank(u.search_vector, plainto_tsquery('simple', :keyword)) desc, ");
        sql.append(resolveUserOrderBy(order));
        sql.append(" offset :offset rows fetch first :limit rows only");

        params.put("keyword", keyword);
        params.put("offset", offset);
        params.put("limit", limit);
        return fetchIds(sql.toString(), params);
    }

    private List<Long> fetchUserIdsByIlike(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        List<Long> excludeIds
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        sql.append("select u.user_id ");
        sql.append("from tb_users u ");
        sql.append("where u.deleted_at is null ");
        sql.append("and (u.handle ilike :pattern or u.display_name ilike :pattern or u.user_name ilike :pattern) ");
        if (!excludeIds.isEmpty()) {
            sql.append("and u.user_id not in (:excludeIds) ");
            params.put("excludeIds", excludeIds);
        }
        sql.append("order by greatest(");
        sql.append("coalesce(extensions.similarity(u.handle, :keyword), 0), ");
        sql.append("coalesce(extensions.similarity(u.display_name, :keyword), 0), ");
        sql.append("coalesce(extensions.similarity(u.user_name, :keyword), 0)) desc, ");
        sql.append(resolveUserOrderBy(order));
        sql.append(" offset :offset rows fetch first :limit rows only");

        params.put("keyword", keyword);
        params.put("pattern", "%" + keyword + "%");
        params.put("offset", offset);
        params.put("limit", limit);
        return fetchIds(sql.toString(), params);
    }

    private List<Long> fetchIds(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        List<?> rows = query.getResultList();
        List<Long> ids = new ArrayList<>();
        for (Object row : rows) {
            if (row instanceof Number number) {
                ids.add(number.longValue());
            }
        }
        return ids;
    }

    private void appendArticleAccessFilter(StringBuilder sql, SearchUserContext context) {
        if (!context.isAuthenticated()) {
            sql.append("and b.visibility = 'PUBLIC' and a.visibility = 'PUBLIC' ");
            return;
        }
        if (context.isManagerOrAdmin()) {
            return;
        }
        sql.append("and (");
        sql.append("(bm.board_manager_id is null or bm.board_role <> 'BANNED') ");
        sql.append("and (");
        sql.append("(b.visibility = 'PUBLIC' and a.visibility in ('PUBLIC', 'MEMBERS')) ");
        sql.append("or (b.visibility = 'PUBLIC' and bm.board_role in ('OWNER', 'MODERATOR') and a.visibility = 'MODERATORS') ");
        sql.append("or (b.visibility = 'GROUP' and (");
        sql.append("a.visibility = 'PUBLIC' ");
        sql.append("or (a.visibility = 'MEMBERS' and bm.board_role in ('OWNER', 'MODERATOR', 'MEMBER')) ");
        sql.append("or (a.visibility = 'MODERATORS' and bm.board_role in ('OWNER', 'MODERATOR'))");
        sql.append(")) ");
        sql.append("or (b.visibility = 'PRIVATE' and bm.board_role = 'OWNER' ");
        sql.append("and a.visibility in ('PUBLIC', 'MEMBERS', 'MODERATORS'))");
        sql.append(")) ");
    }

    private String resolveBoardOrderBy(SortOrder order) {
        if (order == SortOrder.OLDEST) {
            return "b.created_at asc, b.updated_at asc, b.board_id asc";
        }
        return "b.created_at desc, b.updated_at desc, b.board_id desc";
    }

    private String resolveArticleOrderBy(SortOrder order) {
        if (order == SortOrder.OLDEST) {
            return "a.created_at asc, a.updated_at asc, a.article_id asc";
        }
        return "a.created_at desc, a.updated_at desc, a.article_id desc";
    }

    private String resolveCommentOrderBy(SortOrder order) {
        if (order == SortOrder.OLDEST) {
            return "c.created_at asc, c.comment_id asc";
        }
        return "c.created_at desc, c.comment_id desc";
    }

    private String resolveUserOrderBy(SortOrder order) {
        if (order == SortOrder.OLDEST) {
            return "u.created_at asc, u.user_id asc";
        }
        return "u.created_at desc, u.user_id desc";
    }

    private List<BoardEntity> loadBoards(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        QBoardEntity board = QBoardEntity.boardEntity;
        List<BoardEntity> rows = queryFactory.selectFrom(board)
            .where(board.id.in(ids))
            .fetch();
        return orderByIds(ids, rows, BoardEntity::getId);
    }

    private List<ArticleEntity> loadArticles(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        QArticleEntity article = QArticleEntity.articleEntity;
        QBoardEntity board = QBoardEntity.boardEntity;
        QUserEntity author = QUserEntity.userEntity;
        List<ArticleEntity> rows = queryFactory.selectFrom(article)
            .join(article.board, board)
            .join(article.user, author)
            .where(article.id.in(ids))
            .fetch();
        return orderByIds(ids, rows, ArticleEntity::getId);
    }

    private List<CommentEntity> loadComments(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        QCommentEntity comment = QCommentEntity.commentEntity;
        QArticleEntity article = QArticleEntity.articleEntity;
        QBoardEntity board = QBoardEntity.boardEntity;
        QUserEntity author = QUserEntity.userEntity;
        List<CommentEntity> rows = queryFactory.selectFrom(comment)
            .join(comment.article, article)
            .join(article.board, board)
            .join(comment.user, author)
            .where(comment.id.in(ids))
            .fetch();
        return orderByIds(ids, rows, CommentEntity::getId);
    }

    private List<UserEntity> loadUsers(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        QUserEntity user = QUserEntity.userEntity;
        List<UserEntity> rows = queryFactory.selectFrom(user)
            .where(user.id.in(ids))
            .fetch();
        return orderByIds(ids, rows, UserEntity::getId);
    }

    private <T> List<T> orderByIds(List<Long> ids, List<T> rows, Function<T, Long> idExtractor) {
        Map<Long, T> map = new HashMap<>();
        for (T row : rows) {
            map.put(idExtractor.apply(row), row);
        }
        List<T> ordered = new ArrayList<>();
        for (Long id : ids) {
            T row = map.get(id);
            if (row != null) {
                ordered.add(row);
            }
        }
        return ordered;
    }

    private <T> SliceResponse<T> emptySlice(int page, int size) {
        return new SliceResponse<>(List.of(), page, size, false, page > 0);
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

    private Map<Long, Long> loadCommentCounts(List<ArticleEntity> articles) {
        Set<Long> ids = new LinkedHashSet<>();
        articles.forEach(article -> ids.add(article.getId()));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return commentRepository.countByArticleIds(ids).stream()
            .collect(java.util.stream.Collectors.toMap(
                CommentRepository.CommentCountView::getArticleId,
                CommentRepository.CommentCountView::getCount
            ));
    }

    private Map<Long, ReactionCounts> loadReactionCounts(List<ArticleEntity> articles) {
        Set<Long> ids = new LinkedHashSet<>();
        articles.forEach(article -> ids.add(article.getId()));
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, ReactionCounts> counts = new HashMap<>();
        List<ArticleReactionRepository.ArticleReactionCountView> rows = articleReactionRepository.countByArticleIds(ids);
        for (ArticleReactionRepository.ArticleReactionCountView row : rows) {
            ReactionCounts current = counts.getOrDefault(row.getArticleId(), ReactionCounts.empty());
            if (row.getReactionType() == 1) {
                counts.put(row.getArticleId(), current.withLike(row.getCount()));
            } else if (row.getReactionType() == -1) {
                counts.put(row.getArticleId(), current.withDislike(row.getCount()));
            }
        }
        return counts;
    }

    private ArticleSearchResponse toArticleSearchResponse(
        ArticleEntity entity,
        Map<Long, Long> commentCounts,
        Map<Long, ReactionCounts> reactionCounts
    ) {
        ReactionCounts counts = reactionCounts.getOrDefault(entity.getId(), ReactionCounts.empty());
        return new ArticleSearchResponse(
            entity.getId(),
            entity.getBoard().getId(),
            entity.getBoard().getSlug(),
            entity.getBoard().getBoardName(),
            entity.getUser().getId(),
            resolveAuthorName(entity.getUser()),
            entity.getTitle(),
            entity.getHit(),
            commentCounts.getOrDefault(entity.getId(), 0L),
            counts.likeCount(),
            counts.dislikeCount(),
            entity.isNotice(),
            entity.getCreatedAt()
        );
    }

    private CommentSearchResponse toCommentSearchResponse(CommentEntity entity) {
        ArticleEntity article = entity.getArticle();
        BoardEntity board = article.getBoard();
        return new CommentSearchResponse(
            entity.getId(),
            article.getId(),
            article.getTitle(),
            board.getId(),
            board.getSlug(),
            board.getBoardName(),
            entity.getUser().getId(),
            resolveAuthorName(entity.getUser()),
            entity.getContent(),
            entity.getCreatedAt()
        );
    }

    private String resolveAuthorName(UserEntity user) {
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return user.getUserName();
    }

    private SearchUserContext resolveUserContext() {
        return currentUserService.getOptionalUserId()
            .map(this::loadUserContext)
            .orElse(SearchUserContext.anonymous());
    }

    private SearchUserContext loadUserContext(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        return new SearchUserContext(userId, isManagerOrAdmin(user));
    }

    private boolean isManagerOrAdmin(UserEntity user) {
        String roleName = user.getRole().getRoleName();
        return RoleNames.MANAGER.equals(roleName) || RoleNames.ADMIN.equals(roleName);
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }
        return keyword.trim();
    }

    private int normalizePage(Integer page) {
        int resolvedPage = page == null ? DEFAULT_PAGE : page;
        if (resolvedPage < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        return resolvedPage;
    }

    private int normalizeSize(Integer size) {
        int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (resolvedSize <= 0 || resolvedSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        return resolvedSize;
    }

    private record ReactionCounts(long likeCount, long dislikeCount) {
        private static ReactionCounts empty() {
            return new ReactionCounts(0, 0);
        }

        private ReactionCounts withLike(long count) {
            return new ReactionCounts(count, dislikeCount);
        }

        private ReactionCounts withDislike(long count) {
            return new ReactionCounts(likeCount, count);
        }
    }

    private record SearchUserContext(Long userId, boolean managerOrAdmin) {
        private static SearchUserContext anonymous() {
            return new SearchUserContext(null, false);
        }

        private boolean isAuthenticated() {
            return userId != null;
        }

        private boolean isManagerOrAdmin() {
            return managerOrAdmin;
        }
    }
}
