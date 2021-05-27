CREATE TABLE child_images (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid REFERENCES person(id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_images FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$child_images_child_id ON child_images(child_id);
