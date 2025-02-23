ALTER TABLE document_template
    ADD COLUMN confidentiality_duration_years smallint,
    ADD COLUMN confidentiality_basis text;

UPDATE document_template
SET confidential = TRUE,
    confidentiality_duration_years = 100,
    confidentiality_basis = 'Varhaiskasvatuslaki 40 § 3 mom.'
WHERE type IN ('VASU', 'MIGRATED_VASU');

UPDATE document_template
SET confidential = TRUE,
    confidentiality_duration_years = 100,
    confidentiality_basis = 'JulkL 24.1 §'
WHERE type IN ('LEOPS', 'MIGRATED_LEOPS', 'HOJKS', 'PEDAGOGICAL_ASSESSMENT', 'PEDAGOGICAL_REPORT');

UPDATE document_template
SET confidentiality_duration_years = 100,
    confidentiality_basis = 'Ei asetettu'
WHERE type = 'OTHER' AND confidential = TRUE;

ALTER TABLE document_template
ADD CONSTRAINT document_template_confidentiality_check CHECK (
    confidential = FALSE OR (confidentiality_duration_years IS NOT NULL AND confidentiality_basis IS NOT NULL)
);
