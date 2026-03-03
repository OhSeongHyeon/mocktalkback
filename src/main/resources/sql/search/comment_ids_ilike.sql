select c.comment_id
from tb_comments c
join tb_articles a on a.article_id = c.article_id
join tb_boards b on b.board_id = a.board_id
left join tb_board_members bm on bm.board_id = b.board_id and bm.user_id = :userId
where c.deleted_at is null
  and a.deleted_at is null
  and b.deleted_at is null
  and (:hasBoardSlug = false or b.slug = :boardSlug)
  and (
    (:isAnonymous = true and b.visibility = 'PUBLIC' and a.visibility = 'PUBLIC')
    or (:isAnonymous = false and :isManagerOrAdmin = true)
    or (
      :isAnonymous = false
      and :isManagerOrAdmin = false
      and (bm.board_manager_id is null or bm.board_role <> 'BANNED')
      and (
        (b.visibility = 'PUBLIC' and a.visibility in ('PUBLIC', 'MEMBERS'))
        or (b.visibility = 'PUBLIC' and bm.board_role in ('OWNER', 'MODERATOR') and a.visibility = 'MODERATORS')
        or (
          b.visibility = 'GROUP'
          and (
            a.visibility = 'PUBLIC'
            or (a.visibility = 'MEMBERS' and bm.board_role in ('OWNER', 'MODERATOR', 'MEMBER'))
            or (a.visibility = 'MODERATORS' and bm.board_role in ('OWNER', 'MODERATOR'))
          )
        )
        or (
          b.visibility = 'PRIVATE'
          and bm.board_role = 'OWNER'
          and a.visibility in ('PUBLIC', 'MEMBERS', 'MODERATORS')
        )
      )
    )
  )
  and c.content ilike :pattern
  and (:hasExcludeIds = false or c.comment_id not in (:excludeIds))
order by coalesce(extensions.similarity(c.content, :keyword), 0) desc,
  case when :isOldest = true then c.created_at end asc,
  case when :isOldest = true then c.comment_id end asc,
  case when :isOldest = false then c.created_at end desc,
  case when :isOldest = false then c.comment_id end desc
offset :offset rows fetch first :limit rows only
