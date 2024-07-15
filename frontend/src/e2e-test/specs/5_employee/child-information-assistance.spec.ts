// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { AssistanceNeedDecisionStatus } from 'lib-common/generated/api-types/assistanceneed'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  testDaycare,
  testDaycareGroup,
  familyWithTwoGuardians,
  Fixture,
  testCareArea,
  testDaycarePrivateVoucher
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import ChildInformationPage, {
  AssistanceSection
} from '../../pages/employee/child-information'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformationPage: ChildInformationPage
let assistance: AssistanceSection
let childId: UUID
let unitId: UUID
let voucherUnitId: UUID
let admin: DevEmployee

beforeEach(async () => {
  await resetServiceState()

  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.daycare(testDaycarePrivateVoucher).save()
  await Fixture.family(familyWithTwoGuardians).save()
  await createDefaultServiceNeedOptions()
  await createDaycareGroups({ body: [testDaycareGroup] })

  unitId = testDaycare.id
  voucherUnitId = testDaycarePrivateVoucher.id
  childId = familyWithTwoGuardians.children[0].id
  page = await Page.open()
  admin = await Fixture.employeeAdmin().save()
})

const setupPlacement = async (type: PlacementType, voucher = false) => {
  const fixture = Fixture.placement({
    childId,
    unitId: voucher ? voucherUnitId : unitId,
    startDate: LocalDate.todayInSystemTz(),
    endDate: LocalDate.todayInSystemTz(),
    type
  })
  await fixture.save()
  return fixture
}

const logUserIn = async (user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  assistance = await childInformationPage.openCollapsible('assistance')
}

describe('Child Information assistance functionality for employees', () => {
  describe('assistance factor', () => {
    it('can be added', async () => {
      const validDuring = (await setupPlacement('DAYCARE')).dateRange()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      await assistance.createAssistanceFactorButton.click()
      const form = assistance.assistanceFactorForm
      await form.capacityFactor.fill('1.5')
      await form.startDate.fill(validDuring.start.format())
      await form.endDate.fill(validDuring.end.format())
      await form.save.click()

      const row = assistance.assistanceFactorRow(0)
      await row.capacityFactor.assertTextEquals('1,5')
      await row.validDuring.assertTextEquals(validDuring.format())
    })

    it('can be edited', async () => {
      const validDuring = (await setupPlacement('DAYCARE')).dateRange()
      await Fixture.assistanceFactor({
        childId,
        validDuring,
        capacityFactor: 1.5
      }).save()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      const row = assistance.assistanceFactorRow(0)
      await row.capacityFactor.assertTextEquals('1,5')

      await row.edit.click()
      const form = assistance.assistanceFactorForm
      await form.capacityFactor.fill('2.5')
      await form.save.click()

      await row.capacityFactor.assertTextEquals('2,5')
    })

    it('can be deleted', async () => {
      const validDuring = (await setupPlacement('DAYCARE')).dateRange()
      await Fixture.assistanceFactor({
        childId,
        validDuring,
        capacityFactor: 1.5
      }).save()
      await Fixture.assistanceFactor({
        childId,
        validDuring: new FiniteDateRange(
          validDuring.end.addDays(1),
          validDuring.end.addDays(7)
        ),
        capacityFactor: 2.5
      }).save()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      await assistance.assertAssistanceFactorCount(2)
      await assistance.assistanceFactorRow(0).delete()

      await assistance.assertAssistanceFactorCount(1)
      await assistance
        .assistanceFactorRow(0)
        .capacityFactor.assertTextEquals('1,5')
    })
  })

  describe('daycare assistance', () => {
    it('can be added', async () => {
      const validDuring = (await setupPlacement('DAYCARE')).dateRange()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      await assistance.createDaycareAssistanceButton.click()
      const form = assistance.daycareAssistanceForm
      await form.level.selectOption('GENERAL_SUPPORT')
      await form.fillValidDuring(validDuring)
      await form.save.click()

      const row = assistance.daycareAssistanceRow(0)
      await row.level.assertTextEquals('Yleinen tuki, ei päätöstä')
      await row.validDuring.assertTextEquals(validDuring.format())
    })

    it('can be edited', async () => {
      const validDuring = (await setupPlacement('DAYCARE')).dateRange()
      await Fixture.daycareAssistance({
        childId,
        validDuring,
        level: 'GENERAL_SUPPORT'
      }).save()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      const row = assistance.daycareAssistanceRow(0)
      await row.level.assertTextEquals('Yleinen tuki, ei päätöstä')

      await row.edit.click()
      const form = assistance.daycareAssistanceForm
      await form.level.selectOption('SPECIAL_SUPPORT')
      await form.save.click()

      await row.level.assertTextEquals('Erityinen tuki')
    })

    it('can be deleted', async () => {
      const validDuring = (await setupPlacement('DAYCARE')).dateRange()
      await Fixture.daycareAssistance({
        childId,
        validDuring,
        level: 'GENERAL_SUPPORT'
      }).save()
      await Fixture.daycareAssistance({
        childId,
        validDuring: new FiniteDateRange(
          validDuring.end.addDays(1),
          validDuring.end.addDays(7)
        ),
        level: 'INTENSIFIED_SUPPORT'
      }).save()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      await assistance.assertDaycareAssistanceCount(2)
      await assistance.daycareAssistanceRow(0).delete()

      await assistance.assertDaycareAssistanceCount(1)
      await assistance
        .daycareAssistanceRow(0)
        .level.assertTextEquals('Yleinen tuki, ei päätöstä')
    })
  })

  describe('preschool assistance', () => {
    it('can be added', async () => {
      const validDuring = (await setupPlacement('PRESCHOOL')).dateRange()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      await assistance.createPreschoolAssistanceButton.click()
      const form = assistance.preschoolAssistanceForm
      await form.level.selectOption('SPECIAL_SUPPORT')
      await form.fillValidDuring(validDuring)
      await form.save.click()

      const row = assistance.preschoolAssistanceRow(0)
      await row.level.assertTextEquals(
        'Erityinen tuki ilman pidennettyä oppivelvollisuutta'
      )
      await row.validDuring.assertTextEquals(validDuring.format())
    })

    it('can be edited', async () => {
      const validDuring = (await setupPlacement('PRESCHOOL')).dateRange()
      await Fixture.preschoolAssistance({
        childId,
        validDuring,
        level: 'SPECIAL_SUPPORT'
      }).save()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      const row = assistance.preschoolAssistanceRow(0)
      await row.level.assertTextEquals(
        'Erityinen tuki ilman pidennettyä oppivelvollisuutta'
      )

      await row.edit.click()
      const form = assistance.preschoolAssistanceForm
      await form.level.selectOption('INTENSIFIED_SUPPORT')
      await form.save.click()

      await row.level.assertTextEquals('Tehostettu tuki')
    })

    it('can be deleted', async () => {
      const validDuring = (await setupPlacement('PRESCHOOL')).dateRange()
      await Fixture.preschoolAssistance({
        childId,
        validDuring,
        level: 'INTENSIFIED_SUPPORT'
      }).save()
      await Fixture.preschoolAssistance({
        childId,
        validDuring: new FiniteDateRange(
          validDuring.end.addDays(1),
          validDuring.end.addDays(7)
        ),
        level: 'SPECIAL_SUPPORT'
      }).save()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      await assistance.assertPreschoolAssistanceCount(2)
      await assistance.preschoolAssistanceRow(0).delete()

      await assistance.assertPreschoolAssistanceCount(1)
      await assistance
        .preschoolAssistanceRow(0)
        .level.assertTextEquals('Tehostettu tuki')
    })
  })

  describe('other assistance measure', () => {
    it('can be added', async () => {
      const validDuring = (await setupPlacement('DAYCARE')).dateRange()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      await assistance.createOtherAssistanceMeasureButton.click()
      const form = assistance.otherAssistanceMeasureForm
      await form.type.selectOption('ACCULTURATION_SUPPORT')
      await form.fillValidDuring(validDuring)
      await form.save.click()

      const row = assistance.otherAssistanceMeasureRow(0)
      await row.type.assertTextEquals('Lapsen kotoutumisen tuki (ELY)')
      await row.validDuring.assertTextEquals(validDuring.format())
    })

    it('can be edited', async () => {
      const validDuring = (await setupPlacement('DAYCARE')).dateRange()
      await Fixture.otherAssistanceMeasure({
        childId,
        validDuring,
        type: 'TRANSPORT_BENEFIT'
      }).save()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      const row = assistance.otherAssistanceMeasureRow(0)
      await row.type.assertTextEquals('Kuljetusetu (esioppilailla Koski-tieto)')

      await row.edit.click()
      const form = assistance.otherAssistanceMeasureForm
      await form.type.selectOption('ACCULTURATION_SUPPORT')
      await form.save.click()

      await row.type.assertTextEquals('Lapsen kotoutumisen tuki (ELY)')
    })

    it('can be deleted', async () => {
      const validDuring = (await setupPlacement('DAYCARE')).dateRange()
      await Fixture.otherAssistanceMeasure({
        childId,
        validDuring,
        type: 'TRANSPORT_BENEFIT'
      }).save()
      await Fixture.otherAssistanceMeasure({
        childId,
        validDuring: new FiniteDateRange(
          validDuring.end.addDays(1),
          validDuring.end.addDays(7)
        ),
        type: 'ACCULTURATION_SUPPORT'
      }).save()
      await logUserIn(await Fixture.employeeUnitSupervisor(unitId).save())

      await assistance.assertOtherAssistanceMeasureCount(2)
      await assistance.otherAssistanceMeasureRow(0).delete()

      await assistance.assertOtherAssistanceMeasureCount(1)

      await assistance
        .otherAssistanceMeasureRow(0)
        .type.assertTextEquals('Kuljetusetu (esioppilailla Koski-tieto)')
    })
  })

  test('assistance factor completely before preschool for a child in preschool is not shown for unit supervisor', async () => {
    await setupPlacement('PRESCHOOL')
    const unitSupervisor = await Fixture.employeeUnitSupervisor(unitId).save()

    await Fixture.assistanceFactor({
      capacityFactor: 0.5,
      childId: childId,
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz().subDays(2),
        LocalDate.todayInSystemTz().subDays(2)
      ),
      modifiedBy: unitSupervisor.id
    }).save()

    // Shown because overlaps preschool placement
    await Fixture.assistanceFactor({
      capacityFactor: 1.0,
      childId: childId,
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz().subDays(1),
        LocalDate.todayInSystemTz()
      ),
      modifiedBy: unitSupervisor.id
    }).save()

    await Fixture.assistanceFactor({
      capacityFactor: 2.0,
      childId: childId,
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz().addDays(1),
        LocalDate.todayInSystemTz().addDays(1)
      ),
      modifiedBy: unitSupervisor.id
    }).save()

    await logUserIn(unitSupervisor)

    await assistance.assertAssistanceFactorCount(2)
    await assistance.assistanceFactorRow(0).capacityFactor.assertTextEquals('2')
    await assistance.assistanceFactorRow(1).capacityFactor.assertTextEquals('1')
  })

  test('assistance factor for preschool for a child in preschool is shown for unit manager', async () => {
    await setupPlacement('PRESCHOOL')
    const unitSupervisor = await Fixture.employeeUnitSupervisor(unitId).save()

    await Fixture.assistanceFactor({
      childId: childId,
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz().addDays(1)
      ),
      modifiedBy: unitSupervisor.id
    }).save()

    await logUserIn(unitSupervisor)
    await assistance.assertAssistanceFactorCount(1)
  })

  test('assistance need before preschool for a child in preschool is shown for admin', async () => {
    await setupPlacement('PRESCHOOL')
    await Fixture.assistanceFactor({
      childId: childId,
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz().subDays(1),
        LocalDate.todayInSystemTz()
      ),
      modifiedBy: admin.id
    }).save()

    await logUserIn(admin)
    await assistance.assertAssistanceFactorCount(1)
  })

  test('assistance factor before preschool for a child in preschool is shown for Special Education Teacher', async () => {
    await setupPlacement('PRESCHOOL')
    const specialEducationTeacher =
      await Fixture.employeeSpecialEducationTeacher(unitId).save()

    await Fixture.assistanceFactor({
      childId: childId,
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz().subDays(1),
        LocalDate.todayInSystemTz()
      ),
      modifiedBy: specialEducationTeacher.id
    }).save()

    await Fixture.assistanceFactor({
      childId: childId,
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz().addDays(1),
        LocalDate.todayInSystemTz().addDays(2)
      ),
      modifiedBy: specialEducationTeacher.id
    }).save()

    await logUserIn(specialEducationTeacher)
    await assistance.assertAssistanceFactorCount(2)
  })
})

describe('Child assistance need decisions for employees', () => {
  test('Shows an empty draft in the list', async () => {
    await Fixture.assistanceNeedDecision({ childId }).save()

    await logUserIn(admin)
    await assistance.waitUntilAssistanceNeedDecisionsLoaded()

    const decision = await assistance.assistanceNeedDecisions(0)

    expect(decision.date).toEqual('02.01.2019 –')
    expect(decision.unitName).toEqual('-')
    expect(decision.sentDate).toEqual('-')
    expect(decision.decisionMadeDate).toEqual('-')
    expect(decision.status).toEqual('DRAFT')
    expect(decision.actionCount).toEqual(2)
  })

  test('Shows a filled in draft in the list', async () => {
    const serviceWorker = await Fixture.employeeServiceWorker().save()
    await Fixture.preFilledAssistanceNeedDecision({
      childId,
      selectedUnit: testDaycare.id,
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
          personId: familyWithTwoGuardians.guardian.id,
          isHeard: true,
          name: '',
          details: 'Guardian 1 details'
        }
      ],
      sentForDecision: LocalDate.of(2020, 5, 11),
      validityPeriod: new DateRange(
        LocalDate.of(2020, 7, 1),
        LocalDate.of(2020, 12, 11)
      ),
      decisionMade: LocalDate.of(2020, 6, 2),
      assistanceLevels: ['ASSISTANCE_SERVICES_FOR_TIME']
    }).save()

    await logUserIn(admin)
    await assistance.waitUntilAssistanceNeedDecisionsLoaded()

    const decision = await assistance.assistanceNeedDecisions(0)

    expect(decision.date).toEqual('01.07.2020 – 11.12.2020')
    expect(decision.unitName).toEqual(testDaycare.name)
    expect(decision.sentDate).toEqual('11.05.2020')
    expect(decision.decisionMadeDate).toEqual('02.06.2020')
    expect(decision.status).toEqual('DRAFT')
    expect(decision.actionCount).toEqual(2)
  })

  test('Hides edit and delete actions for non-draft/non-workable decisions', async () => {
    await Fixture.preFilledAssistanceNeedDecision({
      childId,
      status: 'ACCEPTED'
    }).save()
    await Fixture.preFilledAssistanceNeedDecision({
      childId,
      status: 'REJECTED'
    }).save()

    await logUserIn(admin)
    await assistance.waitUntilAssistanceNeedDecisionsLoaded()

    const acceptedDecision = await assistance.assistanceNeedDecisions(0)
    expect(acceptedDecision.status).toEqual('ACCEPTED')
    expect(acceptedDecision.actionCount).toEqual(0)

    const rejectedDecision = await assistance.assistanceNeedDecisions(1)
    expect(rejectedDecision.status).toEqual('REJECTED')
    expect(rejectedDecision.actionCount).toEqual(0)
  })

  test('Shows acceted decisions for staff', async () => {
    const staff = await Fixture.employeeStaff(unitId).save()
    await setupPlacement('DAYCARE')
    const statuses: AssistanceNeedDecisionStatus[] = [
      'DRAFT',
      'NEEDS_WORK',
      'ACCEPTED',
      'REJECTED',
      'ANNULLED'
    ]
    await Promise.all(
      statuses.map((status) =>
        Fixture.preFilledAssistanceNeedDecision({
          childId,
          status,
          annulmentReason: status === 'ANNULLED' ? 'not shown to staff' : ''
        }).save()
      )
    )

    await logUserIn(staff)
    await assistance.waitUntilAssistanceNeedDecisionsLoaded()
    await assistance.assertAssistanceNeedDecisionCount(1)

    const acceptedDecision = await assistance.assistanceNeedDecisions(0)
    expect(acceptedDecision.status).toEqual('ACCEPTED')
    expect(acceptedDecision.actionCount).toEqual(0)
  })
})

describe('Child assistance need voucher coefficients for employees', () => {
  async function createVoucherCoefficient(
    coefficient: string,
    validityPeriodStart: string,
    validityPeriodEnd: string
  ) {
    await assistance.createAssistanceNeedVoucherCoefficientBtn.click()
    const form = assistance.assistanceNeedVoucherCoefficientForm(
      assistance.createAssistanceNeedVoucherCoefficientForm
    )
    await form.coefficientInput.type(coefficient)
    await form.validityPeriod.startInput.clear()
    await form.validityPeriod.startInput.type(validityPeriodStart)
    await form.validityPeriod.endInput.clear()
    await form.validityPeriod.endInput.type(validityPeriodEnd)
    await form.saveBtn.click()
  }

  test('assistance need voucher coefficient can be added', async () => {
    await setupPlacement('DAYCARE', true)
    await logUserIn(
      await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
    )

    await createVoucherCoefficient('4,3', '04.02.2021', '01.09.2021')

    await waitUntilEqual(
      async () => await assistance.assistanceNeedVoucherCoefficients(0),
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
      await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
    )

    await createVoucherCoefficient('4,3', '04.02.2021', '01.09.2021')
    await createVoucherCoefficient('1,2', '16.06.2021', '05.10.2021')

    await waitUntilEqual(
      async () => await assistance.assistanceNeedVoucherCoefficients(0),
      {
        coefficient: 'Palvelusetelikerroin 1,2',
        validityPeriod: '16.06.2021 – 05.10.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )

    await waitUntilEqual(
      async () => await assistance.assistanceNeedVoucherCoefficients(1),
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
      await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
    )

    await createVoucherCoefficient('4,3', '04.02.2021', '01.09.2021')

    await assistance.assistanceNeedVoucherCoefficientActions(0).editBtn.click()
    const form = assistance.assistanceNeedVoucherCoefficientForm(
      assistance.editAssistanceNeedVoucherCoefficientForm
    )
    await form.coefficientInput.clear()
    await form.coefficientInput.type('9,8')
    await form.validityPeriod.startInput.clear()
    await form.validityPeriod.startInput.type('10.02.2021')
    await form.validityPeriod.endInput.clear()
    await form.validityPeriod.endInput.type('21.11.2021')
    await form.saveBtn.click()

    await waitUntilEqual(
      async () => await assistance.assistanceNeedVoucherCoefficients(0),
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
      await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
    )

    await createVoucherCoefficient('4,3', '04.02.2021', '01.09.2021')
    await createVoucherCoefficient('1,9', '29.11.2021', '30.12.2021')

    await waitUntilEqual(
      async () => await assistance.assistanceNeedVoucherCoefficients(0),
      {
        coefficient: 'Palvelusetelikerroin 1,9',
        validityPeriod: '29.11.2021 – 30.12.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )

    await assistance.assistanceNeedVoucherCoefficientActions(0).editBtn.click()
    const form = assistance.assistanceNeedVoucherCoefficientForm(
      assistance.editAssistanceNeedVoucherCoefficientForm
    )
    await form.coefficientInput.clear()
    await form.coefficientInput.type('9,8')
    await form.validityPeriod.startInput.clear()
    await form.validityPeriod.startInput.type('20.08.2021')
    await form.validityPeriod.endInput.clear()
    await form.validityPeriod.endInput.type('24.11.2021')
    await form.saveBtn.click()

    await waitUntilEqual(
      async () => await assistance.assistanceNeedVoucherCoefficients(0),
      {
        coefficient: 'Palvelusetelikerroin 9,8',
        validityPeriod: '20.08.2021 – 24.11.2021',
        status: 'ENDED',
        actionCount: 2
      }
    )

    await waitUntilEqual(
      async () => await assistance.assistanceNeedVoucherCoefficients(1),
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
      await Fixture.employeeSpecialEducationTeacher(voucherUnitId).save()
    )

    await createVoucherCoefficient('4,3', '04.02.2021', '01.09.2021')

    await assistance
      .assistanceNeedVoucherCoefficientActions(0)
      .deleteBtn.click()

    await assistance.modalOkBtn.click()

    await waitUntilEqual(assistance.assistanceNeedVoucherCoefficientCount, 0)
  })
})
