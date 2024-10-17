UPDATE mobile_device
SET push_notification_categories = push_notification_categories || 'NEW_ABSENCE'::push_notification_category
WHERE cardinality(push_notification_categories) > 0;
