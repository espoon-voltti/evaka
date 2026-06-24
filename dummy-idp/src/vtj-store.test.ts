// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'node:assert/strict'
import { test } from 'node:test'

import type { MockVtjDataset } from './model'
import { VtjStore } from './vtj-store'

const dataset: MockVtjDataset = {
  persons: [
    {
      firstNames: 'Grandparent',
      lastName: 'One',
      socialSecurityNumber: 'GP1',
      nationalities: []
    },
    {
      firstNames: 'Guardian',
      lastName: 'One',
      socialSecurityNumber: 'G1',
      nationalities: []
    },
    {
      firstNames: 'Child',
      lastName: 'A',
      socialSecurityNumber: 'C1',
      nationalities: []
    },
    {
      firstNames: 'Child',
      lastName: 'B',
      socialSecurityNumber: 'C2',
      nationalities: []
    },
    {
      firstNames: 'Grandchild',
      lastName: 'A',
      socialSecurityNumber: 'GC1',
      nationalities: []
    }
  ],
  guardianDependants: { GP1: ['G1'], G1: ['C1', 'C2'], C1: ['GC1'] }
}

test('get resolves relations one level deep only', () => {
  const store = new VtjStore(dataset)
  const guardian = store.get('G1')
  assert.ok(guardian)
  // direct dependants only; C1's own dependant GC1 must not be flattened in
  assert.deepEqual(
    guardian.dependants.map((p) => p.socialSecurityNumber),
    ['C1', 'C2']
  )
  // direct guardians only; GP1 is G1's guardian
  assert.deepEqual(
    guardian.guardians.map((p) => p.socialSecurityNumber),
    ['GP1']
  )
  const child = store.get('C1')
  assert.ok(child)
  // C1's guardian is G1; GP1 (G1's guardian) must not be flattened in
  assert.deepEqual(
    child.guardians.map((p) => p.socialSecurityNumber),
    ['G1']
  )
  assert.deepEqual(
    child.dependants.map((p) => p.socialSecurityNumber),
    ['GC1']
  )
})

test('get returns undefined for missing ssn', () => {
  const store = new VtjStore(dataset)
  assert.equal(store.get('NOPE'), undefined)
})

test('dependantCount reflects the graph', () => {
  const store = new VtjStore(dataset)
  assert.equal(store.dependantCount('G1'), 2)
  assert.equal(store.dependantCount('C1'), 1)
})

test('upsert overwrites a person and merges links without duplicates', () => {
  const store = new VtjStore(dataset)
  store.upsert({
    persons: [
      {
        firstNames: 'Guardian',
        lastName: 'Renamed',
        socialSecurityNumber: 'G1',
        nationalities: []
      }
    ],
    guardianDependants: { G1: ['C1'] }
  })
  assert.equal(store.get('G1')?.lastName, 'Renamed')
  assert.deepEqual(
    store.get('G1')?.dependants.map((p) => p.socialSecurityNumber),
    ['C1', 'C2']
  )
})

test('clear empties the store', () => {
  const store = new VtjStore(dataset)
  store.clear()
  assert.equal(store.list().length, 0)
  assert.equal(store.get('G1'), undefined)
})

test('snapshot can be restored after clearing and mutating', () => {
  const store = new VtjStore(dataset)
  const snapshot = store.snapshot()

  store.clear()
  store.upsert({
    persons: [
      {
        firstNames: 'Other',
        lastName: 'X',
        socialSecurityNumber: 'O1',
        nationalities: []
      }
    ],
    guardianDependants: {}
  })
  store.clear()
  store.upsert(snapshot)

  assert.deepEqual(
    store.get('G1')?.dependants.map((p) => p.socialSecurityNumber),
    ['C1', 'C2']
  )
  assert.equal(store.get('O1'), undefined)
  assert.equal(store.list().length, 5)
})
