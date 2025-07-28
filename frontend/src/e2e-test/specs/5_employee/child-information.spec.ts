// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PersonId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  testDaycareGroup,
  testChildDeceased,
  testChildNoSsn,
  Fixture,
  familyWithTwoGuardians,
  testCareArea,
  testDaycare,
  testDaycare2,
  testCareArea2
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  forceFullVtjRefresh,
  putDiets,
  resetServiceState
} from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import type {
  AdditionalInformationSection,
  BackupCaresSection,
  DailyServiceTimeSection,
  FamilyContactsSection,
  GuardiansSection
} from '../../pages/employee/child-information'
import ChildInformationPage from '../../pages/employee/child-information'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformationPage: ChildInformationPage
let childId: PersonId
let admin: DevEmployee

const mockedDate = LocalDate.of(2020, 3, 1)

beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()
  await familyWithTwoGuardians.save()
  await testChildNoSsn.saveChild()
  await createDaycareGroups({ body: [testDaycareGroup] })
  admin = await Fixture.employee().admin().save()

  const unitId = testDaycare.id
  childId = familyWithTwoGuardians.children[0].id
  await Fixture.placement({
    childId,
    unitId
  }).save()

  await Fixture.placement({
    childId,
    unitId,
    startDate: LocalDate.of(2020, 1, 1),
    endDate: LocalDate.of(2020, 6, 1)
  }).save()

  // HACK: make sure VTJ guardians are synced between mock VTJ and database,
  // because some parts of the child information page assumes guardian info
  // is up to date or will return wrong results
  await forceFullVtjRefresh({ person: childId })

  page = await Page.open({
    mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await employeeLogin(page, admin)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.waitUntilLoaded()
})

describe('Child Information - edit child information', () => {
  test('Oph person oid can be edited', async () => {
    await page.goto(
      config.employeeUrl + '/child-information/' + testChildNoSsn.id
    )
    await childInformationPage.waitUntilLoaded()
    await childInformationPage.clickEdit()
    await childInformationPage.assertOphPersonOid('')
    await childInformationPage.setOphPersonOid('1.2.3')
    await childInformationPage.assertOphPersonOid('1.2.3')
  })
})

describe('Child Information - edit additional information', () => {
  let section: AdditionalInformationSection
  beforeEach(() => {
    section = childInformationPage.additionalInformationSection()
  })

  test('medication info and be added and removed', async () => {
    const medication = 'Epipen'

    await section.medication.assertTextEquals('')

    await section.editBtn.click()
    await section.medicationInput.fill(medication)
    await section.confirmBtn.click()
    await section.medication.assertTextEquals(medication)

    await section.editBtn.click()
    await section.medicationInput.fill('')
    await section.confirmBtn.click()
    await section.medication.assertTextEquals('')
  })
  test('Language at home can be edited', async () => {
    const language = 'kreikka'
    const languageSearchText = 'kre'
    const languageId = 'ell'
    const details = 'Puhuu modernia kreikkaa, ei antiikin'

    await page.goto(
      config.employeeUrl + '/child-information/' + testChildNoSsn.id
    )
    await childInformationPage.waitUntilLoaded()
    await section.languageAtHome.assertTextEquals('')
    await section.languageAtHomeDetails.assertTextEquals('')
    await section.editBtn.click()
    await section.languageAtHomeCombobox.fillAndSelectItem(
      languageSearchText,
      `language-${languageId}`
    )
    await section.languageAtHomeDetailsInput.fill(details)
    await section.confirmBtn.click()
    await section.languageAtHome.assertTextEquals(language)
    await section.languageAtHomeDetails.assertTextEquals(details)
  })
  test('Special diet can be edited', async () => {
    await putDiets({
      body: [
        {
          id: 79,
          abbreviation: 'L'
        },
        {
          id: 80,
          abbreviation: 'L G'
        }
      ]
    })
    const dietCaption = 'L'
    const dietSearchTerm = 'L'
    const dietId = 79

    await page.goto(
      config.employeeUrl + '/child-information/' + testChildNoSsn.id
    )
    await childInformationPage.waitUntilLoaded()
    await section.specialDiet.assertTextEquals('-')
    await section.editBtn.click()
    await section.specialDietCombobox.fillAndSelectItem(
      dietSearchTerm,
      `diet-${dietId}`
    )
    await section.confirmBtn.click()
    await section.specialDiet.assertTextEquals(dietCaption)
  })
})

describe('Child Information - deceased child', () => {
  test('Deceased child indicator is shown', async () => {
    await testChildDeceased.saveChild({ updateMockVtj: true })
    await page.goto(
      config.employeeUrl + '/child-information/' + testChildDeceased.id
    )
    await childInformationPage.waitUntilLoaded()
    await childInformationPage.deceasedIconIsShown()
  })
})

describe('Child Information - daily service times', () => {
  let section: DailyServiceTimeSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('dailyServiceTimes')
  })

  const tomorrowDate = mockedDate.addDays(1)
  const tomorrow = tomorrowDate.format()
  const in10Days = mockedDate.addDays(10).format()
  const in40Days = mockedDate.addDays(40).format()

  test('can create regular daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStart.fill(in10Days)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '14:00')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in10Days} –`,
      'UPCOMING'
    )
  })

  test('can create irregular daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStart.fill(in40Days)
    await form.checkType('IRREGULAR')
    await form.fillIrregularTimeRange('monday', '09:00', '10:00')
    await form.fillIrregularTimeRange('wednesday', '04:00', '10:00')
    await form.fillIrregularTimeRange('thursday', '12:00', '18:00')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in40Days} –`,
      'UPCOMING'
    )
  })

  test('can create variable daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStart.fill(in40Days)
    await form.checkType('VARIABLE_TIME')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in40Days} –`,
      'UPCOMING'
    )
  })

  test('can create regular daily service times starting tomorrow', async () => {
    const form = await section.create()
    await form.validityPeriodStart.fill(tomorrow)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '15:00')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${tomorrow} –`,
      'UPCOMING'
    )

    // Status changes to active tomorrow
    const pageTomorrow = await Page.open({
      mockedTime: tomorrowDate.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
    await employeeLogin(pageTomorrow, admin)
    await pageTomorrow.goto(
      config.employeeUrl + '/child-information/' + childId
    )

    childInformationPage = new ChildInformationPage(pageTomorrow)
    await childInformationPage.waitUntilLoaded()
    section = await childInformationPage.openCollapsible('dailyServiceTimes')
    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${tomorrow} –`,
      'ACTIVE'
    )
  })

  test('can create multiple daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStart.fill(tomorrow)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '15:00')
    await form.submit()

    const form2 = await section.create()
    await form2.validityPeriodStart.fill(in10Days)
    await form2.checkType('REGULAR')
    await form2.fillRegularTimeRange('08:00', '19:00')
    await form2.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in10Days} –`,
      'UPCOMING'
    )
    await section.assertTableRow(
      1,
      `Päivittäinen varhaiskasvatusaika ${tomorrow} – ${mockedDate
        .addDays(9)
        .format()}`,
      'UPCOMING'
    )
  })

  test('the currently active daily service times is open by default, and others can be opened', async () => {
    const form = await section.create()
    await form.validityPeriodStart.fill(tomorrow)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '15:00')
    await form.submit()

    const form2 = await section.create()
    await form2.validityPeriodStart.fill(in40Days)
    await form2.checkType('IRREGULAR')
    await form2.fillIrregularTimeRange('wednesday', '12:00', '14:00')
    await form2.fillIrregularTimeRange('friday', '12:00', '18:00')
    await form2.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in40Days} –`,
      'UPCOMING'
    )
    await section.assertTableRow(
      1,
      `Päivittäinen varhaiskasvatusaika ${tomorrow} – ${mockedDate
        .addDays(39)
        .format()}`,
      'UPCOMING'
    )
    await section.toggleTableRowCollapsible(1)
    await section.assertTableRowCollapsible(
      1,
      'Säännöllinen varhaiskasvatusaika\nmaanantai–perjantai 08:00–15:00'
    )
    await section.toggleTableRowCollapsible(0)
    await section.assertTableRowCollapsible(
      0,
      'Epäsäännöllinen varhaiskasvatusaika\nkeskiviikko 12:00–14:00, perjantai 12:00–18:00'
    )
  })

  test('can modify daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStart.fill(in10Days)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '14:00')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in10Days} –`,
      'UPCOMING'
    )

    const editor = await section.editTableRow(0)

    await editor.checkType('IRREGULAR')
    await editor.fillIrregularTimeRange('monday', '09:00', '15:00')
    await editor.fillIrregularTimeRange('friday', '13:00', '14:00')
    await editor.submit()

    await section.toggleTableRowCollapsible(0)
    await section.assertTableRowCollapsible(
      0,
      'Epäsäännöllinen varhaiskasvatusaika\nmaanantai 09:00–15:00, perjantai 13:00–14:00'
    )
  })

  test('can delete daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStart.fill(in10Days)
    await form.checkType('VARIABLE_TIME')
    await form.submit()

    await section.assertTableRowCount(1)
    await section.deleteTableRow(0)
    await section.assertTableRowCount(0)
  })
})

describe('Child information - backup care', () => {
  let section: BackupCaresSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('backupCares')
  })

  test('backup care for a child can be added and removed', async () => {
    await section.createBackupCare(
      testDaycare.name,
      LocalDate.of(2020, 2, 1),
      LocalDate.of(2020, 2, 3)
    )
    await waitUntilEqual(
      () => section.getBackupCares(),
      [
        {
          unit: testDaycare.name,
          period: '01.02.2020 - 03.02.2020'
        }
      ]
    )
    await section.deleteBackupCare(0)
    await waitUntilEqual(() => section.getBackupCares(), [])
  })

  test('error is shown if no placement is during the requested range', async () => {
    await section.fillNewBackupCareFields(
      testDaycare.name,
      LocalDate.of(2020, 1, 5),
      LocalDate.of(2020, 9, 5)
    )
    await section.assertError(
      'Varasijoitus ei ole minkään lapsen sijoituksen aikana.'
    )
  })

  test('error is shown if no placement is during the modified range', async () => {
    await section.createBackupCare(
      testDaycare.name,
      LocalDate.of(2020, 1, 2),
      LocalDate.of(2020, 2, 3)
    )

    await section.fillExistingBackupCareRow(
      0,
      LocalDate.of(2020, 5, 4),
      LocalDate.of(2020, 8, 11)
    )
    await section.assertError(
      'Varasijoitus ei ole minkään lapsen sijoituksen aikana.'
    )
  })

  test('error is shown if child has ongoing attendance in another unit', async () => {
    await testCareArea2.save()
    await testDaycare2.save()
    await Fixture.childAttendance({
      unitId: testDaycare.id,
      childId,
      date: mockedDate,
      arrived: LocalTime.of(8, 0),
      departed: null
    }).save()
    await section.fillNewBackupCareFields(
      testDaycare2.name,
      mockedDate,
      mockedDate
    )
    await section.assertError('Lapsi on jo kirjattu sisään toiseen yksikköön.')
  })

  test('error is shown if backup care unit is not open for the whole period', async () => {
    const area = await Fixture.careArea().save()
    const testDaycare = await Fixture.daycare({
      areaId: area.id,
      openingDate: mockedDate.addMonths(1),
      closingDate: mockedDate.addMonths(2)
    }).save()
    const testChild = await Fixture.person().saveChild()
    await Fixture.placement({
      childId: testChild.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: mockedDate.addMonths(3)
    }).save()

    // Backup care period is before the daycare opening date
    await section.fillNewBackupCareFields(
      testDaycare.name,
      mockedDate,
      mockedDate
    )
    await section.assertError(
      'Yksikkö ei ole avoinna koko varasijoituksen ajan.'
    )
    await section.cancelBackupCareForm()

    // Backup care period starts before the daycare opening date
    await section.fillNewBackupCareFields(
      testDaycare.name,
      mockedDate.addMonths(1).subDays(1),
      mockedDate.addMonths(2)
    )
    await section.assertError(
      'Yksikkö ei ole avoinna koko varasijoituksen ajan.'
    )
    await section.cancelBackupCareForm()

    // Backup care period ends after the daycare closing date
    await section.fillNewBackupCareFields(
      testDaycare.name,
      mockedDate.addMonths(1),
      mockedDate.addMonths(2).addDays(1)
    )
    await section.assertError(
      'Yksikkö ei ole avoinna koko varasijoituksen ajan.'
    )
    await section.cancelBackupCareForm()

    // Backup care period is after the daycare closing date
    await section.fillNewBackupCareFields(
      testDaycare.name,
      mockedDate.addMonths(2).addDays(1),
      mockedDate.addMonths(3)
    )
    await section.assertError(
      'Yksikkö ei ole avoinna koko varasijoituksen ajan.'
    )
    await section.cancelBackupCareForm()

    // Backup care period is during the daycare opening and closing dates
    await section.createBackupCare(
      testDaycare.name,
      mockedDate.addMonths(1),
      mockedDate.addMonths(2)
    )
  })
})

describe('Child information - backup pickups', () => {
  let section: FamilyContactsSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('familyContacts')
  })

  test('backup pickups can be added and deleted', async () => {
    const name1 = 'Mikko Mallikas'
    const phone1 = '123456'
    const name2 = 'Jaana Mallikas'
    const phone2 = '987235'
    await section.addBackupPickup(name1, phone1)
    await section.addBackupPickup(name2, phone2)
    await section.assertBackupPickupExists(name1)
    await section.assertBackupPickupExists(name2)
    await section.deleteBackupPickup(name2)
    await section.assertBackupPickupExists(name1)
    await section.assertBackupPickupDoesNotExist(name2)
  })
})

describe('Child information - family contacts', () => {
  let section: FamilyContactsSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('familyContacts')
  })

  test('email, phone and backup phone can be edited', async () => {
    const id = familyWithTwoGuardians.guardian.id
    await section.modifyFamilyContactDetails(id, {
      email: 'foo@example.com',
      phone: '31459265',
      backupPhone: '98765432'
    })
    await section.assertFamilyContactDetails(id, {
      email: 'foo@example.com',
      phone: '31459265',
      backupPhone: '98765432'
    })
  })

  test('email, phone and backup phone can be edited after unsetting them', async () => {
    const id = familyWithTwoGuardians.guardian.id
    await section.modifyFamilyContactDetails(id, {
      email: '',
      phone: '',
      backupPhone: ''
    })
    await section.assertFamilyContactDetails(id, {
      email: '',
      phone: '',
      backupPhone: ''
    })
    await section.modifyFamilyContactDetails(id, {
      email: 'foo@example.com',
      phone: '31459265',
      backupPhone: '98765432'
    })
    await section.assertFamilyContactDetails(id, {
      email: 'foo@example.com',
      phone: '31459265',
      backupPhone: '98765432'
    })
  })
})

describe('Child information - guardian information', () => {
  let section: GuardiansSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('guardians')
  })

  test('guardian information is shown', async () => {
    await section.assertGuardianExists(familyWithTwoGuardians.guardian.id)
  })

  test('guardian information is shown to unit supervisor', async () => {
    const unitSupervisor: DevEmployee = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()
    page = await Page.open({
      mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
    await employeeLogin(page, unitSupervisor)
    await page.goto(config.employeeUrl + '/child-information/' + childId)
    childInformationPage = new ChildInformationPage(page)
    await childInformationPage.waitUntilLoaded()
    await childInformationPage.openCollapsible('guardians')
    await section.assertGuardianExists(familyWithTwoGuardians.guardian.id)
  })
})
