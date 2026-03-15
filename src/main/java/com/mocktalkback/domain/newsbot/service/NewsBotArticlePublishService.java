package com.mocktalkback.domain.newsbot.service;

import org.springframework.stereotype.Service;

import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.article.service.ArticleContentService;
import com.mocktalkback.domain.article.type.ArticleContentFormat;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.newsbot.entity.NewsCollectionJobEntity;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewsBotArticlePublishService {

    private final ArticleRepository articleRepository;
    private final ArticleContentService articleContentService;
    private final NewsBotContentBuilder newsBotContentBuilder;

    public ArticleEntity createArticle(
        NewsCollectionJobEntity job,
        BoardEntity board,
        UserEntity author,
        ArticleCategoryEntity category,
        NewsBotSourceItem item
    ) {
        String contentSource = newsBotContentBuilder.build(item, job.getTimezone());
        ArticleContentService.RenderedContent renderedContent = articleContentService.render(
            contentSource,
            ArticleContentFormat.MARKDOWN
        );

        ArticleEntity article = ArticleEntity.builder()
            .board(board)
            .user(author)
            .category(category)
            .visibility(ContentVisibility.PUBLIC)
            .title(limitTitle(item.title()))
            .content(renderedContent.content())
            .contentSource(renderedContent.contentSource())
            .contentFormat(ArticleContentFormat.MARKDOWN)
            .hit(0L)
            .notice(false)
            .build();
        return articleRepository.save(article);
    }

    public ArticleEntity updateArticle(
        NewsCollectionJobEntity job,
        ArticleEntity article,
        ArticleCategoryEntity category,
        NewsBotSourceItem item
    ) {
        String contentSource = newsBotContentBuilder.build(item, job.getTimezone());
        ArticleContentService.RenderedContent renderedContent = articleContentService.render(
            contentSource,
            ArticleContentFormat.MARKDOWN
        );

        article.update(
            category,
            ContentVisibility.PUBLIC,
            limitTitle(item.title()),
            renderedContent.content(),
            renderedContent.contentSource(),
            ArticleContentFormat.MARKDOWN,
            false
        );
        return article;
    }

    private String limitTitle(String title) {
        if (title == null || title.length() <= 255) {
            return title;
        }
        return title.substring(0, 255);
    }
}
