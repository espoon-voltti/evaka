// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useMemo,
  useState,
  createContext,
  Dispatch,
  SetStateAction,
  useContext,
  useCallback
} from 'react'

import { Result, Loading } from 'lib-common/api'
import {
  ApplicationBasis,
  ApplicationStatusOption,
  ApplicationTypeToggle,
  TransferApplicationFilter
} from 'lib-common/generated/api-types/application'
import {
  DaycareCareArea,
  UnitStub
} from 'lib-common/generated/api-types/daycare'
import {
  ApplicationId,
  AreaId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'

import {
  ApplicationDateType,
  ApplicationSummaryStatusOptions,
  PreschoolType,
  ApplicationDistinctions
} from '../components/common/Filters'
import { areaQuery } from '../components/unit/queries'

import { UserContext } from './user'

interface UIState {
  page: number
  setPage: (p: number) => void
  searchFilters: ApplicationSearchFilters
  setSearchFilters: Dispatch<SetStateAction<ApplicationSearchFilters>>
  confirmedSearchFilters: ApplicationSearchFilters | undefined
  confirmSearchFilters: () => void
  clearSearchFilters: () => void

  availableAreas: Result<DaycareCareArea[]>
  allUnits: Result<UnitStub[]>
  setAllUnits: Dispatch<SetStateAction<Result<UnitStub[]>>>

  checkedIds: ApplicationId[]
  setCheckedIds: (applicationIds: ApplicationId[]) => void
  showCheckboxes: boolean
}

export interface ApplicationSearchFilters {
  area: AreaId[]
  units: DaycareId[]
  basis: ApplicationBasis[]
  status: ApplicationSummaryStatusOptions
  type: ApplicationTypeToggle
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  dateType: ApplicationDateType[]
  searchTerms: string
  transferApplications: TransferApplicationFilter
  voucherApplications: VoucherApplicationFilter
  preschoolType: PreschoolType[]
  allStatuses: ApplicationStatusOption[]
  distinctions: ApplicationDistinctions[]
}

export type VoucherApplicationFilter =
  | 'VOUCHER_FIRST_CHOICE'
  | 'VOUCHER_ONLY'
  | 'NO_VOUCHER'
  | undefined

const defaultState: UIState = {
  page: 1,
  setPage: () => undefined,
  searchFilters: {
    area: [],
    units: [],
    basis: [],
    status: 'SENT',
    type: 'ALL',
    startDate: undefined,
    endDate: undefined,
    dateType: [],
    searchTerms: '',
    transferApplications: 'ALL',
    voucherApplications: undefined,
    preschoolType: [],
    allStatuses: [],
    distinctions: []
  },
  setSearchFilters: () => undefined,
  confirmedSearchFilters: undefined,
  confirmSearchFilters: () => undefined,
  clearSearchFilters: () => undefined,

  availableAreas: Loading.of(),
  allUnits: Loading.of(),
  setAllUnits: () => undefined,

  checkedIds: [],
  setCheckedIds: () => undefined,
  showCheckboxes: false
}

export const ApplicationUIContext = createContext<UIState>(defaultState)

export const ApplicationUIContextProvider = React.memo(
  function ApplicationUIContextProvider({
    children
  }: {
    children: React.JSX.Element
  }) {
    const { loggedIn } = useContext(UserContext)

    const [page, setPage] = useState<number>(defaultState.page)
    const [confirmedSearchFilters, setConfirmedSearchFilters] = useState<
      ApplicationSearchFilters | undefined
    >(defaultState.confirmedSearchFilters)
    const [searchFilters, _setSearchFilters] =
      useState<ApplicationSearchFilters>(defaultState.searchFilters)
    const setSearchFilters = useCallback(
      (value: React.SetStateAction<ApplicationSearchFilters>) => {
        _setSearchFilters(value)
        setConfirmedSearchFilters(undefined)
      },
      []
    )
    const confirmSearchFilters = useCallback(() => {
      setConfirmedSearchFilters(searchFilters)
      setPage(defaultState.page)
    }, [searchFilters])
    const clearSearchFilters = useCallback(
      () => setSearchFilters(defaultState.searchFilters),
      [setSearchFilters]
    )

    const availableAreas = useQueryResult(areaQuery(), { enabled: loggedIn })
    const [allUnits, setAllUnits] = useState<Result<UnitStub[]>>(
      defaultState.allUnits
    )

    const [checkedIds, setCheckedIds] = useState<ApplicationId[]>(
      defaultState.checkedIds
    )
    const showCheckboxes = confirmedSearchFilters
      ? [
          'SENT',
          'WAITING_PLACEMENT',
          'WAITING_DECISION',
          'WAITING_UNIT_CONFIRMATION'
        ].includes(confirmedSearchFilters.status)
      : false

    const value = useMemo(
      () => ({
        page,
        setPage,
        searchFilters,
        confirmedSearchFilters,
        setSearchFilters,
        confirmSearchFilters,
        clearSearchFilters,
        availableAreas,
        allUnits,
        setAllUnits,
        checkedIds,
        setCheckedIds,
        showCheckboxes
      }),
      [
        page,
        searchFilters,
        confirmedSearchFilters,
        setSearchFilters,
        confirmSearchFilters,
        clearSearchFilters,
        availableAreas,
        allUnits,
        checkedIds,
        showCheckboxes
      ]
    )

    return (
      <ApplicationUIContext.Provider value={value}>
        {children}
      </ApplicationUIContext.Provider>
    )
  }
)
