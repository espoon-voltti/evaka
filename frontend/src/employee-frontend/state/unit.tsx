// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  Dispatch,
  SetStateAction,
  useMemo,
  useState
} from 'react'

import { Loading, Result, Success } from 'lib-common/api'
import { DaycareAclRow } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getDaycareAclRows, UnitResponse } from '../api/unit'
import { unitQuery } from '../components/unit/queries'
import { UnitFilters } from '../utils/UnitFilters'

export interface UnitState {
  unitId: UUID
  unitInformation: Result<UnitResponse>
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
  daycareAclRows: Result<DaycareAclRow[]>
  reloadDaycareAclRows: () => void
}

const defaultState: UnitState = {
  unitId: '',
  unitInformation: Loading.of(),
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
  const unitInformation = useQueryResult(unitQuery(id))

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
      filters,
      setFilters,
      daycareAclRows,
      reloadDaycareAclRows
    }),
    [id, unitInformation, filters, daycareAclRows, reloadDaycareAclRows]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
