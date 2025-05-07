ALTER TABLE system_notification
    ADD COLUMN text_sv text CHECK ( text_sv <> '' ),
    ADD COLUMN text_en text CHECK ( text_en <> '' );

UPDATE system_notification
SET text_sv = text, text_en = text
WHERE target_group = 'CITIZENS';

ALTER TABLE system_notification ADD CONSTRAINT notification_i18n
    CHECK (
        CASE
            WHEN target_group = 'CITIZENS'
                THEN (text_sv IS NOT NULL AND text_en IS NOT NULL)
            WHEN target_group = 'EMPLOYEES'
                THEN (text_sv IS NULL AND text_en IS NULL)
        END
    );
