// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { execSimpleApplicationActions } from '../../dev-api'
import {
  applicationFixture,
  Fixture,
  preschoolTerm2021,
  testAdult,
  testCareArea,
  testChild2,
  testDaycare,
  testPreschool
} from '../../dev-api/fixtures'
import {
  cleanUpMessages,
  createApplications,
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedDate = LocalDate.of(2021, 8, 16)

test.use({
  evakaOptions: {
    mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
  }
})

test.describe('Decision draft reasonings', () => {
  let page: Page
  let applicationId: ApplicationId
  let serviceWorker: DevEmployee

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await cleanUpMessages()
    await Fixture.decisionReasoningGenericDefaults().save()
    await preschoolTerm2021.save()
    await testCareArea.save()
    await testDaycare.save()
    await testPreschool.save()
    await Fixture.family({
      guardian: testAdult,
      children: [testChild2]
    }).save()
    serviceWorker = await Fixture.employee().serviceWorker().save()
    await createDefaultServiceNeedOptions()
    await Fixture.feeThresholds().save()

    const fixture = {
      ...applicationFixture(
        testChild2,
        testAdult,
        undefined,
        'PRESCHOOL',
        null,
        [testPreschool.id],
        true,
        'SENT',
        mockedDate
      ),
      id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
    }
    applicationId = fixture.id
    await createApplications({ body: [fixture] })
    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT', 'CREATE_DEFAULT_PLACEMENT_PLAN'],
      HelsinkiDateTime.fromLocal(mockedDate, LocalTime.of(13, 0))
    )

    page = evaka
  })

  test('Generic preview shows the right collection per decision card', async () => {
    await Fixture.decisionReasoningGeneric({
      collectionType: 'PRESCHOOL',
      validFrom: LocalDate.of(2021, 1, 1),
      textFi: 'Esiopetuksen perustelu',
      textSv: 'Förskolemotivering'
    }).save()
    await Fixture.decisionReasoningGeneric({
      collectionType: 'DAYCARE',
      validFrom: LocalDate.of(2021, 1, 1),
      textFi: 'Varhaiskasvatuksen perustelu',
      textSv: 'Småbarnspedagogikmotivering'
    }).save()

    const draftPage = await openDecisionDraft()

    const preschoolCard = draftPage.decisionCard('PRESCHOOL')
    await expect(preschoolCard.genericReasoning('PRESCHOOL')).toContainText(
      'Esiopetuksen perustelu'
    )
    await expect(preschoolCard.genericReasoning('DAYCARE')).toBeHidden()

    const preschoolDaycareCard = draftPage.decisionCard('PRESCHOOL_DAYCARE')
    await expect(
      preschoolDaycareCard.genericReasoning('DAYCARE')
    ).toContainText('Varhaiskasvatuksen perustelu')
    await expect(
      preschoolDaycareCard.genericReasoning('PRESCHOOL')
    ).toBeHidden()
  })

  test('Generic preview marks a not-ready reasoning with the not-ready pill', async () => {
    await Fixture.decisionReasoningGeneric({
      collectionType: 'PRESCHOOL',
      validFrom: LocalDate.of(2021, 1, 1),
      ready: false,
      textFi: 'Esiopetuksen perustelu (luonnos)',
      textSv: 'Förskolemotivering (utkast)'
    }).save()

    const draftPage = await openDecisionDraft()

    const preschoolGenericCard = draftPage
      .decisionCard('PRESCHOOL')
      .genericReasoning('PRESCHOOL')
    await expect(preschoolGenericCard).toContainText(
      'Esiopetuksen perustelu (luonnos)'
    )
    await expect(
      preschoolGenericCard.findByDataQa('not-ready-pill')
    ).toBeVisible()
  })

  test('Picker shows only individual reasonings eligible for the decision type', async () => {
    const preschoolReasoning = await Fixture.decisionReasoningIndividual({
      collectionType: 'PRESCHOOL',
      titleFi: 'Esiopetus yksilö',
      titleSv: 'Förskola individuell',
      textFi: 'Esiopetuksen yksilöperustelu',
      textSv: 'Förskolans individuella motivering'
    }).save()
    const daycareReasoning = await Fixture.decisionReasoningIndividual({
      collectionType: 'DAYCARE',
      titleFi: 'Varhaiskasvatus yksilö',
      titleSv: 'Småbarnspedagogik individuell',
      textFi: 'Varhaiskasvatuksen yksilöperustelu',
      textSv: 'Småbarnspedagogiks individuella motivering'
    }).save()
    const preschoolReasoningId = preschoolReasoning.id
    const daycareReasoningId = daycareReasoning.id

    const draftPage = await openDecisionDraft()

    const preschoolPicker = await draftPage.openPicker('PRESCHOOL')
    await expect(
      preschoolPicker.reasoningRow(preschoolReasoningId)
    ).toBeVisible()
    await expect(preschoolPicker.reasoningRow(daycareReasoningId)).toBeHidden()
    await expect(preschoolPicker.reasoningRows()).toHaveCount(1)
    await preschoolPicker.close()

    const daycarePicker = await draftPage.openPicker('PRESCHOOL_DAYCARE')
    await expect(daycarePicker.reasoningRow(daycareReasoningId)).toBeVisible()
    await expect(daycarePicker.reasoningRow(preschoolReasoningId)).toBeHidden()
    await expect(daycarePicker.reasoningRows()).toHaveCount(1)
    await daycarePicker.close()
  })

  test('Individual reasonings persist on save and the selection can be changed', async () => {
    const reasoningA = await Fixture.decisionReasoningIndividual({
      collectionType: 'PRESCHOOL',
      titleFi: 'Esiopetus yksilö A',
      titleSv: 'Förskola individuell A',
      textFi: 'Esiopetuksen yksilöperustelu A',
      textSv: 'Förskolans individuella motivering A'
    }).save()
    const reasoningB = await Fixture.decisionReasoningIndividual({
      collectionType: 'PRESCHOOL',
      titleFi: 'Esiopetus yksilö B',
      titleSv: 'Förskola individuell B',
      textFi: 'Esiopetuksen yksilöperustelu B',
      textSv: 'Förskolans individuella motivering B'
    }).save()

    let draftPage = await openDecisionDraft()
    let preschoolCard = draftPage.decisionCard('PRESCHOOL')
    // Wait for the reasoning section to load, then assert nothing is selected yet
    await expect(preschoolCard.pickerButton('PRESCHOOL')).toBeVisible()
    await expect(preschoolCard.individualReasoning(reasoningA.id)).toBeHidden()

    // Choose reasoning A and close the modal — visible in page state, not yet saved
    let picker = await draftPage.openPicker('PRESCHOOL')
    await picker.selectReasoning(reasoningA.id)
    await picker.close()
    await expect(preschoolCard.individualReasoning(reasoningA.id)).toBeVisible()

    // Save and reopen — A is persisted
    await draftPage.save()
    await expect(page.findByDataQa('save-decisions-button')).toBeHidden()
    draftPage = await reopenDecisionDraft()
    preschoolCard = draftPage.decisionCard('PRESCHOOL')
    await expect(preschoolCard.individualReasoning(reasoningA.id)).toBeVisible()

    // Switch the selection to B and unselect A, close the modal
    picker = await draftPage.openPicker('PRESCHOOL')
    await picker.selectReasoning(reasoningB.id)
    await picker.deselectReasoning(reasoningA.id)
    await picker.close()
    await expect(preschoolCard.individualReasoning(reasoningB.id)).toBeVisible()
    await expect(preschoolCard.individualReasoning(reasoningA.id)).toBeHidden()

    // Save and reopen — B is persisted and A is gone
    await draftPage.save()
    await expect(page.findByDataQa('save-decisions-button')).toBeHidden()
    draftPage = await reopenDecisionDraft()
    preschoolCard = draftPage.decisionCard('PRESCHOOL')
    await expect(preschoolCard.individualReasoning(reasoningB.id)).toBeVisible()
    await expect(preschoolCard.individualReasoning(reasoningA.id)).toBeHidden()
  })

  test('Swedish unit renders reasonings in Swedish', async () => {
    await Fixture.decisionReasoningGeneric({
      collectionType: 'PRESCHOOL',
      validFrom: LocalDate.of(2021, 1, 1),
      textFi: 'Esiopetuksen perustelu',
      textSv: 'Förskolemotivering'
    }).save()
    const individual = await Fixture.decisionReasoningIndividual({
      collectionType: 'PRESCHOOL',
      titleFi: 'Esiopetus yksilö',
      titleSv: 'Förskola individuell',
      textFi: 'Esiopetuksen yksilöperustelu',
      textSv: 'Förskolans individuella motivering'
    }).save()

    const svPreschool = await Fixture.daycare({
      ...testPreschool,
      id: fromUuid<DaycareId>('b53d80e0-319b-4d2b-950c-f5c3c9f834bd'),
      name: 'Alkuräjähdyksen eskari (SV)',
      language: 'sv'
    }).save()

    const svApplicationId = fromUuid<ApplicationId>(
      '6a9b1b1e-3fdf-11eb-b378-0242ac130003'
    )
    await createApplications({
      body: [
        {
          ...applicationFixture(
            testChild2,
            testAdult,
            undefined,
            'PRESCHOOL',
            null,
            [svPreschool.id],
            true,
            'SENT',
            mockedDate
          ),
          id: svApplicationId
        }
      ]
    })
    await execSimpleApplicationActions(
      svApplicationId,
      ['MOVE_TO_WAITING_PLACEMENT', 'CREATE_DEFAULT_PLACEMENT_PLAN'],
      HelsinkiDateTime.fromLocal(mockedDate, LocalTime.of(13, 0))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    const applicationListView = new ApplicationListView(page)
    await applicationListView.filterByApplicationStatus('WAITING_DECISION')
    await applicationListView.searchButton.click()
    const draftPage = await applicationListView
      .applicationRow(svApplicationId)
      .primaryActionEditDecisionsRedesign()
    await draftPage.waitUntilLoaded()

    const preschoolCard = draftPage.decisionCard('PRESCHOOL')
    await expect(preschoolCard.unitLanguageUnsupportedWarning).toBeHidden()

    await expect(preschoolCard.genericReasoning('PRESCHOOL')).toContainText(
      'Förskolemotivering'
    )

    const picker = await draftPage.openPicker('PRESCHOOL')
    await picker.selectReasoning(individual.id)
    await picker.close()
    await expect(
      preschoolCard.individualReasoning(individual.id)
    ).toContainText('Förskola individuell')
  })

  async function openDecisionDraft() {
    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    const applicationListView = new ApplicationListView(page)
    await applicationListView.filterByApplicationStatus('WAITING_DECISION')
    await applicationListView.searchButton.click()
    const draftPage = await applicationListView
      .applicationRow(applicationId)
      .primaryActionEditDecisionsRedesign()
    await draftPage.waitUntilLoaded()
    return draftPage
  }

  async function reopenDecisionDraft() {
    const applicationListView = new ApplicationListView(page)
    await applicationListView.searchButton.click()
    const draftPage = await applicationListView
      .applicationRow(applicationId)
      .primaryActionEditDecisionsRedesign()
    await draftPage.waitUntilLoaded()
    return draftPage
  }
})
