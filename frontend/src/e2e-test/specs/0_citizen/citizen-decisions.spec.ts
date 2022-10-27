// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  execSimpleApplicationActions,
  getDecisionsByApplication,
  insertApplications,
  resetDatabase,
  runPendingAsyncJobs
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { applicationFixture, Fixture } from '../../dev-api/fixtures'
import AssistanceNeedDecisionPage from '../../pages/citizen/citizen-assistance-need-decision'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let citizenDecisionsPage: CitizenDecisionsPage
let fixtures: AreaAndPersonFixtures

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  page = await Page.open()
  header = new CitizenHeader(page)
  citizenDecisionsPage = new CitizenDecisionsPage(page)
  await enduserLogin(page)
  await page.goto(config.enduserUrl)
})

describe('Citizen application decisions', () => {
  test('Citizen sees their decisions, accepts preschool and rejects preschool daycare', async () => {
    const application = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'PRESCHOOL',
      null,
      [fixtures.daycareFixture.id],
      true
    )
    const applicationId = application.id
    await insertApplications([application])

    await execSimpleApplicationActions(
      applicationId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-decisions-without-proposal'
      ],
      HelsinkiDateTime.now() // TODO: use mock clock
    )

    const decisions = await getDecisionsByApplication(applicationId)
    if (decisions.length !== 2) throw Error('Expected 2 decisions')
    const preschoolDecisionId = decisions.find(
      (d) => d.type === 'PRESCHOOL'
    )?.id
    if (!preschoolDecisionId)
      throw Error('Expected a decision with type PRESCHOOL')
    const preschoolDaycareDecisionId = decisions.find(
      (d) => d.type === 'PRESCHOOL_DAYCARE'
    )?.id
    if (!preschoolDaycareDecisionId)
      throw Error('Expected a decision with type PRESCHOOL_DAYCARE')

    await header.selectTab('decisions')

    await citizenDecisionsPage.assertUnresolvedDecisionsCount(2)
    await citizenDecisionsPage.assertApplicationDecision(
      fixtures.enduserChildFixtureJari.id,
      preschoolDecisionId,
      `Päätös esiopetuksesta ${LocalDate.todayInSystemTz().format()}`,
      LocalDate.todayInSystemTz().format(),
      'Vahvistettavana huoltajalla'
    )
    await citizenDecisionsPage.assertApplicationDecision(
      fixtures.enduserChildFixtureJari.id,
      preschoolDaycareDecisionId,
      `Päätös liittyvästä varhaiskasvatuksesta ${LocalDate.todayInSystemTz().format()}`,
      LocalDate.todayInSystemTz().format(),
      'Vahvistettavana huoltajalla'
    )

    const responsePage = await citizenDecisionsPage.navigateToDecisionResponse(
      applicationId
    )
    await responsePage.assertUnresolvedDecisionsCount(2)

    // preschool daycare decision cannot be accepted before accepting preschool
    await responsePage.assertDecisionCannotBeAccepted(
      preschoolDaycareDecisionId
    )

    await responsePage.assertDecisionData(
      preschoolDecisionId,
      'Päätös esiopetuksesta',
      fixtures.daycareFixture.decisionPreschoolName,
      'Vahvistettavana huoltajalla'
    )

    await responsePage.acceptDecision(preschoolDecisionId)
    await responsePage.assertDecisionStatus(preschoolDecisionId, 'Hyväksytty')
    await responsePage.assertUnresolvedDecisionsCount(1)

    await responsePage.assertDecisionData(
      preschoolDaycareDecisionId,
      'Päätös liittyvästä varhaiskasvatuksesta',
      fixtures.daycareFixture.decisionDaycareName,
      'Vahvistettavana huoltajalla'
    )

    await responsePage.rejectDecision(preschoolDaycareDecisionId)
    await responsePage.assertDecisionStatus(
      preschoolDaycareDecisionId,
      'Hylätty'
    )
    await responsePage.assertUnresolvedDecisionsCount(0)
  })

  test('Rejecting preschool decision also rejects connected daycare after confirmation', async () => {
    const application = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'PRESCHOOL',
      null,
      [fixtures.daycareFixture.id],
      true
    )
    const applicationId = application.id
    await insertApplications([application])

    const now = HelsinkiDateTime.now() // TODO: use mock clock
    await execSimpleApplicationActions(
      applicationId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-decisions-without-proposal'
      ],
      now
    )
    await runPendingAsyncJobs(now)

    const decisions = await getDecisionsByApplication(applicationId)
    if (decisions.length !== 2) throw Error('Expected 2 decisions')
    const preschoolDecisionId = decisions.find(
      (d) => d.type === 'PRESCHOOL'
    )?.id
    if (!preschoolDecisionId)
      throw Error('Expected a decision with type PRESCHOOL')
    const preschoolDaycareDecisionId = decisions.find(
      (d) => d.type === 'PRESCHOOL_DAYCARE'
    )?.id
    if (!preschoolDaycareDecisionId)
      throw Error('Expected a decision with type PRESCHOOL_DAYCARE')

    await header.selectTab('decisions')

    await citizenDecisionsPage.assertUnresolvedDecisionsCount(2)
    const responsePage = await citizenDecisionsPage.navigateToDecisionResponse(
      applicationId
    )
    await responsePage.rejectDecision(preschoolDecisionId)
    await responsePage.confirmRejectCascade()

    await responsePage.assertDecisionStatus(preschoolDecisionId, 'Hylätty')
    await responsePage.assertDecisionStatus(
      preschoolDaycareDecisionId,
      'Hylätty'
    )
    await responsePage.assertUnresolvedDecisionsCount(0)
  })
})

describe('Citizen assistance decisions', () => {
  test('Accepted decision', async () => {
    const decision = await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: { id: fixtures.daycareFixture.id },
        status: 'ACCEPTED',
        assistanceLevels: [
          'ASSISTANCE_SERVICES_FOR_TIME',
          'ENHANCED_ASSISTANCE'
        ],
        validityPeriod: new DateRange(
          LocalDate.of(2020, 2, 5),
          LocalDate.of(2021, 5, 11)
        ),
        decisionMade: LocalDate.of(2020, 1, 17)
      })
      .save()
    await header.selectTab('decisions')

    await citizenDecisionsPage.assertAssistanceDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision.data.id ?? '',
      {
        assistanceLevel:
          'Tukipalvelut päätöksen voimassaolon aikana, tehostettu tuki',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: '05.02.2020 - 11.05.2021',
        decisionMade: '17.01.2020',
        status: 'Hyväksytty'
      }
    )
  })

  test('Rejected decision', async () => {
    const decision = await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: { id: fixtures.daycareFixture.id },
        status: 'REJECTED',
        assistanceLevels: ['ENHANCED_ASSISTANCE'],
        validityPeriod: new DateRange(LocalDate.of(2022, 2, 10), null),
        decisionMade: LocalDate.of(2021, 1, 17)
      })
      .save()
    await header.selectTab('decisions')

    await citizenDecisionsPage.assertAssistanceDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision.data.id ?? '',
      {
        assistanceLevel: 'Tehostettu tuki',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: '10.02.2022 - ',
        decisionMade: '17.01.2021',
        status: 'Hylätty'
      }
    )
  })

  test('Drafts or decisions that need work are not shown', async () => {
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: { id: fixtures.daycareFixture.id },
        status: 'NEEDS_WORK',
        assistanceLevels: ['ENHANCED_ASSISTANCE'],
        decisionMade: LocalDate.of(2021, 1, 17)
      })
      .save()

    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: { id: fixtures.daycareFixture.id },
        status: 'DRAFT',
        assistanceLevels: ['ENHANCED_ASSISTANCE'],
        decisionMade: LocalDate.of(2021, 1, 17)
      })
      .save()
    await header.selectTab('decisions')

    await citizenDecisionsPage.assertNoChildDecisions(
      fixtures.enduserChildFixtureKaarina.id
    )
  })

  test('Unread assistance decisions are indicated', async () => {
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: { id: fixtures.daycareFixture.id },
        status: 'ACCEPTED',
        assistanceLevels: ['SPECIAL_ASSISTANCE'],
        decisionMade: LocalDate.of(2020, 1, 17),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .save()

    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: { id: fixtures.daycareFixture.id },
        status: 'REJECTED',
        assistanceLevels: ['SPECIAL_ASSISTANCE'],
        decisionMade: LocalDate.of(2018, 1, 17),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .save()

    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixturePorriHatterRestricted.id)
      .with({
        selectedUnit: { id: fixtures.daycareFixture.id },
        status: 'ACCEPTED',
        assistanceLevels: ['SPECIAL_ASSISTANCE'],
        decisionMade: LocalDate.of(2020, 1, 17),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .save()
    await header.selectTab('decisions')

    await citizenDecisionsPage.assertUnreadAssistanceNeedDecisions(
      fixtures.enduserChildFixtureKaarina.id,
      2
    )
    await citizenDecisionsPage.assertUnreadAssistanceNeedDecisions(
      fixtures.enduserChildFixturePorriHatterRestricted.id,
      1
    )
  })

  test('Preview shows filled information', async () => {
    const serviceWorker = (await Fixture.employeeServiceWorker().save()).data
    const decision = await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: { id: fixtures.daycareFixture.id },
        status: 'ACCEPTED',
        assistanceLevels: ['ENHANCED_ASSISTANCE'],
        validityPeriod: new DateRange(LocalDate.of(2020, 2, 5), null),
        decisionMade: LocalDate.of(2021, 1, 17),
        decisionMaker: {
          employeeId: serviceWorker.id,
          title: 'head teacher'
        },
        preparedBy1: {
          employeeId: serviceWorker.id,
          title: 'teacher',
          phoneNumber: '010202020202'
        }
      })
      .save()

    await header.selectTab('decisions')
    await citizenDecisionsPage.openAssistanceDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision.data.id ?? ''
    )
    await page.page.waitForURL(
      `${config.enduserUrl}/decisions/assistance/${decision.data.id ?? ''}`
    )
    const assistanceNeedDecisionPage = new AssistanceNeedDecisionPage(page)

    await waitUntilEqual(
      () => assistanceNeedDecisionPage.pedagogicalMotivation,
      'Pedagogical motivation text'
    )
    await assistanceNeedDecisionPage.assertStructuralMotivationOption(
      'groupAssistant'
    )
    await assistanceNeedDecisionPage.assertStructuralMotivationOption(
      'smallerGroup'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.structuralMotivationDescription,
      'Structural motivation description text'
    )
    await assistanceNeedDecisionPage.assertServiceOption(
      'interpretationAndAssistanceServices'
    )
    await assistanceNeedDecisionPage.assertServiceOption('partTimeSpecialEd')
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.careMotivation,
      'Care motivation text'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.guardiansHeardOn,
      '05.04.2020'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.otherRepresentativeDetails,
      'John Doe, 01020304050, via phone'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.viewOfGuardians,
      'VOG text'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.futureLevelOfAssistance,
      'Tehostettu tuki'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.startDate,
      '05.02.2020'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.selectedUnit,
      `${fixtures.daycareFixture.name}\n${fixtures.daycareFixture.streetAddress}\n${fixtures.daycareFixture.postalCode} ${fixtures.daycareFixture.postOffice}\nLoma-aikoina tuen järjestämispaikka ja -tapa saattavat muuttua.`
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.motivationForDecision,
      'Motivation for decision text'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.preparedBy1,
      `${serviceWorker.firstName} ${serviceWorker.lastName}, teacher\n010202020202`
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.decisionMaker,
      `${serviceWorker.firstName} ${serviceWorker.lastName}, head teacher`
    )
  })
})
