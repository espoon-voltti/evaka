ALTER TABLE assistance_need_decision ADD COLUMN assistance_levels text[] DEFAULT array[]::text[];
UPDATE assistance_need_decision SET assistance_levels = array[assistance_level] WHERE assistance_level IS NOT NULL;
ALTER TABLE assistance_need_decision DROP COLUMN assistance_level;
