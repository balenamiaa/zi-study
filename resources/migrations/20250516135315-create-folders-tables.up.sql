-- Folders table to store user-created folders for organizing question sets
CREATE TABLE IF NOT EXISTS folders (
    folder_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

--;;

-- Trigger to update 'updated_at' timestamp on folders table update
CREATE TRIGGER IF NOT EXISTS trigger_folders_updated_at
AFTER UPDATE ON folders
FOR EACH ROW
BEGIN
    UPDATE folders SET updated_at = CURRENT_TIMESTAMP WHERE folder_id = OLD.folder_id;
END;

--;;

-- Junction table to link folders and question sets
-- This allows a question set to be in multiple folders and defines the order within a folder
CREATE TABLE IF NOT EXISTS folder_question_sets (
    folder_question_set_id INTEGER PRIMARY KEY AUTOINCREMENT,
    folder_id INTEGER NOT NULL,
    set_id INTEGER NOT NULL,
    order_in_folder INTEGER NOT NULL DEFAULT 0, -- Used for ordering sets within a folder
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (folder_id) REFERENCES folders(folder_id) ON DELETE CASCADE,
    FOREIGN KEY (set_id) REFERENCES question_sets(set_id) ON DELETE CASCADE,
    UNIQUE (folder_id, set_id) -- A set can only appear once in a specific folder
);

--;;

-- Indexes for faster querying
CREATE INDEX IF NOT EXISTS idx_folders_user_id ON folders(user_id);
--;;
CREATE INDEX IF NOT EXISTS idx_folders_is_public ON folders(is_public);
--;;
CREATE INDEX IF NOT EXISTS idx_folder_question_sets_folder_id ON folder_question_sets(folder_id);
--;;
CREATE INDEX IF NOT EXISTS idx_folder_question_sets_set_id ON folder_question_sets(set_id); 