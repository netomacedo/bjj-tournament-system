-- ============================================================
-- Supabase Advisor: "RLS Disabled in Public" (CRITICAL) on
-- organizations and flyway_schema_history.
--
-- Same fix and reasoning as V2: this app never uses Supabase's
-- PostgREST API, so enabling RLS with no policies just blocks
-- the anon/authenticated API roles from touching these tables,
-- while the app's own direct JDBC connection (as table owner)
-- is unaffected. organizations was added in V4, after V2's pass,
-- so it was missed; flyway_schema_history was never covered.
-- ============================================================

ALTER TABLE public.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.flyway_schema_history ENABLE ROW LEVEL SECURITY;
