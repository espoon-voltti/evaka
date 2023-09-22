ALTER TABLE daycare_acl DROP CONSTRAINT "chk$valid_role";
ALTER TABLE daycare_acl ADD CONSTRAINT "chk$valid_role" CHECK ((role = ANY (ARRAY['UNIT_SUPERVISOR'::user_role,
                                                                                  'STAFF'::user_role,
                                                                                  'SPECIAL_EDUCATION_TEACHER'::user_role,
                                                                                  'EARLY_CHILDHOOD_EDUCATION_SECRETARY'::user_role])));
