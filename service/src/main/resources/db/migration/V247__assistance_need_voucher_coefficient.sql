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

CREATE INDEX idx$assistance_need_voucher_coefficient_child_id ON assistance_need_voucher_coefficient (child_id);

ALTER TABLE voucher_value_decision RENAME COLUMN capacity_factor TO assistance_need_coefficient;
