// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { Loading, Result, Success } from 'lib-common/api'
import { Child } from 'lib-common/generated/api-types/children'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getChildren } from './api'

interface ChildrenContext {
  children: Result<Child[]>
  childrenWithOwnPage: Result<Child[]>
}

const defaultValue: ChildrenContext = {
  children: Loading.of(),
  childrenWithOwnPage: Loading.of()
}

export const ChildrenContext = createContext(defaultValue)

export const ChildrenContextProvider = React.memo(
  function ChildrenContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const user = useUser()

    const [childrenResponse] = useApiState(
      () => (!user ? Promise.resolve(Success.of([])) : getChildren()),
      [user]
    )

    const childrenWithOwnPage = useMemo(
      () =>
        childrenResponse.map((children) =>
          children.filter(
            (child) =>
              child.hasUpcomingPlacements ||
              child.hasPedagogicalDocuments ||
              child.hasCurriculums
          )
        ),
      [childrenResponse]
    )

    return (
      <ChildrenContext.Provider
        value={{
          children: childrenResponse,
          childrenWithOwnPage
        }}
      >
        {children}
      </ChildrenContext.Provider>
    )
  }
)
