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

// Nothing in here yet. Filters will be added here in next PR.

// Some checkbox handling can be copied from git history
// when needed if it still belongs in context.

interface UIState {
  applicationsResult: Result<Paged<ApplicationSummary>>
  setApplicationsResult: (result: Result<Paged<ApplicationSummary>>) => void
  availableAreas: Result<DaycareCareArea[]>
  setAvailableAreas: Dispatch<SetStateAction<Result<DaycareCareArea[]>>>
  dateType: ApplicationDateType[]
  setDateType: (dateTypes: ApplicationDateType[]) => void
  startDate: LocalDate | undefined
  setStartDate: (date: LocalDate | undefined) => void
  endDate: LocalDate | undefined
  setEndDate: (date: LocalDate | undefined) => void
  allUnits: Result<Unit[]>
  setAllUnits: Dispatch<SetStateAction<Result<Unit[]>>>
  searchTerms: string
  setSearchTerms: (searchTerms: string) => void
  debouncedSearchTerms: string
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
  transferApplications: TransferApplicationFilter
  setTransferApplications: Dispatch<SetStateAction<TransferApplicationFilter>>
  voucherApplications: VoucherApplicationFilter
  setVoucherApplications: Dispatch<SetStateAction<VoucherApplicationFilter>>
}

interface ApplicationSearchFilters {
  area: string[]
  units: string[]
  basis: ApplicationBasis[]
  status: ApplicationSummaryStatusOptions
  type: ApplicationTypeToggle
  //startDate: LocalDate | undefined
  //endDate: LocalDate | undefined
  //dateType: ApplicationDateType[]
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
  type: 'ALL'
  //startDate: undefined, endDate: undefined, dateType: []
}

const defaultState: UIState = {
  applicationsResult: Loading.of(),
  setApplicationsResult: () => undefined,
  availableAreas: Loading.of(),
  setAvailableAreas: () => undefined,
  dateType: [],
  setDateType: () => undefined,
  startDate: undefined,
  setStartDate: () => undefined,
  endDate: undefined,
  setEndDate: () => undefined,
  allUnits: Loading.of(),
  setAllUnits: () => undefined,
  searchTerms: '',
  setSearchTerms: () => undefined,
  debouncedSearchTerms: '',
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
  setDistinctions: () => undefined,
  transferApplications: 'ALL',
  setTransferApplications: () => undefined,
  voucherApplications: undefined,
  setVoucherApplications: () => undefined
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
    const [dateType, setDateType] = useState<ApplicationDateType[]>(
      defaultState.dateType
    )
    const [allUnits, setAllUnits] = useState<Result<Unit[]>>(
      defaultState.allUnits
    )
    const [startDate, setStartDate] = useState(defaultState.startDate)
    const [endDate, setEndDate] = useState(defaultState.endDate)
    const [searchTerms, setSearchTerms] = useState<string>(
      defaultState.searchTerms
    )
    const [applicationSearchFilters, setApplicationSearchFilters] =
      useState<ApplicationSearchFilters>(defaultState.applicationSearchFilters)
    const [distinctions, setDistinctions] = useState<ApplicationDistinctions[]>(
      defaultState.distinctions
    )
    const [transferApplications, setTransferApplications] =
      useState<TransferApplicationFilter>(defaultState.transferApplications)
    const [voucherApplications, setVoucherApplications] =
      useState<VoucherApplicationFilter>(defaultState.voucherApplications)
    const debouncedSearchTerms = useDebounce(searchTerms, 500)

    const clearSearchFilters = useCallback(() => {
      setApplicationSearchFilters(defaultState.applicationSearchFilters)
      setStartDate(defaultState.startDate)
      setEndDate(defaultState.endDate)
      setDateType(defaultState.dateType)
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
    ].includes(status)

    const value = useMemo(
      () => ({
        applicationsResult,
        setApplicationsResult,
        availableAreas,
        setAvailableAreas,
        dateType,
        setDateType,
        startDate,
        setStartDate,
        endDate,
        setEndDate,
        allUnits,
        setAllUnits,
        searchTerms,
        setSearchTerms,
        debouncedSearchTerms,
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
        setDistinctions,
        transferApplications,
        setTransferApplications,
        voucherApplications,
        setVoucherApplications
      }),
      [
        applicationsResult,
        setApplicationsResult,
        availableAreas,
        setAvailableAreas,
        dateType,
        setDateType,
        startDate,
        setStartDate,
        endDate,
        setEndDate,
        allUnits,
        setAllUnits,
        searchTerms,
        setSearchTerms,
        debouncedSearchTerms,
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
        setDistinctions,
        transferApplications,
        setTransferApplications,
        voucherApplications,
        setVoucherApplications
      ]
    )

    return (
      <ApplicationUIContext.Provider value={value}>
        {children}
      </ApplicationUIContext.Provider>
    )
  }
)
