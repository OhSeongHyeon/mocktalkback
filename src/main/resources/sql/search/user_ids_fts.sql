select u.user_id
from tb_users u
where u.deleted_at is null
  and u.search_vector @@ plainto_tsquery('simple', :keyword)
order by ts_rank(u.search_vector, plainto_tsquery('simple', :keyword)) desc,
  case when :isOldest = true then u.created_at end asc,
  case when :isOldest = true then u.user_id end asc,
  case when :isOldest = false then u.created_at end desc,
  case when :isOldest = false then u.user_id end desc
offset :offset rows fetch first :limit rows only
