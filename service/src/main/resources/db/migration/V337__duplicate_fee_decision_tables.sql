CREATE TABLE fee_decision_v2 (LIKE fee_decision INCLUDING ALL EXCLUDING STATISTICS);

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT new_fee_decision_approved_by_id_fkey FOREIGN KEY (approved_by_id) REFERENCES employee;

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT new_fee_decision_decision_handler_id_fkey FOREIGN KEY (decision_handler_id) REFERENCES employee;

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT new_fee_decision_head_of_family_id_fkey FOREIGN KEY (head_of_family_id) REFERENCES person;

ALTER TABLE fee_decision_v2
    ADD CONSTRAINT new_fee_decision_partner_id_fkey FOREIGN KEY (partner_id) REFERENCES person;

CREATE TRIGGER set_timestamp BEFORE UPDATE ON fee_decision_v2 FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();


CREATE TABLE fee_decision_child_v2 (LIKE fee_decision_child INCLUDING ALL EXCLUDING STATISTICS);

ALTER TABLE fee_decision_child_v2
    ADD CONSTRAINT fk$service_need_option_id FOREIGN KEY (service_need_option_id) REFERENCES service_need_option;

ALTER TABLE fee_decision_child_v2
    ADD CONSTRAINT new_fee_decision_child_child_id_fkey FOREIGN KEY (child_id) REFERENCES person;

ALTER TABLE fee_decision_child_v2
    ADD CONSTRAINT new_fee_decision_child_fee_decision_id_fkey FOREIGN KEY (fee_decision_id) REFERENCES fee_decision_v2 ON DELETE CASCADE;

ALTER TABLE fee_decision_child_v2
    ADD CONSTRAINT new_fee_decision_child_placement_unit_id_fkey FOREIGN KEY (placement_unit_id) REFERENCES daycare;

CREATE TRIGGER set_timestamp BEFORE UPDATE ON fee_decision_child_v2 FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
