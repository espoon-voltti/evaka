// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.invoicing.service.generator.generateAndInsertFeeDecisionsV2
import fi.espoo.evaka.invoicing.service.generator.generateAndInsertVoucherValueDecisionsV2
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPartnersForPerson
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class FinanceDecisionGenerator(
    private val jsonMapper: JsonMapper,
    private val incomeTypesProvider: IncomeTypesProvider,
    private val coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    env: EvakaEnv,
    private val featureConfig: FeatureConfig
) {
    private val feeDecisionMinDate = env.feeDecisionMinDate

    fun scheduleBatchGeneration(tx: Database.Transaction) {
        val inserted =
            tx.createUpdate {
                    sql(
                        """
WITH ids AS (
    SELECT head_of_child AS head_of_family_id
    FROM fridge_child

    UNION

    SELECT head_of_family_id
    FROM fee_decision

    UNION

    SELECT head_of_family_id
    FROM voucher_value_decision
)
INSERT INTO async_job(type, payload, retry_count, retry_interval)
SELECT 'GenerateFinanceDecisions',
       jsonb_build_object(
               'user', NULL,
               'person', jsonb_build_object(
                       'adultId', head_of_family_id,
                       'skipPropagation', true
                   ),
               'dateRange', jsonb_build_object(
                       'start', ${bind(feeDecisionMinDate)},
                       'end', NULL
                   )
           ),
       3,
       interval '5 minutes'
FROM ids
"""
                    )
                }
                .execute()

        logger.info { "Scheduled GenerateFinanceDecisions for $inserted people" }
    }

    fun createRetroactiveFeeDecisions(
        tx: Database.Transaction,
        headOfFamily: PersonId,
        from: LocalDate
    ) {
        generateAndInsertFeeDecisionsV2(
            tx = tx,
            jsonMapper = jsonMapper,
            incomeTypesProvider = incomeTypesProvider,
            coefficientMultiplierProvider = coefficientMultiplierProvider,
            financeMinDate = feeDecisionMinDate,
            headOfFamilyId = headOfFamily,
            retroactiveOverride = from
        )
    }

    fun createRetroactiveValueDecisions(
        tx: Database.Transaction,
        headOfFamily: PersonId,
        from: LocalDate
    ) {
        tx.getChildrenOfHeadOfFamily(headOfFamily, DateRange(from, null)).forEach { childId ->
            generateAndInsertVoucherValueDecisionsV2(
                tx = tx,
                jsonMapper = jsonMapper,
                incomeTypesProvider = incomeTypesProvider,
                coefficientMultiplierProvider = coefficientMultiplierProvider,
                financeMinDate = feeDecisionMinDate,
                valueDecisionCapacityFactorEnabled =
                    featureConfig.valueDecisionCapacityFactorEnabled,
                childId = childId,
                retroactiveOverride = from
            )
        }
    }

    fun generateNewDecisionsForAdult(
        tx: Database.Transaction,
        personId: PersonId,
        skipPropagation: Boolean = false
    ) {
        val adults =
            if (skipPropagation) setOf(personId)
            else getAllPossiblyAffectedAdultsByAdult(tx, personId)

        val children = adults.flatMap { tx.getChildrenOfHeadOfFamily(it) }.toSet()

        adults.forEach { adult ->
            generateAndInsertFeeDecisionsV2(
                tx = tx,
                jsonMapper = jsonMapper,
                incomeTypesProvider = incomeTypesProvider,
                coefficientMultiplierProvider = coefficientMultiplierProvider,
                financeMinDate = feeDecisionMinDate,
                headOfFamilyId = adult
            )
        }

        children.forEach { childId ->
            generateAndInsertVoucherValueDecisionsV2(
                tx = tx,
                jsonMapper = jsonMapper,
                incomeTypesProvider = incomeTypesProvider,
                coefficientMultiplierProvider = coefficientMultiplierProvider,
                financeMinDate = feeDecisionMinDate,
                valueDecisionCapacityFactorEnabled =
                    featureConfig.valueDecisionCapacityFactorEnabled,
                childId = childId
            )
        }
    }

    fun generateNewDecisionsForChild(tx: Database.Transaction, childId: ChildId) {
        getAllPossiblyAffectedAdultsByChild(tx, childId).forEach { adultId ->
            generateAndInsertFeeDecisionsV2(
                tx = tx,
                jsonMapper = jsonMapper,
                incomeTypesProvider = incomeTypesProvider,
                coefficientMultiplierProvider = coefficientMultiplierProvider,
                financeMinDate = feeDecisionMinDate,
                headOfFamilyId = adultId
            )
        }

        generateAndInsertVoucherValueDecisionsV2(
            tx = tx,
            jsonMapper = jsonMapper,
            incomeTypesProvider = incomeTypesProvider,
            coefficientMultiplierProvider = coefficientMultiplierProvider,
            financeMinDate = feeDecisionMinDate,
            valueDecisionCapacityFactorEnabled = featureConfig.valueDecisionCapacityFactorEnabled,
            childId = childId
        )
    }
}

internal fun getAllPossiblyAffectedAdultsByAdult(
    tx: Database.Read,
    adultId: PersonId
): Set<PersonId> {
    val children =
        tx.getParentships(headOfChildId = adultId, childId = null).map { it.childId }.toSet() +
            tx.getChildrenFromFinanceDecisions(adultId)

    val partners =
        tx.getPartnersForPerson(adultId, false).map { it.person.id } +
            tx.getPartnersFromFinanceDecisions(adultId) +
            children.flatMap { child ->
                tx.getParentships(headOfChildId = null, childId = child).map { it.headOfChildId }
            }

    return (partners + adultId).toSet()
}

internal fun getAllPossiblyAffectedAdultsByChild(
    tx: Database.Read,
    childId: PersonId
): Set<PersonId> {
    val heads = tx.getParentships(headOfChildId = null, childId = childId).map { it.headOfChildId }
    val partners =
        heads.flatMap { head -> tx.getPartnersForPerson(head, false) }.map { it.person.id }
    val feeDecisionParents = tx.getParentsFromFinanceDecisions(childId)
    return (heads + partners + feeDecisionParents).toSet()
}

private fun Database.Read.getPartnersFromFinanceDecisions(personId: PersonId) =
    createQuery {
            sql(
                """
        SELECT partner_id FROM fee_decision WHERE head_of_family_id = ${bind(personId)} AND status NOT IN ('DRAFT', 'IGNORED') AND partner_id IS NOT NULL 
        UNION ALL 
        SELECT head_of_family_id FROM fee_decision WHERE partner_id = ${bind(personId)} AND status NOT IN ('DRAFT', 'IGNORED')
        UNION ALL 
        SELECT partner_id FROM voucher_value_decision WHERE head_of_family_id = ${bind(personId)} AND status NOT IN ('DRAFT', 'IGNORED') AND partner_id IS NOT NULL 
        UNION ALL 
        SELECT head_of_family_id FROM voucher_value_decision WHERE partner_id = ${bind(personId)} AND status NOT IN ('DRAFT', 'IGNORED')
        """
            )
        }
        .toSet<PersonId>()

private fun Database.Read.getChildrenFromFinanceDecisions(personId: PersonId) =
    createQuery {
            sql(
                """
        SELECT fdc.child_id 
        FROM fee_decision fd
        JOIN fee_decision_child fdc ON fd.id = fdc.fee_decision_id
        WHERE (fd.head_of_family_id = ${bind(personId)} OR fd.partner_id = ${bind(personId)}) AND fd.status NOT IN ('DRAFT', 'IGNORED')
        
        UNION ALL 
        
        SELECT vvd.child_id 
        FROM voucher_value_decision vvd
        WHERE (vvd.head_of_family_id = ${bind(personId)} OR vvd.partner_id = ${bind(personId)}) AND vvd.status NOT IN ('DRAFT', 'IGNORED')
        """
            )
        }
        .toSet<PersonId>()

private data class FeeDecisionParents(val headOfFamilyId: PersonId, val partnerId: PersonId?)

private fun Database.Read.getParentsFromFinanceDecisions(personId: PersonId) =
    createQuery {
            sql(
                """
        SELECT fd.head_of_family_id, fd.partner_id
        FROM fee_decision fd
        JOIN fee_decision_child fdc ON fd.id = fdc.fee_decision_id
        WHERE fdc.child_id = ${bind(personId)} AND fd.status NOT IN ('DRAFT', 'IGNORED')
        
        UNION ALL 
        
        SELECT vvd.head_of_family_id, vvd.partner_id
        FROM voucher_value_decision vvd 
        WHERE vvd.child_id = ${bind(personId)} AND vvd.status NOT IN ('DRAFT', 'IGNORED')
        """
            )
        }
        .toList<FeeDecisionParents>()
        .flatMap { listOfNotNull(it.headOfFamilyId, it.partnerId) }
        .toSet()

private fun Database.Read.getChildrenOfHeadOfFamily(personId: PersonId, range: DateRange? = null) =
    createQuery {
            sql(
                """
        SELECT child_id
        FROM fridge_child
        WHERE head_of_child = ${bind(personId)} AND NOT conflict 
        ${if (range != null) "AND daterange(start_date, end_date, '[]') && ${bind(range)}" else ""}
        """
            )
        }
        .toSet<ChildId>()
