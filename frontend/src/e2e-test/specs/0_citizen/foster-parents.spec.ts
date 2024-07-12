// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  execSimpleApplicationActions,
  runPendingAsyncJobs
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  Fixture,
  testAdult,
  testCareArea,
  testDaycare,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createFosterParent,
  createMessageAccounts,
  createVasuDocument,
  getApplicationDecisions,
  publishVasuDocument,
  resetServiceState
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import CitizenPedagogicalDocumentsPage from '../../pages/citizen/citizen-pedagogical-documents'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { VasuPreviewPage } from '../../pages/employee/vasu/vasu'
import { waitUntilEqual } from '../../utils'
import { minimalDaycareForm } from '../../utils/application-forms'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let activeRelationshipPage: Page
let activeRelationshipHeader: CitizenHeader
let fosterParent: DevPerson
let fosterChild: DevPerson

const mockedNow = HelsinkiDateTime.of(2021, 4, 1, 15, 0)
const mockedDate = mockedNow.toLocalDate()

beforeEach(async () => {
  await resetServiceState()
  await initializeAreaAndPersonData()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare().with(testDaycare).save()

  fosterParent = await Fixture.person()
    .with(testAdult)
    .saveAdult({ updateMockVtjWithDependants: [] })
  fosterChild = await Fixture.person()
    .with({ ssn: '120220A995L' })
    .saveChild({ updateMockVtj: true })
  await Fixture.child(fosterChild.id).save()
  await createFosterParent({
    body: [
      {
        id: uuidv4(),
        childId: fosterChild.id,
        parentId: fosterParent.id,
        validDuring: new DateRange(mockedDate, mockedDate)
      }
    ]
  })

  activeRelationshipPage = await Page.open({
    mockedTime: mockedNow
  })
  await enduserLogin(activeRelationshipPage, testAdult)
  activeRelationshipHeader = new CitizenHeader(activeRelationshipPage)
})

async function openEndedRelationshipPage() {
  const endedRelationshipPage = await Page.open({
    mockedTime: mockedDate.addDays(1).toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await enduserLogin(endedRelationshipPage, testAdult)
  const endedRelationshipHeader = new CitizenHeader(endedRelationshipPage)
  return { endedRelationshipPage, endedRelationshipHeader }
}

test('Foster parent can create a daycare application and accept a daycare decision', async () => {
  const applicationsPage = new CitizenApplicationsPage(activeRelationshipPage)

  await activeRelationshipHeader.selectTab('applications')
  const editorPage = await applicationsPage.createApplication(
    fosterChild.id,
    'DAYCARE'
  )
  const applicationId = editorPage.getNewApplicationId()

  const applicationForm = minimalDaycareForm().form
  await editorPage.fillData(applicationForm)
  await editorPage.assertChildAddress(
    `${fosterChild.streetAddress ?? ''}, ${fosterChild.postalCode ?? ''} ${
      fosterChild.postOffice ?? ''
    }`
  )
  await editorPage.verifyAndSend({ hasOtherGuardian: false })

  await applicationsPage.assertApplicationIsListed(
    applicationId,
    'Varhaiskasvatushakemus',
    applicationForm.serviceNeed?.preferredStartDate ?? '',
    'Lähetetty'
  )

  await execSimpleApplicationActions(
    applicationId,
    [
      'move-to-waiting-placement',
      'create-default-placement-plan',
      'send-decisions-without-proposal',
      'confirm-decision-mailed'
    ],
    mockedNow
  )

  const citizenDecisionsPage = new CitizenDecisionsPage(activeRelationshipPage)
  const decisions = await getApplicationDecisions({ applicationId })
  const decisionId = decisions[0].id
  await activeRelationshipHeader.selectTab('decisions')
  const responsePage =
    await citizenDecisionsPage.navigateToDecisionResponse(applicationId)
  await responsePage.assertUnresolvedDecisionsCount(1)
  await responsePage.acceptDecision(decisionId)
  await responsePage.assertDecisionStatus(decisionId, 'Hyväksytty')
  await responsePage.assertUnresolvedDecisionsCount(0)

  const { endedRelationshipPage, endedRelationshipHeader } =
    await openEndedRelationshipPage()
  await endedRelationshipHeader.selectTab('applications')
  await endedRelationshipPage
    .findByDataQa('applications-list')
    .assertAttributeEquals('data-isloading', 'false')
  await endedRelationshipPage
    .findByDataQa(`child-${fosterChild.id}`)
    .waitUntilHidden()
})

test('Foster parent can create a daycare application and accept a daycare decision for a child without a SSN', async () => {
  const fosterChild = await Fixture.person().with({ ssn: null }).saveChild()

  await Fixture.child(fosterChild.id).save()
  await createFosterParent({
    body: [
      {
        id: uuidv4(),
        childId: fosterChild.id,
        parentId: fosterParent.id,
        validDuring: new DateRange(mockedDate, mockedDate)
      }
    ]
  })
  await activeRelationshipPage.reload()
  const applicationsPage = new CitizenApplicationsPage(activeRelationshipPage)

  await activeRelationshipHeader.selectTab('applications')
  const editorPage = await applicationsPage.createApplication(
    fosterChild.id,
    'DAYCARE'
  )
  const applicationId = editorPage.getNewApplicationId()

  const applicationForm = minimalDaycareForm().form
  await editorPage.fillData(applicationForm)
  await editorPage.assertChildAddress(
    `${fosterChild.streetAddress ?? ''}, ${fosterChild.postalCode ?? ''} ${
      fosterChild.postOffice ?? ''
    }`
  )
  await editorPage.verifyAndSend({ hasOtherGuardian: false })

  await applicationsPage.assertApplicationIsListed(
    applicationId,
    'Varhaiskasvatushakemus',
    applicationForm.serviceNeed?.preferredStartDate ?? '',
    'Lähetetty'
  )

  await execSimpleApplicationActions(
    applicationId,
    [
      'move-to-waiting-placement',
      'create-default-placement-plan',
      'send-decisions-without-proposal',
      'confirm-decision-mailed'
    ],
    HelsinkiDateTime.now() // TODO: use mock clock
  )

  const citizenDecisionsPage = new CitizenDecisionsPage(activeRelationshipPage)
  const decisions = await getApplicationDecisions({ applicationId })
  const decisionId = decisions[0].id
  await activeRelationshipHeader.selectTab('decisions')
  const responsePage =
    await citizenDecisionsPage.navigateToDecisionResponse(applicationId)
  await responsePage.assertUnresolvedDecisionsCount(1)
  await responsePage.acceptDecision(decisionId)
  await responsePage.assertDecisionStatus(decisionId, 'Hyväksytty')
  await responsePage.assertUnresolvedDecisionsCount(0)

  const { endedRelationshipPage, endedRelationshipHeader } =
    await openEndedRelationshipPage()
  await endedRelationshipHeader.selectTab('applications')
  await endedRelationshipPage
    .findByDataQa('applications-list')
    .assertAttributeEquals('data-isloading', 'false')
  await endedRelationshipPage
    .findByDataQa(`child-${fosterChild.id}`)
    .waitUntilHidden()
})

test('Foster parent can receive and reply to messages', async () => {
  const unitId = testDaycare.id
  const group = await Fixture.daycareGroup().with({ daycareId: unitId }).save()
  const placementFixture = await Fixture.placement()
    .with({
      childId: fosterChild.id,
      unitId,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: placementFixture.id,
      daycareGroupId: group.id,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()

  const unitSupervisor = await Fixture.employeeUnitSupervisor(unitId).save()
  await createMessageAccounts()
  let unitSupervisorPage = await Page.open({
    mockedTime: mockedNow.subMinutes(1)
  })
  await employeeLogin(unitSupervisorPage, unitSupervisor)

  await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
  let messagesPage = new MessagesPage(unitSupervisorPage)
  const message = {
    title: 'Message title',
    content: 'Message content'
  }
  const messageEditor = await messagesPage.openMessageEditor()
  await messageEditor.sendNewMessage(message)
  await runPendingAsyncJobs(mockedNow)

  await activeRelationshipPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(activeRelationshipPage)
  await citizenMessagesPage.assertThreadContent(message)
  const reply = 'Message reply'
  await citizenMessagesPage.replyToFirstThread(reply)
  await runPendingAsyncJobs(mockedNow.addMinutes(1))
  await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

  unitSupervisorPage = await Page.open({
    mockedTime: mockedNow.addMinutes(1)
  })
  await employeeLogin(unitSupervisorPage, unitSupervisor)
  messagesPage = new MessagesPage(unitSupervisorPage)
  await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
  await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
  await messagesPage.receivedMessage.click()
  await messagesPage.assertMessageContent(1, reply)

  const { endedRelationshipHeader } = await openEndedRelationshipPage()
  await endedRelationshipHeader.assertNoTab('messages')
})

test('Foster parent can read an accepted assistance decision', async () => {
  const citizenDecisionsPage = new CitizenDecisionsPage(activeRelationshipPage)
  const decision = await Fixture.preFilledAssistanceNeedDecision()
    .withChild(fosterChild.id)
    .with({
      selectedUnit: testDaycare.id,
      status: 'ACCEPTED',
      assistanceLevels: ['ASSISTANCE_SERVICES_FOR_TIME', 'ENHANCED_ASSISTANCE'],
      validityPeriod: new DateRange(mockedDate, mockedDate.addYears(1)),
      decisionMade: mockedDate
    })
    .save()
  await activeRelationshipHeader.selectTab('decisions')

  await citizenDecisionsPage.assertAssistanceDecision(
    fosterChild.id,
    decision.id ?? '',
    {
      assistanceLevel:
        'Tukipalvelut päätöksen voimassaolon aikana, tehostettu tuki',
      selectedUnit: testDaycare.name,
      validityPeriod: `${mockedDate.format()} - ${mockedDate
        .addYears(1)
        .format()}`,
      decisionMade: mockedDate.format(),
      status: 'Hyväksytty'
    }
  )

  const { endedRelationshipPage, endedRelationshipHeader } =
    await openEndedRelationshipPage()
  await endedRelationshipHeader.selectTab('decisions')
  await activeRelationshipPage.goto(config.enduserMessagesUrl)
  await new CitizenDecisionsPage(endedRelationshipPage).assertNoChildDecisions(
    fosterChild.id
  )
})

test('Foster parent can read a pedagogical document', async () => {
  await Fixture.placement()
    .with({
      childId: fosterChild.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()
  const document = await Fixture.pedagogicalDocument()
    .with({
      childId: fosterChild.id,
      description: 'e2e test description'
    })
    .save()
  await activeRelationshipPage.reload()

  await activeRelationshipHeader.openChildPage(fosterChild.id)
  const childPage = new CitizenChildPage(activeRelationshipPage)
  await childPage.openCollapsible('pedagogical-documents')
  const pedagogicalDocumentsPage = new CitizenPedagogicalDocumentsPage(
    activeRelationshipPage
  )
  await pedagogicalDocumentsPage.assertPedagogicalDocumentExists(
    document.id,
    LocalDate.todayInSystemTz().format(),
    document.description
  )

  const { endedRelationshipHeader } = await openEndedRelationshipPage()
  await endedRelationshipHeader.assertNoChildrenTab()
})

test('Foster parent can read a daycare curriculum and give permission to share it', async () => {
  await Fixture.placement()
    .with({
      childId: fosterChild.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()
  const vasuDocId = await createVasuDocument({
    body: {
      childId: fosterChild.id,
      templateId: await Fixture.vasuTemplate().saveAndReturnId()
    }
  })
  await publishVasuDocument({ documentId: vasuDocId })
  await activeRelationshipPage.reload()

  await activeRelationshipHeader.openChildPage(fosterChild.id)
  const childPage = new CitizenChildPage(activeRelationshipPage)
  await childPage.openCollapsible('vasu')
  await childPage.assertVasuRow(
    vasuDocId,
    'Luonnos',
    LocalDate.todayInSystemTz().format('dd.MM.yyyy')
  )
  await childPage.openVasu(vasuDocId)
  const vasuPage = new VasuPreviewPage(activeRelationshipPage)
  await activeRelationshipHeader.assertUnreadChildrenCount(1)
  await vasuPage.assertTitleChildName(
    `${fosterChild.firstName} ${fosterChild.lastName}`
  )
  await vasuPage.givePermissionToShare()
  await vasuPage.assertGivePermissionToShareSectionIsNotVisible()
  await activeRelationshipHeader.assertUnreadChildrenCount(0)

  const { endedRelationshipHeader } = await openEndedRelationshipPage()
  await endedRelationshipHeader.assertNoChildrenTab()
})

test('Foster parent can terminate a daycare placement', async () => {
  const endDate = mockedDate.addYears(1)
  await Fixture.placement()
    .with({
      childId: fosterChild.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: endDate
    })
    .save()
  await activeRelationshipPage.reload()

  await activeRelationshipHeader.openChildPage(fosterChild.id)
  const childPage = new CitizenChildPage(activeRelationshipPage)
  await childPage.openCollapsible('termination')

  await childPage.assertTerminatedPlacementCount(0)
  await childPage.assertTerminatablePlacementCount(1)
  await childPage.togglePlacement(
    `Varhaiskasvatus, ${testDaycare.name}, voimassa ${endDate.format()}`
  )
  await childPage.fillTerminationDate(mockedDate)
  await childPage.submitTermination()
  await childPage.assertTerminatablePlacementCount(0)

  await childPage.assertTerminatedPlacementCount(1)
  await waitUntilEqual(
    () => childPage.getTerminatedPlacements(),
    [
      `Varhaiskasvatus, ${
        testDaycare.name
      }, viimeinen läsnäolopäivä: ${mockedDate.format()}`
    ]
  )

  const { endedRelationshipHeader } = await openEndedRelationshipPage()
  await endedRelationshipHeader.assertNoChildrenTab()
})

test('Foster parent can create a repeating reservation', async () => {
  await Fixture.placement()
    .with({
      childId: fosterChild.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()
  await activeRelationshipPage.reload()
  const firstReservationDay = mockedDate.addDays(14)

  await activeRelationshipHeader.selectTab('calendar')
  const calendarPage = new CitizenCalendarPage(
    activeRelationshipPage,
    'desktop'
  )
  const reservationsModal = await calendarPage.openReservationModal()
  await reservationsModal.createRepeatingDailyReservation(
    new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
    '08:00',
    '16:00'
  )

  await calendarPage.assertDay(firstReservationDay, [
    {
      childIds: [fosterChild.id],
      text: '08:00–16:00'
    }
  ])

  const { endedRelationshipHeader } = await openEndedRelationshipPage()
  await endedRelationshipHeader.assertNoTab('calendar')
})

test('Foster parent can see calendar events for foster children', async () => {
  const placement = await Fixture.placement()
    .with({
      childId: fosterChild.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()
  const daycareGroup = await Fixture.daycareGroup()
    .with({
      daycareId: testDaycare.id,
      name: 'Group 1'
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      startDate: mockedDate,
      endDate: mockedDate.addYears(1),
      daycareGroupId: daycareGroup.id,
      daycarePlacementId: placement.id
    })
    .save()
  const groupEventId = uuidv4()
  await Fixture.calendarEvent()
    .with({
      id: groupEventId,
      title: 'Group-wide event',
      description: 'Whole group',
      period: new FiniteDateRange(mockedDate, mockedDate),
      modifiedAt: HelsinkiDateTime.fromLocal(mockedDate, LocalTime.MIN)
    })
    .save()
  await Fixture.calendarEventAttendee()
    .with({
      calendarEventId: groupEventId,
      unitId: testDaycare.id,
      groupId: daycareGroup.id
    })
    .save()
  await activeRelationshipPage.reload()
  await activeRelationshipHeader.selectTab('calendar')
  const calendarPage = new CitizenCalendarPage(
    activeRelationshipPage,
    'desktop'
  )
  await calendarPage.assertEventCount(mockedDate, 1)
  const dayView = await calendarPage.openDayView(mockedDate)
  await dayView.assertEvent(fosterChild.id, groupEventId, {
    title: 'Group-wide event / Group 1',
    description: 'Whole group'
  })
})
