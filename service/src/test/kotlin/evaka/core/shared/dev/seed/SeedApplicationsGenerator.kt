// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.dev.seed

import evaka.core.application.Address
import evaka.core.application.ApplicationForm
import evaka.core.application.ApplicationOrigin
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.ChildDetails
import evaka.core.application.ClubDetails
import evaka.core.application.Guardian
import evaka.core.application.PersonBasics
import evaka.core.application.Preferences
import evaka.core.application.PreferredUnit
import evaka.core.application.SecondGuardian
import evaka.core.application.ServiceNeed
import evaka.core.daycare.CareType
import evaka.core.daycare.DaycareDecisionCustomization
import evaka.core.daycare.MailingAddress
import evaka.core.daycare.UnitManager
import evaka.core.daycare.VisitingAddress
import evaka.core.identity.ExternalId
import evaka.core.shared.ApplicationId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.data.DateSet
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevApplicationWithForm
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevClubTerm
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareCaretaker
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDecisionReasoningGeneric
import evaka.core.shared.dev.DevDecisionReasoningIndividual
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPreschoolTerm
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertApplication
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.service.persondetails.MockVtjDataset
import evaka.core.vtjclient.service.persondetails.MockVtjPerson
import evaka.core.vtjclient.service.persondetails.Ssn
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private const val SSN_CHECKSUM_CHARS = "0123456789ABCDEFHJKLMNPRSTUVWXY"

fun generateSsn(dateOfBirth: LocalDate, serial: Int): String {
    val dd = dateOfBirth.dayOfMonth.toString().padStart(2, '0')
    val mm = dateOfBirth.monthValue.toString().padStart(2, '0')
    val yy = (dateOfBirth.year % 100).toString().padStart(2, '0')
    val centurySign = if (dateOfBirth.year >= 2000) 'A' else '-'
    val nnn = serial.toString().padStart(3, '0')
    val checksum = SSN_CHECKSUM_CHARS["$dd$mm$yy$nnn".toLong().mod(31)]
    return "$dd$mm$yy$centurySign$nnn$checksum"
}

data class ChildSpec(
    val kind: SeedKind,
    val dateOfBirth: LocalDate,
    val assistanceNeeded: Boolean,
    val preparatory: Boolean,
)

data class SeededFamily(
    val guardian: DevPerson,
    val otherGuardian: DevPerson?,
    val children: List<DevPerson>,
)

data class GeneratedFamily(val family: SeededFamily, val childSpecs: List<ChildSpec>)

private fun slugify(name: String): String =
    name.lowercase().replace("ä", "a").replace("ö", "o").replace("å", "a")

private fun seedEmail(firstName: String, lastName: String): String =
    "${slugify(firstName)}.${slugify(lastName)}@evaka.test"

private val KIND_CYCLE: List<SeedKind> =
    SeedApplicationsConfig.TYPE_DISTRIBUTION.flatMap { (kind, weight) -> List(weight) { kind } }

private fun applicationKindFor(index: Int): SeedKind = KIND_CYCLE[index % KIND_CYCLE.size]

private fun childBirthDate(kind: SeedKind, index: Int): LocalDate {
    val year =
        if (kind == SeedKind.DAYCARE) SeedApplicationsConfig.DAYCARE_CHILD_BIRTH_YEAR
        else SeedApplicationsConfig.PRESCHOOL_CHILD_BIRTH_YEAR
    val month = (index % 12) + 1
    val day = ((index * 7) % 27) + 1
    return LocalDate.of(year, month, day)
}

private fun planChildren(): List<ChildSpec> {
    val specs = mutableListOf<ChildSpec>()
    var preschoolCount = 0
    for (i in 0 until SeedApplicationsConfig.APPLICATION_COUNT) {
        val kind = applicationKindFor(i)
        val isPreschool = kind != SeedKind.DAYCARE
        specs.add(
            ChildSpec(
                kind = kind,
                dateOfBirth = childBirthDate(kind, i),
                assistanceNeeded = i % SeedApplicationsConfig.ASSISTANCE_NEED_EVERY == 0,
                preparatory =
                    isPreschool && preschoolCount++ % SeedApplicationsConfig.PREPARATORY_EVERY == 0,
            )
        )
    }
    return specs
}

data class SeededUnitIds(val daycareUnitId: DaycareId, val preschoolUnitId: DaycareId)

private fun dueDateFor(kind: SeedKind): LocalDate =
    if (kind == SeedKind.DAYCARE) SeedApplicationsConfig.SENT_DATE.plusMonths(4)
    else SeedApplicationsConfig.SENT_DATE

fun buildApplication(
    child: DevPerson,
    guardian: DevPerson,
    otherGuardian: DevPerson?,
    spec: ChildSpec,
    units: SeededUnitIds,
    isMarkerApplication: Boolean,
): DevApplicationWithForm {
    val isPreschool = spec.kind != SeedKind.DAYCARE
    val isConnectedDaycare = spec.kind == SeedKind.PRESCHOOL_DAYCARE
    val unitId = if (isPreschool) units.preschoolUnitId else units.daycareUnitId
    val unitName =
        if (isPreschool) SeedApplicationsConfig.SEEDED_UNITS[1].name
        else SeedApplicationsConfig.SEEDED_UNITS[0].name
    val sentAt = HelsinkiDateTime.of(SeedApplicationsConfig.SENT_DATE, LocalTime.of(12, 0))
    return DevApplicationWithForm(
        id =
            if (isMarkerApplication) SeedApplicationsConfig.MARKER_APPLICATION_ID
            else ApplicationId(UUID.randomUUID()),
        type = if (isPreschool) ApplicationType.PRESCHOOL else ApplicationType.DAYCARE,
        createdAt = sentAt,
        createdBy = guardian.evakaUserId(),
        modifiedAt = sentAt,
        modifiedBy = guardian.evakaUserId(),
        sentDate = SeedApplicationsConfig.SENT_DATE,
        sentTime = null,
        dueDate = dueDateFor(spec.kind),
        status = ApplicationStatus.SENT,
        guardianId = guardian.id,
        childId = child.id,
        origin = ApplicationOrigin.ELECTRONIC,
        checkedByAdmin = true,
        confidential = true,
        hideFromGuardian = false,
        transferApplication = false,
        otherGuardians = if (otherGuardian != null) listOf(otherGuardian.id) else emptyList(),
        form =
            ApplicationForm(
                child =
                    ChildDetails(
                        person =
                            PersonBasics(
                                firstName = child.firstName,
                                lastName = child.lastName,
                                socialSecurityNumber = child.ssn,
                            ),
                        dateOfBirth = child.dateOfBirth,
                        address = Address(child.streetAddress, child.postalCode, child.postOffice),
                        futureAddress = null,
                        nationality = "FI",
                        language = "fi",
                        allergies = "",
                        diet = "",
                        assistanceNeeded = spec.assistanceNeeded,
                        assistanceDescription = "",
                    ),
                guardian =
                    Guardian(
                        person =
                            PersonBasics(
                                firstName = guardian.firstName,
                                lastName = guardian.lastName,
                                socialSecurityNumber = guardian.ssn,
                            ),
                        address =
                            Address(
                                guardian.streetAddress,
                                guardian.postalCode,
                                guardian.postOffice,
                            ),
                        futureAddress = null,
                        phoneNumber = guardian.phone,
                        email = guardian.email,
                    ),
                secondGuardian =
                    otherGuardian?.let {
                        SecondGuardian(
                            phoneNumber = it.phone,
                            email = it.email ?: "",
                            agreementStatus = null,
                        )
                    },
                otherPartner = null,
                otherChildren = emptyList(),
                preferences =
                    Preferences(
                        preferredUnits = listOf(PreferredUnit(unitId, unitName)),
                        preferredStartDate = SeedApplicationsConfig.PREFERRED_START_DATE,
                        connectedDaycarePreferredStartDate =
                            if (isConnectedDaycare) SeedApplicationsConfig.PREFERRED_START_DATE
                            else null,
                        serviceNeed =
                            if (isPreschool && !isConnectedDaycare) null
                            else
                                ServiceNeed(
                                    startTime = "08:00",
                                    endTime = "16:00",
                                    shiftCare = false,
                                    partTime = false,
                                    serviceNeedOption = null,
                                ),
                        siblingBasis = null,
                        preparatory = spec.preparatory,
                        urgent = false,
                    ),
                maxFeeAccepted = false,
                otherInfo = "",
                clubDetails = ClubDetails(wasOnDaycare = false, wasOnClubCare = false),
            ),
    )
}

fun generateFamilies(): List<GeneratedFamily> {
    val children = planChildren()
    val families = mutableListOf<GeneratedFamily>()
    var serial = 100
    var childIndex = 0
    var familyIndex = 0

    while (childIndex < children.size) {
        val familySize =
            if (familyIndex % SeedApplicationsConfig.TWO_CHILD_FAMILY_EVERY == 0) 2 else 1
        val isMarkerFamily = familyIndex == 0
        val lastName =
            SeedApplicationsConfig.FAMILY_LAST_NAMES[
                    familyIndex % SeedApplicationsConfig.FAMILY_LAST_NAMES.size]
        val guardianFirstName =
            SeedApplicationsConfig.GUARDIAN_FIRST_NAMES[
                    familyIndex % SeedApplicationsConfig.GUARDIAN_FIRST_NAMES.size]
        val streetAddress = "${SeedApplicationsConfig.CITIZEN_STREET_NAME} ${familyIndex * 2 + 1}"

        val markerBirthDate = LocalDate.of(1980, 1, 1)
        val guardianBirthDate =
            if (isMarkerFamily) markerBirthDate
            else LocalDate.of(1988, (familyIndex % 12) + 1, ((familyIndex * 5) % 27) + 1)
        val guardian =
            DevPerson(
                firstName = guardianFirstName,
                lastName = lastName,
                dateOfBirth = guardianBirthDate,
                ssn =
                    if (isMarkerFamily) generateSsn(markerBirthDate, 1)
                    else generateSsn(guardianBirthDate, serial++),
                phone = "0401234567",
                streetAddress = streetAddress,
                email = seedEmail(guardianFirstName, lastName),
            )

        val otherGuardian =
            if (familyIndex % SeedApplicationsConfig.SINGLE_GUARDIAN_FAMILY_EVERY != 0) {
                val otherFirstName =
                    SeedApplicationsConfig.GUARDIAN_FIRST_NAMES[
                            (familyIndex + 12) % SeedApplicationsConfig.GUARDIAN_FIRST_NAMES.size]
                val otherBirthDate =
                    LocalDate.of(1988, ((familyIndex * 7) % 12) + 1, ((familyIndex * 11) % 27) + 1)
                DevPerson(
                    firstName = otherFirstName,
                    lastName = lastName,
                    dateOfBirth = otherBirthDate,
                    ssn = generateSsn(otherBirthDate, serial++),
                    phone = "0402345678",
                    streetAddress = streetAddress,
                    email = seedEmail(otherFirstName, lastName),
                )
            } else null

        val familyChildren = mutableListOf<DevPerson>()
        val childSpecs = mutableListOf<ChildSpec>()
        var c = 0
        while (c < familySize && childIndex < children.size) {
            val spec = children[childIndex]
            val childFirstName =
                SeedApplicationsConfig.CHILD_FIRST_NAMES[
                        childIndex % SeedApplicationsConfig.CHILD_FIRST_NAMES.size]
            familyChildren.add(
                DevPerson(
                    firstName = childFirstName,
                    lastName = lastName,
                    dateOfBirth = spec.dateOfBirth,
                    ssn = generateSsn(spec.dateOfBirth, serial++),
                    streetAddress = streetAddress,
                    email = seedEmail(childFirstName, lastName),
                )
            )
            childSpecs.add(spec)
            childIndex++
            c++
        }

        families.add(
            GeneratedFamily(
                family = SeededFamily(guardian, otherGuardian, familyChildren),
                childSpecs = childSpecs,
            )
        )
        familyIndex++
    }

    return families
}

fun Database.Transaction.seedApplications(): List<SeededFamily> {
    insert(
        DevCareArea(
            id = SeedApplicationsConfig.SEED_AREA_ID,
            name = "${SeedApplicationsConfig.SEED_LABEL}-alue",
            shortName = "${SeedApplicationsConfig.SEED_LABEL}-alue",
        )
    )

    val unitIds =
        listOf(
            SeedApplicationsConfig.SEED_DAYCARE_UNIT_ID,
            SeedApplicationsConfig.SEED_PRESCHOOL_UNIT_ID,
        )
    for ((index, spec) in SeedApplicationsConfig.SEEDED_UNITS.withIndex()) {
        val id = unitIds[index]
        val managerName = "${spec.supervisor.firstName} ${spec.supervisor.lastName}"
        val daycare =
            DevDaycare(
                id = id,
                areaId = SeedApplicationsConfig.SEED_AREA_ID,
                name = spec.name,
                capacity = spec.capacity,
                type = setOf(CareType.CENTRE, CareType.PRESCHOOL, CareType.PREPARATORY_EDUCATION),
                dailyPreschoolTime = SeedApplicationsConfig.UNIT_DAILY_PRESCHOOL_TIME,
                dailyPreparatoryTime = SeedApplicationsConfig.UNIT_DAILY_PREPARATORY_TIME,
                daycareApplyPeriod = SeedApplicationsConfig.UNIT_DAYCARE_APPLY_PERIOD,
                preschoolApplyPeriod = SeedApplicationsConfig.UNIT_PRESCHOOL_APPLY_PERIOD,
                openingDate = SeedApplicationsConfig.UNIT_OPENING_DATE,
                enabledPilotFeatures = SeedApplicationsConfig.UNIT_PILOT_FEATURES.toSet(),
                phone = spec.phone,
                email = spec.email,
                url = spec.url,
                visitingAddress =
                    VisitingAddress(
                        streetAddress = spec.streetAddress,
                        postalCode = spec.postalCode,
                        postOffice = spec.postOffice,
                    ),
                mailingAddress =
                    MailingAddress(
                        streetAddress = spec.streetAddress,
                        postalCode = spec.postalCode,
                        postOffice = spec.postOffice,
                    ),
                location = spec.location,
                decisionCustomization =
                    DaycareDecisionCustomization(
                        daycareName = spec.name,
                        preschoolName = spec.name,
                        handler = SeedApplicationsConfig.DECISION_HANDLER,
                        handlerAddress = SeedApplicationsConfig.DECISION_HANDLER_ADDRESS,
                    ),
                unitManager =
                    UnitManager(name = managerName, phone = spec.phone, email = spec.email),
            )
        insert(daycare)
        val group = DevDaycareGroup(daycareId = id, name = "${spec.name} – ryhmä")
        insert(group)
        insert(DevDaycareCaretaker(groupId = group.id, amount = BigDecimal(spec.capacity / 7)))
        insert(
            DevEmployee(
                firstName = spec.supervisor.firstName,
                lastName = spec.supervisor.lastName,
                email = spec.supervisor.email,
                externalId = ExternalId.of("espoo-ad", spec.supervisor.externalId),
            ),
            unitRoles = mapOf(id to UserRole.UNIT_SUPERVISOR),
        )
        insert(
            DevEmployee(
                firstName = spec.staff.firstName,
                lastName = spec.staff.lastName,
                email = spec.staff.email,
                externalId = ExternalId.of("espoo-ad", spec.staff.externalId),
            ),
            unitRoles = mapOf(id to UserRole.STAFF),
        )
    }

    insert(
        DevPreschoolTerm(
            finnishPreschool = SeedApplicationsConfig.PRESCHOOL_TERM_FINNISH,
            swedishPreschool = SeedApplicationsConfig.PRESCHOOL_TERM_SWEDISH,
            extendedTerm = SeedApplicationsConfig.PRESCHOOL_TERM_EXTENDED,
            applicationPeriod = SeedApplicationsConfig.PRESCHOOL_TERM_APPLICATION_PERIOD,
            termBreaks = DateSet.of(SeedApplicationsConfig.TERM_BREAKS),
        )
    )
    insert(
        DevClubTerm(
            term = SeedApplicationsConfig.CLUB_TERM,
            applicationPeriod = SeedApplicationsConfig.CLUB_TERM_APPLICATION_PERIOD,
            termBreaks = DateSet.of(SeedApplicationsConfig.TERM_BREAKS),
        )
    )

    for (r in SeedApplicationsConfig.GENERIC_REASONINGS) {
        insert(
            DevDecisionReasoningGeneric(
                collectionType = r.collectionType,
                validFrom = SeedApplicationsConfig.REASONING_VALID_FROM,
                ready = true,
                textFi = r.textFi,
                textSv = r.textSv,
            )
        )
    }
    for (r in SeedApplicationsConfig.INDIVIDUAL_REASONINGS) {
        insert(
            DevDecisionReasoningIndividual(
                collectionType = r.collectionType,
                titleFi = r.titleFi,
                titleSv = r.titleSv,
                textFi = r.textFi,
                textSv = r.textSv,
            )
        )
    }

    val generated = generateFamilies()
    val units =
        SeededUnitIds(
            SeedApplicationsConfig.SEED_DAYCARE_UNIT_ID,
            SeedApplicationsConfig.SEED_PRESCHOOL_UNIT_ID,
        )
    var isFirstApplication = true
    for ((family, childSpecs) in generated) {
        insert(family.guardian, DevPersonType.ADULT)
        family.otherGuardian?.let { insert(it, DevPersonType.ADULT) }
        for (child in family.children) {
            insert(child, DevPersonType.CHILD)
        }
        for ((i, child) in family.children.withIndex()) {
            val application =
                buildApplication(
                    child = child,
                    guardian = family.guardian,
                    otherGuardian = family.otherGuardian,
                    spec = childSpecs[i],
                    units = units,
                    isMarkerApplication = isFirstApplication,
                )
            insertApplication(application)
            application.otherGuardians.forEach { otherGuardianId ->
                createUpdate {
                        sql(
                            "INSERT INTO application_other_guardian (application_id, guardian_id) VALUES (${bind(application.id)}, ${bind(otherGuardianId)})"
                        )
                    }
                    .execute()
            }
            isFirstApplication = false
        }
    }

    return generated.map { it.family }
}

fun buildVtjDataset(families: List<SeededFamily>): MockVtjDataset {
    val persons = mutableListOf<MockVtjPerson>()
    val guardianDependants = mutableMapOf<Ssn, List<Ssn>>()

    for (family in families) {
        persons.add(MockVtjPerson.from(family.guardian))
        family.otherGuardian?.let { persons.add(MockVtjPerson.from(it)) }
        family.children.forEach { persons.add(MockVtjPerson.from(it)) }

        val childSsns = family.children.map { it.ssn!! }
        guardianDependants[family.guardian.ssn!!] = childSsns
        family.otherGuardian?.let { guardianDependants[it.ssn!!] = childSsns }
    }

    return MockVtjDataset(persons = persons, guardianDependants = guardianDependants)
}
