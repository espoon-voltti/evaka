ALTER TABLE public.daycare_acl DROP CONSTRAINT "chk$valid_role";
ALTER TABLE public.daycare_acl ADD CONSTRAINT "chk$valid_role" CHECK ((role = ANY (ARRAY['UNIT_SUPERVISOR'::public.user_role,
                                                                                         'STAFF'::public.user_role,
                                                                                         'SPECIAL_EDUCATION_TEACHER'::public.user_role])))
