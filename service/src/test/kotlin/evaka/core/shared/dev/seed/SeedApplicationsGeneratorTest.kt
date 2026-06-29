// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.dev.seed

import evaka.core.application.ApplicationType
import evaka.core.identity.isValidSSN
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class SeedApplicationsGeneratorTest {
    @Test
    fun `generateSsn produces checksum-valid, deterministic SSNs`() {
        val dob = LocalDate.of(1980, 1, 1)
        val ssn = generateSsn(dob, 1)
        assertTrue(isValidSSN(ssn), "expected $ssn to be a valid SSN")
        assertEquals(ssn, generateSsn(dob, 1))
        assertTrue(isValidSSN(generateSsn(LocalDate.of(2020, 12, 31), 150)))
        assertTrue(isValidSSN(generateSsn(LocalDate.of(2019, 6, 15), 899)))
    }

    @Test
    fun `generateFamilies yields the configured dataset shape`() {
        val families = generateFamilies()
        val children = families.flatMap { it.childSpecs }
        assertEquals(SeedApplicationsConfig.APPLICATION_COUNT, children.size)

        // type distribution follows the 8/7/5 cycle
        val daycare = children.count { it.kind == SeedKind.DAYCARE }
        val pd = children.count { it.kind == SeedKind.PRESCHOOL_DAYCARE }
        val preschool = children.count { it.kind == SeedKind.PRESCHOOL }
        assertEquals(children.size, daycare + pd + preschool)
        assertTrue(daycare > pd && pd > preschool)

        // marker family is first and has the fixed SSN
        val marker = families.first().family.guardian
        assertEquals(generateSsn(LocalDate.of(1980, 1, 1), 1), marker.ssn)

        // all SSNs unique and valid
        val ssns = families.flatMap { f ->
            listOfNotNull(f.family.guardian.ssn, f.family.otherGuardian?.ssn) +
                f.family.children.mapNotNull { it.ssn }
        }
        assertEquals(ssns.size, ssns.toSet().size)
        assertTrue(ssns.all { isValidSSN(it) })

        // at least one single-guardian and one two-child family exist
        assertTrue(families.any { it.family.otherGuardian == null })
        assertTrue(families.any { it.family.children.size == 2 })
    }

    @Test
    fun `buildApplication maps kind to type, due date, and connected daycare`() {
        val units =
            SeededUnitIds(
                daycareUnitId = SeedApplicationsConfig.SEED_DAYCARE_UNIT_ID,
                preschoolUnitId = SeedApplicationsConfig.SEED_PRESCHOOL_UNIT_ID,
            )
        val families = generateFamilies()
        val appsWithKind = families.flatMap { f ->
            f.family.children.mapIndexed { i, child ->
                val spec = f.childSpecs[i]
                spec.kind to
                    buildApplication(
                        child,
                        f.family.guardian,
                        f.family.otherGuardian,
                        spec,
                        units,
                        isMarkerApplication = false,
                    )
            }
        }
        val apps = appsWithKind.map { it.second }
        assertEquals(SeedApplicationsConfig.APPLICATION_COUNT, apps.size)

        val daycareApp = apps.first { it.type == ApplicationType.DAYCARE }
        assertEquals(SeedApplicationsConfig.SENT_DATE.plusMonths(4), daycareApp.dueDate)

        val preschoolApp = apps.first { it.type == ApplicationType.PRESCHOOL }
        assertEquals(SeedApplicationsConfig.SENT_DATE, preschoolApp.dueDate)

        // every application references a seeded unit
        assertTrue(
            apps.all { app ->
                app.form.preferences.preferredUnits.all {
                    it.id == units.daycareUnitId || it.id == units.preschoolUnitId
                }
            }
        )

        // a connected daycare (PRESCHOOL_DAYCARE) application carries the connected start
        // date and a service need
        val connectedApp = apps.first {
            it.form.preferences.connectedDaycarePreferredStartDate != null
        }
        assertEquals(
            SeedApplicationsConfig.PREFERRED_START_DATE,
            connectedApp.form.preferences.connectedDaycarePreferredStartDate,
        )
        assertTrue(connectedApp.form.preferences.serviceNeed != null)

        // a pure PRESCHOOL application has no service need and no connected daycare
        val purePreschoolApp = appsWithKind.first { it.first == SeedKind.PRESCHOOL }.second
        assertEquals(null, purePreschoolApp.form.preferences.serviceNeed)
        assertEquals(null, purePreschoolApp.form.preferences.connectedDaycarePreferredStartDate)

        // at least one application is preparatory
        assertTrue(apps.any { it.form.preferences.preparatory })

        // applicationFixture parity: maxFeeAccepted is false and clubDetails is always present
        assertTrue(apps.all { it.form.maxFeeAccepted == false && it.form.clubDetails != null })

        // two-guardian family: otherGuardians contains the other guardian's id
        val twoGuardianFamily = families.first { it.family.otherGuardian != null }
        val twoGuardianChild = twoGuardianFamily.family.children.first()
        val twoGuardianSpec = twoGuardianFamily.childSpecs.first()
        val twoGuardianApp =
            buildApplication(
                twoGuardianChild,
                twoGuardianFamily.family.guardian,
                twoGuardianFamily.family.otherGuardian,
                twoGuardianSpec,
                units,
                isMarkerApplication = false,
            )
        assertEquals(
            listOf(twoGuardianFamily.family.otherGuardian!!.id),
            twoGuardianApp.otherGuardians,
        )

        // single-guardian family: otherGuardians is empty
        val singleGuardianFamily = families.first { it.family.otherGuardian == null }
        val singleGuardianChild = singleGuardianFamily.family.children.first()
        val singleGuardianSpec = singleGuardianFamily.childSpecs.first()
        val singleGuardianApp =
            buildApplication(
                singleGuardianChild,
                singleGuardianFamily.family.guardian,
                null,
                singleGuardianSpec,
                units,
                isMarkerApplication = false,
            )
        assertTrue(singleGuardianApp.otherGuardians.isEmpty())

        // form SSNs are populated from the person records
        assertEquals(twoGuardianChild.ssn, twoGuardianApp.form.child.person.socialSecurityNumber)
        assertEquals(
            twoGuardianFamily.family.guardian.ssn,
            twoGuardianApp.form.guardian.person.socialSecurityNumber,
        )
    }

    @Test
    fun `buildVtjDataset registers every person and guardian-dependant link`() {
        val families = generateFamilies().map { it.family }
        val dataset = buildVtjDataset(families)

        val expectedPersons = families.sumOf { f ->
            1 + (if (f.otherGuardian != null) 1 else 0) + f.children.size
        }
        assertEquals(expectedPersons, dataset.persons.size)

        val first = families.first()
        val childSsns = first.children.map { it.ssn!! }
        assertEquals(childSsns, dataset.guardianDependants[first.guardian.ssn!!])

        // two-guardian family: both guardians map to the same children
        val twoGuardian = families.first { it.otherGuardian != null }
        assertEquals(
            dataset.guardianDependants[twoGuardian.guardian.ssn!!],
            dataset.guardianDependants[twoGuardian.otherGuardian!!.ssn!!],
        )
    }
}
