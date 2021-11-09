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

INSERT INTO evaka_user (id, type, employee_id, name)
SELECT id, 'EMPLOYEE', id, first_name || ' ' || last_name
FROM employee;

INSERT INTO message_account (employee_id)
SELECT id
FROM employee e
WHERE EXISTS(
    SELECT 1
    FROM daycare_acl acl
    WHERE acl.employee_id = e.id
    AND acl.role IN ('UNIT_SUPERVISOR', 'SPECIAL_EDUCATION_TEACHER'));
