// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.EmployeeWithName
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionChildDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.PersonBasic
import fi.espoo.evaka.invoicing.domain.PersonDetailed
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.argument.ArgumentFactory
import org.jdbi.v3.core.argument.NullArgument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.generic.GenericTypes
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.RowViewMapper
import org.jdbi.v3.core.statement.StatementContext
import org.postgresql.geometric.PGpoint
import org.postgresql.util.PGobject
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import java.util.function.Function

val finiteDateRangeArgumentFactory = pgObjectArgumentFactory<FiniteDateRange> {
    PGobject().apply {
        type = "daterange"
        value = "[${it.start},${it.end}]"
    }
}
val dateRangeArgumentFactory = pgObjectArgumentFactory<DateRange> {
    PGobject().apply {
        type = "daterange"
        value = "[${it.start},${it.end ?: ""}]"
    }
}

val coordinateArgumentFactory = pgObjectArgumentFactory<Coordinate> {
    PGpoint().apply {
        y = it.lat
        x = it.lon
    }
}

val identityArgumentFactory = customArgumentFactory<ExternalIdentifier>(Types.VARCHAR) {
    when (it) {
        is ExternalIdentifier.SSN -> CustomStringArgument(it.ssn)
        is ExternalIdentifier.NoID -> null
    }
}

val externalIdArgumentFactory = toStringArgumentFactory<ExternalId>()

val idArgumentFactory = customArgumentFactory<Id<*>>(Types.OTHER) { CustomObjectArgument(it.raw) }

val helsinkiDateTimeArgumentFactory = customArgumentFactory<HelsinkiDateTime>(Types.TIMESTAMP_WITH_TIMEZONE) {
    CustomObjectArgument(it.toZonedDateTime().toOffsetDateTime())
}

val productKeyArgumentFactory = customArgumentFactory<ProductKey>(Types.VARCHAR) { CustomStringArgument(it.value) }

val finiteDateRangeColumnMapper = PgObjectColumnMapper {
    assert(it.type == "daterange")
    it.value?.let { value ->
        val parts = value.trim('[', ')').split(',')
        val start = LocalDate.parse(parts[0])
        val end = LocalDate.parse(parts[1]).minusDays(1)
        FiniteDateRange(start, end)
    }
}

val dateRangeColumnMapper = PgObjectColumnMapper {
    assert(it.type == "daterange")
    it.value?.let { value ->
        val parts = value.trim('[', ')').split(',')
        val start = LocalDate.parse(parts[0])
        val end = if (parts[1].isNotEmpty()) {
            LocalDate.parse(parts[1]).minusDays(1)
        } else {
            null
        }
        DateRange(start, end)
    }
}

val coordinateColumnMapper = PgObjectColumnMapper {
    (it as PGpoint)
    Coordinate(it.y, it.x)
}

val externalIdColumnMapper =
    ColumnMapper { r, columnNumber, _ -> r.getString(columnNumber)?.let { ExternalId.parse(it) } }

val idColumnMapper = ColumnMapper<Id<*>> { r, columnNumber, _ -> r.getObject(columnNumber, UUID::class.java)?.let { Id<DatabaseTable>(it) } }

val helsinkiDateTimeColumnMapper =
    ColumnMapper { r, columnNumber, _ -> r.getObject(columnNumber, OffsetDateTime::class.java)?.let { HelsinkiDateTime.from(it.toInstant()) } }

val productKeyColumnMapper = ColumnMapper { rs, columnNumber, _ -> ProductKey(rs.getString(columnNumber)) }

class CustomArgumentFactory<T>(private val clazz: Class<T>, private val sqlType: Int, private inline val f: (T) -> Argument?) : ArgumentFactory.Preparable {
    override fun prepare(type: Type, config: ConfigRegistry): Optional<Function<Any?, Argument>> = Optional.ofNullable(
        if (clazz.isAssignableFrom(GenericTypes.getErasedType(type))) {
            Function { nullableValue -> clazz.cast(nullableValue)?.let(f) ?: NullArgument(sqlType) }
        } else {
            null
        }
    )
}

class CustomObjectArgument(val value: Any) : Argument {
    override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) =
        statement.setObject(position, value)
    override fun toString(): String = value.toString()
}

class CustomStringArgument(val value: String) : Argument {
    override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) =
        statement.setString(position, value)

    override fun toString(): String = value
}

inline fun <reified T> customArgumentFactory(sqlType: Int, noinline f: (T) -> Argument?): CustomArgumentFactory<T> = CustomArgumentFactory(T::class.java, sqlType, f)

inline fun <reified T> pgObjectArgumentFactory(noinline serializer: (T) -> PGobject) = customArgumentFactory<T>(Types.OTHER) {
    CustomObjectArgument(serializer(it))
}

inline fun <reified T> toStringArgumentFactory() = customArgumentFactory<T>(Types.VARCHAR) {
    CustomStringArgument(it.toString())
}

class PgObjectColumnMapper<T>(private inline val deserializer: (PGobject) -> T?) : ColumnMapper<T> {
    override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): T? = r.getObject(columnNumber)?.let {
        deserializer(it as PGobject)
    }
}

val feeDecisionRowMapper = RowViewMapper { rv ->
    FeeDecision(
        id = rv.mapColumn("id"),
        status = rv.mapColumn("status"),
        decisionNumber = rv.mapColumn("decision_number"),
        decisionType = rv.mapColumn("decision_type"),
        validDuring = rv.mapColumn("valid_during"),
        headOfFamilyId = rv.mapColumn("head_of_family_id"),
        partnerId = rv.mapColumn("partner_id"),
        headOfFamilyIncome = rv.mapJsonColumn("head_of_family_income"),
        partnerIncome = rv.mapJsonColumn("partner_income"),
        familySize = rv.mapColumn("family_size"),
        feeThresholds = rv.mapJsonColumn("fee_thresholds"),
        // child is not nullable so if it's missing there was nothing to join to the decision
        children = rv.mapColumn<ChildId?>("child_id")?.let { childId ->
            listOf(
                FeeDecisionChild(
                    child = ChildWithDateOfBirth(
                        id = childId,
                        dateOfBirth = rv.mapColumn("child_date_of_birth")
                    ),
                    placement = FeeDecisionPlacement(
                        unitId = rv.mapColumn("placement_unit_id"),
                        type = rv.mapColumn("placement_type"),
                    ),
                    serviceNeed = FeeDecisionServiceNeed(
                        feeCoefficient = rv.mapColumn("service_need_fee_coefficient"),
                        descriptionFi = rv.mapColumn("service_need_description_fi"),
                        descriptionSv = rv.mapColumn("service_need_description_sv"),
                        missing = rv.mapColumn("service_need_missing"),
                    ),
                    baseFee = rv.mapColumn("base_fee"),
                    siblingDiscount = rv.mapColumn("sibling_discount"),
                    fee = rv.mapColumn("fee"),
                    feeAlterations = rv.mapJsonColumn("fee_alterations"),
                    finalFee = rv.mapColumn("final_fee")
                )
            )
        } ?: emptyList(),
        documentKey = rv.mapColumn("document_key"),
        approvedById = rv.mapColumn("approved_by_id"),
        approvedAt = rv.mapColumn("approved_at"),
        decisionHandlerId = rv.mapColumn("decision_handler_id"),
        sentAt = rv.mapColumn("sent_at"),
        created = rv.mapColumn("created")
    )
}

val feeDecisionDetailedRowMapper = RowViewMapper { rv ->
    FeeDecisionDetailed(
        id = rv.mapColumn("id"),
        status = rv.mapColumn("status"),
        decisionNumber = rv.mapColumn("decision_number"),
        decisionType = rv.mapColumn("decision_type"),
        validDuring = rv.mapColumn("valid_during"),
        headOfFamily = PersonDetailed(
            id = rv.mapColumn("head_of_family_id"),
            dateOfBirth = rv.mapColumn("head_date_of_birth"),
            firstName = rv.mapColumn("head_first_name"),
            lastName = rv.mapColumn("head_last_name"),
            ssn = rv.mapColumn("head_ssn"),
            streetAddress = rv.mapColumn("head_street_address"),
            postalCode = rv.mapColumn("head_postal_code"),
            postOffice = rv.mapColumn("head_post_office"),
            language = rv.mapColumn("head_language"),
            restrictedDetailsEnabled = rv.mapColumn("head_restricted_details_enabled"),
            forceManualFeeDecisions = rv.mapColumn("head_force_manual_fee_decisions")
        ),
        partner = rv.mapColumn<PersonId?>("partner_id")?.let { id ->
            PersonDetailed(
                id = id,
                dateOfBirth = rv.mapColumn("partner_date_of_birth"),
                firstName = rv.mapColumn("partner_first_name"),
                lastName = rv.mapColumn("partner_last_name"),
                ssn = rv.mapColumn("partner_ssn"),
                streetAddress = rv.mapColumn("partner_street_address"),
                postalCode = rv.mapColumn("partner_postal_code"),
                postOffice = rv.mapColumn("partner_post_office"),
                restrictedDetailsEnabled = rv.mapColumn("partner_restricted_details_enabled")
            )
        },
        headOfFamilyIncome = rv.mapJsonColumn("head_of_family_income"),
        partnerIncome = rv.mapJsonColumn("partner_income"),
        familySize = rv.mapColumn("family_size"),
        feeThresholds = rv.mapJsonColumn("fee_thresholds"),
        // child is not nullable so if it's missing there was nothing to join to the decision
        children = rv.mapColumn<PersonId?>("child_id")?.let { childId ->
            listOf(
                FeeDecisionChildDetailed(
                    child = PersonDetailed(
                        id = childId,
                        dateOfBirth = rv.mapColumn("child_date_of_birth"),
                        firstName = rv.mapColumn("child_first_name"),
                        lastName = rv.mapColumn("child_last_name"),
                        ssn = rv.mapColumn("child_ssn"),
                        streetAddress = rv.mapColumn("child_address"),
                        postalCode = rv.mapColumn("child_postal_code"),
                        postOffice = rv.mapColumn("child_post_office"),
                        restrictedDetailsEnabled = rv.mapColumn("child_restricted_details_enabled")
                    ),
                    placementType = rv.mapColumn("placement_type"),
                    placementUnit = UnitData(
                        id = rv.mapColumn("placement_unit_id"),
                        name = rv.mapColumn("placement_unit_name"),
                        language = rv.mapColumn("placement_unit_lang"),
                        areaId = rv.mapColumn("placement_unit_area_id"),
                        areaName = rv.mapColumn("placement_unit_area_name")
                    ),
                    serviceNeedFeeCoefficient = rv.mapColumn("service_need_fee_coefficient"),
                    serviceNeedDescriptionFi = rv.mapColumn("service_need_description_fi"),
                    serviceNeedDescriptionSv = rv.mapColumn("service_need_description_sv"),
                    serviceNeedMissing = rv.mapColumn("service_need_missing"),
                    baseFee = rv.mapColumn("base_fee"),
                    siblingDiscount = rv.mapColumn("sibling_discount"),
                    fee = rv.mapColumn("fee"),
                    feeAlterations = rv.mapJsonColumn("fee_alterations"),
                    finalFee = rv.mapColumn("final_fee")
                )
            )
        } ?: emptyList(),
        documentKey = rv.mapColumn("document_key"),
        approvedBy = rv.mapColumn<EmployeeId?>("approved_by_id")?.let { id ->
            EmployeeWithName(
                id = id,
                firstName = rv.mapColumn("approved_by_first_name"),
                lastName = rv.mapColumn("approved_by_last_name")
            )
        },
        approvedAt = rv.mapColumn("approved_at"),
        created = rv.mapColumn("created"),
        sentAt = rv.mapColumn("sent_at"),
        financeDecisionHandlerFirstName = rv.mapColumn("finance_decision_handler_first_name"),
        financeDecisionHandlerLastName = rv.mapColumn("finance_decision_handler_last_name")
    )
}

val feeDecisionSummaryRowMapper = RowViewMapper { rv ->
    FeeDecisionSummary(
        id = rv.mapColumn("id"),
        status = rv.mapColumn("status"),
        decisionNumber = rv.mapColumn("decision_number"),
        validDuring = rv.mapColumn("valid_during"),
        headOfFamily = PersonBasic(
            id = rv.mapColumn("head_of_family_id"),
            dateOfBirth = rv.mapColumn("head_date_of_birth"),
            firstName = rv.mapColumn("head_first_name"),
            lastName = rv.mapColumn("head_last_name"),
            ssn = rv.mapColumn("head_ssn")
        ),
        // child is not nullable so if it's missing there was nothing to join to the decision
        children = rv.mapColumn<PersonId?>("child_id")?.let { id ->
            listOf(
                PersonBasic(
                    id = id,
                    dateOfBirth = rv.mapColumn("child_date_of_birth"),
                    firstName = rv.mapColumn("child_first_name"),
                    lastName = rv.mapColumn("child_last_name"),
                    ssn = rv.mapColumn("child_ssn")
                )
            )
        } ?: emptyList(),
        approvedAt = rv.mapColumn("approved_at"),
        created = rv.mapColumn("created"),
        sentAt = rv.mapColumn("sent_at"),
        finalPrice = rv.mapColumn("sum")
    )
}
