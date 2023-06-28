ALTER TABLE assistance_need_preschool_decision DROP CONSTRAINT check$non_draft;

ALTER TABLE assistance_need_preschool_decision
    ADD CONSTRAINT check$validated CHECK (
        status = 'NEEDS_WORK' OR (status = 'DRAFT' AND sent_for_decision IS NULL) OR (
            type IS NOT NULL AND
            valid_from IS NOT NULL AND
            selected_unit IS NOT NULL AND
            preparer_1_employee_id IS NOT NULL AND
            decision_maker_employee_id IS NOT NULL
        )
    );
