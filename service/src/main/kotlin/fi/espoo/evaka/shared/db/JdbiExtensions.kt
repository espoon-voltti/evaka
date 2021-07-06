// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionChildDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.Id
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
import org.jdbi.v3.core.mapper.RowMapper
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

fun feeDecisionRowMapper(mapper: ObjectMapper): RowMapper<FeeDecision> = RowMapper { rs, ctx ->
    FeeDecision(
        id = FeeDecisionId(rs.getUUID("id")),
        status = FeeDecisionStatus.valueOf(rs.getString("status")),
        decisionNumber = rs.getObject("decision_number") as Long?, // getLong returns 0 for null values
        decisionType = rs.getEnum("decision_type"),
        validDuring = ctx.mapColumn(rs, "valid_during"),
        headOfFamily = PersonData.JustId(rs.getUUID("head_of_family_id")),
        partner = rs.getString("partner_id")?.let { PersonData.JustId(UUID.fromString(it)) },
        headOfFamilyIncome = rs.getString("head_of_family_income")?.let { mapper.readValue<DecisionIncome>(it) },
        partnerIncome = rs.getString("partner_income")?.let { mapper.readValue<DecisionIncome>(it) },
        familySize = rs.getInt("family_size"),
        feeThresholds = mapper.readValue(rs.getString("fee_thresholds")),
        // child is not nullable so if it's missing there was nothing to join to the decision
        children = rs.getString("child_id")?.let {
            listOf(
                FeeDecisionChild(
                    child = PersonData.WithDateOfBirth(
                        id = rs.getUUID("child_id"),
                        dateOfBirth = rs.getDate("child_date_of_birth").toLocalDate()
                    ),
                    placement = FeeDecisionPlacement(
                        unit = UnitData.JustId(DaycareId(rs.getUUID("placement_unit_id"))),
                        type = rs.getEnum("placement_type"),
                    ),
                    serviceNeed = FeeDecisionServiceNeed(
                        feeCoefficient = rs.getBigDecimal("service_need_fee_coefficient"),
                        descriptionFi = rs.getString("service_need_description_fi"),
                        descriptionSv = rs.getString("service_need_description_sv"),
                        missing = rs.getBoolean("service_need_missing"),
                    ),
                    baseFee = rs.getInt("base_fee"),
                    siblingDiscount = rs.getInt("sibling_discount"),
                    fee = rs.getInt("fee"),
                    feeAlterations = mapper.readValue(rs.getString("fee_alterations")),
                    finalFee = rs.getInt("final_fee")
                )
            )
        } ?: emptyList(),
        documentKey = rs.getString("document_key"),
        approvedBy = rs.getString("approved_by_id")?.let { PersonData.JustId(UUID.fromString(it)) },
        approvedAt = rs.getTimestamp("approved_at")?.toInstant(),
        decisionHandler = rs.getString("decision_handler_id")?.let { PersonData.JustId(UUID.fromString(it)) },
        sentAt = rs.getTimestamp("sent_at")?.toInstant(),
        created = rs.getTimestamp("created").toInstant()
    )
}

fun feeDecisionDetailedRowMapper(mapper: ObjectMapper): RowMapper<FeeDecisionDetailed> = RowMapper { rs, ctx ->
    FeeDecisionDetailed(
        id = FeeDecisionId(rs.getUUID("id")),
        status = FeeDecisionStatus.valueOf(rs.getString("status")),
        decisionNumber = rs.getObject("decision_number") as Long?, // getLong returns 0 for null values
        decisionType = FeeDecisionType.valueOf(rs.getString("decision_type")),
        validDuring = ctx.mapColumn(rs, "valid_during"),
        headOfFamily = PersonData.Detailed(
            id = UUID.fromString(rs.getString("head_of_family_id")),
            dateOfBirth = rs.getDate("head_date_of_birth").toLocalDate(),
            firstName = rs.getString("head_first_name"),
            lastName = rs.getString("head_last_name"),
            ssn = rs.getString("head_ssn"),
            streetAddress = rs.getString("head_street_address"),
            postalCode = rs.getString("head_postal_code"),
            postOffice = rs.getString("head_post_office"),
            language = rs.getString("head_language"),
            restrictedDetailsEnabled = rs.getBoolean("head_restricted_details_enabled"),
            forceManualFeeDecisions = rs.getBoolean("head_force_manual_fee_decisions")
        ),
        partner = rs.getString("partner_id")?.let { id ->
            PersonData.Detailed(
                id = UUID.fromString(id),
                dateOfBirth = rs.getDate("partner_date_of_birth").toLocalDate(),
                firstName = rs.getString("partner_first_name"),
                lastName = rs.getString("partner_last_name"),
                ssn = rs.getString("partner_ssn"),
                streetAddress = rs.getString("partner_street_address"),
                postalCode = rs.getString("partner_postal_code"),
                postOffice = rs.getString("partner_post_office"),
                restrictedDetailsEnabled = rs.getBoolean("partner_restricted_details_enabled")
            )
        },
        headOfFamilyIncome = rs.getString("head_of_family_income")?.let { mapper.readValue<DecisionIncome>(it) },
        partnerIncome = rs.getString("partner_income")?.let { mapper.readValue<DecisionIncome>(it) },
        familySize = rs.getInt("family_size"),
        feeThresholds = mapper.readValue(rs.getString("fee_thresholds")),
        // child is not nullable so if it's missing there was nothing to join to the decision
        children = rs.getString("child_id")?.let {
            listOf(
                FeeDecisionChildDetailed(
                    child = PersonData.Detailed(
                        id = UUID.fromString(rs.getString("child_id")),
                        dateOfBirth = rs.getDate("child_date_of_birth").toLocalDate(),
                        firstName = rs.getString("child_first_name"),
                        lastName = rs.getString("child_last_name"),
                        ssn = rs.getString("child_ssn"),
                        streetAddress = rs.getString("child_address"),
                        postalCode = rs.getString("child_postal_code"),
                        postOffice = rs.getString("child_post_office"),
                        restrictedDetailsEnabled = rs.getBoolean("child_restricted_details_enabled")
                    ),
                    placementType = rs.getEnum("placement_type"),
                    placementUnit = UnitData.Detailed(
                        id = DaycareId(rs.getUUID("placement_unit_id")),
                        name = rs.getString("placement_unit_name"),
                        language = rs.getString("placement_unit_lang"),
                        areaId = AreaId(rs.getUUID("placement_unit_area_id")),
                        areaName = rs.getString("placement_unit_area_name")
                    ),
                    serviceNeedFeeCoefficient = rs.getBigDecimal("service_need_fee_coefficient"),
                    serviceNeedDescriptionFi = rs.getString("service_need_description_fi"),
                    serviceNeedDescriptionSv = rs.getString("service_need_description_sv"),
                    serviceNeedMissing = rs.getBoolean("service_need_missing"),
                    baseFee = rs.getInt("base_fee"),
                    siblingDiscount = rs.getInt("sibling_discount"),
                    fee = rs.getInt("fee"),
                    feeAlterations = mapper.readValue(rs.getString("fee_alterations")),
                    finalFee = rs.getInt("final_fee")
                )
            )
        } ?: emptyList(),
        documentKey = rs.getString("document_key"),
        approvedBy = rs.getString("approved_by_id")?.let { id ->
            PersonData.WithName(
                id = UUID.fromString(id),
                firstName = rs.getString("approved_by_first_name"),
                lastName = rs.getString("approved_by_last_name")
            )
        },
        approvedAt = rs.getTimestamp("approved_at")?.toInstant(),
        created = rs.getTimestamp("created").toInstant(),
        sentAt = rs.getTimestamp("sent_at")?.toInstant(),
        financeDecisionHandlerFirstName = rs.getString("finance_decision_handler_first_name"),
        financeDecisionHandlerLastName = rs.getString("finance_decision_handler_last_name")
    )
}

val feeDecisionSummaryRowMapper: RowMapper<FeeDecisionSummary> = RowMapper { rs, ctx ->
    FeeDecisionSummary(
        id = FeeDecisionId(rs.getUUID("id")),
        status = rs.getEnum("status"),
        decisionNumber = rs.getObject("decision_number") as Long?, // getLong returns 0 for null values
        validDuring = ctx.mapColumn(rs, "valid_during"),
        headOfFamily = PersonData.Basic(
            id = rs.getUUID("head_of_family_id"),
            dateOfBirth = rs.getObject("head_date_of_birth", LocalDate::class.java),
            firstName = rs.getString("head_first_name"),
            lastName = rs.getString("head_last_name"),
            ssn = rs.getString("head_ssn")
        ),
        // child is not nullable so if it's missing there was nothing to join to the decision
        children = rs.getString("child_id")?.let {
            listOf(
                PersonData.Basic(
                    id = rs.getUUID("child_id"),
                    dateOfBirth = rs.getObject("child_date_of_birth", LocalDate::class.java),
                    firstName = rs.getString("child_first_name"),
                    lastName = rs.getString("child_last_name"),
                    ssn = rs.getString("child_ssn")
                )
            )
        } ?: emptyList(),
        approvedAt = rs.getTimestamp("approved_at")?.toInstant(),
        created = rs.getTimestamp("created").toInstant(),
        sentAt = rs.getTimestamp("sent_at")?.toInstant(),
        finalPrice = rs.getInt("sum")
    )
}
