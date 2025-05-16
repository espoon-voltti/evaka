// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo } from 'react'

import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import type { Action } from 'lib-common/generated/action'
import type { PersonJSON } from 'lib-common/generated/api-types/pis'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'

import { personQuery } from './queries'

export interface PersonState {
  person: Result<PersonJSON>
  permittedActions: Set<Action.Person>
}

const defaultState: PersonState = {
  person: Loading.of(),
  permittedActions: new Set()
}

const emptyPermittedActions = new Set<Action.Person>()

export const PersonContext = createContext<PersonState>(defaultState)

export const PersonContextProvider = React.memo(function PersonContextProvider({
  id,
  children
}: {
  id: PersonId
  children: React.JSX.Element
}) {
  const personResponse = useQueryResult(personQuery({ personId: id }))

  const person = useMemo(
    () => personResponse.map((response) => response.person),
    [personResponse]
  )
  const permittedActions = useMemo(
    () =>
      personResponse
        .map(({ permittedActions }) => new Set(permittedActions))
        .getOrElse(emptyPermittedActions),
    [personResponse]
  )

  const value = useMemo<PersonState>(
    () => ({
      person,
      permittedActions
    }),
    [person, permittedActions]
  )

  return (
    <PersonContext.Provider value={value}>{children}</PersonContext.Provider>
  )
})
