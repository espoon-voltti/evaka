CREATE TYPE ui_language AS ENUM ('FI', 'SV', 'EN');
ALTER TABLE document_template
    ALTER COLUMN language SET DATA TYPE ui_language USING language::text::ui_language;
