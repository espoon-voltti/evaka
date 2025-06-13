// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { GroupId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import { Fixture, testCareArea, testDaycare } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevDaycare, DevEmployee } from '../../generated/api-types'
import { UnitPage } from '../../pages/employee/units/unit'
import type { UnitGroupsPage } from '../../pages/employee/units/unit-groups-page'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
const groupId1 = randomId<GroupId>()
const groupId2 = randomId<GroupId>()

let daycare: DevDaycare
let unitSupervisor: DevEmployee

beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()
  daycare = testDaycare

  unitSupervisor = await Fixture.employee().unitSupervisor(daycare.id).save()

  await Fixture.nekkuCustomer({
    number: '20012061',
    name: 'Ruokahävikin päiväkoti',
    group: 'Varhaiskasvatus',
    customerType: [
      {
        weekdays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
        type: '100- lasta'
      }
    ]
  }).save()

  await Fixture.daycareGroup({
    id: groupId1,
    daycareId: daycare.id,
    name: 'Ruokailijat',
    nekkuCustomerNumber: '20012061'
  }).save()

  await Fixture.daycareGroup({
    id: groupId2,
    daycareId: daycare.id,
    name: 'Testailijat',
    nekkuCustomerNumber: null
  }).save()
})

const loadUnitGroupsPage = async (): Promise<UnitGroupsPage> => {
  unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(daycare.id)
  const groupsPage = await unitPage.openGroupsPage()
  await groupsPage.waitUntilVisible()
  return groupsPage
}

describe('Nekku manual orders', () => {
  beforeEach(async () => {
    page = await Page.open({
      employeeCustomizations: {
        featureFlags: {
          nekkuIntegration: true
        }
      },
      mockedTime: HelsinkiDateTime.of(2025, 6, 9, 12, 0, 0)
    })
    await employeeLogin(page, unitSupervisor)
  })

  test('Nekku order button is shown for group which has Nekku customer number set', async () => {
    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.assertGroupCollapsibleHasNekkuOrderButton(groupId1)
    await groupsPage.assertGroupCollapsibleNotHasNekkuOrderButton(groupId2)
  })

  test('Nekku order can be made for tomorrow', async () => {
    const groupsPage = await loadUnitGroupsPage()
    const nekkuOrderModal = await groupsPage.openNekkuOrderModal(groupId1)
    await nekkuOrderModal.datePicker.fill(LocalDate.of(2025, 6, 10))
    await nekkuOrderModal.okButton.click()
  })
})
