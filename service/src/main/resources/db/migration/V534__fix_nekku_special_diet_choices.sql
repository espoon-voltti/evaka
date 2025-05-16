ALTER TABLE nekku_special_diet_choices
DROP CONSTRAINT nekku_special_diet_choices_diet_id_fkey;
ALTER TABLE nekku_special_diet_choices
DROP CONSTRAINT nekku_special_diet_choices_field_id_fkey;

ALTER TABLE nekku_special_diet_choices
ADD CONSTRAINT fk$diet_id
FOREIGN KEY (diet_id)
REFERENCES nekku_special_diet (id)
ON DELETE CASCADE;

ALTER TABLE nekku_special_diet_choices
ADD CONSTRAINT fk$field_id
FOREIGN KEY (field_id)
REFERENCES nekku_special_diet_field (id)
ON DELETE CASCADE;
