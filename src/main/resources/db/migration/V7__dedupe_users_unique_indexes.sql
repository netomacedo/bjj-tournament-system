-- ============================================================
-- Supabase Advisor: "Duplicate Index" on public.users
-- ({idx_user_email, uk_6dotkott2kjsp8vw4d0m25fb7}, and the same
-- pattern for username).
--
-- Prod's users table predates Flyway (built by Hibernate auto-DDL
-- before this project had migrations), so it has its own
-- Hibernate-generated unique constraint names (e.g.
-- uk_6dotkott2kjsp8vw4d0m25fb7) in addition to the explicit
-- idx_user_username/idx_user_email indexes from V1's baseline
-- reconstruction - two real, independent objects both enforcing
-- the same uniqueness, not "one index plus the constraint behind
-- it" like V2 assumed. That's why V2's cleanup silently skipped
-- both (it only ever drops a plain index that backs no constraint).
--
-- This is deliberately name-agnostic instead of hardcoding a
-- second guess at prod's object names: for each of
-- username/email, it finds every unique index on just that one
-- column, keeps exactly one - preferring a real named CONSTRAINT
-- over a bare index, since that's what V2 originally intended to
-- end up with and what JPA's @Column(unique = true) mapping
-- actually corresponds to - and drops the rest, via DROP INDEX or
-- ALTER TABLE ... DROP CONSTRAINT depending on which each one
-- actually is. Safe to run anywhere, including environments (like
-- local dev) where V2 already resolved this and only one object
-- remains.
-- ============================================================

DO $$
DECLARE
    col text;
    keep_name text;
    dup RECORD;
    dup_constraint text;
BEGIN
    FOREACH col IN ARRAY ARRAY['username', 'email']
    LOOP
        keep_name := NULL;

        FOR dup IN
            SELECT c.relname AS index_name, c.oid AS index_oid,
                   EXISTS (SELECT 1 FROM pg_constraint k WHERE k.conindid = c.oid) AS is_constraint
            FROM pg_index i
            JOIN pg_class c ON c.oid = i.indexrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            JOIN pg_class t ON t.oid = i.indrelid
            WHERE n.nspname = 'public'
              AND t.relname = 'users'
              AND i.indisunique
              AND (
                  SELECT array_agg(a.attname::text ORDER BY a.attnum)
                  FROM pg_attribute a
                  WHERE a.attrelid = t.oid AND a.attnum = ANY (i.indkey)
              ) = ARRAY[col]
            ORDER BY is_constraint DESC, c.relname
        LOOP
            IF keep_name IS NULL THEN
                keep_name := dup.index_name;
            ELSE
                SELECT conname INTO dup_constraint
                FROM pg_constraint
                WHERE conindid = dup.index_oid;

                IF dup_constraint IS NOT NULL THEN
                    EXECUTE format('ALTER TABLE public.users DROP CONSTRAINT %I', dup_constraint);
                ELSE
                    EXECUTE format('DROP INDEX public.%I', dup.index_name);
                END IF;
            END IF;
        END LOOP;
    END LOOP;
END
$$;
