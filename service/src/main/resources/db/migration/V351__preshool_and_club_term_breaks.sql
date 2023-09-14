ALTER TABLE preschool_term ADD COLUMN term_breaks datemultirange NOT NULL DEFAULT '{}';
ALTER TABLE preschool_term ALTER COLUMN term_breaks DROP DEFAULT;
ALTER TABLE preschool_term
    ADD CONSTRAINT check$finnish_preschool_valid CHECK (NOT (lower_inf(finnish_preschool) OR upper_inf(finnish_preschool))),
    ADD CONSTRAINT check$swedish_preschool_valid CHECK (NOT (lower_inf(swedish_preschool) OR upper_inf(swedish_preschool))),
    ADD CONSTRAINT check$extended_term_valid CHECK (NOT (lower_inf(extended_term) OR upper_inf(extended_term))),
    ADD CONSTRAINT check$application_period_valid CHECK (NOT (lower_inf(application_period) OR upper_inf(application_period))),
    ADD CONSTRAINT check$term_breaks_valid CHECK (NOT (lower_inf(term_breaks) OR upper_inf(term_breaks))),
    ADD CONSTRAINT check$finnish_preschool_contains_term_breaks CHECK (finnish_preschool @> term_breaks);

ALTER TABLE club_term ADD COLUMN term_breaks datemultirange NOT NULL DEFAULT '{}';
ALTER TABLE club_term ALTER COLUMN term_breaks DROP DEFAULT;
ALTER TABLE club_term
    ADD CONSTRAINT check$term_valid CHECK (NOT (lower_inf(term) OR upper_inf(term))),
    ADD CONSTRAINT check$application_period_valid CHECK (NOT (lower_inf(application_period) OR upper_inf(application_period))),
    ADD CONSTRAINT check$term_breaks_valid CHECK (NOT (lower_inf(term_breaks) OR upper_inf(term_breaks))),
    ADD CONSTRAINT check$term_contains_term_breaks CHECK (term @> term_breaks);
