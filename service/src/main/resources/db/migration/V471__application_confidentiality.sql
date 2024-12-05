ALTER TABLE application ADD COLUMN confidential bool;

UPDATE application
SET confidential = (
    (document -> 'careDetails' ->> 'assistanceNeeded')::bool = true OR
    document -> 'careDetails' ->> 'assistanceDescription' <> '' OR
    document -> 'additionalDetails' ->> 'dietType' <> '' OR
    document -> 'additionalDetails' ->> 'allergyType' <> '' OR
    document -> 'additionalDetails' ->> 'otherInfo' <> ''
)
WHERE status NOT IN ('CREATED', 'SENT');
