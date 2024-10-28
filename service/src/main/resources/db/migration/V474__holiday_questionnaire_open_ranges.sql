ALTER TABLE holiday_period_questionnaire
    ADD COLUMN period daterange,
    ADD COLUMN absence_type_threshold integer;
