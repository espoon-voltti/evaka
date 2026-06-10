-- SPDX-FileCopyrightText: 2017-2026 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Use more aggressive autovacuum_analyze settings for top 10 biggest tables.
-- This avoids stale statistics and poor query performance.

DO $$
DECLARE
    target_table TEXT;
    table_list TEXT[] := ARRAY[
        'absence',
        'async_job',
        'attendance_reservation',
        'child_attendance',
        'message',
        'message_recipients',
        'message_thread',
        'message_thread_children',
        'message_thread_participant',
        'staff_attendance_realtime'
    ];
BEGIN
    FOREACH target_table IN ARRAY table_list LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM pg_class
            WHERE relname = target_table
              AND relnamespace = 'public'::regnamespace
              AND 'autovacuum_analyze_scale_factor=0' = ANY(reloptions)
              AND 'autovacuum_analyze_threshold=50000' = ANY(reloptions)
        ) THEN
            EXECUTE format('
                ALTER TABLE public.%I SET (
                    autovacuum_analyze_scale_factor = 0,
                    autovacuum_analyze_threshold = 50000
                )', target_table);
        END IF;
    END LOOP;
END $$;
