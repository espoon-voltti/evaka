--
-- Added manually
--

-- Make default grants to application role for objects created by flyway
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, TRIGGER, INSERT, UPDATE, DELETE ON TABLES TO "${application_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO "${application_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT EXECUTE ON FUNCTIONS TO "${application_user}";

--
-- PostgreSQL database dump
--

--
-- Name: ext; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA IF NOT EXISTS ext;


--
-- Name: btree_gist; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA ext;


--
-- Name: unaccent; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA ext;


--
-- Name: application_origin_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.application_origin_type AS ENUM (
    'ELECTRONIC',
    'PAPER'
    );


--
-- Name: application_status_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.application_status_type AS ENUM (
    'CREATED',
    'SENT',
    'WAITING_PLACEMENT',
    'WAITING_DECISION',
    'WAITING_MAILING',
    'WAITING_CONFIRMATION',
    'REJECTED',
    'ACTIVE',
    'CANCELLED',
    'TERMINATED',
    'DEPRECATED',
    'WAITING_UNIT_CONFIRMATION'
    );


--
-- Name: assistance_action_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.assistance_action_type AS ENUM (
    'ASSISTANCE_SERVICE_CHILD',
    'ASSISTANCE_SERVICE_UNIT',
    'SMALLER_GROUP',
    'SPECIAL_GROUP',
    'PERVASIVE_VEO_SUPPORT',
    'RESOURCE_PERSON',
    'RATIO_DECREASE',
    'PERIODICAL_VEO_SUPPORT',
    'OTHER'
    );


--
-- Name: assistance_basis; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.assistance_basis AS ENUM (
    'AUTISM',
    'DEVELOPMENTAL_DISABILITY_1',
    'DEVELOPMENTAL_DISABILITY_2',
    'FOCUS_CHALLENGE',
    'LINGUISTIC_CHALLENGE',
    'DEVELOPMENT_MONITORING',
    'DEVELOPMENT_MONITORING_PENDING',
    'MULTI_DISABILITY',
    'LONG_TERM_CONDITION',
    'REGULATION_SKILL_CHALLENGE',
    'DISABILITY',
    'OTHER'
    );


--
-- Name: assistance_measure; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.assistance_measure AS ENUM (
    'SPECIAL_ASSISTANCE_DECISION',
    'INTENSIFIED_ASSISTANCE',
    'EXTENDED_COMPULSORY_EDUCATION',
    'CHILD_SERVICE',
    'CHILD_ACCULTURATION_SUPPORT',
    'TRANSPORT_BENEFIT'
    );


--
-- Name: care_types; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.care_types AS ENUM (
    'CENTRE',
    'FAMILY',
    'GROUP_FAMILY',
    'CLUB',
    'PRESCHOOL',
    'PREPARATORY_EDUCATION'
    );


--
-- Name: confirmation_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.confirmation_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED'
    );


--
-- Name: decision2_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.decision2_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED'
    );


--
-- Name: decision2_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.decision2_type AS ENUM (
    'DAYCARE',
    'DAYCARE_PART_TIME',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'PREPARATORY_EDUCATION',
    'CLUB'
    );

--
-- Name: koski_study_right_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.koski_study_right_type AS ENUM (
    'PRESCHOOL',
    'PREPARATORY'
    );


--
-- Name: placement_reject_reason; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.placement_reject_reason AS ENUM (
    'OTHER',
    'REASON_1',
    'REASON_2'
    );


--
-- Name: placement_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.placement_type AS ENUM (
    'DAYCARE',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'DAYCARE_PART_TIME',
    'PREPARATORY',
    'PREPARATORY_DAYCARE',
    'CLUB'
    );


--
-- Name: unit_language; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.unit_language AS ENUM (
    'fi',
    'sv'
    );


--
-- Name: unit_provider_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.unit_provider_type AS ENUM (
    'MUNICIPAL',
    'PURCHASED',
    'PRIVATE',
    'MUNICIPAL_SCHOOL',
    'PRIVATE_SERVICE_VOUCHER'
    );


--
-- Name: user_role; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.user_role AS ENUM (
    'ADMIN',
    'DIRECTOR',
    'FINANCE_ADMIN',
    'SERVICE_WORKER',
    'UNIT_SUPERVISOR',
    'STAFF'
    );


--
-- Name: days_in_range(daterange); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.days_in_range(daterange) RETURNS integer
    LANGUAGE sql IMMUTABLE
AS $_$
SELECT upper($1) - lower($1)
$_$;


--
-- Name: ensure_decision_number_curr_year(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.ensure_decision_number_curr_year() RETURNS trigger
    LANGUAGE plpgsql
AS $$
DECLARE
    min_seq integer;
BEGIN
    -- Ensure current sequence value is used for number
    IF (NEW.number <> currval('decision_number_seq')) THEN
        RAISE EXCEPTION 'Column number is not set using sequence'
            USING HINT = 'Please set number either with DEFAULT value or calling nextval(''decision_number_seq'')';
    END IF;

    -- Minimum sequence number for current year
    min_seq := EXTRACT(YEAR FROM now())::integer * 100000;
    -- Update sequence if the year has changed
    IF (currval('decision_number_seq') <= min_seq) THEN
        PERFORM setval('decision_number_seq', min_seq);
        NEW.number := nextval('decision_number_seq');
    END IF;
    RETURN NEW;
END;
$$;



--
-- Name: trigger_refresh_updated(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.trigger_refresh_updated() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated = NOW();
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

--
-- Name: absence; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.absence (
                                id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                child_id uuid NOT NULL,
                                date date NOT NULL,
                                care_type text NOT NULL,
                                absence_type text NOT NULL,
                                modified_at timestamp with time zone DEFAULT now() NOT NULL,
                                modified_by text NOT NULL
);


--
-- Name: application; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.application (
                                    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                    created timestamp with time zone DEFAULT now() NOT NULL,
                                    updated timestamp with time zone DEFAULT now() NOT NULL,
                                    sentdate date,
                                    duedate date,
                                    application_status bigint NOT NULL,
                                    guardian_id uuid NOT NULL,
                                    child_id uuid NOT NULL,
                                    origin integer DEFAULT 1,
                                    checkedbyadmin boolean DEFAULT false NOT NULL,
                                    hidefromguardian boolean DEFAULT false NOT NULL,
                                    transferapplication boolean DEFAULT false NOT NULL,
                                    other_guardian_id uuid,
                                    additionaldaycareapplication boolean DEFAULT false NOT NULL
);


--
-- Name: application_form; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.application_form (
                                         id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                         application_id uuid NOT NULL,
                                         created timestamp with time zone DEFAULT now() NOT NULL,
                                         revision bigint NOT NULL,
                                         document jsonb NOT NULL,
                                         updated timestamp with time zone DEFAULT now() NOT NULL,
                                         latest boolean NOT NULL
);


--
-- Name: application_note; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.application_note (
                                         id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                         application_id uuid NOT NULL,
                                         content text NOT NULL,
                                         created_by uuid NOT NULL,
                                         created timestamp with time zone DEFAULT now() NOT NULL,
                                         updated timestamp with time zone DEFAULT now() NOT NULL,
                                         updated_by uuid
);


--
-- Name: application_origin; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.application_origin (
                                           id integer NOT NULL,
                                           description text
);


--
-- Name: application_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.application_status (
                                           id bigint NOT NULL,
                                           description character varying(255) NOT NULL
);


--
-- Name: assistance_need; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assistance_need (
                                        id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                        created timestamp with time zone DEFAULT now() NOT NULL,
                                        updated timestamp with time zone DEFAULT now() NOT NULL,
                                        updated_by uuid NOT NULL,
                                        child_id uuid NOT NULL,
                                        start_date date NOT NULL,
                                        end_date date NOT NULL,
                                        capacity_factor numeric DEFAULT 0 NOT NULL,
                                        description text DEFAULT ''::text NOT NULL,
                                        bases public.assistance_basis[] DEFAULT '{}'::public.assistance_basis[] NOT NULL,
                                        other_basis text DEFAULT ''::text NOT NULL,
                                        CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);



--
-- Name: decision_number_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.decision_number_seq
    START WITH 1000000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: decision2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.decision2 (
                                  id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                  number integer DEFAULT nextval('public.decision_number_seq'::regclass) NOT NULL,
                                  created timestamp with time zone DEFAULT now(),
                                  updated timestamp with time zone DEFAULT now(),
                                  created_by uuid NOT NULL,
                                  sent_date date,
                                  unit_id uuid NOT NULL,
                                  application_id uuid NOT NULL,
                                  type public.decision2_type NOT NULL,
                                  start_date date NOT NULL,
                                  end_date date NOT NULL,
                                  status public.decision2_status DEFAULT 'PENDING'::public.decision2_status NOT NULL,
                                  document_uri text,
                                  requested_start_date date,
                                  resolved timestamp with time zone,
                                  resolved_by uuid,
                                  other_guardian_document_uri text,
                                  planned boolean DEFAULT true NOT NULL
);

--
-- Name: decision_number_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.decision_number_seq OWNED BY public.decision2.number;


GRANT UPDATE ON SEQUENCE decision_number_seq TO "${application_user}";


--
-- Name: legacy_placement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.legacy_placement (
                                         id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                         created timestamp with time zone DEFAULT now() NOT NULL,
                                         updated timestamp with time zone DEFAULT now() NOT NULL,
                                         application_id uuid NOT NULL,
                                         start_date date NOT NULL,
                                         status bigint DEFAULT 0 NOT NULL,
                                         end_date date
);


--
-- Name: placement_plan; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.placement_plan (
                                       id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                       created timestamp with time zone DEFAULT now(),
                                       updated timestamp with time zone DEFAULT now(),
                                       type public.placement_type NOT NULL,
                                       unit_id uuid NOT NULL,
                                       application_id uuid NOT NULL,
                                       start_date date NOT NULL,
                                       end_date date NOT NULL,
                                       preschool_daycare_start_date date,
                                       preschool_daycare_end_date date,
                                       deleted boolean DEFAULT false NOT NULL,
                                       preparatory_start_date date,
                                       preparatory_end_date date,
                                       unit_confirmation_status public.confirmation_status DEFAULT 'PENDING'::public.confirmation_status NOT NULL,
                                       unit_reject_reason public.placement_reject_reason,
                                       unit_reject_other_reason text,
                                       CONSTRAINT preparatory_start_before_end CHECK ((preparatory_start_date <= preparatory_end_date)),
                                       CONSTRAINT preschool_daycare_not_null CHECK (((type <> 'PRESCHOOL_DAYCARE'::public.placement_type) OR ((preschool_daycare_start_date IS NOT NULL) AND (preschool_daycare_end_date IS NOT NULL)))),
                                       CONSTRAINT preschool_daycare_start_before_end CHECK ((preschool_daycare_start_date <= preschool_daycare_end_date)),
                                       CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);


--
-- Name: approval_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.approval_type (
                                      id bigint NOT NULL,
                                      description character varying(255) NOT NULL
);


--
-- Name: assistance_action; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assistance_action (
                                          id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                          created timestamp with time zone DEFAULT now() NOT NULL,
                                          updated timestamp with time zone DEFAULT now() NOT NULL,
                                          updated_by uuid NOT NULL,
                                          child_id uuid NOT NULL,
                                          start_date date NOT NULL,
                                          end_date date NOT NULL,
                                          actions public.assistance_action_type[] DEFAULT '{}'::public.assistance_action_type[] NOT NULL,
                                          other_action text DEFAULT ''::text NOT NULL,
                                          measures public.assistance_measure[] DEFAULT '{}'::public.assistance_measure[] NOT NULL,
                                          CONSTRAINT assistance_action_start_before_end CHECK ((start_date <= end_date))
);


--
-- Name: async_job; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.async_job (
                                  id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                  type text NOT NULL,
                                  submitted_at timestamp with time zone DEFAULT now() NOT NULL,
                                  run_at timestamp with time zone DEFAULT now() NOT NULL,
                                  claimed_at timestamp with time zone,
                                  claimed_by bigint,
                                  retry_count integer NOT NULL,
                                  retry_interval interval NOT NULL,
                                  started_at timestamp with time zone,
                                  completed_at timestamp with time zone,
                                  payload jsonb NOT NULL
);


--
-- Name: backup_care; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.backup_care (
                                    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                    created timestamp with time zone DEFAULT now(),
                                    updated timestamp with time zone DEFAULT now(),
                                    child_id uuid NOT NULL,
                                    unit_id uuid NOT NULL,
                                    group_id uuid,
                                    start_date date NOT NULL,
                                    end_date date NOT NULL,
                                    CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);


--
-- Name: care_area; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.care_area (
                                  id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                  name text NOT NULL,
                                  created timestamp with time zone DEFAULT now() NOT NULL,
                                  updated timestamp with time zone DEFAULT now() NOT NULL,
                                  area_code integer,
                                  sub_cost_center text,
                                  short_name text DEFAULT ''::text NOT NULL
);


--
-- Name: child; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.child (
                              id uuid NOT NULL,
                              allergies text DEFAULT ''::text NOT NULL,
                              diet text DEFAULT ''::text NOT NULL,
                              additionalinfo text DEFAULT ''::text NOT NULL
);


--
-- Name: customer_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_id_seq
    START WITH 100000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: daycare; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.daycare (
                                id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                name text NOT NULL,
                                type public.care_types[] DEFAULT '{CENTRE}'::public.care_types[] NOT NULL,
                                care_area_id uuid NOT NULL,
                                phone text,
                                url text,
                                created timestamp with time zone DEFAULT now() NOT NULL,
                                updated timestamp with time zone DEFAULT now() NOT NULL,
                                backup_location text,
                                language_emphasis_id uuid,
                                opening_date date,
                                closing_date date,
                                email text,
                                schedule text,
                                additional_info text,
                                unit_manager_id uuid,
                                cost_center text,
                                upload_to_varda boolean DEFAULT false NOT NULL,
                                can_apply_daycare boolean DEFAULT true NOT NULL,
                                can_apply_preschool boolean DEFAULT true NOT NULL,
                                capacity integer DEFAULT 0 NOT NULL,
                                decision_daycare_name text DEFAULT ''::text NOT NULL,
                                decision_preschool_name text DEFAULT ''::text NOT NULL,
                                decision_handler text DEFAULT ''::text NOT NULL,
                                decision_handler_address text DEFAULT ''::text NOT NULL,
                                round_the_clock boolean DEFAULT false NOT NULL,
                                street_address text DEFAULT ''::text NOT NULL,
                                postal_code text DEFAULT ''::text NOT NULL,
                                post_office text DEFAULT ''::text NOT NULL,
                                mailing_po_box text,
                                location point,
                                mailing_street_address text,
                                mailing_postal_code text,
                                mailing_post_office text,
                                invoiced_by_municipality boolean DEFAULT true NOT NULL,
                                provider_type public.unit_provider_type DEFAULT 'MUNICIPAL'::public.unit_provider_type NOT NULL,
                                language public.unit_language DEFAULT 'fi'::public.unit_language NOT NULL,
                                upload_to_koski boolean DEFAULT false NOT NULL,
                                oph_organization_oid text,
                                oph_unit_oid text,
                                oph_organizer_oid text,
                                can_apply_club boolean DEFAULT false NOT NULL
);


--
-- Name: daycare_acl; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.daycare_acl (
                                    daycare_id uuid NOT NULL,
                                    employee_id uuid NOT NULL,
                                    created timestamp with time zone DEFAULT now() NOT NULL,
                                    updated timestamp with time zone DEFAULT now() NOT NULL,
                                    role public.user_role NOT NULL,
                                    CONSTRAINT "chk$valid_role" CHECK ((role = ANY (ARRAY['UNIT_SUPERVISOR'::public.user_role, 'STAFF'::public.user_role])))
);


--
-- Name: employee; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employee (
                                 id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                 first_name text NOT NULL,
                                 last_name text NOT NULL,
                                 email text,
                                 aad_object_id uuid,
                                 created timestamp with time zone DEFAULT now() NOT NULL,
                                 updated timestamp with time zone DEFAULT now() NOT NULL,
                                 roles public.user_role[] DEFAULT '{}'::public.user_role[] NOT NULL
);


--
-- Name: unit_manager; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.unit_manager (
                                     id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                     name text,
                                     phone text,
                                     email text
);


--
-- Name: daycare_caretaker; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.daycare_caretaker (
                                          id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                          created timestamp with time zone DEFAULT now() NOT NULL,
                                          updated timestamp with time zone DEFAULT now() NOT NULL,
                                          group_id uuid NOT NULL,
                                          amount numeric DEFAULT 0 NOT NULL,
                                          start_date date NOT NULL,
                                          end_date date DEFAULT 'infinity'::date NOT NULL,
                                          CONSTRAINT daycare_caretaker_no_negative_people CHECK ((amount >= (0)::numeric)),
                                          CONSTRAINT daycare_caretaker_start_before_end CHECK ((start_date <= end_date))
);


--
-- Name: daycare_group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.daycare_group (
                                      id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                      daycare_id uuid NOT NULL,
                                      name text NOT NULL,
                                      start_date date NOT NULL,
                                      end_date date DEFAULT 'infinity'::date NOT NULL
);


--
-- Name: daycare_group_placement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.daycare_group_placement (
                                                id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                                created timestamp with time zone DEFAULT now(),
                                                updated timestamp with time zone DEFAULT now(),
                                                daycare_placement_id uuid NOT NULL,
                                                daycare_group_id uuid NOT NULL,
                                                start_date date NOT NULL,
                                                end_date date NOT NULL,
                                                CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);


--
-- Name: decision_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.decision_status (
                                        id bigint NOT NULL,
                                        description character varying(255) NOT NULL
);


--
-- Name: fee_alteration; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_alteration (
                                       id uuid NOT NULL,
                                       person_id uuid NOT NULL,
                                       type text NOT NULL,
                                       amount integer NOT NULL,
                                       is_absolute boolean NOT NULL,
                                       valid_from date NOT NULL,
                                       valid_to date,
                                       notes text NOT NULL,
                                       updated_at timestamp with time zone NOT NULL,
                                       updated_by uuid NOT NULL,
                                       CONSTRAINT fee_alteration_check CHECK ((valid_from <= COALESCE(valid_to, '2099-12-31'::date)))
);


--
-- Name: fee_decision; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_decision (
                                     id uuid NOT NULL,
                                     status text NOT NULL,
                                     head_of_family uuid NOT NULL,
                                     valid_from date,
                                     valid_to date,
                                     created_at timestamp with time zone DEFAULT now(),
                                     cancelled_at date,
                                     decision_number bigint,
                                     partner uuid,
                                     document_key text,
                                     approved_by uuid,
                                     approved_at timestamp with time zone,
                                     head_of_family_income jsonb,
                                     partner_income jsonb,
                                     sent_at timestamp with time zone,
                                     family_size integer NOT NULL,
                                     decision_type text DEFAULT 'NORMAL'::text NOT NULL,
                                     pricing jsonb NOT NULL
);


--
-- Name: fee_decision_number_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fee_decision_number_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fee_decision_part; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_decision_part (
                                          id uuid NOT NULL,
                                          fee_decision_id uuid NOT NULL,
                                          child uuid NOT NULL,
                                          base_fee integer NOT NULL,
                                          sibling_discount integer NOT NULL,
                                          fee_alterations jsonb NOT NULL,
                                          fee integer NOT NULL,
                                          placement_unit uuid NOT NULL,
                                          placement_type text NOT NULL,
                                          service_need text NOT NULL,
                                          date_of_birth date NOT NULL
);


--
-- Name: fridge_child; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fridge_child (
                                     id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                     child_id uuid NOT NULL,
                                     head_of_child uuid NOT NULL,
                                     start_date date NOT NULL,
                                     end_date date NOT NULL,
                                     created timestamp with time zone DEFAULT now(),
                                     updated timestamp with time zone DEFAULT now(),
                                     conflict boolean DEFAULT false NOT NULL,
                                     CONSTRAINT fridge_child_check CHECK ((start_date <= end_date))
);


--
-- Name: fridge_partner; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fridge_partner (
                                       partnership_id uuid NOT NULL,
                                       indx smallint NOT NULL,
                                       person_id uuid NOT NULL,
                                       start_date date NOT NULL,
                                       end_date date NOT NULL,
                                       created timestamp with time zone DEFAULT now(),
                                       updated timestamp with time zone DEFAULT now(),
                                       conflict boolean DEFAULT false NOT NULL,
                                       CONSTRAINT fridge_partner_check CHECK ((start_date <= end_date)),
                                       CONSTRAINT fridge_partner_indx_check CHECK (((indx >= 1) AND (indx <= 2)))
);


--
-- Name: person; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.person (
                               id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                               social_security_number text,
                               first_name text,
                               last_name text,
                               email text,
                               aad_object_id uuid,
                               language text,
                               date_of_birth date NOT NULL,
                               created timestamp with time zone DEFAULT now() NOT NULL,
                               updated timestamp with time zone DEFAULT now() NOT NULL,
                               customer_id bigint DEFAULT nextval('public.customer_id_seq'::regclass) NOT NULL,
                               street_address text DEFAULT ''::character varying NOT NULL,
                               postal_code text DEFAULT ''::character varying NOT NULL,
                               post_office text DEFAULT ''::character varying NOT NULL,
                               nationalities character varying(3)[] DEFAULT '{}'::character varying[] NOT NULL,
                               restricted_details_enabled boolean DEFAULT false,
                               restricted_details_end_date date,
                               phone text DEFAULT NULL::character varying,
                               updated_from_vtj timestamp with time zone,
                               invoicing_street_address text DEFAULT ''::text NOT NULL,
                               invoicing_postal_code text DEFAULT ''::text NOT NULL,
                               invoicing_post_office text DEFAULT ''::text NOT NULL,
                               invoice_recipient_name text DEFAULT ''::text NOT NULL,
                               date_of_death date,
                               residence_code text,
                               force_manual_fee_decisions boolean DEFAULT false NOT NULL
);


--
-- Name: guardian; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.guardian (
                                 guardian_id uuid NOT NULL,
                                 child_id uuid NOT NULL,
                                 created timestamp with time zone DEFAULT now()
);


--
-- Name: holiday; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.holiday (
                                date date NOT NULL,
                                description text
);


--
-- Name: income; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.income (
                               id uuid NOT NULL,
                               person_id uuid NOT NULL,
                               data jsonb NOT NULL,
                               valid_from date NOT NULL,
                               valid_to date,
                               notes text DEFAULT ''::text NOT NULL,
                               updated_at timestamp with time zone NOT NULL,
                               updated_by uuid NOT NULL,
                               effect text DEFAULT 'INCOME'::text NOT NULL,
                               is_entrepreneur boolean DEFAULT false NOT NULL,
                               works_at_echa boolean DEFAULT false NOT NULL
);


--
-- Name: invoice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice (
                                id uuid NOT NULL,
                                status text NOT NULL,
                                number bigint,
                                invoice_date date,
                                due_date date,
                                print_date date,
                                period_start date,
                                period_end date,
                                head_of_family uuid NOT NULL,
                                agreement_type integer,
                                sent_at timestamp with time zone,
                                sent_by uuid
);


--
-- Name: invoice_row; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice_row (
                                    id uuid NOT NULL,
                                    invoice_id uuid NOT NULL,
                                    child uuid NOT NULL,
                                    date_of_birth date NOT NULL,
                                    amount integer NOT NULL,
                                    unit_price integer,
                                    price integer,
                                    period_start date NOT NULL,
                                    period_end date NOT NULL,
                                    product text NOT NULL,
                                    cost_center text NOT NULL,
                                    sub_cost_center text,
                                    description text
);


--
-- Name: placement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.placement (
                                  id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                  created timestamp with time zone DEFAULT now(),
                                  updated timestamp with time zone DEFAULT now(),
                                  type public.placement_type NOT NULL,
                                  child_id uuid NOT NULL,
                                  unit_id uuid NOT NULL,
                                  start_date date NOT NULL,
                                  end_date date NOT NULL,
                                  CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);

--
-- Name: koski_study_right; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.koski_study_right (
                                          id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                          created timestamp with time zone DEFAULT now() NOT NULL,
                                          updated timestamp with time zone DEFAULT now() NOT NULL,
                                          child_id uuid NOT NULL,
                                          unit_id uuid NOT NULL,
                                          type public.koski_study_right_type NOT NULL,
                                          input_data jsonb,
                                          payload jsonb NOT NULL,
                                          version integer NOT NULL,
                                          study_right_oid text,
                                          person_oid text,
                                          void_date date
);



-- Name: language_emphasis; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.language_emphasis (
                                          id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                          start_date date NOT NULL,
                                          end_date date NOT NULL,
                                          created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                          updated timestamp without time zone,
                                          language public.unit_language DEFAULT 'fi'::public.unit_language NOT NULL
);


--
-- Name: placement_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.placement_status (
                                         id bigint NOT NULL,
                                         description character varying(255) NOT NULL
);


--
-- Name: pricing; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pricing (
                                id uuid NOT NULL,
                                valid_from date NOT NULL,
                                valid_to date,
                                multiplier numeric(5,4) NOT NULL,
                                max_threshold_difference integer NOT NULL,
                                min_threshold_2 integer NOT NULL,
                                min_threshold_3 integer NOT NULL,
                                min_threshold_4 integer NOT NULL,
                                min_threshold_5 integer NOT NULL,
                                min_threshold_6 integer NOT NULL,
                                threshold_increase_6_plus integer NOT NULL
);


--
-- Name: service_need; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_need (
                                     id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                     child_id uuid NOT NULL,
                                     start_date date NOT NULL,
                                     end_date date,
                                     hours_per_week numeric(4,2) DEFAULT 0 NOT NULL,
                                     part_day boolean DEFAULT false NOT NULL,
                                     part_week boolean DEFAULT false NOT NULL,
                                     shift_care boolean DEFAULT false NOT NULL,
                                     temporary boolean DEFAULT false NOT NULL,
                                     notes text DEFAULT ''::text NOT NULL,
                                     updated_by uuid NOT NULL,
                                     created timestamp with time zone DEFAULT now() NOT NULL,
                                     updated timestamp with time zone DEFAULT now() NOT NULL,
                                     CONSTRAINT start_date_before_end_date CHECK ((start_date <= end_date))
);


--
-- Name: staff_attendance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff_attendance (
                                         id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                         group_id uuid NOT NULL,
                                         date date NOT NULL,
                                         count numeric NOT NULL,
                                         created_at timestamp with time zone DEFAULT now()
);


--
-- Name: varda_child; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.varda_child (
                                    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                    person_id uuid NOT NULL,
                                    varda_person_id bigint NOT NULL,
                                    varda_person_oid character varying NOT NULL,
                                    varda_child_id bigint,
                                    created_at timestamp with time zone DEFAULT now() NOT NULL,
                                    modified_at timestamp with time zone DEFAULT now() NOT NULL,
                                    uploaded_at timestamp with time zone
);


--
-- Name: varda_decision; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.varda_decision (
                                       id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                       varda_decision_id bigint NOT NULL,
                                       evaka_decision_id uuid,
                                       evaka_placement_id uuid,
                                       created_at timestamp with time zone DEFAULT now() NOT NULL,
                                       uploaded_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: varda_fee_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.varda_fee_data (
                                       id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                       evaka_fee_decision_id uuid,
                                       varda_fee_data_id bigint NOT NULL,
                                       created_at timestamp with time zone DEFAULT now() NOT NULL,
                                       uploaded_at timestamp with time zone DEFAULT now() NOT NULL,
                                       evaka_placement_id uuid
);


--
-- Name: varda_organizer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.varda_organizer (
                                        id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                        organizer text DEFAULT 'Espoo'::text NOT NULL,
                                        varda_organizer_id bigint,
                                        url text,
                                        email text,
                                        phone text,
                                        iban text,
                                        municipality_code text,
                                        created_at timestamp with time zone DEFAULT now() NOT NULL,
                                        updated_at timestamp with time zone DEFAULT now() NOT NULL,
                                        uploaded_at timestamp without time zone,
                                        varda_organizer_oid text
);


--
-- Name: varda_placement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.varda_placement (
                                        id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
                                        varda_placement_id bigint NOT NULL,
                                        evaka_placement_id uuid,
                                        created_at timestamp with time zone DEFAULT now() NOT NULL,
                                        uploaded_at timestamp with time zone DEFAULT now() NOT NULL,
                                        decision_id uuid NOT NULL
);


--
-- Name: varda_unit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.varda_unit (
                                   evaka_daycare_id uuid NOT NULL,
                                   varda_unit_id bigint NOT NULL,
                                   uploaded_at timestamp with time zone NOT NULL,
                                   created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Data for Name: application_origin; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.application_origin (id, description) FROM stdin;
1	Electronic
2	Paper
\.


--
-- Data for Name: application_status; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.application_status (id, description) FROM stdin;
1	Created
2	Sent
4	Additional info required
5	Placement proposed
8	Placement queued
9	Cleared for confirmation
10	Rejected
11	Active
12	Terminated
13	Archived
150	Verified
3	Waiting placement
6	Additional info received
7	Waiting confirmation
200	Processed
300	Cancelled
14	WAITING DECISION
15	WAITING MAILING
16	WAITING_UNIT_CONFIRMATION
\.


--
-- Data for Name: approval_type; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.approval_type (id, description) FROM stdin;
1	Accepted
2	Rejected
\.

--
-- Data for Name: decision_status; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.decision_status (id, description) FROM stdin;
1	Pending
2	Accepted
3	Rejected
\.


--
-- Data for Name: placement_status; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.placement_status (id, description) FROM stdin;
1	Pending
2	Waiting approval
3	Confirmed
4	Rejected
10	Cancelled
11	Capacity rejected
5	Queued
\.


--
-- Name: customer_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.customer_id_seq', 100000, false);


--
-- Name: decision_number_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.decision_number_seq', 1, true);


--
-- Name: fee_decision_number_sequence; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.fee_decision_number_sequence', 1000000000, false);


--
-- Name: absence absence_child_id_date_placement_type_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT absence_child_id_date_placement_type_key UNIQUE (child_id, date, care_type);


--
-- Name: application_form application_form_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_form
    ADD CONSTRAINT application_form_pkey PRIMARY KEY (id);


--
-- Name: application_origin application_origin_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_origin
    ADD CONSTRAINT application_origin_pkey PRIMARY KEY (id);


--
-- Name: application application_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_pkey PRIMARY KEY (id);


--
-- Name: application_status application_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_status
    ADD CONSTRAINT application_status_pkey PRIMARY KEY (id);


--
-- Name: approval_type approval_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.approval_type
    ADD CONSTRAINT approval_type_pkey PRIMARY KEY (id);


--
-- Name: assistance_action assistance_action_no_overlap; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assistance_action
    ADD CONSTRAINT assistance_action_no_overlap EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);


--
-- Name: assistance_action assistance_action_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assistance_action
    ADD CONSTRAINT assistance_action_pkey PRIMARY KEY (id);


--
-- Name: assistance_need assistance_need_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assistance_need
    ADD CONSTRAINT assistance_need_pkey PRIMARY KEY (id);


--
-- Name: care_area care_area_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_area
    ADD CONSTRAINT care_area_pkey PRIMARY KEY (id);


--
-- Name: care_area care_area_short_name_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_area
    ADD CONSTRAINT care_area_short_name_unique UNIQUE (short_name);


--
-- Name: child child_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.child
    ADD CONSTRAINT child_pkey PRIMARY KEY (id);

--
-- Name: daycare_caretaker daycare_caretaker_no_overlap; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_caretaker
    ADD CONSTRAINT daycare_caretaker_no_overlap EXCLUDE USING gist (group_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);


--
-- Name: daycare_caretaker daycare_caretaker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_caretaker
    ADD CONSTRAINT daycare_caretaker_pkey PRIMARY KEY (id);


--
-- Name: daycare_group daycare_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_group
    ADD CONSTRAINT daycare_group_pkey PRIMARY KEY (id);


--
-- Name: daycare_group_placement daycare_group_placement_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_group_placement
    ADD CONSTRAINT daycare_group_placement_pkey PRIMARY KEY (id);


--
-- Name: daycare daycare_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare
    ADD CONSTRAINT daycare_pkey PRIMARY KEY (id);


--
-- Name: decision_status decision_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.decision_status
    ADD CONSTRAINT decision_status_pkey PRIMARY KEY (id);

--
-- Name: employee employee_aad_object_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_aad_object_id_key UNIQUE (aad_object_id);


--
-- Name: employee employee_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_pkey PRIMARY KEY (id);


--
-- Name: backup_care exclude$backup_care_no_overlap; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "exclude$backup_care_no_overlap" EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);


--
-- Name: fee_decision exclude$feedecision_no_overlapping_draft; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT "exclude$feedecision_no_overlapping_draft" EXCLUDE USING gist (head_of_family WITH =, daterange(valid_from, valid_to, '[]'::text) WITH &&) WHERE ((status = 'DRAFT'::text));


--
-- Name: fee_decision exclude$feedecision_no_overlapping_sent; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT "exclude$feedecision_no_overlapping_sent" EXCLUDE USING gist (head_of_family WITH =, daterange(valid_from, valid_to, '[]'::text) WITH &&) WHERE ((status = ANY (ARRAY['SENT'::text, 'WAITING_FOR_SENDING'::text, 'WAITING_FOR_MANUAL_SENDING'::text])));


--
-- Name: fridge_child exclude$fridge_child_no_overlapping_head_of_child; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT "exclude$fridge_child_no_overlapping_head_of_child" EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&) WHERE ((conflict = false));


--
-- Name: fridge_partner exclude$fridge_partner_enforce_monogamy; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT "exclude$fridge_partner_enforce_monogamy" EXCLUDE USING gist (person_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&) WHERE ((conflict = false));


--
-- Name: placement exclude$placement_no_overlap; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT "exclude$placement_no_overlap" EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);



--
-- Name: fridge_child fridge_child_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT fridge_child_pkey PRIMARY KEY (id);


--
-- Name: fridge_partner fridge_partner_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT fridge_partner_pkey PRIMARY KEY (partnership_id, indx);


--
-- Name: language_emphasis language_emphasis_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.language_emphasis
    ADD CONSTRAINT language_emphasis_pkey PRIMARY KEY (id);


--
-- Name: legacy_placement legacy_placement_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legacy_placement
    ADD CONSTRAINT legacy_placement_pkey PRIMARY KEY (id);


--
-- Name: assistance_need no_overlap; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assistance_need
    ADD CONSTRAINT no_overlap EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);


--
-- Name: daycare_group_placement no_overlap_within_daycare_placement; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_group_placement
    ADD CONSTRAINT no_overlap_within_daycare_placement EXCLUDE USING gist (daycare_placement_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);


--
-- Name: income no_overlapping_income; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.income
    ADD CONSTRAINT no_overlapping_income EXCLUDE USING gist (person_id WITH =, daterange(valid_from, COALESCE(valid_to, '2099-12-31'::date), '[]'::text) WITH &&);


--
-- Name: service_need no_overlaps; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_need
    ADD CONSTRAINT no_overlaps EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);


--
-- Name: fridge_partner partnership_end_date_matches; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT partnership_end_date_matches EXCLUDE USING gist (partnership_id WITH =, end_date WITH <>) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: fridge_partner partnership_start_date_matches; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT partnership_start_date_matches EXCLUDE USING gist (partnership_id WITH =, start_date WITH <>) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: person person_identitynumber_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_identitynumber_key UNIQUE (social_security_number);


--
-- Name: person person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);


--
-- Name: absence pk$absence_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT "pk$absence_id" PRIMARY KEY (id);


--
-- Name: application_note pk$application_note; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_note
    ADD CONSTRAINT "pk$application_note" PRIMARY KEY (id);


--
-- Name: async_job pk$async_job; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.async_job
    ADD CONSTRAINT "pk$async_job" PRIMARY KEY (id);


--
-- Name: backup_care pk$backup_care; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "pk$backup_care" PRIMARY KEY (id);


--
-- Name: decision2 pk$decision2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.decision2
    ADD CONSTRAINT "pk$decision2" PRIMARY KEY (id);


--
-- Name: fee_alteration pk$fee_alteration; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_alteration
    ADD CONSTRAINT "pk$fee_alteration" PRIMARY KEY (id);


--
-- Name: holiday pk$holiday; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.holiday
    ADD CONSTRAINT "pk$holiday" PRIMARY KEY (date);


--
-- Name: income pk$income; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.income
    ADD CONSTRAINT "pk$income" PRIMARY KEY (id);


--
-- Name: invoice_row pk$invoice_row; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_row
    ADD CONSTRAINT "pk$invoice_row" PRIMARY KEY (id);


--
-- Name: invoice pk$invoices; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT "pk$invoices" PRIMARY KEY (id);


--
-- Name: koski_study_right pk$koski_study_right; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.koski_study_right
    ADD CONSTRAINT "pk$koski_study_right" PRIMARY KEY (id);


--
-- Name: fee_decision_part pk$payment_decision_part; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision_part
    ADD CONSTRAINT "pk$payment_decision_part" PRIMARY KEY (id);


--
-- Name: fee_decision pk$payment_decisions; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT "pk$payment_decisions" PRIMARY KEY (id);


--
-- Name: placement pk$placement; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT "pk$placement" PRIMARY KEY (id);


--
-- Name: placement_plan pk$placement_plan; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.placement_plan
    ADD CONSTRAINT "pk$placement_plan" PRIMARY KEY (id);


--
-- Name: pricing pk$pricing; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pricing
    ADD CONSTRAINT "pk$pricing" PRIMARY KEY (id);


--
-- Name: staff_attendance pk$staff_attendance_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT "pk$staff_attendance_id" PRIMARY KEY (id);


--
-- Name: varda_decision pk$varda_decision; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_decision
    ADD CONSTRAINT "pk$varda_decision" PRIMARY KEY (id);


--
-- Name: varda_fee_data pk$varda_fee_data; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_fee_data
    ADD CONSTRAINT "pk$varda_fee_data" PRIMARY KEY (id);


--
-- Name: varda_child pk$varda_person; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_child
    ADD CONSTRAINT "pk$varda_person" PRIMARY KEY (id);


--
-- Name: varda_placement pk$varda_placement; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_placement
    ADD CONSTRAINT "pk$varda_placement" PRIMARY KEY (id);


--
-- Name: placement_status placement_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.placement_status
    ADD CONSTRAINT placement_status_pkey PRIMARY KEY (id);


--
-- Name: pricing pricing_daterange_excl; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pricing
    ADD CONSTRAINT pricing_daterange_excl EXCLUDE USING gist (daterange(valid_from, COALESCE(valid_to, '2099-12-31'::date), '[]'::text) WITH &&);


--
-- Name: service_need service_need_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_need
    ADD CONSTRAINT service_need_pkey PRIMARY KEY (id);


--
-- Name: staff_attendance staff_attendance_group_id_date_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT staff_attendance_group_id_date_key UNIQUE (group_id, date);


--
-- Name: care_area uniq$care_area_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_area
    ADD CONSTRAINT "uniq$care_area_name" UNIQUE (name);


--
-- Name: koski_study_right uniq$koski_study_right_child_unit_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.koski_study_right
    ADD CONSTRAINT "uniq$koski_study_right_child_unit_type" UNIQUE (child_id, unit_id, type);


--
-- Name: placement_plan uniq$placement_plan_application_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.placement_plan
    ADD CONSTRAINT "uniq$placement_plan_application_id" UNIQUE (application_id);


--
-- Name: guardian unique_guardian_child; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guardian
    ADD CONSTRAINT unique_guardian_child UNIQUE (guardian_id, child_id);


--
-- Name: invoice unique_invoice_num; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT unique_invoice_num UNIQUE (number);


--
-- Name: fridge_partner unique_partnership_person; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT unique_partnership_person UNIQUE (partnership_id, person_id);


--
-- Name: unit_manager unit_manager_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.unit_manager
    ADD CONSTRAINT unit_manager_pkey PRIMARY KEY (id);


--
-- Name: varda_child varda_child_person_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_child
    ADD CONSTRAINT varda_child_person_id_key UNIQUE (person_id);


--
-- Name: varda_fee_data varda_fee_data_evaka_fee_decision_id_evaka_placement_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_fee_data
    ADD CONSTRAINT varda_fee_data_evaka_fee_decision_id_evaka_placement_id_key UNIQUE (evaka_fee_decision_id, evaka_placement_id);


--
-- Name: varda_organizer varda_organizer_organizer_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_organizer
    ADD CONSTRAINT varda_organizer_organizer_key UNIQUE (organizer);


--
-- Name: varda_organizer varda_organizer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_organizer
    ADD CONSTRAINT varda_organizer_pkey PRIMARY KEY (id);


--
-- Name: varda_unit varda_upload_unique_evaka_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_unit
    ADD CONSTRAINT varda_upload_unique_evaka_id PRIMARY KEY (evaka_daycare_id);


--
-- Name: absence_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX absence_date_idx ON public.absence USING btree (date);


--
-- Name: application_child_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX application_child_id_idx ON public.application USING btree (child_id);


--
-- Name: application_guardian_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX application_guardian_id_idx ON public.application USING btree (guardian_id);


--
-- Name: assistance_action_child_id_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX assistance_action_child_id_start_date_end_date_idx ON public.assistance_action USING btree (child_id, start_date, end_date);


--
-- Name: assistance_need_child_id_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX assistance_need_child_id_start_date_end_date_idx ON public.assistance_need USING btree (child_id, start_date, end_date);


--
-- Name: daycare_acl_daycare_id_employee_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX daycare_acl_daycare_id_employee_id_idx ON public.daycare_acl USING btree (daycare_id, employee_id);


--
-- Name: daycare_acl_employee_id_daycare_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX daycare_acl_employee_id_daycare_id_idx ON public.daycare_acl USING btree (employee_id, daycare_id);


--
-- Name: daycare_care_area_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX daycare_care_area_id_idx ON public.daycare USING btree (care_area_id);


--
-- Name: daycare_caretaker_group_id_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX daycare_caretaker_group_id_start_date_end_date_idx ON public.daycare_caretaker USING btree (group_id, start_date, end_date);


--
-- Name: daycare_group_daycare_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX daycare_group_daycare_id_idx ON public.daycare_group USING btree (daycare_id);


--
-- Name: daycare_group_placement_daycare_group_id_start_date_end_dat_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX daycare_group_placement_daycare_group_id_start_date_end_dat_idx ON public.daycare_group_placement USING btree (daycare_group_id, start_date, end_date);


--
-- Name: decision2_application_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX decision2_application_id_idx ON public.decision2 USING btree (application_id);


--
-- Name: decision2_unit_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX decision2_unit_id_idx ON public.decision2 USING btree (unit_id);


--
-- Name: fee_alteration_person_id_valid_from_valid_to_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fee_alteration_person_id_valid_from_valid_to_idx ON public.fee_alteration USING btree (person_id, valid_from, valid_to);


--
-- Name: fee_decision_head_of_family_valid_from_valid_to_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fee_decision_head_of_family_valid_from_valid_to_idx ON public.fee_decision USING btree (head_of_family, valid_from, valid_to);


--
-- Name: fee_decision_part_fee_decision_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fee_decision_part_fee_decision_id_idx ON public.fee_decision_part USING btree (fee_decision_id);


--
-- Name: fridge_child_child_id_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fridge_child_child_id_start_date_end_date_idx ON public.fridge_child USING btree (child_id, start_date, end_date);


--
-- Name: fridge_child_head_of_child_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fridge_child_head_of_child_start_date_end_date_idx ON public.fridge_child USING btree (head_of_child, start_date, end_date);


--
-- Name: fridge_partner_person_id_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fridge_partner_person_id_start_date_end_date_idx ON public.fridge_partner USING btree (person_id, start_date, end_date);


--
-- Name: idx$application_doc; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$application_doc" ON public.application_form USING gin (document jsonb_path_ops);


--
-- Name: idx$async_job_run_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$async_job_run_at" ON public.async_job USING btree (run_at) WHERE (completed_at IS NULL);


--
-- Name: idx$backup_care_child; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$backup_care_child" ON public.backup_care USING btree (child_id, start_date, end_date);


--
-- Name: idx$backup_care_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$backup_care_group" ON public.backup_care USING btree (group_id, start_date, end_date);


--
-- Name: idx$backup_care_unit; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$backup_care_unit" ON public.backup_care USING btree (unit_id, start_date, end_date);


--
-- Name: idx$fridge_child_conflicts; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$fridge_child_conflicts" ON public.fridge_child USING btree (id) WHERE (conflict IS TRUE);


--
-- Name: idx$fridge_partner_conflicts; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$fridge_partner_conflicts" ON public.fridge_partner USING btree (person_id) WHERE (conflict IS TRUE);


--
-- Name: idx$guardian_child_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$guardian_child_id" ON public.guardian USING btree (child_id);


--
-- Name: idx$guardian_guardian_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$guardian_guardian_id" ON public.guardian USING btree (guardian_id);


--
-- Name: idx$koski_study_right_person_oid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$koski_study_right_person_oid" ON public.koski_study_right USING btree (person_oid);


--
-- Name: idx$koski_study_right_study_right_oid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$koski_study_right_study_right_oid" ON public.koski_study_right USING btree (study_right_oid);


--
-- Name: idx$koski_study_right_unit; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "idx$koski_study_right_unit" ON public.koski_study_right USING btree (unit_id);


--
-- Name: income_person_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX income_person_id_idx ON public.income USING btree (person_id);


--
-- Name: invoice_head_of_family_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX invoice_head_of_family_idx ON public.invoice USING btree (head_of_family);


--
-- Name: invoice_row_invoice_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX invoice_row_invoice_id_idx ON public.invoice_row USING btree (invoice_id);


--
-- Name: legacy_placement_application_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX legacy_placement_application_id_idx ON public.legacy_placement USING btree (application_id);


--
-- Name: placement_child_id_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX placement_child_id_start_date_end_date_idx ON public.placement USING btree (child_id, start_date, end_date);


--
-- Name: placement_plan_unit_id_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX placement_plan_unit_id_start_date_end_date_idx ON public.placement_plan USING btree (unit_id, start_date, end_date);


--
-- Name: placement_unit_id_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX placement_unit_id_start_date_end_date_idx ON public.placement USING btree (unit_id, start_date, end_date);


--
-- Name: service_need_child_id_start_date_end_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX service_need_child_id_start_date_end_date_idx ON public.service_need USING btree (child_id, start_date, end_date);


--
-- Name: staff_attendance_date_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX staff_attendance_date_idx ON public.staff_attendance USING btree (date);


--
-- Name: staff_attendance_group_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX staff_attendance_group_id_idx ON public.staff_attendance USING btree (group_id);


--
-- Name: uniq$application_form_latest; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "uniq$application_form_latest" ON public.application_form USING btree (application_id) WHERE (latest IS TRUE);


--
-- Name: uniq$application_id_rev; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "uniq$application_id_rev" ON public.application_form USING btree (application_id, revision);


--
-- Name: uniq$decision2_number; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "uniq$decision2_number" ON public.decision2 USING btree (number);


--
-- Name: uniq$fridge_child_no_full_duplicates; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "uniq$fridge_child_no_full_duplicates" ON public.fridge_child USING btree (head_of_child, child_id, start_date, end_date, conflict);



--
-- Name: uniq$person_aad_object_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "uniq$person_aad_object_id" ON public.person USING btree (aad_object_id);


--
-- Name: application set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.application FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: application_form set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.application_form FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: application_note set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.application_note FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: assistance_action set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.assistance_action FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: assistance_need set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.assistance_need FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: backup_care set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.backup_care FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: care_area set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.care_area FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: daycare set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: daycare_acl set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_acl FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: daycare_caretaker set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_caretaker FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: daycare_group_placement set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_group_placement FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: decision2 set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.decision2 FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: employee set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.employee FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: fridge_child set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.fridge_child FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: fridge_partner set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.fridge_partner FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: koski_study_right set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.koski_study_right FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: language_emphasis set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.language_emphasis FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: legacy_placement set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.legacy_placement FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: person set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.person FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: placement set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.placement FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: placement_plan set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.placement_plan FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: service_need set_timestamp; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.service_need FOR EACH ROW EXECUTE PROCEDURE public.trigger_refresh_updated();


--
-- Name: decision2 update_decision_number_sequence; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_decision_number_sequence BEFORE INSERT ON public.decision2 FOR EACH ROW EXECUTE PROCEDURE public.ensure_decision_number_curr_year();


--
-- Name: application application_other_guardian_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_other_guardian_fkey FOREIGN KEY (other_guardian_id) REFERENCES public.person(id);


--
-- Name: fee_decision approved_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT approved_by FOREIGN KEY (approved_by) REFERENCES public.employee(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: decision2 created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.decision2
    ADD CONSTRAINT created_by FOREIGN KEY (created_by) REFERENCES public.employee(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: service_need created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_need
    ADD CONSTRAINT created_by FOREIGN KEY (updated_by) REFERENCES public.employee(id);


--
-- Name: daycare_acl daycare_acl_employee_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_acl
    ADD CONSTRAINT daycare_acl_employee_id FOREIGN KEY (employee_id) REFERENCES public.employee(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: daycare_group daycare_group_daycare_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_group
    ADD CONSTRAINT daycare_group_daycare_id_fkey FOREIGN KEY (daycare_id) REFERENCES public.daycare(id) ON DELETE CASCADE;


--
-- Name: varda_unit evaka_daycare_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_unit
    ADD CONSTRAINT evaka_daycare_id_fkey FOREIGN KEY (evaka_daycare_id) REFERENCES public.daycare(id) NOT VALID;


--
-- Name: application_form fk$application_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_form
    ADD CONSTRAINT "fk$application_id" FOREIGN KEY (application_id) REFERENCES public.application(id);


--
-- Name: legacy_placement fk$application_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legacy_placement
    ADD CONSTRAINT "fk$application_id" FOREIGN KEY (application_id) REFERENCES public.application(id);


--
-- Name: placement_plan fk$application_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.placement_plan
    ADD CONSTRAINT "fk$application_id" FOREIGN KEY (application_id) REFERENCES public.application(id);


--
-- Name: decision2 fk$application_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.decision2
    ADD CONSTRAINT "fk$application_id" FOREIGN KEY (application_id) REFERENCES public.application(id);


--
-- Name: application_note fk$application_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_note
    ADD CONSTRAINT "fk$application_id" FOREIGN KEY (application_id) REFERENCES public.application(id) ON DELETE CASCADE;


--
-- Name: application fk$application_origin; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application
    ADD CONSTRAINT "fk$application_origin" FOREIGN KEY (origin) REFERENCES public.application_origin(id);


--
-- Name: application fk$application_status; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application
    ADD CONSTRAINT "fk$application_status" FOREIGN KEY (application_status) REFERENCES public.application_status(id) ON UPDATE CASCADE;


--
-- Name: daycare fk$care_area; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare
    ADD CONSTRAINT "fk$care_area" FOREIGN KEY (care_area_id) REFERENCES public.care_area(id);


--
-- Name: fee_decision_part fk$child; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision_part
    ADD CONSTRAINT "fk$child" FOREIGN KEY (child) REFERENCES public.person(id);


--
-- Name: service_need fk$child_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_need
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;


--
-- Name: assistance_need fk$child_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assistance_need
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;


--
-- Name: invoice_row fk$child_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_row
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child) REFERENCES public.person(id);


--
-- Name: absence fk$child_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id);


--
-- Name: placement fk$child_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id);


--
-- Name: backup_care fk$child_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id);


--
-- Name: assistance_action fk$child_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assistance_action
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;


--
-- Name: guardian fk$child_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guardian
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.person(id);


--
-- Name: koski_study_right fk$child_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.koski_study_right
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id);


--
-- Name: child fk$child_id_person_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.child
    ADD CONSTRAINT "fk$child_id_person_id" FOREIGN KEY (id) REFERENCES public.person(id) ON DELETE CASCADE;


--
-- Name: application_note fk$created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_note
    ADD CONSTRAINT "fk$created_by" FOREIGN KEY (created_by) REFERENCES public.employee(id);


--
-- Name: daycare_acl fk$daycare_acl_daycare_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_acl
    ADD CONSTRAINT "fk$daycare_acl_daycare_id" FOREIGN KEY (daycare_id) REFERENCES public.daycare(id) ON DELETE CASCADE;


--
-- Name: daycare_group_placement fk$daycare_group_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_group_placement
    ADD CONSTRAINT "fk$daycare_group_id" FOREIGN KEY (daycare_group_id) REFERENCES public.daycare_group(id) ON DELETE CASCADE;


--
-- Name: varda_placement fk$decision_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_placement
    ADD CONSTRAINT "fk$decision_id" FOREIGN KEY (decision_id) REFERENCES public.varda_decision(id) ON DELETE CASCADE;


--
-- Name: varda_decision fk$evaka_decision_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_decision
    ADD CONSTRAINT "fk$evaka_decision_id" FOREIGN KEY (evaka_decision_id) REFERENCES public.decision2(id) ON DELETE SET NULL;


--
-- Name: varda_fee_data fk$evaka_fee_decision; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_fee_data
    ADD CONSTRAINT "fk$evaka_fee_decision" FOREIGN KEY (evaka_fee_decision_id) REFERENCES public.fee_decision(id);


--
-- Name: varda_placement fk$evaka_placement_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_placement
    ADD CONSTRAINT "fk$evaka_placement_id" FOREIGN KEY (evaka_placement_id) REFERENCES public.placement(id) ON DELETE SET NULL;


--
-- Name: varda_decision fk$evaka_placement_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_decision
    ADD CONSTRAINT "fk$evaka_placement_id" FOREIGN KEY (evaka_placement_id) REFERENCES public.placement(id) ON DELETE SET NULL;


--
-- Name: varda_fee_data fk$evaka_placement_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_fee_data
    ADD CONSTRAINT "fk$evaka_placement_id" FOREIGN KEY (evaka_placement_id) REFERENCES public.placement(id) ON DELETE SET NULL;


--
-- Name: daycare_caretaker fk$group_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_caretaker
    ADD CONSTRAINT "fk$group_id" FOREIGN KEY (group_id) REFERENCES public.daycare_group(id) ON DELETE CASCADE;


--
-- Name: staff_attendance fk$group_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT "fk$group_id" FOREIGN KEY (group_id) REFERENCES public.daycare_group(id) ON DELETE CASCADE;


--
-- Name: backup_care fk$group_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "fk$group_id" FOREIGN KEY (group_id) REFERENCES public.daycare_group(id);


--
-- Name: guardian fk$guardian_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guardian
    ADD CONSTRAINT "fk$guardian_id" FOREIGN KEY (guardian_id) REFERENCES public.person(id);


--
-- Name: invoice fk$head_of_family; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT "fk$head_of_family" FOREIGN KEY (head_of_family) REFERENCES public.person(id);


--
-- Name: fee_decision fk$head_of_family; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT "fk$head_of_family" FOREIGN KEY (head_of_family) REFERENCES public.person(id);


--
-- Name: invoice_row fk$invoice_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_row
    ADD CONSTRAINT "fk$invoice_id" FOREIGN KEY (invoice_id) REFERENCES public.invoice(id) ON DELETE CASCADE;

--
-- Name: daycare fk$language_emphasis; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare
    ADD CONSTRAINT "fk$language_emphasis" FOREIGN KEY (language_emphasis_id) REFERENCES public.language_emphasis(id);


--
-- Name: fee_decision fk$partner; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT "fk$partner" FOREIGN KEY (partner) REFERENCES public.person(id);


--
-- Name: fee_decision_part fk$payment_decision_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision_part
    ADD CONSTRAINT "fk$payment_decision_id" FOREIGN KEY (fee_decision_id) REFERENCES public.fee_decision(id) ON DELETE CASCADE;


--
-- Name: varda_child fk$person_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.varda_child
    ADD CONSTRAINT "fk$person_id" FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: daycare_group_placement fk$placement_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare_group_placement
    ADD CONSTRAINT "fk$placement_id" FOREIGN KEY (daycare_placement_id) REFERENCES public.placement(id);


--
-- Name: legacy_placement fk$status; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legacy_placement
    ADD CONSTRAINT "fk$status" FOREIGN KEY (status) REFERENCES public.placement_status(id);


--
-- Name: fee_decision_part fk$unit_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_decision_part
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (placement_unit) REFERENCES public.daycare(id);


--
-- Name: placement fk$unit_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);


--
-- Name: placement_plan fk$unit_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.placement_plan
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);


--
-- Name: decision2 fk$unit_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.decision2
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);


--
-- Name: backup_care fk$unit_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);


--
-- Name: koski_study_right fk$unit_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.koski_study_right
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);


--
-- Name: daycare fk$unit_manager; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daycare
    ADD CONSTRAINT "fk$unit_manager" FOREIGN KEY (unit_manager_id) REFERENCES public.unit_manager(id);


--
-- Name: assistance_action fk$updated_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assistance_action
    ADD CONSTRAINT "fk$updated_by" FOREIGN KEY (updated_by) REFERENCES public.employee(id);


--
-- Name: application_note fk$updated_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_note
    ADD CONSTRAINT "fk$updated_by" FOREIGN KEY (updated_by) REFERENCES public.employee(id);


--
-- Name: fridge_child fridge_child_child_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT fridge_child_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;


--
-- Name: fridge_child fridge_child_head_of_child_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT fridge_child_head_of_child_fkey FOREIGN KEY (head_of_child) REFERENCES public.person(id) ON DELETE CASCADE;


--
-- Name: fridge_partner fridge_partner_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT fridge_partner_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE CASCADE;


--
-- Name: invoice sent_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT sent_by FOREIGN KEY (sent_by) REFERENCES public.employee(id);


--
-- Name: fee_alteration updated_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_alteration
    ADD CONSTRAINT updated_by FOREIGN KEY (updated_by) REFERENCES public.employee(id);


--
-- Name: assistance_need updated_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assistance_need
    ADD CONSTRAINT updated_by FOREIGN KEY (updated_by) REFERENCES public.employee(id);


--
-- PostgreSQL database dump complete
--

