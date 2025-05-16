// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

beforeEach(() => resetServiceState())

describe('Absence application', () => {
  const mockedTime = HelsinkiDateTime.of(2025, 5, 5, 13, 0)

  const adult = Fixture.person({ ssn: '070644-937X' })
  const child = Fixture.person()

  test('accepted flow', async () => {
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(unit.id)
      .save()
    await Fixture.family({ guardian: adult, children: [child] }).save()
    await Fixture.placement({
      type: 'PRESCHOOL',
      childId: child.id,
      unitId: unit.id,
      startDate: mockedTime.toLocalDate(),
      endDate: mockedTime.toLocalDate()
    }).save()

    const citizenPage = await Page.open({
      mockedTime,
      citizenCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await enduserLogin(citizenPage, adult)
    const citizenHeader = new CitizenHeader(citizenPage)
    const citizenChildPage = await citizenHeader.openChildPage(child.id)
    await citizenChildPage.openCollapsible('absence-applications')
    await citizenChildPage.assertAbsenceApplications([])
    await createAbsenceApplication(
      citizenChildPage,
      new FiniteDateRange(mockedTime.toLocalDate(), mockedTime.toLocalDate()),
      'hello world'
    )
    await citizenChildPage.openCollapsible('absence-applications')
    await citizenChildPage.assertAbsenceApplications([
      {
        range: '05.05.2025 - 05.05.2025',
        status: 'Odottaa päätöstä',
        description: 'hello world'
      }
    ])

    const employeePage = await Page.open({
      mockedTime,
      employeeCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await employeeLogin(employeePage, unitSupervisor)
    await employeePage.goto(`${config.employeeUrl}/units/${unit.id}`)
    const unitPage = new UnitPage(employeePage)
    const applicationProcessTab = await unitPage.openApplicationProcessTab()
    await applicationProcessTab.assertAbsenceApplications([
      `${child.lastName} ${child.firstName}\t05.05.2025 - 05.05.2025\thello world`
    ])
    const childPage = await applicationProcessTab.openAbsenceApplication(0)
    const childAbsenceApplications = await childPage.openCollapsible(
      'absenceApplications'
    )
    await childAbsenceApplications.assertCompleted([])
    await childAbsenceApplications.accept(0)
    await childAbsenceApplications.assertIncompleted([])
    await childAbsenceApplications.assertCompleted([
      `05.05.2025 - 05.05.2025\thello world\t${adult.lastName} ${adult.firstName} (huoltaja)\tHyväksytty, 05.05.2025`
    ])
  })

  test('rejected flow', async () => {
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(unit.id)
      .save()
    await Fixture.family({ guardian: adult, children: [child] }).save()
    await Fixture.placement({
      type: 'PRESCHOOL',
      childId: child.id,
      unitId: unit.id,
      startDate: mockedTime.toLocalDate(),
      endDate: mockedTime.toLocalDate()
    }).save()

    const citizenPage = await Page.open({
      mockedTime,
      citizenCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await enduserLogin(citizenPage, adult)
    const citizenHeader = new CitizenHeader(citizenPage)
    const citizenChildPage = await citizenHeader.openChildPage(child.id)
    await citizenChildPage.openCollapsible('absence-applications')
    await citizenChildPage.assertAbsenceApplications([])
    await createAbsenceApplication(
      citizenChildPage,
      new FiniteDateRange(mockedTime.toLocalDate(), mockedTime.toLocalDate()),
      'hello world'
    )
    await citizenChildPage.openCollapsible('absence-applications')
    await citizenChildPage.assertAbsenceApplications([
      {
        range: '05.05.2025 - 05.05.2025',
        status: 'Odottaa päätöstä',
        description: 'hello world'
      }
    ])

    const employeePage = await Page.open({
      mockedTime,
      employeeCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await employeeLogin(employeePage, unitSupervisor)
    await employeePage.goto(`${config.employeeUrl}/units/${unit.id}`)
    const unitPage = new UnitPage(employeePage)
    const applicationProcessTab = await unitPage.openApplicationProcessTab()
    await applicationProcessTab.assertAbsenceApplications([
      `${child.lastName} ${child.firstName}\t05.05.2025 - 05.05.2025\thello world`
    ])
    const childPage = await applicationProcessTab.openAbsenceApplication(0)
    const childAbsenceApplications = await childPage.openCollapsible(
      'absenceApplications'
    )
    await childAbsenceApplications.assertCompleted([])
    const rejectModal = await childAbsenceApplications.openRejectModal(0)
    await rejectModal.reason.fill('hello world')
    await rejectModal.submit()
    await childAbsenceApplications.assertIncompleted([])
    await childAbsenceApplications.assertCompleted([
      `05.05.2025 - 05.05.2025\thello world\t${adult.lastName} ${adult.firstName} (huoltaja)\tHylätty, 05.05.2025\nSyy: hello world`
    ])
    await employeePage.goBack()
    await applicationProcessTab.assertAbsenceApplications([])
  })

  test('delete flow', async () => {
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(unit.id)
      .save()
    await Fixture.family({ guardian: adult, children: [child] }).save()
    await Fixture.placement({
      type: 'PRESCHOOL',
      childId: child.id,
      unitId: unit.id,
      startDate: mockedTime.toLocalDate(),
      endDate: mockedTime.toLocalDate()
    }).save()

    const citizenPage = await Page.open({
      mockedTime,
      citizenCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await enduserLogin(citizenPage, adult)
    const citizenHeader = new CitizenHeader(citizenPage)
    const citizenChildPage = await citizenHeader.openChildPage(child.id)
    await citizenChildPage.openCollapsible('absence-applications')
    await citizenChildPage.assertAbsenceApplications([])
    await createAbsenceApplication(
      citizenChildPage,
      new FiniteDateRange(mockedTime.toLocalDate(), mockedTime.toLocalDate()),
      'hello world'
    )
    await citizenChildPage.openCollapsible('absence-applications')
    await citizenChildPage.assertAbsenceApplications([
      {
        range: '05.05.2025 - 05.05.2025',
        status: 'Odottaa päätöstä',
        description: 'hello world'
      }
    ])
    await citizenChildPage.deleteAbsenceApplication(0)
    await citizenChildPage.assertAbsenceApplications([])

    const employeePage = await Page.open({
      mockedTime,
      employeeCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await employeeLogin(employeePage, unitSupervisor)
    await employeePage.goto(`${config.employeeUrl}/units/${unit.id}`)
    const unitPage = new UnitPage(employeePage)
    const applicationProcessTab = await unitPage.openApplicationProcessTab()
    await applicationProcessTab.assertAbsenceApplications([])
  })
})

const createAbsenceApplication = async (
  childPage: CitizenChildPage,
  range: FiniteDateRange,
  description: string
) => {
  const newAbsenceApplicationPage = await childPage.newAbsenceApplicationPage()
  await newAbsenceApplicationPage.startDate.fill(range.start)
  await newAbsenceApplicationPage.endDate.fill(range.end)
  await newAbsenceApplicationPage.description.fill(description)
  await newAbsenceApplicationPage.createButton.assertDisabled(true)
  await newAbsenceApplicationPage.confirmation.click()
  await newAbsenceApplicationPage.createButton.assertDisabled(false)
  await newAbsenceApplicationPage.createButton.click()
}
