// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import type { EvakaUser } from 'lib-common/generated/api-types/user'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid } from 'lib-common/id-type'

import type { MarkingMetadata } from './attendance-marking'
import {
  isAddedOnLaterDate,
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
