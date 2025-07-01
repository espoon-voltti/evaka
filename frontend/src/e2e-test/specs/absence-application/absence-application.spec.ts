// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
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

  test('absence modal shows link to absence application page for child in preschool', async () => {
    const mockedDate = mockedTime.toLocalDate()
    const termRange = new FiniteDateRange(mockedDate, mockedDate.addYears(1))
    await Fixture.preschoolTerm({
      extendedTerm: termRange,
      finnishPreschool: termRange,
      swedishPreschool: termRange,
      applicationPeriod: termRange.withStart(mockedDate.subMonths(2))
    }).save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({
      areaId: area.id,
      enabledPilotFeatures: ['RESERVATIONS']
    }).save()
    await Fixture.family({ guardian: adult, children: [child] }).save()
    await Fixture.placement({
      type: 'PRESCHOOL',
      childId: child.id,
      unitId: unit.id,
      startDate: termRange.start,
      endDate: termRange.end
    }).save()

    const citizenPage = await Page.open({
      mockedTime,
      citizenCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await enduserLogin(citizenPage, adult)
    const citizenHeader = new CitizenHeader(citizenPage)
    await citizenHeader.selectTab('calendar')
    const citizenCalendarPage = new CitizenCalendarPage(citizenPage, 'desktop')
    const absenceModal = await citizenCalendarPage.openAbsencesModal()
    const startDate = termRange.start.addMonths(1)
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1))
    )
    await absenceModal.selectAbsenceType('OTHER_ABSENCE')
    await absenceModal.tooManyAbsencesError(child.id).waitUntilVisible()
    await absenceModal.modalSendButton.assertDisabled(true)
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1).subDays(1))
    )
    await absenceModal.tooManyAbsencesError(child.id).waitUntilHidden()
    await absenceModal.modalSendButton.assertDisabled(false)
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1))
    )
    await absenceModal.tooManyAbsencesError(child.id).waitUntilVisible()
    await absenceModal.modalSendButton.assertDisabled(true)
    await absenceModal.selectAbsenceType('SICKLEAVE')
    await absenceModal.tooManyAbsencesError(child.id).waitUntilHidden()
    await absenceModal.modalSendButton.assertDisabled(false)
  })

  test('absence modal shows link to absence application page handling preschool term break', async () => {
    const mockedDate = mockedTime.toLocalDate()
    const termRange = new FiniteDateRange(mockedDate, mockedDate.addYears(1))
    const startDate = termRange.start.addMonths(1)
    await Fixture.preschoolTerm({
      extendedTerm: termRange,
      finnishPreschool: termRange,
      swedishPreschool: termRange,
      applicationPeriod: termRange.withStart(mockedDate.subMonths(2)),
      termBreaks: [new FiniteDateRange(startDate, startDate)]
    }).save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({
      areaId: area.id,
      enabledPilotFeatures: ['RESERVATIONS']
    }).save()
    await Fixture.family({ guardian: adult, children: [child] }).save()
    await Fixture.placement({
      type: 'PRESCHOOL',
      childId: child.id,
      unitId: unit.id,
      startDate: termRange.start,
      endDate: termRange.end
    }).save()

    const citizenPage = await Page.open({
      mockedTime,
      citizenCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await enduserLogin(citizenPage, adult)
    const citizenHeader = new CitizenHeader(citizenPage)
    await citizenHeader.selectTab('calendar')
    const citizenCalendarPage = new CitizenCalendarPage(citizenPage, 'desktop')
    const absenceModal = await citizenCalendarPage.openAbsencesModal()
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1).subDays(1))
    )
    await absenceModal.selectAbsenceType('OTHER_ABSENCE')
    await absenceModal.tooManyAbsencesError(child.id).waitUntilHidden()
    await absenceModal.modalSendButton.assertDisabled(false)
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1))
    )
    await absenceModal.tooManyAbsencesError(child.id).waitUntilHidden()
    await absenceModal.modalSendButton.assertDisabled(false)
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1).addDays(1))
    )
    await absenceModal.tooManyAbsencesError(child.id).waitUntilVisible()
    await absenceModal.modalSendButton.assertDisabled(true)
  })

  test('absence modal shows link to absence application page handling holiday', async () => {
    const mockedDate = mockedTime.toLocalDate()
    const termRange = new FiniteDateRange(mockedDate, mockedDate.addYears(1))
    const startDate = LocalDate.of(2025, 5, 29) // ascension day
    await Fixture.preschoolTerm({
      extendedTerm: termRange,
      finnishPreschool: termRange,
      swedishPreschool: termRange,
      applicationPeriod: termRange.withStart(mockedDate.subMonths(2))
    }).save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({
      areaId: area.id,
      enabledPilotFeatures: ['RESERVATIONS']
    }).save()
    await Fixture.family({ guardian: adult, children: [child] }).save()
    await Fixture.placement({
      type: 'PRESCHOOL',
      childId: child.id,
      unitId: unit.id,
      startDate: termRange.start,
      endDate: termRange.end
    }).save()

    const citizenPage = await Page.open({
      mockedTime,
      citizenCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await enduserLogin(citizenPage, adult)
    const citizenHeader = new CitizenHeader(citizenPage)
    await citizenHeader.selectTab('calendar')
    const citizenCalendarPage = new CitizenCalendarPage(citizenPage, 'desktop')
    const absenceModal = await citizenCalendarPage.openAbsencesModal()
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1).subDays(1))
    )
    await absenceModal.selectAbsenceType('OTHER_ABSENCE')
    await absenceModal.tooManyAbsencesError(child.id).waitUntilHidden()
    await absenceModal.modalSendButton.assertDisabled(false)
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1))
    )
    await absenceModal.tooManyAbsencesError(child.id).waitUntilHidden()
    await absenceModal.modalSendButton.assertDisabled(false)
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1).addDays(1))
    )
    await absenceModal.tooManyAbsencesError(child.id).waitUntilVisible()
    await absenceModal.modalSendButton.assertDisabled(true)
  })

  test("absence modal doesn't show link to absence application page for child in daycare", async () => {
    const mockedDate = mockedTime.toLocalDate()
    const termRange = new FiniteDateRange(mockedDate, mockedDate.addYears(1))
    await Fixture.preschoolTerm({
      extendedTerm: termRange,
      finnishPreschool: termRange,
      swedishPreschool: termRange,
      applicationPeriod: termRange.withStart(mockedDate.subMonths(2))
    }).save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({
      areaId: area.id,
      enabledPilotFeatures: ['RESERVATIONS']
    }).save()
    await Fixture.family({ guardian: adult, children: [child] }).save()
    await Fixture.placement({
      type: 'DAYCARE',
      childId: child.id,
      unitId: unit.id,
      startDate: termRange.start,
      endDate: termRange.end
    }).save()

    const citizenPage = await Page.open({
      mockedTime,
      citizenCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await enduserLogin(citizenPage, adult)
    const citizenHeader = new CitizenHeader(citizenPage)
    await citizenHeader.selectTab('calendar')
    const citizenCalendarPage = new CitizenCalendarPage(citizenPage, 'desktop')
    const absenceModal = await citizenCalendarPage.openAbsencesModal()
    const startDate = termRange.start.addMonths(1)
    await absenceModal.selectDates(
      new FiniteDateRange(startDate, startDate.addWeeks(1))
    )
    await absenceModal.selectAbsenceType('OTHER_ABSENCE')
    await absenceModal.tooManyAbsencesError(child.id).waitUntilHidden()
    await absenceModal.modalSendButton.assertDisabled(false)
  })

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

  test('Form is invalid if absence date range is not on placement date range', async () => {
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    await Fixture.family({ guardian: adult, children: [child] }).save()
    await Fixture.placement({
      type: 'PRESCHOOL',
      childId: child.id,
      unitId: unit.id,
      startDate: mockedTime.toLocalDate(),
      endDate: mockedTime.toLocalDate()
    }).save()

    const page = await Page.open({
      mockedTime,
      citizenCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await enduserLogin(page, adult)
    const citizenHeader = new CitizenHeader(page)
    const citizenChildPage = await citizenHeader.openChildPage(child.id)
    await citizenChildPage.openCollapsible('absence-applications')
    const newAbsenceApplicationPage =
      await citizenChildPage.newAbsenceApplicationPage()
    await newAbsenceApplicationPage.startDate.fill(
      mockedTime.toLocalDate().addDays(1)
    )
    await newAbsenceApplicationPage.endDate.fill(
      mockedTime.toLocalDate().addDays(1)
    )
    await newAbsenceApplicationPage.description.fill('test')
    await newAbsenceApplicationPage.confirmation.check()
    await newAbsenceApplicationPage.dateRangeWarning.waitUntilVisible()
    await newAbsenceApplicationPage.createButton.assertDisabled(true)
  })

  test('Form is valid if absence date range is partly within placement date range', async () => {
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    await Fixture.family({ guardian: adult, children: [child] }).save()
    await Fixture.placement({
      type: 'PRESCHOOL',
      childId: child.id,
      unitId: unit.id,
      startDate: mockedTime.toLocalDate(),
      endDate: mockedTime.toLocalDate().addDays(5)
    }).save()

    const page = await Page.open({
      mockedTime,
      citizenCustomizations: { featureFlags: { absenceApplications: true } }
    })
    await enduserLogin(page, adult)
    const citizenHeader = new CitizenHeader(page)
    const citizenChildPage = await citizenHeader.openChildPage(child.id)
    await citizenChildPage.openCollapsible('absence-applications')
    const newAbsenceApplicationPage =
      await citizenChildPage.newAbsenceApplicationPage()
    await newAbsenceApplicationPage.startDate.fill(mockedTime.toLocalDate())
    await newAbsenceApplicationPage.endDate.fill(
      mockedTime.toLocalDate().addDays(8)
    )
    await newAbsenceApplicationPage.description.fill('test')
    await newAbsenceApplicationPage.confirmation.check()
    await newAbsenceApplicationPage.createButton.assertDisabled(false)
    await newAbsenceApplicationPage.createButton.click()

    await citizenChildPage.openCollapsible('absence-applications')
    await citizenChildPage.assertAbsenceApplications([
      {
        range: '05.05.2025 - 13.05.2025',
        status: 'Odottaa päätöstä',
        description: 'test'
      }
    ])
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
