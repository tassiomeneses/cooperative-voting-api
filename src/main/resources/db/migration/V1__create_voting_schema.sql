CREATE TABLE agendas (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE voting_sessions (
    id UUID PRIMARY KEY,
    agenda_id UUID NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_voting_sessions_agenda
        FOREIGN KEY (agenda_id)
        REFERENCES agendas (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_voting_sessions_agenda_id UNIQUE (agenda_id),
    CONSTRAINT ck_voting_sessions_period CHECK (closed_at > opened_at)
);

CREATE TABLE votes (
    id UUID PRIMARY KEY,
    agenda_id UUID NOT NULL,
    associate_id VARCHAR(64) NOT NULL,
    choice VARCHAR(3) NOT NULL,
    cast_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_votes_agenda
        FOREIGN KEY (agenda_id)
        REFERENCES agendas (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_votes_agenda_associate UNIQUE (agenda_id, associate_id),
    CONSTRAINT ck_votes_choice CHECK (choice IN ('YES', 'NO'))
);

CREATE INDEX idx_votes_agenda_choice
    ON votes (agenda_id, choice);
