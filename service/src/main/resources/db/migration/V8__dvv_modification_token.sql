CREATE TABLE dvv_modification_token(
    token text PRIMARY KEY,
    created timestamp with time zone DEFAULT now() NOT NULL
);

-- First token of Oct. 1st, 2020
INSERT INTO dvv_modification_token (token) VALUES ('151822305');
