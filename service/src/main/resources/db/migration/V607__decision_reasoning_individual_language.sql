-- Individual reasonings are language-specific collections rather than translations of each
-- other: Finnish and Swedish organisations maintain different sets of reasons.

CREATE TEMPORARY TABLE reasoning_split AS
SELECT id AS fi_id, ext.uuid_generate_v1mc() AS sv_id, collection_type,
       title_sv, text_sv, removed_at, created_at, modified_at
FROM decision_reasoning_individual
WHERE btrim(title_sv) <> '' AND btrim(text_sv) <> '';

ALTER TABLE decision_reasoning_individual
    ADD COLUMN language official_language,
    ADD COLUMN title text,
    ADD COLUMN text text;

-- Existing rows keep their id as the Finnish variant so existing selections stay valid.
UPDATE decision_reasoning_individual
SET language = 'FI', title = title_fi, text = text_fi;

ALTER TABLE decision_reasoning_individual
    DROP COLUMN title_fi,
    DROP COLUMN title_sv,
    DROP COLUMN text_fi,
    DROP COLUMN text_sv;

INSERT INTO decision_reasoning_individual
    (id, collection_type, language, title, text, removed_at, created_at, modified_at)
SELECT sv_id, collection_type, 'SV', title_sv, text_sv, removed_at, created_at, modified_at
FROM reasoning_split;

-- Decisions made to Swedish-language units now point at the Swedish row.
UPDATE decision_reasoning_individual_selection sel
SET reasoning_id = s.sv_id
FROM reasoning_split s, decision d, daycare u
WHERE sel.reasoning_id = s.fi_id
  AND d.id = sel.decision_id
  AND u.id = d.unit_id
  AND u.language = 'sv';

DROP TABLE reasoning_split;

ALTER TABLE decision_reasoning_individual
    ALTER COLUMN language SET NOT NULL,
    ALTER COLUMN title SET NOT NULL,
    ALTER COLUMN text SET NOT NULL;
