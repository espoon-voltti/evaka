// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.invoicing.calculateIncomeTotal
import fi.espoo.evaka.invoicing.data.parseIncomeDataJson
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.getTotalIncome
import fi.espoo.evaka.invoicing.domain.getTotalIncomeEffect
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.pis.HasDateOfBirth
import fi.espoo.evaka.pis.determineHeadOfFamily
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class FamilyOverviewService(
    private val jsonMapper: JsonMapper,
    private val incomeTypesProvider: IncomeTypesProvider,
    private val coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider
) {
    fun getFamilyByAdult(tx: Database.Read, clock: EvakaClock, adultId: PersonId): FamilyOverview? {
        val today = clock.today()
        val (adults, children) =
            tx.createQuery {
                    sql(
                        """
WITH adult_ids AS
(
    SELECT ${bind(adultId)} AS id
    
    UNION
    
    SELECT fp2.person_id AS id 
    FROM fridge_partner fp1
    JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id 
        AND fp1.person_id != fp2.person_id 
        AND fp1.conflict = false 
        AND fp2.conflict = false
    WHERE fp1.person_id = ${bind(adultId)} AND daterange(fp1.start_date, fp1.end_date, '[]') @> ${bind(today)}
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
LEFT JOIN income i on p.id = i.person_id AND daterange(i.valid_from, i.valid_to, '[]') @> ${bind(today)}

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
LEFT JOIN income i on p.id = i.person_id AND daterange(i.valid_from, i.valid_to, '[]') @> ${bind(today)}
WHERE daterange(fc.start_date, fc.end_date, '[]') @> ${bind(today)}
AND fc.conflict = FALSE
ORDER BY date_of_birth ASC
"""
                    )
                }
                .map {
                    toFamilyOverviewPerson(
                        jsonMapper,
                        incomeTypesProvider,
                        coefficientMultiplierProvider
                    )
                }
                .useIterable { rows -> rows.partition { it.headOfChild == null } }

        if (adults.isEmpty()) {
            throw NotFound("No adult found")
        }

        val supposedHead = adults.first()
        val supposedPartner = adults.getOrNull(1)
        val (headOfFamily, partner) =
            determineHeadOfFamily(
                Pair(supposedHead, children.filter { it.headOfChild == supposedHead.personId }),
                Pair(
                    supposedPartner,
                    children.filter { it.headOfChild == supposedPartner?.personId }
                )
            )

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
    override val dateOfBirth: LocalDate,
    val restrictedDetailsEnabled: Boolean,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    @JsonInclude(JsonInclude.Include.NON_NULL) val headOfChild: PersonId?,
    val income: FamilyOverviewIncome?
) : HasDateOfBirth

data class FamilyOverviewIncome(
    @JsonInclude(JsonInclude.Include.NON_NULL) val effect: IncomeEffect?,
    @JsonInclude(JsonInclude.Include.NON_NULL) val total: Int?
)

fun Row.toFamilyOverviewPerson(
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider
): FamilyOverviewPerson {
    return FamilyOverviewPerson(
        personId = column("id"),
        firstName = column("first_name"),
        lastName = column("last_name"),
        dateOfBirth = column("date_of_birth"),
        restrictedDetailsEnabled = column("restricted_details_enabled"),
        streetAddress = column("street_address"),
        postalCode = column("postal_code"),
        postOffice = column("post_office"),
        headOfChild = column("head_of_child"),
        income =
            FamilyOverviewIncome(
                effect = column("income_effect"),
                total =
                    column<String?>("income_data")?.let {
                        calculateIncomeTotal(
                            parseIncomeDataJson(
                                it,
                                jsonMapper,
                                incomeTypesProvider.get(),
                                coefficientMultiplierProvider
                            ),
                            coefficientMultiplierProvider
                        )
                    }
            )
    )
}
