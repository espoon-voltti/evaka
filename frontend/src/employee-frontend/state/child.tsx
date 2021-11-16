// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { ChildBackupCare } from '../types/child'
import { Loading, Result } from 'lib-common/api'
import { Parentship, PersonJSON } from 'lib-common/generated/api-types/pis'
import { Action } from 'lib-common/generated/action'
import { DaycarePlacementWithDetails } from 'lib-common/generated/api-types/placement'
import { getPlacements } from '../api/child/placements'
import { useApiState, useRestApi } from 'lib-common/utils/useRestApi'
import { UUID } from 'lib-common/types'
import { getChildDetails, getPersonGuardians } from '../api/person'
import { requireRole } from '../utils/roles'
import { getParentshipsByChild } from '../api/parentships'
import { UserContext } from './user'
import { ChildResponse } from 'lib-common/generated/api-types/daycare'

export interface ChildState {
  person: Result<PersonJSON>
  setPerson: (value: PersonJSON) => void
  permittedActions: Set<Action.Child | Action.Person>
  placements: Result<DaycarePlacementWithDetails[]>
  loadPlacements: () => void
  parentships: Result<Parentship[]>
  backupCares: Result<ChildBackupCare[]>
  setBackupCares: (request: Result<ChildBackupCare[]>) => void
  guardians: Result<PersonJSON[]>
}

const emptyPermittedActions = new Set<Action.Child | Action.Person>()

const defaultState: ChildState = {
  person: Loading.of(),
  setPerson: () => undefined,
  permittedActions: emptyPermittedActions,
  placements: Loading.of(),
  loadPlacements: () => undefined,
  parentships: Loading.of(),
  backupCares: Loading.of(),
  setBackupCares: () => undefined,
  guardians: Loading.of()
}

export const ChildContext = createContext<ChildState>(defaultState)

export const ChildContextProvider = React.memo(function ChildContextProvider({
  id,
  children
}: {
  id: UUID
  children: JSX.Element
}) {
  const { roles } = useContext(UserContext)

  const [childResponse, setChildResponse] = useState<Result<ChildResponse>>(
    Loading.of()
  )
  const [permittedActions, setPermittedActions] = useState<
    Set<Action.Child | Action.Person>
  >(emptyPermittedActions)

  const setFullChildResponse = useCallback(
    (response: Result<ChildResponse>) => {
      setPermittedActions(
        response
          .map(
            ({ permittedActions, permittedPersonActions }) =>
              new Set([...permittedActions, ...permittedPersonActions])
          )
          .getOrElse(emptyPermittedActions)
      )
      setChildResponse(response)
    },
    []
  )
  const loadChild = useRestApi(getChildDetails, setFullChildResponse)
  useEffect(() => loadChild(id), [loadChild, id])

  const person = useMemo(
    () => childResponse.map((response) => response.person),
    [childResponse]
  )
  const setPerson = useCallback(
    (person: PersonJSON) =>
      setChildResponse((prev) =>
        prev.map((child) => ({
          ...child,
          person
        }))
      ),
    []
  )

  const [placements, loadPlacements] = useApiState(
    () => getPlacements(id),
    [id]
  )
  const [parentships] = useApiState(
    async (): Promise<Result<Parentship[]>> =>
      requireRole(roles, 'SERVICE_WORKER', 'UNIT_SUPERVISOR', 'FINANCE_ADMIN')
        ? await getParentshipsByChild(id)
        : Loading.of(),
    [roles, id]
  )
  const [backupCares, setBackupCares] = useState<Result<ChildBackupCare[]>>(
    defaultState.backupCares
  )
  const [guardians] = useApiState(() => getPersonGuardians(id), [id])

  const value = useMemo(
    (): ChildState => ({
      person,
      setPerson,
      permittedActions,
      placements,
      loadPlacements,
      parentships,
      backupCares,
      setBackupCares,
      guardians
    }),
    [
      person,
      setPerson,
      permittedActions,
      placements,
      loadPlacements,
      parentships,
      backupCares,
      guardians
    ]
  )

  return <ChildContext.Provider value={value}>{children}</ChildContext.Provider>
})
