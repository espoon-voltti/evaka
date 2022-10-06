// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  execSimpleApplicationActions,
  getDecisionsByApplication,
  insertFosterParents,
  resetDatabase,
  runPendingAsyncJobs,
  upsertMessageAccounts
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { Fixture } from '../../dev-api/fixtures'
import { PersonDetail } from '../../dev-api/types'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import CitizenPedagogicalDocumentsPage from '../../pages/citizen/citizen-pedagogical-documents'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { waitUntilEqual } from '../../utils'
import { minimalDaycareForm } from '../../utils/application-forms'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let citizenPage: Page
let header: CitizenHeader
let fixtures: AreaAndPersonFixtures
let fosterParent: PersonDetail
let fosterChild: PersonDetail
const mockedDate = LocalDate.of(2021, 4, 1)

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  fosterParent = fixtures.enduserGuardianFixture
  fosterChild = (await Fixture.person().with({ ssn: '290413A902C' }).save())
    .data
  await Fixture.child(fosterChild.id).save()
  await insertFosterParents([
    {
      childId: fosterChild.id,
      parentId: fosterParent.id,
      validDuring: new DateRange(mockedDate, null)
    }
  ])

  citizenPage = await Page.open({ mockedTime: mockedDate.toSystemTzDate() })
  await enduserLogin(citizenPage)
  header = new CitizenHeader(citizenPage)
})

test('Foster parent can create a daycare application and accept a daycare decision', async () => {
  const applicationsPage = new CitizenApplicationsPage(citizenPage)

  await header.selectTab('applications')
  const editorPage = await applicationsPage.createApplication(
    fosterChild.id,
    'DAYCARE'
  )
  const applicationId = editorPage.getNewApplicationId()

  await editorPage.fillData(minimalDaycareForm.form)
  await editorPage.assertChildAddress(
    `${fosterChild.streetAddress ?? ''}, ${fosterChild.postalCode ?? ''} ${
      fosterChild.postOffice ?? ''
    }`
  )
  await editorPage.verifyAndSend()

  await applicationsPage.assertApplicationIsListed(
    applicationId,
    'Varhaiskasvatushakemus',
    minimalDaycareForm.form.unitPreference?.preferredUnits?.[0].name ?? '',
    minimalDaycareForm.form.serviceNeed?.preferredStartDate ?? '',
    'Lähetetty'
  )

  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-decisions-without-proposal',
    'confirm-decision-mailed'
  ])
  await runPendingAsyncJobs()

  const citizenDecisionsPage = new CitizenDecisionsPage(citizenPage)
  const decisions = await getDecisionsByApplication(applicationId)
  const decisionId = decisions[0].id
  await header.selectTab('decisions')
  const responsePage = await citizenDecisionsPage.navigateToDecisionResponse(
    applicationId
  )
  await responsePage.assertUnresolvedDecisionsCount(1)
  await responsePage.acceptDecision(decisionId)
  await responsePage.assertDecisionStatus(decisionId, 'Hyväksytty')
  await responsePage.assertUnresolvedDecisionsCount(0)
})

test('Foster parent can receive and reply to messages', async () => {
  const unitId = fixtures.daycareFixture.id
  const group = await Fixture.daycareGroup().with({ daycareId: unitId }).save()
  const placementFixture = await Fixture.placement()
    .with({
      childId: fosterChild.id,
      unitId,
      startDate: mockedDate.formatIso(),
      endDate: mockedDate.addYears(1).formatIso()
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: placementFixture.data.id,
      daycareGroupId: group.data.id,
      startDate: mockedDate.formatIso(),
      endDate: mockedDate.addYears(1).formatIso()
    })
    .save()

  const unitSupervisor = await Fixture.employeeUnitSupervisor(unitId).save()
  await upsertMessageAccounts()
  const unitSupervisorPage = await Page.open({
    mockedTime: mockedDate.toSystemTzDate()
  })
  await employeeLogin(unitSupervisorPage, unitSupervisor.data)

  await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
  const messagesPage = new MessagesPage(unitSupervisorPage)
  const message = {
    title: 'Message title',
    content: 'Message content'
  }
  await messagesPage.sendNewMessage(message)

  await citizenPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
  await citizenMessagesPage.assertThreadContent(message)
  const reply = 'Message reply'
  await citizenMessagesPage.replyToFirstThread(reply)
  await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

  await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
  await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
  await messagesPage.assertMessageContent(1, reply)
})

test('Foster parent can read an accepted assistance decision', async () => {
  const citizenDecisionsPage = new CitizenDecisionsPage(citizenPage)
  const decision = await Fixture.preFilledAssistanceNeedDecision()
    .withChild(fosterChild.id)
    .with({
      selectedUnit: { id: fixtures.daycareFixture.id },
      status: 'ACCEPTED',
      assistanceLevels: ['ASSISTANCE_SERVICES_FOR_TIME', 'ENHANCED_ASSISTANCE'],
      validityPeriod: new DateRange(mockedDate, mockedDate.addYears(1)),
      decisionMade: mockedDate
    })
    .save()
  await header.selectTab('decisions')

  await citizenDecisionsPage.assertAssistanceDecision(
    fosterChild.id,
    decision.data.id ?? '',
    {
      assistanceLevel:
        'Tukipalvelut päätöksen voimassaolon aikana, tehostettu tuki',
      selectedUnit: fixtures.daycareFixture.name,
      validityPeriod: `${mockedDate.format()} - ${mockedDate
        .addYears(1)
        .format()}`,
      decisionMade: mockedDate.format(),
      status: 'Hyväksytty'
    }
  )
})

test('Foster parent can read a pedagogical document', async () => {
  await Fixture.placement()
    .with({
      childId: fosterChild.id,
      unitId: fixtures.daycareFixture.id,
      startDate: mockedDate.formatIso(),
      endDate: mockedDate.addYears(1).formatIso()
    })
    .save()
  const document = await Fixture.pedagogicalDocument()
    .with({
      childId: fosterChild.id,
      description: 'e2e test description'
    })
    .save()
  await citizenPage.reload()

  await header.openChildPage(fosterChild.id)
  const childPage = new CitizenChildPage(citizenPage)
  await childPage.openCollapsible('pedagogical-documents')
  const pedagogicalDocumentsPage = new CitizenPedagogicalDocumentsPage(
    citizenPage
  )
  await pedagogicalDocumentsPage.assertPedagogicalDocumentExists(
    document.data.id,
    LocalDate.todayInSystemTz().format(),
    document.data.description
  )
})
