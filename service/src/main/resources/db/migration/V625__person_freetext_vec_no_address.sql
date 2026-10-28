ALTER TABLE person ADD COLUMN freetext_vec_no_address tsvector GENERATED ALWAYS AS (
    to_tsvector('unaccent', coalesce(first_name, '')) ||
    to_tsvector('unaccent', coalesce(last_name, ''))
) STORED;
