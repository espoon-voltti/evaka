CREATE TABLE assistance_need_voucher_coefficient (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    child_id uuid NOT NULL references child on delete restrict,
    validity_period daterange NOT NULL,
    coefficient numeric(4, 2) NOT NULL,

    CONSTRAINT check$no_validity_overlap EXCLUDE USING gist (
      child_id WITH =,
      validity_period WITH &&
    )
);

-- rename old capacity_factor column
ALTER TABLE voucher_value_decision ADD COLUMN assistance_need_coefficient numeric(4, 2);

UPDATE voucher_value_decision SET assistance_need_coefficient = capacity_factor;

ALTER TABLE voucher_value_decision
  ALTER COLUMN assistance_need_coefficient SET NOT NULL,
  DROP COLUMN capacity_factor;
