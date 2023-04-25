// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import type { ModalAttendance } from './StaffAttendanceDetailsModal'
import {
  externalAttendanceValidator,
  staffAttendanceValidator
} from './StaffAttendanceTable'

const today = LocalDate.of(2022, 1, 2)
const yesterday = today.subDays(1)
const tomorrow = today.addDays(1)

jest.useFakeTimers().setSystemTime(today.toSystemTzDate())

const getConfig = (attendances: ModalAttendance[], date = today) => ({
  date,
  attendances
})

describe('validateEditState', () => {
  it('maps the state to a request body', () => {
    const validate = staffAttendanceValidator(
      getConfig([
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
          departed: null
        }
      ])
    )
    const [body, errors] = validate([
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
    ])
    expect(body).toEqual([
      {
        id: 'id1',
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
        departed: HelsinkiDateTime.fromLocal(today, LocalTime.of(7, 0))
      },
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(today, LocalTime.of(8, 0)),
        departed: HelsinkiDateTime.fromLocal(today, LocalTime.of(12, 0))
      }
    ])
    expect(errors).toEqual([{}, {}])
  })

  it('maps the state to a request body for external attendances', () => {
    const validate = externalAttendanceValidator(
      getConfig([
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
          departed: null
        }
      ])
    )
    const [body, errors] = validate([
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
    ])
    expect(body).toEqual([
      {
        id: 'id1',
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
        departed: HelsinkiDateTime.fromLocal(today, LocalTime.of(7, 0))
      },
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(today, LocalTime.of(8, 0)),
        departed: HelsinkiDateTime.fromLocal(today, LocalTime.of(12, 0))
      }
    ])
    expect(errors).toEqual([{}, {}])
  })

  it('maps the departure before arrival at last item to next day', () => {
    const validate = staffAttendanceValidator(getConfig([]))
    const [body, errors] = validate([
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
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(today, LocalTime.of(12, 0)),
        departed: HelsinkiDateTime.fromLocal(today, LocalTime.of(16, 0))
      },
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(today, LocalTime.of(16, 0)),
        departed: HelsinkiDateTime.fromLocal(tomorrow, LocalTime.of(10, 0))
      }
    ])
    expect(errors).toEqual([{}, {}])
  })

  it('requires arrived timestamp', () => {
    const validate = staffAttendanceValidator(getConfig([]))
    const [body, errors] = validate([
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
    const validate = staffAttendanceValidator(
      getConfig([
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
          departed: null
        }
      ])
    )
    const [body, errors] = validate([
      {
        id: 'id1',
        type: 'PRESENT',
        groupId: 'group1',
        arrived: '',
        departed: ''
      }
    ])
    expect(body).toEqual([
      {
        id: 'id1',
        groupId: 'group1',
        arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
        departed: null,
        type: 'PRESENT'
      }
    ])
    expect(errors).toEqual([{}])
  })
  it('requires departed timestamp for all except the last entry', () => {
    const validate = staffAttendanceValidator(getConfig([]))
    const [body, errors] = validate([
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
    const validate = staffAttendanceValidator(getConfig([]))
    const [body, errors] = validate([
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
    const validate = staffAttendanceValidator(
      getConfig([
        {
          id: 'id1',
          type: 'PRESENT',
          groupId: 'group1',
          arrived: HelsinkiDateTime.fromLocal(yesterday, LocalTime.of(8, 0)),
          departed: null
        }
      ])
    )
    const [body, errors] = validate([
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
    ])
    expect(body).toBeUndefined()
    expect(errors).toEqual([{ departed: 'timeFormat' }, {}])
  })

  it('requires a value only for arrival if editing a day that IS today', () => {
    const validate = staffAttendanceValidator(getConfig([]))
    const [body, errors] = validate([
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

  it('requires a value for both arrival and departure if editing a day that is NOT today', () => {
    const validate = staffAttendanceValidator(getConfig([], yesterday))
    const [body, errors] = validate([
      {
        id: null,
        type: 'PRESENT',
        groupId: 'group1',
        arrived: '',
        departed: ''
      }
    ])
    expect(body).toBeUndefined()
    expect(errors).toEqual([{ arrived: 'required', departed: 'required' }])
  })

  it('requires groupId only if type is present in group', () => {
    const validate = staffAttendanceValidator(getConfig([]))
    const [body, errors] = validate([
      {
        id: 'id1',
        type: 'PRESENT',
        groupId: null,
        arrived: '8:00',
        departed: '9:00'
      },
      {
        id: 'id2',
        type: 'OVERTIME',
        groupId: null,
        arrived: '9:00',
        departed: '10:00'
      },
      {
        id: 'id3',
        type: 'JUSTIFIED_CHANGE',
        groupId: null,
        arrived: '10:00',
        departed: '11:00'
      },
      {
        id: 'id4',
        type: 'TRAINING',
        groupId: null,
        arrived: '11:00',
        departed: '12:00'
      },
      {
        id: 'id5',
        type: 'OTHER_WORK',
        groupId: null,
        arrived: '12:00',
        departed: '13:00'
      }
    ])
    expect(body).toBeUndefined()
    expect(errors).toEqual([
      { groupId: 'required' },
      { groupId: 'required' },
      { groupId: 'required' },
      {},
      {}
    ])
  })
})
