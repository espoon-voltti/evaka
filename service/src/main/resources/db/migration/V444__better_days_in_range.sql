CREATE OR REPLACE FUNCTION days_in_range(daterange) RETURNS integer
LANGUAGE sql IMMUTABLE PARALLEL SAFE RETURNS NULL ON NULL INPUT
RETURN
CASE $1
  WHEN 'empty' THEN 0
  ELSE
    (CASE
        WHEN upper($1) IS NULL THEN 'infinity' -- invalid case, and converting to infinity here gives a nicer error
        WHEN upper_inc($1) THEN upper($1) - 1
        ELSE upper($1)
    END)
    -
    (CASE
        WHEN lower($1) IS NULL THEN '-infinity' -- invalid case, and converting to -infinity here gives a nicer error
        WHEN lower_inc($1) THEN lower($1)
        ELSE lower($1) + 1
    END)
END;
