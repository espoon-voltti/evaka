// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'

import { Loading, Result } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import {
  ParentshipWithPermittedActions,
  PersonJSON,
  PersonResponse
} from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { useApiState, useRestApi } from 'lib-common/utils/useRestApi'

import { getFamilyOverview } from '../api/family-overview'
import { getParentshipsByHeadOfChild } from '../api/parentships'
import { getPersonDetails } from '../api/person'
import { FamilyOverview } from '../types/family-overview'

export interface PersonState {
  person: Result<PersonJSON>
  permittedActions: Set<Action.Person>
  family: Result<FamilyOverview>
  fridgeChildren: Result<ParentshipWithPermittedActions[]>
  setPerson: (person: PersonJSON) => void
  reloadFamily: () => void
  reloadFridgeChildren: () => void
}

const defaultState: PersonState = {
  person: Loading.of(),
  permittedActions: new Set(),
  family: Loading.of(),
  fridgeChildren: Loading.of(),
  setPerson: () => undefined,
  reloadFamily: () => undefined,
  reloadFridgeChildren: () => undefined
}

const emptyPermittedActions: Set<Action.Person> = new Set()

export const PersonContext = createContext<PersonState>(defaultState)

export const PersonContextProvider = React.memo(function PersonContextProvider({
  id,
  children
}: {
  id: UUID
  children: JSX.Element
}) {
  const [personResponse, setPersonResponse] = useState<Result<PersonResponse>>(
    Loading.of()
  )
  const [permittedActions, setPermittedActions] = useState<Set<Action.Person>>(
    emptyPermittedActions
  )
  const setFullPersonResponse = useCallback(
    (response: Result<PersonResponse>) => {
      setPermittedActions(
        response
          .map(({ permittedActions }) => new Set(permittedActions))
          .getOrElse(emptyPermittedActions)
      )
      setPersonResponse(response)
    },
    []
  )
  const loadPerson = useRestApi(getPersonDetails, setFullPersonResponse)
  useEffect(() => {
    void loadPerson(id)
  }, [loadPerson, id])

  const [family, reloadFamily] = useApiState(() => getFamilyOverview(id), [id])

  const [fridgeChildren, loadFridgeChildren] = useApiState(
    () => getParentshipsByHeadOfChild(id),
    [id]
  )

  const reloadFridgeChildren = useCallback(() => {
    reloadFamily()
    loadFridgeChildren()
  }, [reloadFamily, loadFridgeChildren])

  const person = useMemo(
    () => personResponse.map((response) => response.person),
    [personResponse]
  )
  const setPerson = useCallback(
    (person: PersonJSON) => {
      setPersonResponse((prev) =>
        prev.map((response) => ({ ...response, person }))
      )
      reloadFamily()
    },
    [reloadFamily]
  )

  const value = useMemo<PersonState>(
    () => ({
      person,
      setPerson,
      permittedActions,
      family,
      fridgeChildren,
      reloadFamily,
      reloadFridgeChildren
    }),
    [
      person,
      setPerson,
      permittedActions,
      family,
      fridgeChildren,
      reloadFamily,
      reloadFridgeChildren
    ]
  )

  return (
    <PersonContext.Provider value={value}>{children}</PersonContext.Provider>
  )
})
