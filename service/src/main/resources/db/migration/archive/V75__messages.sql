CREATE TABLE message_account (
                                 id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
                                 created timestamp with time zone DEFAULT now() NOT NULL,
                                 updated timestamp with time zone DEFAULT now() NOT NULL,
                                 daycare_group_id uuid references daycare_group(id),
                                 employee_id uuid references employee(id),
                                 person_id uuid references person(id)
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_account FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

ALTER TABLE message_account ADD CONSTRAINT message_account_created_for_fk CHECK (
        num_nonnulls(daycare_group_id, employee_id, person_id) = 1
    );
ALTER TABLE message_account ADD CONSTRAINT message_account_daycare_group_uniq UNIQUE (daycare_group_id);
ALTER TABLE message_account ADD CONSTRAINT message_account_employee_uniq UNIQUE (employee_id);
ALTER TABLE message_account ADD CONSTRAINT message_account_person_uniq UNIQUE (person_id);

CREATE INDEX idx$message_account_daycare_group_id ON message_account (daycare_group_id);
CREATE INDEX idx$message_account_employee_id ON message_account (employee_id);
CREATE INDEX idx$message_account_person_id ON message_account (person_id);

CREATE TABLE message (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    title text,
    content text NOT NULL,
    sender_name text NOT NULL
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE INDEX idx$message_created ON message (created);

CREATE TYPE message_type AS ENUM ('MESSAGE', 'BULLETIN');

CREATE TABLE message_thread (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    message_type message_type NOT NULL
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_thread FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE INDEX idx$message_thread_created ON message_thread (created);

CREATE TABLE message_thread_messages (
    message_id uuid references message(id) NOT NULL,
    thread_id uuid references message_thread(id) NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT message_thread_messages_pkey PRIMARY KEY (thread_id, message_id)
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_thread_messages FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE INDEX idx$message_thread_messages_message_id ON message_thread_messages (message_id);

CREATE TABLE message_participants (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    message_id uuid references message(id) NOT NULL,
    account_id uuid references message_account(id) NOT NULL,
    sender boolean NOT NULL,
    read_at timestamp with time zone
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_participants FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE INDEX idx$message_participants_message_id ON message_participants (message_id);
CREATE INDEX idx$message_participants_account_id ON message_participants (account_id);
