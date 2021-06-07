package fi.espoo.evaka.ai

import fi.espoo.evaka.ai.model.EvakaUnit
import fi.espoo.evaka.ai.model.getUnitData
import fi.espoo.evaka.ai.utils.pickRandom
import fi.espoo.evaka.ai.utils.random
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestPerson
import java.time.LocalDate

fun generateData(tx: Database.Transaction, date: LocalDate, amount: Int) {
    val unitOptions = getUnitData(tx, date)

    val guardianId = tx.insertTestPerson(DevPerson())

    for (i in 0..amount) {
        val childId = tx.insertTestPerson(DevPerson())
        tx.insertTestChild(DevChild(childId))

        val applicationId = tx.insertTestApplication(
            status = ApplicationStatus.WAITING_PLACEMENT,
            guardianId = guardianId,
            childId = childId
        )
        tx.insertTestApplicationForm(
            applicationId = applicationId,
            document = generateTestChild(unitOptions, date)
        )
    }
}

private fun generateTestChild(units: List<EvakaUnit>, date: LocalDate): DaycareFormV0 {
    val r1 = random.nextDouble()
    val onlySwedish = r1 < 0.1
    val onlyFinnish = r1 > 0.15

    val filteredUnits = when {
        onlyFinnish -> units.filter { it.language == Language.fi }
        onlySwedish -> units.filter { it.language == Language.sv }
        else -> units
    }

    val firstPreference = filteredUnits.pickRandom()
    val preferredUnits = mutableListOf(firstPreference.id)

    val filteredNearbyUnits = firstPreference.nearbyUnits.let {
        when {
            onlyFinnish -> it.filter { it.language == Language.fi }
            onlySwedish -> it.filter { it.language == Language.sv }
            else -> it
        }
    }

    val r2 = random.nextDouble()
    if (r2 < 0.7) {
        val secondPreference = filteredNearbyUnits.slice(0 until 8).pickRandom().id
        preferredUnits.add(secondPreference)
    }
    if (r2 < 0.4) {
        val thirdPreference = firstPreference.nearbyUnits
            .slice(0 until 15)
            .filterNot { (id) -> preferredUnits.contains(id) }
            .pickRandom()
            .id
        preferredUnits.add(thirdPreference)
    }

    return DaycareFormV0(
        child = Child(dateOfBirth = LocalDate.now().minusYears(5)),
        guardian = Adult(),
        type = ApplicationType.PRESCHOOL,
        apply = Apply(
            preferredUnits = preferredUnits
        ),
        preferredStartDate = date,
        connectedDaycare = random.nextDouble() < 0.75
    )
}
