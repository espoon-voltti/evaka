// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  execSimpleApplicationActions,
  runPendingAsyncJobs
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { applicationFixture, Fixture } from '../../dev-api/fixtures'
import {
  createApplications,
  getApplicationDecisions,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import AssistanceNeedDecisionPage from '../../pages/citizen/citizen-assistance-need-decision'
import AssistanceNeedPreschoolDecisionPage from '../../pages/citizen/citizen-assistance-need-preschool-decision'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let decisionMaker: DevEmployee
const now = HelsinkiDateTime.of(2023, 3, 15, 12, 0)

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()
  decisionMaker = await Fixture.employeeServiceWorker().save()
})

async function openCitizenDecisionsPage(citizen: { ssn: string | null }) {
  const page = await Page.open({
    mockedTime: now
  })
  await enduserLogin(page, citizen.ssn ?? undefined)
  const header = new CitizenHeader(page)
  await header.selectTab('decisions')
  const citizenDecisionsPage = new CitizenDecisionsPage(page)
  return {
    page,
    header,
    citizenDecisionsPage
  }
}

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
    await createApplications({ body: [application] })

    await execSimpleApplicationActions(
      applicationId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-decisions-without-proposal'
      ],
      now
    )

    const decisions = await getApplicationDecisions({ applicationId })
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

    const { citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )

    await citizenDecisionsPage.assertUnresolvedDecisionsCount(2)
    await citizenDecisionsPage.assertApplicationDecision(
      fixtures.enduserChildFixtureJari.id,
      preschoolDecisionId,
      `Päätös esiopetuksesta ${now.toLocalDate().format()}`,
      now.toLocalDate().format(),
      'Vahvistettavana huoltajalla'
    )
    await citizenDecisionsPage.assertApplicationDecision(
      fixtures.enduserChildFixtureJari.id,
      preschoolDaycareDecisionId,
      `Päätös liittyvästä varhaiskasvatuksesta ${now.toLocalDate().format()}`,
      now.toLocalDate().format(),
      'Vahvistettavana huoltajalla'
    )

    const responsePage =
      await citizenDecisionsPage.navigateToDecisionResponse(applicationId)
    await responsePage.assertUnresolvedDecisionsCount(2)

    // preschool daycare decision cannot be accepted before accepting preschool
    await responsePage.assertDecisionCannotBeAccepted(
      preschoolDaycareDecisionId
    )

    await responsePage.assertDecisionData(
      preschoolDecisionId,
      'Päätös esiopetuksesta',
      fixtures.daycareFixture.decisionCustomization.preschoolName,
      'Vahvistettavana huoltajalla'
    )

    await responsePage.acceptDecision(preschoolDecisionId)
    await responsePage.assertDecisionStatus(preschoolDecisionId, 'Hyväksytty')
    await responsePage.assertUnresolvedDecisionsCount(1)

    await responsePage.assertDecisionData(
      preschoolDaycareDecisionId,
      'Päätös liittyvästä varhaiskasvatuksesta',
      fixtures.daycareFixture.decisionCustomization.daycareName,
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
    await createApplications({ body: [application] })

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

    const decisions = await getApplicationDecisions({ applicationId })
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

    const { citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )

    await citizenDecisionsPage.assertUnresolvedDecisionsCount(2)
    const responsePage =
      await citizenDecisionsPage.navigateToDecisionResponse(applicationId)
    await responsePage.rejectDecision(preschoolDecisionId)
    await responsePage.confirmRejectCascade()

    await responsePage.assertDecisionStatus(preschoolDecisionId, 'Hylätty')
    await responsePage.assertDecisionStatus(
      preschoolDaycareDecisionId,
      'Hylätty'
    )
    await responsePage.assertUnresolvedDecisionsCount(0)
  })

  test('Guardian sees decisions related to applications made by the other guardian', async () => {
    const child = await Fixture.person()
      .with({ ssn: '010116A9219' })
      .saveAndUpdateMockVtj()
    const guardian = await Fixture.person()
      .with({ ssn: '010106A973C' })
      .saveAndUpdateMockVtj([child])
    const otherGuardian = await Fixture.person()
      .with({ ssn: '010106A9388' })
      .saveAndUpdateMockVtj([child])

    const application = applicationFixture(
      child,
      guardian,
      otherGuardian,
      'PRESCHOOL',
      null,
      [fixtures.daycareFixture.id],
      true
    )
    await createApplications({ body: [application] })

    await execSimpleApplicationActions(
      application.id,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-decisions-without-proposal'
      ],
      now
    )

    const { citizenDecisionsPage } =
      await openCitizenDecisionsPage(otherGuardian)
    await citizenDecisionsPage.assertChildDecisionCount(2, child.id)
    // other guardian can only see the decisions but not resolve them -> not shown in counts
    await citizenDecisionsPage.assertUnresolvedDecisionsCount(0)
  })
})

describe('Citizen assistance decisions', () => {
  test('Accepted decision', async () => {
    const decision = await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: fixtures.daycareFixture.id,
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
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )

    await citizenDecisionsPage.assertAssistanceDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision.id ?? '',
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
        selectedUnit: fixtures.daycareFixture.id,
        status: 'REJECTED',
        assistanceLevels: ['ENHANCED_ASSISTANCE'],
        validityPeriod: new DateRange(LocalDate.of(2022, 2, 10), null),
        decisionMade: LocalDate.of(2021, 1, 17)
      })
      .save()
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )

    await citizenDecisionsPage.assertAssistanceDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision.id ?? '',
      {
        assistanceLevel: 'Tehostettu tuki',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: '10.02.2022 -',
        decisionMade: '17.01.2021',
        status: 'Hylätty'
      }
    )
  })

  test('Annulled decision', async () => {
    const decision = await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: fixtures.daycareFixture.id,
        status: 'ANNULLED',
        annulmentReason: 'Well because',
        assistanceLevels: ['ENHANCED_ASSISTANCE'],
        validityPeriod: new DateRange(LocalDate.of(2022, 2, 10), null),
        decisionMade: LocalDate.of(2021, 1, 17)
      })
      .save()
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )

    await citizenDecisionsPage.assertAssistanceDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision.id ?? '',
      {
        assistanceLevel: 'Tehostettu tuki',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: '10.02.2022 -',
        decisionMade: '17.01.2021',
        status: 'Mitätöity'
      }
    )
  })

  test('Drafts or decisions that need work are not shown', async () => {
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: fixtures.daycareFixture.id,
        status: 'NEEDS_WORK',
        assistanceLevels: ['ENHANCED_ASSISTANCE'],
        decisionMade: LocalDate.of(2021, 1, 17)
      })
      .save()

    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: fixtures.daycareFixture.id,
        status: 'DRAFT',
        assistanceLevels: ['ENHANCED_ASSISTANCE'],
        decisionMade: LocalDate.of(2021, 1, 17)
      })
      .save()
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )

    await citizenDecisionsPage.assertNoChildDecisions(
      fixtures.enduserChildFixtureKaarina.id
    )
  })

  test('Unread assistance decisions are indicated', async () => {
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: fixtures.daycareFixture.id,
        status: 'ACCEPTED',
        assistanceLevels: ['SPECIAL_ASSISTANCE'],
        decisionMade: LocalDate.of(2020, 1, 17),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .save()

    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: fixtures.daycareFixture.id,
        status: 'REJECTED',
        assistanceLevels: ['SPECIAL_ASSISTANCE'],
        decisionMade: LocalDate.of(2018, 1, 17),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .save()

    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixturePorriHatterRestricted.id)
      .with({
        selectedUnit: fixtures.daycareFixture.id,
        status: 'ACCEPTED',
        assistanceLevels: ['SPECIAL_ASSISTANCE'],
        decisionMade: LocalDate.of(2020, 1, 17),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .save()
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )

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
    const serviceWorker = await Fixture.employeeServiceWorker().save()
    const decision = await Fixture.preFilledAssistanceNeedDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .with({
        selectedUnit: fixtures.daycareFixture.id,
        status: 'ACCEPTED',
        assistanceLevels: ['ENHANCED_ASSISTANCE'],
        validityPeriod: new DateRange(LocalDate.of(2020, 2, 5), null),
        decisionMade: LocalDate.of(2021, 1, 17),
        decisionMaker: {
          employeeId: serviceWorker.id,
          title: 'head teacher',
          name: null,
          phoneNumber: null
        },
        preparedBy1: {
          employeeId: serviceWorker.id,
          title: 'teacher',
          phoneNumber: '010202020202',
          name: null
        }
      })
      .save()

    const { page, citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )
    await citizenDecisionsPage.openAssistanceDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision.id ?? ''
    )
    await page.page.waitForURL(
      `${config.enduserUrl}/decisions/assistance/${decision.id ?? ''}`
    )
    const assistanceNeedDecisionPage = new AssistanceNeedDecisionPage(page)

    await waitUntilEqual(
      () => assistanceNeedDecisionPage.pedagogicalMotivation(),
      'Pedagogical motivation text'
    )
    await assistanceNeedDecisionPage.assertStructuralMotivationOption(
      'groupAssistant'
    )
    await assistanceNeedDecisionPage.assertStructuralMotivationOption(
      'smallerGroup'
    )
    await assistanceNeedDecisionPage.structuralMotivationDescription.assertTextEquals(
      'Structural motivation description text'
    )
    await assistanceNeedDecisionPage.assertServiceOption(
      'interpretationAndAssistanceServices'
    )
    await assistanceNeedDecisionPage.assertServiceOption('partTimeSpecialEd')
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.careMotivation(),
      'Care motivation text'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.guardiansHeardOn(),
      '05.04.2020'
    )
    await assistanceNeedDecisionPage.otherRepresentativeDetails.assertTextEquals(
      'John Doe, 01020304050, via phone'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.viewOfGuardians(),
      'VOG text'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.futureLevelOfAssistance(),
      'Tehostettu tuki'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.startDate(),
      '05.02.2020'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.selectedUnit(),
      `${fixtures.daycareFixture.name}\n${fixtures.daycareFixture.visitingAddress.streetAddress}\n${fixtures.daycareFixture.visitingAddress.postalCode} ${fixtures.daycareFixture.visitingAddress.postOffice}\nLoma-aikoina tuen järjestämispaikka ja -tapa saattavat muuttua.`
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.motivationForDecision(),
      'Motivation for decision text'
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.preparedBy1(),
      `${serviceWorker.firstName} ${serviceWorker.lastName}, teacher\n010202020202`
    )
    await waitUntilEqual(
      () => assistanceNeedDecisionPage.decisionMaker(),
      `${serviceWorker.firstName} ${serviceWorker.lastName}, head teacher`
    )
  })
})

describe('Citizen assistance preschool decisions', () => {
  test('Decisions are properly listed', async () => {
    const decision = await Fixture.assistanceNeedPreschoolDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .withGuardian(fixtures.enduserGuardianFixture.id)
      .withRequiredFieldsFilled(
        fixtures.daycareFixture.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ACCEPTED',
        sentForDecision: LocalDate.of(2020, 1, 1),
        decisionMade: LocalDate.of(2020, 1, 2),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .withForm({
        type: 'NEW',
        validFrom: LocalDate.of(2020, 1, 3),
        validTo: LocalDate.of(2020, 2, 2)
      })
      .save()

    const decision2 = await Fixture.assistanceNeedPreschoolDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .withGuardian(fixtures.enduserGuardianFixture.id)
      .withRequiredFieldsFilled(
        fixtures.daycareFixture.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ACCEPTED',
        sentForDecision: LocalDate.of(2020, 2, 1),
        decisionMade: LocalDate.of(2020, 2, 2),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .withForm({
        type: 'CONTINUING',
        validFrom: LocalDate.of(2020, 2, 3),
        validTo: LocalDate.of(2020, 4, 2)
      })
      .save()

    const decision3 = await Fixture.assistanceNeedPreschoolDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .withGuardian(fixtures.enduserGuardianFixture.id)
      .withRequiredFieldsFilled(
        fixtures.daycareFixture.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'REJECTED',
        sentForDecision: LocalDate.of(2020, 3, 1),
        decisionMade: LocalDate.of(2020, 3, 2),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .withForm({
        type: 'TERMINATED',
        validFrom: LocalDate.of(2020, 3, 3)
      })
      .save()

    const decision4 = await Fixture.assistanceNeedPreschoolDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .withGuardian(fixtures.enduserGuardianFixture.id)
      .withRequiredFieldsFilled(
        fixtures.daycareFixture.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ACCEPTED',
        sentForDecision: LocalDate.of(2020, 4, 1),
        decisionMade: LocalDate.of(2020, 4, 2),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .withForm({
        type: 'TERMINATED',
        validFrom: LocalDate.of(2020, 4, 3)
      })
      .save()

    const decision5 = await Fixture.assistanceNeedPreschoolDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .withGuardian(fixtures.enduserGuardianFixture.id)
      .withRequiredFieldsFilled(
        fixtures.daycareFixture.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ANNULLED',
        annulmentReason: 'Hyvä syy',
        sentForDecision: LocalDate.of(2020, 5, 1),
        decisionMade: LocalDate.of(2020, 5, 2),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .withForm({
        type: 'TERMINATED',
        validFrom: LocalDate.of(2020, 5, 3)
      })
      .save()

    // should not be shown
    await Fixture.assistanceNeedPreschoolDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .withGuardian(fixtures.enduserGuardianFixture.id)
      .withRequiredFieldsFilled(
        fixtures.daycareFixture.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'DRAFT',
        sentForDecision: LocalDate.of(2020, 1, 10)
      })
      .withForm({
        type: 'TERMINATED',
        validFrom: LocalDate.of(2020, 1, 12)
      })
      .save()

    // should not be shown
    await Fixture.assistanceNeedPreschoolDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .withGuardian(fixtures.enduserGuardianFixture.id)
      .withRequiredFieldsFilled(
        fixtures.daycareFixture.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'NEEDS_WORK',
        sentForDecision: LocalDate.of(2020, 1, 10)
      })
      .withForm({
        type: 'TERMINATED',
        validFrom: LocalDate.of(2020, 1, 12)
      })
      .save()

    const { citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )

    await citizenDecisionsPage.assertChildDecisionCount(
      5,
      fixtures.enduserChildFixtureKaarina.id
    )

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision.id ?? '',
      {
        type: 'Erityinen tuki alkaa',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: `03.01.2020 - 02.02.2020`,
        decisionMade: '02.01.2020',
        status: 'Hyväksytty'
      }
    )

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision2.id ?? '',
      {
        type: 'Erityinen tuki jatkuu',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: `03.02.2020 - 02.04.2020`,
        decisionMade: '02.02.2020',
        status: 'Hyväksytty'
      }
    )

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision3.id ?? '',
      {
        type: 'Erityinen tuki päättyy',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: `03.03.2020 -`,
        decisionMade: '02.03.2020',
        status: 'Hylätty'
      }
    )

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision4.id ?? '',
      {
        type: 'Erityinen tuki päättyy',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: `03.04.2020 -`,
        decisionMade: '02.04.2020',
        status: 'Hyväksytty'
      }
    )

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision5.id ?? '',
      {
        type: 'Erityinen tuki päättyy',
        selectedUnit: fixtures.daycareFixture.name,
        validityPeriod: `03.05.2020 -`,
        decisionMade: '02.05.2020',
        status: 'Mitätöity',
        annulmentReason: 'Hyvä syy'
      }
    )
  })

  test('Preview shows filled information', async () => {
    const decision = await Fixture.assistanceNeedPreschoolDecision()
      .withChild(fixtures.enduserChildFixtureKaarina.id)
      .withGuardian(fixtures.enduserGuardianFixture.id)
      .withRequiredFieldsFilled(
        fixtures.daycareFixture.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ACCEPTED',
        sentForDecision: LocalDate.of(2020, 1, 1),
        decisionMade: LocalDate.of(2020, 1, 2),
        unreadGuardianIds: [fixtures.enduserGuardianFixture.id]
      })
      .withForm({
        type: 'NEW',
        validFrom: LocalDate.of(2020, 1, 3),
        grantedAssistanceService: true,
        grantedServicesBasis: 'Tarvitsee avustajan',
        primaryGroup: 'ryhmä',
        decisionBasis: 'perustelut',
        basisDocumentPedagogicalReport: true,
        basisDocumentsInfo: 'VEO:n arvio',
        guardiansHeardOn: LocalDate.of(2020, 1, 1)
      })
      .save()

    const { page, citizenDecisionsPage } = await openCitizenDecisionsPage(
      fixtures.enduserGuardianFixture
    )
    await citizenDecisionsPage.openAssistanceDecision(
      fixtures.enduserChildFixtureKaarina.id,
      decision.id ?? ''
    )
    await page.page.waitForURL(
      `${config.enduserUrl}/decisions/assistance-preschool/${decision.id ?? ''}`
    )

    const decisionPage = new AssistanceNeedPreschoolDecisionPage(page)
    await decisionPage.status.assertTextEquals('Hyväksytty')
    await decisionPage.type.assertTextEquals('Erityinen tuki alkaa')
    await decisionPage.validFrom.assertTextEquals('03.01.2020')
    await decisionPage.extendedCompulsoryEducation.assertTextEquals('Ei')
    await decisionPage.grantedServices.assertText((s) =>
      s.includes('Lapselle myönnetään avustajapalveluita')
    )
    await decisionPage.grantedServicesBasis.assertText(
      (s) => s.trim() === 'Tarvitsee avustajan'
    )
    await decisionPage.selectedUnit.assertTextEquals(
      fixtures.daycareFixture.name
    )
    await decisionPage.primaryGroup.assertTextEquals('ryhmä')
    await decisionPage.decisionBasis.assertText(
      (s) => s.trim() === 'perustelut'
    )
    await decisionPage.documentBasis.assertText((s) =>
      s.includes('Pedagoginen selvitys')
    )
    await decisionPage.basisDocumentsInfo.assertText(
      (s) => s.trim() === 'VEO:n arvio'
    )
    await decisionPage.guardiansHeardOn.assertTextEquals('01.01.2020')
    await decisionPage.viewOfGuardians.assertText((s) => s.trim() === 'ok')
    await decisionPage.preparer1.assertTextEquals(
      'Paula Palveluohjaaja, Käsittelijä'
    )
    await decisionPage.decisionMaker.assertTextEquals(
      'Paula Palveluohjaaja, Päättäjä'
    )
  })
})
