CREATE TABLE password_blacklist_source (
    id int PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name text NOT NULL,
    imported_at timestamp with time zone NOT NULL
);

ALTER TABLE password_blacklist_source
    ADD CONSTRAINT uniq$password_blacklist_source_name UNIQUE (name)
;

CREATE TABLE password_blacklist (
    password text PRIMARY KEY,
    source int NOT NULL
);

ALTER TABLE password_blacklist
    ADD CONSTRAINT fk$source FOREIGN KEY (source) REFERENCES password_blacklist_source (id) ON DELETE CASCADE
;
