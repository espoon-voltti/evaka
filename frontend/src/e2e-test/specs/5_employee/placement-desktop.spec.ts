// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { execSimpleApplicationActions } from '../../dev-api'
import { applicationFixture, Fixture } from '../../dev-api/fixtures'
import {
  createApplications,
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import type { DevApplicationWithForm } from '../../generated/api-types'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import PlacementDesktopView from '../../pages/employee/applications/placement-desktop-view'
import { PlacementDraftPage } from '../../pages/employee/placement-draft-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

import '../../jest'

const mockedDate = LocalDate.of(2021, 8, 16)

let page: Page
let applicationListView: ApplicationListView
let placementDesktopView: PlacementDesktopView

const careArea = Fixture.careArea()
const daycare1 = Fixture.daycare({ areaId: careArea.id, name: 'Daycare 1' })
const daycare2 = Fixture.daycare({ areaId: careArea.id, name: 'Daycare 2' })
const daycare3 = Fixture.daycare({ areaId: careArea.id, name: 'Daycare 3' })
const daycares = [daycare1, daycare2, daycare3]
const serviceWorker = Fixture.employee().serviceWorker()

const adult = Fixture.person()
const child1 = Fixture.person({
  ssn: '060820A807H',
  dateOfBirth: LocalDate.of(2020, 8, 6),
  firstName: 'Hupu',
  lastName: 'Ankka'
})
const child2 = Fixture.person({
  ssn: '050317A420K',
  dateOfBirth: LocalDate.of(2017, 3, 5),
  firstName: 'Lupu',
  lastName: 'Ankka'
})
const child3 = Fixture.person({
  ssn: '190816A0360',
  dateOfBirth: LocalDate.of(2016, 8, 19),
  firstName: 'Tupu',
  lastName: 'Ankka'
})
const child4 = Fixture.person({
  ssn: '180216A789T',
  dateOfBirth: LocalDate.of(2016, 2, 18)
})

const preferredStartDate = mockedDate.addMonths(1)
const dueDate = mockedDate.addMonths(2)
const application1: DevApplicationWithForm = {
  ...applicationFixture(
    child1,
    adult,
    undefined,
    'DAYCARE',
    null,
    [daycare1.id],
    false,
    'SENT',
    preferredStartDate
  ),
  id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002'),
  sentDate: mockedDate.subMonths(1),
  dueDate
}

const application2: DevApplicationWithForm = {
  ...applicationFixture(
    child2,
    adult,
    undefined,
    'DAYCARE',
    null,
    [daycare1.id, daycare2.id],
    false,
    'SENT',
    preferredStartDate
  ),
  id: fromUuid<ApplicationId>('096854bd-8db7-45c1-bf3c-bc9c48992096'),
  sentDate: mockedDate.subMonths(1),
  dueDate
}

beforeEach(async () => {
  await resetServiceState()
  await createDefaultServiceNeedOptions()
  await careArea.save()
  for (const daycare of daycares) {
    await daycare.save()
    const group = await Fixture.daycareGroup({
      daycareId: daycare.id
    }).save()
    await Fixture.daycareCaretakers({
      groupId: group.id,
      startDate: mockedDate,
      amount: 1
    }).save()
  }
  await serviceWorker.save()

  await Fixture.family({
    guardian: adult,
    children: [child1, child2, child3, child4]
  }).save()

  // Create an existing placement to show confirmed occupancy values
  await Fixture.placement({
    unitId: daycare1.id,
    childId: child4.id,
    startDate: mockedDate
  }).save()

  const applications = [application1, application2]
  await createApplications({ body: applications })
  for (const application of applications) {
    await execSimpleApplicationActions(
      application.id,
      ['MOVE_TO_WAITING_PLACEMENT'],
      mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
    )
  }

  page = await Page.open({
    mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  applicationListView = new ApplicationListView(page)
  placementDesktopView = new PlacementDesktopView(page)
})

describe('Placement desktop', () => {
  test('Data is shown', async () => {
    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationListView.filterByApplicationStatus('WAITING_PLACEMENT')
    await applicationListView.searchButton.click()
    await applicationListView.showAsDesktop()

    await placementDesktopView.applicationCards.assertCount(2)
    const appCard1 = placementDesktopView.applicationCard(0)
    await appCard1.childName.assertTextEquals('Ankka Hupu')
    await appCard1.dueDate.assertTextEquals(dueDate.format())
    await appCard1.preferredStartDate.assertTextEquals(
      preferredStartDate.format()
    )
    await appCard1.unitPreferences.assertCount(1)
    await appCard1
      .unitPreference(0)
      .title.assertTextEquals(`1. ${daycare1.name}`)
    const appCard2 = placementDesktopView.applicationCard(1)
    await appCard2.childName.assertTextEquals('Ankka Lupu')
    await appCard2.dueDate.assertTextEquals(dueDate.format())
    await appCard2.preferredStartDate.assertTextEquals(
      preferredStartDate.format()
    )
    await appCard2.unitPreferences.assertCount(2)
    await appCard2
      .unitPreference(0)
      .title.assertTextEquals(`1. ${daycare1.name}`)
    await appCard2
      .unitPreference(1)
      .title.assertTextEquals(`2. ${daycare2.name}`)

    await placementDesktopView.daycareCards.assertCount(1)
    const daycareCard1 = placementDesktopView.daycareCard(0)
    await daycareCard1.name.assertTextEquals(daycare1.name)
    await daycareCard1.assertOccupancies(14.3, 14.3, 14.3)
  })

  test('Draft placement and placement plan', async () => {
    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationListView.filterByApplicationStatus('WAITING_PLACEMENT')
    await applicationListView.searchButton.click()
    await applicationListView.showAsDesktop()

    const appCard2 = placementDesktopView.applicationCard(1)

    await placementDesktopView.daycareCards.assertCount(1)
    const daycareCard1 = placementDesktopView.daycareCard(0)
    await daycareCard1.assertOccupancies(14.3, 14.3, 14.3)

    // view daycare 2
    await appCard2
      .unitPreference(1)
      .createPlacementDraftButton.waitUntilHidden()
    await appCard2.unitPreference(1).showUnitButton.click()
    await appCard2
      .unitPreference(1)
      .createPlacementDraftButton.waitUntilVisible()
    await placementDesktopView.daycareCards.assertCount(2)
    const daycareCard2 = placementDesktopView.daycareCard(1)
    await daycareCard2.assertOccupancies(0, 0, 0)

    // draft place to daycare 2
    await appCard2.unitPreference(1).createPlacementDraftButton.click()
    await daycareCard2.draftPlacementRows.assertCount(1)
    await daycareCard2.assertOccupancies(0, 0, 14.3)

    // cancel draft placement
    await appCard2.unitPreference(1).cancelPlacementDraftButton.click()
    await daycareCard2.assertOccupancies(0, 0, 0)
    await daycareCard2.draftPlacementRows.assertCount(0)

    // place to daycare 3 through combobox
    await appCard2.addOtherUnitButton.click()
    await appCard2.draftPlacementCombobox.fillAndSelectFirst(daycare3.name)
    await placementDesktopView.daycareCards.assertCount(3)
    const daycareCard3 = placementDesktopView.daycareCard(2)
    await daycareCard3.draftPlacementRows.assertCount(1)

    // cancel draft placement through the draft placement row
    const draftPlacementRow = daycareCard3.drawPlacementRow(0)
    await draftPlacementRow.childName.assertTextEquals('Ankka Lupu')
    await draftPlacementRow.cancelPlacementDraftButton.click()
    await daycareCard3.assertOccupancies(0, 0, 0)

    // hide daycare 3
    await daycareCard3.hideUnitButton.click()
    await placementDesktopView.daycareCards.assertCount(2)

    // draft place to daycare 1
    await appCard2.unitPreference(0).createPlacementDraftButton.click()
    await daycareCard1.draftPlacementRows.assertCount(1)
    await daycareCard1.assertOccupancies(14.3, 14.3, 28.6)

    // edit placement start
    await appCard2.unitPreference(0).assertPlacementDate(preferredStartDate)
    await daycareCard1
      .drawPlacementRow(0)
      .assertPlacementDate(preferredStartDate)
    await appCard2.unitPreference(0).editPlacementDateButton.click()
    await appCard2.unitPreference(0).placementDatePicker.fill(dueDate)
    await appCard2.unitPreference(0).savePlacementDateButton.click()
    await appCard2.unitPreference(0).assertPlacementDate(dueDate)
    await daycareCard1.drawPlacementRow(0).assertPlacementDate(dueDate)

    // create placement plan
    await appCard2.toPlacementPlanButton.click()
    const placementDraftPage = new PlacementDraftPage(page)
    await placementDraftPage.submit()

    // verify that draft is removed and planned occupancy updated
    await placementDesktopView.daycareCards.assertCount(2)
    await daycareCard1.assertOccupancies(14.3, 28.6, 28.6)
    await daycareCard1.draftPlacementRows.assertCount(0)
    await placementDesktopView.applicationCards.assertCount(1)
  })

  test('Verify correct number of requests for performance reasons', async () => {
    let requestsMadeToApplicationSearch = 0
    let requestsMadeToDaycares = 0
    let requestsMadeToDaycare1 = 0
    let requestsMadeToDaycare2 = 0
    page.onRequest((req) => {
      if (req.url().includes('/applications/search')) {
        requestsMadeToApplicationSearch++
      }
      if (req.url().includes('/placement-desktop/daycares?unitIds')) {
        requestsMadeToDaycares++
      }
      if (req.url().includes(`/placement-desktop/daycares/${daycare1.id}`)) {
        requestsMadeToDaycare1++
      }
      if (req.url().includes(`/placement-desktop/daycares/${daycare2.id}`)) {
        requestsMadeToDaycare2++
      }
    })

    let requestsExpectedToApplicationSearch = 0
    let requestsExpectedToDaycares = 0
    const requestsExpectedToDaycare1 = 0
    let requestsExpectedToDaycare2 = 0

    const assertRequests = async () =>
      waitUntilEqual(
        () =>
          Promise.resolve([
            requestsMadeToApplicationSearch,
            requestsMadeToDaycares,
            requestsMadeToDaycare1,
            requestsMadeToDaycare2
          ]),
        [
          requestsExpectedToApplicationSearch,
          requestsExpectedToDaycares,
          requestsExpectedToDaycare1,
          requestsExpectedToDaycare2
        ]
      )

    await assertRequests()
    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationListView.filterByApplicationStatus('WAITING_PLACEMENT')
    await applicationListView.searchButton.click()
    requestsExpectedToApplicationSearch++
    await assertRequests()

    await applicationListView.showAsDesktop()
    requestsExpectedToDaycares++
    await assertRequests()

    const appCard2 = placementDesktopView.applicationCard(1)

    // view daycare 2
    await appCard2.unitPreference(1).showUnitButton.click()
    requestsExpectedToDaycare2++
    await assertRequests()

    // change occupancy period
    await placementDesktopView.occupancyPeriodStartPicker.fill(
      mockedDate.addWeeks(2)
    )
    await placementDesktopView.occupancyPeriodEnd.assertTextEquals(
      mockedDate.addWeeks(2).addMonths(3).format()
    )
    requestsExpectedToDaycares++
    await assertRequests()

    // for the final check wait for any more requests to be made and double check counts
    await page.waitForTimeout(500)
    await assertRequests()
  })
})
