-- Fix legacy data which used 9999-12-31 instead of NULL for end_dates
UPDATE fridge_partner
SET end_date = NULL
WHERE end_date >= '3000-01-01';
