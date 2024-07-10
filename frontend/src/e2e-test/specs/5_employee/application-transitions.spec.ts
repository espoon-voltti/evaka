// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { execSimpleApplicationActions } from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  applicationFixture,
  testDaycare,
  decisionFixture,
  Fixture,
  testPreschool,
  uuidv4
} from '../../dev-api/fixtures'
import {
  cleanUpMessages,
  createApplications,
  createDecisionPdf,
  createDecisions,
  createDefaultServiceNeedOptions,
  getApplicationDecisions,
  rejectDecisionByCitizen,
  resetServiceState
} from '../../generated/api-clients'
import { DevApplicationWithForm, DevEmployee } from '../../generated/api-types'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedTime = LocalDate.of(2021, 8, 16)
let page: Page
let applicationWorkbench: ApplicationWorkbenchPage
let applicationReadView: ApplicationReadView

let fixtures: AreaAndPersonFixtures
let serviceWorker: DevEmployee
let applicationId: string

beforeEach(async () => {
  await resetServiceState()
  await cleanUpMessages()
  fixtures = await initializeAreaAndPersonData()
  serviceWorker = await Fixture.employeeServiceWorker().save()
  await createDefaultServiceNeedOptions()
  await Fixture.feeThresholds().save()

  page = await Page.open({
    mockedTime: mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  applicationWorkbench = new ApplicationWorkbenchPage(page)
  applicationReadView = new ApplicationReadView(page)
})

describe('Application transitions', () => {
  test('Service worker accepts decision on behalf of the enduser', async () => {
    const fixture = {
      ...applicationFixture(fixtures.testChild, fixtures.testAdult),
      status: 'SENT' as const
    }
    applicationId = fixture.id

    await createApplications({ body: [fixture] })
    await execSimpleApplicationActions(
      applicationId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-decisions-without-proposal'
      ],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await employeeLogin(page, serviceWorker)
    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.acceptDecision('DAYCARE')

    await applicationReadView.waitUntilLoaded()
    await applicationReadView.assertApplicationStatus('Paikka vastaanotettu')
  })

  test('Service worker accepts decision on behalf of the enduser and forwards start date 2 weeks', async () => {
    const fixture = {
      ...applicationFixture(fixtures.testChild, fixtures.testAdult),
      status: 'SENT' as const
    }
    applicationId = fixture.id

    await createApplications({ body: [fixture] })
    await execSimpleApplicationActions(
      applicationId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-decisions-without-proposal'
      ],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await employeeLogin(page, serviceWorker)
    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.setDecisionStartDate(
      'DAYCARE',
      fixture.form.preferences.preferredStartDate?.addWeeks(2).format() ?? ''
    )

    await applicationReadView.acceptDecision('DAYCARE')
    await applicationReadView.waitUntilLoaded()
    await applicationReadView.assertApplicationStatus('Paikka vastaanotettu')
  })

  test('Sending decision sets application to waiting confirmation state', async () => {
    const fixture = {
      ...applicationFixture(fixtures.testChild, fixtures.testAdult),
      status: 'SENT' as const
    }
    applicationId = fixture.id

    await createApplications({ body: [fixture] })
    await execSimpleApplicationActions(
      applicationId,
      ['move-to-waiting-placement', 'create-default-placement-plan'],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openDecisionQueue()
    await applicationWorkbench.sendDecisionsWithoutProposal(applicationId)

    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.waitUntilLoaded()
    await applicationReadView.assertApplicationStatus(
      'Vahvistettavana huoltajalla'
    )
  })

  test('Accepting decision for non vtj guardian sets application to waiting for mailing state', async () => {
    const fixture = {
      ...applicationFixture(
        fixtures.testChild2,
        fixtures.familyWithTwoGuardians.guardian
      ),
      status: 'SENT' as const
    }
    applicationId = fixture.id

    await createApplications({ body: [fixture] })
    await execSimpleApplicationActions(
      applicationId,
      ['move-to-waiting-placement', 'create-default-placement-plan'],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openDecisionQueue()
    await applicationWorkbench.sendDecisionsWithoutProposal(applicationId)

    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.waitUntilLoaded()
    await applicationReadView.assertApplicationStatus('Odottaa postitusta')
  })

  test('Placement dialog works', async () => {
    const preferredStartDate = mockedTime

    const group = await Fixture.daycareGroup()
      .with({ daycareId: fixtures.testDaycare.id })
      .save()
    await Fixture.daycareCaretakers()
      .with({
        groupId: group.id,
        startDate: preferredStartDate,
        amount: 1
      })
      .save()

    const group2 = await Fixture.daycareGroup()
      .with({ daycareId: fixtures.testPreschool.id })
      .save()
    await Fixture.daycareCaretakers()
      .with({
        groupId: group2.id,
        startDate: preferredStartDate,
        amount: 2
      })
      .save()

    // Create existing placements to show meaningful occupancy values
    await Fixture.placement()
      .with({
        unitId: fixtures.testDaycare.id,
        childId: fixtures.testChildRestricted.id,
        startDate: preferredStartDate
      })
      .save()
    await Fixture.placement()
      .with({
        unitId: fixtures.testPreschool.id,
        childId: fixtures.testChild.id,
        startDate: preferredStartDate
      })
      .save()

    const fixture = {
      ...applicationFixture(
        fixtures.testChild2,
        fixtures.familyWithTwoGuardians.guardian,
        undefined,
        'DAYCARE',
        null,
        [testDaycare.id],
        true,
        'SENT',
        preferredStartDate
      ),
      id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
    }
    const applicationId = fixture.id

    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['move-to-waiting-placement'],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementQueue()
    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()

    const planStartDate = preferredStartDate.addDays(1)
    await placementDraftPage.startDate.fill(planStartDate)

    await placementDraftPage.assertOccupancies(fixtures.testDaycare.id, {
      max3Months: '14,3 %',
      max6Months: '14,3 %',
      max3MonthsSpeculated: '28,6 %',
      max6MonthsSpeculated: '28,6 %'
    })

    await placementDraftPage.addOtherUnit(fixtures.testPreschool.name)
    await placementDraftPage.assertOccupancies(fixtures.testPreschool.id, {
      max3Months: '7,1 %',
      max6Months: '7,1 %',
      max3MonthsSpeculated: '14,3 %',
      max6MonthsSpeculated: '14,3 %'
    })

    await placementDraftPage.placeToUnit(fixtures.testPreschool.id)
    await placementDraftPage.submit()

    await applicationWorkbench.waitUntilLoaded()
    await applicationWorkbench.openDecisionQueue()
    await applicationWorkbench.assertApplicationStartDate(0, planStartDate)
  })

  test('Placement dialog shows warning if guardian has restricted details', async () => {
    const restrictedDetailsGuardianApplication = {
      ...applicationFixture(
        fixtures.familyWithRestrictedDetailsGuardian.children[0],
        fixtures.familyWithRestrictedDetailsGuardian.guardian,
        fixtures.familyWithRestrictedDetailsGuardian.otherGuardian,
        'DAYCARE',
        'NOT_AGREED'
      ),
      id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
    }
    const applicationId = restrictedDetailsGuardianApplication.id

    await createApplications({ body: [restrictedDetailsGuardianApplication] })

    await execSimpleApplicationActions(
      applicationId,
      ['move-to-waiting-placement'],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementQueue()

    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()
    await placementDraftPage.assertRestrictedDetailsWarning()
  })

  test('Decision draft page works without unit selection', async () => {
    const fixture = {
      ...applicationFixture(
        fixtures.testChild2,
        fixtures.familyWithTwoGuardians.guardian,
        undefined,
        'PRESCHOOL',
        null,
        [testPreschool.id],
        true,
        'SENT',
        mockedTime
      ),
      id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
    }
    const applicationId = fixture.id
    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['move-to-waiting-placement'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 40))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementQueue()
    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()

    await placementDraftPage.placeToUnit(fixtures.testPreschool.id)
    await placementDraftPage.submit()
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openDecisionQueue()
    const decisionEditorPage =
      await applicationWorkbench.openDecisionEditorById(applicationId)
    await decisionEditorPage.waitUntilLoaded()

    await decisionEditorPage.save()
    await applicationWorkbench.waitUntilLoaded()

    await execSimpleApplicationActions(
      applicationId,
      ['send-decisions-without-proposal'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 41))
    )

    const decisions = await getApplicationDecisions({ applicationId })
    expect(
      decisions
        .map(({ type, unit: { id: unitId } }) => ({ type, unitId }))
        .sort((a, b) => a.type.localeCompare(b.type))
    ).toEqual([
      { type: 'PRESCHOOL', unitId: testPreschool.id },
      { type: 'PRESCHOOL_DAYCARE', unitId: testPreschool.id }
    ])
  })

  test('Decision draft page works with unit selection', async () => {
    const fixture = {
      ...applicationFixture(
        fixtures.testChild2,
        fixtures.familyWithTwoGuardians.guardian,
        undefined,
        'PRESCHOOL',
        null,
        [testPreschool.id],
        true,
        'SENT',
        mockedTime
      ),
      id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
    }
    const applicationId = fixture.id
    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['move-to-waiting-placement'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 40))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementQueue()
    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()

    await placementDraftPage.placeToUnit(fixtures.testPreschool.id)
    await placementDraftPage.submit()
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openDecisionQueue()
    const decisionEditorPage =
      await applicationWorkbench.openDecisionEditorById(applicationId)
    await decisionEditorPage.waitUntilLoaded()

    await decisionEditorPage.selectUnit('PRESCHOOL_DAYCARE', testDaycare.id)
    await decisionEditorPage.save()
    await applicationWorkbench.waitUntilLoaded()

    await execSimpleApplicationActions(
      applicationId,
      ['send-decisions-without-proposal'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 41))
    )

    const decisions = await getApplicationDecisions({ applicationId })
    expect(
      decisions
        .map(({ type, unit: { id: unitId } }) => ({ type, unitId }))
        .sort((a, b) => a.type.localeCompare(b.type))
    ).toStrictEqual([
      { type: 'PRESCHOOL', unitId: testDaycare.id },
      { type: 'PRESCHOOL_DAYCARE', unitId: testDaycare.id }
    ])
  })

  test('Placement proposal flow', async () => {
    const fixture1 = {
      ...applicationFixture(
        fixtures.testChild,
        fixtures.familyWithTwoGuardians.guardian
      ),
      status: 'SENT' as const
    }
    applicationId = fixture1.id

    const applicationId2 = 'dd54782e-231c-4014-abaf-a63eed4e2627'
    const fixture2 = {
      ...applicationFixture(
        fixtures.testChild2,
        fixtures.familyWithSeparatedGuardians.guardian
      ),
      status: 'SENT' as const,
      id: applicationId2
    }

    await createApplications({ body: [fixture1, fixture2] })
    await execSimpleApplicationActions(
      applicationId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-placement-proposal'
      ],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )
    await execSimpleApplicationActions(
      applicationId2,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-placement-proposal'
      ],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    const page2 = await Page.open()
    const unitPage = new UnitPage(page2)

    const unitSupervisor = await Fixture.employeeUnitSupervisor(
      fixtures.testDaycare.id
    ).save()
    await employeeLogin(page2, unitSupervisor)

    // unit supervisor
    await unitPage.navigateToUnit(fixtures.testDaycare.id)
    let placementProposals = (await unitPage.openApplicationProcessTab())
      .placementProposals

    await placementProposals.assertAcceptButtonDisabled()
    await placementProposals.clickProposalAccept(applicationId)
    await placementProposals.assertAcceptButtonEnabled()
    await placementProposals.clickProposalAccept(applicationId2)

    await placementProposals.clickProposalReject(applicationId2)
    await placementProposals.selectProposalRejectionReason(0)
    await placementProposals.submitProposalRejectionReason()

    // service worker
    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementProposalQueue()
    await applicationWorkbench.withdrawPlacementProposal(applicationId2)
    await applicationWorkbench.assertWithdrawPlacementProposalsButtonDisabled()

    // unit supervisor
    await unitPage.navigateToUnit(fixtures.testDaycare.id)
    const applicationProcessPage = await unitPage.openApplicationProcessTab()
    placementProposals = applicationProcessPage.placementProposals
    await placementProposals.assertAcceptButtonEnabled()
    await placementProposals.clickAcceptButton()
    await placementProposals.assertPlacementProposalRowCount(0)
    await applicationProcessPage.waitUntilLoaded()

    await execSimpleApplicationActions(
      applicationId,
      ['confirm-decision-mailed'],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await unitPage.navigateToUnit(fixtures.testDaycare.id)
    const waitingConfirmation = (await unitPage.openApplicationProcessTab())
      .waitingConfirmation
    await waitingConfirmation.assertRowCount(1)
  })

  test('Placement proposal rejection status', async () => {
    const fixture1 = {
      ...applicationFixture(
        fixtures.testChild,
        fixtures.familyWithTwoGuardians.guardian
      ),
      status: 'SENT' as const
    }
    applicationId = fixture1.id

    await createApplications({ body: [fixture1] })

    const now = mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    await execSimpleApplicationActions(
      applicationId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-placement-proposal'
      ],
      now
    )

    const page2 = await Page.open()
    const unitPage = new UnitPage(page2)

    await employeeLogin(
      page2,
      await Fixture.employeeUnitSupervisor(fixtures.testDaycare.id).save()
    )

    // unit supervisor
    await unitPage.navigateToUnit(fixtures.testDaycare.id)
    const placementProposals = (await unitPage.openApplicationProcessTab())
      .placementProposals

    await placementProposals.clickProposalReject(applicationId)
    await placementProposals.selectProposalRejectionReason(0)
    await placementProposals.submitProposalRejectionReason()
    await placementProposals.clickAcceptButton()

    // service worker
    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()
    await applicationWorkbench.openPlacementProposalQueue()

    await applicationWorkbench.assertApplicationStatusTextMatches(
      0,
      'TILARAJOITE'
    )
  })

  test('Decision cannot be accepted on behalf of guardian if application is in placement proposal state', async () => {
    const fixture1 = {
      ...applicationFixture(
        fixtures.testChild,
        fixtures.familyWithTwoGuardians.guardian
      ),
      status: 'SENT' as const
    }
    applicationId = fixture1.id

    await createApplications({ body: [fixture1] })
    await execSimpleApplicationActions(
      applicationId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-placement-proposal'
      ],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    const unitSupervisor = await Fixture.employeeUnitSupervisor(
      fixtures.testDaycare.id
    ).save()
    await employeeLogin(page, unitSupervisor)
    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.waitUntilLoaded()
    await applicationReadView.assertDecisionDisabled('DAYCARE')
  })

  test('Supervisor can download decision PDF only after it has been generated', async () => {
    const application = {
      ...applicationFixture(fixtures.testChild, fixtures.testAdult),
      status: 'SENT' as const
    }
    applicationId = application.id

    await createApplications({ body: [application] })

    const decision = decisionFixture(
      applicationId,
      application.form.preferences.preferredStartDate ??
        LocalDate.todayInSystemTz(),
      application.form.preferences.preferredStartDate ??
        LocalDate.todayInSystemTz()
    )
    const decisionId = decision.id

    // NOTE: This will NOT generate a PDF, just create the decision
    await createDecisions({
      body: [
        {
          ...decision,
          employeeId: serviceWorker.id
        }
      ]
    })
    await employeeLogin(page, serviceWorker)

    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.waitUntilLoaded()
    await applicationReadView.assertDecisionDownloadPending(decision.type)

    // NOTE: No need to wait for pending async jobs as this is synchronous (unlike the normal flow of users creating
    // decisions that would trigger PDF generation as an async job).
    await createDecisionPdf({ id: decisionId })

    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.waitUntilLoaded()
    await applicationReadView.assertDecisionAvailableForDownload(decision.type)
  })

  test('Application rejected by citizen is shown for 2 weeks', async () => {
    const application1: DevApplicationWithForm = {
      ...applicationFixture(fixtures.testChild, fixtures.testAdult),
      id: uuidv4(),
      status: 'WAITING_CONFIRMATION'
    }
    const application2: DevApplicationWithForm = {
      ...applicationFixture(fixtures.testChild2, fixtures.testAdult),
      id: uuidv4(),
      status: 'WAITING_CONFIRMATION'
    }
    const placementStartDate = LocalDate.of(2021, 8, 16)

    await createApplications({ body: [application1, application2] })

    await Fixture.placementPlan()
      .with({
        applicationId: application1.id,
        unitId: fixtures.testDaycare.id,
        periodStart: placementStartDate,
        periodEnd: placementStartDate
      })
      .save()

    await Fixture.placementPlan()
      .with({
        applicationId: application2.id,
        unitId: fixtures.testDaycare.id,
        periodStart: placementStartDate,
        periodEnd: placementStartDate
      })
      .save()

    const decisionId = (
      await Fixture.decision()
        .with({
          applicationId: application2.id,
          employeeId: serviceWorker.id,
          unitId: fixtures.testDaycare.id,
          startDate: placementStartDate,
          endDate: placementStartDate
        })
        .save()
    ).id

    await rejectDecisionByCitizen({ id: decisionId })

    const unitSupervisor = await Fixture.employeeUnitSupervisor(
      fixtures.testDaycare.id
    ).save()

    async function assertApplicationRows(
      addDays: number,
      expectRejectedApplicationToBeVisible: boolean
    ) {
      const page = await Page.open({
        mockedTime:
          addDays !== 0
            ? LocalDate.todayInSystemTz()
                .addDays(addDays)
                .toHelsinkiDateTime(LocalTime.of(12, 0))
            : undefined
      })

      await employeeLogin(page, unitSupervisor)
      const unitPage = new UnitPage(page)
      await unitPage.navigateToUnit(fixtures.testDaycare.id)
      const waitingConfirmation = (await unitPage.openApplicationProcessTab())
        .waitingConfirmation

      await waitingConfirmation.assertNotificationCounter(1)
      if (expectRejectedApplicationToBeVisible) {
        await waitingConfirmation.assertRowCount(2)
        await waitingConfirmation.assertRejectedRowCount(1)
        await waitingConfirmation.assertRow(application1.id, false)
        await waitingConfirmation.assertRow(application2.id, true)
      } else {
        await waitingConfirmation.assertRowCount(1)
        await waitingConfirmation.assertRejectedRowCount(0)
        await waitingConfirmation.assertRow(application1.id, false)
      }
      await page.close()
    }

    await assertApplicationRows(14, true)
    await assertApplicationRows(15, false)
  })
})
