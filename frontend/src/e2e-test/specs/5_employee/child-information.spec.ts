// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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

beforeEach(async () => {
  await resetDatabase()

  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])
  const admin = await Fixture.employeeAdmin().save()

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  await Fixture.placement()
    .with({
      childId,
      unitId
    })
    .save()

  page = await Page.open()
  await employeeLogin(page, admin.data)
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

  const today = LocalDate.todayInHelsinkiTz().format()
  const in10Days = LocalDate.todayInHelsinkiTz().addDays(10).format()
  const in40Days = LocalDate.todayInHelsinkiTz().addDays(40).format()

  test('can create regular daily service times', async () => {
    const form = await section.create()
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(in10Days)
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
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(in40Days)
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
    await form.validityPeriodStartInput.clear()
    await form.validityPeriodStartInput.fill(in40Days)
    await form.checkType('VARIABLE_TIME')
    await form.submit()

    await section.assertTableRow(
      0,
      `Päivittäinen varhaiskasvatusaika ${in40Days} –`,
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
      `Päivittäinen varhaiskasvatusaika ${today} –`,
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
      `Päivittäinen varhaiskasvatusaika ${in10Days} –`,
      'UPCOMING'
    )
    await section.assertTableRow(
      1,
      `Päivittäinen varhaiskasvatusaika ${today} – ${LocalDate.todayInHelsinkiTz()
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
      `Päivittäinen varhaiskasvatusaika ${in40Days} –`,
      'UPCOMING'
    )
    await section.assertTableRow(
      1,
      `Päivittäinen varhaiskasvatusaika ${today} – ${LocalDate.todayInHelsinkiTz()
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
})

describe('Chind information - backup care', () => {
  let section: BackupCaresSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('backupCares')
  })

  test('backup care for a child can be added and removed', async () => {
    await section.createBackupCare(
      fixtures.daycareFixture,
      '01.02.2020',
      '03.02.2020'
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
