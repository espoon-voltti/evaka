ALTER TABLE employee DROP CONSTRAINT "chk$valid_role";
ALTER TABLE employee ADD CONSTRAINT "chk$valid_role" CHECK (
        array['ADMIN', 'DIRECTOR', 'REPORT_VIEWER', 'FINANCE_ADMIN', 'SERVICE_WORKER']::user_role[] @> roles
    );
