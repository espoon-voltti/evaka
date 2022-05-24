-- drop rarely used index in huge table
DROP INDEX absence_date_idx;


-- index names are global -> improve naming
ALTER INDEX no_overlap RENAME TO exclude$assistance_need_no_overlap;


-- add missing primary key
ALTER TABLE backup_pickup ADD CONSTRAINT pk$backup_pickup PRIMARY KEY (id);


-- add missing primary key
ALTER TABLE daycare_daily_note ADD CONSTRAINT pk$daycare_daily_note PRIMARY KEY (id);


-- index names are global -> improve naming
ALTER INDEX no_overlap_within_daycare_placement RENAME TO exclude$daycare_group_placement_no_overlap;


-- fix confusing legacy "decision2" naming
ALTER INDEX pk$decision2 RENAME TO pk$decision;
ALTER INDEX uniq$decision2_number RENAME TO uniq$decision_number;
ALTER INDEX decision2_application_id_idx RENAME TO idx$decision_application;
ALTER INDEX decision2_unit_id_idx RENAME TO idx$decision_unit;


-- drop pointless btree index on daterange
DROP INDEX idx$fee_decision_valid_during;


-- index names are global -> improve naming
ALTER INDEX no_overlapping_income RENAME TO exclude$income_no_overlap;


-- index names are global -> fix confusing name
ALTER INDEX new_service_need_no_overlap RENAME TO exclude$service_need_no_overlap;


-- drop pointless btree index on daterange
DROP INDEX idx$voucher_value_report_decision_part_realized_period;

CREATE FUNCTION between_start_and_end(daterange, date) RETURNS bool PARALLEL SAFE AS $$
    SELECT $2 BETWEEN coalesce(lower($1), '-infinity') AND coalesce(upper($1), 'infinity') - 1
$$ LANGUAGE sql IMMUTABLE;
COMMENT ON FUNCTION between_start_and_end(daterange, date) IS
'Checks if the given range contains the given date using the `BETWEEN start AND end` operator.
This function is useful when the date parameter refers to a table column with a normal B-tree index';
