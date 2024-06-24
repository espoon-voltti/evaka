// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.OfficialLanguage

fun Database.Transaction.insertVasuTemplate(
    name: String,
    valid: FiniteDateRange,
    type: CurriculumType,
    language: OfficialLanguage,
    content: VasuContent
): VasuTemplateId =
    createQuery {
        sql(
            """
                INSERT INTO curriculum_template (valid, type, language, name, content)
                VALUES (${bind(valid)}, ${bind(type)}, ${bind(language)}, ${bind(name)}, ${bind(content)})
                RETURNING id
                """
        )
    }.exactlyOne<VasuTemplateId>()

fun Database.Read.getVasuTemplate(id: VasuTemplateId): VasuTemplate? =
    createQuery {
        sql(
            """
                SELECT ct.*, (SELECT count(*) FROM curriculum_document cd WHERE ct.id = cd.template_id) AS document_count
                FROM curriculum_template ct
                WHERE ct.id = ${bind(id)}
            """
        )
    }.exactlyOneOrNull<VasuTemplate>()

fun Database.Read.getVasuTemplates(
    clock: EvakaClock,
    validOnly: Boolean
): List<VasuTemplateSummary> =
    createQuery {
        sql(
            """
                SELECT 
                    id,
                    name,
                    valid,
                    type,
                    language,
                    (SELECT count(*) FROM curriculum_document cd WHERE cd.template_id = ct.id) AS document_count
                FROM curriculum_template ct
                ${if (validOnly) "WHERE valid @> ${bind(clock.today())}" else ""}
                """
        )
    }.toList<VasuTemplateSummary>()

fun Database.Transaction.updateVasuTemplateContent(
    id: VasuTemplateId,
    content: VasuContent
) {
    createUpdate {
        sql(
            """
                UPDATE curriculum_template
                SET content = ${bind(content)}
                WHERE id = ${bind(id)}
                """
        )
    }.updateExactlyOne()
}

fun Database.Read.getVasuTemplateForUpdate(id: VasuTemplateId): VasuTemplateSummary? =
    createQuery {
        sql(
            """
                SELECT
                    id,
                    name,
                    valid,
                    type,
                    language,
                    (SELECT COUNT(*) FROM curriculum_document cd WHERE cd.template_id = ct.id) AS document_count
                FROM curriculum_template ct
                WHERE id = ${bind(id)}
                FOR UPDATE
                """
        )
    }.exactlyOneOrNull<VasuTemplateSummary>()

fun Database.Transaction.updateVasuTemplate(
    id: VasuTemplateId,
    params: VasuTemplateUpdate
) {
    createUpdate {
        sql(
            """
                UPDATE curriculum_template
                SET name = ${bind(params.name)}, valid = ${bind(params.valid)}
                WHERE id = ${bind(id)}
                """
        )
    }.updateExactlyOne()
}

fun Database.Transaction.deleteUnusedVasuTemplate(id: VasuTemplateId) {
    createUpdate {
        sql(
            """
                DELETE FROM curriculum_template ct
                WHERE ct.id = ${bind(id)} AND NOT EXISTS(SELECT 1 FROM curriculum_document cd WHERE cd.template_id = ct.id)
                """
        )
    }.updateExactlyOne(notFoundMsg = "template $id not found or is in use")
}
