// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createApplications,
  createMessageAccounts,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import ApplicationsPage from '../../pages/employee/applications'
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
  await initializeAreaAndPersonData()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare().with(testDaycare).save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  serviceWorker = await Fixture.employeeServiceWorker().save()
  messagingAndServiceWorker = await Fixture.employeeServiceWorker()
    .with({
      roles: ['SERVICE_WORKER', 'MESSAGING']
    })
    .save()
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
      const applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      const applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
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
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.assertThreadContent({ title, content })
    })

    it('should NOT be possible for a citizen without placed children to send new messages', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      const applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
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
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.newMessageButton.assertDisabled(true)
    })

    it('should prefill the receiver and title fields when sending a new message', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      const applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()

      await messageEditor.assertReceiver(
        `${testAdult.lastName} ${testAdult.firstName}`
      )
      await messageEditor.assertTitle(
        `Hakemus 08.11.2022: ${testChild.firstName} ${testChild.lastName}`
      )
    })

    it('should create an application note with the message contents when sending a new message', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      const applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
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
      const applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      const applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
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
      let applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      let applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
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
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.assertThreadContent({ title, content })
      const replyContent = 'This is my reply'
      await citizenMessagesPage.replyToFirstThread(replyContent)

      await openStaffPage(mockedTime.addHours(2), serviceWorker)
      applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
      await applReadView.assertNote(1, `Lähetetty viesti\n\n${replyContent}`)
    })

    it('should open the reply to thread box and create an application note when the service worker sends a second message', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      let applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      let applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
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
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.assertThreadContent({ title, content })
      await citizenMessagesPage.replyToFirstThread('This is a reply')

      await openStaffPage(mockedTime.addHours(2), serviceWorker)
      applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
      const messagesPage = await applReadView.openMessagesPage()
      await messagesPage.assertReplyContentIsEmpty()
      const replyContent = 'Service worker reply'
      await messagesPage.fillReplyContent(replyContent)
      await messagesPage.sendReplyButton.click()

      await openStaffPage(mockedTime.addHours(2), serviceWorker)
      applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
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
      const applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      const applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      await messageEditor.assertSimpleViewVisible()
    })

    it('should not be possible for service workers to delete or edit notes created from messages', async () => {
      await openStaffPage(mockedTime, serviceWorker)
      const applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      const applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
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
      let applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      let applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
      const messageEditor = (
        await applReadView.openMessagesPage()
      ).getMessageEditor()
      const content = 'This is the message'
      await messageEditor.inputContent.fill(content)
      await messageEditor.sendButton.click()
      await messageEditor.waitUntilHidden()
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      await openStaffPage(mockedTime, messagingAndServiceWorker)
      applicationsPage = new ApplicationsPage(staffPage)
      await new EmployeeNav(staffPage).applicationsTab.click()
      applReadView = await applicationsPage
        .applicationRow(applicationFixtureId)
        .openApplication()
      await applReadView.assertNote(0, `Lähetetty viesti\n\n${content}`)
      const messagesPageWithThread =
        await applReadView.clickMessageThreadLinkInNote(0)
      await messagesPageWithThread.assertMessageContent(0, content)
    })
  })
})
