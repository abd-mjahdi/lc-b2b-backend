-- Add is_deactivated column to users table to support admin activation/deactivation
ALTER TABLE app.users ADD COLUMN is_deactivated BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN app.users.is_deactivated IS 'Flag indicating if the user has been deactivated (soft-deleted) by an administrator.';
