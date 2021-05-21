CREATE TABLE assistance_action_option (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    value text NOT NULL,
    name_fi text NOT NULL,
    is_other boolean NOT NULL DEFAULT FALSE,
    priority int
);

CREATE UNIQUE INDEX uniq$assistance_action_option_value ON assistance_action_option (value);
CREATE UNIQUE INDEX uniq$assistance_action_option_is_other ON assistance_action_option (is_other) WHERE is_other;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_action_option FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

INSERT INTO assistance_action_option (value, name_fi, is_other, priority)
SELECT action, CASE action
    WHEN 'ASSISTANCE_SERVICE_CHILD'::assistance_action_type THEN 'Avustamispalvelut yhdelle lapselle'
    WHEN 'ASSISTANCE_SERVICE_UNIT'::assistance_action_type THEN 'Avustamispalvelut yksikköön'
    WHEN 'SMALLER_GROUP'::assistance_action_type THEN 'Pienennetty ryhmä'
    WHEN 'SPECIAL_GROUP'::assistance_action_type THEN 'Erityisryhmä'
    WHEN 'PERVASIVE_VEO_SUPPORT'::assistance_action_type THEN 'Laaja-alaisen veon tuki'
    WHEN 'RESOURCE_PERSON'::assistance_action_type THEN 'Resurssihenkilö'
    WHEN 'RATIO_DECREASE'::assistance_action_type THEN 'Suhdeluvun väljennys'
    WHEN 'PERIODICAL_VEO_SUPPORT'::assistance_action_type THEN 'Jaksottainen veon tuki (2–6 kk)'
    WHEN 'OTHER'::assistance_action_type THEN 'Muu tukitoimi'
END, CASE action
    WHEN 'OTHER'::assistance_action_type THEN TRUE
    ELSE FALSE
END, CASE action
    WHEN 'ASSISTANCE_SERVICE_CHILD'::assistance_action_type THEN 10
    WHEN 'ASSISTANCE_SERVICE_UNIT'::assistance_action_type THEN 20
    WHEN 'SMALLER_GROUP'::assistance_action_type THEN 30
    WHEN 'SPECIAL_GROUP'::assistance_action_type THEN 40
    WHEN 'PERVASIVE_VEO_SUPPORT'::assistance_action_type THEN 50
    WHEN 'RESOURCE_PERSON'::assistance_action_type THEN 60
    WHEN 'RATIO_DECREASE'::assistance_action_type THEN 70
    WHEN 'PERIODICAL_VEO_SUPPORT'::assistance_action_type THEN 80
    WHEN 'OTHER'::assistance_action_type THEN 99
END
FROM (SELECT DISTINCT unnest(actions) AS action FROM assistance_action) aa;

CREATE TABLE assistance_action_option_ref (
    action_id uuid NOT NULL REFERENCES assistance_action ON DELETE CASCADE,
    option_id uuid NOT NULL REFERENCES assistance_action_option,
    created timestamp with time zone DEFAULT now() NOT NULL,
    PRIMARY KEY (action_id, option_id)
);

INSERT INTO assistance_action_option_ref (action_id, option_id, created)
SELECT aa.id, aao.id, aa.updated
FROM (SELECT id, updated, unnest(actions) AS action FROM assistance_action) aa
LEFT JOIN assistance_action_option aao ON aao.value::assistance_action_type = aa.action
ON CONFLICT DO NOTHING;

ALTER TABLE assistance_action DROP COLUMN actions;
DROP TYPE assistance_action_type;
