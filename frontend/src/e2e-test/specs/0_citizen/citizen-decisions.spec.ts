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
  applicationFixture,
  Fixture,
  preschoolTerm2022,
  testAdult,
  testCareArea,
  testChild,
  testChild2,
  testChildRestricted,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createApplications,
  getApplicationDecisions,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee, DevPerson } from '../../generated/api-types'
import AssistanceNeedDecisionPage from '../../pages/citizen/citizen-assistance-need-decision'
import AssistanceNeedPreschoolDecisionPage from '../../pages/citizen/citizen-assistance-need-preschool-decision'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let decisionMaker: DevEmployee
const now = HelsinkiDateTime.of(2023, 3, 15, 12, 0)

beforeEach(async () => {
  await resetServiceState()
  await Fixture.preschoolTerm(preschoolTerm2022).save()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2, testChildRestricted]
  }).save()
  decisionMaker = await Fixture.employee({
    firstName: 'Paula',
    lastName: 'Palveluohjaaja'
  })
    .serviceWorker()
    .save()
})

async function openCitizenDecisionsPage(citizen: DevPerson) {
  const page = await Page.open({
    mockedTime: now
  })
  await enduserLogin(page, citizen)
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
      testChild,
      testAdult,
      undefined,
      'PRESCHOOL',
      null,
      [testDaycare.id],
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

    const { citizenDecisionsPage } = await openCitizenDecisionsPage(testAdult)

    await citizenDecisionsPage.assertUnresolvedDecisionsCount(2)
    await citizenDecisionsPage.assertApplicationDecision(
      testChild.id,
      preschoolDecisionId,
      `Päätös esiopetuksesta ${now.toLocalDate().format()}`,
      now.toLocalDate().format(),
      'Vahvistettavana huoltajalla'
    )
    await citizenDecisionsPage.assertApplicationDecision(
      testChild.id,
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
      testDaycare.decisionCustomization.preschoolName,
      'Vahvistettavana huoltajalla'
    )

    await responsePage.acceptDecision(preschoolDecisionId)
    await responsePage.assertDecisionStatus(preschoolDecisionId, 'Hyväksytty')
    await responsePage.assertUnresolvedDecisionsCount(1)

    await responsePage.assertDecisionData(
      preschoolDaycareDecisionId,
      'Päätös liittyvästä varhaiskasvatuksesta',
      testDaycare.decisionCustomization.daycareName,
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
      testChild,
      testAdult,
      undefined,
      'PRESCHOOL',
      null,
      [testDaycare.id],
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

    const { citizenDecisionsPage } = await openCitizenDecisionsPage(testAdult)

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
    const child = await Fixture.person({ ssn: '010116A9219' }).saveChild({
      updateMockVtj: true
    })
    const guardian = await Fixture.person({ ssn: '010106A973C' }).saveAdult({
      updateMockVtjWithDependants: [child]
    })
    const otherGuardian = await Fixture.person({
      ssn: '010106A9388'
    }).saveAdult({ updateMockVtjWithDependants: [child] })

    const application = applicationFixture(
      child,
      guardian,
      otherGuardian,
      'PRESCHOOL',
      null,
      [testDaycare.id],
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
    const decision = await Fixture.preFilledAssistanceNeedDecision({
      childId: testChild2.id,
      selectedUnit: testDaycare.id,
      status: 'ACCEPTED',
      assistanceLevels: ['ASSISTANCE_SERVICES_FOR_TIME', 'ENHANCED_ASSISTANCE'],
      validityPeriod: new DateRange(
        LocalDate.of(2020, 2, 5),
        LocalDate.of(2021, 5, 11)
      ),
      decisionMade: LocalDate.of(2020, 1, 17)
    }).save()
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(testAdult)

    await citizenDecisionsPage.assertAssistanceDecision(
      testChild2.id,
      decision.id ?? '',
      {
        assistanceLevel:
          'Tukipalvelut päätöksen voimassaolon aikana, tehostettu tuki',
        selectedUnit: testDaycare.name,
        validityPeriod: '05.02.2020 - 11.05.2021',
        decisionMade: '17.01.2020',
        status: 'Hyväksytty'
      }
    )
  })

  test('Rejected decision', async () => {
    const decision = await Fixture.preFilledAssistanceNeedDecision({
      childId: testChild2.id,
      selectedUnit: testDaycare.id,
      status: 'REJECTED',
      assistanceLevels: ['ENHANCED_ASSISTANCE'],
      validityPeriod: new DateRange(LocalDate.of(2022, 2, 10), null),
      decisionMade: LocalDate.of(2021, 1, 17)
    }).save()
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(testAdult)

    await citizenDecisionsPage.assertAssistanceDecision(
      testChild2.id,
      decision.id ?? '',
      {
        assistanceLevel: 'Tehostettu tuki',
        selectedUnit: testDaycare.name,
        validityPeriod: '10.02.2022 -',
        decisionMade: '17.01.2021',
        status: 'Hylätty'
      }
    )
  })

  test('Annulled decision', async () => {
    const decision = await Fixture.preFilledAssistanceNeedDecision({
      childId: testChild2.id,
      selectedUnit: testDaycare.id,
      status: 'ANNULLED',
      annulmentReason: 'Well because',
      assistanceLevels: ['ENHANCED_ASSISTANCE'],
      validityPeriod: new DateRange(LocalDate.of(2022, 2, 10), null),
      decisionMade: LocalDate.of(2021, 1, 17)
    }).save()
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(testAdult)

    await citizenDecisionsPage.assertAssistanceDecision(
      testChild2.id,
      decision.id ?? '',
      {
        assistanceLevel: 'Tehostettu tuki',
        selectedUnit: testDaycare.name,
        validityPeriod: '10.02.2022 -',
        decisionMade: '17.01.2021',
        status: 'Mitätöity'
      }
    )
  })

  test('Drafts or decisions that need work are not shown', async () => {
    await Fixture.preFilledAssistanceNeedDecision({
      childId: testChild2.id,
      selectedUnit: testDaycare.id,
      status: 'NEEDS_WORK',
      assistanceLevels: ['ENHANCED_ASSISTANCE'],
      decisionMade: LocalDate.of(2021, 1, 17)
    }).save()

    await Fixture.preFilledAssistanceNeedDecision({
      childId: testChild2.id,
      selectedUnit: testDaycare.id,
      status: 'DRAFT',
      assistanceLevels: ['ENHANCED_ASSISTANCE'],
      decisionMade: LocalDate.of(2021, 1, 17)
    }).save()
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(testAdult)

    await citizenDecisionsPage.assertNoChildDecisions(testChild2.id)
  })
  test('Unread assistance decisions are indicated', async () => {
    await Fixture.preFilledAssistanceNeedDecision({
      childId: testChild2.id,
      selectedUnit: testDaycare.id,
      status: 'ACCEPTED',
      assistanceLevels: ['SPECIAL_ASSISTANCE'],
      decisionMade: LocalDate.of(2020, 1, 17),
      unreadGuardianIds: [testAdult.id]
    }).save()

    await Fixture.preFilledAssistanceNeedDecision({
      childId: testChild2.id,
      selectedUnit: testDaycare.id,
      status: 'REJECTED',
      assistanceLevels: ['SPECIAL_ASSISTANCE'],
      decisionMade: LocalDate.of(2018, 1, 17),
      unreadGuardianIds: [testAdult.id]
    }).save()

    await Fixture.preFilledAssistanceNeedDecision({
      childId: testChildRestricted.id,
      selectedUnit: testDaycare.id,
      status: 'ACCEPTED',
      assistanceLevels: ['SPECIAL_ASSISTANCE'],
      decisionMade: LocalDate.of(2020, 1, 17),
      unreadGuardianIds: [testAdult.id]
    }).save()
    const { citizenDecisionsPage } = await openCitizenDecisionsPage(testAdult)

    await citizenDecisionsPage.assertUnreadAssistanceNeedDecisions(
      testChild2.id,
      2
    )
    await citizenDecisionsPage.assertUnreadAssistanceNeedDecisions(
      testChildRestricted.id,
      1
    )
  })

  test('Preview shows filled information', async () => {
    const serviceWorker = await Fixture.employee().serviceWorker().save()
    const decision = await Fixture.preFilledAssistanceNeedDecision({
      childId: testChild2.id,
      selectedUnit: testDaycare.id,
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
    }).save()

    const { page, citizenDecisionsPage } =
      await openCitizenDecisionsPage(testAdult)
    await citizenDecisionsPage.openAssistanceDecision(
      testChild2.id,
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
      `${testDaycare.name}\n${testDaycare.visitingAddress.streetAddress}\n${testDaycare.visitingAddress.postalCode} ${testDaycare.visitingAddress.postOffice}\nLoma-aikoina tuen järjestämispaikka ja -tapa saattavat muuttua.`
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
    const decision = await Fixture.assistanceNeedPreschoolDecision({
      childId: testChild2.id
    })
      .withGuardian(testAdult.id)
      .withRequiredFieldsFilled(
        testDaycare.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ACCEPTED',
        sentForDecision: LocalDate.of(2020, 1, 1),
        decisionMade: LocalDate.of(2020, 1, 2),
        unreadGuardianIds: [testAdult.id]
      })
      .withForm({
        type: 'NEW',
        validFrom: LocalDate.of(2020, 1, 3),
        validTo: LocalDate.of(2020, 2, 2)
      })
      .save()

    const decision2 = await Fixture.assistanceNeedPreschoolDecision({
      childId: testChild2.id
    })
      .withGuardian(testAdult.id)
      .withRequiredFieldsFilled(
        testDaycare.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ACCEPTED',
        sentForDecision: LocalDate.of(2020, 2, 1),
        decisionMade: LocalDate.of(2020, 2, 2),
        unreadGuardianIds: [testAdult.id]
      })
      .withForm({
        type: 'CONTINUING',
        validFrom: LocalDate.of(2020, 2, 3),
        validTo: LocalDate.of(2020, 4, 2)
      })
      .save()

    const decision3 = await Fixture.assistanceNeedPreschoolDecision({
      childId: testChild2.id
    })
      .withGuardian(testAdult.id)
      .withRequiredFieldsFilled(
        testDaycare.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'REJECTED',
        sentForDecision: LocalDate.of(2020, 3, 1),
        decisionMade: LocalDate.of(2020, 3, 2),
        unreadGuardianIds: [testAdult.id]
      })
      .withForm({
        type: 'TERMINATED',
        validFrom: LocalDate.of(2020, 3, 3)
      })
      .save()

    const decision4 = await Fixture.assistanceNeedPreschoolDecision({
      childId: testChild2.id
    })
      .withGuardian(testAdult.id)
      .withRequiredFieldsFilled(
        testDaycare.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ACCEPTED',
        sentForDecision: LocalDate.of(2020, 4, 1),
        decisionMade: LocalDate.of(2020, 4, 2),
        unreadGuardianIds: [testAdult.id]
      })
      .withForm({
        type: 'TERMINATED',
        validFrom: LocalDate.of(2020, 4, 3)
      })
      .save()

    const decision5 = await Fixture.assistanceNeedPreschoolDecision({
      childId: testChild2.id
    })
      .withGuardian(testAdult.id)
      .withRequiredFieldsFilled(
        testDaycare.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ANNULLED',
        annulmentReason: 'Hyvä syy',
        sentForDecision: LocalDate.of(2020, 5, 1),
        decisionMade: LocalDate.of(2020, 5, 2),
        unreadGuardianIds: [testAdult.id]
      })
      .withForm({
        type: 'TERMINATED',
        validFrom: LocalDate.of(2020, 5, 3)
      })
      .save()

    // should not be shown
    await Fixture.assistanceNeedPreschoolDecision({ childId: testChild2.id })
      .withGuardian(testAdult.id)
      .withRequiredFieldsFilled(
        testDaycare.id,
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
    await Fixture.assistanceNeedPreschoolDecision({ childId: testChild2.id })
      .withGuardian(testAdult.id)
      .withRequiredFieldsFilled(
        testDaycare.id,
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

    const { citizenDecisionsPage } = await openCitizenDecisionsPage(testAdult)

    await citizenDecisionsPage.assertChildDecisionCount(5, testChild2.id)

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      testChild2.id,
      decision.id ?? '',
      {
        type: 'Erityinen tuki alkaa',
        selectedUnit: testDaycare.name,
        validityPeriod: `03.01.2020 - 02.02.2020`,
        decisionMade: '02.01.2020',
        status: 'Hyväksytty'
      }
    )

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      testChild2.id,
      decision2.id ?? '',
      {
        type: 'Erityinen tuki jatkuu',
        selectedUnit: testDaycare.name,
        validityPeriod: `03.02.2020 - 02.04.2020`,
        decisionMade: '02.02.2020',
        status: 'Hyväksytty'
      }
    )

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      testChild2.id,
      decision3.id ?? '',
      {
        type: 'Erityinen tuki päättyy',
        selectedUnit: testDaycare.name,
        validityPeriod: `03.03.2020 -`,
        decisionMade: '02.03.2020',
        status: 'Hylätty'
      }
    )

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      testChild2.id,
      decision4.id ?? '',
      {
        type: 'Erityinen tuki päättyy',
        selectedUnit: testDaycare.name,
        validityPeriod: `03.04.2020 -`,
        decisionMade: '02.04.2020',
        status: 'Hyväksytty'
      }
    )

    await citizenDecisionsPage.assertAssistancePreschoolDecision(
      testChild2.id,
      decision5.id ?? '',
      {
        type: 'Erityinen tuki päättyy',
        selectedUnit: testDaycare.name,
        validityPeriod: `03.05.2020 -`,
        decisionMade: '02.05.2020',
        status: 'Mitätöity',
        annulmentReason: 'Hyvä syy'
      }
    )
  })

  test('Preview shows filled information', async () => {
    const decision = await Fixture.assistanceNeedPreschoolDecision({
      childId: testChild2.id
    })
      .withGuardian(testAdult.id)
      .withRequiredFieldsFilled(
        testDaycare.id,
        decisionMaker.id,
        decisionMaker.id
      )
      .with({
        status: 'ACCEPTED',
        sentForDecision: LocalDate.of(2020, 1, 1),
        decisionMade: LocalDate.of(2020, 1, 2),
        unreadGuardianIds: [testAdult.id]
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

    const { page, citizenDecisionsPage } =
      await openCitizenDecisionsPage(testAdult)
    await citizenDecisionsPage.openAssistanceDecision(
      testChild2.id,
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
    await decisionPage.selectedUnit.assertTextEquals(testDaycare.name)
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
