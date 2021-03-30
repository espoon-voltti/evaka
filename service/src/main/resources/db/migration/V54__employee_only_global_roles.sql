UPDATE employee SET roles = array(
    SELECT DISTINCT role
    FROM (
         SELECT role
         FROM unnest(roles) AS role
         WHERE role IN ('ADMIN', 'DIRECTOR', 'FINANCE_ADMIN', 'SERVICE_WORKER')
    ) AS filtered_roles
    ORDER BY role
);

ALTER TABLE employee ADD CONSTRAINT "chk$valid_role" CHECK (
  array['ADMIN', 'DIRECTOR', 'FINANCE_ADMIN', 'SERVICE_WORKER']::user_role[] @> roles
);
