CREATE SEQUENCE public.assistance_need_preschool_decision_number_seq
    START WITH 1000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TYPE assistance_need_preschool_decision_type AS ENUM ('NEW', 'CONTINUING', 'TERMINATED');

CREATE TABLE assistance_need_preschool_decision
(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    decision_number integer NOT NULL DEFAULT nextval('public.assistance_need_preschool_decision_number_seq'::regclass),
    child_id uuid NOT NULL REFERENCES child ON DELETE RESTRICT,
    status assistance_need_decision_status NOT NULL DEFAULT 'DRAFT',
    language document_language NOT NULL DEFAULT 'FI',

    type assistance_need_preschool_decision_type DEFAULT NULL,
    valid_from date DEFAULT NULL,

    extended_compulsory_education bool NOT NULL DEFAULT false,
    extended_compulsory_education_info text NOT NULL DEFAULT '',

    granted_assistance_service bool NOT NULL DEFAULT false,
    granted_interpretation_service bool NOT NULL DEFAULT false,
    granted_assistive_devices bool NOT NULL DEFAULT false,
    granted_services_basis text NOT NULL DEFAULT '',

    selected_unit uuid REFERENCES daycare ON DELETE RESTRICT DEFAULT NULL,
    primary_group text NOT NULL DEFAULT '',
    decision_basis text NOT NULL DEFAULT '',

    basis_document_pedagogical_report bool NOT NULL DEFAULT false,
    basis_document_psychologist_statement bool NOT NULL DEFAULT false,
    basis_document_social_report bool NOT NULL DEFAULT false,
    basis_document_doctor_statement bool NOT NULL DEFAULT false,
    basis_document_other_or_missing bool NOT NULL DEFAULT false,
    basis_document_other_or_missing_info text NOT NULL DEFAULT '',
    basis_documents_info text NOT NULL DEFAULT '',

    guardians_heard_on date DEFAULT NULL,
    other_representative_heard boolean NOT NULL DEFAULT false,
    other_representative_details text NOT NULL DEFAULT '',
    view_of_guardians text NOT NULL DEFAULT '',

    preparer_1_employee_id uuid REFERENCES employee ON DELETE RESTRICT DEFAULT NULL,
    preparer_1_title text NOT NULL DEFAULT '',
    preparer_1_phone_number text NOT NULL DEFAULT '',
    preparer_2_employee_id uuid REFERENCES employee ON DELETE RESTRICT DEFAULT NULL,
    preparer_2_title text NOT NULL DEFAULT '',
    preparer_2_phone_number text NOT NULL DEFAULT '',
    decision_maker_employee_id uuid REFERENCES employee ON DELETE RESTRICT DEFAULT NULL,
    decision_maker_title text NOT NULL DEFAULT '',

    sent_for_decision date DEFAULT NULL,
    decision_made date DEFAULT NULL,
    decision_maker_has_opened boolean NOT NULL DEFAULT false,
    unread_guardian_ids uuid[] DEFAULT NULL,
    annulment_reason text NOT NULL DEFAULT '',
    document_key text DEFAULT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_need_preschool_decision
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

ALTER TABLE assistance_need_preschool_decision
    ADD CONSTRAINT check$non_draft CHECK (
                status = 'DRAFT' OR (
                type IS NOT NULL AND
                valid_from IS NOT NULL AND
                selected_unit IS NOT NULL AND
                primary_group <> '' AND
                decision_basis <> '' AND
                guardians_heard_on IS NOT NULL AND
                view_of_guardians <> '' AND
                preparer_1_employee_id IS NOT NULL AND
                preparer_1_title <> '' AND
                decision_maker_employee_id IS NOT NULL AND
                decision_maker_title <> '' AND
                sent_for_decision IS NOT NULL AND
                (extended_compulsory_education = false OR extended_compulsory_education_info <> '') AND
                (basis_document_other_or_missing = false OR basis_document_other_or_missing_info <> '') AND
                (other_representative_heard = false OR other_representative_details <> '') AND
                (preparer_2_employee_id IS NULL OR preparer_2_title <> '')
            )
        ),
    ADD CONSTRAINT check$decision_made CHECK (
                status NOT IN ('ACCEPTED', 'REJECTED', 'ANNULLED') OR (
                decision_made IS NOT NULL AND
                unread_guardian_ids IS NOT NULL
            )
        ),
    ADD CONSTRAINT check$annulled CHECK (
        CASE
            WHEN status = 'ANNULLED'
                THEN annulment_reason <> ''
            ELSE annulment_reason = ''
            END
        );

CREATE INDEX idx$assistance_need_preschool_decision_child
    ON assistance_need_preschool_decision (child_id);

CREATE INDEX idx$assistance_need_preschool_decision_unit
    ON assistance_need_preschool_decision (selected_unit)
    WHERE selected_unit IS NOT NULL;

CREATE INDEX idx$assistance_need_preschool_decision_decision_maker
    ON assistance_need_preschool_decision (decision_maker_employee_id)
    WHERE assistance_need_preschool_decision.decision_maker_employee_id IS NOT NULL;

CREATE INDEX idx$assistance_need_preschool_decision_preparer_1
    ON assistance_need_preschool_decision (preparer_1_employee_id)
    WHERE assistance_need_preschool_decision.preparer_1_employee_id IS NOT NULL;

CREATE INDEX idx$assistance_need_preschool_decision_preparer_2
    ON assistance_need_preschool_decision (preparer_2_employee_id)
    WHERE assistance_need_preschool_decision.preparer_2_employee_id IS NOT NULL;

CREATE TABLE assistance_need_preschool_decision_guardian
(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    assistance_need_decision_id uuid NOT NULL REFERENCES assistance_need_preschool_decision ON DELETE CASCADE,
    person_id uuid NOT NULL REFERENCES person ON DELETE RESTRICT,
    is_heard boolean NOT NULL DEFAULT false,
    details text NOT NULL DEFAULT ''
);

CREATE INDEX idx$assistance_need_preschool_decision_guardian_decision_id
    ON assistance_need_preschool_decision_guardian (assistance_need_decision_id);

CREATE INDEX idx$assistance_need_preschool_decision_guardian_person
    ON assistance_need_preschool_decision_guardian (person_id);
