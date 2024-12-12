// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid, randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { execSimpleApplicationActions } from '../../dev-api'
import {
  applicationFixture,
  testDaycare,
  decisionFixture,
  Fixture,
  testPreschool,
  familyWithRestrictedDetailsGuardian,
  familyWithSeparatedGuardians,
  familyWithTwoGuardians,
  testAdult,
  testChild,
  testChildRestricted,
  testChild2,
  testCareArea,
  preschoolTerm2021
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

let serviceWorker: DevEmployee
let applicationId: ApplicationId

beforeEach(async () => {
  await resetServiceState()
  await cleanUpMessages()
  await Fixture.preschoolTerm(preschoolTerm2021).save()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.daycare(testPreschool).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2, testChildRestricted]
  }).save()
  await Fixture.family(familyWithTwoGuardians).save()
  await Fixture.family(familyWithSeparatedGuardians).save()
  await Fixture.family(familyWithRestrictedDetailsGuardian).save()
  serviceWorker = await Fixture.employee().serviceWorker().save()
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
      ...applicationFixture(testChild, testAdult),
      status: 'SENT' as const
    }
    applicationId = fixture.id

    await createApplications({ body: [fixture] })
    await execSimpleApplicationActions(
      applicationId,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_DECISIONS_WITHOUT_PROPOSAL'
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
      ...applicationFixture(testChild, testAdult),
      status: 'SENT' as const
    }
    applicationId = fixture.id

    await createApplications({ body: [fixture] })
    await execSimpleApplicationActions(
      applicationId,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_DECISIONS_WITHOUT_PROPOSAL'
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
      ...applicationFixture(testChild, testAdult),
      status: 'SENT' as const
    }
    applicationId = fixture.id

    await createApplications({ body: [fixture] })
    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT', 'CREATE_DEFAULT_PLACEMENT_PLAN'],
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
      ...applicationFixture(testChild2, familyWithTwoGuardians.guardian),
      status: 'SENT' as const
    }
    applicationId = fixture.id

    await createApplications({ body: [fixture] })
    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT', 'CREATE_DEFAULT_PLACEMENT_PLAN'],
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

  test('Application with e.g. diet must be checked before placing', async () => {
    const preferredStartDate = mockedTime
    const fixture: DevApplicationWithForm = {
      ...applicationFixture(
        testChild2,
        familyWithTwoGuardians.guardian,
        undefined,
        'DAYCARE',
        null,
        [testDaycare.id],
        true,
        'SENT',
        preferredStartDate
      )
    }
    fixture.form.child.diet = 'Vegaani'

    const applicationId = fixture.id

    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT'],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()
    await applicationWorkbench.openPlacementQueue()

    await applicationWorkbench
      .getPrimaryActionCheck(applicationId)
      .waitUntilVisible()
    await applicationWorkbench
      .getPrimaryActionCreatePlacementPlan(applicationId)
      .waitUntilHidden()

    await applicationWorkbench.getPrimaryActionCheck(applicationId).click()

    const applicationReadView = new ApplicationReadView(page)
    await applicationReadView.setVerifiedButton.waitUntilVisible()
    // confidentiality has been set automatically
    await applicationReadView.confidentialRadioYes.waitUntilHidden()
    await applicationReadView.confidentialRadioNo.waitUntilHidden()

    await applicationReadView.setVerifiedButton.click()
    await page.goBack()

    await applicationWorkbench
      .getPrimaryActionCreatePlacementPlan(applicationId)
      .waitUntilVisible()
    await applicationWorkbench
      .getPrimaryActionCheck(applicationId)
      .waitUntilHidden()

    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()
  })

  test('Confidentiality must be set on an application before placing if other info is the only potential source of confidentiality', async () => {
    const preferredStartDate = mockedTime
    const fixture: DevApplicationWithForm = {
      ...applicationFixture(
        testChild2,
        familyWithTwoGuardians.guardian,
        undefined,
        'DAYCARE',
        null,
        [testDaycare.id],
        true,
        'SENT',
        preferredStartDate
      )
    }
    fixture.form.otherInfo = 'Eipä ihmeempiä'

    const applicationId = fixture.id

    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT'],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()
    await applicationWorkbench.openPlacementQueue()

    await applicationWorkbench
      .getPrimaryActionCheck(applicationId)
      .waitUntilVisible()
    await applicationWorkbench
      .getPrimaryActionCreatePlacementPlan(applicationId)
      .waitUntilHidden()

    await applicationWorkbench.getPrimaryActionCheck(applicationId).click()

    const applicationReadView = new ApplicationReadView(page)
    await applicationReadView.setVerifiedButton.waitUntilVisible()
    await applicationReadView.setVerifiedButton.assertDisabled(true)

    await applicationReadView.confidentialRadioYes.check()
    await applicationReadView.confidentialRadioNo.check()

    await applicationReadView.setVerifiedButton.assertDisabled(false)
    await applicationReadView.setVerifiedButton.click()
    await page.goBack()

    await applicationWorkbench
      .getPrimaryActionCreatePlacementPlan(applicationId)
      .waitUntilVisible()
    await applicationWorkbench
      .getPrimaryActionCheck(applicationId)
      .waitUntilHidden()

    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()
  })

  test('Confidentiality must be set on an application before cancelling if other info is the only potential source of confidentiality', async () => {
    const preferredStartDate = mockedTime
    const fixture: DevApplicationWithForm = {
      ...applicationFixture(
        testChild2,
        familyWithTwoGuardians.guardian,
        undefined,
        'DAYCARE',
        null,
        [testDaycare.id],
        true,
        'SENT',
        preferredStartDate
      )
    }
    fixture.form.otherInfo = 'Eipä ihmeempiä'

    const applicationId = fixture.id

    await createApplications({ body: [fixture] })

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    const applicationList = new ApplicationListView(page)
    await applicationList.actionsMenu(applicationId).click()
    await applicationList.actionsMenuItems.cancelApplication.click()

    await applicationList.cancelConfirmation.submitButton.assertDisabled(true)
    await applicationList.cancelConfirmation.confidentialRadioYes.check()
    await applicationList.cancelConfirmation.submitButton.click()

    await applicationWorkbench.applicationsAll.click()
    await applicationWorkbench
      .getApplicationListItem(applicationId)
      .assertText((text) => text.includes('Poistettu käsittelystä'))
  })

  test('Placement dialog works', async () => {
    const preferredStartDate = mockedTime

    const group = await Fixture.daycareGroup({
      daycareId: testDaycare.id
    }).save()
    await Fixture.daycareCaretakers({
      groupId: group.id,
      startDate: preferredStartDate,
      amount: 1
    }).save()

    const group2 = await Fixture.daycareGroup({
      daycareId: testPreschool.id
    }).save()
    await Fixture.daycareCaretakers({
      groupId: group2.id,
      startDate: preferredStartDate,
      amount: 2
    }).save()

    // Create existing placements to show meaningful occupancy values
    await Fixture.placement({
      unitId: testDaycare.id,
      childId: testChildRestricted.id,
      startDate: preferredStartDate
    }).save()
    await Fixture.placement({
      unitId: testPreschool.id,
      childId: testChild.id,
      startDate: preferredStartDate
    }).save()

    const fixture = {
      ...applicationFixture(
        testChild2,
        familyWithTwoGuardians.guardian,
        undefined,
        'DAYCARE',
        null,
        [testDaycare.id],
        true,
        'SENT',
        preferredStartDate
      ),
      id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
    }
    const applicationId = fixture.id

    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT'],
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

    await placementDraftPage.assertOccupancies(testDaycare.id, {
      max3Months: '14,3 %',
      max6Months: '14,3 %',
      max3MonthsSpeculated: '28,6 %',
      max6MonthsSpeculated: '28,6 %'
    })

    await placementDraftPage.addOtherUnit(testPreschool.name)
    await placementDraftPage.assertOccupancies(testPreschool.id, {
      max3Months: '7,1 %',
      max6Months: '7,1 %',
      max3MonthsSpeculated: '14,3 %',
      max6MonthsSpeculated: '14,3 %'
    })

    await placementDraftPage.placeToUnit(testPreschool.id)
    await placementDraftPage.submit()

    await applicationWorkbench.waitUntilLoaded()
    await applicationWorkbench.openDecisionQueue()
    await applicationWorkbench.assertApplicationStartDate(0, planStartDate)
  })

  test('Placement dialog shows warning if guardian has restricted details', async () => {
    const restrictedDetailsGuardianApplication = {
      ...applicationFixture(
        familyWithRestrictedDetailsGuardian.children[0],
        familyWithRestrictedDetailsGuardian.guardian,
        familyWithRestrictedDetailsGuardian.otherGuardian,
        'DAYCARE',
        'NOT_AGREED'
      ),
      id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
    }
    const applicationId = restrictedDetailsGuardianApplication.id

    await createApplications({ body: [restrictedDetailsGuardianApplication] })

    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT'],
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
        testChild2,
        familyWithTwoGuardians.guardian,
        undefined,
        'PRESCHOOL',
        null,
        [testPreschool.id],
        true,
        'SENT',
        mockedTime
      ),
      id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
    }
    const applicationId = fixture.id
    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 40))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementQueue()
    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()

    await placementDraftPage.placeToUnit(testPreschool.id)
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
      ['SEND_DECISIONS_WITHOUT_PROPOSAL'],
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
        testChild2,
        familyWithTwoGuardians.guardian,
        undefined,
        'PRESCHOOL',
        null,
        [testPreschool.id],
        true,
        'SENT',
        mockedTime
      ),
      id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
    }
    const applicationId = fixture.id
    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 40))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementQueue()
    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()

    await placementDraftPage.placeToUnit(testPreschool.id)
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
      ['SEND_DECISIONS_WITHOUT_PROPOSAL'],
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
      ...applicationFixture(testChild, familyWithTwoGuardians.guardian),
      status: 'SENT' as const
    }
    applicationId = fixture1.id

    const applicationId2 = fromUuid<ApplicationId>(
      'dd54782e-231c-4014-abaf-a63eed4e2627'
    )
    const fixture2 = {
      ...applicationFixture(testChild2, familyWithSeparatedGuardians.guardian),
      status: 'SENT' as const,
      id: applicationId2
    }

    await createApplications({ body: [fixture1, fixture2] })
    await execSimpleApplicationActions(
      applicationId,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_PLACEMENT_PROPOSAL'
      ],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )
    await execSimpleApplicationActions(
      applicationId2,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_PLACEMENT_PROPOSAL'
      ],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    const page2 = await Page.open()
    const unitPage = new UnitPage(page2)

    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()
    await employeeLogin(page2, unitSupervisor)

    // unit supervisor
    await unitPage.navigateToUnit(testDaycare.id)
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
    await unitPage.navigateToUnit(testDaycare.id)
    const applicationProcessPage = await unitPage.openApplicationProcessTab()
    placementProposals = applicationProcessPage.placementProposals
    await placementProposals.assertAcceptButtonEnabled()
    await placementProposals.clickAcceptButton()
    await placementProposals.assertPlacementProposalRowCount(0)
    await applicationProcessPage.waitUntilLoaded()

    await execSimpleApplicationActions(
      applicationId,
      ['CONFIRM_DECISION_MAILED'],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    await unitPage.navigateToUnit(testDaycare.id)
    const waitingConfirmation = (await unitPage.openApplicationProcessTab())
      .waitingConfirmation
    await waitingConfirmation.assertRowCount(1)
  })

  test('Placement proposal rejection status', async () => {
    const fixture1 = {
      ...applicationFixture(testChild, familyWithTwoGuardians.guardian),
      status: 'SENT' as const
    }
    applicationId = fixture1.id

    await createApplications({ body: [fixture1] })

    const now = mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    await execSimpleApplicationActions(
      applicationId,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_PLACEMENT_PROPOSAL'
      ],
      now
    )

    const page2 = await Page.open()
    const unitPage = new UnitPage(page2)

    await employeeLogin(
      page2,
      await Fixture.employee().unitSupervisor(testDaycare.id).save()
    )

    // unit supervisor
    await unitPage.navigateToUnit(testDaycare.id)
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
    await applicationWorkbench.openPlacementQueue()

    await applicationWorkbench.assertServiceWorkerNoteMatches(0, 'TILARAJOITE')
  })

  test('Decision cannot be accepted on behalf of guardian if application is in placement proposal state', async () => {
    const fixture1 = {
      ...applicationFixture(testChild, familyWithTwoGuardians.guardian),
      status: 'SENT' as const
    }
    applicationId = fixture1.id

    await createApplications({ body: [fixture1] })
    await execSimpleApplicationActions(
      applicationId,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_PLACEMENT_PROPOSAL'
      ],
      mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    )

    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()
    await employeeLogin(page, unitSupervisor)
    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.waitUntilLoaded()
    await applicationReadView.assertDecisionDisabled('DAYCARE')
  })

  test('Supervisor can download decision PDF only after it has been generated', async () => {
    const application = {
      ...applicationFixture(testChild, testAdult),
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
      ...applicationFixture(testChild, testAdult),
      id: randomId<ApplicationId>(),
      status: 'WAITING_CONFIRMATION'
    }
    const application2: DevApplicationWithForm = {
      ...applicationFixture(testChild2, testAdult),
      id: randomId<ApplicationId>(),
      status: 'WAITING_CONFIRMATION'
    }
    const placementStartDate = LocalDate.of(2021, 8, 16)

    await createApplications({ body: [application1, application2] })

    await Fixture.placementPlan({
      applicationId: application1.id,
      unitId: testDaycare.id,
      periodStart: placementStartDate,
      periodEnd: placementStartDate
    }).save()

    await Fixture.placementPlan({
      applicationId: application2.id,
      unitId: testDaycare.id,
      periodStart: placementStartDate,
      periodEnd: placementStartDate
    }).save()

    const decisionId = (
      await Fixture.decision({
        applicationId: application2.id,
        employeeId: serviceWorker.id,
        unitId: testDaycare.id,
        startDate: placementStartDate,
        endDate: placementStartDate
      }).save()
    ).id

    await rejectDecisionByCitizen({ id: decisionId })

    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()

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
      await unitPage.navigateToUnit(testDaycare.id)
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
