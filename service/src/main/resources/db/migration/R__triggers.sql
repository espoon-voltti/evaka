CREATE OR REPLACE FUNCTION public.trigger_refresh_updated() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated = NOW();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.trigger_refresh_updated_at() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;
