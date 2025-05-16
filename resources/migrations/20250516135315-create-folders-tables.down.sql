-- Drop junction table first due to foreign key constraints
DROP TABLE IF EXISTS folder_question_sets;

-- Drop folders table
DROP TABLE IF EXISTS folders;

-- Drop trigger (optional, as it's associated with the table)
DROP TRIGGER IF EXISTS trigger_folders_updated_at; 