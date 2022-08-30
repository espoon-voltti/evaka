// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { EmployeeDetail } from 'e2e-test/dev-api/types'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { insertDaycareGroupFixtures, resetDatabase } from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  daycareGroupFixture,
  enduserDeceasedChildFixture,
  enduserNonSsnChildFixture,
  Fixture
} from '../../dev-api/fixtures'
import ChildInformationPage, {
  AdditionalInformationSection,
  BackupCaresSection,
  ConsentsSection,
  DailyServiceTimeSection,
  FamilyContactsSection,
  GuardiansSection
} from '../../pages/employee/child-information'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformationPage: ChildInformationPage
let fixtures: AreaAndPersonFixtures
let childId: UUID
let admin: EmployeeDetail

const mockedDate = LocalDate.of(2022, 3, 1)

beforeEach(async () => {
  await resetDatabase()

  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])
  admin = (await Fixture.employeeAdmin().save()).data

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  await Fixture.placement()
    .with({
      childId,
      unitId
    })
    .save()

  page = await Page.open({ mockedTime: mockedDate.toSystemTzDate() })
  await employeeLogin(page, admin)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.waitUntilLoaded()
})

describe('Child Information - edit child information', () => {
  test('Oph person oid can be edited', async () => {
    await page.goto(
      config.employeeUrl + '/child-information/' + enduserNonSsnChildFixture.id
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

    await section.assertMedication('')

    await section.edit()
    await section.fillMedication(medication)
    await section.save()
    await section.assertMedication(medication)

    await section.edit()
    await section.fillMedication('')
    await section.save()
    await section.assertMedication('')
  })
})

describe('Child Information - deceased child', () => {
  test('Deceased child indicator is shown', async () => {
    await page.goto(
      config.employeeUrl +
        '/child-information/' +
        enduserDeceasedChildFixture.id
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

  const today = mockedDate
  const in10Days = mockedDate.addDays(10)
  const in40Days = mockedDate.addDays(40)

  test('can create regular daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(in10Days)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '14:00')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in10Days.format()} –`,
      'UPCOMING'
    )
  })

  test('can create irregular daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(in40Days)
    await form.checkType('IRREGULAR')
    await form.fillIrregularTimeRange('monday', '09:00', '10:00')
    await form.fillIrregularTimeRange('wednesday', '04:00', '10:00')
    await form.fillIrregularTimeRange('thursday', '12:00', '18:00')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in40Days.format()} –`,
      'UPCOMING'
    )
  })

  test('can create variable daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(in40Days)
    await form.checkType('VARIABLE_TIME')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in40Days.format()} –`,
      'UPCOMING'
    )
  })

  test('can create regular daily service times starting today', async () => {
    const form = await section.create()
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(today)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '15:00')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${today.format()} –`,
      'ACTIVE'
    )
  })

  test('can create multiple daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(today)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '15:00')
    await form.submit()

    const form2 = await section.create()
    await form2.validityPeriodStartInput.clear()
    await form2.validityPeriodStartInput.fill(in10Days)
    await form2.checkType('REGULAR')
    await form2.fillRegularTimeRange('08:00', '19:00')
    await form2.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in10Days.format()} –`,
      'UPCOMING'
    )
    await section.assertTableRow(
      1,
      `Päivittäinen varhaiskasvatusaika ${today.format()} – ${mockedDate
        .addDays(9)
        .format()}`,
      'ACTIVE'
    )
  })

  test('the currently active daily service times is open by default, and others can be opened', async () => {
    const form = await section.create()
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(today)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '15:00')
    await form.submit()

    const form2 = await section.create()
    await form2.validityPeriodStartInput.clear()
    await form2.validityPeriodStartInput.fill(in40Days)
    await form2.checkType('IRREGULAR')
    await form2.fillIrregularTimeRange('wednesday', '12:00', '14:00')
    await form2.fillIrregularTimeRange('friday', '12:00', '18:00')
    await form2.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in40Days.format()} –`,
      'UPCOMING'
    )
    await section.assertTableRow(
      1,
      `Päivittäinen varhaiskasvatusaika ${today.format()} – ${mockedDate
        .addDays(39)
        .format()}`,
      'ACTIVE'
    )
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
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(in10Days)
    await form.checkType('REGULAR')
    await form.fillRegularTimeRange('08:00', '14:00')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in10Days.format()} –`,
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
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(in10Days)
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
      fixtures.daycareFixture,
      LocalDate.of(2020, 2, 1),
      LocalDate.of(2020, 2, 3)
    )
    await waitUntilEqual(
      () => section.getBackupCares(),
      [
        {
          unit: fixtures.daycareFixture.name,
          period: '01.02.2020 - 03.02.2020'
        }
      ]
    )
    await section.deleteBackupCare(0)
    await waitUntilEqual(() => section.getBackupCares(), [])
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

  test('phone can be edited', async () => {
    const id = fixtures.familyWithTwoGuardians.guardian.id
    await section.assertFamilyContactExists(id)
    await section.assertFamilyContactPhone(id, '123456789')
    await section.setFamilyContactPhone(id, '31459265')
    await section.assertFamilyContactPhone(id, '31459265')
  })

  test('phone is editable after unset', async () => {
    const id = fixtures.familyWithTwoGuardians.guardian.id
    await section.assertFamilyContactExists(id)
    await section.setFamilyContactPhone(id, '')
    await section.assertFamilyContactPhone(id, '')
    await section.setFamilyContactPhone(id, '31459265')
    await section.assertFamilyContactPhone(id, '31459265')
  })

  test('email can be edited', async () => {
    const id = fixtures.familyWithTwoGuardians.guardian.id
    await section.assertFamilyContactExists(id)
    await section.assertFamilyContactEmail(id, 'mikael.hogfors@evaka.test')
    await section.setFamilyContactEmail(id, 'foo@example.com')
    await section.assertFamilyContactEmail(id, 'foo@example.com')
  })

  test('email is editable after unset', async () => {
    const id = fixtures.familyWithTwoGuardians.guardian.id
    await section.assertFamilyContactExists(id)
    await section.setFamilyContactEmail(id, '')
    await section.assertFamilyContactEmail(id, '')
    await section.setFamilyContactEmail(id, 'foo@example.com')
    await section.assertFamilyContactEmail(id, 'foo@example.com')
  })
})

describe('Child information - guardian information', () => {
  let section: GuardiansSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('guardians')
  })

  test('guardian information is shown', async () => {
    await section.assertGuardianExists(
      fixtures.familyWithTwoGuardians.guardian.ssn
    )
  })
})

describe('Child information - consent', () => {
  let section: ConsentsSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('consents')
  })

  test('profile photo consent can be consented to', async () => {
    await section.evakaProfilePicYes.check()
    await section.save()
    await waitUntilEqual(
      () => section.evakaProfilePicModifiedBy.innerText,
      `Merkintä: ${mockedDate.format()} ${admin.firstName} ${admin.lastName}`
    )
    await page.reload()
    section = await childInformationPage.openCollapsible('consents')
    await section.evakaProfilePicYes.waitUntilChecked(true)
    await section.evakaProfilePicNo.waitUntilChecked(false)
    await waitUntilEqual(
      () => section.evakaProfilePicModifiedBy.innerText,
      `Merkintä: ${mockedDate.format()} ${admin.firstName} ${admin.lastName}`
    )
  })

  test('profile photo consent can be not consented to', async () => {
    await section.evakaProfilePicNo.check()
    await section.save()
    await waitUntilEqual(
      () => section.evakaProfilePicModifiedBy.innerText,
      `Merkintä: ${mockedDate.format()} ${admin.firstName} ${admin.lastName}`
    )
    await page.reload()
    section = await childInformationPage.openCollapsible('consents')
    await section.evakaProfilePicYes.waitUntilChecked(false)
    await section.evakaProfilePicNo.waitUntilChecked(true)
    await waitUntilEqual(
      () => section.evakaProfilePicModifiedBy.innerText,
      `Merkintä: ${mockedDate.format()} ${admin.firstName} ${admin.lastName}`
    )
  })

  test('profile photo consent can be cleared', async () => {
    await section.evakaProfilePicNo.check()
    await section.save()
    await waitUntilEqual(
      () => section.evakaProfilePicModifiedBy.innerText,
      `Merkintä: ${mockedDate.format()} ${admin.firstName} ${admin.lastName}`
    )
    await section.evakaProfilePicClear.click()
    await section.save()
    await section.evakaProfilePicYes.waitUntilChecked(false)
    await section.evakaProfilePicNo.waitUntilChecked(false)
  })
})
