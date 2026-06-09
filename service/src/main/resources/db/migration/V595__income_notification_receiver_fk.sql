DELETE FROM income_notification WHERE NOT EXISTS (SELECT 1 FROM person WHERE person.id = income_notification.receiver_id);

ALTER TABLE income_notification ADD CONSTRAINT fk$income_notification_receiver FOREIGN KEY (receiver_id) REFERENCES person (id) ON DELETE CASCADE;
