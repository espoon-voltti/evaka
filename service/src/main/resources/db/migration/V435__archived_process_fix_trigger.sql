DROP TRIGGER set_timestamp ON archived_process;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON archived_process
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
