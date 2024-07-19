// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import { startTest } from '../../browser'
import config from '../../config'
import {
  createDaycarePlacementFixture,
  testDaycare,
  testDaycareGroup,
  Fixture,
  uuidv4,
  familyWithTwoGuardians,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements
} from '../../generated/api-clients'
import {
  DevAssistanceNeedPreschoolDecision,
  DevEmployee
} from '../../generated/api-types'
import AssistanceNeedPreschoolDecisionPage from '../../pages/employee/assistance-need-decision/assistance-need-preschool-decision-page'
import AssistanceNeedPreschoolDecisionPreviewPage from '../../pages/employee/assistance-need-decision/assistance-need-preschool-decision-preview-page'
import ChildInformationPage from '../../pages/employee/child-information'
import {
  AssistanceNeedDecisionsReport,
  AssistanceNeedPreschoolDecisionsReportDecision
} from '../../pages/employee/reports'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let serviceWorker: DevEmployee
let staff: DevEmployee
let decisionPage: AssistanceNeedPreschoolDecisionPage
let previewPage: AssistanceNeedPreschoolDecisionPreviewPage
let childId: UUID
let assistanceNeedDecision: DevAssistanceNeedPreschoolDecision

const mockedTime = LocalDate.of(2022, 12, 20)

beforeEach(async () => {
  await startTest()

  serviceWorker = await Fixture.employee().serviceWorker().save()

  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family(familyWithTwoGuardians).save()
  await createDaycareGroups({ body: [testDaycareGroup] })

  const unitId = testDaycare.id
  childId = familyWithTwoGuardians.children[0].id

  staff = await Fixture.employee().staff(unitId).save()
  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )

  await createDaycarePlacements({ body: [daycarePlacementFixture] })
})

const openPage = async (addDays = 0) =>
  await Page.open({
    mockedTime: mockedTime
      .addDays(addDays)
      .toHelsinkiDateTime(LocalTime.of(12, 0))
  })

describe('Assistance Need Preschool Decisions - Editing', () => {
  beforeEach(async () => {
    assistanceNeedDecision = await Fixture.assistanceNeedPreschoolDecision({
      childId
    })
      .withGuardian(familyWithTwoGuardians.guardian.id)
      .withGuardian(familyWithTwoGuardians.otherGuardian.id)
      .save()

    page = await openPage()
    await employeeLogin(page, serviceWorker)
    await page.goto(
      `${
        config.employeeUrl
      }/child-information/${childId}/assistance-need-preschool-decisions/${
        assistanceNeedDecision?.id ?? ''
      }/edit`
    )
    decisionPage = new AssistanceNeedPreschoolDecisionPage(page)
  })

  test('Some fields are visible', async () => {
    await decisionPage.primaryGroupInput.waitUntilVisible()
  })

  test('Info header shows correct information', async () => {
    await decisionPage.status.assertTextEquals('Luonnos')
    await decisionPage.decisionNumber.assertText((s) =>
      s.includes(assistanceNeedDecision.decisionNumber.toString(10))
    )
  })

  test('Autosave works', async () => {
    await decisionPage.primaryGroupInput.fill('Keijukaiset')
    await waitUntilEqual(
      () => decisionPage.autoSaveIndicator.getAttribute('data-status'),
      'saving'
    )
    await waitUntilEqual(
      () => decisionPage.autoSaveIndicator.getAttribute('data-status'),
      'saved'
    )
  })

  test('Clicking the preview button opens the decision in preview mode', async () => {
    await decisionPage.typeRadioNew.click()
    await decisionPage.validFromInput.fill('01.08.2022')
    await page.keyboard.press('Enter')
    await decisionPage.unitSelect.fillAndSelectFirst(testDaycare.name)
    await decisionPage.primaryGroupInput.fill('Keijukaiset')
    await decisionPage.decisionBasisInput.fill('Hyvät perustelut')
    await decisionPage.basisPedagogicalReportCheckbox.check()
    await decisionPage.guardiansHeardInput.fill('01.06.2022')
    await page.keyboard.press('Enter')
    await decisionPage.guardianHeardCheckbox(0).click()
    await decisionPage.guardianDetailsInput(0).fill('Puhelimitse')
    await decisionPage.guardianHeardCheckbox(1).click()
    await decisionPage.guardianDetailsInput(1).fill('Telepaattisesti')
    await decisionPage.viewOfGuardiansInput.fill('ok')
    await decisionPage.preparer1Select.fillAndSelectFirst(
      serviceWorker.firstName
    )
    await decisionPage.preparer1TitleInput.fill('Valmistelija')
    await decisionPage.decisionMakerSelect.fillAndSelectFirst(
      serviceWorker.firstName
    )
    await decisionPage.decisionMakerTitleInput.fill('Päättäjä')
    await decisionPage.previewButton.click()

    await page.page.waitForURL(
      `${
        config.employeeUrl
      }/child-information/${childId}/assistance-need-preschool-decisions/${
        assistanceNeedDecision?.id ?? ''
      }`
    )
  })
})

describe('Assistance Need Decisions - Decision process', () => {
  beforeEach(async () => {
    assistanceNeedDecision = await Fixture.assistanceNeedPreschoolDecision({
      childId
    })
      .withGuardian(familyWithTwoGuardians.guardian.id)
      .withGuardian(familyWithTwoGuardians.otherGuardian.id)
      .withRequiredFieldsFilled(
        testDaycare.id,
        serviceWorker.id,
        serviceWorker.id
      )
      .save()

    page = await openPage()
    await employeeLogin(page, serviceWorker)
    await page.goto(
      `${
        config.employeeUrl
      }/child-information/${childId}/assistance-need-preschool-decisions/${
        assistanceNeedDecision?.id ?? ''
      }`
    )
    decisionPage = new AssistanceNeedPreschoolDecisionPage(page)
  })

  test('Sending for decision, returning, editing, resending, accepting and annulling', async () => {
    await decisionPage.sendDecisionButton.click()
    await waitUntilEqual(() => decisionPage.sendDecisionButton.visible, false)
    await decisionPage.editButton.assertDisabled(true)

    await page.goto(`${config.employeeUrl}/reports/assistance-need-decisions/`)

    const reportListPage = new AssistanceNeedDecisionsReport(page)
    await reportListPage.rows.assertCount(1)
    await reportListPage.rows.nth(0).click()
    await page.page.waitForURL(
      `${config.employeeUrl}/reports/assistance-need-preschool-decisions/${
        assistanceNeedDecision?.id ?? ''
      }`
    )

    const reportDecisionPage =
      new AssistanceNeedPreschoolDecisionsReportDecision(page)
    await reportDecisionPage.returnForEditBtn.click()

    await page.goto(
      `${
        config.employeeUrl
      }/child-information/${childId}/assistance-need-preschool-decisions/${
        assistanceNeedDecision?.id ?? ''
      }`
    )
    await decisionPage.editButton.click()
    await decisionPage.decisionBasisInput.fill('Paremmat perustelut')
    await decisionPage.previewButton.click()
    await decisionPage.sendDecisionButton.click()

    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-preschool-decisions/${
        assistanceNeedDecision?.id ?? ''
      }`
    )
    await reportDecisionPage.approveBtn.click()
    await reportDecisionPage.modalOkBtn.click()
    await reportDecisionPage.status.assertTextEquals('Hyväksytty')

    await reportDecisionPage.annulBtn.click()
    await reportDecisionPage.modalOkBtn.assertDisabled(true)
    await reportDecisionPage.annulReasonInput.fill('Joku syy')
    await reportDecisionPage.modalOkBtn.click()
    await reportDecisionPage.status.assertTextEquals('Mitätöity')
    await reportDecisionPage.annulmentReason.assertTextEquals('Joku syy')
  })

  test('Sending for decision and rejecting', async () => {
    await decisionPage.sendDecisionButton.click()
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-preschool-decisions/${
        assistanceNeedDecision?.id ?? ''
      }`
    )

    const reportDecisionPage =
      new AssistanceNeedPreschoolDecisionsReportDecision(page)
    await reportDecisionPage.rejectBtn.click()
    await reportDecisionPage.modalOkBtn.click()
    await reportDecisionPage.status.assertTextEquals('Hylätty')
  })
})

let acceptedAssistanceNeedPreschoolDecision: DevAssistanceNeedPreschoolDecision
describe('Decision visibility for role', () => {
  describe('Staff', () => {
    beforeEach(async () => {
      acceptedAssistanceNeedPreschoolDecision =
        await Fixture.assistanceNeedPreschoolDecision({ childId })
          .withGuardian(familyWithTwoGuardians.guardian.id)
          .withGuardian(familyWithTwoGuardians.otherGuardian.id)
          .withForm({
            validFrom: LocalDate.of(2022, 7, 1),
            guardiansHeardOn: LocalDate.of(2022, 7, 1)
          })
          .withRequiredFieldsFilled(
            testDaycare.id,
            serviceWorker.id,
            serviceWorker.id
          )
          .with({
            decisionMade: LocalDate.of(2022, 7, 1),
            status: 'ACCEPTED',
            unreadGuardianIds: [familyWithTwoGuardians.guardian.id]
          })
          .save()

      await Fixture.assistanceNeedPreschoolDecision({ childId })
        .withGuardian(familyWithTwoGuardians.guardian.id)
        .withGuardian(familyWithTwoGuardians.otherGuardian.id)
        .withRequiredFieldsFilled(
          testDaycare.id,
          serviceWorker.id,
          serviceWorker.id
        )
        .with({
          status: 'DRAFT'
        })
        .withForm({ validFrom: LocalDate.of(2022, 8, 1) })
        .save()

      page = await openPage()
      await employeeLogin(page, staff)
    })

    test('Preview shows filled information', async () => {
      await page.goto(
        `${
          config.employeeUrl
        }/child-information/${childId}/assistance-need-preschool-decisions/${
          acceptedAssistanceNeedPreschoolDecision?.id ?? ''
        }`
      )
      previewPage = new AssistanceNeedPreschoolDecisionPreviewPage(page)

      await previewPage.guardiansHeardOn.assertTextEquals(
        acceptedAssistanceNeedPreschoolDecision.form.guardiansHeardOn?.format() ??
          ''
      )
      await previewPage.selectedUnit.assertTextEquals(testDaycare.name)
      await previewPage.preparedBy1.assertTextEquals(
        `${serviceWorker.firstName} ${serviceWorker.lastName}, ${acceptedAssistanceNeedPreschoolDecision.form.preparer1Title}`
      )
      await previewPage.decisionMaker.assertTextEquals(
        `${serviceWorker.firstName} ${serviceWorker.lastName}, ${acceptedAssistanceNeedPreschoolDecision.form.decisionMakerTitle}`
      )
    })

    test('Decision cannot be sent to the decision maker', async () => {
      await page.goto(
        `${
          config.employeeUrl
        }/child-information/${childId}/assistance-need-preschool-decisions/${
          acceptedAssistanceNeedPreschoolDecision?.id ?? ''
        }`
      )
      previewPage = new AssistanceNeedPreschoolDecisionPreviewPage(page)
      await previewPage.sendDecisionButton.waitUntilHidden()
    })

    test('Only accepted decisions can be seen by staff', async () => {
      await page.goto(config.employeeUrl + '/child-information/' + childId)
      const childInformationPage = new ChildInformationPage(page)
      const assistance =
        await childInformationPage.openCollapsible('assistance')

      await assistance.assertAssistanceNeedDecisionCount(1)
      const decision = await assistance.assistanceNeedDecisions(0)
      expect(decision.status).toEqual('ACCEPTED')
    })
  })
})
