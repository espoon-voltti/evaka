ALTER TABLE application
    ADD COLUMN senttime time;
ALTER TABLE decision
    ADD COLUMN sent_time time;
