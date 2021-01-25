UPDATE application_form
    SET document = document || jsonb_build_object('type', upper(document ->> 'type'))
    WHERE 1=1;
