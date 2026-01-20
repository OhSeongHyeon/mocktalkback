CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE tb_boards
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        to_tsvector(
            'simple',
            coalesce(board_name, '') || ' ' || coalesce(slug, '') || ' ' || coalesce(description, '')
        )
    ) STORED;

CREATE INDEX ix_tb_boards_search_vector ON tb_boards USING GIN (search_vector);
CREATE INDEX ix_tb_boards_board_name_trgm ON tb_boards USING GIN (board_name gin_trgm_ops);
CREATE INDEX ix_tb_boards_slug_trgm ON tb_boards USING GIN (slug gin_trgm_ops);
CREATE INDEX ix_tb_boards_description_trgm ON tb_boards USING GIN (description gin_trgm_ops);

ALTER TABLE tb_articles
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(content, '')), 'B')
    ) STORED;

CREATE INDEX ix_tb_articles_search_vector ON tb_articles USING GIN (search_vector);
CREATE INDEX ix_tb_articles_title_trgm ON tb_articles USING GIN (title gin_trgm_ops);
CREATE INDEX ix_tb_articles_content_trgm ON tb_articles USING GIN (content gin_trgm_ops);

ALTER TABLE tb_comments
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        to_tsvector('simple', coalesce(content, ''))
    ) STORED;

CREATE INDEX ix_tb_comments_search_vector ON tb_comments USING GIN (search_vector);
CREATE INDEX ix_tb_comments_content_trgm ON tb_comments USING GIN (content gin_trgm_ops);

ALTER TABLE tb_users
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        to_tsvector(
            'simple',
            coalesce(handle, '') || ' ' || coalesce(display_name, '') || ' ' || coalesce(user_name, '')
        )
    ) STORED;

CREATE INDEX ix_tb_users_search_vector ON tb_users USING GIN (search_vector);
CREATE INDEX ix_tb_users_handle_trgm ON tb_users USING GIN (handle gin_trgm_ops);
CREATE INDEX ix_tb_users_display_name_trgm ON tb_users USING GIN (display_name gin_trgm_ops);
CREATE INDEX ix_tb_users_user_name_trgm ON tb_users USING GIN (user_name gin_trgm_ops);
