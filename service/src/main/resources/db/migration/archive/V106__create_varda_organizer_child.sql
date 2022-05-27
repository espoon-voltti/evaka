CREATE TABLE varda_organizer_child (
    evaka_person_id uuid NOT NULL,
    varda_person_oid character varying NOT NULL,
    varda_child_id bigint NOT NULL,
    organizer_oid character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    modified_at timestamp with time zone DEFAULT now() NOT NULL,
    uploaded_at timestamp with time zone
);
