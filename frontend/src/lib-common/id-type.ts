// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  EmployeeId,
  EvakaUserId,
  PersonId
} from './generated/api-types/shared'

declare const id: unique symbol
export type Id<B extends string> = string & { [id]: B }

const regex =
  /^(?:[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}|00000000-0000-0000-0000-000000000000|ffffffff-ffff-ffff-ffff-ffffffffffff)$/i

const validate = (uuid: string) => regex.test(uuid)

export function tryFromUuid<T extends Id<string>>(id: string): T | undefined {
  if (!validate(id)) {
    return undefined
  }
  return id as T
}

export function fromUuid<T extends Id<string>>(id: string): T {
  if (!validate(id)) {
    throw new Error(`Invalid UUID: ${id}`)
  }
  return id as T
}

export function fromNullableUuid<T extends Id<string>>(
  id: string | null
): T | null {
  if (id === null) return null
  return fromUuid<T>(id)
}

export function randomId<T extends Id<string>>(): T {
  return crypto.randomUUID() as T
}

export function evakaUserId(employeeId: EmployeeId): EvakaUserId
export function evakaUserId(personId: PersonId): EvakaUserId
export function evakaUserId(id: string): EvakaUserId {
  return id as EvakaUserId
}
