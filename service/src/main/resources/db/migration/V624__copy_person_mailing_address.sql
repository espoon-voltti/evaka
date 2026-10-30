ALTER TABLE person DISABLE TRIGGER set_timestamp;
UPDATE person SET
    mailing_street_address = street_address,
    mailing_postal_code = postal_code,
    mailing_post_office = post_office;
ALTER TABLE person ENABLE TRIGGER set_timestamp;
