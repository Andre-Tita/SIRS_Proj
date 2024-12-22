DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS notes CASCADE;
DROP TABLE IF EXISTS note_versions CASCADE;
DROP TABLE IF EXISTS access_logs CASCADE;
DROP TABLE IF EXISTS encryption_keys CASCADE;

-- Create Users Table
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    public_key VARCHAR(255) NOT NULL,
    is_loggedin BOOLEAN
);

-- Notes Table (metadata)
CREATE TABLE notes (
    note_id SERIAL PRIMARY KEY,
    owner_id INT REFERENCES users(user_id),
    title VARCHAR(255) NOT NULL UNIQUE,
    data_created TIMESTAMP
);

-- Note Versions Table (tracks versions)
CREATE TABLE note_versions (
    version_id SERIAL PRIMARY KEY,
    note_id INT REFERENCES notes(note_id) ON DELETE CASCADE,
    version INT NOT NULL,
    content TEXT,
    data_created TIMESTAMP,
    modified_at TIMESTAMP,
    modified_by INT NOT NULL,
    UNIQUE(note_id, version) -- Prevent duplicate versions
);

-- Create Access Logs Table, so we know which user has access to which note and what type of access
CREATE TABLE access_logs (
    log_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id),
    note_id INT REFERENCES notes(note_id),
    owner_id INT,
    user_role VARCHAR(50) NOT NULL,
    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Encryption Keys Table (Optional) ??
CREATE TABLE encryption_keys (
    key_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id),
    key_value BYTEA,
    data_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
