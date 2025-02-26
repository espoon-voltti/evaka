CREATE TRIGGER set_timestamp BEFORE UPDATE ON person_email_verification FOR EACH ROW EXECUTE FUNCTION trigger_refresh_updated_at();
