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
  and b.search_vector @@ plainto_tsquery('simple', :keyword)
order by ts_rank(b.search_vector, plainto_tsquery('simple', :keyword)) desc,
  case when :isOldest = true then b.created_at end asc,
  case when :isOldest = true then b.updated_at end asc,
  case when :isOldest = true then b.board_id end asc,
  case when :isOldest = false then b.created_at end desc,
  case when :isOldest = false then b.updated_at end desc,
  case when :isOldest = false then b.board_id end desc
offset :offset rows fetch first :limit rows only
