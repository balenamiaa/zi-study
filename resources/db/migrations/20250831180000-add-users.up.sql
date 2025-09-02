CREATE TABLE IF NOT EXISTS app_user (
  id SERIAL PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT,
  name TEXT,
  provider TEXT,
  provider_id TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);
