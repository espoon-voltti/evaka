// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
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
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { DaycarePlacement, EmployeeDetail } from '../../dev-api/types'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import AssistanceNeedDecisionPage from '../../pages/citizen/citizen-assistance-need-decision'
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

const mockedDate = LocalDate.of(2022, 3, 1)

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  page = await Page.open({ mockedTime: mockedDate.toSystemTzDate() })
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
    startDate: LocalDate = mockedDate.subMonths(2)
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
      const endDate = mockedDate.addYears(2)
      await createDaycarePlacement(endDate)

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await childPage.assertTerminatablePlacementCount(1)
      await childPage.togglePlacement(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${endDate.format()}`
      )
      await childPage.fillTerminationDate(mockedDate)
      await childPage.submitTermination()
      await childPage.assertTerminatablePlacementCount(0)

      await childPage.assertTerminatedPlacementCount(1)
      await assertTerminatedPlacements(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${mockedDate.format()}`
      )
    })

    test('Daycare placement cannot be terminated if termination is not enabled for unit', async () => {
      const endDate = mockedDate.addYears(2)
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
      const startDate = mockedDate.addDays(1)
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
      const endDate = mockedDate.addYears(2)
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
        mockedDate,
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
      await childPage.fillTerminationDate(mockedDate)
      await childPage.submitTermination()
      await childPage.assertTerminatablePlacementCount(0)

      await header.selectTab('applications')
      await new CitizenApplicationsPage(page).assertApplicationDoesNotExist(
        applicationFixtureId
      )
    })

    test('Daycare placements are grouped by type and unit, and invoiced daycare can be terminated separately', async () => {
      const daycare1Start = mockedDate.subMonths(2)
      const daycare1End = mockedDate.addMonths(3)
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
      await childPage.assertTerminatablePlacementCount(1)
      await childPage.assertNonTerminatablePlacementCount(2)
      await assertTerminatablePlacements([labels.daycare1])
      await assertNonTerminatablePlacements([
        `Alkuräjähdyksen eskari, voimassa ${daycare2end.format()}`,
        `Alkuräjähdyksen eskari, voimassa ${preschool2End.format()}`
      ])
    })

    test('Daycare placements are grouped by type and unit, future placement cannot be terminated', async () => {
      const daycare1Start = mockedDate.subMonths(2)
      const daycare1End = mockedDate.addMonths(3)
      const daycare2start = daycare1End.addDays(1)
      const daycare2end = daycare1End.addMonths(2)
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
        }
      ]
      await insertDaycarePlacementFixtures(placements)
      const labels = {
        daycare1: `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${daycare1End.format()}`,
        daycare2: `Varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${daycare2end.format()}`
      }

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await childPage.assertTerminatablePlacementCount(1)
      await assertTerminatablePlacements([labels.daycare1])
      await childPage.togglePlacement(labels.daycare1)
      const daycare1FirstTermination = mockedDate.addWeeks(1)
      await childPage.fillTerminationDate(daycare1FirstTermination, 0)
      await childPage.submitTermination(0)
      await childPage.assertTerminatablePlacementCount(1)
      await childPage.assertTerminatedPlacementCount(1)
      await assertTerminatedPlacements(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${daycare1FirstTermination.format()}`
      )
      await assertToggledPlacements([])

      await childPage.togglePlacement(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${daycare1FirstTermination.format()}`
      )
      await childPage.fillTerminationDate(mockedDate, 0)
      await childPage.submitTermination(0)
      await childPage.assertTerminatablePlacementCount(1)
      await assertTerminatedPlacements(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ${mockedDate.format()}`
      )

      await childPage.assertNonTerminatablePlacementCount(1)
      await assertNonTerminatablePlacements([
        `Alkuräjähdyksen eskari, voimassa ${daycare2end.format()}`
      ])
    })

    test('Invoiced daycare can be terminated separately', async () => {
      const preschool2Start = mockedDate.subMonths(2)
      const preschool2End = mockedDate.addMonths(3)
      const daycareAfterPreschoolStart = preschool2End.addDays(1)
      const daycareAfterPreschoolEnd = preschool2End.addMonths(6)
      const placements: DaycarePlacement[] = [
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
        preschool: `Esiopetus, Alkuräjähdyksen eskari, voimassa ${preschool2End.format()}`,
        daycareAfterPreschool: `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${daycareAfterPreschoolEnd.format()}`
      }

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await childPage.assertTerminatablePlacementCount(2)

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
      await childPage.fillTerminationDate(daycareAfterPreschoolEnd.subMonths(1))
      await childPage.submitTermination()

      await assertTerminatedPlacements([
        `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, viimeinen läsnäolopäivä: ${daycareAfterPreschoolEnd
          .subMonths(1)
          .format()}`
      ])

      // terminating preschool terminates daycare after preschool
      await childPage.togglePlacement(labels.preschool)
      await childPage.fillTerminationDate(mockedDate)
      await childPage.submitTermination()
      await assertTerminatedPlacements([
        `Esiopetus, Alkuräjähdyksen eskari, viimeinen läsnäolopäivä: ${mockedDate.format()}`
      ])
    })

    test('Terminating paid daycare only is possible', async () => {
      const endDate = mockedDate.addYears(2)
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
      const terminationDate = mockedDate.addMonths(1)
      await childPage.fillTerminationDate(terminationDate)
      await childPage.submitTermination()
      await assertTerminatablePlacements([
        `Esiopetus, Alkuräjähdyksen eskari, voimassa ${endDate.format()}`,
        `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${terminationDate.format()}`
      ])

      await childPage.assertTerminatedPlacementCount(0) // the paid daycare is not terminated, just split to PRESCHOOL_DAYCARE and PRESCHOOL
    })
  })

  describe('Assistance need decisions table', () => {
    test('Has an accepted decision', async () => {
      await Fixture.preFilledAssistanceNeedDecision()
        .withChild(fixtures.enduserChildFixtureKaarina.id)
        .with({
          selectedUnit: { id: fixtures.daycareFixture.id },
          status: 'ACCEPTED',
          assistanceLevel: 'SPECIAL_ASSISTANCE',
          startDate: LocalDate.of(2020, 2, 5),
          endDate: LocalDate.of(2021, 5, 11),
          decisionMade: LocalDate.of(2020, 1, 17)
        })
        .save()

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openAssistanceNeedCollapsible()

      await waitUntilEqual(() => childPage.getAssistanceNeedDecisionRow(0), {
        assistanceLevel: 'Erityinen tuki',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: '05.02.2020 – 11.05.2021',
        decisionMade: '17.01.2020',
        status: 'ACCEPTED'
      })
    })
    test('Has a rejected decision', async () => {
      await Fixture.preFilledAssistanceNeedDecision()
        .withChild(fixtures.enduserChildFixtureKaarina.id)
        .with({
          selectedUnit: { id: fixtures.daycareFixture.id },
          status: 'REJECTED',
          assistanceLevel: 'ENHANCED_ASSISTANCE',
          startDate: LocalDate.of(2022, 2, 10),
          decisionMade: LocalDate.of(2021, 1, 17)
        })
        .save()

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openAssistanceNeedCollapsible()

      await waitUntilEqual(() => childPage.getAssistanceNeedDecisionRow(0), {
        assistanceLevel: 'Tehostettu tuki',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: '10.02.2022 –',
        decisionMade: '17.01.2021',
        status: 'REJECTED'
      })
    })
    test('Does not have a needs work or draft decision', async () => {
      await Fixture.preFilledAssistanceNeedDecision()
        .withChild(fixtures.enduserChildFixtureKaarina.id)
        .with({
          selectedUnit: { id: fixtures.daycareFixture.id },
          status: 'NEEDS_WORK',
          assistanceLevel: 'ENHANCED_ASSISTANCE',
          startDate: LocalDate.of(2022, 2, 10),
          decisionMade: LocalDate.of(2021, 1, 17)
        })
        .save()

      await Fixture.preFilledAssistanceNeedDecision()
        .withChild(fixtures.enduserChildFixtureKaarina.id)
        .with({
          selectedUnit: { id: fixtures.daycareFixture.id },
          status: 'DRAFT',
          assistanceLevel: 'ENHANCED_ASSISTANCE',
          startDate: LocalDate.of(2020, 2, 5),
          endDate: LocalDate.of(2021, 5, 11),
          decisionMade: LocalDate.of(2021, 1, 17)
        })
        .save()

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openAssistanceNeedCollapsible()

      await waitUntilEqual(
        () => childPage.getAssistanceNeedDecisionRowCount(),
        0
      )
    })
  })
})

describe('Citizen assistance need decision page', () => {
  let assistanceNeedDecisionPage: AssistanceNeedDecisionPage
  let serviceWorker: EmployeeDetail

  beforeEach(async () => {
    serviceWorker = (await Fixture.employeeServiceWorker().save()).data

    const decision = await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: { id: fixtures.daycareFixture.id },
        status: 'ACCEPTED',
        assistanceLevel: 'ENHANCED_ASSISTANCE',
        startDate: LocalDate.of(2020, 2, 5),
        endDate: LocalDate.of(2021, 5, 11),
        decisionMade: LocalDate.of(2021, 1, 17),
        decisionMaker: {
          employeeId: serviceWorker.id,
          title: 'head teacher'
        },
        preparedBy1: {
          employeeId: serviceWorker.id,
          title: 'teacher',
          phoneNumber: '010202020202'
        }
      })
      .save()

    await header.selectTab('children')
    await childrenPage.openChildPage('Kaarina')
    await childPage.openAssistanceNeedCollapsible()
    await childPage.getAssistanceNeedDecisionRowClick(0)

    await page.page.waitForURL(
      `${config.enduserUrl}/children/${
        fixtures.enduserChildFixtureKaarina.id
      }/assistance-need-decision/${decision.data.id ?? ''}`
    )

    assistanceNeedDecisionPage = new AssistanceNeedDecisionPage(page)
  })

  test('Preview shows filled information', async () => {
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.pedagogicalMotivation,
      'Pedagogical motivation text'
    )
    await assistanceNeedDecisionPage.assertStructuralMotivationOption(
      'groupAssistant'
    )
    await assistanceNeedDecisionPage.assertStructuralMotivationOption(
      'smallerGroup'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.structuralMotivationDescription,
      'Structural motivation description text'
    )
    await assistanceNeedDecisionPage.assertServiceOption(
      'interpretationAndAssistanceServices'
    )
    await assistanceNeedDecisionPage.assertServiceOption('partTimeSpecialEd')
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.careMotivation,
      'Care motivation text'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.guardiansHeardOn,
      '05.04.2020'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.otherRepresentativeDetails,
      'John Doe, 01020304050, via phone'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.viewOfGuardians,
      'VOG text'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.futureLevelOfAssistance,
      'Tehostettu tuki'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.startDate,
      '05.02.2020'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.selectedUnit,
      `${fixtures.daycareFixture.name}\n${fixtures.daycareFixture.streetAddress}\n${fixtures.daycareFixture.postalCode} ${fixtures.daycareFixture.postOffice}\nLoma-aikoina tuen järjestämispaikka ja -tapa saattavat muuttua.`
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.motivationForDecision,
      'Motivation for decision text'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.preparedBy1,
      `${serviceWorker.firstName} ${serviceWorker.lastName}, teacher\n${
        serviceWorker.email ?? ''
      }\n010202020202`
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.decisionMaker,
      `${serviceWorker.firstName} ${serviceWorker.lastName}, head teacher`
    )
  })
})
