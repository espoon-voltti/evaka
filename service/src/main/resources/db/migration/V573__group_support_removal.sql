DELETE
FROM preschool_assistance
WHERE level = 'GROUP_SUPPORT'
  AND lower(valid_during) >= '2025-08-01';