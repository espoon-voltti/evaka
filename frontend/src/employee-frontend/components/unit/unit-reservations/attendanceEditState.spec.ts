// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { validateEditState } from './StaffAttendanceDetailsModal'

const today = LocalDate.of(2022, 1, 2)
const yesterday = today.subDays(1)
const tomorrow = today.addDays(1)

describe('validateEditState', () => {
  it('maps the state to a request body', () => {
    const [body, errors] = validateEditState(
      [
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
          departed: null,
          occupancyCoefficient: 1
        }
      ],
      today,
      [
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: '',
          departed: '07:00'
        },
        {
          id: null,
          type: 'PRESENT',
          groupId: 'group1',
          arrived: '08:00',
          departed: '12:00'
        }
      ]
    )
    expect(body).toEqual([
      {
        attendanceId: 'id1',
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
        departed: HelsinkiDateTime.fromLocal(today, LocalTime.of(7, 0))
      },
      {
        attendanceId: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(today, LocalTime.of(8, 0)),
        departed: HelsinkiDateTime.fromLocal(today, LocalTime.of(12, 0))
      }
    ])
    expect(errors).toEqual([{}, {}])
  })

  it('maps the departure before arrival at last item to next day', () => {
    const [body, errors] = validateEditState([], today, [
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: '12:00',
        departed: '16:00'
      },
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: '16:00',
        departed: '10:00'
      }
    ])
    expect(body).toEqual([
      {
        attendanceId: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(today, LocalTime.of(12, 0)),
        departed: HelsinkiDateTime.fromLocal(today, LocalTime.of(16, 0))
      },
      {
        attendanceId: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(today, LocalTime.of(16, 0)),
        departed: HelsinkiDateTime.fromLocal(tomorrow, LocalTime.of(10, 0))
      }
    ])
    expect(errors).toEqual([{}, {}])
  })

  it('requires arrived timestamp', () => {
    const [body, errors] = validateEditState([], today, [
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: '',
        departed: ''
      }
    ])
    expect(body).toBeUndefined()
    expect(errors).toEqual([{ arrived: 'required' }])
  })

  it('does not require the arrived timestamp for the first entry for an ongoing overnight attendance', () => {
    const [body, errors] = validateEditState(
      [
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
          departed: null,
          occupancyCoefficient: 1
        }
      ],
      today,
      [
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: '',
          departed: ''
        }
      ]
    )
    expect(body).toEqual([
      {
        attendanceId: 'id1',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
        departed: null,
        type: 'PRESENT'
      }
    ])
    expect(errors).toEqual([{}])
  })
  it('requires departed timestamp for all except the last entry', () => {
    const [body, errors] = validateEditState([], today, [
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: '7:00',
        departed: ''
      },
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group2',
        arrived: '8:00',
        departed: ''
      }
    ])
    expect(body).toBeUndefined()
    expect(errors).toEqual([{ departed: 'required' }, {}])
  })

  it('requires valid timestamps', () => {
    const [body, errors] = validateEditState([], today, [
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: 'invalid',
        departed: 'also invalid'
      }
    ])
    expect(body).toBeUndefined()
    expect(errors).toEqual([{ arrived: 'timeFormat', departed: 'timeFormat' }])
  })

  it('requires departure to be after arrival for all but the last item', () => {
    const [body, errors] = validateEditState(
      [
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
          departed: null,
          occupancyCoefficient: 1
        }
      ],
      today,
      [
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: '8:00',
          departed: '07:00'
        },
        {
          id: null,
          type: 'PRESENT',
          groupId: 'group1',
          arrived: '10:00',
          departed: '09:00'
        }
      ]
    )
    expect(body).toBeUndefined()
    expect(errors).toEqual([{ departed: 'timeFormat' }, {}])
  })
})
