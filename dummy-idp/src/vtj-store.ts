// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MockVtjDataset, VtjPerson } from './model'

export type VtjPersonWithRelations = VtjPerson & {
  guardians: VtjPerson[]
  dependants: VtjPerson[]
}

export class VtjStore {
  private persons = new Map<string, VtjPerson>()
  private dependantsOfGuardian = new Map<string, string[]>()
  private guardiansOfDependant = new Map<string, string[]>()

  constructor(initial?: MockVtjDataset) {
    if (initial) this.upsert(initial)
  }

  clear(): void {
    this.persons.clear()
    this.dependantsOfGuardian.clear()
    this.guardiansOfDependant.clear()
  }

  upsert(dataset: MockVtjDataset): void {
    for (const person of dataset.persons) {
      this.persons.set(person.socialSecurityNumber, person)
    }
    for (const [guardian, dependants] of Object.entries(
      dataset.guardianDependants
    )) {
      for (const dependant of dependants) {
        addLink(this.dependantsOfGuardian, guardian, dependant)
        addLink(this.guardiansOfDependant, dependant, guardian)
      }
    }
  }

  list(): VtjPerson[] {
    return [...this.persons.values()]
  }

  get(ssn: string): VtjPersonWithRelations | undefined {
    const person = this.persons.get(ssn)
    if (!person) return undefined
    return {
      ...person,
      guardians: resolve(this.guardiansOfDependant.get(ssn), this.persons),
      dependants: resolve(this.dependantsOfGuardian.get(ssn), this.persons)
    }
  }

  dependantCount(ssn: string): number {
    return this.dependantsOfGuardian.get(ssn)?.length ?? 0
  }
}

function addLink(map: Map<string, string[]>, key: string, value: string): void {
  const existing = map.get(key) ?? []
  if (!existing.includes(value)) existing.push(value)
  map.set(key, existing)
}

function resolve(
  ssns: string[] | undefined,
  persons: Map<string, VtjPerson>
): VtjPerson[] {
  return (ssns ?? []).flatMap((ssn) => {
    const person = persons.get(ssn)
    return person ? [person] : []
  })
}
