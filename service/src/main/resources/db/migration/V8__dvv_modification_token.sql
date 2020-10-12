CREATE TABLE dvv_modification_token(
    token text PRIMARY KEY,
    next_token text,
    ssns_sent numeric NOT NULL default 0,
    modifications_received numeric NOT NULL default 0,
    created timestamp with time zone DEFAULT now() NOT NULL
);

-- First token of Oct. 1st, 2020
INSERT INTO dvv_modification_token (token, next_token) VALUES ('000000000', '151822305');
