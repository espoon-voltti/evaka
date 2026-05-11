-- SPDX-FileCopyrightText: 2017-2026 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

CREATE TABLE decision_generic_reasoning (
    decision_id  uuid NOT NULL REFERENCES decision(id) ON DELETE CASCADE,
    reasoning_id uuid NOT NULL REFERENCES decision_reasoning_generic(id),
    created_at   timestamp with time zone NOT NULL,
    created_by   uuid NOT NULL REFERENCES evaka_user(id),
    PRIMARY KEY (decision_id, reasoning_id)
);
CREATE INDEX fk$decision_generic_reasoning_reasoning_id ON decision_generic_reasoning (reasoning_id);
CREATE INDEX fk$decision_generic_reasoning_created_by ON decision_generic_reasoning (created_by);

CREATE TABLE decision_individual_reasoning (
    decision_id  uuid NOT NULL REFERENCES decision(id) ON DELETE CASCADE,
    reasoning_id uuid NOT NULL REFERENCES decision_reasoning_individual(id),
    created_at   timestamp with time zone NOT NULL,
    created_by   uuid NOT NULL REFERENCES evaka_user(id),
    PRIMARY KEY (decision_id, reasoning_id)
);
CREATE INDEX fk$decision_individual_reasoning_reasoning_id ON decision_individual_reasoning (reasoning_id);
CREATE INDEX fk$decision_individual_reasoning_created_by ON decision_individual_reasoning (created_by);
