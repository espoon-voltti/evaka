// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'

import { Loading, Result, wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Action } from 'lib-common/generated/action'
import { ChildBackupCareResponse } from 'lib-common/generated/api-types/backupcare'
import { ChildResponse } from 'lib-common/generated/api-types/daycare'
import {
  ParentshipWithPermittedActions,
  PersonJSON
} from 'lib-common/generated/api-types/pis'
import { PlacementResponse } from 'lib-common/generated/api-types/placement'
import { UUID } from 'lib-common/types'
import { useApiState, useRestApi } from 'lib-common/utils/useRestApi'

import { getPlacements } from '../api/child/placements'
import { getParentshipsByChild } from '../api/parentships'
import { getChildDetails, getPersonGuardians } from '../api/person'
import { getChildBackupCares } from '../generated/api-clients/backupcare'

export interface ChildState {
  childId: UUID | undefined
  person: Result<PersonJSON>
  setPerson: (value: PersonJSON) => void
  permittedActions: Set<Action.Child | Action.Person>
  placements: Result<PlacementResponse>
  loadPlacements: () => void
  parentships: Result<ParentshipWithPermittedActions[]>
  backupCares: Result<ChildBackupCareResponse[]>
  loadBackupCares: () => Promise<Result<unknown>>
  guardians: Result<PersonJSON[]>
  reloadGuardians: () => Promise<Result<unknown>>
  reloadPermittedActions: () => void
  assistanceNeedVoucherCoefficientsEnabled: Result<boolean>
  consecutivePlacementRanges: FiniteDateRange[]
}

const emptyPermittedActions = new Set<Action.Child | Action.Person>()

const defaultState: ChildState = {
  childId: undefined,
  person: Loading.of(),
  setPerson: () => undefined,
  permittedActions: emptyPermittedActions,
  placements: Loading.of(),
  loadPlacements: () => undefined,
  parentships: Loading.of(),
  backupCares: Loading.of(),
  loadBackupCares: () => Promise.resolve(Loading.of()),
  guardians: Loading.of(),
  reloadGuardians: () => Promise.resolve(Loading.of()),
  reloadPermittedActions: () => undefined,
  assistanceNeedVoucherCoefficientsEnabled: Loading.of(),
  consecutivePlacementRanges: []
}

export const ChildContext = createContext<ChildState>(defaultState)

export const ChildContextProvider = React.memo(function ChildContextProvider({
  id,
  children
}: {
  id: UUID
  children: React.ReactNode
}) {
  const [childResponse, setChildResponse] = useState<Result<ChildResponse>>(
    Loading.of()
  )
  const [permittedActions, setPermittedActions] = useState<
    Set<Action.Child | Action.Person>
  >(emptyPermittedActions)
  const [
    assistanceNeedVoucherCoefficientsEnabled,
    setAssistanceNeedVoucherCoefficientsEnabled
  ] = useState<Result<boolean>>(Loading.of())

  const updatePermittedActions = useCallback(
    (response: Result<ChildResponse>) => {
      setPermittedActions(
        response
          .map(
            ({ permittedActions, permittedPersonActions }) =>
              new Set([...permittedActions, ...permittedPersonActions])
          )
          .getOrElse(emptyPermittedActions)
      )
    },
    []
  )

  const setFullChildResponse = useCallback(
    (response: Result<ChildResponse>) => {
      updatePermittedActions(response)
      setChildResponse(response)
      setAssistanceNeedVoucherCoefficientsEnabled(
        response.map((res) => res.assistanceNeedVoucherCoefficientsEnabled)
      )
    },
    [updatePermittedActions]
  )
  const loadChild = useRestApi(getChildDetails, setFullChildResponse)
  useEffect(() => {
    void loadChild(id)
  }, [loadChild, id])

  const reloadPermittedActions = useCallback(() => {
    void getChildDetails(id).then(updatePermittedActions)
  }, [id, updatePermittedActions])

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
    async () =>
      permittedActions.has('READ_PLACEMENT')
        ? await getPlacements(id)
        : Loading.of<PlacementResponse>(),
    [id, permittedActions]
  )
  const [parentships] = useApiState(
    async (): Promise<Result<ParentshipWithPermittedActions[]>> =>
      permittedActions.has('READ_PARENTSHIPS')
        ? await getParentshipsByChild(id)
        : Loading.of(),
    [id, permittedActions]
  )

  const [backupCares, loadBackupCares] = useApiState(
    async () =>
      permittedActions.has('READ_BACKUP_CARE')
        ? wrapResult(getChildBackupCares)({ childId: id }).then((res) =>
            res.map((x) => x.backupCares)
          )
        : Loading.of<ChildBackupCareResponse[]>(),
    [id, permittedActions]
  )

  const [guardians, reloadGuardians] = useApiState(
    async () =>
      permittedActions.has('READ_GUARDIANS')
        ? await getPersonGuardians(id)
        : Loading.of<PersonJSON[]>(),
    [id, permittedActions]
  )

  const consecutivePlacementRanges = useMemo(
    () =>
      placements
        .map(({ placements: p }) =>
          sortBy(p, (placement) =>
            placement.startDate.toSystemTzDate().getTime()
          ).reduce((prev, curr) => {
            const currentRange = new FiniteDateRange(
              curr.startDate,
              curr.endDate
            )
            const fittingExistingIndex = prev.findIndex((range) =>
              range.adjacentTo(currentRange)
            )

            if (fittingExistingIndex > -1) {
              const fittingExisting = prev[fittingExistingIndex]

              const newRange = fittingExisting.leftAdjacentTo(currentRange)
                ? fittingExisting.withEnd(curr.endDate)
                : fittingExisting.withStart(curr.startDate)

              const copy = Array.from(prev)
              copy[fittingExistingIndex] = newRange
              return copy
            }

            return [...prev, currentRange]
          }, [] as FiniteDateRange[])
        )
        .getOrElse([]),
    [placements]
  )

  const value = useMemo(
    (): ChildState => ({
      childId: id,
      person,
      setPerson,
      permittedActions,
      placements,
      loadPlacements,
      parentships,
      backupCares,
      loadBackupCares,
      guardians,
      reloadGuardians,
      reloadPermittedActions,
      assistanceNeedVoucherCoefficientsEnabled,
      consecutivePlacementRanges
    }),
    [
      id,
      person,
      setPerson,
      permittedActions,
      placements,
      loadPlacements,
      parentships,
      backupCares,
      guardians,
      reloadGuardians,
      reloadPermittedActions,
      assistanceNeedVoucherCoefficientsEnabled,
      consecutivePlacementRanges,
      loadBackupCares
    ]
  )

  return <ChildContext.Provider value={value}>{children}</ChildContext.Provider>
})
