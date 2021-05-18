// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'
import { Loading, Result } from 'lib-common/api'
import { ServiceNeedOptionPublicInfo } from 'lib-common/api-types/serviceNeed/common'

export interface DaycareApplicationState {
  serviceNeedOptions: Result<ServiceNeedOptionPublicInfo[]>
  setServiceNeedOptions: (request: Result<ServiceNeedOptionPublicInfo[]>) => void
}

const defaultState: DaycareApplicationState = {
  serviceNeedOptions: Loading.of(),
  setServiceNeedOptions: () => undefined
}

export const DaycareApplicationContext = createContext<DaycareApplicationState>(defaultState)

export const DaycareApplicationContextProvider = React.memo(function DaycareApplicationContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [serviceNeedOptions, setServiceNeedOptions] = useState<Result<ServiceNeedOptionPublicInfo[]>>(defaultState.serviceNeedOptions)

  const value = useMemo(
    () => ({
      serviceNeedOptions,
      setServiceNeedOptions
    }),
    [
      serviceNeedOptions,
      setServiceNeedOptions
    ]
  )

  return <DaycareApplicationContext.Provider value={value}>{children}</DaycareApplicationContext.Provider>
})
