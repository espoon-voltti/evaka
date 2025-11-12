UPDATE child_document_decision
SET annulment_reason = 'Mitätöinnin syy ei tiedossa / Orsak till ogiltigförklaring okänd'
WHERE status = 'ANNULLED' AND annulment_reason = '';

ALTER TABLE child_document_decision
ADD CONSTRAINT check$annulment_reason CHECK (
    CASE status
        WHEN 'ANNULLED' THEN annulment_reason <> ''
        ELSE annulment_reason = ''
    END
);