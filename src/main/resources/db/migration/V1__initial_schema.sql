CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE pets (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(30) NOT NULL,
    experience INTEGER NOT NULL DEFAULT 0 CHECK (experience >= 0),
    meals INTEGER NOT NULL DEFAULT 0 CHECK (meals >= 0),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE deadlines (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    due_at TIMESTAMPTZ NOT NULL,
    difficulty VARCHAR(10) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    status VARCHAR(10) NOT NULL CHECK (status IN ('OPEN', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT deadline_completion_consistent CHECK (
        (status = 'OPEN' AND completed_at IS NULL) OR
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
    )
);

CREATE INDEX deadlines_user_status_idx ON deadlines(user_id, status);
CREATE INDEX deadlines_user_due_idx ON deadlines(user_id, due_at);
