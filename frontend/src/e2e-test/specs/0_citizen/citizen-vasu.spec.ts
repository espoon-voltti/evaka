// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  testDaycareGroup,
  testAdult,
  Fixture,
  uuidv4,
  familyWithTwoGuardians,
  testChild,
  testCareArea,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements,
  createVasuDocument,
  insertGuardians,
  publishVasuDocument,
  resetServiceState,
  revokeSharingPermission
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { VasuPreviewPage } from '../../pages/employee/vasu/vasu'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let child: DevPerson
let child2Id: UUID
let templateId: UUID
let vasuDocId: UUID
let header: CitizenHeader

const mockedNow = HelsinkiDateTime.of(2022, 7, 31, 13, 0)

beforeEach(async () => {
  await resetServiceState()

  await initializeAreaAndPersonData()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare().with(testDaycare).save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  await Fixture.family(familyWithTwoGuardians).save()
  await createDaycareGroups({ body: [testDaycareGroup] })

  const unitId = testDaycare.id
  child = familyWithTwoGuardians.children[0]
  child2Id = testChild.id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    child.id,
    unitId
  )

  await createDaycarePlacements({ body: [daycarePlacementFixture] })
  templateId = await Fixture.vasuTemplate().saveAndReturnId()
  vasuDocId = await createVasuDocument({
    body: { childId: child.id, templateId }
  })
  await publishVasuDocument({ documentId: vasuDocId })

  page = await Page.open({ mockedTime: mockedNow })
  header = new CitizenHeader(page, 'desktop')
  await enduserLogin(page, testAdult)
})

const insertVasu = async (childId: string): Promise<string> => {
  vasuDocId = await createVasuDocument({
    body: {
      childId,
      templateId: await Fixture.vasuTemplate().saveAndReturnId()
    }
  })
  await publishVasuDocument({ documentId: vasuDocId })
  return vasuDocId
}

describe('Citizen vasu document page', () => {
  const openDocument = async () => {
    await page.goto(`${config.enduserUrl}/vasu/${vasuDocId}`)
    return new VasuPreviewPage(page)
  }

  test('View child vasu document and give permission to share', async () => {
    await insertGuardians({
      body: [
        {
          guardianId: testAdult.id,
          childId: child.id
        }
      ]
    })

    const vasuPage = await openDocument()
    await header.assertUnreadChildrenCount(1)
    await vasuPage.assertTitleChildName('Antero Onni Leevi Aatu Högfors')
    await vasuPage.givePermissionToShare()

    await vasuPage.assertGivePermissionToShareSectionIsNotVisible()
    await header.assertUnreadChildrenCount(0)
    await revokeSharingPermission({ docId: vasuDocId })
    await page.reload()
    await vasuPage.assertGivePermissionToShareSectionIsVisible()
  })
})

describe('Citizen child documents listing page', () => {
  test('Published vasu document is in the list', async () => {
    await insertGuardians({
      body: [
        {
          guardianId: testAdult.id,
          childId: child.id
        }
      ]
    })
    await page.reload()
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(page)
    await childPage.openCollapsible('vasu')
    await childPage.assertVasuRow(
      vasuDocId,
      'Luonnos',
      LocalDate.todayInSystemTz().format('dd.MM.yyyy')
    )
  })

  test('Indicators for more than 1 child are shown', async () => {
    await insertGuardians({
      body: [
        {
          guardianId: testAdult.id,
          childId: child.id
        },
        {
          guardianId: testAdult.id,
          childId: child2Id
        }
      ]
    })
    // first child (`child`) has a vasu already created in beforeEach
    await insertVasu(child2Id)
    await page.reload()
    await header.assertChildUnreadCount(child.id, 1)
    await header.assertChildUnreadCount(child2Id, 1)
  })
})
