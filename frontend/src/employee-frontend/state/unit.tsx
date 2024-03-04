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

import { Loading, Result, Success, wrapResult } from 'lib-common/api'
import { DaycareResponse } from 'lib-common/generated/api-types/daycare'
import { DaycareAclRow } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import { unitQuery } from '../components/unit/queries'
import { getDaycareAcl } from '../generated/api-clients/daycare'
import { UnitFilters } from '../utils/UnitFilters'

const getDaycareAclResult = wrapResult(getDaycareAcl)

export interface UnitState {
  unitId: UUID
  unitInformation: Result<DaycareResponse>
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
  children: React.JSX.Element
}) {
  const [filters, setFilters] = useState(defaultState.filters)
  const unitInformation = useQueryResult(unitQuery({ daycareId: id }))

  const [daycareAclRows, reloadDaycareAclRows] = useApiState(
    () =>
      unitInformation.isSuccess &&
      unitInformation.value.permittedActions.includes('READ_ACL')
        ? getDaycareAclResult({ daycareId: id })
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
