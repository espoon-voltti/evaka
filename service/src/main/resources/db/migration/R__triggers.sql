CREATE OR REPLACE FUNCTION public.trigger_refresh_updated() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.updated = OLD.updated THEN
        NEW.updated = NOW();
    END IF;
    RETURN NEW;
END;
$$;
