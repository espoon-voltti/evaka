WITH updated_answer_values AS (
    SELECT
        id,
        (CASE
             WHEN answer ->> 'type' = 'CHECKBOX_GROUP'
             THEN jsonb_set(answer, '{answer}', (
                 SELECT jsonb_agg(jsonb_build_object('optionId', option_id))
                 FROM jsonb_array_elements(answer -> 'answer') option_id
             ))
             ELSE answer
        END) AS answer
    FROM child_document, jsonb_array_elements(content -> 'answers') answer
), updated_answers as (
    SELECT dc.id, jsonb_agg(u.answer) as answers
    FROM child_document dc
    JOIN updated_answer_values u ON dc.id = u.id
    GROUP BY dc.id
)
UPDATE child_document cd
SET content = jsonb_set(cd.content, '{answers}', uc.answers)
FROM updated_answers uc
WHERE uc.id = cd.id;
