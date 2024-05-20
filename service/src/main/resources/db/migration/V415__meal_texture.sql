CREATE TABLE meal_texture
(
    id   INTEGER PRIMARY KEY NOT NULL,
    name TEXT    NOT NULL
);
ALTER TABLE child
    ADD meal_texture_id INTEGER;

ALTER TABLE child
    ADD CONSTRAINT fk$meal_texture_id_meal_texture_id
        FOREIGN KEY (meal_texture_id) REFERENCES meal_texture (id)
            ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX idx$child_meal_texture_id
    ON child (meal_texture_id);

