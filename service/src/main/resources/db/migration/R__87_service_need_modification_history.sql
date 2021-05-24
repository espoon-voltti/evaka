CREATE TABLE varda_service_need(
    evaka_service_need_id             uuid unique primary key not null,
    evaka_service_need_option_id      uuid not null,
    evaka_service_need_updated        timestamp with time zone not null,
    evaka_service_need_option_updated timestamp with time zone not null,
    evaka_child_id                    uuid not null REFERENCES person(id),
    varda_decision_id                 text,
    varda_relation_id                 text
);

CREATE INDEX varda_service_need_change_updated_idx_1 ON varda_service_need USING btree (evaka_service_need_updated);
CREATE INDEX varda_service_need_change_updated_idx_2 ON varda_service_need USING btree (evaka_service_need_option_updated);
CREATE INDEX varda_decision_id_idx ON varda_service_need USING btree (varda_decision_id);
