// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  execSimpleApplicationActions,
  runPendingAsyncJobs
} from '../../dev-api'
import {
  applicationFixture,
  applicationFixtureId,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createApplications,
  createMessageAccounts,
  getApplicationDecisions,
  resetServiceState
} from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import EmployeeNav from '../../pages/employee/employee-nav'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let citizenPage: Page
let staffPage: Page
let serviceWorker: DevEmployee
let messagingAndServiceWorker: DevEmployee

const mockedToday = LocalDate.of(2022, 11, 8)
const mockedTime = HelsinkiDateTime.fromLocal(
  mockedToday,
  LocalTime.of(10, 49, 0)
)

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  serviceWorker = await Fixture.employee().serviceWorker().save()
  messagingAndServiceWorker = await Fixture.employee({
    roles: ['SERVICE_WORKER', 'MESSAGING']
  }).save()
  await createMessageAccounts()
})

async function openCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime })
  await enduserLogin(citizenPage, testAdult)
}

async function openStaffPage(
  mockedTime: HelsinkiDateTime,
  employee: DevEmployee
) {
  staffPage = await Page.open({ mockedTime })
  await employeeLogin(staffPage, employee)
  await staffPage.goto(config.employeeUrl)
  return staffPage
}

describe('Service Worker Messaging', () => {
  describe('Citizen only', () => {
    it('should NOT show the messaging section for a citizen', async () => {
      await openCitizenPage(mockedTime)
      const header = new CitizenHeader(citizenPage)
      await header.assertNoTab('messages')
    })
  })

  describe('Service Worker and citizen', () => {
    beforeEach(async () => {
      const applFixture = {
        ...applicationFixture(testChild, testAdult),
        sentDate: mockedToday
      }
      await createApplications({ body: [applFixture] })
    })

    it('should be possible for service workers to send messages relating to an application', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      const title = 'Message to citizen'
      const content = 'Hello citizen!'
      await messageEditor.inputTitle.fill(title)
      await messageEditor.inputContent.fill(content)
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      await openCitizenPage(mockedTime.addHours(1))
      const header = new CitizenHeader(citizenPage)
      await header.selectTab('messages')
      const citizenMessagesPage = new CitizenMessagesPage(
        citizenPage,
        'desktop'
      )
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.assertThreadContent({ title, content })
    })

    it('should NOT be possible for a citizen without placed children to send new messages', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      await messageEditor.inputContent.fill('message')
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      await openCitizenPage(mockedTime.addHours(1))
      const header = new CitizenHeader(citizenPage)
      await header.selectTab('messages')
      const citizenMessagesPage = new CitizenMessagesPage(
        citizenPage,
        'desktop'
      )
      await citizenMessagesPage.newMessageButton.assertDisabled(true)
    })

    it('citizen can reply to a message when it is in progress, but not after that', async () => {
      // Service worker sends a message regarding an application
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      await messageEditor.inputContent.fill('message')
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      // Progress the application for the citizen to accept
      await execSimpleApplicationActions(
        applicationFixtureId,
        [
          'MOVE_TO_WAITING_PLACEMENT',
          'CREATE_DEFAULT_PLACEMENT_PLAN',
          'SEND_DECISIONS_WITHOUT_PROPOSAL'
        ],
        mockedTime.addMinutes(2)
      )

      // Citizen replies to the application related message
      await openCitizenPage(mockedTime.addHours(1))
      const header = new CitizenHeader(citizenPage)
      await header.selectTab('messages')
      const citizenMessagesPage = new CitizenMessagesPage(
        citizenPage,
        'desktop'
      )
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.replyToFirstThread('This is my reply')
      await citizenPage.findByDataQa('timed-toast-close-button').click()

      // Citizen accepts the decision
      await header.selectTab('decisions')
      const decisionsPage = new CitizenDecisionsPage(citizenPage)
      const decisionResponse =
        await decisionsPage.navigateToDecisionResponse(applicationFixtureId)
      const decisions = await getApplicationDecisions({
        applicationId: applicationFixtureId
      })
      await decisionResponse.acceptDecision(decisions[0].id)

      // Citizen can no longer reply to a message related to the accepted decision
      await header.selectTab('messages')
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.assertOpenReplyEditorButtonIsHidden()
    })

    it('service worker can organize threads in folders', async () => {
      // Service worker sends a message regarding an application
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messagesPage = await applReadView.openMessagesPage()
      const messageEditor = messagesPage.getMessageEditor()
      await messageEditor.inputContent.fill('message')
      await messageEditor.folderSelection.selectOption({ label: 'Kansio 1' })
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      const folderMessagesPage = await messagesPage.openFolder('Kansio 1')
      await folderMessagesPage.messages.assertCount(1)

      await folderMessagesPage.moveFirstThread('Kansio 2')

      await folderMessagesPage.messages.assertCount(0)
      await messagesPage.openFolder('Kansio 2')
      await folderMessagesPage.messages.assertCount(1)

      await folderMessagesPage.openFirstThreadReplyEditor()
    })

    it('service worker cannot mark a message sent by them as unread', async () => {
      // Using this scenario from the previous test as it enables testing
      // the "Mark unread" button to be not shown on employee side
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messagesPage = await applReadView.openMessagesPage()
      const messageEditor = messagesPage.getMessageEditor()
      await messageEditor.inputContent.fill('message')
      await messageEditor.folderSelection.selectOption({ label: 'Kansio 1' })
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      const folderMessagesPage = await messagesPage.openFolder('Kansio 1')
      await folderMessagesPage.messages.assertCount(1)
      await folderMessagesPage.openFirstThread()
      await folderMessagesPage.assertHasNoMarkUnreadButton()
    })

    it('should prefill the recipient and title fields when sending a new message', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()

      await messageEditor.assertRecipient(
        `${testAdult.lastName} ${testAdult.firstName}`
      )
      await messageEditor.assertTitle(
        `Hakemus 08.11.2022: ${testChild.firstName} ${testChild.lastName}`
      )
    })

    it('should create an application note with the message contents when sending a new message', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      const content = 'This should be visible in the application note'
      await messageEditor.inputContent.fill(content)
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      await applReadView.reload()
      await applReadView.assertNote(0, `Lähetetty viesti\n\n${content}`)
    })

    it('should create an application note with a clickable link to the message thread', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      const content = 'This should be visible in the application note'
      await messageEditor.inputContent.fill(content)
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      await applReadView.reload()
      await applReadView.assertNote(0, `Lähetetty viesti\n\n${content}`)

      const messagesPageWithThread =
        await applReadView.clickMessageThreadLinkInNote(0)
      await messagesPageWithThread.assertMessageContent(0, content)
    })

    it('should create an application note when the citizen answers a message', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      const title = 'Message about application that needs a reply'
      const content = 'Hello citizen, please reply to this'
      await messageEditor.inputTitle.fill(title)
      await messageEditor.inputContent.fill(content)
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      await openCitizenPage(mockedTime.addHours(1))
      const header = new CitizenHeader(citizenPage)
      await header.selectTab('messages')
      const citizenMessagesPage = new CitizenMessagesPage(
        citizenPage,
        'desktop'
      )
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.assertThreadContent({ title, content })
      const replyContent = 'This is my reply'
      await citizenMessagesPage.replyToFirstThread(replyContent)

      await openStaffPage(mockedTime.addHours(2), serviceWorker)
      await applReadView.navigateToApplication(applicationFixtureId)
      await applReadView.assertNote(1, `Lähetetty viesti\n\n${replyContent}`)
    })

    it('should open the reply to thread box and create an application note when the service worker sends a second message', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      let applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      const title = 'This is the first message'
      const content = 'First!!1'
      await messageEditor.inputTitle.fill(title)
      await messageEditor.inputContent.fill(content)
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      await openCitizenPage(mockedTime.addHours(1))
      const header = new CitizenHeader(citizenPage)
      await header.selectTab('messages')
      const citizenMessagesPage = new CitizenMessagesPage(
        citizenPage,
        'desktop'
      )
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.assertThreadContent({ title, content })
      await citizenMessagesPage.replyToFirstThread('This is a reply')

      await openStaffPage(mockedTime.addHours(2), serviceWorker)
      applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messagesPage = await applReadView.openMessagesPage()
      await messagesPage.assertReplyContentIsEmpty()
      const replyContent = 'Service worker reply'
      await messagesPage.fillReplyContent(replyContent)
      await messagesPage.sendReplyButton.click()

      await openStaffPage(mockedTime.addHours(3), serviceWorker)
      applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      await applReadView.assertNote(2, `Lähetetty viesti\n\n${replyContent}`)
    })

    it('should not be possible for service workers to compose new messages from the messages page', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      await new EmployeeNav(staffPage).messagesTab.click()
      const messagesPage = new MessagesPage(staffPage)
      await messagesPage.newMessageButton.assertDisabled(true)
    })

    it('should show the simple message editor for service workers', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      await messageEditor.assertSimpleViewVisible()
    })

    it('should not be possible for service workers to delete or edit notes created from messages', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      await messageEditor.inputContent.fill('message content')
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      await applReadView.reload()
      await applReadView.assertNoteNotEditable(0)
      await applReadView.assertNoteNotDeletable(0)
    })

    it('should show the correct thread even when an employee with both messaging and service worker roles click the thread link on the application', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationFixtureId)
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      const content = 'This is the message'
      await messageEditor.inputContent.fill(content)
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      await openStaffPage(mockedTime, messagingAndServiceWorker)
      await applReadView.navigateToApplication(applicationFixtureId)
      await applReadView.assertNote(0, `Lähetetty viesti\n\n${content}`)
      const messagesPageWithThread =
        await applReadView.clickMessageThreadLinkInNote(0)
      await messagesPageWithThread.assertMessageContent(0, content)
    })
  })

  describe('Special education teacher', () => {
    let specialEducationTeacher: DevEmployee
    const applicationId: ApplicationId = uuidv4() as ApplicationId

    beforeEach(async () => {
      await createApplications({
        body: [
          {
            ...applicationFixture(
              testChild,
              testAdult,
              undefined,
              'DAYCARE',
              null,
              [testDaycare.id],
              false,
              'SENT',
              LocalDate.of(2021, 8, 16),
              false,
              true
            ),
            sentDate: mockedToday,
            id: applicationId
          }
        ]
      })
      specialEducationTeacher = await Fixture.employee()
        .specialEducationTeacher(testDaycare.id)
        .save()
    })

    it('should not be possible for special education teacher to send messages relating to an application', async () => {
      const staffPage = await openStaffPage(mockedTime, specialEducationTeacher)
      const applReadView = new ApplicationReadView(staffPage)
      await applReadView.navigateToApplication(applicationId)

      // Currently VEO can write notes so use this to ensure page is loaded
      await staffPage.findByDataQa('add-note').waitUntilVisible()
      await staffPage.findByDataQa('send-message-button').waitUntilHidden()
    })
  })
})
