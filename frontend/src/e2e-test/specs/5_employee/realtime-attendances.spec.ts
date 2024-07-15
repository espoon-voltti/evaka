// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import {
  testCareArea2,
  testDaycare2,
  Fixture,
  uuidv4,
  familyWithTwoGuardians
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import { DevDaycare, DevEmployee, DevPerson } from '../../generated/api-types'
import { UnitPage } from '../../pages/employee/units/unit'
import {
  UnitStaffAttendancesTable,
  UnitWeekCalendarPage
} from '../../pages/employee/units/unit-week-calendar-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
let attendancesSection: UnitWeekCalendarPage
let child1Fixture: DevPerson
let child1DaycarePlacementId: UUID
let daycare: DevDaycare
let unitSupervisor: DevEmployee
let nonGroupStaff: DevEmployee
let groupStaff: DevEmployee

const mockedToday = LocalDate.of(2022, 3, 30)
const placementStartDate = mockedToday.subWeeks(4)
const placementEndDate = mockedToday.addWeeks(4)
const groupId = uuidv4()
const groupId2 = uuidv4()

beforeEach(async () => {
  await resetServiceState()

  await Fixture.family(familyWithTwoGuardians).save()
  const careArea = await Fixture.careArea(testCareArea2).save()
  daycare = await Fixture.daycare({
    ...testDaycare2,
    areaId: careArea.id,
    enabledPilotFeatures: ['REALTIME_STAFF_ATTENDANCE']
  }).save()

  unitSupervisor = await Fixture.employee({
    email: 'essi.esimies@evaka.test',
    firstName: 'Essi',
    lastName: 'Esimies'
  })
    .unitSupervisor(daycare.id)
    .save()

  await createDefaultServiceNeedOptions()

  await Fixture.daycareGroup({
    id: groupId,
    daycareId: daycare.id,
    name: 'Testailijat'
  }).save()

  await Fixture.daycareGroup({
    id: groupId2,
    daycareId: daycare.id,
    name: 'Testailijat 2'
  }).save()

  child1Fixture = familyWithTwoGuardians.children[0]
  child1DaycarePlacementId = uuidv4()
  await Fixture.placement({
    id: child1DaycarePlacementId,
    childId: child1Fixture.id,
    unitId: daycare.id,
    startDate: placementStartDate,
    endDate: placementEndDate
  }).save()

  await Fixture.groupPlacement({
    daycareGroupId: groupId,
    daycarePlacementId: child1DaycarePlacementId,
    startDate: placementStartDate,
    endDate: placementEndDate
  }).save()

  groupStaff = await Fixture.employee({
    email: 'kalle.kasvattaja@evaka.test',
    firstName: 'Kalle',
    lastName: 'Kasvattaja'
  })
    .withDaycareAcl(daycare.id, 'STAFF')
    .withGroupAcl(groupId)
    .withGroupAcl(groupId2)
    .save()
  nonGroupStaff = await Fixture.employee({
    email: 'kaisa.kasvattaja@evaka.test',
    firstName: 'Kaisa',
    lastName: 'Kasvattaja'
  })
    .staff(daycare.id)
    .save()

  await Fixture.staffOccupancyCoefficient(daycare.id, groupStaff.id).save()

  page = await Page.open({
    viewport: { width: 1440, height: 720 },
    mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(18, 0))
  })

  await employeeLogin(page, unitSupervisor)
})

const openWeekCalendar = async (): Promise<UnitWeekCalendarPage> => {
  unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(daycare.id)
  return await unitPage.openWeekCalendar()
}

describe('Realtime staff attendances', () => {
  test('Occupancy graph', async () => {
    await Fixture.realtimeStaffAttendance({
      employeeId: groupStaff.id,
      groupId,
      arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
      departed: null
    }).save()

    attendancesSection = await openWeekCalendar()
    await attendancesSection.occupancies.assertGraphIsVisible()
    await attendancesSection.setFilterStartDate(LocalDate.of(2022, 3, 1))
    await attendancesSection.occupancies.assertGraphHasNoData()
  })

  describe('Group selection: staff', () => {
    let staffAttendances: UnitStaffAttendancesTable

    beforeEach(async () => {
      attendancesSection = await openWeekCalendar()
      await attendancesSection.selectGroup('staff')
      staffAttendances = attendancesSection.staffAttendances
    })

    test('The staff attendances table shows all unit staff', async () => {
      await waitUntilEqual(
        () => staffAttendances.allNames,
        [nonGroupStaff, groupStaff].map(staffName)
      )
    })

    test('The icon tells whether a staff member is counted in occupancy or not', async () => {
      await staffAttendances.assertPositiveOccupancyCoefficientCount(1)
      await staffAttendances.assertZeroOccupancyCoefficientCount(1)
    })

    test('Sunday entries are shown in the calendar', async () => {
      await Fixture.realtimeStaffAttendance({
        employeeId: nonGroupStaff.id,
        groupId,
        arrived: mockedToday.subDays(3).toHelsinkiDateTime(LocalTime.of(7, 0)),
        departed: mockedToday.subDays(3).toHelsinkiDateTime(LocalTime.of(15, 0))
      }).save()

      await attendancesSection.changeWeekToDate(mockedToday.subWeeks(1))
      await staffAttendances.assertTableRow({
        rowIx: 0,
        nth: 6,
        attendances: [['07:00', '15:00']]
      })
    })
  })

  describe('Group staff attendances', () => {
    test('Attendance is shown on week view and day modal', async () => {
      await Fixture.staffAttendancePlan({
        id: uuidv4(),
        employeeId: groupStaff.id,
        startTime: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
        endTime: mockedToday.toHelsinkiDateTime(LocalTime.of(15, 0))
      }).save()

      await Fixture.realtimeStaffAttendance({
        employeeId: groupStaff.id,
        groupId,
        type: 'OVERTIME',
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 3)),
        departed: null
      }).save()

      attendancesSection = await openWeekCalendar()
      await attendancesSection.selectGroup(groupId)
      const staffAttendances = attendancesSection.staffAttendances

      await staffAttendances.assertTableRow({
        rowIx: 0,
        nth: 2,
        name: staffName(groupStaff),
        plannedAttendances: [['07:00', '15:00']],
        attendances: [['07:03', '–']]
      })

      const modal = await staffAttendances.openDetails(0, mockedToday)
      await waitUntilEqual(() => modal.summary(), {
        plan: '07:00 – 15:00',
        realized: '07:03 –',
        hours: '10:57 (+2:57)'
      })
    })

    test('Employee without group ACL is shown if they have attendances', async () => {
      await Fixture.realtimeStaffAttendance({
        employeeId: nonGroupStaff.id,
        groupId,
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
        departed: mockedToday.toHelsinkiDateTime(LocalTime.of(12, 15))
      }).save()

      attendancesSection = await openWeekCalendar()
      await attendancesSection.selectGroup(groupId)
      const staffAttendances = attendancesSection.staffAttendances

      await staffAttendances.assertTableRow({
        rowIx: 0,
        nth: 2,
        name: staffName(nonGroupStaff),
        attendances: [['07:00', '12:15']]
      })
    })

    test('Staff can edit only own attendances', async () => {
      await Fixture.realtimeStaffAttendance({
        employeeId: groupStaff.id,
        groupId,
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0)),
        departed: mockedToday.toHelsinkiDateTime(LocalTime.of(15, 30))
      }).save()

      await Fixture.realtimeStaffAttendance({
        employeeId: nonGroupStaff.id,
        groupId,
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
        departed: mockedToday.toHelsinkiDateTime(LocalTime.of(12, 15))
      }).save()

      await employeeLogin(page, groupStaff)

      attendancesSection = await openWeekCalendar()
      await attendancesSection.selectGroup(groupId)
      const staffAttendances = attendancesSection.staffAttendances

      await staffAttendances.assertTableRow({
        rowIx: 0,
        nth: 2,
        name: staffName(nonGroupStaff),
        attendances: [['07:00', '12:15']]
      })
      await staffAttendances.assertOpenDetailsVisible(0, mockedToday, false)

      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        name: staffName(groupStaff),
        attendances: [['08:00', '15:30']]
      })

      await staffAttendances.assertOpenDetailsVisible(1, mockedToday, true)
    })

    test('Automatically closed attendance is indicated and cleared on edit', async () => {
      const yesterday = mockedToday.subDays(1)

      const otherGroupStaff = await Fixture.employee({
        email: 'raija.raivo@evaka.test',
        firstName: 'Raija',
        lastName: 'Raivo',
        roles: []
      })
        .withDaycareAcl(daycare.id, 'STAFF')
        .withGroupAcl(groupId)
        .withGroupAcl(groupId2)
        .save()

      await Fixture.realtimeStaffAttendance({
        employeeId: groupStaff.id,
        groupId,
        type: 'OVERTIME',
        arrived: yesterday.toHelsinkiDateTime(LocalTime.of(7, 0)),
        departed: yesterday.toHelsinkiDateTime(LocalTime.of(20, 0)),
        departedAutomatically: true
      }).save()

      await Fixture.realtimeStaffAttendance({
        employeeId: otherGroupStaff.id,
        groupId,
        type: 'OVERTIME',
        arrived: yesterday.toHelsinkiDateTime(LocalTime.of(8, 0)),
        departed: yesterday.toHelsinkiDateTime(LocalTime.of(16, 0)),
        departedAutomatically: false
      }).save()

      await Fixture.realtimeStaffAttendance({
        employeeId: otherGroupStaff.id,
        groupId,
        type: 'OVERTIME',
        arrived: yesterday.subDays(1).toHelsinkiDateTime(LocalTime.of(9, 0)),
        departed: yesterday.subDays(1).toHelsinkiDateTime(LocalTime.of(15, 0)),
        departedAutomatically: false
      }).save()

      await Fixture.staffAttendancePlan({
        id: uuidv4(),
        employeeId: otherGroupStaff.id,
        startTime: yesterday.toHelsinkiDateTime(LocalTime.of(7, 0)),
        endTime: yesterday.toHelsinkiDateTime(LocalTime.of(15, 0))
      }).save()

      attendancesSection = await openWeekCalendar()
      await attendancesSection.selectGroup(groupId)
      const staffAttendances = attendancesSection.staffAttendances

      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 1,
        name: staffName(otherGroupStaff),
        attendances: [['08:00', '16:00']]
      })

      await staffAttendances.assertTableRow({
        rowIx: 0,
        nth: 1,
        name: staffName(groupStaff),
        attendances: [['07:00', '20:00*']]
      })

      await staffAttendances.assertTooltip(0, yesterday, 'Automaattikatkaistu')

      const modal = await staffAttendances.openDetails(0, yesterday)
      await modal.setDepartureTime(0, '15:00')
      await modal.save()
      await staffAttendances.assertTableRow({
        rowIx: 0,
        nth: 1,
        name: staffName(groupStaff),
        attendances: [['07:00', '15:00']]
      })
    })
  })
  describe('Details modal', () => {
    let staffAttendances: UnitStaffAttendancesTable

    async function prepareTest({ arrived }: { arrived: HelsinkiDateTime }) {
      await Fixture.realtimeStaffAttendance({
        employeeId: groupStaff.id,
        groupId,
        arrived,
        departed: null
      }).save()
      attendancesSection = await openWeekCalendar()
      await attendancesSection.selectGroup('staff')
      staffAttendances = attendancesSection.staffAttendances
    }

    test('An existing entry can be edited', async () => {
      await prepareTest({
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0))
      })

      let modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setDepartureTime(0, '15:00')
      await modal.save()

      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        attendances: [['07:00', '15:00']]
      })

      modal = await staffAttendances.openDetails(1, mockedToday)
      await waitUntilEqual(() => modal.summary(), {
        plan: '–',
        realized: '07:00 – 15:00',
        hours: '8:00'
      })
    })
    test('An existing overnight entry can be edited', async () => {
      await prepareTest({
        arrived: mockedToday.subDays(1).toHelsinkiDateTime(LocalTime.of(7, 0))
      })

      let modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setDepartureTime(0, '16:00')
      await modal.save()

      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 1,
        attendances: [['07:00', '→']]
      })
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        attendances: [['→', '16:00']]
      })

      modal = await staffAttendances.openDetails(1, mockedToday)
      await waitUntilEqual(() => modal.summary(), {
        plan: '–',
        realized: '→ – 16:00',
        hours: '33:00'
      })
    })
    test('If departure is earlier than arrival, departure is on the next day', async () => {
      const arrivalDate = mockedToday.subDays(1)
      await prepareTest({
        arrived: arrivalDate.toHelsinkiDateTime(LocalTime.of(7, 0))
      })

      let modal = await staffAttendances.openDetails(1, arrivalDate)
      await modal.setDepartureTime(0, '06:00')
      await modal.save()

      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 1,
        attendances: [['07:00', '→']]
      })
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        attendances: [['→', '06:00']]
      })

      modal = await staffAttendances.openDetails(1, arrivalDate)
      await waitUntilEqual(() => modal.summary(), {
        plan: '–',
        realized: '07:00 – →',
        hours: '23:00'
      })
    })
    test('Multiple new entries can be added', async () => {
      await prepareTest({
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0))
      })

      let modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setDepartureTime(0, '12:00')
      await modal.addNewAttendance()
      await modal.setType(1, 'TRAINING')

      // Bug check: reset group if type does not require one
      await modal.setType(1, 'PRESENT')
      await modal.setGroup(1, groupId)
      await modal.setType(1, 'TRAINING')

      await modal.setArrivalTime(1, '12:00')
      await modal.setDepartureTime(1, '13:00')
      await modal.addNewAttendance()
      await modal.setGroup(2, groupId)
      await modal.setType(2, 'PRESENT')
      await modal.setArrivalTime(2, '13:00')
      await modal.setDepartureTime(2, '14:30')
      await modal.addNewAttendance()
      await modal.setType(3, 'OTHER_WORK')
      await modal.setArrivalTime(3, '14:30')
      await modal.setDepartureTime(3, '15:00')
      await modal.save()

      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        attendances: [
          ['07:00', '12:00'],
          ['13:00', '14:30']
        ]
      })

      modal = await staffAttendances.openDetails(1, mockedToday)
      await waitUntilEqual(() => modal.summary(), {
        plan: '–',
        realized: '07:00 – 15:00',
        hours: '8:00'
      })
    })
    test('Gaps in attendances are warned about', async () => {
      await prepareTest({
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0))
      })

      const modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setDepartureTime(0, '12:00')
      await modal.addNewAttendance()
      await modal.setGroup(1, groupId)
      await modal.setType(1, 'TRAINING')
      await modal.setArrivalTime(1, '12:30')
      await modal.setDepartureTime(1, '13:00')
      await modal.addNewAttendance()
      await modal.setGroup(2, groupId)
      await modal.setType(2, 'PRESENT')
      await modal.setArrivalTime(2, '13:20')
      await modal.setDepartureTime(2, '14:30')

      await waitUntilEqual(
        () => modal.gapWarning(1),
        'Kirjaus puuttuu välillä 12:00 – 12:30'
      )
      await waitUntilEqual(
        () => modal.gapWarning(2),
        'Kirjaus puuttuu välillä 13:00 – 13:20'
      )
    })
    test('Departure time is required when editing days that are not today', async () => {
      await prepareTest({
        arrived: mockedToday.subDays(2).toHelsinkiDateTime(LocalTime.of(7, 0))
      })

      const modal = await staffAttendances.openDetails(
        1,
        mockedToday.subDays(1)
      )
      await modal.setArrivalTime(0, '08:00')
      await waitUntilEqual(() => modal.departureTimeInfo(0), 'Pakollinen tieto')
    })
    test('Departure time is NOT required when editing today', async () => {
      await prepareTest({
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0))
      })

      const modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setArrivalTime(0, '')
      await waitUntilEqual(() => modal.arrivalTimeInfo(0), 'Pakollinen tieto')
      await modal.assertDepartureTimeInfoHidden(0)
    })
    test('If there is an open arrival, new arrival cannot be marked', async () => {
      await prepareTest({
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)).subHours(24)
      })

      let modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.openAttendanceWarning.waitUntilVisible()
      await modal.arrivalTimeInputInfo.assertText((text) =>
        text.includes('Avoin kirjaus')
      )

      await modal.setDepartureTime(0, '06:00')
      await modal.save()

      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 1,
        attendances: [['07:00', '→']]
      })
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        attendances: [['→', '06:00']]
      })

      modal = await staffAttendances.openDetails(1, mockedToday)
      await waitUntilEqual(() => modal.summary(), {
        plan: '–',
        realized: '→ – 06:00',
        hours: '23:00'
      })
    })
  })
  describe('Staff count sums in the table', () => {
    let staffAttendances: UnitStaffAttendancesTable

    beforeEach(async () => {
      const times: [number, number, number][] = [
        [0, 12, 15],
        [4, 9, 10],
        [5, 10, 13]
      ]
      for (const [day, arrivalHour, departureHour] of times) {
        await Fixture.realtimeStaffAttendance({
          employeeId: groupStaff.id,
          groupId,
          arrived: mockedToday
            .addDays(day)
            .toHelsinkiDateTime(LocalTime.of(arrivalHour, 0)),
          departed: mockedToday
            .addDays(day)
            .toHelsinkiDateTime(LocalTime.of(departureHour, 0))
        }).save()
      }

      // This employee has no group ACLs, but should still be included in totals
      await Fixture.realtimeStaffAttendance({
        employeeId: nonGroupStaff.id,
        groupId,
        arrived: mockedToday.subDays(1).toHelsinkiDateTime(LocalTime.of(7, 0)),
        departed: mockedToday.toHelsinkiDateTime(LocalTime.of(15, 0))
      }).save()

      attendancesSection = await openWeekCalendar()
      staffAttendances = attendancesSection.staffAttendances
    })

    test('Total staff counts', async () => {
      await attendancesSection.selectGroup(groupId2)
      await waitUntilEqual(() => staffAttendances.personCountSum(0), '– hlö')

      await attendancesSection.selectGroup(groupId)
      await waitUntilEqual(() => staffAttendances.personCountSum(0), '– hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(1), '1 hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(2), '2 hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(3), '– hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(4), '– hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(5), '– hlö')
    })
  })
  describe('External staff members', () => {
    let staffAttendances: UnitStaffAttendancesTable

    beforeEach(async () => {
      attendancesSection = await openWeekCalendar()
      staffAttendances = attendancesSection.staffAttendances
    })

    test('Can an add and modify an external', async () => {
      const addPersonModal = await attendancesSection.clickAddPersonButton()
      await addPersonModal.setArrivalDate(mockedToday.format())
      await addPersonModal.setArrivalTime('11:00')
      await addPersonModal.typeName('Sijainen Saija')
      await addPersonModal.selectGroup(groupId)
      await addPersonModal.setStaffOccupancyEffect(false)

      await addPersonModal.save()

      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        name: 'Sijainen Saija',
        attendances: [['11:00', '–']],
        hasStaffOccupancyEffect: false
      })

      let modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setDepartureTime(0, '13:00')
      await modal.save()

      await waitUntilEqual(() => staffAttendances.rowCount, 2)
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        name: 'Sijainen Saija',
        attendances: [['11:00', '13:00']],
        hasStaffOccupancyEffect: false
      })

      modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.removeAttendance(0)
      await modal.save()

      await waitUntilEqual(() => staffAttendances.rowCount, 1)
    })

    test('Can an add multiple entries to an external', async () => {
      const addPersonModal = await attendancesSection.clickAddPersonButton()
      await addPersonModal.setArrivalDate(mockedToday.subDays(2).format())
      await addPersonModal.setArrivalTime('11:00')
      await addPersonModal.setDepartureTime('13:00')
      await addPersonModal.typeName('Sijainen Saija')
      await addPersonModal.setStaffOccupancyEffect(false)
      await addPersonModal.selectGroup(groupId)

      await addPersonModal.save()

      let modal = await staffAttendances.openDetails(1, mockedToday.subDays(1))
      await modal.addNewAttendance()
      await modal.setArrivalTime(0, '08:00')
      await modal.setDepartureTime(0, '12:30')
      await modal.save()

      modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.addNewAttendance()
      await modal.setArrivalTime(0, '09:00')
      await modal.setDepartureTime(0, '17:30')
      await modal.save()

      await waitUntilEqual(() => staffAttendances.rowCount, 2)
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 0,
        name: 'Sijainen Saija',
        attendances: [['11:00', '13:00']],
        hasStaffOccupancyEffect: false
      })
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 1,
        attendances: [['08:00', '12:30']]
      })
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        attendances: [['09:00', '17:30']]
      })
    })

    test('Can an add new external with arrival and departure times', async () => {
      const addPersonModal = await attendancesSection.clickAddPersonButton()
      await addPersonModal.setArrivalDate(mockedToday.format())
      await addPersonModal.setArrivalTime('11:00')
      await addPersonModal.setDepartureTime('16:00')
      await addPersonModal.typeName('Sijainen Saija')
      await addPersonModal.selectGroup(groupId)
      await addPersonModal.setStaffOccupancyEffect(true)
      await addPersonModal.save()

      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 2,
        name: 'Sijainen Saija',
        attendances: [['11:00', '16:00']],
        hasStaffOccupancyEffect: true
      })
    })

    test('Cannot add new external with invalid data', async () => {
      const addPersonModal = await attendancesSection.clickAddPersonButton()
      await addPersonModal.setArrivalDate(mockedToday.format())
      await addPersonModal.setArrivalTime('9:99')
      await addPersonModal.typeName('S')
      await addPersonModal.selectGroup(groupId)

      await addPersonModal.timeErrorVisible()
      await addPersonModal.nameErrorVisible()
    })
  })
})

function staffName(employeeDetail: DevEmployee): string {
  return `${employeeDetail.lastName} ${employeeDetail.firstName}`
}
