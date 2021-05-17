-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO employee (id, first_name, last_name, email, external_id, roles) VALUES 
    ('00000000-0000-0000-0000-000000000001', 'Päivi', 'Pääkäyttäjä', 'paivi.paakayttaja@espoo.fi', 'espoo-ad:00000000-0000-0000-0000-000000000001', '{ADMIN, SERVICE_WORKER, FINANCE_ADMIN}'::user_role[]),
    ('00000000-0000-0000-0001-000000000000', 'Paula', 'Palveluohjaaja', 'paula.palveluohjaaja@espoo.fi', 'espoo-ad:00000000-0000-0000-0001-000000000000', '{SERVICE_WORKER}'::user_role[]),
    ('00000000-0000-0000-0002-000000000000', 'Lasse', 'Laskuttaja', 'lasse.laskuttaja@espoo.fi', 'espoo-ad:00000000-0000-0000-0002-000000000000', '{FINANCE_ADMIN}'::user_role[]),
    ('00000000-0000-0000-0003-000000000000', 'Raisa', 'Raportoija', 'raisa.raportoija@espoo.fi', 'espoo-ad:00000000-0000-0000-0003-000000000000', '{DIRECTOR}'::user_role[]);
INSERT INTO employee (id, first_name, last_name, email, external_id) VALUES 
    ('00000000-0000-0000-0004-000000000000', 'Essi', 'Esimies', 'essi.esimies@espoo.fi', 'espoo-ad:00000000-0000-0000-0004-000000000000'),
    ('00000000-0000-0000-0004-000000000001', 'Eemeli', 'Esimies', 'eemeli.esimies@espoo.fi', 'espoo-ad:00000000-0000-0000-0004-000000000001'),
    ('00000000-0000-0000-0005-000000000000', 'Kaisa', 'Kasvattaja', 'kaisa.kasvattaja@espoo.fi', 'espoo-ad:00000000-0000-0000-0005-000000000000'),
    ('00000000-0000-0000-0005-000000000001', 'Kalle', 'Kasvattaja', 'kalle.kasvattaja@espoo.fi', 'espoo-ad:00000000-0000-0000-0005-000000000001'),
    ('00000000-0000-0000-0006-000000000000', 'Erkki', 'Erityisopettaja', 'erkki.erityisopettaja@espoo.fi', 'espoo-ad:00000000-0000-0000-0006-000000000000');

INSERT INTO message_account (employee_id) SELECT id FROM employee;

INSERT INTO daycare_acl (daycare_id, employee_id, role) VALUES
    ('2dd6e5f6-788e-11e9-bd72-9f1cfe2d8405', '00000000-0000-0000-0004-000000000000', 'UNIT_SUPERVISOR'),
    ('2dda790a-788e-11e9-bdab-27ae0339236a', '00000000-0000-0000-0004-000000000000', 'UNIT_SUPERVISOR'),
    ('2dd5ed54-788e-11e9-bd66-93ebec18bb41', '00000000-0000-0000-0004-000000000001', 'UNIT_SUPERVISOR'),
    ('d5c7c7f8-2abe-11e9-8d80-2b926c773065', '00000000-0000-0000-0004-000000000001', 'UNIT_SUPERVISOR');

INSERT INTO daycare_acl (daycare_id, employee_id, role) VALUES
    ('2dd6e5f6-788e-11e9-bd72-9f1cfe2d8405', '00000000-0000-0000-0005-000000000000', 'STAFF'),
    ('2dda790a-788e-11e9-bdab-27ae0339236a', '00000000-0000-0000-0005-000000000000', 'STAFF'),
    ('2dd5ed54-788e-11e9-bd66-93ebec18bb41', '00000000-0000-0000-0005-000000000001', 'STAFF'),
    ('d5c7c7f8-2abe-11e9-8d80-2b926c773065', '00000000-0000-0000-0005-000000000001', 'STAFF'),
    ('2dd6e5f6-788e-11e9-bd72-9f1cfe2d8405', '00000000-0000-0000-0006-000000000000', 'SPECIAL_EDUCATION_TEACHER'),
    ('2dda790a-788e-11e9-bdab-27ae0339236a', '00000000-0000-0000-0006-000000000000', 'SPECIAL_EDUCATION_TEACHER');

