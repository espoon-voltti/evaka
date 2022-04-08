// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useMemo,
  useState,
  createContext,
  Dispatch,
  SetStateAction,
  useCallback
} from 'react'

import { Result, Loading, Paged } from 'lib-common/api'
import { ApplicationSummary } from 'lib-common/generated/api-types/application'
import { DaycareCareArea } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useDebounce } from 'lib-common/utils/useDebounce'

import { Unit } from '../api/daycare'
import {
  ApplicationDateType,
  ApplicationTypeToggle,
  ApplicationBasis,
  ApplicationSummaryStatusOptions,
  PreschoolType,
  ApplicationSummaryStatusAllOptions,
  ApplicationDistinctions,
  TransferApplicationFilter
} from '../components/common/Filters'

interface UIState {
  applicationsResult: Result<Paged<ApplicationSummary>>
  setApplicationsResult: (result: Result<Paged<ApplicationSummary>>) => void
  availableAreas: Result<DaycareCareArea[]>
  setAvailableAreas: Dispatch<SetStateAction<Result<DaycareCareArea[]>>>
  allUnits: Result<Unit[]>
  setAllUnits: Dispatch<SetStateAction<Result<Unit[]>>>
  applicationSearchFilters: ApplicationSearchFilters
  setApplicationSearchFilters: (
    applicationFilters: ApplicationSearchFilters
  ) => void
  debouncedApplicationSearchFilters: ApplicationSearchFilters
  clearSearchFilters: () => void
  checkedIds: string[]
  setCheckedIds: (applicationIds: UUID[]) => void
  showCheckboxes: boolean
  preschoolType: PreschoolType[]
  setPreschoolType: (type: PreschoolType[]) => void
  allStatuses: ApplicationSummaryStatusAllOptions[]
  setAllStatuses: (statuses: ApplicationSummaryStatusAllOptions[]) => void
  distinctions: ApplicationDistinctions[]
  setDistinctions: (distinctions: ApplicationDistinctions[]) => void
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
  voucherApplications: undefined
}

const defaultState: UIState = {
  applicationsResult: Loading.of(),
  setApplicationsResult: () => undefined,
  availableAreas: Loading.of(),
  setAvailableAreas: () => undefined,
  allUnits: Loading.of(),
  setAllUnits: () => undefined,
  applicationSearchFilters: clearApplicationSearchFilters,
  setApplicationSearchFilters: () => undefined,
  debouncedApplicationSearchFilters: clearApplicationSearchFilters,
  clearSearchFilters: () => undefined,
  checkedIds: [],
  setCheckedIds: () => undefined,
  showCheckboxes: false,
  preschoolType: [],
  setPreschoolType: () => undefined,
  allStatuses: [],
  setAllStatuses: () => undefined,
  distinctions: [],
  setDistinctions: () => undefined
}

export const ApplicationUIContext = createContext<UIState>(defaultState)

export const ApplicationUIContextProvider = React.memo(
  function ApplicationUIContextProvider({
    children
  }: {
    children: JSX.Element
  }) {
    const [applicationsResult, setApplicationsResult] = useState<
      Result<Paged<ApplicationSummary>>
    >(Loading.of())
    const [availableAreas, setAvailableAreas] = useState<
      Result<DaycareCareArea[]>
    >(defaultState.availableAreas)
    const [allUnits, setAllUnits] = useState<Result<Unit[]>>(
      defaultState.allUnits
    )
    const [applicationSearchFilters, setApplicationSearchFilters] =
      useState<ApplicationSearchFilters>(defaultState.applicationSearchFilters)
    const [distinctions, setDistinctions] = useState<ApplicationDistinctions[]>(
      defaultState.distinctions
    )

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
    const [preschoolType, setPreschoolType] = useState<PreschoolType[]>(
      defaultState.preschoolType
    )
    const [allStatuses, setAllStatuses] = useState<
      ApplicationSummaryStatusAllOptions[]
    >(defaultState.allStatuses)
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
        setAvailableAreas,
        allUnits,
        setAllUnits,
        applicationSearchFilters,
        setApplicationSearchFilters,
        debouncedApplicationSearchFilters,
        clearSearchFilters,
        checkedIds,
        setCheckedIds,
        showCheckboxes,
        preschoolType,
        setPreschoolType,
        allStatuses,
        setAllStatuses,
        distinctions,
        setDistinctions
      }),
      [
        applicationsResult,
        setApplicationsResult,
        availableAreas,
        setAvailableAreas,
        allUnits,
        setAllUnits,
        applicationSearchFilters,
        setApplicationSearchFilters,
        debouncedApplicationSearchFilters,
        clearSearchFilters,
        checkedIds,
        setCheckedIds,
        showCheckboxes,
        preschoolType,
        setPreschoolType,
        allStatuses,
        setAllStatuses,
        distinctions,
        setDistinctions
      ]
    )

    return (
      <ApplicationUIContext.Provider value={value}>
        {children}
      </ApplicationUIContext.Provider>
    )
  }
)
