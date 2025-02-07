create table daycare_acl_schedule (
    daycare_id uuid not null references daycare on delete cascade,
    employee_id uuid not null references employee on delete cascade,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null,
    role user_role not null
        constraint chk$valid_role
            check (role = ANY (ARRAY [
                'UNIT_SUPERVISOR'::user_role,
                'STAFF'::user_role,
                'SPECIAL_EDUCATION_TEACHER'::user_role,
                'EARLY_CHILDHOOD_EDUCATION_SECRETARY'::user_role
            ])),
    start_date date not null,
    end_date date
);

create trigger set_timestamp before update on daycare_acl_schedule for each row execute procedure trigger_refresh_updated_at();

create unique index uniq$daycare_acl_schedule_employee_daycare on daycare_acl_schedule (employee_id, daycare_id);
create unique index uniq$daycare_acl_schedule_daycare_employee on daycare_acl_schedule (daycare_id, employee_id);
