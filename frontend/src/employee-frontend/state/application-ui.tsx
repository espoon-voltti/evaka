// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useMemo,
  useState,
  createContext,
  Dispatch,
  SetStateAction,
  useCallback,
  useContext
} from 'react'

import { Result, Loading } from 'lib-common/api'
import {
  ApplicationStatusOption,
  ApplicationTypeToggle,
  PagedApplicationSummaries,
  TransferApplicationFilter
} from 'lib-common/generated/api-types/application'
import {
  DaycareCareArea,
  UnitStub
} from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useDebounce } from 'lib-common/utils/useDebounce'

import {
  ApplicationDateType,
  ApplicationBasis,
  ApplicationSummaryStatusOptions,
  PreschoolType,
  ApplicationDistinctions
} from '../components/common/Filters'
import { areaQuery } from '../components/unit/queries'

import { UserContext } from './user'

interface UIState {
  applicationsResult: Result<PagedApplicationSummaries>
  setApplicationsResult: (result: Result<PagedApplicationSummaries>) => void
  availableAreas: Result<DaycareCareArea[]>
  allUnits: Result<UnitStub[]>
  setAllUnits: Dispatch<SetStateAction<Result<UnitStub[]>>>
  applicationSearchFilters: ApplicationSearchFilters
  setApplicationSearchFilters: (
    applicationFilters: ApplicationSearchFilters
  ) => void
  debouncedApplicationSearchFilters: ApplicationSearchFilters
  clearSearchFilters: () => void
  checkedIds: string[]
  setCheckedIds: (applicationIds: UUID[]) => void
  showCheckboxes: boolean
}

interface ApplicationSearchFilters {
  area: string[]
  units: string[]
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

const clearApplicationSearchFilters: ApplicationSearchFilters = {
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
}

const defaultState: UIState = {
  applicationsResult: Loading.of(),
  setApplicationsResult: () => undefined,
  availableAreas: Loading.of(),
  allUnits: Loading.of(),
  setAllUnits: () => undefined,
  applicationSearchFilters: clearApplicationSearchFilters,
  setApplicationSearchFilters: () => undefined,
  debouncedApplicationSearchFilters: clearApplicationSearchFilters,
  clearSearchFilters: () => undefined,
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

    const [applicationsResult, setApplicationsResult] = useState<
      Result<PagedApplicationSummaries>
    >(Loading.of())
    const availableAreas = useQueryResult(areaQuery(), { enabled: loggedIn })
    const [allUnits, setAllUnits] = useState<Result<UnitStub[]>>(
      defaultState.allUnits
    )
    const [applicationSearchFilters, setApplicationSearchFilters] =
      useState<ApplicationSearchFilters>(defaultState.applicationSearchFilters)
    const clearSearchFilters = useCallback(() => {
      setApplicationSearchFilters(defaultState.applicationSearchFilters)
    }, [])

    const debouncedApplicationSearchFilters = useDebounce(
      applicationSearchFilters,
      500
    )

    const [checkedIds, setCheckedIds] = useState<string[]>(
      defaultState.checkedIds
    )
    const showCheckboxes = [
      'SENT',
      'WAITING_PLACEMENT',
      'WAITING_DECISION',
      'WAITING_UNIT_CONFIRMATION'
    ].includes(applicationSearchFilters.status)

    const value = useMemo(
      () => ({
        applicationsResult,
        setApplicationsResult,
        availableAreas,
        allUnits,
        setAllUnits,
        applicationSearchFilters,
        setApplicationSearchFilters,
        debouncedApplicationSearchFilters,
        clearSearchFilters,
        checkedIds,
        setCheckedIds,
        showCheckboxes
      }),
      [
        applicationsResult,
        setApplicationsResult,
        availableAreas,
        allUnits,
        setAllUnits,
        applicationSearchFilters,
        setApplicationSearchFilters,
        debouncedApplicationSearchFilters,
        clearSearchFilters,
        checkedIds,
        setCheckedIds,
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
