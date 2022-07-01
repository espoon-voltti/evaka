// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'

import {
  insertApplications,
  insertDaycarePlacementFixtures,
  resetDatabase
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  uuidv4
} from '../../dev-api/fixtures'
import { DaycarePlacement } from '../../dev-api/types'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import {
  CitizenChildPage,
  CitizenChildrenPage
} from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let page: Page
let header: CitizenHeader
let childPage: CitizenChildPage
let childrenPage: CitizenChildrenPage

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  page = await Page.open()
  await enduserLogin(page)
  header = new CitizenHeader(page)
  childPage = new CitizenChildPage(page)
  childrenPage = new CitizenChildrenPage(page)
})

describe('Citizen children page', () => {
  describe('Child page', () => {
    test('Citizen can see its children and navigate to their page', async () => {
      await header.selectTab('children')
      await childrenPage.assertChildCount(3)
      await childrenPage.navigateToChild(0)
      await childPage.assertChildNameIsShown(
        'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani Karhula'
      )
      await childPage.goBack()

      await childrenPage.navigateToChild(1)
      await childPage.assertChildNameIsShown('Kaarina Veera Nelli Karhula')
    })
  })

  async function createDaycarePlacement(
    endDate: LocalDate,
    unitId = fixtures.daycareFixture.id,
    type: PlacementType = 'DAYCARE',
    startDate: LocalDate = LocalDate.todayInSystemTz().subMonths(2)
  ) {
    await insertDaycarePlacementFixtures([
      {
        id: uuidv4(),
        type,
        childId: fixtures.enduserChildFixtureKaarina.id,
        unitId,
        startDate: startDate.formatIso(),
        endDate: endDate.formatIso()
      }
    ])
  }

  describe('Placement termination', () => {
    const assertToggledPlacements = async (labels: string[]) =>
      waitUntilEqual(() => childPage.getToggledPlacements(), labels)
    const assertTerminatablePlacements = async (labels: string[]) =>
      waitUntilEqual(() => childPage.getTerminatablePlacements(), labels)
    const assertNonTerminatablePlacements = async (labels: string[]) =>
      waitUntilEqual(() => childPage.getNonTerminatablePlacements(), labels)
    const assertTerminatedPlacements = async (labels: string | string[]) =>
      waitUntilEqual(
        () => childPage.getTerminatedPlacements(),
        typeof labels === 'string' ? [labels] : labels
      )

    test('Simple daycare placement can be terminated', async () => {
      const endDate = LocalDate.todayInSystemTz().addYears(2)
      await createDaycarePlacement(endDate)

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await childPage.assertTerminatablePlacementCount(1)
      await childPage.togglePlacement(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${endDate.format()}`
      )
      await childPage.fillTerminationDate(LocalDate.todayInSystemTz())
      await childPage.submitTermination()
      await childPage.assertTerminatablePlacementCount(0)

      await childPage.assertTerminatedPlacementCount(1)
      await assertTerminatedPlacements(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${LocalDate.todayInSystemTz().format()}`
      )
    })

    test('Daycare placement cannot be terminated if termination is not enabled for unit', async () => {
      const endDate = LocalDate.todayInSystemTz().addYears(2)
      await createDaycarePlacement(endDate, fixtures.clubFixture.id, 'CLUB')
      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await childPage.assertTerminatablePlacementCount(0)
      await assertNonTerminatablePlacements([
        `Alkuräjähdyksen kerho, voimassa ${endDate.format()}`
      ])
    })

    test('Daycare placement cannot be terminated if placement is in the future', async () => {
      const startDate = LocalDate.todayInSystemTz().addDays(1)
      const endDate = startDate
      await createDaycarePlacement(
        endDate,
        fixtures.daycareFixture.id,
        'DAYCARE',
        startDate
      )
      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await childPage.assertTerminatablePlacementCount(0)
      await assertNonTerminatablePlacements([
        `Alkuräjähdyksen päiväkoti, voimassa ${endDate.format()}`
      ])
    })

    test('Upcoming transfer application is deleted when placement is terminated', async () => {
      const endDate = LocalDate.todayInSystemTz().addYears(2)
      await createDaycarePlacement(endDate)

      const application = applicationFixture(
        fixtures.enduserChildFixtureKaarina,
        fixtures.enduserGuardianFixture,
        undefined,
        'DAYCARE',
        null,
        [fixtures.daycareFixture.id],
        true,
        'SENT',
        LocalDate.todayInSystemTz(),
        true
      )
      await insertApplications([application])

      await header.selectTab('applications')
      await new CitizenApplicationsPage(page).assertApplicationExists(
        applicationFixtureId
      )

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()
      const placementLabel = `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${endDate.format()}`
      await childPage.togglePlacement(placementLabel)
      await childPage.fillTerminationDate(LocalDate.todayInSystemTz())
      await childPage.submitTermination()
      await childPage.assertTerminatablePlacementCount(0)

      await header.selectTab('applications')
      await new CitizenApplicationsPage(page).assertApplicationDoesNotExist(
        applicationFixtureId
      )
    })

    /*
      TODO
      This test is now broken because placements in the future cannot be terminated.
      It does test valuable things though, so this should be converted as several
      tests that test just one ongoing (set of) placement(s) at a time
    * */
    test.skip('Daycare placements are grouped by type and unit, and invoiced daycare can be terminated separately', async () => {
      const daycare1Start = LocalDate.todayInSystemTz().subMonths(2)
      const daycare1End = LocalDate.todayInSystemTz().addMonths(3)
      const daycare2start = daycare1End.addDays(1)
      const daycare2end = daycare1End.addMonths(2)
      const preschool1Start = daycare2end.addDays(1)
      const preschool1End = daycare2end.addMonths(6)
      const preschool2Start = preschool1End.addDays(1)
      const preschool2End = preschool1End.addMonths(6)
      const daycareAfterPreschoolStart = preschool2End.addDays(1)
      const daycareAfterPreschoolEnd = preschool2End.addMonths(6)
      const placements: DaycarePlacement[] = [
        {
          id: uuidv4(),
          type: 'DAYCARE',
          childId: fixtures.enduserChildFixtureKaarina.id,
          unitId: fixtures.daycareFixture.id,
          startDate: daycare1Start.formatIso(),
          endDate: daycare1End.formatIso()
        },
        {
          id: uuidv4(),
          type: 'DAYCARE',
          childId: fixtures.enduserChildFixtureKaarina.id,
          unitId: fixtures.preschoolFixture.id,
          startDate: daycare2start.formatIso(),
          endDate: daycare2end.formatIso()
        },
        {
          id: uuidv4(),
          type: 'PRESCHOOL',
          childId: fixtures.enduserChildFixtureKaarina.id,
          unitId: fixtures.preschoolFixture.id,
          startDate: preschool1Start.formatIso(),
          endDate: preschool1End.formatIso()
        },
        {
          id: uuidv4(),
          type: 'PRESCHOOL_DAYCARE', // this gets grouped with the above
          childId: fixtures.enduserChildFixtureKaarina.id,
          unitId: fixtures.preschoolFixture.id,
          startDate: preschool2Start.formatIso(),
          endDate: preschool2End.formatIso()
        },
        {
          id: uuidv4(),
          type: 'DAYCARE', // this is shown under PRESCHOOL as "Maksullinen varhaiskasvatus"
          childId: fixtures.enduserChildFixtureKaarina.id,
          unitId: fixtures.preschoolFixture.id,
          startDate: daycareAfterPreschoolStart.formatIso(),
          endDate: daycareAfterPreschoolEnd.formatIso()
        }
      ]
      await insertDaycarePlacementFixtures(placements)
      const labels = {
        daycare1: `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${daycare1End.format()}`,
        daycare2: `Varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${daycare2end.format()}`,
        preschool: `Esiopetus, Alkuräjähdyksen eskari, voimassa ${preschool2End.format()}`,
        daycareAfterPreschool: `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${daycareAfterPreschoolEnd.format()}`
      }

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await childPage.assertTerminatablePlacementCount(4)
      await assertTerminatablePlacements(Object.values(labels))

      await childPage.togglePlacement(labels.daycare1)
      const daycare1FirstTermination = LocalDate.todayInSystemTz().addWeeks(1)
      await childPage.fillTerminationDate(daycare1FirstTermination, 0)
      await childPage.submitTermination(0)
      await childPage.assertTerminatablePlacementCount(4) // still 4 because not fully terminated

      await childPage.assertTerminatedPlacementCount(1)
      await assertTerminatedPlacements(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${daycare1FirstTermination.format()}`
      )
      await assertToggledPlacements([])

      await childPage.togglePlacement(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${daycare1FirstTermination.format()}`
      )
      await childPage.fillTerminationDate(LocalDate.todayInSystemTz(), 0)
      await childPage.submitTermination(0)
      await childPage.assertTerminatablePlacementCount(3)
      await assertTerminatedPlacements(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${LocalDate.todayInSystemTz().format()}`
      )
      await assertTerminatablePlacements(Object.values(labels).slice(1))

      // selecting preschool selects daycare after preschool too
      await childPage.togglePlacement(labels.preschool)
      await assertToggledPlacements([
        labels.preschool,
        labels.daycareAfterPreschool
      ])
      // deselecting preschool does not deselect daycare after preschool
      await childPage.togglePlacement(labels.preschool)
      await assertToggledPlacements([labels.daycareAfterPreschool])
      // re-selecting preschool selects daycare after preschool too
      await childPage.togglePlacement(labels.preschool)
      await assertToggledPlacements([
        labels.preschool,
        labels.daycareAfterPreschool
      ])
      // de-selecting daycare after preschool de-selects preschool too
      await childPage.togglePlacement(labels.daycareAfterPreschool)
      await assertToggledPlacements([])

      await childPage.togglePlacement(labels.daycareAfterPreschool)
      await childPage.fillTerminationDate(
        daycareAfterPreschoolEnd.subMonths(1),
        1
      )
      await childPage.submitTermination(1)

      await assertTerminatedPlacements([
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${LocalDate.todayInSystemTz().format()}`,
        `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, viimeinen läsnäolopäivä: ${daycareAfterPreschoolEnd
          .subMonths(1)
          .format()}`
      ])

      // terminating preschool terminates daycare after preschool
      await childPage.togglePlacement(labels.preschool)
      await childPage.fillTerminationDate(LocalDate.todayInSystemTz(), 1)
      await childPage.submitTermination(1)

      await assertTerminatablePlacements([labels.daycare2])
      await assertTerminatedPlacements([
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${LocalDate.todayInSystemTz().format()}`
        // preschool was in future and is cancelled -> deleted from db -> no sign of termination
      ])
    })

    test('Terminating paid daycare only is possible', async () => {
      const endDate = LocalDate.todayInSystemTz().addYears(2)
      await createDaycarePlacement(
        endDate,
        fixtures.preschoolFixture.id,
        'PRESCHOOL_DAYCARE'
      )

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await assertTerminatablePlacements([
        `Esiopetus, Alkuräjähdyksen eskari, voimassa ${endDate.format()}`,
        `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${endDate.format()}`
      ])
      await childPage.togglePlacement(
        `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${endDate.format()}`
      )
      const terminationDate = LocalDate.todayInSystemTz().addMonths(1)
      await childPage.fillTerminationDate(terminationDate)
      await childPage.submitTermination()
      await assertTerminatablePlacements([
        `Esiopetus, Alkuräjähdyksen eskari, voimassa ${endDate.format()}`,
        `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${terminationDate.format()}`
      ])

      await childPage.assertTerminatedPlacementCount(0) // the paid daycare is not terminated, just split to PRESCHOOL_DAYCARE and PRESCHOOL
    })
  })
})
