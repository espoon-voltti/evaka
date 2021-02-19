CREATE TYPE public.daycare_daily_note_level_info AS ENUM (
    'GOOD',
    'MEDIUM',
    'NONE'
    );

CREATE TYPE public.daycare_daily_note_reminder AS ENUM (
    'DIAPERS',
    'CLOTHES',
    'LAUNDRY'
    );

CREATE TABLE public.daycare_daily_note(
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    child_id uuid,
    group_id uuid,
    date date not null,
    note text,
    feeding_note daycare_daily_note_level_info,
    sleeping_note daycare_daily_note_level_info,
    sleeping_hours numeric,
    reminders daycare_daily_note_reminder[],
    reminder_note text,
    modified_at timestamp with time zone DEFAULT now() NOT NULL,
    modified_by text NOT NULL
);

ALTER TABLE ONLY public.daycare_daily_note
    ADD CONSTRAINT daycare_daily_note_child_fkey FOREIGN KEY (child_id) REFERENCES public.person(id);

ALTER TABLE ONLY public.daycare_daily_note
    ADD CONSTRAINT daycare_daily_note_group_fkey FOREIGN KEY (group_id) REFERENCES public.daycare_group(id);

ALTER TABLE ONLY public.daycare_daily_note
    ADD CHECK (child_id IS NOT NULL OR group_id IS NOT NULL)