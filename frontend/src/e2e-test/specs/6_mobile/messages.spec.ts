// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { EmployeeId, GroupId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { mobileViewport } from '../../browser'
import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import {
  testDaycare,
  testChild,
  testChild2,
  Fixture,
  testAdult,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createMessageAccounts,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import type { DevDaycareGroup, DevPerson } from '../../generated/api-types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import MobileChildMessagesPage from '../../pages/mobile/child-messages-page'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import MessageEditor from '../../pages/mobile/message-editor'
import MobileMessageEditor from '../../pages/mobile/message-editor'
import MobileMessagesPage from '../../pages/mobile/messages'
import MobileNav from '../../pages/mobile/mobile-nav'
import PinLoginPage from '../../pages/mobile/pin-login-page'
import ThreadViewPage from '../../pages/mobile/thread-view'
import UnreadMobileMessagesPage from '../../pages/mobile/unread-message-counts'
import type { NewEvakaPage } from '../../playwright'
import { test, expect } from '../../playwright'
import { pairMobileDevice, pairPersonalMobileDevice } from '../../utils/mobile'
import type { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const daycareGroupId = randomId<GroupId>()
const daycareGroup2Id = randomId<GroupId>()
const daycareGroup3Id = randomId<GroupId>()

const employeeId = randomId<EmployeeId>()
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

const mockedDate = LocalDate.of(2022, 5, 20) // Friday
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

async function setupTestData() {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()

  const daycareGroup = await Fixture.daycareGroup({
    id: daycareGroupId,
    daycareId: testDaycare.id
  }).save()

  const daycareGroup2 = await Fixture.daycareGroup({
    id: daycareGroup2Id,
    daycareId: testDaycare.id
  }).save()

  const daycareGroup3 = await Fixture.daycareGroup({
    id: daycareGroup3Id,
    daycareId: testDaycare.id
  }).save()

  const employee = await Fixture.employee({
    id: employeeId,
    firstName: empFirstName,
    lastName: empLastName,
    email: 'yy@example.com',
    roles: []
  })
    .unitSupervisor(testDaycare.id)
    .save()

  const staff = await Fixture.employee({
    firstName: staffFirstName,
    lastName: staffLastName,
    email: 'zz@example.com',
    roles: []
  })
    .staff(testDaycare.id)
    .groupAcl(daycareGroup.id, mockedDateAt10)
    .groupAcl(daycareGroup2.id, mockedDateAt10)
    .groupAcl(daycareGroup3.id, mockedDateAt10)
    .save()

  const staff2 = await Fixture.employee({
    firstName: staff2FirstName,
    lastName: staff2LastName,
    email: 'aa@example.com',
    roles: []
  })
    .staff(testDaycare.id)
    .save()

  await Fixture.employeePin({ userId: employee.id, pin }).save()
  await Fixture.employeePin({ userId: staff.id, pin }).save()
  await Fixture.employeePin({ userId: staff2.id, pin }).save()

  const placementFixture = await Fixture.placement({
    childId: testChild.id,
    unitId: testDaycare.id,
    startDate: mockedDate,
    endDate: mockedDate
  }).save()
  await Fixture.groupPlacement({
    daycareGroupId: daycareGroup.id,
    daycarePlacementId: placementFixture.id,
    startDate: mockedDate,
    endDate: mockedDate
  }).save()

  const placement2Fixture = await Fixture.placement({
    childId: testChild2.id,
    unitId: testDaycare.id,
    startDate: mockedDate,
    endDate: mockedDate
  }).save()
  await Fixture.groupPlacement({
    daycareGroupId: daycareGroup2.id,
    daycarePlacementId: placement2Fixture.id,
    startDate: mockedDate,
    endDate: mockedDate
  }).save()

  await createMessageAccounts()
  await insertGuardians({
    body: [
      {
        childId: testChild.id,
        guardianId: testAdult.id
      },
      {
        childId: testChild2.id,
        guardianId: testAdult.id
      }
    ]
  })

  return { daycareGroup, daycareGroup2, daycareGroup3 }
}

test.describe('Messages in child page', () => {
  test.use({
    viewport: mobileViewport,
    evakaOptions: { mockedTime: mockedDateAt11 }
  })

  let page: Page
  let listPage: MobileListPage
  let childPage: MobileChildPage
  let childMessagesPage: MobileChildMessagesPage
  let citizenPage: Page
  let messageEditor: MessageEditor
  let threadView: ThreadViewPage
  let pinLoginPage: PinLoginPage

  let daycareGroup: DevDaycareGroup
  let child: DevPerson

  let newPage: NewEvakaPage

  test.beforeEach(async ({ evaka, newEvakaPage }) => {
    const data = await setupTestData()
    daycareGroup = data.daycareGroup
    child = testChild

    page = evaka
    listPage = new MobileListPage(page)
    childPage = new MobileChildPage(page)
    childMessagesPage = new MobileChildMessagesPage(page)
    pinLoginPage = new PinLoginPage(page)
    messageEditor = new MessageEditor(page)
    threadView = new ThreadViewPage(page)
    newPage = newEvakaPage

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)
  })

  async function initCitizenPage(mockedTime: HelsinkiDateTime) {
    citizenPage = await newPage({
      viewport: mobileViewport,
      mockedTime
    })
    await enduserLogin(citizenPage, testAdult)
  }

  test('Employee can open editor and send message', async () => {
    await listPage.selectChild(child.id)
    await childPage.messagesLink.click()
    await pinLoginPage.login(employeeName, pin)
    await childMessagesPage.newMessageButton.click()
    await messageEditor.fillMessage({ title: 'Foo', content: 'Bar' })
    await messageEditor.send.click()
    await childMessagesPage.backButton.click()
    await childPage.waitUntilLoaded()
  })

  test('Employee can open editor and send an urgent message', async () => {
    const message = { title: 'Foo', content: 'Bar', urgent: true }
    await listPage.selectChild(child.id)
    await childPage.messagesLink.click()
    await pinLoginPage.login(employeeName, pin)
    await childMessagesPage.newMessageButton.click()
    await messageEditor.fillMessage(message)
    await messageEditor.send.click()
    await childMessagesPage.backButton.click()
    await childPage.waitUntilLoaded()
    await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

    await initCitizenPage(mockedDateAt12)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'mobile')
    await citizenMessagesPage.assertThreadContent(message)
  })

  test('Employee sees a received message', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await citizenPage.close()

    await listPage.selectChild(child.id)
    await childPage.messagesLink.click()
    await pinLoginPage.login(employeeName, pin)
    await childMessagesPage
      .thread(0)
      .assertText((s) => s.includes('Testiviestin sisältö'))
    await childMessagesPage.thread(0).click()
    await threadView.singleMessageContents
      .nth(0)
      .assertText((s) => s.includes('Testiviestin sisältö'))
  })

  async function citizenSendsMessageToGroup() {
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'mobile')
    const title = 'Otsikko'
    const content = 'Testiviestin sisältö'
    const childIds = [child.id]
    const recipients = [daycareGroup.name + ' (Henkilökunta)']
    await citizenMessagesPage.sendNewMessage(
      title,
      content,
      childIds,
      recipients,
      false
    )
  }
})

test.describe('Messages page', () => {
  test.use({
    viewport: mobileViewport,
    evakaOptions: { mockedTime: mockedDateAt11 }
  })

  let page: Page
  let listPage: MobileListPage
  let citizenPage: Page
  let messageEditor: MessageEditor
  let messagesPage: MobileMessagesPage
  let threadView: ThreadViewPage
  let pinLoginPage: PinLoginPage
  let unreadMessageCountsPage: UnreadMobileMessagesPage
  let nav: MobileNav

  let daycareGroup: DevDaycareGroup
  let daycareGroup2: DevDaycareGroup
  let daycareGroup3: DevDaycareGroup
  let child: DevPerson
  let child2: DevPerson

  let newPage: NewEvakaPage

  test.beforeEach(async ({ evaka, newEvakaPage }) => {
    const data = await setupTestData()
    daycareGroup = data.daycareGroup
    daycareGroup2 = data.daycareGroup2
    daycareGroup3 = data.daycareGroup3
    child = testChild
    child2 = testChild2

    page = evaka
    listPage = new MobileListPage(page)
    unreadMessageCountsPage = new UnreadMobileMessagesPage(page)
    pinLoginPage = new PinLoginPage(page)
    messageEditor = new MessageEditor(page)
    messagesPage = new MobileMessagesPage(page)
    threadView = new ThreadViewPage(page)
    nav = new MobileNav(page)
    newPage = newEvakaPage

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)
  })

  async function initCitizenPage(mockedTime: HelsinkiDateTime) {
    citizenPage = await newPage({
      viewport: mobileViewport,
      mockedTime
    })
    await enduserLogin(citizenPage, testAdult)
  }

  async function citizenSendsMessageToGroup() {
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'mobile')
    const title = 'Otsikko'
    const content = 'Testiviestin sisältö'
    const childIds = [child.id]
    const recipients = [daycareGroup.name + ' (Henkilökunta)']
    await citizenMessagesPage.sendNewMessage(
      title,
      content,
      childIds,
      recipients,
      false
    )
  }

  async function citizenSendsMessageToGroup2() {
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'mobile')
    const title = 'Hei ryhmä 2'
    const content = 'Testiviestin sisältö'
    const childIds = [child2.id]
    const recipients = [daycareGroup2.name + ' (Henkilökunta)']
    await citizenMessagesPage.sendNewMessage(
      title,
      content,
      childIds,
      recipients,
      false
    )
  }

  async function citizenSeesMessage(message: {
    title: string
    content: string
    urgent?: boolean
  }) {
    await initCitizenPage(mockedDateAt12)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'mobile')
    await citizenMessagesPage.assertThreadContent(message)
  }

  async function userSeesNewMessagesIndicator() {
    await page.goto(config.mobileUrl)
    await expect(listPage.unreadMessagesIndicator).toBeVisible()
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
    await unreadMessageCountsPage.pinLoginButton.click()
    await pinLoginPage.login(employeeName, pin)
    const linkToGroup = unreadMessageCountsPage.linkToGroup(daycareGroupId)
    await expect(linkToGroup).toBeVisible()
    await linkToGroup.click()
  }

  async function employeeLoginsToMessagesPage() {
    await nav.messages.click()
    await employeeNavigatesToMessagesSelectingLogin()
  }

  async function employeeLoginsToMessagesPageThroughGroup() {
    await nav.messages.click()
    await employeeNavigatesToMessagesSelectingGroup()
  }

  async function staffStartsNewMessage(): Promise<MobileMessageEditor> {
    await page.goto(config.mobileUrl)
    await nav.messages.click()
    await unreadMessageCountsPage.pinLoginButton.click()
    await pinLoginPage.login(employeeName, pin)
    await unreadMessageCountsPage.linkToGroup(daycareGroupId).click()
    await messagesPage.newMessage.click()
    return new MobileMessageEditor(page)
  }

  test('Employee sees unread counts and pin login button', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessageIndicatorAndClicks()

    await unreadMessageCountsPage.groupLinksExist()
    await unreadMessageCountsPage.pinButtonExists()
  })

  test('Employee navigates using login button and sees messages', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()
    await unreadMessageCountsPage.linkToGroup(daycareGroupId).click()
    await messagesPage.assertThreadsExist()
  })

  test('Employee navigates using group link and sees messages', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPageThroughGroup()
    await messagesPage.assertThreadsExist()
  })

  test('Draft replies are indicated in the thread list', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await unreadMessageCountsPage.linkToGroup(daycareGroupId).click()

    await messagesPage.thread(0).click()
    await expect(threadView.singleMessageContents).toHaveCount(1)
    const replyContent = 'Testivastauksen sisältö'
    await threadView.replyButton.click()
    await threadView.replyContent.fill(replyContent)

    await threadView.goBack.click()

    await expect(messagesPage.thread(0).draftIndicator).toBeVisible()
  })

  test('Employee replies as a group to message sent to group', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await unreadMessageCountsPage.linkToGroup(daycareGroupId).click()

    await messagesPage.thread(0).click()
    await expect(threadView.singleMessageContents).toHaveCount(1)
    const replyContent = 'Testivastauksen sisältö'
    await threadView.replyButton.click()
    await threadView.replyContent.fill(replyContent)
    await threadView.sendReplyButton.click()
    await expect(threadView.singleMessageContents).toHaveCount(2)
    await expect(threadView.messageContent(1)).toHaveText(replyContent)
    await expect(threadView.messageSender(1)).toHaveText(
      `${testDaycare.name} - ${daycareGroup.name}`
    )
  })

  test('Employee discards a reply', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await unreadMessageCountsPage.linkToGroup(daycareGroupId).click()

    await messagesPage.thread(0).click()
    await expect(threadView.singleMessageContents).toHaveCount(1)
    await threadView.replyButton.click()
    await threadView.replyContent.fill('foo bar baz')
    await threadView.discardReplyButton.click()
    await threadView.replyButton.click()
    await expect(threadView.replyContent).toHaveValue('')
  })

  test('Employee sends a message to a group', async () => {
    const extraChildFixture = await Fixture.person().saveChild()
    const extraGuardianFixture1 = await Fixture.person({
      ssn: '240190-5442'
    }).saveAdult()
    const extraGuardianFixture2 = await Fixture.person({
      ssn: '210390-383J'
    }).saveAdult()
    await insertGuardians({
      body: [
        {
          childId: extraChildFixture.id,
          guardianId: extraGuardianFixture1.id
        },
        {
          childId: extraChildFixture.id,
          guardianId: extraGuardianFixture2.id
        }
      ]
    })
    const extraPlacementFixture = await Fixture.placement({
      childId: extraChildFixture.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: mockedDate
    }).save()
    await Fixture.groupPlacement({
      daycareGroupId: daycareGroup.id,
      daycarePlacementId: extraPlacementFixture.id,
      startDate: mockedDate,
      endDate: mockedDate
    }).save()

    await staffStartsNewMessage()

    await expect(messageEditor.senderName).toHaveText(
      `${testDaycare.name} - ${daycareGroup.name}`
    )

    // Required fields not filled -> send button is disabled
    await messageEditor.send.assertDisabled(true)

    // The user has access to all groups, but only the one whose messages are viewed should be available in the
    // message editor
    await messageEditor.recipients.open()
    await messageEditor.recipients.option(`${daycareGroup.id}+false`).click()
    await expect(
      messageEditor.recipients.option(`${daycareGroup2.id}+false`)
    ).toBeHidden()
    await expect(
      messageEditor.recipients.option(`${daycareGroup3.id}+false`)
    ).toBeHidden()

    const message = { title: 'Otsikko', content: 'Testiviestin sisältö' }
    await messageEditor.fillMessage(message)
    await messageEditor.send.click()
    await expect(messageEditor.manyRecipientsWarning).toBeVisible()
    await messageEditor.manyRecipientsConfirm.click()
    await expect(messageEditor).toBeHidden()
    await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

    // Check that citizen received the message
    await citizenSeesMessage(message)
  })

  test('Employee sees sent messages', async () => {
    await staffStartsNewMessage()
    await messageEditor.recipients.open()
    await messageEditor.recipients.option(`${daycareGroup.id}+false`).click()
    const message = { title: 'Otsikko', content: 'Testiviestin sisältö' }
    await messageEditor.fillMessage(message)
    await messageEditor.send.click()
    await expect(messageEditor).toBeHidden()

    const sentTab = await messagesPage.openSentTab()
    const first = sentTab.message(0)
    await expect(first.title).toHaveText(message.title)

    const firstMessage = await first.openMessage()
    await expect(firstMessage.topBarTitle).toHaveText(message.title)
    await expect(firstMessage.content).toHaveText(message.content)
  })

  test('Employee sees a draft message and can send it', async () => {
    let messageEditor = await staffStartsNewMessage()
    await messageEditor.recipients.open()
    await messageEditor.recipients.option(`${daycareGroup.id}+false`).click()
    const message = { title: 'Otsikko', content: 'Testiviestin sisältö' }
    await messageEditor.fillMessage(message)
    await messageEditor.close.click()

    const draftsTab = await messagesPage.openDraftsTab()
    const firstDraft = draftsTab.message(0)
    await expect(firstDraft.title).toHaveText(message.title)

    messageEditor = await firstDraft.editDraft()

    await expect(messageEditor.recipients.values).toHaveText([
      daycareGroup.name
    ])
    await expect(messageEditor.title).toHaveValue(message.title)
    await expect(messageEditor.content).toHaveValue(message.content)
    await messageEditor.send.click()

    await expect(draftsTab.list).toBeVisible()
    await expect(draftsTab.message(0)).toBeHidden()
  })

  test('Employee can discard a draft message', async () => {
    let messageEditor = await staffStartsNewMessage()
    await messageEditor.recipients.open()
    await messageEditor.recipients.option(`${daycareGroup.id}+false`).click()
    const message = { title: 'Otsikko', content: 'Testiviestin sisältö' }
    await messageEditor.fillMessage(message)
    await messageEditor.close.click()

    const draftsTab = await messagesPage.openDraftsTab()
    const firstDraft = draftsTab.message(0)
    await expect(firstDraft.title).toHaveText(message.title)

    messageEditor = await firstDraft.editDraft()
    await messageEditor.discard.click()

    await expect(draftsTab.list).toBeVisible()
    await expect(draftsTab.message(0)).toBeHidden()
  })

  test('Message button goes to unread messages if user has no pin session', async () => {
    await nav.messages.click()
    await unreadMessageCountsPage.groupLinksExist()
    await unreadMessageCountsPage.pinButtonExists()
  })

  test('Message button goes to unread messages if user has pin session', async () => {
    await employeeLoginsToMessagesPage()
    await nav.children.click()
    await nav.messages.click()
    await unreadMessageCountsPage.groupLinksExist()
    await unreadMessageCountsPage.pinButtonDoesNotExist()
  })

  test("Staff sees citizen's message for group", async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await staffLoginsToMessagesPage()

    await unreadMessageCountsPage.linkToGroup(daycareGroup.id).click()
    await messagesPage.assertThreadsExist()
    await messagesPage.thread(0).click()

    await expect(threadView.singleMessageContents).toHaveCount(1)
    await expect(threadView.messageContent(0)).toHaveText(
      'Testiviestin sisältö'
    )
  })

  test('Supervisor navigates through group message boxes', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await citizenSendsMessageToGroup2()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await unreadMessageCountsPage.linkToGroup(daycareGroup2.id).click()
    await messagesPage.assertThreadsExist()
    await expect(messagesPage.threadTitle(0)).toHaveText('Hei ryhmä 2')

    await nav.selectGroup(daycareGroup.id)
    await messagesPage.assertThreadsExist()
    await expect(messagesPage.threadTitle(0)).toHaveText('Otsikko')
  })

  test('Staff without group access sees info that no accounts were found', async () => {
    await staff2LoginsToMessagesPage()
    await expect(messagesPage.noAccountInfo).toBeVisible()
  })
})

test.describe('Personal mobile device', () => {
  test.use({
    viewport: mobileViewport,
    evakaOptions: { mockedTime: mockedDateAt11 }
  })

  let page: Page
  let unreadMessageCountsPage: UnreadMobileMessagesPage
  let pinLoginPage: PinLoginPage
  let messagesPage: MobileMessagesPage
  let nav: MobileNav

  let daycareGroup: DevDaycareGroup

  test.beforeEach(async ({ evaka }) => {
    const data = await setupTestData()
    daycareGroup = data.daycareGroup

    page = evaka
    unreadMessageCountsPage = new UnreadMobileMessagesPage(page)
    pinLoginPage = new PinLoginPage(page)
    messagesPage = new MobileMessagesPage(page)
    nav = new MobileNav(page)

    const mobileSignupUrl = await pairPersonalMobileDevice(employeeId)
    await page.goto(mobileSignupUrl)
  })

  test('Supervisor can access messages', async () => {
    await nav.messages.click()
    await unreadMessageCountsPage.pinLoginButton.click()
    await pinLoginPage.personalDeviceLogin(pin)
    await unreadMessageCountsPage.linkToGroup(daycareGroup.id).click()

    await expect(messagesPage.receivedTab).toBeVisible()
    await expect(messagesPage.sentTab).toBeVisible()
    await expect(messagesPage.draftsTab).toBeVisible()
  })
})
