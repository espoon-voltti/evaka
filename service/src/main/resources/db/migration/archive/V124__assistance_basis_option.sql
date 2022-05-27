CREATE TABLE assistance_basis_option (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    value text NOT NULL,
    name_fi text NOT NULL,
    description_fi text,
    display_order int
);

CREATE UNIQUE INDEX uniq$assistance_basis_option_value ON assistance_basis_option (value);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_basis_option FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

INSERT INTO assistance_basis_option (value, name_fi, description_fi, display_order)
SELECT basis, CASE basis
    WHEN 'AUTISM'::assistance_basis THEN 'Autismin kirjo'
    WHEN 'DEVELOPMENTAL_DISABILITY_1'::assistance_basis THEN 'Kehitysvamma 1'
    WHEN 'DEVELOPMENTAL_DISABILITY_2'::assistance_basis THEN 'Kehitysvamma 2'
    WHEN 'FOCUS_CHALLENGE'::assistance_basis THEN 'Keskittymisen / tarkkaavaisuuden vaikeus'
    WHEN 'LINGUISTIC_CHALLENGE'::assistance_basis THEN 'Kielellinen vaikeus'
    WHEN 'DEVELOPMENT_MONITORING'::assistance_basis THEN 'Lapsen kehityksen seuranta'
    WHEN 'DEVELOPMENT_MONITORING_PENDING'::assistance_basis THEN 'Lapsen kehityksen seuranta, tutkimukset kesken'
    WHEN 'MULTI_DISABILITY'::assistance_basis THEN 'Monivammaisuus'
    WHEN 'LONG_TERM_CONDITION'::assistance_basis THEN 'Pitkäaikaissairaus'
    WHEN 'REGULATION_SKILL_CHALLENGE'::assistance_basis THEN 'Säätelytaitojen vaikeus'
    WHEN 'DISABILITY'::assistance_basis THEN 'Vamma (näkö, kuulo, liikunta, muu)'
END, CASE basis
    WHEN 'DEVELOPMENTAL_DISABILITY_2'::assistance_basis THEN 'Käytetään silloin, kun esiopetuksessa oleva lapsi on vaikeasti kehitysvammainen.'
    WHEN 'DEVELOPMENT_MONITORING_PENDING'::assistance_basis THEN 'Lapsi on terveydenhuollon tutkimuksissa, diagnoosi ei ole vielä varmistunut.'
END, CASE basis
    WHEN 'AUTISM'::assistance_basis THEN 10
    WHEN 'DEVELOPMENTAL_DISABILITY_1'::assistance_basis THEN 15
    WHEN 'DEVELOPMENTAL_DISABILITY_2'::assistance_basis THEN 20
    WHEN 'FOCUS_CHALLENGE'::assistance_basis THEN 25
    WHEN 'LINGUISTIC_CHALLENGE'::assistance_basis THEN 30
    WHEN 'DEVELOPMENT_MONITORING'::assistance_basis THEN 35
    WHEN 'DEVELOPMENT_MONITORING_PENDING'::assistance_basis THEN 40
    WHEN 'MULTI_DISABILITY'::assistance_basis THEN 45
    WHEN 'LONG_TERM_CONDITION'::assistance_basis THEN 50
    WHEN 'REGULATION_SKILL_CHALLENGE'::assistance_basis THEN 55
    WHEN 'DISABILITY'::assistance_basis THEN 60
END
FROM (SELECT DISTINCT unnest(bases) AS basis FROM assistance_need) ab WHERE basis <> 'OTHER';

CREATE TABLE assistance_basis_option_ref (
    need_id uuid NOT NULL REFERENCES assistance_need ON DELETE CASCADE,
    option_id uuid NOT NULL REFERENCES assistance_basis_option,
    created timestamp with time zone DEFAULT now() NOT NULL,
    PRIMARY KEY (need_id, option_id)
);

INSERT INTO assistance_basis_option_ref (need_id, option_id, created)
SELECT ab.id, abo.id, ab.updated
FROM (SELECT id, updated, unnest(bases) AS basis FROM assistance_need) ab
LEFT JOIN assistance_basis_option abo ON abo.value::assistance_basis = ab.basis
WHERE basis <> 'OTHER'
ON CONFLICT DO NOTHING;

ALTER TABLE assistance_need DROP COLUMN bases;
DROP TYPE assistance_basis;
