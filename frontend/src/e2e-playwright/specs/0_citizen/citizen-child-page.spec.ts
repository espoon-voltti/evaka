// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { enduserLogin } from 'e2e-playwright/utils/user'
import {
  insertApplications,
  insertDaycarePlacementFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import LocalDate from 'lib-common/local-date'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../../e2e-test-common/dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  uuidv4
} from '../../../e2e-test-common/dev-api/fixtures'
import { DaycarePlacement } from '../../../e2e-test-common/dev-api/types'
import {
  CitizenChildPage,
  CitizenChildrenPage
} from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'

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

  describe('Placement termination', () => {
    const assertToggledPlacements = async (labels: string[]) =>
      waitUntilEqual(() => childPage.getToggledPlacements(), labels)
    const assertTerminatablePlacements = async (labels: string[]) =>
      waitUntilEqual(() => childPage.getTerminatablePlacements(), labels)
    const assertTerminatedPlacements = async (labels: string | string[]) =>
      waitUntilEqual(
        () => childPage.getTerminatedPlacements(),
        typeof labels === 'string' ? [labels] : labels
      )

    test('Simple daycare placement can be terminated', async () => {
      const endDate = LocalDate.today().addYears(2)
      await insertDaycarePlacementFixtures([
        {
          id: uuidv4(),
          type: 'DAYCARE',
          childId: fixtures.enduserChildFixtureKaarina.id,
          unitId: fixtures.daycareFixture.id,
          startDate: LocalDate.today().subMonths(2).formatIso(),
          endDate: endDate.formatIso()
        }
      ])

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await childPage.assertTerminatablePlacementCount(1)
      await childPage.togglePlacement(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${endDate.format()}`
      )
      await childPage.fillTerminationDate(LocalDate.today())
      await childPage.submitTermination()
      await childPage.assertTerminatablePlacementCount(0)

      await childPage.assertTerminatedPlacementCount(1)
      await assertTerminatedPlacements(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${LocalDate.today().format()}`
      )
    })

    test('Upcoming transfer application is deleted when placement is terminated', async () => {
      const endDate = LocalDate.today().addYears(2)
      await insertDaycarePlacementFixtures([
        {
          id: uuidv4(),
          type: 'DAYCARE',
          childId: fixtures.enduserChildFixtureKaarina.id,
          unitId: fixtures.daycareFixture.id,
          startDate: LocalDate.today().subMonths(2).formatIso(),
          endDate: endDate.formatIso()
        }
      ])
      
      const application = applicationFixture(
        fixtures.enduserChildFixtureKaarina,
        fixtures.enduserGuardianFixture,
        undefined,
        'DAYCARE',
        null,
        [fixtures.daycareFixture.id],
        true,
        'SENT',
        LocalDate.today(),
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
      await childPage.fillTerminationDate(LocalDate.today())
      await childPage.submitTermination()
      await childPage.assertTerminatablePlacementCount(0)

      await header.selectTab('applications')
      await new CitizenApplicationsPage(page).assertApplicationDoesNotExist(
        applicationFixtureId
      )
    })

    test('Daycare placements are grouped by type and unit, and invoiced daycare can be terminated separately', async () => {
      const daycare1End = LocalDate.today().addMonths(3)
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
          startDate: LocalDate.today().subMonths(2).formatIso(),
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
      const daycare1FirstTermination = LocalDate.today().addWeeks(1)
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
      await childPage.fillTerminationDate(LocalDate.today(), 0)
      await childPage.submitTermination(0)
      await childPage.assertTerminatablePlacementCount(3)
      await assertTerminatedPlacements(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${LocalDate.today().format()}`
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
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${LocalDate.today().format()}`,
        `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, viimeinen läsnäolopäivä: ${daycareAfterPreschoolEnd
          .subMonths(1)
          .format()}`
      ])

      // terminating preschool terminates daycare after preschool
      await childPage.togglePlacement(labels.preschool)
      await childPage.fillTerminationDate(LocalDate.today(), 1)
      await childPage.submitTermination(1)

      await assertTerminatablePlacements([labels.daycare2])
      await assertTerminatedPlacements([
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${LocalDate.today().format()}`
        // preschool was in future and is cancelled -> deleted from db -> no sign of termination
      ])
    })
  })
})
