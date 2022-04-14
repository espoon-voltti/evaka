UPDATE evaka_user AS eu
SET name = p.last_name || ' ' || p.first_name
FROM person p
WHERE p.id = eu.citizen_id;

UPDATE evaka_user AS eu
SET name = e.last_name || ' ' || e.first_name
FROM employee e
WHERE e.id = eu.employee_id;
