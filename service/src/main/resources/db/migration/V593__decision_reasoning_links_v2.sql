-- move reasoning links to decision table
DROP TABLE IF EXISTS decision_generic_reasoning;
ALTER TABLE decision ADD COLUMN generic_reasoning_id uuid REFERENCES decision_reasoning_generic(id);
CREATE INDEX fk$decision_generic_reasoning_id ON decision (generic_reasoning_id);

-- rename individual reasoning link table and related constraints and indexes
ALTER TABLE decision_individual_reasoning
    DROP CONSTRAINT decision_individual_reasoning_created_by_fkey,
    DROP CONSTRAINT decision_individual_reasoning_decision_id_fkey,
    DROP CONSTRAINT decision_individual_reasoning_reasoning_id_fkey;

ALTER TABLE decision_individual_reasoning RENAME TO decision_reasoning_individual_selection;

ALTER TABLE decision_reasoning_individual_selection
    ADD CONSTRAINT decision_reasoning_individual_selection_created_by_fkey FOREIGN KEY (created_by) REFERENCES evaka_user(id),
    ADD CONSTRAINT decision_reasoning_individual_selection_decision_id_fkey FOREIGN KEY (decision_id) REFERENCES decision(id) ON DELETE CASCADE,
    ADD CONSTRAINT decision_reasoning_individual_selection_reasoning_id_fkey FOREIGN KEY (reasoning_id) REFERENCES decision_reasoning_individual(id);

ALTER INDEX fk$decision_individual_reasoning_reasoning_id RENAME TO fk$decision_reasoning_individual_selection_reasoning_id;
ALTER INDEX fk$decision_individual_reasoning_created_by RENAME TO fk$decision_reasoning_individual_selection_created_by;
ALTER INDEX decision_individual_reasoning_pkey RENAME TO decision_reasoning_individual_selection_pkey;


-- claude suggested version, including the not_null constraint renaming
--  -- move generic reasoning link to a column on decision (a decision has at most one generic reasoning)
--   DROP TABLE IF EXISTS decision_generic_reasoning;
--   ALTER TABLE decision ADD COLUMN generic_reasoning_id uuid REFERENCES decision_reasoning_generic(id);
--   CREATE INDEX fk$decision_generic_reasoning_id ON decision (generic_reasoning_id);

--   -- rename individual reasoning link table and align its constraint/index names
--   ALTER TABLE decision_individual_reasoning RENAME TO decision_reasoning_individual_selection;

--   ALTER TABLE decision_reasoning_individual_selection RENAME CONSTRAINT decision_individual_reasoning_decision_id_fkey TO decision_reasoning_individual_selection_decision_id_fkey;
--   ALTER TABLE decision_reasoning_individual_selection RENAME CONSTRAINT decision_individual_reasoning_reasoning_id_fkey TO decision_reasoning_individual_selection_reasoning_id_fkey;
--   ALTER TABLE decision_reasoning_individual_selection RENAME CONSTRAINT decision_individual_reasoning_created_by_fkey TO decision_reasoning_individual_selection_created_by_fkey;

--   ALTER TABLE decision_reasoning_individual_selection RENAME CONSTRAINT decision_individual_reasoning_decision_id_not_null TO decision_reasoning_individual_selection_decision_id_not_null;
--   ALTER TABLE decision_reasoning_individual_selection RENAME CONSTRAINT decision_individual_reasoning_reasoning_id_not_null TO decision_reasoning_individual_selection_reasoning_id_not_null;
--   ALTER TABLE decision_reasoning_individual_selection RENAME CONSTRAINT decision_individual_reasoning_created_at_not_null TO decision_reasoning_individual_selection_created_at_not_null;
--   ALTER TABLE decision_reasoning_individual_selection RENAME CONSTRAINT decision_individual_reasoning_created_by_not_null TO decision_reasoning_individual_selection_created_by_not_null;

--   ALTER INDEX decision_individual_reasoning_pkey RENAME TO decision_reasoning_individual_selection_pkey;
--   ALTER INDEX fk$decision_individual_reasoning_reasoning_id RENAME TO fk$decision_reasoning_individual_selection_reasoning_id;
--   ALTER INDEX fk$decision_individual_reasoning_created_by RENAME TO fk$decision_reasoning_individual_selection_created_by;
