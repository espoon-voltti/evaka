CREATE SEQUENCE child_document_decision_number_seq
    START WITH 10000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE child_document_decision
    ADD COLUMN decision_number integer default nextval('child_document_decision_number_seq'::regclass) NOT NULL;
