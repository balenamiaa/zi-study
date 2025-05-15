-- resources/migrations/20250515151016-add-questions-fts-table.down.sql

-- Drop the triggers first
DROP TRIGGER IF EXISTS questions_au;

--;;

DROP TRIGGER IF EXISTS questions_ad;

--;;

DROP TRIGGER IF EXISTS questions_ai;

--;;

-- Then drop the FTS table
DROP TABLE IF EXISTS questions_fts;
