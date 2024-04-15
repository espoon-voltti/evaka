ALTER TABLE service_need_option
    ADD COLUMN valid_from date,
    ADD COLUMN valid_to date;

ALTER TABLE service_need_option
    ADD CONSTRAINT start_before_end CHECK ( valid_to IS NULL OR valid_to >= valid_from );

-- arbitrary start date far in the past
UPDATE service_need_option SET valid_from = '2000-01-01'::date;
ALTER TABLE service_need_option ALTER COLUMN valid_from SET NOT NULL;

-- arbitrary end date for terminated ones, can be manually corrected per installation
UPDATE service_need_option SET valid_to = '2023-12-31'::date WHERE active IS FALSE;

ALTER TABLE service_need_option DROP COLUMN active;
