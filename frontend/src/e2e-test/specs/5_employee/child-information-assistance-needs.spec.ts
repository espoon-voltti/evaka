// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { addDays, subDays } from 'date-fns'

import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  familyWithTwoGuardians,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import ChildInformationPage, {
  AssistanceNeedSection
} from '../../pages/employee/child-information'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformationPage: ChildInformationPage
let assistanceNeeds: AssistanceNeedSection
let childId: UUID
let unitId: UUID
let voucherUnitId: UUID
let admin: EmployeeDetail

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  unitId = fixtures.daycareFixture.id
  voucherUnitId = fixtures.daycareFixturePrivateVoucher.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  page = await Page.open()
  admin = (await Fixture.employeeAdmin().save()).data
})

const setupPlacement = async (
  childPlacementType: PlacementType,
  voucher = false
) => {
  await insertDaycarePlacementFixtures([
    createDaycarePlacementFixture(
      uuidv4(),
      childId,
      voucher ? voucherUnitId : unitId,
      LocalDate.todayInSystemTz().formatIso(),
      LocalDate.todayInSystemTz().formatIso(),
      childPlacementType
    )
  ])
}

const logUserIn = async (user: EmployeeDetail) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  assistanceNeeds = await childInformationPage.openCollapsible('assistanceNeed')
}

describe('Child Information assistance need functionality for employees', () => {
  test('assistance need can be added', async () => {
    await setupPlacement('DAYCARE')
    await logUserIn((await Fixture.employeeUnitSupervisor(unitId).save()).data)
    await assistanceNeeds.createNewAssistanceNeed()
    await assistanceNeeds.setAssistanceNeedMultiplier('1,5')
    await assistanceNeeds.confirmAssistanceNeed()
    await assistanceNeeds.assertAssistanceNeedMultiplier('1,5')
  })

  test('assistance need before preschool for a child in preschool is not shown for unit supervisor', async () => {
    await setupPlacement('PRESCHOOL')
    const unitSupervisor = (await Fixture.employeeUnitSupervisor(unitId).save())
      .data
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        endDate: new Date(),
        description:
          'Test service need to be hidden because it starts before preschool started',
        updatedBy: unitSupervisor.id
      })
      .save()

    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: addDays(new Date(), 1),
        endDate: addDays(new Date(), 1),
        description:
          'Test service need to be shown because it starts after preschool started',
        updatedBy: unitSupervisor.id
      })
      .save()

    await logUserIn(unitSupervisor)
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need for preschool for a child in preschool is shown for unit manager', async () => {
    await setupPlacement('PRESCHOOL')
    const unitSupervisor = (await Fixture.employeeUnitSupervisor(unitId).save())
      .data
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: new Date(),
        description:
          'Test service need to be shown because it starts when preschool started',
        updatedBy: unitSupervisor.id
      })
      .save()

    await logUserIn(unitSupervisor)
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need before preschool for a child in preschool is shown for admin', async () => {
    await setupPlacement('PRESCHOOL')
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        description: 'Test service need to be shown because user is admin',
        updatedBy: admin.id
      })
      .save()

    await logUserIn(admin)
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need before preschool for a child in preschool is shown for Special Education Teacher', async () => {
    await setupPlacement('PRESCHOOL')
    const specialEducationTeacher = (
      await Fixture.employeeSpecialEducationTeacher(unitId).save()
    ).data

    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        endDate: new Date(),
        description:
          'Test service need to be shown to SEO if she has acl rights to child',
        updatedBy: specialEducationTeacher.id
      })
      .save()

    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: addDays(new Date(), 1),
        endDate: addDays(new Date(), 2),
        description:
          'Test service to be shown because it is active only during preschool period',
        updatedBy: specialEducationTeacher.id
      })
      .save()

    await logUserIn(specialEducationTeacher)
    await assistanceNeeds.assertAssistanceNeedCount(2)
  })
})

describe('Child assistance need decisions for employees', () => {
  test('Shows an empty draft in the list', async () => {
    await Fixture.assistanceNeedDecision().withChild(childId).save()

    await logUserIn(admin)
    await assistanceNeeds.waitUntilAssistanceNeedDecisionsLoaded()

    const decision = await assistanceNeeds.assistanceNeedDecisions(0)

    expect(decision.date).toEqual('-')
    expect(decision.unitName).toEqual('-')
    expect(decision.sentDate).toEqual('-')
    expect(decision.decisionMadeDate).toEqual('-')
    expect(decision.status).toEqual('DRAFT')
    expect(decision.actionCount).toEqual(2)
  })

  test('Shows a filled in draft in the list', async () => {
    const serviceWorker = (await Fixture.employeeServiceWorker().save()).data
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(childId)
      .with({
        selectedUnit: { id: daycareFixture.id },
        decisionMaker: {
          employeeId: serviceWorker.id,
          title: 'head teacher'
        },
        preparedBy1: {
          employeeId: serviceWorker.id,
          title: 'teacher',
          phoneNumber: '010202020202'
        },
        guardianInfo: [
          {
            id: null,
            personId: familyWithTwoGuardians.guardian.id,
            isHeard: true,
            name: '',
            details: 'Guardian 1 details'
          }
        ],
        sentForDecision: LocalDate.of(2020, 5, 11),
        startDate: LocalDate.of(2020, 7, 1),
        endDate: LocalDate.of(2020, 12, 11),
        decisionMade: LocalDate.of(2020, 6, 2)
      })
      .save()

    await logUserIn(admin)
    await assistanceNeeds.waitUntilAssistanceNeedDecisionsLoaded()

    const decision = await assistanceNeeds.assistanceNeedDecisions(0)

    expect(decision.date).toEqual('01.07.2020 – 11.12.2020')
    expect(decision.unitName).toEqual(daycareFixture.name)
    expect(decision.sentDate).toEqual('11.05.2020')
    expect(decision.decisionMadeDate).toEqual('02.06.2020')
    expect(decision.status).toEqual('DRAFT')
    expect(decision.actionCount).toEqual(2)
  })

  test('Hides edit and delete actions for non-draft/non-workable decisions', async () => {
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(childId)
      .with({
        status: 'ACCEPTED'
      })
      .save()
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(childId)
      .with({
        status: 'REJECTED'
      })
      .save()

    await logUserIn(admin)
    await assistanceNeeds.waitUntilAssistanceNeedDecisionsLoaded()

    const acceptedDecision = await assistanceNeeds.assistanceNeedDecisions(0)
    expect(acceptedDecision.status).toEqual('ACCEPTED')
    expect(acceptedDecision.actionCount).toEqual(0)

    const rejectedDecision = await assistanceNeeds.assistanceNeedDecisions(1)
    expect(rejectedDecision.status).toEqual('REJECTED')
    expect(rejectedDecision.actionCount).toEqual(0)
  })
})

describe('Child assistance need voucher coefficients for employees', () => {
  async function createVoucherCoefficient() {
    await assistanceNeeds.createAssistanceNeedVoucherCoefficientBtn.click()
    const form = assistanceNeeds.assistanceNeedVoucherCoefficientForm(
      assistanceNeeds.createAssistanceNeedVoucherCoefficientForm
    )
    await form.coefficientInput.type('4,3')
    await form.validityPeriod.startInput.clear()
    await form.validityPeriod.startInput.type('04.02.2021')
    await form.validityPeriod.endInput.clear()
    await form.validityPeriod.endInput.type('01.09.2021')
    await form.saveBtn.click()
  }

  test('assistance need voucher coefficient can be added', async () => {
    await setupPlacement('DAYCARE', true)
    await logUserIn(
      (
        await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
      ).data
    )
    await createVoucherCoefficient()

    await waitUntilEqual(
      async () => await assistanceNeeds.assistanceNeedVoucherCoefficients(0),
      {
        coefficient: 'Palvelusetelikerroin 4,3',
        validityPeriod: '04.02.2021 – 01.09.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )
  })

  test('new assistance need voucher coefficient cuts off previous one', async () => {
    await setupPlacement('DAYCARE', true)
    await logUserIn(
      (
        await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
      ).data
    )
    await createVoucherCoefficient()
    await assistanceNeeds.createAssistanceNeedVoucherCoefficientBtn.click()
    const form = assistanceNeeds.assistanceNeedVoucherCoefficientForm(
      assistanceNeeds.createAssistanceNeedVoucherCoefficientForm
    )
    await form.coefficientInput.type('1,2')
    await form.validityPeriod.startInput.clear()
    await form.validityPeriod.startInput.type('16.06.2021')
    await form.validityPeriod.endInput.clear()
    await form.validityPeriod.endInput.type('05.10.2021')
    await form.saveBtn.click()

    await waitUntilEqual(
      async () => await assistanceNeeds.assistanceNeedVoucherCoefficients(0),
      {
        coefficient: 'Palvelusetelikerroin 1,2',
        validityPeriod: '16.06.2021 – 05.10.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )

    await waitUntilEqual(
      async () => await assistanceNeeds.assistanceNeedVoucherCoefficients(1),
      {
        coefficient: 'Palvelusetelikerroin 4,3',
        validityPeriod: '04.02.2021 – 15.06.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )
  })

  test('assistance need voucher coefficient can be edited', async () => {
    await setupPlacement('DAYCARE', true)
    await logUserIn(
      (
        await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
      ).data
    )
    await createVoucherCoefficient()

    await assistanceNeeds
      .assistanceNeedVoucherCoefficientActions(0)
      .editBtn.click()
    const form = assistanceNeeds.assistanceNeedVoucherCoefficientForm(
      assistanceNeeds.editAssistanceNeedVoucherCoefficientForm
    )
    await form.coefficientInput.clear()
    await form.coefficientInput.type('9,8')
    await form.validityPeriod.startInput.clear()
    await form.validityPeriod.startInput.type('10.02.2021')
    await form.validityPeriod.endInput.clear()
    await form.validityPeriod.endInput.type('21.11.2021')
    await form.saveBtn.click()

    await waitUntilEqual(
      async () => await assistanceNeeds.assistanceNeedVoucherCoefficients(0),
      {
        coefficient: 'Palvelusetelikerroin 9,8',
        validityPeriod: '10.02.2021 – 21.11.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )
  })

  test('assistance need voucher coefficient editing cuts off other coefficient', async () => {
    await setupPlacement('DAYCARE', true)
    await logUserIn(
      (
        await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
      ).data
    )
    await createVoucherCoefficient()

    await assistanceNeeds.createAssistanceNeedVoucherCoefficientBtn.click()
    const creationForm = assistanceNeeds.assistanceNeedVoucherCoefficientForm(
      assistanceNeeds.createAssistanceNeedVoucherCoefficientForm
    )
    await creationForm.coefficientInput.type('1,9')
    await creationForm.validityPeriod.startInput.clear()
    await creationForm.validityPeriod.startInput.type('29.11.2021')
    await creationForm.validityPeriod.endInput.clear()
    await creationForm.validityPeriod.endInput.type('30.12.2021')
    await creationForm.saveBtn.click()

    await waitUntilEqual(
      async () => await assistanceNeeds.assistanceNeedVoucherCoefficients(0),
      {
        coefficient: 'Palvelusetelikerroin 1,9',
        validityPeriod: '29.11.2021 – 30.12.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )

    await assistanceNeeds
      .assistanceNeedVoucherCoefficientActions(0)
      .editBtn.click()
    const form = assistanceNeeds.assistanceNeedVoucherCoefficientForm(
      assistanceNeeds.editAssistanceNeedVoucherCoefficientForm
    )
    await form.coefficientInput.clear()
    await form.coefficientInput.type('9,8')
    await form.validityPeriod.startInput.clear()
    await form.validityPeriod.startInput.type('20.08.2021')
    await form.validityPeriod.endInput.clear()
    await form.validityPeriod.endInput.type('24.11.2021')
    await form.saveBtn.click()

    await waitUntilEqual(
      async () => await assistanceNeeds.assistanceNeedVoucherCoefficients(0),
      {
        coefficient: 'Palvelusetelikerroin 9,8',
        validityPeriod: '20.08.2021 – 24.11.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )

    await waitUntilEqual(
      async () => await assistanceNeeds.assistanceNeedVoucherCoefficients(1),
      {
        coefficient: 'Palvelusetelikerroin 4,3',
        validityPeriod: '04.02.2021 – 19.08.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )
  })

  test('assistance need voucher coefficient can be deleted', async () => {
    await setupPlacement('DAYCARE', true)
    await logUserIn(
      (
        await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
      ).data
    )
    await createVoucherCoefficient()

    await assistanceNeeds
      .assistanceNeedVoucherCoefficientActions(0)
      .deleteBtn.click()

    await assistanceNeeds.modalOkBtn.click()

    await waitUntilEqual(
      assistanceNeeds.assistanceNeedVoucherCoefficientCount,
      0
    )
  })
})
