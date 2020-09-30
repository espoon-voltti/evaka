CREATE TABLE async_job (
  id uuid CONSTRAINT pk$async_job PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
  type text NOT NULL,
  submitted_at timestamp with time zone NOT NULL DEFAULT now(),
  run_at timestamp with time zone NOT NULL DEFAULT now(),
  claimed_at timestamp with time zone,
  claimed_by bigint,
  retry_count integer NOT NULL,
  retry_interval interval NOT NULL,
  started_at timestamp with time zone,
  completed_at timestamp with time zone,
  token text NOT NULL,
  payload jsonb NOT NULL
);

CREATE INDEX idx$async_job_run_at ON async_job (run_at) WHERE completed_at IS NULL;
