-- V8: expand shared admin broadcast notifications into one row per admin
-- so is_read is isolated per administrator.

-- Duplicate each shared ADMIN broadcast (recipient_user_id IS NULL) for every admin.
INSERT INTO app.notifications (
    recipient_user_id,
    recipient_role,
    title,
    message,
    type,
    related_entity_type,
    related_entity_id,
    target_url,
    is_read,
    created_at
)
SELECT
    u.id,
    n.recipient_role,
    n.title,
    n.message,
    n.type,
    n.related_entity_type,
    n.related_entity_id,
    n.target_url,
    false,
    n.created_at
FROM app.notifications n
CROSS JOIN app.users u
WHERE n.recipient_role = 'ADMIN'
  AND n.recipient_user_id IS NULL
  AND u.role = 'ADMIN';

-- Drop the shared rows (is_read was global).
DELETE FROM app.notifications
WHERE recipient_role = 'ADMIN'
  AND recipient_user_id IS NULL;

COMMENT ON COLUMN app.notifications.recipient_user_id IS
    'Always set for new notifications. Admin broadcasts are one row per admin user.';
