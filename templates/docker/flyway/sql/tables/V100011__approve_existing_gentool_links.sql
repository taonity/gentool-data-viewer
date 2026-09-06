UPDATE gentool_user_link
SET status = 'APPROVED',
    decided_at = COALESCE(decided_at, requested_at),
    decided_by_user_id = COALESCE(decided_by_user_id, user_id)
WHERE status <> 'APPROVED';