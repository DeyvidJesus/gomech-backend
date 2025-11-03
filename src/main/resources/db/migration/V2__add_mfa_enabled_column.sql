-- Ensure required columns exist on the users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS name VARCHAR(255);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(50);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(512);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN;

-- Populate role column from legacy role relationship when available
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND column_name = 'role_id'
    ) THEN
        UPDATE users u
        SET role = r.nome
        FROM roles r
        WHERE u.role IS NULL
          AND r.id = u.role_id;
    END IF;
END $$;

-- Backfill defaults for null or blank values
UPDATE users
SET name = COALESCE(NULLIF(name, ''), email, 'User'),
    role = COALESCE(NULLIF(role, ''), 'USER')
WHERE name IS NULL
   OR name = ''
   OR role IS NULL
   OR role = '';

UPDATE users
SET mfa_enabled = FALSE
WHERE mfa_enabled IS NULL;

-- Enforce constraints expected by the application
ALTER TABLE users
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN mfa_enabled SET DEFAULT FALSE,
    ALTER COLUMN mfa_enabled SET NOT NULL;

-- Clean up legacy relationship columns
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND column_name = 'role_id'
    ) THEN
        ALTER TABLE users
            DROP CONSTRAINT IF EXISTS fk_users_role;

        ALTER TABLE users
            DROP COLUMN IF EXISTS role_id;
    END IF;
END $$;
