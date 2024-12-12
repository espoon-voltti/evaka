-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO employee (id, first_name, last_name, email, external_id, roles, active) VALUES
    ('00000000-0000-4000-8000-000000000001', 'Päivi', 'Pääkäyttäjä', 'paivi.paakayttaja@espoo.fi', 'espoo-ad:00000000-0000-4000-8000-000000000001', '{ADMIN, SERVICE_WORKER, FINANCE_ADMIN}'::user_role[], TRUE),
    ('00000000-0000-4000-8001-000000000000', 'Paula', 'Palveluohjaaja', 'paula.palveluohjaaja@espoo.fi', 'espoo-ad:00000000-0000-4000-8001-000000000000', '{SERVICE_WORKER}'::user_role[], TRUE),
    ('00000000-0000-4000-8002-000000000000', 'Lasse', 'Laskuttaja', 'lasse.laskuttaja@espoo.fi', 'espoo-ad:00000000-0000-4000-8002-000000000000', '{FINANCE_ADMIN}'::user_role[], TRUE),
    ('00000000-0000-4000-8009-000000000001', 'Lassi', 'Ulkoistettu Laskuttaja', 'ulkoistettu.laskuttaja@espoo.fi', 'espoo-ad:00000000-0000-4009-8000-000000000000', '{FINANCE_STAFF}'::user_role[], TRUE),
    ('00000000-0000-4000-8003-000000000000', 'Hemmo', 'Hallinto', 'hemmo.hallinto@espoo.fi', 'espoo-ad:00000000-0000-4000-8003-000000000000', '{DIRECTOR}'::user_role[], TRUE),
    ('00000000-0000-4000-8007-000000000000', 'Raisa', 'Raportoija', 'raisa.raportoija@espoo.fi', 'espoo-ad:00000000-0000-4000-8007-000000000000', '{REPORT_VIEWER}'::user_role[], TRUE),
    ('00000000-0000-4000-8008-000000000000', 'Viena', 'Viestittäjä', 'viena.viestittaja@espoo.fi', 'espoo-ad:00000000-0000-4000-8008-000000000000', '{MESSAGING}'::user_role[], TRUE);
INSERT INTO employee (id, first_name, last_name, email, external_id, active) VALUES
    ('00000000-0000-4000-8004-000000000000', 'Essi', 'Esimies', 'essi.esimies@espoo.fi', 'espoo-ad:00000000-0000-4000-8004-000000000000', TRUE),
    ('00000000-0000-4000-8004-000000000001', 'Eemeli', 'Esimies', 'eemeli.esimies@espoo.fi', 'espoo-ad:00000000-0000-4000-8004-000000000001', TRUE),
    ('00000000-0000-4000-8005-000000000000', 'Kaisa', 'Kasvattaja', 'kaisa.kasvattaja@espoo.fi', 'espoo-ad:00000000-0000-4000-8005-000000000000', TRUE),
    ('00000000-0000-4000-8005-000000000001', 'Kalle', 'Kasvattaja', 'kalle.kasvattaja@espoo.fi', 'espoo-ad:00000000-0000-4000-8005-000000000001', TRUE),
    ('00000000-0000-4000-8006-000000000000', 'Erkki', 'Erityisopettaja', 'erkki.erityisopettaja@espoo.fi', 'espoo-ad:00000000-0000-4000-8006-000000000000', TRUE);
INSERT INTO employee (id, first_name, last_name, social_security_number, active) VALUES
    ('fdbf9276-c5a9-4092-87f8-6a27521d940a', 'Hannele', 'Finström', '060195-966B', true);

INSERT INTO evaka_user (id, type, employee_id, name)
SELECT id, 'EMPLOYEE', id, last_name || ' ' || first_name
FROM employee;

INSERT INTO daycare_acl (daycare_id, employee_id, role) VALUES
    ('2dcf0fc0-788e-11e9-bd12-db78e886e666', '00000000-0000-4000-8004-000000000000', 'UNIT_SUPERVISOR'),
    ('2dcf0fc0-788e-11e9-bd12-db78e886e666', '00000000-0000-4000-8005-000000000000', 'STAFF'),
    ('2dcf0fc0-788e-11e9-bd12-db78e886e666', '00000000-0000-4000-8006-000000000000', 'SPECIAL_EDUCATION_TEACHER'),
    ('2dd6e5f6-788e-11e9-bd72-9f1cfe2d8405', '00000000-0000-4000-8004-000000000001', 'UNIT_SUPERVISOR'),
    ('2dd6e5f6-788e-11e9-bd72-9f1cfe2d8405', '00000000-0000-4000-8005-000000000001', 'STAFF'),
    ('2dd6e5f6-788e-11e9-bd72-9f1cfe2d8405', '00000000-0000-4000-8006-000000000000', 'SPECIAL_EDUCATION_TEACHER'),
    ('2dd6e5f6-788e-11e9-bd72-9f1cfe999999', 'fdbf9276-c5a9-4092-87f8-6a27521d940a', 'UNIT_SUPERVISOR');

INSERT INTO message_account (employee_id, type)
SELECT id, 'PERSONAL'::message_account_type AS type
FROM employee e
WHERE EXISTS(
    SELECT 1
    FROM daycare_acl acl
    WHERE acl.employee_id = e.id
    AND acl.role IN ('UNIT_SUPERVISOR', 'SPECIAL_EDUCATION_TEACHER'));
