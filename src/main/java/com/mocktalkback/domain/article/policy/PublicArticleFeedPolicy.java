package com.mocktalkback.domain.article.policy;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.role.type.ContentVisibility;

@Component
public class PublicArticleFeedPolicy {

    private static final Set<String> EXCLUDED_BOARD_SLUGS = Set.of("notice", "inquiry");

    public Set<String> excludedBoardSlugs() {
        return EXCLUDED_BOARD_SLUGS;
    }

    public boolean isPublicFeedTarget(ArticleEntity article) {
        if (article == null || article.isDeleted() || article.getVisibility() != ContentVisibility.PUBLIC) {
            return false;
        }

        BoardEntity board = article.getBoard();
        return board != null
            && !board.isDeleted()
            && board.getVisibility() == BoardVisibility.PUBLIC
            && !EXCLUDED_BOARD_SLUGS.contains(board.getSlug());
    }
}
