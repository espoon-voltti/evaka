-- original migration was missing coalesces
UPDATE application
SET confidential = (
    coalesce((document -> 'careDetails' ->> 'assistanceNeeded')::bool, false) = true OR
    coalesce(document -> 'careDetails' ->> 'assistanceDescription', '') <> '' OR
    coalesce(document -> 'additionalDetails' ->> 'dietType', '') <> '' OR
    coalesce(document -> 'additionalDetails' ->> 'allergyType', '') <> '' OR
    coalesce(document -> 'additionalDetails' ->> 'otherInfo', '') <> ''
)
WHERE status NOT IN ('CREATED', 'SENT') AND confidential IS NULL;

ALTER TABLE application ADD CONSTRAINT check_confidentiality CHECK (
    CASE
        WHEN status IN ('CREATED', 'SENT') THEN true
        WHEN status = 'WAITING_PLACEMENT' THEN checkedbyadmin = false OR confidential IS NOT NULL
        ELSE confidential IS NOT NULL
    END
);
