// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  DevAssistanceNeedPreschoolDecision,
  EmployeeDetail
} from '../../dev-api/types'
import AssistanceNeedPreschoolDecisionPage from '../../pages/employee/assistance-need-decision/assistance-need-preschool-decision-page'
import {
  AssistanceNeedDecisionsReport,
  AssistanceNeedPreschoolDecisionsReportDecision
} from '../../pages/employee/reports'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let fixtures: AreaAndPersonFixtures
let serviceWorker: EmployeeDetail
let decisionPage: AssistanceNeedPreschoolDecisionPage
let childId: UUID
let assistanceNeedDecision: DevAssistanceNeedPreschoolDecision

beforeEach(async () => {
  await resetDatabase()

  serviceWorker = (await Fixture.employeeServiceWorker().save()).data

  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )

  await insertDaycarePlacementFixtures([daycarePlacementFixture])
})

describe('Assistance Need Preschool Decisions - Editing', () => {
  beforeEach(async () => {
    assistanceNeedDecision = (
      await Fixture.assistanceNeedPreschoolDecision()
        .withChild(childId)
        .withGuardian(fixtures.familyWithTwoGuardians.guardian.id)
        .withGuardian(fixtures.familyWithTwoGuardians.otherGuardian.id)
        .save()
    ).data

    page = await Page.open()
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
    await decisionPage.unitSelect.fillAndSelectFirst(daycareFixture.name)
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
    assistanceNeedDecision = (
      await Fixture.assistanceNeedPreschoolDecision()
        .withChild(childId)
        .withGuardian(fixtures.familyWithTwoGuardians.guardian.id)
        .withGuardian(fixtures.familyWithTwoGuardians.otherGuardian.id)
        .withRequiredFieldsFilled(
          daycareFixture.id,
          serviceWorker.id,
          serviceWorker.id
        )
        .save()
    ).data

    page = await Page.open()
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
