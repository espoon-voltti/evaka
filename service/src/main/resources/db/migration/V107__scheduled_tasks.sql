CREATE TABLE scheduled_tasks (
  task_name text NOT NULL,
  task_instance text NOT NULL,
  task_data bytea,
  execution_time timestamp with time zone NOT NULL,
  picked boolean NOT NULL,
  picked_by text,
  last_success timestamp with time zone,
  last_failure timestamp with time zone,
  consecutive_failures int,
  last_heartbeat timestamp with time zone,
  version bigint NOT NULL,
  PRIMARY KEY (task_name, task_instance)
);
