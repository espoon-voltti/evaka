ALTER TABLE absence
    ADD COLUMN questionnaire_id uuid
        CONSTRAINT fk$holiday_questionnaire REFERENCES holiday_period_questionnaire ON DELETE SET NULL;

CREATE INDEX idx$absence_questionnaire ON absence (questionnaire_id);

CREATE TABLE holiday_questionnaire_answer (
    id               uuid PRIMARY KEY         DEFAULT ext.uuid_generate_v1mc(),
    created          timestamp with time zone DEFAULT now() NOT NULL,
    updated          timestamp with time zone DEFAULT now() NOT NULL,
    modified_by      uuid
        CONSTRAINT fk$evaka_user REFERENCES evaka_user ON DELETE RESTRICT,
    questionnaire_id uuid
        CONSTRAINT fk$questionnaire REFERENCES holiday_period_questionnaire ON DELETE RESTRICT,
    child_id         uuid
        CONSTRAINT fk$child REFERENCES child ON DELETE RESTRICT,
    fixed_period     daterange
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON holiday_questionnaire_answer FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$questionnaire_answer_questionnaire ON holiday_questionnaire_answer (questionnaire_id);
CREATE UNIQUE INDEX uniq$questionnaire_answer_child_questionnaire ON holiday_questionnaire_answer (child_id, questionnaire_id);
