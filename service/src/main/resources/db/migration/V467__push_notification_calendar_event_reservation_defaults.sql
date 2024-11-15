UPDATE mobile_device
SET push_notification_categories = push_notification_categories || 'CALENDAR_EVENT_RESERVATION'::push_notification_category
WHERE cardinality(push_notification_categories) > 0 AND employee_id IS NULL;
