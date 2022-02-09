ALTER TABLE holiday_period
    ADD COLUMN free_period_deadline            date,
    ADD COLUMN free_period_question_label      jsonb,
    ADD COLUMN free_period_period_options      daterange[],
    ADD COLUMN free_period_period_option_label jsonb;
