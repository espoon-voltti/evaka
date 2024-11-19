ALTER TABLE holiday_period_questionnaire
    ADD COLUMN period daterange,
    ADD COLUMN absence_type_threshold integer;

ALTER TABLE  holiday_questionnaire_answer
    ADD COLUMN open_ranges daterange[]
