// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  familyWithTwoGuardians,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements,
  resetDatabase
} from '../../generated/api-clients'
import {
  DevAssistanceNeedDecision,
  DevEmployee
} from '../../generated/api-types'
import AssistanceNeedDecisionEditPage from '../../pages/employee/assistance-need-decision/assistance-need-decision-edit-page'
import AssistanceNeedDecisionPreviewPage from '../../pages/employee/assistance-need-decision/assistance-need-decision-preview-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let fixtures: AreaAndPersonFixtures
let serviceWorker: DevEmployee
let staff: DevEmployee
let assistanceNeedDecisionEditPage: AssistanceNeedDecisionEditPage
let childId: UUID
let assistanceNeedDecision: DevAssistanceNeedDecision
let preFilledAssistanceNeedDecision: DevAssistanceNeedDecision

const mockedTime = LocalDate.of(2022, 12, 20)

beforeEach(async () => {
  await resetDatabase()

  serviceWorker = (await Fixture.employeeServiceWorker().save()).data

  fixtures = await initializeAreaAndPersonData()
  await createDaycareGroups({ body: [daycareGroupFixture] })

  const unitId = fixtures.daycareFixture.id
  staff = (await Fixture.employeeStaff(unitId).save()).data
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )

  assistanceNeedDecision = (
    await Fixture.assistanceNeedDecision().withChild(childId).save()
  ).data

  preFilledAssistanceNeedDecision = (
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(childId)
      .with({
        selectedUnit: fixtures.daycareFixture.id,
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
        },
        guardianInfo: [
          {
            id: null,
            personId: fixtures.familyWithTwoGuardians.guardian.id,
            isHeard: true,
            name: '',
            details: 'Guardian 1 details'
          }
        ]
      })
      .save()
  ).data

  await createDaycarePlacements({ body: [daycarePlacementFixture] })
})

const openPage = async (addDays = 0) =>
  await Page.open({
    mockedTime: mockedTime
      .addDays(addDays)
      .toHelsinkiDateTime(LocalTime.of(12, 0))
  })

describe('Assistance Need Decisions - Edit page', () => {
  beforeEach(async () => {
    page = await openPage()
    await employeeLogin(page, serviceWorker)
    await page.goto(
      `${
        config.employeeUrl
      }/child-information/${childId}/assistance-need-decision/${
        assistanceNeedDecision?.id ?? ''
      }/edit`
    )
    assistanceNeedDecisionEditPage = new AssistanceNeedDecisionEditPage(page)
  })

  test('Some fields are visible', async () => {
    await assistanceNeedDecisionEditPage.assertDeciderSelectVisible()
  })

  test('Info header shows correct information', async () => {
    await assistanceNeedDecisionEditPage.assertDecisionStatus('Luonnos')
    await assistanceNeedDecisionEditPage.assertDecisionNumber(
      assistanceNeedDecision.decisionNumber
    )
  })

  test('Autosave works', async () => {
    await assistanceNeedDecisionEditPage.pedagogicalMotivationInput.fill(
      'this is a motivation'
    )
    await assistanceNeedDecisionEditPage.waitUntilSaved()
  })

  test('Clicking the preview button opens the decision in preview mode', async () => {
    await assistanceNeedDecisionEditPage.assertDeciderSelectVisible()
    await assistanceNeedDecisionEditPage.selectUnit(daycareFixture.name)
    await assistanceNeedDecisionEditPage.pedagogicalMotivationInput.fill(
      'Pedagogical motivation text'
    )
    await assistanceNeedDecisionEditPage.guardiansHeardOnInput.fill(
      '20.04.2020'
    )
    await assistanceNeedDecisionEditPage.fillDecisionMaker(
      serviceWorker.lastName,
      'aluepäällikkö'
    )
    await assistanceNeedDecisionEditPage.fillPreparator(
      serviceWorker.lastName,
      'erityisopettaja'
    )
    await assistanceNeedDecisionEditPage.clickPreviewButton()

    await page.page.waitForURL(
      `${
        config.employeeUrl
      }/child-information/${childId}/assistance-need-decision/${
        assistanceNeedDecision?.id ?? ''
      }`
    )
  })

  test('Clicking the leave page button opens the child info page', async () => {
    await assistanceNeedDecisionEditPage.assertDeciderSelectVisible()
    await assistanceNeedDecisionEditPage.clickLeavePageButton()

    await page.page.waitForURL(
      `${config.employeeUrl}/child-information/${childId}`
    )
  })
})

describe('Assistance Need Decisions - Language', () => {
  beforeEach(async () => {
    page = await openPage()
    await employeeLogin(page, serviceWorker)
    await page.goto(
      `${
        config.employeeUrl
      }/child-information/${childId}/assistance-need-decision/${
        preFilledAssistanceNeedDecision?.id ?? ''
      }/edit`
    )
    assistanceNeedDecisionEditPage = new AssistanceNeedDecisionEditPage(page)
  })

  test('Change language to swedish', async () => {
    await assistanceNeedDecisionEditPage.assertPageTitle(
      'Päätös tuesta varhaiskasvatuksessa'
    )
    await assistanceNeedDecisionEditPage.selectLanguage('Ruotsi')
    await assistanceNeedDecisionEditPage.assertPageTitle('Beslut om stöd')
    await assistanceNeedDecisionEditPage.waitUntilSaved()

    await page.goto(
      `${
        config.employeeUrl
      }/child-information/${childId}/assistance-need-decision/${
        preFilledAssistanceNeedDecision?.id ?? ''
      }`
    )
    const assistanceNeedDecisionPreviewPage =
      new AssistanceNeedDecisionPreviewPage(page)
    await assistanceNeedDecisionPreviewPage.assertPageTitle('Beslut om stöd')
  })
})

describe('Assistance Need Decisions - Preview page', () => {
  let assistanceNeedDecisionPreviewPage: AssistanceNeedDecisionPreviewPage

  describe('Service worker', () => {
    beforeEach(async () => {
      page = await openPage()
      await employeeLogin(page, serviceWorker)
      await page.goto(
        `${
          config.employeeUrl
        }/child-information/${childId}/assistance-need-decision/${
          preFilledAssistanceNeedDecision?.id ?? ''
        }`
      )
      assistanceNeedDecisionPreviewPage = new AssistanceNeedDecisionPreviewPage(
        page
      )
    })

    test('Preview shows filled information', async () => {
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.pedagogicalMotivation,
        'Pedagogical motivation text'
      )
      await assistanceNeedDecisionPreviewPage.assertStructuralMotivationOption(
        'groupAssistant'
      )
      await assistanceNeedDecisionPreviewPage.assertStructuralMotivationOption(
        'smallerGroup'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.structuralMotivationDescription,
        'Structural motivation description text'
      )
      await assistanceNeedDecisionPreviewPage.assertServiceOption(
        'interpretationAndAssistanceServices'
      )
      await assistanceNeedDecisionPreviewPage.assertServiceOption(
        'partTimeSpecialEd'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.careMotivation,
        'Care motivation text'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.guardiansHeardOn,
        '05.04.2020'
      )
      await waitUntilEqual(
        () =>
          assistanceNeedDecisionPreviewPage.heardGuardian(
            familyWithTwoGuardians.guardian.id
          ),
        `${familyWithTwoGuardians.guardian.lastName} ${familyWithTwoGuardians.guardian.firstName}: Guardian 1 details`
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.otherRepresentativeDetails,
        'John Doe, 01020304050, via phone'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.viewOfGuardians,
        'VOG text'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.futureLevelOfAssistance,
        'Tehostettu tuki'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.startDate,
        '02.01.2019'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.selectedUnit,
        `${daycareFixture.name}\n${daycareFixture.streetAddress}\n${daycareFixture.postalCode} ${daycareFixture.postOffice}\nLoma-aikoina tuen järjestämispaikka ja -tapa saattavat muuttua.`
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.motivationForDecision,
        'Motivation for decision text'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.preparedBy1,
        `${serviceWorker.firstName} ${serviceWorker.lastName}, teacher\n010202020202`
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.decisionMaker,
        `${serviceWorker.firstName} ${serviceWorker.lastName}, head teacher`
      )
    })

    test('Decision can be sent to the decision maker', async () => {
      await assistanceNeedDecisionPreviewPage.sendDecisionButton.click()
      await assistanceNeedDecisionPreviewPage.decisionSentAt.assertTextEquals(
        mockedTime.format()
      )
      await assistanceNeedDecisionPreviewPage.sendDecisionButton.assertAttributeEquals(
        'disabled',
        ''
      )
    })
  })

  describe('Staff', () => {
    beforeEach(async () => {
      const acceptedAssistanceNeedDecision = (
        await Fixture.preFilledAssistanceNeedDecision()
          .withChild(childId)
          .with({
            status: 'ACCEPTED',
            selectedUnit: fixtures.daycareFixture.id,
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
            },
            guardianInfo: [
              {
                id: null,
                personId: fixtures.familyWithTwoGuardians.guardian.id,
                isHeard: true,
                name: '',
                details: 'Guardian 1 details'
              }
            ]
          })
          .save()
      ).data

      page = await openPage()
      await employeeLogin(page, staff)
      await page.goto(
        `${
          config.employeeUrl
        }/child-information/${childId}/assistance-need-decision/${
          acceptedAssistanceNeedDecision?.id ?? ''
        }`
      )
      assistanceNeedDecisionPreviewPage = new AssistanceNeedDecisionPreviewPage(
        page
      )
    })

    test('Preview shows filled information', async () => {
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.pedagogicalMotivation,
        'Pedagogical motivation text'
      )
      await assistanceNeedDecisionPreviewPage.assertStructuralMotivationOption(
        'groupAssistant'
      )
      await assistanceNeedDecisionPreviewPage.assertStructuralMotivationOption(
        'smallerGroup'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.structuralMotivationDescription,
        'Structural motivation description text'
      )
      await assistanceNeedDecisionPreviewPage.assertServiceOption(
        'interpretationAndAssistanceServices'
      )
      await assistanceNeedDecisionPreviewPage.assertServiceOption(
        'partTimeSpecialEd'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.careMotivation,
        'Care motivation text'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.guardiansHeardOn,
        '05.04.2020'
      )
      await waitUntilEqual(
        () =>
          assistanceNeedDecisionPreviewPage.heardGuardian(
            familyWithTwoGuardians.guardian.id
          ),
        `${familyWithTwoGuardians.guardian.lastName} ${familyWithTwoGuardians.guardian.firstName}: Guardian 1 details`
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.otherRepresentativeDetails,
        'John Doe, 01020304050, via phone'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.viewOfGuardians,
        'VOG text'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.futureLevelOfAssistance,
        'Tehostettu tuki'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.startDate,
        '02.01.2019'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.selectedUnit,
        `${daycareFixture.name}\n${daycareFixture.streetAddress}\n${daycareFixture.postalCode} ${daycareFixture.postOffice}\nLoma-aikoina tuen järjestämispaikka ja -tapa saattavat muuttua.`
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.motivationForDecision,
        'Motivation for decision text'
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.preparedBy1,
        `${serviceWorker.firstName} ${serviceWorker.lastName}, teacher\n010202020202`
      )
      await waitUntilEqual(
        () => assistanceNeedDecisionPreviewPage.decisionMaker,
        `${serviceWorker.firstName} ${serviceWorker.lastName}, head teacher`
      )
    })

    test('Decision cannot be sent to the decision maker', async () => {
      await assistanceNeedDecisionPreviewPage.sendDecisionButton.waitUntilHidden()
    })
  })

  describe('Admin', () => {
    test('Sent decision can be reverted by admin', async () => {
      const admin = await Fixture.employeeAdmin().save()

      page = await openPage()
      await employeeLogin(page, admin.data)

      await page.goto(
        `${
          config.employeeUrl
        }/child-information/${childId}/assistance-need-decision/${
          preFilledAssistanceNeedDecision?.id ?? ''
        }`
      )
      assistanceNeedDecisionPreviewPage = new AssistanceNeedDecisionPreviewPage(
        page
      )

      await assistanceNeedDecisionPreviewPage.sendDecisionButton.click()
      await assistanceNeedDecisionPreviewPage.decisionSentAt.assertTextEquals(
        mockedTime.format()
      )
      await assistanceNeedDecisionPreviewPage.sendDecisionButton.assertAttributeEquals(
        'disabled',
        ''
      )

      await assistanceNeedDecisionPreviewPage.revertToUnsent.click()
      await assistanceNeedDecisionPreviewPage.decisionSentAt.waitUntilHidden()
      await assistanceNeedDecisionPreviewPage.revertToUnsent.waitUntilHidden()
    })
  })
})
