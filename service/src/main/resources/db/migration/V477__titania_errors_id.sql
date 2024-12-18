ALTER TABLE titania_errors
ADD COLUMN id uuid DEFAULT ext.uuid_generate_v1mc();

ALTER TABLE titania_errors
ADD CONSTRAINT titania_errors_pkey PRIMARY KEY (id);
