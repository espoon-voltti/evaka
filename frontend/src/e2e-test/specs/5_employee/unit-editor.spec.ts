// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { Fixture, testCareArea, testDaycare } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevDaycare } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import {
  UnitEditor,
  UnitInfoPage,
  UnitPage
} from '../../pages/employee/units/unit'
import UnitsPage from '../../pages/employee/units/units'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const today = LocalDate.of(2024, 3, 1)
const now = today.toHelsinkiDateTime(LocalTime.of(12, 0))

describe('Employee - unit details', () => {
  let page: Page
  let unitsPage: UnitsPage
  let daycare1: DevDaycare

  beforeEach(async () => {
    await resetServiceState()
    await Fixture.careArea(testCareArea).save()
    await Fixture.daycare(testDaycare).save()
    daycare1 = testDaycare
    const admin = await Fixture.employee().admin().save()

    page = await Page.open({ mockedTime: now })
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl)
    await new EmployeeNav(page).openTab('units')
    unitsPage = new UnitsPage(page)
  })

  test('Admin creates a new unit', async () => {
    const unitEditorPage = await unitsPage.openNewUnitEditor()

    await unitEditorPage.fillUnitName('Uusi Kerho')
    await unitEditorPage.chooseArea('Superkeskus')
    await unitEditorPage.selectCareType('CLUB')
    await unitEditorPage.toggleApplicationType('CLUB')
    await unitEditorPage.fillVisitingAddress('Kamreerintie 1', '02100', 'Espoo')
    await unitEditorPage.fillManagerData(
      'Kerhon Johtaja',
      '01234567',
      'manager@example.com'
    )

    await unitEditorPage.submit()
    const unitInfoPage = new UnitInfoPage(page)
    await unitInfoPage.waitUntilLoaded()
    await unitInfoPage.assertUnitName('Uusi Kerho')
  })

  test('Admin can edit unit details', async () => {
    const unitEditorPage = await UnitEditor.openById(page, daycare1.id)

    await unitEditorPage.fillManagerData(
      'Päiväkodin Johtaja',
      '01234567',
      'manager@example.com'
    )

    // The daycare fixture has this on but it needs to be turned off to be able to save
    await unitEditorPage.setInvoiceByMunicipality(false)

    await unitEditorPage.fillDayTimeRange(3, '10:00', '16:00')

    const unitDetailsPage = await unitEditorPage.submit()

    await unitDetailsPage.assertTimeRangeByDay(3, '10:00 - 16:00')
    await unitDetailsPage.assertUnitName(daycare1.name)
    await unitDetailsPage.assertManagerData(
      'Päiväkodin Johtaja',
      '01234567',
      'manager@example.com'
    )
  })

  test('Admin can edit meal times', async () => {
    const unitEditorPage = await UnitEditor.openById(page, daycare1.id)

    // fill required fields not filled by daycare fixture
    await unitEditorPage.fillManagerData(
      'Päiväkodin Johtaja',
      '01234567',
      'manager@example.com'
    )

    const mealTimes = {
      mealtimeBreakfast: { start: '08:00', end: '08:30' },
      mealtimeLunch: { start: '11:00', end: '11:30' },
      mealtimeSnack: { start: '13:00', end: '13:30' }
    }
    await unitEditorPage.fillMealTimes(mealTimes)

    const unitDetailsPage = await unitEditorPage.submit()
    await unitDetailsPage.assertMealTimes(mealTimes)
  })
})

describe('Employee - unit editor validations and warnings', () => {
  beforeEach(async () => {
    await resetServiceState()

    await Fixture.careArea(testCareArea).save()
    await Fixture.daycare(testDaycare).save()
  })

  const openUnitEditorPage = async () => {
    const admin = await Fixture.employee().admin().save()
    const page = await Page.open()
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, testDaycare.id)
    const unitInfoPage = await unitPage.openUnitInformation()
    const unitDetailsPage = await unitInfoPage.openUnitDetails()
    return await unitDetailsPage.edit()
  }

  test('Unit closing date error is shown if there are active placements', async () => {
    const placementStart = today.addMonths(1)
    const placementEnd = today.addYears(1)

    const child = await Fixture.person().saveChild()
    await Fixture.placement({
      childId: child.id,
      unitId: testDaycare.id,
      startDate: placementStart,
      endDate: placementEnd
    }).save()

    const unitEditorPage = await openUnitEditorPage()
    await unitEditorPage.assertWarningIsNotVisible('unit-closing-placement')
    await unitEditorPage.selectClosingDate(placementEnd.subDays(1))
    await unitEditorPage.assertWarningIsVisible('unit-closing-placement')
    await unitEditorPage.clearClosingDate()
    await unitEditorPage.assertWarningIsNotVisible('closing-date-warning')
  })

  test('Invalid unit operation times produce a form error', async () => {
    const unitEditorPage = await openUnitEditorPage()
    await unitEditorPage.assertWarningIsNotVisible('unit-operationtimes')
    await unitEditorPage.fillDayTimeRange(2, '10:00', '10:66')
    await unitEditorPage.assertWarningIsVisible('unit-operationtimes')
    await unitEditorPage.clearDayTimeRange(2)
    await unitEditorPage.assertWarningIsNotVisible('unit-operationtimes')

    await unitEditorPage.fillDayTimeRange(3, '12:00', '10:00')
    await unitEditorPage.assertWarningIsVisible('unit-operationtimes')
    await unitEditorPage.clearDayTimeRange(3)
    await unitEditorPage.assertWarningIsNotVisible('unit-operationtimes')
  })

  test('Invalid unit shift care operation times produce a form error', async () => {
    const unitEditorPage = await openUnitEditorPage()
    await unitEditorPage.assertWarningIsNotVisible(
      'shift-care-unit-operationtimes'
    )
    await unitEditorPage.fillShiftCareDayTimeRange(6, '10:00', '10:66')
    await unitEditorPage.assertWarningIsVisible(
      'unit-shift-care-operationtimes'
    )
    await unitEditorPage.clearShiftCareDayTimeRange(6)
    await unitEditorPage.assertWarningIsNotVisible(
      'unit-shift-care-operationtimes'
    )

    await unitEditorPage.fillShiftCareDayTimeRange(6, '12:00', '10:00')
    await unitEditorPage.assertWarningIsVisible(
      'unit-shift-care-operationtimes'
    )
    await unitEditorPage.clearShiftCareDayTimeRange(6)
    await unitEditorPage.assertWarningIsNotVisible(
      'unit-shift-care-operationtimes'
    )

    await unitEditorPage.fillShiftCareDayTimeRange(7, '16:00', '22:00')

    await unitEditorPage.fillManagerData(
      'Päiväkodin Johtaja',
      '01234567',
      'manager@example.com'
    )

    const unitDetailsPage = await unitEditorPage.submit()
    await unitDetailsPage.assertShiftCareOperationTime(6, '')
    await unitDetailsPage.assertShiftCareOperationTime(7, '16:00 - 22:00')
  })

  test('Varda unit warning is shown for non varda units', async () => {
    const unitEditorPage = await openUnitEditorPage()
    await unitEditorPage.selectProviderType('MUNICIPAL')
    await unitEditorPage.assertWarningIsNotVisible('send-to-varda-warning')
    await unitEditorPage.selectProviderType('PRIVATE')
    await unitEditorPage.assertWarningIsVisible('send-to-varda-warning')
  })

  test('Municipal, service voucher and purchased units shows warning if handler address is missing', async () => {
    const unitEditorPage = await openUnitEditorPage()
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'MUNICIPAL',
      '',
      true
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'MUNICIPAL',
      'An address',
      false
    )

    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'MUNICIPAL_SCHOOL',
      '',
      true
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'MUNICIPAL_SCHOOL',
      'An address',
      false
    )

    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PURCHASED',
      '',
      true
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PURCHASED',
      'An address',
      false
    )

    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PRIVATE_SERVICE_VOUCHER',
      '',
      true
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PRIVATE_SERVICE_VOUCHER',
      'An address',
      false
    )

    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PRIVATE',
      '',
      false
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'EXTERNAL_PURCHASED',
      '',
      false
    )
  })
})
