DROP VIEW IF EXISTS child_document_latest_published_version;

CREATE VIEW child_document_latest_published_version AS
SELECT DISTINCT ON (v.child_document_id)
    v.child_document_id,
    v.id AS version_id,
    v.created_at AS published_at,
    v.version_number,
    v.created_by AS published_by_id,
    u.name AS published_by_name,
    v.published_content,
    v.document_key
FROM child_document_published_version v
JOIN evaka_user u ON v.created_by = u.id
ORDER BY v.child_document_id, v.version_number DESC;
