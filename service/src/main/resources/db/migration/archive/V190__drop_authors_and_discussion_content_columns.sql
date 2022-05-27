UPDATE curriculum_template SET content = jsonb_build_object(
    'sections', jsonb_build_array(CASE
        WHEN language = 'SV' THEN jsonb_build_object(
            'name', 'Uppgörande av barnets plan för småbarnspedagogik',
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'MULTI_FIELD',
                    'ophKey', NULL,
                    'name', 'Person som ansvarat för uppgörande av planen',
                    'keys', jsonb_build_array(
                        jsonb_build_object('name', 'Förnamn'),
                        jsonb_build_object('name', 'Efternamn'),
                        jsonb_build_object('name', 'Titel'),
                        jsonb_build_object('name', 'Telefonnummer')
                    ),
                    'value', jsonb_build_array('', '', '', '')
                ),
                jsonb_build_object(
                    'type', 'MULTI_FIELD_LIST',
                    'ophKey', NULL,
                    'name', 'Övrig personal/sakkunniga som deltagit i uppgörandet av planen',
                    'keys', jsonb_build_array(
                        jsonb_build_object('name', 'Förnamn'),
                        jsonb_build_object('name', 'Efternamn'),
                        jsonb_build_object('name', 'Titel'),
                        jsonb_build_object('name', 'Telefonnummer')
                    ),
                    'value', jsonb_build_array()
                )
            )
        )
        ELSE jsonb_build_object(
            'name', 'Lapsen varhaiskasvatussuunnitelman laatijat',
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'MULTI_FIELD',
                    'name', 'Laatimisesta vastaava henkilö',
                    'keys', jsonb_build_array(
                        jsonb_build_object('name', 'Etunimi'),
                        jsonb_build_object('name', 'Sukunimi'),
                        jsonb_build_object('name', 'Nimike'),
                        jsonb_build_object('name', 'Puhelinnumero')
                    ),
                    'value', jsonb_build_array('', '', '', '')
                ),
                jsonb_build_object(
                    'type', 'MULTI_FIELD_LIST',
                    'name', 'Muu laatimiseen osallistunut henkilöstö/asiantuntijat',
                    'keys', jsonb_build_array(
                        jsonb_build_object('name', 'Etunimi'),
                        jsonb_build_object('name', 'Sukunimi'),
                        jsonb_build_object('name', 'Nimike'),
                        jsonb_build_object('name', 'Puhelinnumero')
                    ),
                    'value', jsonb_build_array()
                )
            )
        )
    END) || (content->'sections')::jsonb
);

UPDATE curriculum_content SET content = jsonb_build_object(
    'sections', jsonb_build_array(CASE
        WHEN language = 'SV' THEN jsonb_build_object(
            'name', 'Uppgörande av barnets plan för småbarnspedagogik',
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'MULTI_FIELD',
                    'ophKey', NULL,
                    'name', 'Person som ansvarat för uppgörande av planen',
                    'keys', jsonb_build_array(
                        jsonb_build_object('name', 'Förnamn'),
                        jsonb_build_object('name', 'Efternamn'),
                        jsonb_build_object('name', 'Titel'),
                        jsonb_build_object('name', 'Telefonnummer')
                    ),
                    'value', jsonb_build_array(
                        authors_content->'primaryAuthor'->>'name',
                        '',
                        authors_content->'primaryAuthor'->>'title',
                        authors_content->'primaryAuthor'->>'phone'
                    )
                ),
                jsonb_build_object(
                    'type', 'MULTI_FIELD_LIST',
                    'ophKey', NULL,
                    'name', 'Övrig personal/sakkunniga som deltagit i uppgörandet av planen',
                    'keys', jsonb_build_array(
                        jsonb_build_object('name', 'Förnamn'),
                        jsonb_build_object('name', 'Efternamn'),
                        jsonb_build_object('name', 'Titel'),
                        jsonb_build_object('name', 'Telefonnummer')
                    ),
                    'value', jsonb_build_array(
                        jsonb_build_array(
                            authors_content->'otherAuthors'->0->>'name',
                            '',
                            authors_content->'otherAuthors'->0->>'title',
                            authors_content->'otherAuthors'->0->>'phone'
                        )
                    )
                )
            )
        )
        ELSE jsonb_build_object(
            'name', 'Lapsen varhaiskasvatussuunnitelman laatijat',
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'MULTI_FIELD',
                    'name', 'Laatimisesta vastaava henkilö',
                    'keys', jsonb_build_array(
                        jsonb_build_object('name', 'Etunimi'),
                        jsonb_build_object('name', 'Sukunimi'),
                        jsonb_build_object('name', 'Nimike'),
                        jsonb_build_object('name', 'Puhelinnumero')
                    ),
                    'value', jsonb_build_array(
                        authors_content->'primaryAuthor'->>'name',
                        '',
                        authors_content->'primaryAuthor'->>'title',
                        authors_content->'primaryAuthor'->>'phone'
                    )
                ),
                jsonb_build_object(
                    'type', 'MULTI_FIELD_LIST',
                    'name', 'Muu laatimiseen osallistunut henkilöstö/asiantuntijat',
                    'keys', jsonb_build_array(
                        jsonb_build_object('name', 'Etunimi'),
                        jsonb_build_object('name', 'Sukunimi'),
                        jsonb_build_object('name', 'Nimike'),
                        jsonb_build_object('name', 'Puhelinnumero')
                    ),
                    'value', jsonb_build_array(
                        jsonb_build_array(
                            authors_content->'otherAuthors'->0->>'name',
                            '',
                            authors_content->'otherAuthors'->0->>'title',
                            authors_content->'otherAuthors'->0->>'phone'
                        )
                    )
                )
            )
        )
    END) || (curriculum_content.content->'sections')::jsonb
)
FROM curriculum_document
JOIN curriculum_template ON curriculum_template.id = curriculum_document.template_id
WHERE curriculum_content.document_id = curriculum_document.id;

ALTER TABLE curriculum_content DROP COLUMN authors_content;

UPDATE curriculum_template SET content = jsonb_build_object(
    'sections', (content->'sections')::jsonb || jsonb_build_array(CASE
        WHEN language = 'SV' THEN jsonb_build_object(
            'name', 'Samtal om barnets plan för småbarnspedagogik',
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'DATE',
                    'ophKey', NULL,
                    'name', 'Datum för samtalet om barnets plan för småbarnspedagogik',
                    'info', '',
                    'trackedInEvents', TRUE,
                    'nameInEvents', 'Varhaiskasvatussuunnitelmakeskustelu',
                    'value', NULL
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Vårdnadshavare som deltog i samtalet',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Samarbete med vårdnadshavaren/-havarna och synpunkter på innehållet i barnets plan',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                )
            )
        )
        ELSE jsonb_build_object(
            'name', 'Lapsen varhaiskasvatussuunnitelmakeskustelu',
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'PARAGRAPH',
                    'ophKey', NULL,
                    'name', '',
                    'info', '',
                    'title', 'Varhaiskasvatussuunnitelma on käyty läpi yhteistyössä huoltajien kanssa',
                    'paragraph', ''
                ),
                jsonb_build_object(
                    'type', 'DATE',
                    'ophKey', NULL,
                    'name', 'Varhaiskasvatuskeskustelun päivämäärä',
                    'info', '',
                    'trackedInEvents', TRUE,
                    'nameInEvents', 'Varhaiskasvatussuunnitelmakeskustelu',
                    'value', NULL
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Keskusteluun osallistuneet huoltajat',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                )
            )
        )
    END)
);

UPDATE curriculum_content SET content = jsonb_build_object(
    'sections', (curriculum_content.content->'sections')::jsonb || jsonb_build_array(CASE
        WHEN language = 'SV' THEN jsonb_build_object(
            'name', 'Samtal om barnets plan för småbarnspedagogik',
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'DATE',
                    'ophKey', NULL,
                    'name', 'Datum för samtalet om barnets plan för småbarnspedagogik',
                    'info', '',
                    'trackedInEvents', TRUE,
                    'nameInEvents', 'Varhaiskasvatussuunnitelmakeskustelu',
                    'value', curriculum_content.curriculum_discussion_content->'discussionDate'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Vårdnadshavare som deltog i samtalet',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.curriculum_discussion_content->'participants'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Samarbete med vårdnadshavaren/-havarna och synpunkter på innehållet i barnets plan',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.curriculum_discussion_content->'guardianViewsAndCollaboration'
                )
            )
        )
        ELSE jsonb_build_object(
            'name', 'Lapsen varhaiskasvatussuunnitelmakeskustelu',
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'PARAGRAPH',
                    'ophKey', NULL,
                    'name', '',
                    'info', '',
                    'title', 'Varhaiskasvatussuunnitelma on käyty läpi yhteistyössä huoltajien kanssa',
                    'paragraph', ''
                ),
                jsonb_build_object(
                    'type', 'DATE',
                    'ophKey', NULL,
                    'name', 'Varhaiskasvatuskeskustelun päivämäärä',
                    'info', '',
                    'trackedInEvents', TRUE,
                    'nameInEvents', 'Varhaiskasvatussuunnitelmakeskustelu',
                    'value', curriculum_content.curriculum_discussion_content->'discussionDate'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Keskusteluun osallistuneet huoltajat',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.curriculum_discussion_content->'participants'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.curriculum_discussion_content->'guardianViewsAndCollaboration'
                )
            )
        )
    END)
)
FROM curriculum_document
JOIN curriculum_template ON curriculum_template.id = curriculum_document.template_id
WHERE curriculum_content.document_id = curriculum_document.id;

ALTER TABLE curriculum_content DROP COLUMN curriculum_discussion_content;

UPDATE curriculum_template SET content = jsonb_build_object(
    'sections', (content->'sections')::jsonb || jsonb_build_array(CASE
        WHEN language = 'SV' THEN jsonb_build_object(
            'name', 'Utvärdering av genomförandet',
            'hideBeforeReady', TRUE,
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'DATE',
                    'ophKey', NULL,
                    'name', 'Datum för utvärderingssamtalet',
                    'info', '',
                    'trackedInEvents', TRUE,
                    'nameInEvents', 'Arviointikeskustelu',
                    'value', NULL
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Vårdnadshavare som deltog i utvärderingssamtalet',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Samarbete med vårdnadshavaren/-havarna och synpunkter på innehållet i barnets plan',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Utvärdering av hur målen och åtgärderna har genomförts',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                )
            )
        )
        ELSE jsonb_build_object(
            'name', 'Toteutumisen arviointi',
            'hideBeforeReady', TRUE,
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'PARAGRAPH',
                    'ophKey', NULL,
                    'name', '',
                    'info', '',
                    'title', 'Lapsen varhaiskasvatussuunnitelman arviointikeskustelu huoltajien kanssa',
                    'paragraph', ''
                ),
                jsonb_build_object(
                    'type', 'DATE',
                    'ophKey', NULL,
                    'name', 'Arviointikeskustelun päivämäärä',
                    'info', '',
                    'trackedInEvents', TRUE,
                    'nameInEvents', 'Arviointikeskustelu',
                    'value', NULL
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Arviointikeskusteluun osallistuneet huoltajat',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Tavoitteiden ja toimenpiteiden toteutumisen arviointi',
                    'info', '',
                    'multiline', TRUE,
                    'value', ''
                )
            )
        )
    END)
);

UPDATE curriculum_content SET content = jsonb_build_object(
    'sections', (curriculum_content.content->'sections')::jsonb || jsonb_build_array(CASE
        WHEN language = 'SV' THEN jsonb_build_object(
            'name', 'Utvärdering av genomförandet',
            'hideBeforeReady', TRUE,
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'DATE',
                    'ophKey', NULL,
                    'name', 'Datum för utvärderingssamtalet',
                    'info', '',
                    'trackedInEvents', TRUE,
                    'nameInEvents', 'Arviointikeskustelu',
                    'value', curriculum_content.evaluation_discussion_content->'discussionDate'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Vårdnadshavare som deltog i utvärderingssamtalet',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.evaluation_discussion_content->'participants'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Samarbete med vårdnadshavaren/-havarna och synpunkter på innehållet i barnets plan',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.evaluation_discussion_content->'guardianViewsAndCollaboration'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Utvärdering av hur målen och åtgärderna har genomförts',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.evaluation_discussion_content->'evaluation'
                )
            )
        )
        ELSE jsonb_build_object(
            'name', 'Toteutumisen arviointi',
            'hideBeforeReady', TRUE,
            'questions', jsonb_build_array(
                jsonb_build_object(
                    'type', 'PARAGRAPH',
                    'ophKey', NULL,
                    'name', '',
                    'info', '',
                    'title', 'Lapsen varhaiskasvatussuunnitelman arviointikeskustelu huoltajien kanssa',
                    'paragraph', ''
                ),
                jsonb_build_object(
                    'type', 'DATE',
                    'ophKey', NULL,
                    'name', 'Arviointikeskustelun päivämäärä',
                    'info', '',
                    'trackedInEvents', TRUE,
                    'nameInEvents', 'Arviointikeskustelu',
                    'value', curriculum_content.evaluation_discussion_content->'discussionDate'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Arviointikeskusteluun osallistuneet huoltajat',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.evaluation_discussion_content->'participants'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.evaluation_discussion_content->'guardianViewsAndCollaboration'
                ),
                jsonb_build_object(
                    'type', 'TEXT',
                    'ophKey', NULL,
                    'name', 'Tavoitteiden ja toimenpiteiden toteutumisen arviointi',
                    'info', '',
                    'multiline', TRUE,
                    'value', curriculum_content.evaluation_discussion_content->'evaluation'
                )
            )
        )
    END)
)
FROM curriculum_document
JOIN curriculum_template ON curriculum_template.id = curriculum_document.template_id
WHERE curriculum_content.document_id = curriculum_document.id;

ALTER TABLE curriculum_content DROP COLUMN evaluation_discussion_content;
