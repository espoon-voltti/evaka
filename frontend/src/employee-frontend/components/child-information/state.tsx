// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { createContext, useMemo } from 'react'

import { Loading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Action } from 'lib-common/generated/action'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { PlacementResponse } from 'lib-common/generated/api-types/placement'
import { ChildId } from 'lib-common/generated/api-types/shared'
import { pendingQuery, useQueryResult } from 'lib-common/query'

import { childQuery, placementsQuery } from './queries'

export interface ChildState {
  childId: ChildId | undefined
  person: Result<PersonJSON>
  permittedActions: Set<Action.Child | Action.Person>
  placements: Result<PlacementResponse>
  assistanceNeedVoucherCoefficientsEnabled: Result<boolean>
  consecutivePlacementRanges: FiniteDateRange[]
}

const emptyPermittedActions = new Set<Action.Child | Action.Person>()

const defaultState: ChildState = {
  childId: undefined,
  person: Loading.of(),
  permittedActions: emptyPermittedActions,
  placements: Loading.of(),
  assistanceNeedVoucherCoefficientsEnabled: Loading.of(),
  consecutivePlacementRanges: []
}

export const ChildContext = createContext<ChildState>(defaultState)

export const ChildContextProvider = React.memo(function ChildContextProvider({
  id,
  children
}: {
  id: ChildId
  children: React.ReactNode
}) {
  const childResponse = useQueryResult(childQuery({ childId: id }))
  const permittedActions = useMemo(() => {
    return childResponse
      .map(
        ({ permittedActions, permittedPersonActions }) =>
          new Set([...permittedActions, ...permittedPersonActions])
      )
      .getOrElse(emptyPermittedActions)
  }, [childResponse])
  const assistanceNeedVoucherCoefficientsEnabled = useMemo(
    () =>
      childResponse.map((res) => res.assistanceNeedVoucherCoefficientsEnabled),
    [childResponse]
  )

  const person = useMemo(
    () => childResponse.map((response) => response.person),
    [childResponse]
  )

  const placements = useQueryResult(
    permittedActions.has('READ_PLACEMENT')
      ? placementsQuery({ childId: id })
      : pendingQuery<PlacementResponse>()
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
      permittedActions,
      placements,
      assistanceNeedVoucherCoefficientsEnabled,
      consecutivePlacementRanges
    }),
    [
      id,
      person,
      permittedActions,
      placements,
      assistanceNeedVoucherCoefficientsEnabled,
      consecutivePlacementRanges
    ]
  )

  return <ChildContext.Provider value={value}>{children}</ChildContext.Provider>
})
