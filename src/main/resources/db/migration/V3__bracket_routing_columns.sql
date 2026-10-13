-- ============================================================
-- Adds explicit winner-routing columns to matches, needed for the
-- cascading-bye single-elimination bracket shape: a winner who
-- isn't immediately paired in the very next round becomes that
-- round's bye-carryover and lands two rounds ahead instead of one,
-- so routing can no longer always be derived as "roundNumber + 1".
--
-- Nullable and additive only - existing matches (generated before
-- this column existed) are left as NULL, and advanceWinnerToNextRound
-- falls back to the old parity-based computation whenever these are
-- unset, so in-progress brackets generated under the old shape keep
-- working unchanged.
-- ============================================================

ALTER TABLE public.matches ADD COLUMN advances_to_round INTEGER;
ALTER TABLE public.matches ADD COLUMN advances_to_position INTEGER;
ALTER TABLE public.matches ADD COLUMN advances_to_slot VARCHAR(10);
