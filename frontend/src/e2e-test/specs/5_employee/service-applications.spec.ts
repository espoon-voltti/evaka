// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { CitizenNewServiceApplicationPage } from '../../pages/citizen/citizen-new-service-application'
import ChildInformationPage from '../../pages/employee/child-information'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let unitSupervisor: DevEmployee
let serviceNeedOption1: ServiceNeedOption
let serviceNeedOption2: ServiceNeedOption

describe('Service applications', () => {
  beforeEach(async () => {
    await resetServiceState()
    await Fixture.careArea(testCareArea).save()
    serviceNeedOption1 = await Fixture.serviceNeedOption({
      validPlacementType: 'DAYCARE',
      defaultOption: false,
      nameFi: 'Kokopäiväinen alle 30h/vko',
      nameSv: 'Kokopäiväinen alle 30h/vko (sv)',
      nameEn: 'Kokopäiväinen alle 30h/vko (en)'
    }).save()
    serviceNeedOption2 = await Fixture.serviceNeedOption({
      validPlacementType: 'DAYCARE',
      defaultOption: false,
      nameFi: 'Kokopäiväinen yli 30h/vko',
      nameSv: 'Kokopäiväinen yli 30h/vko (sv)',
      nameEn: 'Kokopäiväinen yli 30h/vko (en)'
    }).save()
    await Fixture.daycare(testDaycare).save()
    unitSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()
    await Fixture.family({
      guardian: testAdult,
      children: [testChild]
    }).save()
  })

  it('accept flow works', async () => {
    const mockedTime1 = LocalDate.of(2022, 3, 1).toHelsinkiDateTime(
      LocalTime.of(8, 0)
    )
    const placement = await Fixture.placement({
      childId: testChild.id,
      unitId: testDaycare.id,
      startDate: mockedTime1.toLocalDate().subMonths(1),
      endDate: mockedTime1.toLocalDate().addMonths(8),
      type: 'DAYCARE'
    }).save()
    const oldServiceNeed = await Fixture.serviceNeed({
      placementId: placement.id,
      startDate: placement.startDate,
      endDate: placement.endDate,
      optionId: serviceNeedOption1.id,
      confirmedBy: unitSupervisor.id
    }).save()

    let citizenPage = await Page.open({ mockedTime: mockedTime1 })

    await enduserLogin(citizenPage, testAdult)
    let citizenHeader = new CitizenHeader(citizenPage)
    await citizenHeader.openChildPage(testChild.id)
    let citizenChildPage = new CitizenChildPage(citizenPage)
    await citizenChildPage.openCollapsible(
      'service-need-and-daily-service-time'
    )
    await citizenChildPage.createServiceApplicationButton.click()

    const citizenNewServiceApplicationPage =
      new CitizenNewServiceApplicationPage(citizenPage)
    const startDate = mockedTime1.toLocalDate().addMonths(2).withDate(1)
    await citizenNewServiceApplicationPage.startDate.fill(startDate)
    await citizenNewServiceApplicationPage.serviceNeed.selectOption(
      serviceNeedOption2.id
    )
    const additionalInfo = 'Sain uuden työn'
    await citizenNewServiceApplicationPage.additionalInfo.fill(additionalInfo)
    await citizenNewServiceApplicationPage.createButton.click()

    await citizenChildPage.openCollapsible(
      'service-need-and-daily-service-time'
    )
    await citizenChildPage.openApplicationInfoBox.waitUntilVisible()
    await citizenChildPage.createServiceApplicationButton.waitUntilHidden()
    await citizenChildPage
      .serviceApplicationSentDate(0)
      .assertTextEquals(mockedTime1.toLocalDate().format())
    await citizenChildPage
      .serviceApplicationStartDate(0)
      .assertTextEquals(startDate.format())
    await citizenChildPage
      .serviceApplicationServiceNeed(0)
      .assertTextEquals(serviceNeedOption2.nameFi)
    await citizenChildPage
      .serviceApplicationStatus(0)
      .assertTextEquals('Ehdotettu')
    await citizenChildPage.serviceApplicationCancelButton(0).waitUntilVisible()
    await citizenChildPage.assertServiceApplicationDetails(
      0,
      additionalInfo,
      'Ehdotettu',
      null
    )
    await citizenPage.close()

    const mockedTime2 = LocalDate.of(2022, 3, 2).toHelsinkiDateTime(
      LocalTime.of(14, 0)
    )
    const employeePage = await Page.open({ mockedTime: mockedTime2 })
    await employeeLogin(employeePage, unitSupervisor)

    const unitPage = new UnitPage(employeePage)
    await unitPage.navigateToUnit(testDaycare.id)
    let unitServiceApplications = (await unitPage.openApplicationProcessTab())
      .serviceApplications
    await unitServiceApplications.assertApplicationCount(1)
    await unitServiceApplications.applicationChildLink(0).click()

    const childInformationPage = new ChildInformationPage(employeePage)
    await childInformationPage.waitUntilLoaded()
    const childServiceApplications = await childInformationPage.openCollapsible(
      'serviceApplications'
    )
    await childServiceApplications.undecidedApplication.waitUntilVisible()
    await childServiceApplications.assertUndecidedApplication(
      startDate.format(),
      serviceNeedOption2.nameFi,
      additionalInfo
    )
    await childServiceApplications.acceptApplication()
    await childServiceApplications.undecidedApplication.waitUntilHidden()
    await childServiceApplications.assertDecidedApplication(
      0,
      startDate.format(),
      serviceNeedOption2.nameFi,
      'Hyväksytty',
      mockedTime2
    )

    await employeePage.reload() // TODO: Remove when the page reload issue is fixed

    await childInformationPage.waitUntilLoaded()
    const placementsSection =
      await childInformationPage.openCollapsible('placements')
    await placementsSection.assertNthServiceNeedName(
      0,
      serviceNeedOption2.nameFi
    )
    await placementsSection.assertNthServiceNeedRange(
      0,
      new FiniteDateRange(startDate, placement.endDate)
    )
    await placementsSection.assertNthServiceNeedName(
      1,
      serviceNeedOption1.nameFi
    )
    await placementsSection.assertNthServiceNeedRange(
      1,
      new FiniteDateRange(oldServiceNeed.startDate, startDate.subDays(1))
    )

    await unitPage.navigateToUnit(testDaycare.id)
    unitServiceApplications = (await unitPage.openApplicationProcessTab())
      .serviceApplications
    await unitServiceApplications.assertApplicationCount(0)
    await employeePage.close()

    const mockedTime3 = LocalDate.of(2022, 3, 5).toHelsinkiDateTime(
      LocalTime.of(18, 0)
    )
    citizenPage = await Page.open({ mockedTime: mockedTime3 })

    await enduserLogin(citizenPage, testAdult)
    citizenHeader = new CitizenHeader(citizenPage)
    await citizenHeader.openChildPage(testChild.id)
    citizenChildPage = new CitizenChildPage(citizenPage)
    await citizenChildPage.openCollapsible(
      'service-need-and-daily-service-time'
    )
    await citizenChildPage.createServiceApplicationButton.assertDisabled(false)
    await citizenChildPage
      .serviceApplicationStatus(0)
      .assertTextEquals('Hyväksytty')
    await citizenChildPage.serviceApplicationCancelButton(0).waitUntilHidden()
    await citizenChildPage.assertServiceApplicationDetails(
      0,
      additionalInfo,
      'Hyväksytty',
      null
    )
  })

  it('reject flow works', async () => {
    const mockedTime1 = LocalDate.of(2022, 3, 1).toHelsinkiDateTime(
      LocalTime.of(8, 0)
    )
    await Fixture.placement({
      childId: testChild.id,
      unitId: testDaycare.id,
      startDate: mockedTime1.toLocalDate().subMonths(1),
      endDate: mockedTime1.toLocalDate().addMonths(8),
      type: 'DAYCARE'
    }).save()

    let citizenPage = await Page.open({ mockedTime: mockedTime1 })

    await enduserLogin(citizenPage, testAdult)
    let citizenHeader = new CitizenHeader(citizenPage)
    await citizenHeader.openChildPage(testChild.id)
    let citizenChildPage = new CitizenChildPage(citizenPage)
    await citizenChildPage.openCollapsible(
      'service-need-and-daily-service-time'
    )
    await citizenChildPage.createServiceApplicationButton.click()

    const citizenNewServiceApplicationPage =
      new CitizenNewServiceApplicationPage(citizenPage)
    const startDate = mockedTime1.toLocalDate().addMonths(2).withDate(1)
    await citizenNewServiceApplicationPage.startDate.fill(startDate)
    await citizenNewServiceApplicationPage.serviceNeed.selectOption(
      serviceNeedOption2.id
    )
    const additionalInfo = 'Ois kiva'
    await citizenNewServiceApplicationPage.additionalInfo.fill(additionalInfo)
    await citizenNewServiceApplicationPage.createButton.click()
    await citizenChildPage.openCollapsible(
      'service-need-and-daily-service-time'
    )
    await citizenChildPage.assertServiceApplicationDetails(
      0,
      additionalInfo,
      'Ehdotettu',
      null
    )
    await citizenPage.close()

    const mockedTime2 = LocalDate.of(2022, 3, 2).toHelsinkiDateTime(
      LocalTime.of(14, 0)
    )
    const employeePage = await Page.open({ mockedTime: mockedTime2 })
    await employeeLogin(employeePage, unitSupervisor)

    const childInformationPage = new ChildInformationPage(employeePage)
    await childInformationPage.navigateToChild(testChild.id)
    await childInformationPage.waitUntilLoaded()
    const childServiceApplications = await childInformationPage.openCollapsible(
      'serviceApplications'
    )
    await childServiceApplications.undecidedApplication.waitUntilVisible()
    const rejectReason = 'Huono syy'
    await childServiceApplications.rejectApplication(rejectReason)
    await childServiceApplications.undecidedApplication.waitUntilHidden()
    await childServiceApplications.assertDecidedApplication(
      0,
      startDate.format(),
      serviceNeedOption2.nameFi,
      'Hylätty',
      mockedTime2
    )
    await employeePage.close()

    const mockedTime3 = LocalDate.of(2022, 3, 5).toHelsinkiDateTime(
      LocalTime.of(18, 0)
    )
    citizenPage = await Page.open({ mockedTime: mockedTime3 })

    await enduserLogin(citizenPage, testAdult)
    citizenHeader = new CitizenHeader(citizenPage)
    await citizenHeader.openChildPage(testChild.id)
    citizenChildPage = new CitizenChildPage(citizenPage)
    await citizenChildPage.openCollapsible(
      'service-need-and-daily-service-time'
    )
    await citizenChildPage.createServiceApplicationButton.assertDisabled(false)
    await citizenChildPage
      .serviceApplicationStatus(0)
      .assertTextEquals('Hylätty')
    await citizenChildPage.serviceApplicationCancelButton(0).waitUntilHidden()

    await citizenChildPage.assertServiceApplicationDetails(
      0,
      additionalInfo,
      'Hylätty',
      rejectReason
    )
  })

  it('cancelling application', async () => {
    const mockedTime1 = LocalDate.of(2022, 3, 1).toHelsinkiDateTime(
      LocalTime.of(8, 0)
    )
    await Fixture.placement({
      childId: testChild.id,
      unitId: testDaycare.id,
      startDate: mockedTime1.toLocalDate().subMonths(1),
      endDate: mockedTime1.toLocalDate().addMonths(8),
      type: 'DAYCARE'
    }).save()

    const citizenPage = await Page.open({ mockedTime: mockedTime1 })

    await enduserLogin(citizenPage, testAdult)
    const citizenHeader = new CitizenHeader(citizenPage)
    await citizenHeader.openChildPage(testChild.id)
    const citizenChildPage = new CitizenChildPage(citizenPage)
    await citizenChildPage.openCollapsible(
      'service-need-and-daily-service-time'
    )
    await citizenChildPage.createServiceApplicationButton.click()

    const citizenNewServiceApplicationPage =
      new CitizenNewServiceApplicationPage(citizenPage)
    const startDate = mockedTime1.toLocalDate().addMonths(2).withDate(1)
    await citizenNewServiceApplicationPage.startDate.fill(startDate)
    await citizenNewServiceApplicationPage.serviceNeed.selectOption(
      serviceNeedOption2.id
    )
    const additionalInfo = 'Sain uuden työn'
    await citizenNewServiceApplicationPage.additionalInfo.fill(additionalInfo)
    await citizenNewServiceApplicationPage.createButton.click()

    await citizenChildPage.openCollapsible(
      'service-need-and-daily-service-time'
    )
    await citizenChildPage.openApplicationInfoBox.waitUntilVisible()
    await citizenChildPage.createServiceApplicationButton.waitUntilHidden()
    await citizenChildPage.serviceApplicationCancelButton(0).click()
    await citizenChildPage.createServiceApplicationButton.waitUntilVisible()
    await citizenChildPage.assertServiceApplicationsCount(0)
    await citizenPage.close()

    const mockedTime2 = LocalDate.of(2022, 3, 2).toHelsinkiDateTime(
      LocalTime.of(14, 0)
    )
    const employeePage = await Page.open({ mockedTime: mockedTime2 })
    await employeeLogin(employeePage, unitSupervisor)

    const unitPage = new UnitPage(employeePage)
    await unitPage.navigateToUnit(testDaycare.id)
    const unitServiceApplications = (await unitPage.openApplicationProcessTab())
      .serviceApplications
    await unitServiceApplications.assertApplicationCount(0)

    const childInformationPage = new ChildInformationPage(employeePage)
    await childInformationPage.navigateToChild(testChild.id)
    await childInformationPage.waitUntilLoaded()
    const childServiceApplications = await childInformationPage.openCollapsible(
      'serviceApplications'
    )
    await childServiceApplications.undecidedApplication.waitUntilHidden()
  })
})
