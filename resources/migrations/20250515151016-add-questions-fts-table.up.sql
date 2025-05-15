  -- resources/migrations/20250515151016-add-questions-fts-table.up.sql

-- Create FTS5 virtual table for searching questions
CREATE VIRTUAL TABLE questions_fts USING fts5(
    question_id UNINDEXED, -- We will join on this, so no need for FTS5 to index it directly for search
    set_id UNINDEXED,      -- For context, not directly searched by FTS5 here
    searchable_text,       -- This is the column FTS5 will index and search
    tokenize = 'porter unicode61' -- Use porter stemmer for better matching
);

--;;

-- Triggers to keep FTS table in sync with questions table.
-- These will handle INSERT, UPDATE, DELETE on the 'questions' table.
-- NOTE: The actual content for 'searchable_text' will be populated/updated
-- by application logic, as parsing EDN in SQL triggers is not feasible.
-- The triggers here will primarily handle row creation/deletion in questions_fts
-- and ensure question_id and set_id are present. The 'searchable_text'
-- will be initially NULL or empty, and updated by the app.

-- After a new question is inserted, create a corresponding FTS entry.
-- searchable_text will be populated by the application later.
CREATE TRIGGER questions_ai AFTER INSERT ON questions BEGIN
    INSERT INTO questions_fts (question_id, set_id, searchable_text)
    VALUES (new.question_id, new.set_id, ''); -- Initially empty, app will update
END;

--;;

-- Before a question is updated, we could delete the old FTS entry and re-insert,
-- or update. Updating is generally better.
-- For simplicity here, we'll delete and rely on an application-level update
-- to re-populate/update the searchable_text.
-- A more robust solution might involve an UPDATE trigger that calls a user-defined function
-- if the database supported it easily for EDN parsing, but since it doesn't,
-- the app handles the 'searchable_text' content.

-- When a question is deleted, remove its FTS entry.
CREATE TRIGGER questions_ad AFTER DELETE ON questions BEGIN
    DELETE FROM questions_fts WHERE question_id = old.question_id;
END;

--;;

-- After a question's main content is updated, the application
-- will be responsible for updating the corresponding questions_fts.searchable_text.
-- However, if set_id changes (unlikely for an existing question, but possible),
-- this trigger ensures the FTS table reflects it.
-- We'll also clear searchable_text as it would need re-processing by the app.
CREATE TRIGGER questions_au AFTER UPDATE OF question_data, retention_aid, set_id ON questions BEGIN
    UPDATE questions_fts
    SET set_id = new.set_id,
        searchable_text = '' -- Mark for app to re-process
    WHERE question_id = new.question_id;
END; 