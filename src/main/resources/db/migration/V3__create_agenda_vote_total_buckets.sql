CREATE TABLE agenda_vote_total_buckets (
    agenda_id UUID NOT NULL,
    bucket SMALLINT NOT NULL,
    yes_votes BIGINT NOT NULL DEFAULT 0,
    no_votes BIGINT NOT NULL DEFAULT 0,
    total_votes BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_agenda_vote_total_buckets
        PRIMARY KEY (agenda_id, bucket),
    CONSTRAINT fk_agenda_vote_total_buckets_agenda
        FOREIGN KEY (agenda_id)
        REFERENCES agendas (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_agenda_vote_total_buckets_range
        CHECK (bucket >= 0 AND bucket < 64),
    CONSTRAINT ck_agenda_vote_total_buckets_non_negative
        CHECK (yes_votes >= 0 AND no_votes >= 0 AND total_votes >= 0),
    CONSTRAINT ck_agenda_vote_total_buckets_consistent
        CHECK (total_votes = yes_votes + no_votes)
);

INSERT INTO agenda_vote_total_buckets (agenda_id, bucket, updated_at)
SELECT agenda.id, bucket.value, agenda.created_at
FROM agendas agenda
CROSS JOIN generate_series(0, 63) AS bucket(value)
ON CONFLICT (agenda_id, bucket) DO NOTHING;

INSERT INTO agenda_vote_total_buckets (
    agenda_id,
    bucket,
    yes_votes,
    no_votes,
    total_votes,
    updated_at
)
SELECT
    vote.agenda_id,
    ((hashtext(vote.voter_key) % 64 + 64) % 64)::smallint AS bucket,
    SUM(CASE WHEN vote.choice = 'YES' THEN 1 ELSE 0 END),
    SUM(CASE WHEN vote.choice = 'NO' THEN 1 ELSE 0 END),
    COUNT(vote.id),
    MAX(vote.cast_at)
FROM votes vote
GROUP BY vote.agenda_id, ((hashtext(vote.voter_key) % 64 + 64) % 64)::smallint
ON CONFLICT (agenda_id, bucket) DO UPDATE
SET
    yes_votes = EXCLUDED.yes_votes,
    no_votes = EXCLUDED.no_votes,
    total_votes = EXCLUDED.total_votes,
    updated_at = EXCLUDED.updated_at;

CREATE OR REPLACE FUNCTION create_agenda_vote_total_buckets()
RETURNS trigger AS $$
BEGIN
    INSERT INTO agenda_vote_total_buckets (agenda_id, bucket, updated_at)
    SELECT NEW.id, bucket.value, NEW.created_at
    FROM generate_series(0, 63) AS bucket(value)
    ON CONFLICT (agenda_id, bucket) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_agendas_create_vote_total_buckets
AFTER INSERT ON agendas
FOR EACH ROW
EXECUTE FUNCTION create_agenda_vote_total_buckets();

CREATE OR REPLACE FUNCTION increment_agenda_vote_total_bucket()
RETURNS trigger AS $$
DECLARE
    vote_bucket SMALLINT;
BEGIN
    vote_bucket := ((hashtext(NEW.voter_key) % 64 + 64) % 64)::smallint;

    INSERT INTO agenda_vote_total_buckets (
        agenda_id,
        bucket,
        yes_votes,
        no_votes,
        total_votes,
        updated_at
    )
    VALUES (
        NEW.agenda_id,
        vote_bucket,
        CASE WHEN NEW.choice = 'YES' THEN 1 ELSE 0 END,
        CASE WHEN NEW.choice = 'NO' THEN 1 ELSE 0 END,
        1,
        NEW.cast_at
    )
    ON CONFLICT (agenda_id, bucket) DO UPDATE
    SET
        yes_votes = agenda_vote_total_buckets.yes_votes + CASE WHEN NEW.choice = 'YES' THEN 1 ELSE 0 END,
        no_votes = agenda_vote_total_buckets.no_votes + CASE WHEN NEW.choice = 'NO' THEN 1 ELSE 0 END,
        total_votes = agenda_vote_total_buckets.total_votes + 1,
        updated_at = GREATEST(agenda_vote_total_buckets.updated_at, NEW.cast_at);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_votes_increment_agenda_vote_total_bucket
AFTER INSERT ON votes
FOR EACH ROW
EXECUTE FUNCTION increment_agenda_vote_total_bucket();
