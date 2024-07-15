// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import {
  applicationFixture,
  applicationFixtureId,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testChild2,
  testClub,
  testDaycare,
  testPreschool,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createApplications,
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import { DevPlacement } from '../../generated/api-types'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { KeycloakRealmClient } from '../../utils/keycloak'
import { Page } from '../../utils/page'
import {
  citizenWeakAccount,
  enduserLogin,
  enduserLoginWeak
} from '../../utils/user'

let page: Page

const mockedDate = LocalDate.of(2022, 3, 1)

describe('Citizen children page', () => {
  beforeEach(async () => {
    await resetServiceState()
    await Fixture.careArea().with(testCareArea).save()
    await Fixture.daycare(testDaycare).save()
    await Fixture.daycare(testPreschool).save()
    await Fixture.family({
      guardian: testAdult,
      children: [testChild, testChild2]
    }).save()

    page = await Page.open({
      mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
  })

  test('Citizen can see its children and navigate to their page', async () => {
    await createDaycarePlacements({
      body: [testChild, testChild2].map((child) => ({
        id: uuidv4(),
        type: 'DAYCARE',
        childId: child.id,
        unitId: testDaycare.id,
        startDate: mockedDate.subMonths(1),
        endDate: mockedDate,
        placeGuarantee: false,
        terminatedBy: null,
        terminationRequestedDate: null
      }))
    })

    await enduserLogin(page, testAdult)
    const header = new CitizenHeader(page)
    const childPage = new CitizenChildPage(page)

    await header.openChildPage(testChild.id)
    await childPage.assertChildNameIsShown(
      'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani Karhula'
    )
    await header.openChildPage(testChild2.id)
    await childPage.assertChildNameIsShown('Kaarina Veera Nelli Karhula')
  })

  async function createDaycarePlacement(
    endDate: LocalDate,
    unitId = testDaycare.id,
    type: PlacementType = 'DAYCARE',
    startDate: LocalDate = mockedDate.subMonths(2)
  ) {
    await createDaycarePlacements({
      body: [
        {
          id: uuidv4(),
          type,
          childId: testChild2.id,
          unitId,
          startDate: startDate,
          endDate: endDate,
          placeGuarantee: false,
          terminationRequestedDate: null,
          terminatedBy: null
        }
      ]
    })
  }

  describe('Placement termination', () => {
    let childPage: CitizenChildPage

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

      await enduserLogin(page, testAdult)
      const header = new CitizenHeader(page)
      childPage = new CitizenChildPage(page)

      await header.openChildPage(testChild2.id)
      await childPage.openCollapsible('termination')

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
      await Fixture.daycare(testClub).save()

      const endDate = mockedDate.addYears(2)
      await createDaycarePlacement(endDate, testClub.id, 'CLUB')

      await enduserLogin(page, testAdult)
      const header = new CitizenHeader(page)
      childPage = new CitizenChildPage(page)

      await header.openChildPage(testChild2.id)
      await childPage.openCollapsible('termination')

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
        testDaycare.id,
        'DAYCARE',
        startDate
      )

      await enduserLogin(page, testAdult)
      const header = new CitizenHeader(page)
      childPage = new CitizenChildPage(page)

      await header.openChildPage(testChild2.id)
      await childPage.openCollapsible('termination')

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
        testChild2,
        testAdult,
        undefined,
        'DAYCARE',
        null,
        [testDaycare.id],
        true,
        'SENT',
        mockedDate,
        true
      )
      await createApplications({ body: [application] })

      await enduserLogin(page, testAdult)
      const header = new CitizenHeader(page)
      childPage = new CitizenChildPage(page)

      await header.selectTab('applications')
      await new CitizenApplicationsPage(page).assertApplicationExists(
        applicationFixtureId
      )

      await header.openChildPage(testChild2.id)
      await childPage.openCollapsible('termination')
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
      const placements: DevPlacement[] = [
        {
          id: uuidv4(),
          type: 'DAYCARE',
          childId: testChild2.id,
          unitId: testDaycare.id,
          startDate: daycare1Start,
          endDate: daycare1End,
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        },
        {
          id: uuidv4(),
          type: 'DAYCARE',
          childId: testChild2.id,
          unitId: testPreschool.id,
          startDate: daycare2start,
          endDate: daycare2end,
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        },
        {
          id: uuidv4(),
          type: 'PRESCHOOL',
          childId: testChild2.id,
          unitId: testPreschool.id,
          startDate: preschool1Start,
          endDate: preschool1End,
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        },
        {
          id: uuidv4(),
          type: 'PRESCHOOL_DAYCARE', // this gets grouped with the above
          childId: testChild2.id,
          unitId: testPreschool.id,
          startDate: preschool2Start,
          endDate: preschool2End,
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        },
        {
          id: uuidv4(),
          type: 'DAYCARE', // this is shown under PRESCHOOL as "Maksullinen varhaiskasvatus"
          childId: testChild2.id,
          unitId: testPreschool.id,
          startDate: daycareAfterPreschoolStart,
          endDate: daycareAfterPreschoolEnd,
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        }
      ]
      await createDaycarePlacements({ body: placements })

      await enduserLogin(page, testAdult)
      const header = new CitizenHeader(page)
      childPage = new CitizenChildPage(page)

      const labels = {
        daycare1: `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${daycare1End.format()}`,
        daycare2: `Varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${daycare2end.format()}`,
        preschool: `Esiopetus, Alkuräjähdyksen eskari, voimassa ${preschool2End.format()}`,
        daycareAfterPreschool: `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${daycareAfterPreschoolEnd.format()}`
      }

      await header.openChildPage(testChild2.id)
      await childPage.openCollapsible('termination')

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
      const placements: DevPlacement[] = [
        {
          id: uuidv4(),
          type: 'DAYCARE',
          childId: testChild2.id,
          unitId: testDaycare.id,
          startDate: daycare1Start,
          endDate: daycare1End,
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        },
        {
          id: uuidv4(),
          type: 'DAYCARE',
          childId: testChild2.id,
          unitId: testPreschool.id,
          startDate: daycare2start,
          endDate: daycare2end,
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        }
      ]
      await createDaycarePlacements({ body: placements })

      await enduserLogin(page, testAdult)
      const header = new CitizenHeader(page)
      childPage = new CitizenChildPage(page)

      const labels = {
        daycare1: `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${daycare1End.format()}`,
        daycare2: `Varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${daycare2end.format()}`
      }

      await header.openChildPage(testChild2.id)
      await childPage.openCollapsible('termination')

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
      await childPage.assertTerminatablePlacementCount(0)
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
      const placements: DevPlacement[] = [
        {
          id: uuidv4(),
          type: 'PRESCHOOL_DAYCARE', // this gets grouped with the above
          childId: testChild2.id,
          unitId: testPreschool.id,
          startDate: preschool2Start,
          endDate: preschool2End,
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        },
        {
          id: uuidv4(),
          type: 'DAYCARE', // this is shown under PRESCHOOL as "Maksullinen varhaiskasvatus"
          childId: testChild2.id,
          unitId: testPreschool.id,
          startDate: daycareAfterPreschoolStart,
          endDate: daycareAfterPreschoolEnd,
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        }
      ]
      await createDaycarePlacements({ body: placements })

      await enduserLogin(page, testAdult)
      const header = new CitizenHeader(page)
      childPage = new CitizenChildPage(page)

      const labels = {
        preschool: `Esiopetus, Alkuräjähdyksen eskari, voimassa ${preschool2End.format()}`,
        daycareAfterPreschool: `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, voimassa ${daycareAfterPreschoolEnd.format()}`
      }

      await header.openChildPage(testChild2.id)
      await childPage.openCollapsible('termination')

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
        testPreschool.id,
        'PRESCHOOL_DAYCARE'
      )

      await enduserLogin(page, testAdult)
      const header = new CitizenHeader(page)
      childPage = new CitizenChildPage(page)

      await header.openChildPage(testChild2.id)
      await childPage.openCollapsible('termination')

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

      await childPage.assertTerminatedPlacementCount(1) // the paid daycare is not terminated, just split to PRESCHOOL_DAYCARE and PRESCHOOL
      await assertTerminatedPlacements([
        `Maksullinen varhaiskasvatus, Alkuräjähdyksen eskari, viimeinen läsnäolopäivä: ${terminationDate.format()}`
      ])
    })
  })
})

describe.each(['desktop', 'mobile'] as const)(
  'Citizen children page with weak login (%s)',
  (env) => {
    beforeEach(async () => {
      await resetServiceState()
      await Fixture.careArea().with(testCareArea).save()
      await Fixture.daycare(testDaycare).save()
      await Fixture.daycare(testPreschool).save()
      await Fixture.family({
        guardian: testAdult,
        children: [testChild, testChild2]
      }).save()

      const viewport =
        env === 'mobile'
          ? { width: 375, height: 812 }
          : { width: 1920, height: 1080 }
      page = await Page.open({
        viewport,
        screen: viewport,
        mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
      })
    })

    test('Citizen can see its children and their service needs and daily service times', async () => {
      const daycareSupervisor = await Fixture.employeeUnitSupervisor(
        testDaycare.id
      ).save()
      const serviceNeedOption = await Fixture.serviceNeedOption()
        .with({
          validPlacementType: 'DAYCARE',
          defaultOption: false,
          nameFi: 'Kokopäiväinen',
          nameSv: 'Kokopäiväinen (sv)',
          nameEn: 'Kokopäiväinen (en)'
        })
        .save()
      await Fixture.serviceNeedOption()
        .with({
          validPlacementType: 'PRESCHOOL',
          defaultOption: true,
          nameFi: 'Esiopetus',
          nameSv: 'Esiopetus (sv)',
          nameEn: 'Esiopetus (en)'
        })
        .save()
      const placement = await Fixture.placement({
        childId: testChild.id,
        unitId: testDaycare.id,
        type: 'DAYCARE',
        startDate: mockedDate.subMonths(2),
        endDate: mockedDate
      }).save()
      await Fixture.serviceNeed({
        placementId: placement.id,
        startDate: mockedDate.subMonths(1),
        endDate: mockedDate,
        optionId: serviceNeedOption.id,
        confirmedBy: daycareSupervisor.id
      }).save()
      await Fixture.dailyServiceTime(testChild.id)
        .with({
          validityPeriod: new DateRange(
            mockedDate.subMonths(3),
            mockedDate.subMonths(2).subDays(1)
          ),
          type: 'REGULAR',
          regularTimes: new TimeRange(LocalTime.of(8, 15), LocalTime.of(14, 46))
        })
        .save()
      await Fixture.dailyServiceTime(testChild.id)
        .with({
          validityPeriod: new DateRange(
            mockedDate.subMonths(2),
            mockedDate.subMonths(1).subDays(1)
          ),
          type: 'IRREGULAR',
          regularTimes: null,
          mondayTimes: new TimeRange(LocalTime.of(8, 15), LocalTime.of(14, 46)),
          thursdayTimes: new TimeRange(
            LocalTime.of(7, 46),
            LocalTime.of(16, 32)
          )
        })
        .save()
      await Fixture.dailyServiceTime(testChild.id)
        .with({
          validityPeriod: new DateRange(mockedDate.subMonths(1), null),
          type: 'VARIABLE_TIME',
          regularTimes: null
        })
        .save()
      await Fixture.placement({
        childId: testChild2.id,
        unitId: testPreschool.id,
        type: 'PRESCHOOL',
        startDate: mockedDate.subMonths(1),
        endDate: mockedDate
      }).save()

      const keycloak = await KeycloakRealmClient.createCitizenClient()
      await keycloak.deleteAllUsers()
      const account = citizenWeakAccount(testAdult)
      await keycloak.createUser({ ...account, enabled: true })
      await enduserLoginWeak(page, account)
      const header = new CitizenHeader(page, env)
      const childPage = new CitizenChildPage(page, env)

      await header.openChildPage(testChild.id)
      await childPage.assertChildNameIsShown(
        'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani Karhula'
      )
      await childPage.openCollapsible('service-need-and-daily-service-time')
      await childPage.assertServiceNeedTable([
        {
          dateRange: '01.02.2022 - 01.03.2022',
          description: 'Kokopäiväinen',
          unit: 'Alkuräjähdyksen päiväkoti'
        },
        {
          dateRange: '01.01.2022 - 31.01.2022',
          description: '',
          unit: 'Alkuräjähdyksen päiväkoti'
        }
      ])
      await childPage.assertDailyServiceTimeTable([
        {
          dateRange: '01.02.2022 -',
          description: 'Päivittäinen aika vaihtelee'
        },
        {
          dateRange: '01.01.2022 - 31.01.2022',
          description: 'Ma 08:15–14:46, To 07:46–16:32'
        },
        {
          dateRange: '01.12.2021 - 31.12.2021',
          description: '08:15–14:46'
        }
      ])
      await childPage.closeCollapsible()
      await header.openChildPage(testChild2.id)
      await childPage.assertChildNameIsShown('Kaarina Veera Nelli Karhula')
      await childPage.openCollapsible('service-need-and-daily-service-time')
      await childPage.assertServiceNeedTable([
        {
          dateRange: '01.02.2022 - 01.03.2022',
          description: 'Esiopetus',
          unit: 'Alkuräjähdyksen eskari'
        }
      ])
      await childPage.assertDailyServiceTimeTable([])
    })
  }
)
