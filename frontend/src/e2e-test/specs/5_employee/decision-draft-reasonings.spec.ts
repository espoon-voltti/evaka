// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
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

  test('Linking and unlinking an individual reasoning updates the decision card', async () => {
    const reasoning = await Fixture.decisionReasoningIndividual({
      collectionType: 'PRESCHOOL',
      titleFi: 'Esiopetus yksilö',
      titleSv: 'Förskola individuell',
      textFi: 'Esiopetuksen yksilöperustelu',
      textSv: 'Förskolans individuella motivering'
    }).save()

    const draftPage = await openDecisionDraft()
    const preschoolCard = draftPage.decisionCard('PRESCHOOL')
    await expect(preschoolCard.individualReasoning(reasoning.id)).toBeHidden()

    const picker = await draftPage.openPicker('PRESCHOOL')
    await picker.selectReasoning(reasoning.id)
    await picker.close()
    await expect(preschoolCard.individualReasoning(reasoning.id)).toBeVisible()

    await page.reload()
    await draftPage.waitUntilLoaded()
    await expect(preschoolCard.individualReasoning(reasoning.id)).toBeVisible()

    const reopenedPicker = await draftPage.openPicker('PRESCHOOL')
    await reopenedPicker.deselectReasoning(reasoning.id)
    await reopenedPicker.close()
    await expect(preschoolCard.individualReasoning(reasoning.id)).toBeHidden()
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
})
