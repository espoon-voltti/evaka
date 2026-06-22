// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'node:assert/strict'
import { test } from 'node:test'

import type { MockVtjDataset } from './model'
import { VtjStore } from './vtj-store'

const dataset: MockVtjDataset = {
  persons: [
    { firstNames: 'Guardian', lastName: 'One', socialSecurityNumber: 'G1', nationalities: [] },
    { firstNames: 'Child', lastName: 'A', socialSecurityNumber: 'C1', nationalities: [] },
    { firstNames: 'Child', lastName: 'B', socialSecurityNumber: 'C2', nationalities: [] }
  ],
  guardianDependants: { G1: ['C1', 'C2'] }
}

test('get returns person with one-level dependants and guardians', () => {
  const store = new VtjStore(dataset)
  const guardian = store.get('G1')
  assert.ok(guardian)
  assert.deepEqual(guardian.dependants.map((p) => p.socialSecurityNumber), ['C1', 'C2'])
  assert.deepEqual(guardian.guardians, [])
  const child = store.get('C1')
  assert.ok(child)
  assert.deepEqual(child.guardians.map((p) => p.socialSecurityNumber), ['G1'])
  assert.deepEqual(child.dependants, [])
})

test('get returns undefined for missing ssn', () => {
  const store = new VtjStore(dataset)
  assert.equal(store.get('NOPE'), undefined)
})

test('dependantCount reflects the graph', () => {
  const store = new VtjStore(dataset)
  assert.equal(store.dependantCount('G1'), 2)
  assert.equal(store.dependantCount('C1'), 0)
})

test('upsert overwrites a person and merges links without duplicates', () => {
  const store = new VtjStore(dataset)
  store.upsert({
    persons: [{ firstNames: 'Guardian', lastName: 'Renamed', socialSecurityNumber: 'G1', nationalities: [] }],
    guardianDependants: { G1: ['C1'] }
  })
  assert.equal(store.get('G1')?.lastName, 'Renamed')
  assert.deepEqual(store.get('G1')?.dependants.map((p) => p.socialSecurityNumber), ['C1', 'C2'])
})

test('clear empties the store', () => {
  const store = new VtjStore(dataset)
  store.clear()
  assert.equal(store.list().length, 0)
  assert.equal(store.get('G1'), undefined)
})
