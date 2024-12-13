ALTER TABLE holiday_period_questionnaire
    ADD COLUMN period daterange,
    ADD COLUMN absence_type_threshold integer;

ALTER TABLE holiday_period_questionnaire
    ADD CONSTRAINT period_null_or_valid
        CHECK ((period IS NULL OR NOT (lower_inf(period) OR upper_inf(period))));

ALTER TABLE  holiday_questionnaire_answer
    ADD COLUMN open_ranges daterange[]
