package com.mocktalkback.domain.newsbot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.repository.ArticleCategoryRepository;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.type.BoardArticleWritePolicy;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.newsbot.entity.NewsCollectionJobEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewsBotBoardProvisionService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final ArticleCategoryRepository articleCategoryRepository;

    @Transactional
    public BoardEntity ensureBoard(NewsCollectionJobEntity job) {
        BoardEntity existing = boardRepository.findBySlugAndDeletedAtIsNull(job.getTargetBoardSlug()).orElse(null);
        if (existing != null) {
            return existing;
        }
        if (!job.isAutoCreateBoard()) {
            throw new IllegalArgumentException("대상 게시판이 없고 자동 생성도 비활성화되어 있습니다: " + job.getTargetBoardSlug());
        }

        String boardName = StringUtils.hasText(job.getTargetBoardName())
            ? job.getTargetBoardName().trim()
            : job.getTargetBoardSlug();

        BoardEntity created = boardRepository.save(
            BoardEntity.builder()
                .boardName(boardName)
                .slug(job.getTargetBoardSlug())
                .description("뉴스봇이 외부 새소식을 자동 수집해 적재하는 게시판입니다.")
                .visibility(BoardVisibility.PUBLIC)
                .articleWritePolicy(BoardArticleWritePolicy.OWNER)
                .build()
        );

        BoardMemberEntity owner = BoardMemberEntity.builder()
            .user(job.getAuthorUser())
            .board(created)
            .grantedByUser(job.getUpdatedByUser())
            .boardRole(BoardRole.OWNER)
            .build();
        boardMemberRepository.save(owner);
        return created;
    }

    @Transactional
    public ArticleCategoryEntity ensureCategory(BoardEntity board, NewsCollectionJobEntity job) {
        if (!StringUtils.hasText(job.getTargetCategoryName())) {
            return null;
        }

        ArticleCategoryEntity existing = articleCategoryRepository
            .findByBoardIdAndCategoryNameIgnoreCase(board.getId(), job.getTargetCategoryName().trim())
            .orElse(null);
        if (existing != null) {
            return existing;
        }
        if (!job.isAutoCreateCategory()) {
            throw new IllegalArgumentException("대상 카테고리가 없고 자동 생성도 비활성화되어 있습니다: " + job.getTargetCategoryName());
        }

        return articleCategoryRepository.save(
            ArticleCategoryEntity.builder()
                .board(board)
                .categoryName(job.getTargetCategoryName().trim())
                .build()
        );
    }
}
