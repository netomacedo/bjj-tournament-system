-- ============================================================
-- Supabase Advisor fixes: RLS, missing PK, unindexed FKs,
-- duplicate indexes.
--
-- This app never uses Supabase's own REST API (PostgREST) - the
-- frontend only ever talks to this Spring Boot backend, which
-- connects to Postgres directly over JDBC as the table owner.
-- Enabling RLS with no policies blocks PostgREST's anon/
-- authenticated roles from touching these tables at all (closing
-- the "someone queries the DB straight through Supabase's public
-- API" hole) while having zero effect on this app, since table
-- owners always bypass RLS. This also resolves "Sensitive Columns
-- Exposed" on users.password, which becomes unreachable through
-- the now-blocked REST API too.
--
-- Written defensively (IF EXISTS / IF NOT EXISTS throughout) so
-- it's safe to apply whether or not any of this was already
-- applied by hand against the live database before Flyway existed.
-- ============================================================

ALTER TABLE public.tournaments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.divisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.athletes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.division_athletes ENABLE ROW LEVEL SECURITY;


-- No Primary Key: division_athletes already has a UNIQUE
-- constraint on this exact column pair - promoting it to the
-- primary key satisfies the advisor without changing what's
-- actually enforced.
ALTER TABLE public.division_athletes DROP CONSTRAINT IF EXISTS uk_division_athlete;
ALTER TABLE public.division_athletes ADD PRIMARY KEY (division_id, athlete_id);


-- Unindexed foreign keys: matches.athlete1_id/athlete2_id/winner_id
-- had no index, unlike division_id. Pure performance fix.
CREATE INDEX IF NOT EXISTS idx_match_athlete1 ON public.matches (athlete1_id);
CREATE INDEX IF NOT EXISTS idx_match_athlete2 ON public.matches (athlete2_id);
CREATE INDEX IF NOT EXISTS idx_match_winner ON public.matches (winner_id);


-- Duplicate Index: users (username, email) had both a UNIQUE
-- constraint and a separate explicitly-named unique index on the
-- same columns. Dropping the explicitly-named ones leaves the
-- constraint-backed ones in place, so uniqueness is still enforced
-- identically with one index per column instead of two.
--
-- Only drop an index when no constraint depends on it. On databases
-- where Hibernate created the unique constraint itself, the constraint
-- is NAMED idx_user_username / idx_user_email, so that index IS the
-- constraint and Postgres refuses to drop it (it is already the single
-- index per column, which is the goal here).
DO $$
DECLARE
    idx_name text;
BEGIN
    FOREACH idx_name IN ARRAY ARRAY['idx_user_username', 'idx_user_email']
    LOOP
        IF EXISTS (
            SELECT 1
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'public'
              AND c.relname = idx_name
              AND c.relkind = 'i'
              AND NOT EXISTS (
                  SELECT 1 FROM pg_constraint k WHERE k.conindid = c.oid
              )
        ) THEN
            EXECUTE format('DROP INDEX public.%I', idx_name);
        END IF;
    END LOOP;
END
$$;
