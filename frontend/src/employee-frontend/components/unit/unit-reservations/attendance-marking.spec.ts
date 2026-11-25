// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import type { EvakaUser } from 'lib-common/generated/api-types/user'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid } from 'lib-common/id-type'
import LocalTime from 'lib-common/local-time'

import type { MarkingMetadata } from './attendance-marking'
import {
  isAddedOnLaterDate,
  isChildMarkingRetroactive,
  isModifiedAfterAdding,
  isModifiedByPersonAfterAdding,
  isRetroactive
} from './attendance-marking'

const person: EvakaUser = {
  id: fromUuid('5f9a8c56-0c3a-4c4e-9d6d-2b0d5d3a8c01'),
  name: 'Iltanen Iida',
  type: 'EMPLOYEE'
}
const other: EvakaUser = {
  id: fromUuid('5f9a8c56-0c3a-4c4e-9d6d-2b0d5d3a8c02'),
  name: 'Esimies Essi',
  type: 'EMPLOYEE'
}
const system: EvakaUser = {
  id: fromUuid('00000000-0000-0000-0000-000000000000'),
  name: 'eVaka',
  type: 'SYSTEM'
}
const time = HelsinkiDateTime.of(2026, 10, 28, 9, 50)
const date = time.toLocalDate()
const noMetadata: MarkingMetadata = {
  addedAt: null,
  addedBy: null,
  modifiedAt: null,
  modifiedBy: null
}
const addedOnTime: MarkingMetadata = {
  addedAt: time,
  addedBy: person,
  modifiedAt: time,
  modifiedBy: person
}

describe('isModifiedAfterAdding', () => {
  it('is true when modified later by the same user', () => {
    expect(
      isModifiedAfterAdding({
        ...addedOnTime,
        modifiedAt: time.addMinutes(5)
      })
    ).toBe(true)
  })
  it('is true when modified by the system', () => {
    expect(
      isModifiedAfterAdding({
        ...noMetadata,
        modifiedAt: time,
        modifiedBy: system
      })
    ).toBe(true)
  })
})

describe('isModifiedByPersonAfterAdding', () => {
  it('is false when modified by the system, e.g. an automatic departure', () => {
    expect(
      isModifiedByPersonAfterAdding({
        ...noMetadata,
        modifiedAt: time,
        modifiedBy: system
      })
    ).toBe(false)
  })
})

describe('isAddedOnLaterDate', () => {
  it('is true when added by a person on a later date', () => {
    expect(
      isAddedOnLaterDate(
        { ...addedOnTime, addedAt: HelsinkiDateTime.of(2026, 10, 29, 0, 5) },
        date
      )
    ).toBe(true)
  })
  it('is false when added by the system on a later date', () => {
    expect(
      isAddedOnLaterDate(
        {
          ...addedOnTime,
          addedAt: HelsinkiDateTime.of(2026, 10, 29, 0, 5),
          addedBy: system
        },
        date
      )
    ).toBe(false)
  })
})

describe('isRetroactive', () => {
  it('is true when only modified after adding', () => {
    expect(isRetroactive({ ...addedOnTime, modifiedBy: other }, date)).toBe(
      true
    )
  })
})

describe('isChildMarkingRetroactive', () => {
  const nextDay = HelsinkiDateTime.of(2026, 10, 29, 8, 0)
  const addedNextDay: MarkingMetadata = {
    addedAt: nextDay,
    addedBy: person,
    modifiedAt: nextDay,
    modifiedBy: person
  }
  it('is false for a 23:59 departure added the next day', () => {
    expect(
      isChildMarkingRetroactive(
        addedNextDay,
        LocalTime.of(23, 59),
        'departure',
        date
      )
    ).toBe(false)
  })
  it('is false for a 00:00 arrival added the next day', () => {
    expect(
      isChildMarkingRetroactive(
        addedNextDay,
        LocalTime.of(0, 0),
        'arrival',
        date
      )
    ).toBe(false)
  })
  it('is true for a 23:59 departure that a person modified afterwards', () => {
    expect(
      isChildMarkingRetroactive(
        { ...addedNextDay, modifiedAt: nextDay.addHours(1), modifiedBy: other },
        LocalTime.of(23, 59),
        'departure',
        date
      )
    ).toBe(true)
  })
  it('is true for a departure added the next day', () => {
    expect(
      isChildMarkingRetroactive(
        addedNextDay,
        LocalTime.of(16, 0),
        'departure',
        date
      )
    ).toBe(true)
  })
  it('compares an arrival with the date the attendance ends', () => {
    expect(
      isChildMarkingRetroactive(
        addedNextDay,
        LocalTime.of(20, 0),
        'arrival',
        date
      )
    ).toBe(true)
    expect(
      isChildMarkingRetroactive(
        addedNextDay,
        LocalTime.of(20, 0),
        'arrival',
        nextDay.toLocalDate()
      )
    ).toBe(false)
  })
})
