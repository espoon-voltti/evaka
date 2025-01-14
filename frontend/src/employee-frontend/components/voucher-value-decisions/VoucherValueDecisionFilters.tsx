// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Fragment,
  useCallback,
  useContext,
  useEffect,
  useState
} from 'react'
import { useMemo } from 'react'

import { wrapResult } from 'lib-common/api'
import {
  VoucherValueDecisionDifference,
  VoucherValueDecisionDistinctiveParams,
  VoucherValueDecisionStatus
} from 'lib-common/generated/api-types/invoicing'
import { DaycareId, EmployeeId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { Gap } from 'lib-components/white-space'

import { getUnits } from '../../generated/api-clients/daycare'
import { getFinanceDecisionHandlers } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import {
  InvoicingUiContext,
  ValueDecisionSearchFilters
} from '../../state/invoicing-ui'
import {
  AreaFilter,
  Filters,
  ValueDecisionStatusFilter,
  UnitFilter,
  FinanceDecisionHandlerFilter,
  ValueDecisionDateFilter,
  VoucherValueDecisionDistinctionsFilter,
  VoucherValueDecisionDifferenceFilter
} from '../common/Filters'

const getUnitsResult = wrapResult(getUnits)
const getFinanceDecisionHandlersResult = wrapResult(getFinanceDecisionHandlers)

const emptyFilters: ValueDecisionSearchFilters = {
  searchTerms: '',
  distinctiveDetails: [],
  statuses: ['DRAFT'],
  area: [],
  difference: [],
  startDate: undefined,
  endDate: LocalDate.todayInSystemTz(),
  searchByStartDate: false
}

export default React.memo(function VoucherValueDecisionFilters() {
  const { i18n } = useTranslation()

  const {
    valueDecisions: { setConfirmedSearchFilters },
    shared: {
      units,
      setUnits,
      financeDecisionHandlers,
      setFinanceDecisionHandlers,
      availableAreas
    }
  } = useContext(InvoicingUiContext)

  const [searchFilters, _setSearchFilters] =
    useState<ValueDecisionSearchFilters>(emptyFilters)
  const setSearchFilters = useCallback(
    (value: React.SetStateAction<ValueDecisionSearchFilters>) => {
      _setSearchFilters(value)
      setConfirmedSearchFilters(undefined)
    },
    [setConfirmedSearchFilters]
  )
  const clearSearchFilters = useCallback(() => {
    _setSearchFilters(emptyFilters)
    setConfirmedSearchFilters(undefined)
  }, [setConfirmedSearchFilters])
  const confirmSearchFilters = useCallback(
    () => setConfirmedSearchFilters(searchFilters),
    [searchFilters, setConfirmedSearchFilters]
  )

  useEffect(() => {
    void getFinanceDecisionHandlersResult().then(setFinanceDecisionHandlers)
  }, [setFinanceDecisionHandlers])

  useEffect(() => {
    void getUnitsResult({ areaIds: null, type: 'DAYCARE', from: null }).then(
      setUnits
    )
  }, [setUnits])

  const selectedFinanceDecisionHandler = useMemo(
    () =>
      financeDecisionHandlers
        .getOrElse([])
        .find(
          (handler) => handler.value === searchFilters.financeDecisionHandlerId
        ),
    [searchFilters.financeDecisionHandlerId] // eslint-disable-line react-hooks/exhaustive-deps
  )

  // remove selected unit filter if the unit is not included in the selected areas
  useEffect(() => {
    if (
      units
        .map(
          (xs) =>
            !!searchFilters.unit &&
            xs.map(({ id }) => id).includes(searchFilters.unit)
        )
        .getOrElse(false)
    ) {
      setSearchFilters((filters) => ({ ...filters, unit: undefined }))
    }
  }, [units]) // eslint-disable-line react-hooks/exhaustive-deps

  const toggleArea = useCallback(
    (code: string) => () => {
      setSearchFilters(({ area, ...filters }) => ({
        ...filters,
        area: area.includes(code)
          ? area.filter((v) => v !== code)
          : [...area, code]
      }))
    },
    [setSearchFilters]
  )

  const selectUnit = useCallback(
    (unit?: DaycareId) => setSearchFilters((filters) => ({ ...filters, unit })),
    [setSearchFilters]
  )

  const selectFinanceDecisionHandler = useCallback(
    (financeDecisionHandlerId?: EmployeeId) =>
      setSearchFilters((filters) => ({ ...filters, financeDecisionHandlerId })),
    [setSearchFilters]
  )

  const toggleDifference = (difference: VoucherValueDecisionDifference) => {
    if (searchFilters.difference.includes(difference)) {
      setSearchFilters({
        ...searchFilters,
        difference: searchFilters.difference.filter((v) => v !== difference)
      })
    } else {
      setSearchFilters({
        ...searchFilters,
        difference: [...searchFilters.difference, difference]
      })
    }
  }

  const toggleStatus = (status: VoucherValueDecisionStatus) => () => {
    if (searchFilters.statuses.includes(status)) {
      setSearchFilters({
        ...searchFilters,
        statuses: searchFilters.statuses.filter((v) => v !== status)
      })
    } else {
      setSearchFilters({
        ...searchFilters,
        statuses: [...searchFilters.statuses, status]
      })
    }
  }

  const setStartDate = (startDate: LocalDate | undefined) => {
    setSearchFilters({
      ...searchFilters,
      startDate: startDate
    })
  }

  const setEndDate = (endDate: LocalDate | undefined) => {
    setSearchFilters({
      ...searchFilters,
      endDate: endDate
    })
  }

  const setSearchByStartDate = (value: boolean) => {
    setSearchFilters({
      ...searchFilters,
      searchByStartDate: value
    })
  }

  const toggleDistinctiveParams =
    (id: VoucherValueDecisionDistinctiveParams) => () => {
      if (searchFilters.distinctiveDetails.includes(id)) {
        setSearchFilters({
          ...searchFilters,
          distinctiveDetails: searchFilters.distinctiveDetails.filter(
            (v) => v !== id
          )
        })
      } else {
        setSearchFilters({
          ...searchFilters,
          distinctiveDetails: [...searchFilters.distinctiveDetails, id]
        })
      }
    }

  return (
    <Filters
      searchPlaceholder={i18n.filters.freeTextPlaceholder}
      freeText={searchFilters.searchTerms}
      setFreeText={(s) =>
        setSearchFilters((prev) => ({ ...prev, searchTerms: s }))
      }
      onSearch={confirmSearchFilters}
      clearFilters={clearSearchFilters}
      column1={
        <>
          <AreaFilter
            areas={availableAreas.getOrElse([])}
            toggled={searchFilters.area}
            toggle={toggleArea}
          />
          <Gap size="L" />
          <UnitFilter
            units={units.getOrElse([])}
            selected={units
              .map((us) => us.find((unit) => unit.id === searchFilters.unit))
              .getOrElse(undefined)}
            select={selectUnit}
          />
          <Gap size="L" />
          <FinanceDecisionHandlerFilter
            financeDecisionHandlers={financeDecisionHandlers.getOrElse([])}
            selected={selectedFinanceDecisionHandler}
            select={selectFinanceDecisionHandler}
          />
        </>
      }
      column2={
        <Fragment>
          <VoucherValueDecisionDistinctionsFilter
            toggled={searchFilters.distinctiveDetails}
            toggle={toggleDistinctiveParams}
          />
          <Gap size="L" />
          <VoucherValueDecisionDifferenceFilter
            toggled={searchFilters.difference}
            toggle={toggleDifference}
          />
        </Fragment>
      }
      column3={
        <>
          <ValueDecisionStatusFilter
            toggled={searchFilters.statuses}
            toggle={toggleStatus}
          />
          <Gap size="L" />
          <ValueDecisionDateFilter
            startDate={searchFilters.startDate}
            setStartDate={setStartDate}
            endDate={searchFilters.endDate}
            setEndDate={setEndDate}
            searchByStartDate={searchFilters.searchByStartDate}
            setSearchByStartDate={setSearchByStartDate}
          />
        </>
      }
    />
  )
})
