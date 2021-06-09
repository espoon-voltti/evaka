create text search configuration unaccent (copy = pg_catalog.simple);

-- Remove accents from words
alter text search configuration unaccent
    alter mapping for hword, hword_part, word with unaccent, simple;

alter table person add column freetext_vec tsvector generated always as (
    to_tsvector('unaccent', coalesce(first_name, '')) ||
    to_tsvector('unaccent', coalesce(last_name, '')) ||
    to_tsvector('unaccent', street_address) ||
    to_tsvector('unaccent', postal_code)
) stored;

create index person_freetext on person using gin(freetext_vec);
