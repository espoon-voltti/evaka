// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.invoicing.data.parseIncomeDataJson
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.getTotalIncome
import fi.espoo.evaka.invoicing.domain.getTotalIncomeEffect
import fi.espoo.evaka.invoicing.domain.incomeTotal
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import org.jdbi.v3.core.result.RowView
import org.springframework.stereotype.Service

@Service
class FamilyOverviewService(
    private val jsonMapper: JsonMapper,
    private val incomeTypesProvider: IncomeTypesProvider
) {
    fun getFamilyByAdult(tx: Database.Read, clock: EvakaClock, adultId: PersonId): FamilyOverview? {
        val sql =
            """
WITH adult_ids AS
(
    SELECT :id AS id
    
    UNION
    
    SELECT fp2.person_id AS id 
    FROM fridge_partner fp1
    JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id 
        AND fp1.person_id != fp2.person_id 
        AND fp1.conflict = false 
        AND fp2.conflict = false
    WHERE fp1.person_id = :id AND daterange(fp1.start_date, fp1.end_date, '[]') @> :today
)
SELECT 
    p.id,
    p.first_name, 
    p.last_name, 
    p.date_of_birth, 
    p.street_address, 
    p.postal_code,
    p.post_office,
    p.restricted_details_enabled,
    null as head_of_child,
    i.effect as income_effect,
    i.data as income_data
FROM person p 
JOIN adult_ids a on p.id = a.id
LEFT JOIN income i on p.id = i.person_id AND daterange(i.valid_from, i.valid_to, '[]') @> :today

UNION

SELECT
    p.id, 
    p.first_name, 
    p.last_name, 
    p.date_of_birth,
    p.street_address, 
    p.postal_code, 
    p.post_office,
    p.restricted_details_enabled,
    fc.head_of_child as head_of_child,
    i.effect as income_effect,
    i.data as income_data
FROM fridge_child fc
JOIN adult_ids a ON fc.head_of_child = a.id 
JOIN person p ON fc.child_id = p.id
LEFT JOIN income i on p.id = i.person_id AND daterange(i.valid_from, i.valid_to, '[]') @> :today
WHERE daterange(fc.start_date, fc.end_date, '[]') @> :today
AND fc.conflict = FALSE
ORDER BY date_of_birth ASC
"""
        val familyMembersNow =
            tx.createQuery(sql).bind("today", clock.today()).bind("id", adultId).map { row ->
                toFamilyOverviewPerson(row, jsonMapper, incomeTypesProvider)
            }

        val (adults, children) = familyMembersNow.partition { it.headOfChild == null }

        if (adults.isEmpty()) {
            throw NotFound("No adult found")
        }

        val headOfFamily =
            adults.find { adult -> children.any { it.headOfChild == adult.personId } }
                ?: adults.first()

        val partner = adults.find { it.personId != headOfFamily.personId }

        return FamilyOverview(
            headOfFamily = headOfFamily,
            partner = partner,
            children = children,
            totalIncome =
                FamilyOverviewIncome(
                    effect =
                        getTotalIncomeEffect(
                            partner != null,
                            headOfFamily.income?.effect,
                            partner?.income?.effect
                        ),
                    total =
                        getTotalIncome(
                            partner != null,
                            headOfFamily.income?.effect,
                            headOfFamily.income?.total,
                            partner?.income?.effect,
                            partner?.income?.total
                        )
                )
        )
    }
}

data class FamilyOverview(
    val headOfFamily: FamilyOverviewPerson,
    @JsonInclude(JsonInclude.Include.NON_NULL) val partner: FamilyOverviewPerson?,
    val children: List<FamilyOverviewPerson>,
    val totalIncome: FamilyOverviewIncome?
)

data class FamilyOverviewPerson(
    val personId: PersonId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val restrictedDetailsEnabled: Boolean,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    @JsonInclude(JsonInclude.Include.NON_NULL) val headOfChild: PersonId?,
    val income: FamilyOverviewIncome?
)

data class FamilyOverviewIncome(
    @JsonInclude(JsonInclude.Include.NON_NULL) val effect: IncomeEffect?,
    @JsonInclude(JsonInclude.Include.NON_NULL) val total: Int?
)

fun toFamilyOverviewPerson(
    row: RowView,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider
): FamilyOverviewPerson {
    return FamilyOverviewPerson(
        personId = row.mapColumn("id"),
        firstName = row.mapColumn("first_name"),
        lastName = row.mapColumn("last_name"),
        dateOfBirth = row.mapColumn("date_of_birth"),
        restrictedDetailsEnabled = row.mapColumn("restricted_details_enabled"),
        streetAddress = row.mapColumn("street_address"),
        postalCode = row.mapColumn("postal_code"),
        postOffice = row.mapColumn("post_office"),
        headOfChild = row.mapColumn("head_of_child"),
        income =
            FamilyOverviewIncome(
                effect = row.mapColumn("income_effect"),
                total =
                    row.mapColumn<String?>("income_data")?.let {
                        incomeTotal(parseIncomeDataJson(it, jsonMapper, incomeTypesProvider.get()))
                    }
            )
    )
}
