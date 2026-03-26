// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type {
  DevCareArea,
  DevDaycare,
  DevEmployee
} from '../../generated/api-types'
import { UnitPage } from '../../pages/employee/units/unit'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedTime = LocalDate.of(2022, 12, 1).toHelsinkiDateTime(
  LocalTime.of(12, 0)
)

test.use({ evakaOptions: { mockedTime } })

test.describe('Employee - Unit - Service worker note', () => {
  let area: DevCareArea
  let daycare: DevDaycare
  let serviceWorker: DevEmployee
  let page: Page

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    area = await Fixture.careArea().save()
    daycare = await Fixture.daycare({ areaId: area.id }).save()
    serviceWorker = await Fixture.employee().serviceWorker().save()

    page = evaka
  })

  test('happy path', async () => {
    await employeeLogin(page, serviceWorker)
    const unitPage = new UnitPage(page)
    await unitPage.navigateToUnit(daycare.id)

    await expect(unitPage.serviceWorkerNote.addButton).toBeVisible()
    await expect(unitPage.serviceWorkerNote.content).toBeHidden()

    await unitPage.serviceWorkerNote.addButton.click()
    const text1 = 'Väistötiloissa joulukuussa, esteellinen sijainti'
    await unitPage.serviceWorkerNote.input.fill(text1)
    await unitPage.serviceWorkerNote.saveButton.click()
    await expect(unitPage.serviceWorkerNote.saveButton).toBeHidden()
    await expect(unitPage.serviceWorkerNote.content).toHaveText(text1, {
      useInnerText: true
    })

    await unitPage.serviceWorkerNote.editButton.click()
    const text2 = 'Väistötiloissa marraskuussa 2025, esteellinen sijainti'
    await unitPage.serviceWorkerNote.input.fill(text2)
    await unitPage.serviceWorkerNote.saveButton.click()
    await expect(unitPage.serviceWorkerNote.saveButton).toBeHidden()
    await expect(unitPage.serviceWorkerNote.content).toHaveText(text2, {
      useInnerText: true
    })

    await unitPage.serviceWorkerNote.removeButton.click()
    await expect(unitPage.serviceWorkerNote.addButton).toBeVisible()
    await expect(unitPage.serviceWorkerNote.content).toBeHidden()
  })
})
