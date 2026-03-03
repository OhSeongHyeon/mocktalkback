select b.board_id
from tb_boards b
left join tb_board_members bm on bm.board_id = b.board_id and bm.user_id = :userId
where b.deleted_at is null
  and (
    (:isAnonymous = true and b.visibility = 'PUBLIC')
    or (:isAnonymous = false and :isManagerOrAdmin = true)
    or (
      :isAnonymous = false
      and :isManagerOrAdmin = false
      and (bm.board_manager_id is null or bm.board_role <> 'BANNED')
      and (
        b.visibility in ('PUBLIC', 'GROUP')
        or (b.visibility = 'PRIVATE' and bm.board_role = 'OWNER')
      )
    )
  )
  and (
    b.board_name ilike :pattern
    or b.slug ilike :pattern
    or b.description ilike :pattern
  )
  and (:hasExcludeIds = false or b.board_id not in (:excludeIds))
order by greatest(
    coalesce(extensions.similarity(b.board_name, :keyword), 0),
    coalesce(extensions.similarity(b.slug, :keyword), 0),
    coalesce(extensions.similarity(b.description, :keyword), 0)
  ) desc,
  case when :isOldest = true then b.created_at end asc,
  case when :isOldest = true then b.updated_at end asc,
  case when :isOldest = true then b.board_id end asc,
  case when :isOldest = false then b.created_at end desc,
  case when :isOldest = false then b.updated_at end desc,
  case when :isOldest = false then b.board_id end desc
offset :offset rows fetch first :limit rows only
