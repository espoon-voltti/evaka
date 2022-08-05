// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { Success } from 'lib-common/api'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getApplicationNotifications } from './api'

interface ApplicationsContext {
  waitingConfirmationCount: number
  refreshWaitingConfirmationCount: () => void
}

const defaultValue: ApplicationsContext = {
  waitingConfirmationCount: 0,
  refreshWaitingConfirmationCount: () => undefined
}

export const ApplicationsContext = createContext(defaultValue)

export const ApplicationsContextProvider = React.memo(
  function ApplicationsContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const user = useUser()

    const [waitingConfirmationCount, refreshWaitingConfirmationCount] =
      useApiState(
        () =>
          !user
            ? Promise.resolve(Success.of(0))
            : getApplicationNotifications(),
        [user]
      )

    return (
      <ApplicationsContext.Provider
        value={{
          waitingConfirmationCount: waitingConfirmationCount.getOrElse(0),
          refreshWaitingConfirmationCount
        }}
      >
        {children}
      </ApplicationsContext.Provider>
    )
  }
)
