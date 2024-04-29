CREATE TYPE official_language AS ENUM ('FI', 'SV');

ALTER TABLE assistance_need_decision
    ALTER COLUMN language SET DATA TYPE official_language USING language::text::official_language;

ALTER TABLE assistance_need_preschool_decision
    ALTER COLUMN language DROP DEFAULT,
    ALTER COLUMN language SET DATA TYPE official_language USING language::text::official_language,
    ALTER COLUMN language SET DEFAULT 'FI';

ALTER TABLE curriculum_template
    ALTER COLUMN language SET DATA TYPE official_language USING language::text::official_language;

ALTER TABLE document_template
    ALTER COLUMN language SET DATA TYPE official_language USING language::text::official_language;

DROP TYPE curriculum_language;
DROP TYPE document_language;
