CREATE TYPE child_consent_type AS ENUM ('EVAKA_PROFILE_PICTURE');

CREATE TABLE child_consent (
  id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
  child_id uuid NOT NULL REFERENCES person (id) ON DELETE CASCADE,
  type child_consent_type NOT NULL,
  given bool NOT NULL,
  given_by_guardian uuid REFERENCES person (id),
  given_by_employee uuid REFERENCES employee (id),
  given_at timestamp with time zone NOT NULL DEFAULT now(),

  CONSTRAINT unique$child_consent_type UNIQUE (child_id, type),
  CONSTRAINT check$child_consent_given_by CHECK ((given_by_guardian IS NULL) != (given_by_employee IS NULL))
);

CREATE INDEX idx$child_consent_child_id ON child_consent (child_id);
