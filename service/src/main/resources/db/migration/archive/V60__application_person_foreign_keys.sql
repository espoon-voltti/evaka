ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_guardian_fkey FOREIGN KEY (guardian_id) REFERENCES public.person(id);

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_child_fkey FOREIGN KEY (child_id) REFERENCES public.person(id);
