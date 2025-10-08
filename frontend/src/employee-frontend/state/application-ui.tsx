// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Dispatch, SetStateAction } from 'react'
import React, {
  useMemo,
  useState,
  createContext,
  useContext,
  useCallback
} from 'react'

import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import type {
  ApplicationBasis,
  ApplicationStatusOption,
  ApplicationTypeToggle,
  TransferApplicationFilter
} from 'lib-common/generated/api-types/application'
import type { DaycareCareArea } from 'lib-common/generated/api-types/daycare'
import type {
  ApplicationId,
  AreaId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'

import type { PreschoolType } from '../components/applications/ApplicationsFilters'
import type {
  ApplicationDateType,
  ApplicationSummaryStatusOptions,
  ApplicationDistinctions
} from '../components/common/Filters'
import { areasQuery } from '../queries'

import { UserContext } from './user'

type PlacementMode = 'list' | 'desktop'

interface UIState {
  page: number
  setPage: (p: number) => void
  searchFilters: RawApplicationSearchFilters
  setSearchFilters: Dispatch<SetStateAction<RawApplicationSearchFilters>>
  confirmedSearchFilters: ApplicationSearchFilters | undefined
  confirmSearchFilters: () => void
  clearSearchFilters: () => void

  availableAreas: Result<DaycareCareArea[]>

  checkedIds: ApplicationId[]
  setCheckedIds: (applicationIds: ApplicationId[]) => void
  showCheckboxes: boolean

  placementMode: PlacementMode
  setPlacementMode: (mode: PlacementMode) => void
}

interface RawApplicationSearchFilters {
  area: AreaId[]
  units: DaycareId[]
  basis: ApplicationBasis[]
  status: ApplicationSummaryStatusOptions
  type: ApplicationTypeToggle
  startDate: string
  endDate: string
  dateType: ApplicationDateType[]
  searchTerms: string
  transferApplications: TransferApplicationFilter
  voucherApplications: VoucherApplicationFilter
  preschoolType: PreschoolType[]
  allStatuses: ApplicationStatusOption[]
  distinctions: ApplicationDistinctions[]
}

export interface ApplicationSearchFilters
  extends Omit<RawApplicationSearchFilters, 'startDate' | 'endDate'> {
  startDate: LocalDate | null
  endDate: LocalDate | null
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
    startDate: '',
    endDate: '',
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

  checkedIds: [],
  setCheckedIds: () => undefined,
  showCheckboxes: false,

  placementMode: 'list',
  setPlacementMode: () => undefined
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
      useState<RawApplicationSearchFilters>(defaultState.searchFilters)
    const setSearchFilters = useCallback(
      (value: React.SetStateAction<RawApplicationSearchFilters>) => {
        _setSearchFilters(value)
        setConfirmedSearchFilters(undefined)
      },
      []
    )

    const confirmSearchFilters = useCallback(() => {
      const startDate = LocalDate.parseFiOrNull(searchFilters.startDate)
      const endDate = LocalDate.parseFiOrNull(searchFilters.endDate)

      // reformat / clear if invalid
      _setSearchFilters((prev) => ({
        ...prev,
        startDate: startDate ? startDate.format() : '',
        endDate: endDate ? endDate.format() : ''
      }))

      setConfirmedSearchFilters({
        ...searchFilters,
        startDate,
        endDate
      })
      setPage(defaultState.page)
    }, [searchFilters])

    const clearSearchFilters = useCallback(
      () => setSearchFilters(defaultState.searchFilters),
      [setSearchFilters]
    )

    const availableAreas = useQueryResult(areasQuery(), { enabled: loggedIn })

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

    const [placementMode, setPlacementMode] = useState<'list' | 'desktop'>(
      'list'
    )

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
        checkedIds,
        setCheckedIds,
        showCheckboxes,
        placementMode,
        setPlacementMode
      }),
      [
        page,
        searchFilters,
        confirmedSearchFilters,
        setSearchFilters,
        confirmSearchFilters,
        clearSearchFilters,
        availableAreas,
        checkedIds,
        showCheckboxes,
        placementMode,
        setPlacementMode
      ]
    )

    return (
      <ApplicationUIContext.Provider value={value}>
        {children}
      </ApplicationUIContext.Provider>
    )
  }
)
