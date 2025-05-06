-- resources/migrations/20250430201000-create-question-bank-tables.down.sql

DROP INDEX IF EXISTS idx_user_bookmarks_user_id;
--;;
DROP TABLE IF EXISTS user_bookmarks;
--;;
DROP INDEX IF EXISTS idx_user_answers_user_question;
--;;
DROP TABLE IF EXISTS user_answers;
--;;
DROP INDEX IF EXISTS idx_questions_set_id;
--;;
DROP TRIGGER IF EXISTS questions_updated_at;
--;;
DROP TABLE IF EXISTS questions;
--;;
DROP TABLE IF EXISTS question_set_tags;
--;;
DROP TABLE IF EXISTS tags;
--;;
DROP TRIGGER IF EXISTS question_sets_updated_at;
--;;
DROP TABLE IF EXISTS question_sets; 