// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  testDaycareGroup,
  Fixture,
  testAdult2,
  testAdult,
  testChild
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  resetServiceState
} from '../../generated/api-clients'
import CreateApplicationModal from '../../pages/employee/applications/create-application-modal'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let childInformationPage: ChildInformationPage

let fixtures: AreaAndPersonFixtures
let page: Page
let createApplicationModal: CreateApplicationModal
const now = HelsinkiDateTime.of(2023, 3, 15, 12, 0)

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()
  await Fixture.family({
    guardian: testAdult,
    otherGuardian: testAdult2,
    children: [testChild]
  }).save()
  await createDaycareGroups({ body: [testDaycareGroup] })
  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open({ mockedTime: now })
  await employeeLogin(page, admin)

  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.navigateToChild(testChild.id)

  const applications =
    await childInformationPage.openCollapsible('applications')
  createApplicationModal = await applications.openCreateApplicationModal()
})

const formatPersonName = (person: { firstName: string; lastName: string }) =>
  `${person.lastName} ${person.firstName}`

const formatPersonAddress = ({
  streetAddress,
  postalCode,
  postOffice
}: {
  streetAddress?: string
  postalCode?: string
  postOffice?: string
}) => `${streetAddress ?? ''}, ${postalCode ?? ''} ${postOffice ?? ''}`

describe('Employee - paper application', () => {
  test('Paper application can be created for guardian and child with ssn', async () => {
    const applicationEditPage = await createApplicationModal.submit()
    await applicationEditPage.assertGuardian(
      formatPersonName(testAdult),
      testAdult.ssn ?? '',
      formatPersonAddress(testAdult)
    )
  })

  test('Paper application can be created for other guardian and child with ssn', async () => {
    await createApplicationModal.selectGuardian(
      `${testAdult2.firstName} ${testAdult2.lastName}`
    )
    const applicationEditPage = await createApplicationModal.submit()

    await applicationEditPage.assertGuardian(
      formatPersonName(testAdult2),
      testAdult2.ssn ?? '',
      formatPersonAddress(testAdult2)
    )
  })

  test('Paper application can be created for other non guardian vtj person and child with ssn', async () => {
    const ssn = '270372-905L'
    const child = await Fixture.person()
      .with({
        ssn: '010106A981M',
        firstName: 'Lapsi',
        lastName: 'Korhonen-Hämäläinen'
      })
      .saveChild({ updateMockVtj: true })
    await Fixture.person()
      .with({
        ssn,
        firstName: 'Sirkka-Liisa Marja-Leena Minna-Mari Anna-Kaisa',
        lastName: 'Korhonen-Hämäläinen',
        streetAddress: 'Kamreerintie 2',
        postalCode: '00370',
        postOffice: 'Espoo'
      })
      .saveAdult({ updateMockVtjWithDependants: [child] })
    await createApplicationModal.selectVtjPersonAsGuardian(ssn)
    const applicationEditPage = await createApplicationModal.submit()

    await applicationEditPage.assertGuardian(
      'Korhonen-Hämäläinen Sirkka-Liisa Marja-Leena Minna-Mari Anna-Kaisa',
      '270372-905L',
      'Kamreerintie 2, 00370 Espoo'
    )
  })

  test('Paper application can be created with new guardian person', async () => {
    await createApplicationModal.selectCreateNewPersonAsGuardian(
      'Testi',
      'Testinen',
      '01.11.1980',
      'Katuosoite A1',
      '02200',
      'Espoo',
      '123456789',
      'testi.testinen@evaka.test'
    )
    const applicationEditPage = await createApplicationModal.submit()

    await applicationEditPage.assertGuardian(
      'Testinen Testi',
      '',
      'Katuosoite A1, 02200 Espoo'
    )
  })

  test('Service worker fills paper application with minimal info and saves it', async () => {
    const applicationEditPage = await createApplicationModal.submit()

    await applicationEditPage.fillStartDate(now.toLocalDate().format())
    await applicationEditPage.fillTimes()
    await applicationEditPage.pickUnit(fixtures.testDaycare.name)
    await applicationEditPage.fillApplicantPhoneAndEmail(
      '123456',
      'email@evaka.test'
    )
    const applicationViewPage = await applicationEditPage.saveApplication()
    await applicationViewPage.waitUntilLoaded()
  })

  test('Service worker fills paper application with second guardian contact info and agreement status', async () => {
    const applicationEditPage = await createApplicationModal.submit()

    await applicationEditPage.fillStartDate(now.toLocalDate().format())
    await applicationEditPage.fillTimes()
    await applicationEditPage.pickUnit(fixtures.testDaycare.name)
    await applicationEditPage.fillApplicantPhoneAndEmail(
      '123456',
      'email@evaka.test'
    )

    await applicationEditPage.fillSecondGuardianContactInfo(
      '654321',
      'second-email@evaka.test'
    )
    await applicationEditPage.setGuardianAgreementStatus('AGREED')

    const applicationViewPage = await applicationEditPage.saveApplication()
    await applicationViewPage.waitUntilLoaded()
  })

  test('Paper application due date is saved on submit', async () => {
    const applicationEditPage = await createApplicationModal.submit()
    await applicationEditPage.fillStartDate(now.toLocalDate().format())
    await applicationEditPage.fillTimes()
    await applicationEditPage.pickUnit(fixtures.testDaycare.name)
    await applicationEditPage.fillApplicantPhoneAndEmail(
      '123456',
      'email@evaka.test'
    )
    const dueDate = now.toLocalDate().addDays(7)
    await applicationEditPage.setDueDate(dueDate)
    const applicationViewPage = await applicationEditPage.saveApplication()
    await applicationViewPage.waitUntilLoaded()
    await applicationViewPage.assertDueDate(dueDate)
  })

  test('Service worker fills preschool application with minimal info and saves it', async () => {
    await createApplicationModal.selectApplicationType('PRESCHOOL')
    const applicationEditPage = await createApplicationModal.submit()

    await applicationEditPage.fillStartDate(now.toLocalDate().format())
    await applicationEditPage.checkConnectedDaycare()
    await applicationEditPage.fillTimes()
    await applicationEditPage.fillConnectedDaycarePreferredStartDate(
      now.toLocalDate().format()
    )
    await applicationEditPage.pickUnit(fixtures.testDaycare.name)
    await applicationEditPage.fillApplicantPhoneAndEmail(
      '123456',
      'email@evaka.test'
    )
    const applicationViewPage = await applicationEditPage.saveApplication()
    await applicationViewPage.waitUntilLoaded()
  })
})
