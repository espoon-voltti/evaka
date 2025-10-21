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
  PreferredUnit,
  TransferApplicationFilter
} from 'lib-common/generated/api-types/application'
import {
  applicationBasisOptions,
  applicationDateTypeOptions,
  applicationStatusOptions
} from 'lib-common/generated/api-types/application'
import type { DaycareCareArea } from 'lib-common/generated/api-types/daycare'
import type {
  ApplicationId,
  AreaId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { toSimpleHash } from 'lib-common/string'

import type { PreschoolType } from '../components/applications/ApplicationsFilters'
import { preschoolTypes } from '../components/applications/ApplicationsFilters'
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
  placementDesktopDaycares: PreferredUnit[] | undefined
  setPlacementDesktopDaycares: Dispatch<
    SetStateAction<PreferredUnit[] | undefined>
  >
  occupancyPeriodStart: LocalDate
  setOccupancyPeriodStart: (date: LocalDate) => void
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

// This example data is used for creating a hash to detect changes in the typings and
// invalidate local storage data when the structure changes.
const constantUuid = '00000000-0000-0000-0000-000000000000'
const exampleFilters: RawApplicationSearchFilters = {
  area: [fromUuid(constantUuid)],
  units: [fromUuid(constantUuid)],
  basis: [...applicationBasisOptions],
  status: 'SENT',
  type: 'ALL',
  startDate: '31.12.2000',
  endDate: '31.12.2000',
  dateType: [...applicationDateTypeOptions],
  searchTerms: '',
  transferApplications: 'ALL',
  voucherApplications: 'VOUCHER_FIRST_CHOICE',
  preschoolType: [...preschoolTypes],
  allStatuses: [...applicationStatusOptions],
  distinctions: ['SECONDARY']
}
const manualVersionNumber = 1 // override to force invalidation
const versionHash = `${toSimpleHash(JSON.stringify(exampleFilters))}-${manualVersionNumber}`
const localStorageKey = `application-search-filters:${versionHash}`

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
  setPlacementMode: () => undefined,
  placementDesktopDaycares: undefined,
  setPlacementDesktopDaycares: () => undefined,
  occupancyPeriodStart: LocalDate.todayInHelsinkiTz(),
  setOccupancyPeriodStart: () => undefined
}

export const ApplicationUIContext = createContext<UIState>(defaultState)

export const ApplicationUIContextProvider = React.memo(
  function ApplicationUIContextProvider({
    children
  }: {
    children: React.JSX.Element
  }) {
    const { loggedIn } = useContext(UserContext)

    const [placementMode, setPlacementMode] = useState<'list' | 'desktop'>(
      defaultState.placementMode
    )
    const [placementDesktopDaycares, setPlacementDesktopDaycares] =
      useState<PreferredUnit[]>()
    const [occupancyPeriodStart, setOccupancyPeriodStart] = useState<LocalDate>(
      defaultState.occupancyPeriodStart
    )

    const [page, setPage] = useState<number>(defaultState.page)
    const [confirmedSearchFilters, setConfirmedSearchFilters] = useState<
      ApplicationSearchFilters | undefined
    >(defaultState.confirmedSearchFilters)
    const storedFiltersJson = useMemo(
      () => localStorage.getItem(localStorageKey),
      []
    )
    const [searchFilters, _setSearchFilters] =
      useState<RawApplicationSearchFilters>(
        storedFiltersJson
          ? (JSON.parse(storedFiltersJson) as RawApplicationSearchFilters)
          : defaultState.searchFilters
      )
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

      // reformat / clear date strings if invalid
      const newSearchFilters = {
        ...searchFilters,
        startDate: startDate ? startDate.format() : '',
        endDate: endDate ? endDate.format() : ''
      }
      _setSearchFilters(newSearchFilters)

      localStorage.setItem(localStorageKey, JSON.stringify(newSearchFilters))

      setPlacementDesktopDaycares(undefined)
      if (searchFilters.dateType.some((dt) => dt === 'START' || dt === 'DUE')) {
        setOccupancyPeriodStart(
          startDate ?? endDate ?? LocalDate.todayInHelsinkiTz()
        )
      } else {
        setOccupancyPeriodStart(LocalDate.todayInHelsinkiTz())
      }

      setConfirmedSearchFilters({
        ...searchFilters,
        startDate,
        endDate
      })
      setPage(defaultState.page)
    }, [searchFilters])

    const clearSearchFilters = useCallback(() => {
      localStorage.removeItem(localStorageKey)
      setSearchFilters(defaultState.searchFilters)
    }, [setSearchFilters])

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
        setPlacementMode,
        placementDesktopDaycares,
        setPlacementDesktopDaycares,
        occupancyPeriodStart,
        setOccupancyPeriodStart
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
        setPlacementMode,
        placementDesktopDaycares,
        setPlacementDesktopDaycares,
        occupancyPeriodStart,
        setOccupancyPeriodStart
      ]
    )

    return (
      <ApplicationUIContext.Provider value={value}>
        {children}
      </ApplicationUIContext.Provider>
    )
  }
)
