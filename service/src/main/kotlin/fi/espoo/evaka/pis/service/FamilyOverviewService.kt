// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.data.parseIncomeDataJson
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.getTotalIncome
import fi.espoo.evaka.invoicing.domain.getTotalIncomeEffect
import fi.espoo.evaka.invoicing.domain.incomeTotal
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

@Service
class FamilyOverviewService(
    private val objectMapper: ObjectMapper,
    private val incomeTypesProvider: IncomeTypesProvider
) {
    fun getFamilyByAdult(tx: Database.Read, adultId: PersonId): FamilyOverview? {
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
    WHERE fp1.person_id = :id AND daterange(fp1.start_date, fp1.end_date, '[]') @> current_date
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
LEFT JOIN income i on p.id = i.person_id AND daterange(i.valid_from, i.valid_to, '[]') @> current_date

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
LEFT JOIN income i on p.id = i.person_id AND daterange(i.valid_from, i.valid_to, '[]') @> current_date
WHERE daterange(fc.start_date, fc.end_date, '[]') @> current_date
AND fc.conflict = FALSE
ORDER BY date_of_birth ASC
"""
        val familyMembersNow =
            tx.createQuery(sql)
                .bind("id", adultId)
                .map { rs, _ -> toFamilyOverviewPerson(rs, objectMapper, incomeTypesProvider) }

        val (adults, children) = familyMembersNow.partition { it.headOfChild == null }

        if (adults.isEmpty()) {
            throw NotFound("No adult found")
        }

        val headOfFamily = adults.find { adult -> children.any { it.headOfChild == adult.personId } } ?: adults.first()

        val partner = adults.find { it.personId != headOfFamily.personId }

        return FamilyOverview(
            headOfFamily = headOfFamily,
            partner = partner,
            children = children,
            totalIncome = FamilyOverviewIncome(
                effect = getTotalIncomeEffect(partner != null, headOfFamily.income?.effect, partner?.income?.effect),
                total = getTotalIncome(
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val partner: FamilyOverviewPerson?,
    val children: List<FamilyOverviewPerson>,
    val totalIncome: FamilyOverviewIncome?
)

data class FamilyOverviewPerson(
    val personId: UUID,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val restrictedDetailsEnabled: Boolean,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val headOfChild: UUID?,
    val income: FamilyOverviewIncome?
)

data class FamilyOverviewIncome(
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val effect: IncomeEffect?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val total: Int?
)

fun toFamilyOverviewPerson(
    rs: ResultSet,
    objectMapper: ObjectMapper,
    incomeTypesProvider: IncomeTypesProvider
): FamilyOverviewPerson {
    return FamilyOverviewPerson(
        personId = UUID.fromString(rs.getString("id")),
        firstName = rs.getString("first_name"),
        lastName = rs.getString("last_name"),
        dateOfBirth = rs.getDate("date_of_birth").toLocalDate(),
        restrictedDetailsEnabled = rs.getBoolean("restricted_details_enabled"),
        streetAddress = rs.getString("street_address"),
        postalCode = rs.getString("postal_code"),
        postOffice = rs.getString("post_office"),
        headOfChild = rs.getString("head_of_child")?.let { UUID.fromString(it) },
        income = FamilyOverviewIncome(
            effect = rs.getString("income_effect")?.let { IncomeEffect.valueOf(it) },
            total = rs.getString("income_data")?.let {
                incomeTotal(parseIncomeDataJson(it, objectMapper, incomeTypesProvider.get()))
            }
        )
    )
}
