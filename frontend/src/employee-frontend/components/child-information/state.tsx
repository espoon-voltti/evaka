// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo } from 'react'

import { Loading, Result } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { ChildId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'

import { childQuery } from './queries'

export interface ChildState {
  childId: ChildId | undefined
  person: Result<PersonJSON>
  permittedActions: Set<Action.Child | Action.Person>
  assistanceNeedVoucherCoefficientsEnabled: Result<boolean>
}

const emptyPermittedActions = new Set<Action.Child | Action.Person>()

const defaultState: ChildState = {
  childId: undefined,
  person: Loading.of(),
  permittedActions: emptyPermittedActions,
  assistanceNeedVoucherCoefficientsEnabled: Loading.of()
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

  const value = useMemo(
    (): ChildState => ({
      childId: id,
      person,
      permittedActions,
      assistanceNeedVoucherCoefficientsEnabled
    }),
    [id, person, permittedActions, assistanceNeedVoucherCoefficientsEnabled]
  )

  return <ChildContext.Provider value={value}>{children}</ChildContext.Provider>
})
