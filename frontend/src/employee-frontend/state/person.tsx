// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { PersonJSON, PersonResponse } from 'lib-common/generated/api-types/pis'
import { Loading, Result } from 'lib-common/api'
import { Decision } from '../types/decision'
import { Invoice } from '../types/invoicing'
import { FamilyOverview } from '../types/family-overview'
import { getFamilyOverview } from '../api/family-overview'
import { Action } from 'lib-common/generated/action'
import { getPersonDetails } from '../api/person'
import { UUID } from 'lib-common/types'

export interface PersonState {
  person: Result<PersonJSON>
  permittedActions: Set<Action.Person>
  decisions: Result<Decision[]>
  family: Result<FamilyOverview>
  invoices: Result<Invoice[]>
  setPerson: (person: PersonJSON) => void
  setDecisions: (r: Result<Decision[]>) => void
  setFamily: (r: Result<FamilyOverview>) => void
  setInvoices: (r: Result<Invoice[]>) => void
  reloadFamily: (id: string) => void
}

const defaultState: PersonState = {
  person: Loading.of(),
  permittedActions: new Set(),
  decisions: Loading.of(),
  family: Loading.of(),
  invoices: Loading.of(),
  setPerson: () => undefined,
  setDecisions: () => undefined,
  setFamily: () => undefined,
  setInvoices: () => undefined,
  reloadFamily: (_id: string) => undefined
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
  useEffect(() => loadPerson(id), [loadPerson, id])

  const [family, setFamily] = useState(defaultState.family)
  const loadFamily = useRestApi(getFamilyOverview, setFamily)
  const reloadFamily = useCallback((id: string) => loadFamily(id), [loadFamily])

  const person = useMemo(
    () => personResponse.map((response) => response.person),
    [personResponse]
  )
  const setPerson = useCallback(
    (person: PersonJSON) => {
      setPersonResponse((prev) =>
        prev.map((response) => ({ ...response, person }))
      )
      reloadFamily(id)
    },
    [id, reloadFamily]
  )

  const [decisions, setDecisions] = useState(defaultState.decisions)
  const [invoices, setInvoices] = useState(defaultState.invoices)

  const value = useMemo<PersonState>(
    () => ({
      person,
      setPerson,
      permittedActions,
      decisions,
      setDecisions,
      family,
      setFamily,
      invoices,
      setInvoices,
      reloadFamily
    }),
    [
      person,
      setPerson,
      permittedActions,
      decisions,
      family,
      invoices,
      reloadFamily
    ]
  )

  return (
    <PersonContext.Provider value={value}>{children}</PersonContext.Provider>
  )
})
