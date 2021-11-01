ALTER TABLE service_need_option RENAME COLUMN name TO name_fi;
ALTER TABLE service_need_option ADD COLUMN name_sv TEXT;
ALTER TABLE service_need_option ADD COLUMN name_en TEXT;

UPDATE service_need_option SET name_sv = name_fi, name_en = name_fi;

ALTER TABLE service_need_option ALTER COLUMN name_sv SET NOT NULL;
ALTER TABLE service_need_option ALTER COLUMN name_en SET NOT NULL;

UPDATE application_form
SET document = jsonb_set(document, '{serviceNeedOption}', jsonb_build_object(
    'id', document -> 'serviceNeedOption' ->> 'id',
    'nameFi', document -> 'serviceNeedOption' ->> 'name',
    'nameSv', document -> 'serviceNeedOption' ->> 'name',
    'nameEn', document -> 'serviceNeedOption' ->> 'name'
))
WHERE document ->> 'serviceNeedOption' IS NOT NULL;
