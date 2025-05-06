-- resources/migrations/20250430201000-create-question-bank-tables.up.sql

-- Question Sets Table
CREATE TABLE question_sets (
    set_id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP -- Consider adding triggers for automatic update
);

--;;

-- Trigger to update updated_at on question_sets update
CREATE TRIGGER question_sets_updated_at
AFTER UPDATE ON question_sets
FOR EACH ROW
BEGIN
    UPDATE question_sets SET updated_at = CURRENT_TIMESTAMP WHERE set_id = OLD.set_id;
END;

--;;

-- Tags Table
CREATE TABLE tags (
    tag_id INTEGER PRIMARY KEY AUTOINCREMENT,
    tag_name TEXT NOT NULL UNIQUE
);

--;;

-- Junction Table for Sets and Tags (Many-to-Many)
CREATE TABLE question_set_tags (
    set_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (set_id, tag_id),
    FOREIGN KEY (set_id) REFERENCES question_sets(set_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

--;;

-- Questions Table
CREATE TABLE questions (
    question_id INTEGER PRIMARY KEY AUTOINCREMENT,
    set_id INTEGER NOT NULL,
    question_type TEXT NOT NULL CHECK (question_type IN ('written', 'mcq-single', 'mcq-multi', 'emq', 'cloze', 'true-false')),
    difficulty INTEGER CHECK (difficulty BETWEEN 1 AND 5),
    question_data TEXT NOT NULL, -- Store as EDN or JSON string
    retention_aid TEXT,
    order_in_set INTEGER, -- For maintaining order within a set
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, -- Consider adding triggers
    FOREIGN KEY (set_id) REFERENCES question_sets(set_id) ON DELETE CASCADE
);

--;;

-- Trigger to update updated_at on questions update
CREATE TRIGGER questions_updated_at
AFTER UPDATE ON questions
FOR EACH ROW
BEGIN
    UPDATE questions SET updated_at = CURRENT_TIMESTAMP WHERE question_id = OLD.question_id;
END;

--;;

-- Index for faster question lookup by set
CREATE INDEX idx_questions_set_id ON questions(set_id);

--;;

-- User Answers Table
-- Assuming 'users' table exists with 'user_id' INTEGER PRIMARY KEY
CREATE TABLE user_answers (
    answer_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    answer_data TEXT NOT NULL, -- Store user's answer as EDN or JSON string
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_correct INTEGER, -- 1 for true, 0 for false, NULL for pending self-eval
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
    UNIQUE (user_id, question_id) -- Ensures only the latest answer state per user/question
);

--;;

-- Index for faster lookup of user answers
CREATE INDEX idx_user_answers_user_question ON user_answers(user_id, question_id);

--;;

-- User Bookmarks Table
-- Assuming 'users' table exists with 'user_id' INTEGER PRIMARY KEY
CREATE TABLE user_bookmarks (
    user_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    bookmarked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, question_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
);

--;;

-- Index for faster lookup of bookmarks by user
CREATE INDEX idx_user_bookmarks_user_id ON user_bookmarks(user_id); 