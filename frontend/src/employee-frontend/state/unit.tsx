// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Dispatch, SetStateAction } from 'react'
import React, { createContext, useMemo, useState } from 'react'

import type { Result } from 'lib-common/api'
import { Loading, Success } from 'lib-common/api'
import type { UnitNotifications } from 'lib-common/generated/api-types/daycare'
import type { DaycareAclRow } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import type { UnitResponse } from '../api/unit'
import {
  getDaycare,
  getDaycareAclRows,
  getUnitNotifications
} from '../api/unit'
import { UnitFilters } from '../utils/UnitFilters'

export interface UnitState {
  unitId: UUID
  unitInformation: Result<UnitResponse>
  unitNotifications: Result<UnitNotifications>
  reloadUnitNotifications: () => void
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
  daycareAclRows: Result<DaycareAclRow[]>
  reloadDaycareAclRows: () => void
}

const defaultState: UnitState = {
  unitId: '',
  unitInformation: Loading.of(),
  unitNotifications: Loading.of(),
  reloadUnitNotifications: () => undefined,
  filters: new UnitFilters(LocalDate.todayInSystemTz(), '1 day'),
  setFilters: () => undefined,
  daycareAclRows: Loading.of(),
  reloadDaycareAclRows: () => undefined
}

export const UnitContext = createContext<UnitState>(defaultState)

export const UnitContextProvider = React.memo(function UnitContextProvider({
  id,
  children
}: {
  id: UUID
  children: JSX.Element
}) {
  const [filters, setFilters] = useState(defaultState.filters)
  const [unitInformation] = useApiState(() => getDaycare(id), [id])
  const [unitNotifications, reloadUnitNotifications] = useApiState(
    () => getUnitNotifications(id),
    [id]
  )
  const [daycareAclRows, reloadDaycareAclRows] = useApiState(
    () =>
      unitInformation.isSuccess &&
      unitInformation.value.permittedActions.has('READ_ACL')
        ? getDaycareAclRows(id)
        : Promise.resolve(Success.of([])),
    [id, unitInformation]
  )

  const value = useMemo(
    () => ({
      unitId: id,
      unitInformation,
      unitNotifications,
      reloadUnitNotifications,
      filters,
      setFilters,
      daycareAclRows,
      reloadDaycareAclRows
    }),
    [
      id,
      unitInformation,
      unitNotifications,
      reloadUnitNotifications,
      filters,
      daycareAclRows,
      reloadDaycareAclRows
    ]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
