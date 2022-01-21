ALTER TABLE invoice_row ADD COLUMN unit_id uuid CONSTRAINT fk$unit REFERENCES daycare (id);

UPDATE invoice_row SET unit_id = (
    SELECT daycare.id
    FROM daycare
    JOIN care_area ca ON daycare.care_area_id = ca.id
    WHERE daycare.cost_center = invoice_row.cost_center
    AND ca.sub_cost_center = invoice_row.sub_cost_center
    ORDER BY id
    LIMIT 1
);

ALTER TABLE invoice_row ALTER COLUMN unit_id SET NOT NULL;

ALTER TABLE invoice_row RENAME cost_center TO saved_cost_center;
ALTER TABLE invoice_row RENAME sub_cost_center TO saved_sub_cost_center;
ALTER TABLE invoice_row ALTER COLUMN saved_cost_center DROP NOT NULL;

CREATE INDEX idx$invoice_row_unit_id ON invoice_row (unit_id);
