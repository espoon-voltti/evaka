CREATE TABLE public.backup_pickup(
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    child_id uuid NOT NULL,
    name text NOT NULL,
    phone text NOT NULL
);

ALTER TABLE ONLY public.backup_pickup
    ADD CONSTRAINT backup_pickup_child_fkey FOREIGN KEY (child_id) REFERENCES public.person(id);
