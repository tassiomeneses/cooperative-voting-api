ALTER TABLE votes
    ADD COLUMN voter_key VARCHAR(64);

UPDATE votes
SET voter_key = md5(associate_id)
WHERE voter_key IS NULL;

ALTER TABLE votes
    ALTER COLUMN voter_key SET NOT NULL;

ALTER TABLE votes
    ADD CONSTRAINT uk_votes_agenda_voter_key UNIQUE (agenda_id, voter_key);
