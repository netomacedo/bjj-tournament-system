-- ============================================================
-- Multi-tenancy, stage B: bootstraps the very first platform-admin
-- account. Self-registration is disabled and there's no startup
-- seeder in this app, so this one-time insert is the only way to
-- get the first platform admin in - every organization created
-- after this goes through POST /api/platform/organizations instead.
--
-- Password is bcrypt-hashed below, never stored as plaintext - log
-- in with the credentials you were given out-of-band, then change
-- it immediately via POST /api/auth/change-password.
--
-- organization_id is NULL - a platform admin isn't tied to any
-- single organization, matching User.organizationId's documented
-- "null only for a platform admin" contract.
--
-- The existing gym's current admin account is left completely
-- untouched by this migration - they remain the org-admin for the
-- one real organization, exactly as before.
-- ============================================================

INSERT INTO users (username, email, password, full_name, role, enabled, account_non_locked, organization_id, created_at, updated_at)
VALUES (
    'bjjopsadmin',
    'bjjopsadmin@platform.local',
    '$2a$10$q1r5xo5C.p4MUl/nbwCI6uju.b2DRw173FxtqntA9G8g6uTJrWuZa',
    'Platform Admin',
    'ROLE_PLATFORM_ADMIN',
    true,
    true,
    NULL,
    now(),
    now()
);
