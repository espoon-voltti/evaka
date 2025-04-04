CREATE TABLE nekku_special_diet_choices (
  child_id UUID NOT NULL REFERENCES child (id),
  diet_id TEXT NOT NULL REFERENCES nekku_special_diet (id),
  field_id TEXT NOT NULL REFERENCES nekku_special_diet_field (id),
  value TEXT NOT NULL
);

CREATE INDEX fk$nekku_special_diet_choices_child_id
    ON nekku_special_diet_choices (child_id);

CREATE INDEX fk$nekku_special_diet_choices_diet_id
    ON nekku_special_diet_choices (diet_id);

CREATE INDEX fk$nekku_special_diet_choices_field_id
    ON nekku_special_diet_choices (field_id);
