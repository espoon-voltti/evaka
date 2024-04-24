CREATE FUNCTION trigger_guardian_check_guardian_blocklist() RETURNS trigger
LANGUAGE plpgsql STABLE PARALLEL SAFE AS $$
BEGIN
    IF EXISTS (SELECT FROM guardian_blocklist WHERE guardian_id = NEW.guardian_id AND child_id = NEW.child_id) THEN
        RAISE EXCEPTION 'Guardian conflicts with blocklist row (%, %)', NEW.guardian_id, NEW.child_id;
    END IF;
    RETURN NEW;
END
$$;

CREATE FUNCTION trigger_guardian_blocklist_check_guardian() RETURNS trigger
LANGUAGE plpgsql STABLE PARALLEL SAFE AS $$
BEGIN
    IF EXISTS (SELECT FROM guardian WHERE guardian_id = NEW.guardian_id AND child_id = NEW.child_id) THEN
        RAISE EXCEPTION 'Blocklist row conflicts with guardian (%, %)', NEW.guardian_id, NEW.child_id;
    END IF;
    RETURN NEW;
END
$$;

-- These constraint are just soft sanity checks and are not guaranteed to fully prevent conflicts, unless used
-- with strict transaction isolation levels and/or locks

CREATE CONSTRAINT TRIGGER check_blocklist AFTER INSERT OR UPDATE ON guardian
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE PROCEDURE trigger_guardian_check_guardian_blocklist();

CREATE CONSTRAINT TRIGGER check_guardian AFTER INSERT OR UPDATE ON guardian_blocklist
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE PROCEDURE trigger_guardian_blocklist_check_guardian();
