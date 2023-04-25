// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  insertGuardianFixtures,
  resetDatabase,
  upsertMessageAccounts
} from '../../dev-api'
import type { AreaAndPersonFixtures } from '../../dev-api/data-init'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import type { DaycareGroupBuilder } from '../../dev-api/fixtures'
import { Fixture, uuidv4 } from '../../dev-api/fixtures'
import type { Daycare, PersonDetail } from '../../dev-api/types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import ChildInformationPage from '../../pages/employee/child-information'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import MobileMessageEditorPage from '../../pages/mobile/message-editor'
import MobileMessagesPage from '../../pages/mobile/messages'
import MobileNav from '../../pages/mobile/mobile-nav'
import PinLoginPage from '../../pages/mobile/pin-login-page'
import ThreadViewPage from '../../pages/mobile/thread-view'
import UnreadMobileMessagesPage from '../../pages/mobile/unread-message-counts'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let page: Page
let fixtures: AreaAndPersonFixtures
let listPage: MobileListPage
let childPage: MobileChildPage
let citizenPage: Page
let messageEditorPage: MobileMessageEditorPage
let messagesPage: MobileMessagesPage
let threadView: ThreadViewPage
let pinLoginPage: PinLoginPage
let unreadMessageCountsPage: UnreadMobileMessagesPage
let nav: MobileNav

const daycareGroupId = uuidv4()
const daycareGroup2Id = uuidv4()
const daycareGroup3Id = uuidv4()

let daycareGroup: DaycareGroupBuilder
let daycareGroup2: DaycareGroupBuilder
let daycareGroup3: DaycareGroupBuilder
let child: PersonDetail
let child2: PersonDetail

let unit: Daycare

const empFirstName = 'Yrjö'
const empLastName = 'Yksikkö'
const employeeName = `${empLastName} ${empFirstName}`

const staffFirstName = 'Sari'
const staffLastName = 'Staff'
const staffName = `${staffLastName} ${staffFirstName}`

const staff2FirstName = 'Mari'
const staff2LastName = 'Staff'
const staff2Name = `${staff2LastName} ${staff2FirstName}`

const pin = '2580'

const mockedDate = LocalDate.of(2022, 5, 21)
const mockedDateAt10 = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(10, 2)
)
const mockedDateAt11 = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(11, 31)
)
const mockedDateAt12 = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(12, 17)
)

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  child = fixtures.enduserChildFixtureJari
  child2 = fixtures.enduserChildFixtureKaarina
  unit = fixtures.daycareFixture

  daycareGroup = await Fixture.daycareGroup()
    .with({ id: daycareGroupId, daycareId: unit.id })
    .save()

  daycareGroup2 = await Fixture.daycareGroup()
    .with({ id: daycareGroup2Id, daycareId: unit.id })
    .save()

  daycareGroup3 = await Fixture.daycareGroup()
    .with({ id: daycareGroup3Id, daycareId: unit.id })
    .save()

  const employee = await Fixture.employee()
    .with({
      firstName: empFirstName,
      lastName: empLastName,
      email: 'yy@example.com',
      roles: []
    })
    .withDaycareAcl(unit.id, 'UNIT_SUPERVISOR')
    .save()

  const staff = await Fixture.employee()
    .with({
      firstName: staffFirstName,
      lastName: staffLastName,
      email: 'zz@example.com',
      roles: []
    })
    .withDaycareAcl(unit.id, 'STAFF')
    .withGroupAcl(daycareGroup.data.id)
    .withGroupAcl(daycareGroup2.data.id)
    .withGroupAcl(daycareGroup3.data.id)
    .save()

  const staff2 = await Fixture.employee()
    .with({
      firstName: staff2FirstName,
      lastName: staff2LastName,
      email: 'aa@example.com',
      roles: []
    })
    .withDaycareAcl(unit.id, 'STAFF')
    .withGroupAcl(daycareGroup.data.id)
    .withGroupAcl(daycareGroup2.data.id)
    .withGroupAcl(daycareGroup3.data.id)
    .save()

  await Fixture.employeePin().with({ userId: employee.data.id, pin }).save()
  await Fixture.employeePin().with({ userId: staff.data.id, pin }).save()
  await Fixture.employeePin().with({ userId: staff2.data.id, pin }).save()

  const placementFixture = await Fixture.placement()
    .with({
      childId: child.id,
      unitId: unit.id,
      startDate: mockedDate.formatIso(),
      endDate: mockedDate.formatIso()
    })
    .save()
  await Fixture.groupPlacement()
    .withGroup(daycareGroup)
    .withPlacement(placementFixture)
    .save()

  const placement2Fixture = await Fixture.placement()
    .with({
      childId: child2.id,
      unitId: unit.id,
      startDate: mockedDate.formatIso(),
      endDate: mockedDate.formatIso()
    })
    .save()
  await Fixture.groupPlacement()
    .withGroup(daycareGroup2)
    .withPlacement(placement2Fixture)
    .save()

  await upsertMessageAccounts()
  await insertGuardianFixtures([
    {
      childId: child.id,
      guardianId: fixtures.enduserGuardianFixture.id
    },
    {
      childId: child2.id,
      guardianId: fixtures.enduserGuardianFixture.id
    }
  ])

  page = await Page.open({ mockedTime: mockedDateAt11.toSystemTzDate() })
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  unreadMessageCountsPage = new UnreadMobileMessagesPage(page)
  pinLoginPage = new PinLoginPage(page)
  messageEditorPage = new MobileMessageEditorPage(page)
  messagesPage = new MobileMessagesPage(page)
  threadView = new ThreadViewPage(page)
  nav = new MobileNav(page)

  const mobileSignupUrl = await pairMobileDevice(unit.id)
  await page.goto(mobileSignupUrl)
})

async function initCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime: mockedTime.toSystemTzDate() })
  await enduserLogin(citizenPage)
}

describe('Message editor in child page', () => {
  test('Employee can open editor and send message', async () => {
    await listPage.selectChild(child.id)
    await childPage.openMessageEditor()
    await pinLoginPage.login(employeeName, pin)
    await messageEditorPage.draftNewMessage({ title: 'Foo', content: 'Bar' })
    await messageEditorPage.sendEditedMessage()
    await childPage.waitUntilLoaded()
  })
  test('Employee can open editor and send an urgent message', async () => {
    const message = { title: 'Foo', content: 'Bar', urgent: true }
    await listPage.selectChild(child.id)
    await childPage.openMessageEditor()
    await pinLoginPage.login(employeeName, pin)
    await messageEditorPage.draftNewMessage(message)
    await messageEditorPage.sendEditedMessage()
    await childPage.waitUntilLoaded()

    await initCitizenPage(mockedDateAt12)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertThreadContent(message)
  })
})

describe('Child message thread', () => {
  test('Employee sees unread counts and pin login button', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await userSeesNewMessageIndicatorAndClicks()

    await unreadMessageCountsPage.groupLinksExist()
    await unreadMessageCountsPage.pinButtonExists()
  })

  test('Employee navigates using login button and sees messages', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()
    await nav.selectGroup(daycareGroupId)
    await waitUntilTrue(() => messagesPage.messagesExist())
  })

  test('Employee navigates using group link and sees messages', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPageThroughGroup()
    await waitUntilTrue(() => messagesPage.messagesExist())
  })

  test('Employee replies as a group to message sent to group', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await nav.selectGroup(daycareGroupId)

    await messagesPage.openFirstThread()
    await waitUntilEqual(() => threadView.countMessages(), 1)
    const replyContent = 'Testivastauksen sisältö'
    await threadView.replyThread(replyContent)
    await waitUntilEqual(() => threadView.countMessages(), 2)
    await waitUntilEqual(() => threadView.getMessageContent(1), replyContent)
    await waitUntilEqual(
      () => threadView.getMessageSender(1),
      `${unit.name} - ${daycareGroup.data.name}`
    )
  })

  test('Message button goes to unread messages if user has no pin session', async () => {
    await nav.messages.click()
    await unreadMessageCountsPage.groupLinksExist()
    await unreadMessageCountsPage.pinButtonExists()
  })

  test('Message button goes to messages if user has pin session', async () => {
    await employeeLoginsToMessagesPage()
    await nav.children.click()
    await nav.messages.click()
    await messagesPage.messagesContainer.waitUntilVisible()
  })

  test("Staff sees citizen's message for group", async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await userSeesNewMessagesIndicator()
    await staffLoginsToMessagesPage()

    await nav.selectGroup(daycareGroup.data.id)
    await waitUntilTrue(() => messagesPage.messagesExist())
    await messagesPage.openFirstThread()

    await waitUntilEqual(() => threadView.countMessages(), 1)
    await waitUntilEqual(
      () => threadView.getMessageContent(0),
      'Testiviestin sisältö'
    )
  })

  test('Supervisor navigates through group message boxes', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await citizenSendsMessageToGroup2()
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await nav.selectGroup(daycareGroup2.data.id)
    await waitUntilTrue(() => messagesPage.messagesExist())
    await waitUntilEqual(() => messagesPage.getThreadTitle(0), 'Hei ryhmä 2')

    await nav.selectGroup(daycareGroup.data.id)
    await waitUntilTrue(() => messagesPage.messagesExist())
    await waitUntilEqual(() => messagesPage.getThreadTitle(0), 'Otsikko')
  })

  test('Staff without group access sees info that no accounts were found', async () => {
    await staff2LoginsToMessagesPage()
    await messagesPage.noAccountInfo.waitUntilVisible()
  })

  test('Employee sees info while trying to send message to child whose guardians are blocked', async () => {
    // Add child's guardians to block list
    const admin = await Fixture.employeeAdmin().save()
    const adminPage = await Page.open({
      mockedTime: mockedDateAt10.toSystemTzDate()
    })
    await employeeLogin(adminPage, admin.data)
    await adminPage.goto(`${config.employeeUrl}/child-information/${child.id}`)
    const childInformationPage = new ChildInformationPage(adminPage)
    await childInformationPage.waitUntilLoaded()

    const blocklistSection = await childInformationPage.openCollapsible(
      'messageBlocklist'
    )
    await blocklistSection.addParentToBlockList(
      fixtures.enduserGuardianFixture.id
    )
    await blocklistSection.addParentToBlockList(
      fixtures.enduserChildJariOtherGuardianFixture.id
    )

    await listPage.selectChild(child.id)
    await childPage.openMessageEditor()
    await pinLoginPage.login(employeeName, pin)
    await messageEditorPage.noReceiversInfo.waitUntilVisible()
  })
})

async function citizenSendsMessageToGroup() {
  await citizenPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
  const title = 'Otsikko'
  const content = 'Testiviestin sisältö'
  const childIds = [child.id]
  const receivers = [daycareGroup.data.name + ' (Henkilökunta)']
  await citizenMessagesPage.sendNewMessage(title, content, childIds, receivers)
}

async function citizenSendsMessageToGroup2() {
  await citizenPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
  const title = 'Hei ryhmä 2'
  const content = 'Testiviestin sisältö'
  const childIds = [child2.id]
  const receivers = [daycareGroup2.data.name + ' (Henkilökunta)']
  await citizenMessagesPage.sendNewMessage(title, content, childIds, receivers)
}

async function userSeesNewMessagesIndicator() {
  await page.goto(config.mobileUrl)
  await listPage.unreadMessagesIndicator.waitUntilVisible()
}

async function userSeesNewMessageIndicatorAndClicks() {
  await userSeesNewMessagesIndicator()
  await nav.messages.click()
}

async function staffLoginsToMessagesPage() {
  await nav.messages.click()
  await unreadMessageCountsPage.pinLoginButton.click()
  await pinLoginPage.login(staffName, pin)
}

async function staff2LoginsToMessagesPage() {
  await nav.messages.click()
  await unreadMessageCountsPage.pinLoginButton.click()
  await pinLoginPage.login(staff2Name, pin)
}

async function employeeNavigatesToMessagesSelectingLogin() {
  await unreadMessageCountsPage.pinLoginButton.click()
  await pinLoginPage.login(employeeName, pin)
}

async function employeeNavigatesToMessagesSelectingGroup() {
  const linkToGroup = unreadMessageCountsPage.linkToGroup(daycareGroupId)
  await linkToGroup.waitUntilVisible()
  await linkToGroup.click()
  await pinLoginPage.login(employeeName, pin)
}

async function employeeLoginsToMessagesPage() {
  await nav.messages.click()
  await employeeNavigatesToMessagesSelectingLogin()
}

async function employeeLoginsToMessagesPageThroughGroup() {
  await nav.messages.click()
  await employeeNavigatesToMessagesSelectingGroup()
}
