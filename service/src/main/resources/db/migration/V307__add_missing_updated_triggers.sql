CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_need_voucher_coefficient FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_need_decision FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE TRIGGER set_timestamp BEFORE UPDATE ON foster_parent FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE TRIGGER set_timestamp BEFORE UPDATE ON application_other_guardian FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE TRIGGER set_timestamp BEFORE UPDATE ON club_term FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
