// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { runPendingAsyncJobs } from '../../dev-api'
import {
  familyWithTwoGuardians,
  Fixture,
  testAdultRestricted,
  testCareArea,
  testChildZeroYearOld,
  testDaycare,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  createVoucherValues,
  resetServiceState
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import ChildInformationPage from '../../pages/employee/child-information'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let guardianInformation: GuardianInformationPage
let childInformation: ChildInformationPage

let regularPerson: DevPerson
let fridgePartner: DevPerson
let child: DevPerson

const mockToday = LocalDate.of(2020, 1, 1)
const childZeroYo = Fixture.person({
  ...testChildZeroYearOld,
  dateOfBirth: mockToday.subWeeks(9),
  firstName: 'Vauva',
  id: '023c3d55-3bd5-494b-8996-60a3643fe94b'
}).data

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family(familyWithTwoGuardians).save()
  await Fixture.person(testAdultRestricted).saveAdult({
    updateMockVtjWithDependants: []
  })
  await createDefaultServiceNeedOptions()
  await createVoucherValues()
  regularPerson = familyWithTwoGuardians.guardian
  fridgePartner = familyWithTwoGuardians.otherGuardian
  child = familyWithTwoGuardians.children[0]
  await Fixture.person(childZeroYo).saveChild()
  await Fixture.feeThresholds({
    validDuring: new DateRange(LocalDate.of(2020, 1, 1), null),
    minIncomeThreshold2: 210200,
    minIncomeThreshold3: 271300,
    minIncomeThreshold4: 308000,
    minIncomeThreshold5: 344700,
    minIncomeThreshold6: 381300,
    maxIncomeThreshold2: 479900,
    maxIncomeThreshold3: 541000,
    maxIncomeThreshold4: 577700,
    maxIncomeThreshold5: 614400,
    maxIncomeThreshold6: 651000,
    incomeMultiplier2: 0.107,
    incomeMultiplier3: 0.107,
    incomeMultiplier4: 0.107,
    incomeMultiplier5: 0.107,
    incomeMultiplier6: 0.107,
    incomeThresholdIncrease6Plus: 14200,
    siblingDiscount2: 0.5,
    siblingDiscount2Plus: 0.8,
    minFee: 2700,
    maxFee: 28900,
    temporaryFee: 2800,
    temporaryFeePartDay: 1500,
    temporaryFeeSibling: 1500,
    temporaryFeeSiblingPartDay: 800
  }).save()

  const admin = await Fixture.employee({
    firstName: 'Seppo',
    lastName: 'Sorsa'
  })
    .admin()
    .save()

  page = await Page.open({
    mockedTime: mockToday.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await employeeLogin(page, admin)

  guardianInformation = new GuardianInformationPage(page)
  childInformation = new ChildInformationPage(page)
})

describe('Employee - Head of family details', () => {
  test('guardian has restriction details enabled', async () => {
    await guardianInformation.navigateToGuardian(testAdultRestricted.id)
    await guardianInformation.assertRestrictedDetails(true)
  })

  test('guardian does not have restriction details enabled', async () => {
    await guardianInformation.navigateToGuardian(regularPerson.id)
    await guardianInformation.assertRestrictedDetails(false)
  })

  test('Zero-year-old child is shown as age 0', async () => {
    await guardianInformation.navigateToGuardian(regularPerson.id)
    const children = await guardianInformation.openCollapsible('children')

    await children.addChild(childZeroYo.firstName, '01.01.2020')

    await children.verifyChildAge(0)

    const familyOverview =
      await guardianInformation.openCollapsible('familyOverview')
    await familyOverview.assertPerson({
      personId: childZeroYo.id,
      age: 0
    })
  })

  test('Retroactive fee decisions can start before the minimum fee decision date', async () => {
    await guardianInformation.navigateToGuardian(regularPerson.id)
    const children = await guardianInformation.openCollapsible('children')
    await children.addChild(child.firstName, '01.01.2020')

    await childInformation.navigateToChild(child.id)
    const placements = await childInformation.openCollapsible('placements')
    await placements.createNewPlacement({
      unitName: testDaycare.name,
      startDate: '01.01.2020',
      endDate: '31.07.2020'
    })

    await runPendingAsyncJobs(
      HelsinkiDateTime.fromLocal(mockToday, LocalTime.of(15, 0))
    )

    await guardianInformation.navigateToGuardian(regularPerson.id)
    const feeDecisions =
      await guardianInformation.openCollapsible('feeDecisions')
    await feeDecisions.assertFeeDecision(0, {
      startDate: '01.03.2020',
      endDate: '31.07.2020',
      status: 'Luonnos'
    })

    await feeDecisions.createRetroactiveFeeDecisions('01.01.2020')
    await feeDecisions.assertFeeDecision(0, {
      startDate: '01.01.2020',
      endDate: '31.07.2020',
      status: 'Luonnos'
    })
  })

  test('Added partner is shown in family overview', async () => {
    await guardianInformation.navigateToGuardian(regularPerson.id)
    const partnersSection =
      await guardianInformation.openCollapsible('partners')
    await partnersSection.addPartner(fridgePartner.firstName, '01.01.2020')

    const familyOverview =
      await guardianInformation.openCollapsible('familyOverview')
    await familyOverview.assertPerson({ personId: fridgePartner.id })
  })

  test('Partner metadata is shown in timeline', async () => {
    await guardianInformation.navigateToGuardian(regularPerson.id)
    const partnersSection =
      await guardianInformation.openCollapsible('partners')
    await partnersSection.addPartner(fridgePartner.firstName, '01.01.2020')

    const timelinePage = await guardianInformation.openTimeline()
    const timelineEvent = timelinePage.getTimelineEvent('partner', 0)
    await timelineEvent.expandEvent()
    await timelineEvent.assertMetadataContains('Luotu: 01.01.2020')
    await timelineEvent.assertMetadataContains(' - Käyttäjä Sorsa Seppo')
  })

  test('Manually added income is shown in family overview', async () => {
    await Fixture.fridgeChild({
      id: uuidv4(),
      childId: child.id,
      headOfChild: regularPerson.id,
      startDate: mockToday,
      endDate: mockToday
    }).save()

    const totalIncome = 100000
    const employee = await Fixture.employee().save()
    await Fixture.income({
      personId: regularPerson.id,
      effect: 'INCOME',
      validFrom: mockToday,
      validTo: mockToday.addYears(1),
      data: {
        MAIN_INCOME: {
          multiplier: 1,
          amount: totalIncome,
          monthlyAmount: totalIncome,
          coefficient: 'MONTHLY_NO_HOLIDAY_BONUS'
        }
      },
      updatedBy: employee.id
    }).save()

    const totalChildIncome = 1234

    await Fixture.income({
      personId: child.id,
      effect: 'INCOME',
      validFrom: mockToday,
      validTo: mockToday.addYears(1),
      data: {
        MAIN_INCOME: {
          multiplier: 1,
          amount: totalChildIncome,
          monthlyAmount: totalChildIncome,
          coefficient: 'MONTHLY_NO_HOLIDAY_BONUS'
        }
      },
      updatedBy: employee.id
    }).save()

    await guardianInformation.navigateToGuardian(regularPerson.id)
    const familyOverview =
      await guardianInformation.openCollapsible('familyOverview')
    await familyOverview.assertPerson({
      personId: regularPerson.id,
      incomeCents: totalIncome
    })

    await familyOverview.assertPerson({
      personId: child.id,
      incomeCents: totalChildIncome
    })
  })
})
