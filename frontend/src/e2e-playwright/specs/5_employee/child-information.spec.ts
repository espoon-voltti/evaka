// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  daycareGroupFixture,
  enduserDeceasedChildFixture,
  enduserNonSsnChildFixture,
  Fixture
} from 'e2e-test-common/dev-api/fixtures'
import ChildInformationPage, {
  AdditionalInformationSection,
  BackupCaresSection,
  DailyServiceTimeSection,
  FamilyContactsSection,
  GuardiansSection
} from 'e2e-playwright/pages/employee/child-information'
import {
  waitUntilEqual,
  waitUntilFalse,
  waitUntilTrue
} from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { UUID } from 'lib-common/types'
import { Page } from '../../utils/page'

let page: Page
let childInformationPage: ChildInformationPage
let fixtures: AreaAndPersonFixtures
let childId: UUID

beforeEach(async () => {
  await resetDatabase()

  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  await Fixture.placement()
    .with({
      childId,
      unitId
    })
    .save()

  page = await Page.open()
  await employeeLogin(page, 'ADMIN')
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.waitUntilLoaded()
})

describe('Child Information - edit child information', () => {
  test('Oph person oid can be edited', async () => {
    await page.goto(
      config.employeeUrl + '/child-information/' + enduserNonSsnChildFixture.id
    )
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

  test('no service times initially', async () => {
    await waitUntilEqual(() => section.typeText, 'Ei asetettu')
    await waitUntilFalse(() => section.hasTimesText)
  })

  test('cannot save regular without setting times', async () => {
    const editor = await section.edit()
    await editor.selectRegularTime()
    await waitUntilTrue(() => editor.submitIsDisabled)
  })

  test('set regular daily service times', async () => {
    let editor = await section.edit()
    await editor.selectRegularTime()
    await editor.fillTimeRange('regular', '09:00', '17:00')
    await editor.submit()

    await waitUntilEqual(
      () => section.typeText,
      'Säännöllinen varhaiskasvatusaika'
    )
    await waitUntilEqual(
      () => section.timesText,
      'maanantai-perjantai 09:00–17:00'
    )

    // Check that initial values are correct when editing
    editor = await section.edit()
    await waitUntilTrue(() => editor.regularTimeIsSelected())
    await waitUntilTrue(() => editor.hasTimeRange('regular', '09:00', '17:00'))
  })

  test('cannot save irregular without setting times', async () => {
    const editor = await section.edit()
    await editor.selectIrregularTime()
    await waitUntilTrue(() => editor.submitIsDisabled)
  })

  test('set irregular daily service times', async () => {
    let editor = await section.edit()
    await editor.selectIrregularTime()
    await editor.selectDay('monday')
    await editor.fillTimeRange('monday', '08:15', '16:45')
    await editor.selectDay('friday')
    await editor.fillTimeRange('friday', '09:00', '13:30')
    await editor.selectDay('sunday')
    await editor.fillTimeRange('sunday', '13:50', '09:20')
    await editor.submit()

    await waitUntilEqual(
      () => section.typeText,
      'Epäsäännöllinen varhaiskasvatusaika'
    )
    await waitUntilEqual(
      () => section.timesText,
      'maanantai 08:15-16:45, perjantai 09:00-13:30, sunnuntai 13:50-09:20'
    )

    // Check that initial values are correct when editing
    editor = await section.edit()
    await waitUntilTrue(() => editor.dayIsSelected('monday'))
    await waitUntilTrue(() => editor.hasTimeRange('monday', '08:15', '16:45'))
    await waitUntilFalse(() => editor.dayIsSelected('tuesday'))
    await waitUntilFalse(() => editor.dayIsSelected('wednesday'))
    await waitUntilFalse(() => editor.dayIsSelected('thursday'))
    await waitUntilTrue(() => editor.dayIsSelected('friday'))
    await waitUntilTrue(() => editor.hasTimeRange('friday', '09:00', '13:30'))
    await waitUntilFalse(() => editor.dayIsSelected('saturday'))
    await waitUntilTrue(() => editor.dayIsSelected('sunday'))
    await waitUntilTrue(() => editor.hasTimeRange('sunday', '13:50', '09:20'))
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
