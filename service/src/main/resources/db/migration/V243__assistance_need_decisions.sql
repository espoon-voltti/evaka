CREATE SEQUENCE public.assistance_need_decision_number_seq
    START WITH 1000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

create table assistance_need_decision
(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone default now() NOT NULL,
    updated timestamp with time zone default now() NOT NULL,
    decision_number integer default nextval('public.assistance_need_decision_number_seq'::regclass) NOT NULL,
    child_id uuid NOT NULL references child on delete restrict,
    start_date date,
    end_date date,
    status text NOT NULL,

    language text NOT NULL,
    decision_made date,
    sent_for_decision date,
    selected_unit uuid references daycare on delete restrict,
    decision_maker_employee_id uuid references employee on delete restrict,
    decision_maker_title text,
    preparer_1_employee_id uuid references employee on delete restrict,
    preparer_1_title text,
    preparer_2_employee_id uuid references employee on delete restrict,
    preparer_2_title text,

    pedagogical_motivation text,
    structural_motivation_opt_smaller_group bool default false NOT NULL,
    structural_motivation_opt_special_group bool default false NOT NULL,
    structural_motivation_opt_small_group bool default false NOT NULL,
    structural_motivation_opt_group_assistant bool default false NOT NULL,
    structural_motivation_opt_child_assistant bool default false NOT NULL,
    structural_motivation_opt_additional_staff bool default false NOT NULL,
    structural_motivation_description text,
    care_motivation text,
    service_opt_consultation_special_ed bool default false NOT NULL,
    service_opt_part_time_special_ed bool default false NOT NULL,
    service_opt_full_time_special_ed bool default false NOT NULL,
    service_opt_interpretation_and_assistance_services bool default false NOT NULL,
    service_opt_special_aides bool default false NOT NULL,
    services_motivation text,
    expert_responsibilities text,
    guardians_heard_on date,
    view_of_guardians text,
    other_representative_heard boolean default false NOT NULL,
    other_representative_details text,

    assistance_level text,
    assistance_service_start date,
    assistance_service_end date,
    motivation_for_decision text
);

create index idx$assistance_need_decision_child
    on assistance_need_decision (child_id);

create table assistance_need_decision_guardian
(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone default now() not null,
    assistance_need_decision_id uuid not null references assistance_need_decision on delete cascade,
    person_id uuid NOT NULL references person on delete restrict,
    is_heard boolean default false NOT NULL,
    details text
);

create index idx$assistance_need_decision_id
    on assistance_need_decision_guardian (assistance_need_decision_id);

