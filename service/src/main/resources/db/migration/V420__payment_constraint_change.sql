ALTER TABLE public.payment
    DROP CONSTRAINT check$payment_state,
    ADD CONSTRAINT check$payment_state
        CHECK ((status = ANY('{DRAFT,CONFIRMED}'::payment_status[])) OR
               ((unit_business_id IS NOT NULL) AND (unit_business_id <> ''::text) AND (unit_iban IS NOT NULL) AND
                (unit_iban <> ''::text) AND (unit_provider_id IS NOT NULL) AND (unit_provider_id <> ''::text) AND
                (number IS NOT NULL) AND (payment_date IS NOT NULL) AND (due_date IS NOT NULL) AND
                (sent_at IS NOT NULL) AND (sent_by IS NOT NULL)));