// SPDX-FileCopyrightText: 2017-2020 City of Espoo
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
import LocalDate from '@evaka/lib-common/src/local-date'
import { Unit } from '../types/invoicing'
import { Result, Loading, Paged } from '@evaka/lib-common/src/api'
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
import { useDebounce } from '@evaka/lib-common/src/utils/useDebounce'
import { UUID } from '../types'
import { CareArea } from '../types/unit'
import { ApplicationListSummary } from '../types/application'

// Nothing in here yet. Filters will be added here in next PR.

// Some checkbox handling can be copied from git history
// when needed if it still belongs in context.

interface UIState {
  applicationsResult: Result<Paged<ApplicationListSummary>>
  setApplicationsResult: (result: Result<Paged<ApplicationListSummary>>) => void
  area: string[]
  setArea: (areas: string[]) => void
  availableAreas: Result<CareArea[]>
  setAvailableAreas: Dispatch<SetStateAction<Result<CareArea[]>>>
  units: string[]
  setUnits: (units: string[]) => void
  type: ApplicationTypeToggle
  setType: (type: ApplicationTypeToggle) => void
  status: ApplicationSummaryStatusOptions
  setStatus: (status: ApplicationSummaryStatusOptions) => void
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
  clearSearchFilters: () => void
  basis: ApplicationBasis[]
  setBasis: (basis: ApplicationBasis[]) => void
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

export type VoucherApplicationFilter =
  | 'VOUCHER_FIRST_CHOICE'
  | 'VOUCHER_ONLY'
  | 'NO_VOUCHER'
  | undefined

const defaultState: UIState = {
  applicationsResult: Loading.of(),
  setApplicationsResult: () => undefined,
  area: [],
  setArea: () => undefined,
  availableAreas: Loading.of(),
  setAvailableAreas: () => undefined,
  units: [],
  setUnits: () => undefined,
  type: 'ALL',
  setType: () => undefined,
  status: 'SENT' as const,
  setStatus: () => undefined,
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
  clearSearchFilters: () => undefined,
  basis: [],
  setBasis: () => undefined,
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
      Result<Paged<ApplicationListSummary>>
    >(Loading.of())
    const [area, setArea] = useState<string[]>(defaultState.area)
    const [availableAreas, setAvailableAreas] = useState<Result<CareArea[]>>(
      defaultState.availableAreas
    )
    const [status, setStatus] = useState<ApplicationSummaryStatusOptions>(
      defaultState.status
    )
    const [dateType, setDateType] = useState<ApplicationDateType[]>(
      defaultState.dateType
    )
    const [units, setUnits] = useState<string[]>(defaultState.units)
    const [type, setType] = useState<ApplicationTypeToggle>(defaultState.type)
    const [allUnits, setAllUnits] = useState<Result<Unit[]>>(
      defaultState.allUnits
    )
    const [startDate, setStartDate] = useState(defaultState.startDate)
    const [endDate, setEndDate] = useState(defaultState.endDate)
    const [searchTerms, setSearchTerms] = useState<string>(
      defaultState.searchTerms
    )
    const [distinctions, setDistinctions] = useState<ApplicationDistinctions[]>(
      defaultState.distinctions
    )
    const [
      transferApplications,
      setTransferApplications
    ] = useState<TransferApplicationFilter>(defaultState.transferApplications)
    const [
      voucherApplications,
      setVoucherApplications
    ] = useState<VoucherApplicationFilter>(defaultState.voucherApplications)
    const debouncedSearchTerms = useDebounce(searchTerms, 500)

    const clearSearchFilters = useCallback(() => {
      setArea(defaultState.area)
      setUnits(defaultState.units)
      setBasis(defaultState.basis)
      setType(defaultState.type)
      setStatus(defaultState.status)
      setStartDate(defaultState.startDate)
      setEndDate(defaultState.endDate)
      setDateType(defaultState.dateType)
    }, [])
    const [basis, setBasis] = useState<ApplicationBasis[]>(defaultState.basis)
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
        area,
        setArea,
        availableAreas,
        setAvailableAreas,
        status,
        setStatus,
        dateType,
        setDateType,
        startDate,
        setStartDate,
        endDate,
        setEndDate,
        units,
        setUnits,
        type,
        setType,
        allUnits,
        setAllUnits,
        searchTerms,
        setSearchTerms,
        debouncedSearchTerms,
        clearSearchFilters,
        basis,
        setBasis,
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
        area,
        setArea,
        availableAreas,
        setAvailableAreas,
        status,
        setStatus,
        dateType,
        setDateType,
        startDate,
        setStartDate,
        endDate,
        setEndDate,
        units,
        setUnits,
        type,
        setType,
        allUnits,
        setAllUnits,
        searchTerms,
        setSearchTerms,
        debouncedSearchTerms,
        clearSearchFilters,
        basis,
        setBasis,
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
