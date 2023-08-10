CREATE TABLE fee_decision_v2 AS SELECT * FROM fee_decision;

ALTER TABLE fee_decision_v2 ADD PRIMARY KEY (id);

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT new_fee_decision_approved_by_id_fkey FOREIGN KEY (approved_by_id) REFERENCES employee;

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT new_fee_decision_decision_handler_id_fkey FOREIGN KEY (decision_handler_id) REFERENCES employee;

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT new_fee_decision_head_of_family_id_fkey FOREIGN KEY (head_of_family_id) REFERENCES person;

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT new_fee_decision_partner_id_fkey FOREIGN KEY (partner_id) REFERENCES person;

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT exclude$fee_decision_v2_no_overlapping_draft EXCLUDE USING gist (head_of_family_id WITH =, valid_during WITH &&) WHERE (status = 'DRAFT'::fee_decision_status);

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT exclude$fee_decision_v2_no_overlapping_sent EXCLUDE USING gist (head_of_family_id WITH =, valid_during WITH &&) WHERE (status = ANY ('{SENT,WAITING_FOR_SENDING,WAITING_FOR_MANUAL_SENDING}'::fee_decision_status[]));

ALTER TABLE fee_decision_v2 ADD CONSTRAINT check$head_of_family_is_not_partner CHECK (partner_id IS NULL OR head_of_family_id != partner_id);

create index idx$fee_decision_v2_status
    on fee_decision_v2 (status);

create index idx$fee_decision_v2_head_of_family_id
    on fee_decision_v2 (head_of_family_id);

create index idx$fee_decision_v2_partner_id
    on fee_decision_v2 (partner_id);

create index idx$fee_decision_v2_decision_handler_id
    on fee_decision_v2 (decision_handler_id);

create index idx$fee_decision_v2_valid_during
    on fee_decision_v2 using gist (valid_during);

create index idx$fee_decision_v2_approved_by
    on fee_decision_v2 (approved_by_id)
    where (approved_by_id IS NOT NULL);

create index idx$fee_decision_v2_waiting_for_sending
    on fee_decision_v2 using gist (valid_during)
    where (status = 'WAITING_FOR_SENDING'::fee_decision_status);

create index idx$fee_decision_v2_waiting_for_manual_sending
    on fee_decision_v2 using gist (valid_during)
    where (status = 'WAITING_FOR_MANUAL_SENDING'::fee_decision_status);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON fee_decision_v2 FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();


CREATE TABLE fee_decision_child_v2 AS SELECT * FROM fee_decision_child;

ALTER TABLE fee_decision_child_v2 ADD PRIMARY KEY (id);

ALTER TABLE fee_decision_child_v2 ADD CONSTRAINT unique_child_decision_v2_pair UNIQUE (fee_decision_id, child_id);

ALTER TABLE fee_decision_child_v2
    ADD CONSTRAINT fk$service_need_option_id FOREIGN KEY (service_need_option_id) REFERENCES service_need_option;

ALTER TABLE fee_decision_child_v2
    ADD CONSTRAINT new_fee_decision_child_child_id_fkey FOREIGN KEY (child_id) REFERENCES person;

ALTER TABLE fee_decision_child_v2
    ADD CONSTRAINT new_fee_decision_child_fee_decision_id_fkey FOREIGN KEY (fee_decision_id) REFERENCES fee_decision_v2 ON DELETE CASCADE;

ALTER TABLE fee_decision_child_v2
    ADD CONSTRAINT new_fee_decision_child_placement_unit_id_fkey FOREIGN KEY (placement_unit_id) REFERENCES daycare;

create index idx$fee_decision_child_v2_fee_decision_id
    on fee_decision_child_v2 (fee_decision_id);

create index idx$fee_decision_child_v2_child_id
    on fee_decision_child_v2 (child_id);

create index idx$fee_decision_child_v2_placement_unit_id
    on fee_decision_child_v2 (placement_unit_id);

create index idx$fee_decision_child_v2_service_need_option
    on fee_decision_child_v2 (service_need_option_id);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON fee_decision_child_v2 FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
