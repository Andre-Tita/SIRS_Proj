-- Create Users Table
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    public_key VARCHAR(255) NOT NULL
);

-- Create Notes Table
CREATE TABLE notes (
    note_id SERIAL PRIMARY KEY,
    owner_id INT REFERENCES users(user_id),
    version INT,
    title VARCHAR(255),
    content TEXT,
    is_encrypted BOOLEAN DEFAULT TRUE,                  -- # Delete on final delievery (everything will be encrypted)
    created_at TIMESTAMP
);

-- Create Access Logs Table, so we know which user has access to which note and what type of access
CREATE TABLE access_logs (
    log_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id),
    note_id INT REFERENCES notes(note_id),
    owner_id INT,
    user_role VARCHAR(50) NOT NULL,
    action_timestamp TIMESTAMP
);

-- Encryption Keys Table (Optional) ??
CREATE TABLE encryption_keys (
    key_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id),
    key_value BYTEA,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
