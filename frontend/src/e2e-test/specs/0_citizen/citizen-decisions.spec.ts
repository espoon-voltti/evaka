// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

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
import type { DevPerson } from '../../generated/api-types'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const now = HelsinkiDateTime.of(2023, 3, 15, 12, 0)

beforeEach(async () => {
  await resetServiceState()
  await preschoolTerm2022.save()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2, testChildRestricted]
  }).save()
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
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_DECISIONS_WITHOUT_PROPOSAL'
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
      'Esiopetus',
      now.toLocalDate().format(),
      'Odottaa vahvistusta'
    )
    await citizenDecisionsPage.assertApplicationDecision(
      testChild.id,
      preschoolDaycareDecisionId,
      'Liittyvä varhaiskasvatus',
      now.toLocalDate().format(),
      'Odottaa vahvistusta'
    )

    const responsePage = await citizenDecisionsPage.navigateToDecisionResponse(
      applicationId,
      2
    )

    // preschool daycare decision cannot be accepted before accepting preschool
    await responsePage.assertDecisionCannotBeAccepted(
      preschoolDaycareDecisionId
    )

    await responsePage.assertDecisionData(
      preschoolDecisionId,
      'Esiopetus',
      testDaycare.decisionCustomization.preschoolName,
      'Odottaa vahvistusta'
    )

    await responsePage.acceptDecision(preschoolDecisionId)
    await responsePage.assertDecisionStatus(preschoolDecisionId, 'Hyväksytty')
    await responsePage.assertPageTitle(1)

    await responsePage.assertDecisionData(
      preschoolDaycareDecisionId,
      'Liittyvä varhaiskasvatus',
      testDaycare.decisionCustomization.daycareName,
      'Odottaa vahvistusta'
    )

    await responsePage.rejectDecision(preschoolDaycareDecisionId)
    await responsePage.assertDecisionStatus(preschoolDecisionId, 'Hyväksytty')
    await responsePage.assertDecisionStatus(
      preschoolDaycareDecisionId,
      'Hylätty'
    )
    await responsePage.assertPageTitle(0)

    // Reload page - decisions should now be gone from response page
    await responsePage.reload()
    await responsePage.assertPageTitle(0)
    await responsePage.assertNoDecisionsVisible()
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
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_DECISIONS_WITHOUT_PROPOSAL'
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
    const responsePage = await citizenDecisionsPage.navigateToDecisionResponse(
      applicationId,
      2
    )
    await responsePage.rejectDecision(preschoolDecisionId)
    await responsePage.confirmRejectCascade()

    await responsePage.assertDecisionStatus(preschoolDecisionId, 'Hylätty')
    await responsePage.assertDecisionStatus(
      preschoolDaycareDecisionId,
      'Hylätty'
    )
    await responsePage.assertPageTitle(0)
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
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_DECISIONS_WITHOUT_PROPOSAL'
      ],
      now
    )

    const { citizenDecisionsPage } =
      await openCitizenDecisionsPage(otherGuardian)
    await citizenDecisionsPage.assertChildDecisionCount(2, child.id)
    // other guardian can only see the decisions but not resolve them -> not shown in counts
    await citizenDecisionsPage.assertUnresolvedDecisionsCount(0)
  })

  test('Citizen can open and view decision metadata', async () => {
    const application = applicationFixture(
      testChild,
      testAdult,
      undefined,
      'DAYCARE',
      null,
      [testDaycare.id],
      true,
      'SENT'
    )
    await createApplications({ body: [application] })
    await execSimpleApplicationActions(
      application.id,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_DECISIONS_WITHOUT_PROPOSAL'
      ],
      now
    )
    const decisions = await getApplicationDecisions({
      applicationId: application.id
    })
    if (decisions.length !== 1) throw Error('Expected 1 decision')

    const { citizenDecisionsPage } = await openCitizenDecisionsPage(testAdult)

    await citizenDecisionsPage.viewDecisionMetadata(decisions[0].id)
  })
})
