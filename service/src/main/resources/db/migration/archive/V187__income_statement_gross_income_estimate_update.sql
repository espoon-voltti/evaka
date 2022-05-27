UPDATE income_statement
SET gross_estimated_monthly_income = 0
WHERE
    type = 'INCOME' AND
    gross_income_source IS NOT NULL AND
    gross_estimated_monthly_income IS NULL;
