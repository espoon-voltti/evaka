// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  Fixture,
  preschoolTerm2021,
  testAdult,
  testAdult2,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import { getApplication, resetServiceState } from '../../generated/api-clients'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import {
  minimalDaycareFormWithServiceNeedOption,
  minimalPreschoolFormWithServiceNeedOption
} from '../../utils/application-forms'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const mockedDate = LocalDate.of(2021, 4, 1)
const mockedTime = HelsinkiDateTime.fromLocal(mockedDate, LocalTime.of(15, 0))

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild]
  }).save()
  await Fixture.person(testAdult2).saveAdult({
    updateMockVtjWithDependants: [testChild]
  })
})

describe('Citizen daycare applications', () => {
  let page: Page
  let header: CitizenHeader
  let applicationsPage: CitizenApplicationsPage

  beforeEach(async () => {
    page = await Page.open({
      mockedTime,
      citizenCustomizations: {
        featureFlags: {
          daycareApplication: { dailyTimes: false, serviceNeedOption: true }
        }
      }
    })
    await enduserLogin(page, testAdult)
    header = new CitizenHeader(page)
    applicationsPage = new CitizenApplicationsPage(page)
  })

  it('Minimal valid daycare application with service need option can be sent', async () => {
    await Fixture.serviceNeedOption({
      nameFi: 'Kokopäiväinen mennyt',
      validPlacementType: 'DAYCARE',
      validFrom: LocalDate.of(2020, 1, 1),
      validTo: LocalDate.of(2020, 12, 31)
    }).save()
    const fullTimeServiceNeedOption1 = await Fixture.serviceNeedOption({
      nameFi: 'Kokopäiväinen 1',
      validPlacementType: 'DAYCARE',
      validFrom: LocalDate.of(2020, 1, 1),
      validTo: null
    }).save()
    const fullTimeServiceNeedOption2 = await Fixture.serviceNeedOption({
      nameFi: 'Kokopäiväinen 2',
      validPlacementType: 'DAYCARE',
      validFrom: LocalDate.of(2020, 1, 1),
      validTo: LocalDate.of(2022, 12, 31)
    }).save()
    await Fixture.serviceNeedOption({
      nameFi: 'Kokopäiväinen tuleva',
      validPlacementType: 'DAYCARE',
      validFrom: LocalDate.of(2022, 1, 1)
    }).save()
    const partTimeServiceNeedOption1 = await Fixture.serviceNeedOption({
      nameFi: 'Osapäiväinen 1',
      validPlacementType: 'DAYCARE_PART_TIME',
      validFrom: LocalDate.of(2020, 1, 1),
      validTo: null
    }).save()
    const partTimeServiceNeedOption2 = await Fixture.serviceNeedOption({
      nameFi: 'Osapäiväinen 2',
      validPlacementType: 'DAYCARE_PART_TIME',
      validFrom: LocalDate.of(2020, 1, 1),
      validTo: LocalDate.of(2022, 12, 31)
    }).save()

    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()

    const applicationForm = minimalDaycareFormWithServiceNeedOption({
      serviceNeedOption: fullTimeServiceNeedOption1
    })
    await editorPage.fillData(applicationForm.form)
    await editorPage.assertServiceNeedOptions('DAYCARE', [
      fullTimeServiceNeedOption1,
      fullTimeServiceNeedOption2
    ])
    await editorPage.selectBooleanRadio('partTime', true)
    await editorPage.assertServiceNeedOptions('DAYCARE_PART_TIME', [
      partTimeServiceNeedOption1,
      partTimeServiceNeedOption2
    ])
    await editorPage.selectBooleanRadio('partTime', false)
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication({ applicationId })
    applicationForm.validateResult(application)
  })
})

describe('Citizen preschool applications', () => {
  let page: Page
  let header: CitizenHeader
  let applicationsPage: CitizenApplicationsPage

  beforeEach(async () => {
    await Fixture.preschoolTerm(preschoolTerm2021).save()

    page = await Page.open({
      mockedTime,
      citizenCustomizations: {
        featureFlags: {
          preschoolApplication: {
            connectedDaycarePreferredStartDate: true,
            serviceNeedOption: true
          }
        }
      }
    })
    await enduserLogin(page, testAdult)
    header = new CitizenHeader(page)
    applicationsPage = new CitizenApplicationsPage(page)
  })

  it('Minimal valid preschool application with service need option can be sent', async () => {
    await Fixture.serviceNeedOption({
      nameFi: 'Mennyt',
      validPlacementType: 'PRESCHOOL_DAYCARE',
      validFrom: LocalDate.of(2020, 1, 1),
      validTo: LocalDate.of(2020, 12, 31)
    }).save()
    const serviceNeedOption1 = await Fixture.serviceNeedOption({
      nameFi: 'Voimassa 1',
      validPlacementType: 'PRESCHOOL_DAYCARE',
      validFrom: LocalDate.of(2020, 1, 1),
      validTo: null
    }).save()
    const serviceNeedOption2 = await Fixture.serviceNeedOption({
      nameFi: 'Voimassa 2',
      validPlacementType: 'PRESCHOOL_DAYCARE',
      validFrom: LocalDate.of(2020, 1, 1),
      validTo: LocalDate.of(2022, 12, 31)
    }).save()
    await Fixture.serviceNeedOption({
      nameFi: 'Tuleva',
      validPlacementType: 'PRESCHOOL_DAYCARE',
      validFrom: LocalDate.of(2022, 1, 1)
    }).save()

    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'PRESCHOOL'
    )
    const applicationId = editorPage.getNewApplicationId()

    const applicationForm = minimalPreschoolFormWithServiceNeedOption({
      serviceNeedOption: serviceNeedOption1
    })
    await editorPage.fillData(applicationForm.form)
    await editorPage.assertServiceNeedOptions('PRESCHOOL_DAYCARE', [
      serviceNeedOption1,
      serviceNeedOption2
    ])
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication({ applicationId })
    applicationForm.validateResult(application)
  })
})
