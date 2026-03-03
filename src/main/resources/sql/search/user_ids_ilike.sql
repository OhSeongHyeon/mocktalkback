select u.user_id
from tb_users u
where u.deleted_at is null
  and (
    u.handle ilike :pattern
    or u.display_name ilike :pattern
    or u.user_name ilike :pattern
  )
  and (:hasExcludeIds = false or u.user_id not in (:excludeIds))
order by greatest(
    coalesce(extensions.similarity(u.handle, :keyword), 0),
    coalesce(extensions.similarity(u.display_name, :keyword), 0),
    coalesce(extensions.similarity(u.user_name, :keyword), 0)
  ) desc,
  case when :isOldest = true then u.created_at end asc,
  case when :isOldest = true then u.user_id end asc,
  case when :isOldest = false then u.created_at end desc,
  case when :isOldest = false then u.user_id end desc
offset :offset rows fetch first :limit rows only
