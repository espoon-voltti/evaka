// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  testDaycare,
  testChild,
  testChild2,
  Fixture,
  uuidv4,
  testAdult2,
  testAdult,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createMessageAccounts,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import { DevDaycareGroup, DevPerson } from '../../generated/api-types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import ChildInformationPage from '../../pages/employee/child-information'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import MessageEditor from '../../pages/mobile/message-editor'
import MobileMessageEditor from '../../pages/mobile/message-editor'
import MessageEditorPage from '../../pages/mobile/message-editor-page'
import MobileMessagesPage from '../../pages/mobile/messages'
import MobileNav from '../../pages/mobile/mobile-nav'
import PinLoginPage from '../../pages/mobile/pin-login-page'
import ThreadViewPage from '../../pages/mobile/thread-view'
import UnreadMobileMessagesPage from '../../pages/mobile/unread-message-counts'
import { waitUntilEqual } from '../../utils'
import { pairMobileDevice, pairPersonalMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let citizenPage: Page
let messageEditor: MessageEditor
let messageEditorPage: MessageEditorPage
let messagesPage: MobileMessagesPage
let threadView: ThreadViewPage
let pinLoginPage: PinLoginPage
let unreadMessageCountsPage: UnreadMobileMessagesPage
let nav: MobileNav

const daycareGroupId = uuidv4()
const daycareGroup2Id = uuidv4()
const daycareGroup3Id = uuidv4()

let daycareGroup: DevDaycareGroup
let daycareGroup2: DevDaycareGroup
let daycareGroup3: DevDaycareGroup
let child: DevPerson
let child2: DevPerson

const employeeId = uuidv4()
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
  await resetServiceState()
  await initializeAreaAndPersonData()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare().with(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  child = testChild
  child2 = testChild2

  daycareGroup = await Fixture.daycareGroup()
    .with({ id: daycareGroupId, daycareId: testDaycare.id })
    .save()

  daycareGroup2 = await Fixture.daycareGroup()
    .with({ id: daycareGroup2Id, daycareId: testDaycare.id })
    .save()

  daycareGroup3 = await Fixture.daycareGroup()
    .with({ id: daycareGroup3Id, daycareId: testDaycare.id })
    .save()

  const employee = await Fixture.employee()
    .with({
      id: employeeId,
      firstName: empFirstName,
      lastName: empLastName,
      email: 'yy@example.com',
      roles: []
    })
    .withDaycareAcl(testDaycare.id, 'UNIT_SUPERVISOR')
    .save()

  const staff = await Fixture.employee()
    .with({
      firstName: staffFirstName,
      lastName: staffLastName,
      email: 'zz@example.com',
      roles: []
    })
    .withDaycareAcl(testDaycare.id, 'STAFF')
    .withGroupAcl(daycareGroup.id)
    .withGroupAcl(daycareGroup2.id)
    .withGroupAcl(daycareGroup3.id)
    .save()

  const staff2 = await Fixture.employee()
    .with({
      firstName: staff2FirstName,
      lastName: staff2LastName,
      email: 'aa@example.com',
      roles: []
    })
    .withDaycareAcl(testDaycare.id, 'STAFF')
    .save()

  await Fixture.employeePin().with({ userId: employee.id, pin }).save()
  await Fixture.employeePin().with({ userId: staff.id, pin }).save()
  await Fixture.employeePin().with({ userId: staff2.id, pin }).save()

  const placementFixture = await Fixture.placement()
    .with({
      childId: child.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: mockedDate
    })
    .save()
  await Fixture.groupPlacement()
    .with({ daycareGroupId: daycareGroup.id })
    .withPlacement(placementFixture)
    .save()

  const placement2Fixture = await Fixture.placement()
    .with({
      childId: child2.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: mockedDate
    })
    .save()
  await Fixture.groupPlacement()
    .with({ daycareGroupId: daycareGroup2.id })
    .withPlacement(placement2Fixture)
    .save()

  await createMessageAccounts()
  await insertGuardians({
    body: [
      {
        childId: child.id,
        guardianId: testAdult.id
      },
      {
        childId: child2.id,
        guardianId: testAdult.id
      }
    ]
  })

  page = await Page.open({ mockedTime: mockedDateAt11 })
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  unreadMessageCountsPage = new UnreadMobileMessagesPage(page)
  pinLoginPage = new PinLoginPage(page)
  messageEditor = new MessageEditor(page)
  messageEditorPage = new MessageEditorPage(page)
  messagesPage = new MobileMessagesPage(page)
  threadView = new ThreadViewPage(page)
  nav = new MobileNav(page)
})

async function initCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime })
  await enduserLogin(citizenPage, testAdult)
}

describe('Message editor in child page', () => {
  beforeEach(async () => {
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)
  })

  test('Employee can open editor and send message', async () => {
    await listPage.selectChild(child.id)
    await childPage.messageEditorLink.click()
    await pinLoginPage.login(employeeName, pin)
    await messageEditor.fillMessage({ title: 'Foo', content: 'Bar' })
    await messageEditor.send.click()
    await childPage.waitUntilLoaded()
  })
  test('Employee can open editor and send an urgent message', async () => {
    const message = { title: 'Foo', content: 'Bar', urgent: true }
    await listPage.selectChild(child.id)
    await childPage.messageEditorLink.click()
    await pinLoginPage.login(employeeName, pin)
    await messageEditor.fillMessage(message)
    await messageEditor.send.click()
    await childPage.waitUntilLoaded()
    await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

    await initCitizenPage(mockedDateAt12)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertThreadContent(message)
  })
})

describe('Messages page', () => {
  beforeEach(async () => {
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)
  })

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
    await nav.selectGroup(daycareGroupId)
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

    await nav.selectGroup(daycareGroupId)

    await messagesPage.thread(0).click()
    await waitUntilEqual(() => threadView.singleMessageContents.count(), 1)
    const replyContent = 'Testivastauksen sisältö'
    await threadView.replyButton.click()
    await threadView.replyContent.fill(replyContent)

    await threadView.goBack.click()

    await messagesPage.thread(0).draftIndicator.waitUntilVisible()
  })

  test('Employee replies as a group to message sent to group', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await nav.selectGroup(daycareGroupId)

    await messagesPage.thread(0).click()
    await waitUntilEqual(() => threadView.singleMessageContents.count(), 1)
    const replyContent = 'Testivastauksen sisältö'
    await threadView.replyButton.click()
    await threadView.replyContent.fill(replyContent)
    await threadView.sendReplyButton.click()
    await waitUntilEqual(() => threadView.singleMessageContents.count(), 2)
    await waitUntilEqual(() => threadView.getMessageContent(1), replyContent)
    await waitUntilEqual(
      () => threadView.getMessageSender(1),
      `${testDaycare.name} - ${daycareGroup.name}`
    )
  })

  test('Employee discards a reply', async () => {
    await initCitizenPage(mockedDateAt10)
    await citizenSendsMessageToGroup()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await nav.selectGroup(daycareGroupId)

    await messagesPage.thread(0).click()
    await waitUntilEqual(() => threadView.singleMessageContents.count(), 1)
    await threadView.replyButton.click()
    await threadView.replyContent.fill('foo bar baz')
    await threadView.discardReplyButton.click()
    await threadView.replyButton.click()
    await waitUntilEqual(() => threadView.replyContent.inputValue, '')
  })

  test('Employee sends a message to a group', async () => {
    const extraChildFixture = await Fixture.person().saveChild()
    const extraGuardianFixture1 = await Fixture.person()
      .with({ ssn: '240190-5442' })
      .saveAdult()
    const extraGuardianFixture2 = await Fixture.person()
      .with({ ssn: '210390-383J' })
      .saveAdult()
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
    await Fixture.child(extraChildFixture.id).save()
    const extraPlacementFixture = await Fixture.placement()
      .with({
        childId: extraChildFixture.id,
        unitId: testDaycare.id,
        startDate: mockedDate,
        endDate: mockedDate
      })
      .save()
    await Fixture.groupPlacement()
      .with({ daycareGroupId: daycareGroup.id })
      .withPlacement(extraPlacementFixture)
      .save()

    await staffStartsNewMessage()

    await messageEditor.senderName.assertTextEquals(
      `${testDaycare.name} - ${daycareGroup.name}`
    )

    // Required fields not filled -> send button is disabled
    await messageEditor.send.assertDisabled(true)

    // The user has access to all groups, but only the one whose messages are viewed should be available in the
    // message editor
    await messageEditor.recipients.open()
    await messageEditor.recipients.option(daycareGroup.id).click()
    await messageEditor.recipients.option(daycareGroup2.id).waitUntilHidden()
    await messageEditor.recipients.option(daycareGroup3.id).waitUntilHidden()

    const message = { title: 'Otsikko', content: 'Testiviestin sisältö' }
    await messageEditor.fillMessage(message)
    await messageEditor.send.click()
    await messageEditor.manyRecipientsWarning.waitUntilVisible()
    await messageEditor.manyRecipientsConfirm.click()
    await messageEditor.waitUntilHidden()
    await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

    // Check that citizen received the message
    await citizenSeesMessage(message)
  })

  test('Employee sees sent messages', async () => {
    await staffStartsNewMessage()
    await messageEditor.recipients.open()
    await messageEditor.recipients.option(daycareGroup.id).click()
    const message = { title: 'Otsikko', content: 'Testiviestin sisältö' }
    await messageEditor.fillMessage(message)
    await messageEditor.send.click()
    await messageEditor.waitUntilHidden()

    const sentTab = await messagesPage.openSentTab()
    const first = sentTab.message(0)
    await first.title.assertTextEquals(message.title)

    const firstMessage = await first.openMessage()
    await firstMessage.topBarTitle.assertTextEquals(message.title)
    await firstMessage.content.assertTextEquals(message.content)
  })

  test('Employee sees a draft message and can send it', async () => {
    let messageEditor = await staffStartsNewMessage()
    await messageEditor.recipients.open()
    await messageEditor.recipients.option(daycareGroup.id).click()
    const message = { title: 'Otsikko', content: 'Testiviestin sisältö' }
    await messageEditor.fillMessage(message)
    await messageEditor.close.click()

    const draftsTab = await messagesPage.openDraftsTab()
    const firstDraft = draftsTab.message(0)
    await firstDraft.title.assertTextEquals(message.title)

    messageEditor = await firstDraft.editDraft()

    await messageEditor.recipients.values.assertTextsEqual([daycareGroup.name])
    await messageEditor.title.assertValueEquals(message.title)
    await messageEditor.content.assertValueEquals(message.content)
    await messageEditor.send.click()

    await draftsTab.list.waitUntilVisible()
    await draftsTab.message(0).waitUntilHidden()
  })

  test('Employee can discard a draft message', async () => {
    let messageEditor = await staffStartsNewMessage()
    await messageEditor.recipients.open()
    await messageEditor.recipients.option(daycareGroup.id).click()
    const message = { title: 'Otsikko', content: 'Testiviestin sisältö' }
    await messageEditor.fillMessage(message)
    await messageEditor.close.click()

    const draftsTab = await messagesPage.openDraftsTab()
    const firstDraft = draftsTab.message(0)
    await firstDraft.title.assertTextEquals(message.title)

    messageEditor = await firstDraft.editDraft()
    await messageEditor.discard.click()

    await draftsTab.list.waitUntilVisible()
    await draftsTab.message(0).waitUntilHidden()
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
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))
    await userSeesNewMessagesIndicator()
    await staffLoginsToMessagesPage()

    await nav.selectGroup(daycareGroup.id)
    await messagesPage.assertThreadsExist()
    await messagesPage.thread(0).click()

    await waitUntilEqual(() => threadView.singleMessageContents.count(), 1)
    await waitUntilEqual(
      () => threadView.getMessageContent(0),
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

    await nav.selectGroup(daycareGroup2.id)
    await messagesPage.assertThreadsExist()
    await waitUntilEqual(() => messagesPage.getThreadTitle(0), 'Hei ryhmä 2')

    await nav.selectGroup(daycareGroup.id)
    await messagesPage.assertThreadsExist()
    await waitUntilEqual(() => messagesPage.getThreadTitle(0), 'Otsikko')
  })

  test('Staff without group access sees info that no accounts were found', async () => {
    await staff2LoginsToMessagesPage()
    await messagesPage.noAccountInfo.waitUntilVisible()
  })

  test('Employee sees info while trying to send message to child whose guardians are blocked', async () => {
    await Fixture.person()
      .with(testAdult2)
      .saveAdult({
        updateMockVtjWithDependants: [testChild]
      })

    // Add child's guardians to block list
    const admin = await Fixture.employeeAdmin().save()
    const adminPage = await Page.open({
      mockedTime: mockedDateAt10
    })
    await employeeLogin(adminPage, admin)
    await adminPage.goto(`${config.employeeUrl}/child-information/${child.id}`)
    const childInformationPage = new ChildInformationPage(adminPage)
    await childInformationPage.waitUntilLoaded()

    const blocklistSection =
      await childInformationPage.openCollapsible('messageBlocklist')
    await blocklistSection.addParentToBlockList(testAdult.id)
    await blocklistSection.addParentToBlockList(testAdult2.id)

    await listPage.selectChild(child.id)
    await childPage.messageEditorLink.click()
    await pinLoginPage.login(employeeName, pin)
    await messageEditorPage.noReceiversInfo.waitUntilVisible()
  })
})

describe('Personal mobile device', () => {
  beforeEach(async () => {
    const mobileSignupUrl = await pairPersonalMobileDevice(employeeId)
    await page.goto(mobileSignupUrl)
  })

  test('Supervisor can access messages', async () => {
    await nav.messages.click()
    await unreadMessageCountsPage.pinLoginButton.click()
    await pinLoginPage.personalDeviceLogin(pin)

    await messagesPage.receivedTab.waitUntilVisible()
    await messagesPage.sentTab.waitUntilVisible()
    await messagesPage.draftsTab.waitUntilVisible()
  })
})

async function citizenSendsMessageToGroup() {
  await citizenPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
  const title = 'Otsikko'
  const content = 'Testiviestin sisältö'
  const childIds = [child.id]
  const receivers = [daycareGroup.name + ' (Henkilökunta)']
  await citizenMessagesPage.sendNewMessage(title, content, childIds, receivers)
}

async function citizenSendsMessageToGroup2() {
  await citizenPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
  const title = 'Hei ryhmä 2'
  const content = 'Testiviestin sisältö'
  const childIds = [child2.id]
  const receivers = [daycareGroup2.name + ' (Henkilökunta)']
  await citizenMessagesPage.sendNewMessage(title, content, childIds, receivers)
}

async function citizenSeesMessage(message: {
  title: string
  content: string
  urgent?: boolean
}) {
  await initCitizenPage(mockedDateAt12)
  await citizenPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
  await citizenMessagesPage.assertThreadContent(message)
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

async function staffStartsNewMessage(): Promise<MobileMessageEditor> {
  await page.goto(config.mobileUrl)
  await nav.messages.click()
  await unreadMessageCountsPage.linkToGroup(daycareGroupId).click()
  await pinLoginPage.login(staffName, pin)
  await messagesPage.newMessage.click()
  return new MobileMessageEditor(page)
}
