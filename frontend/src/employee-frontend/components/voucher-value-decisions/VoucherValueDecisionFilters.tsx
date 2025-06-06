// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useContext, useEffect } from 'react'
import { useMemo } from 'react'

import type { Result } from 'lib-common/api'
import type {
  VoucherValueDecisionDifference,
  VoucherValueDecisionDistinctiveParams,
  VoucherValueDecisionStatus
} from 'lib-common/generated/api-types/invoicing'
import type {
  DaycareId,
  EmployeeId
} from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { useQueryResult } from 'lib-common/query'
import { Gap } from 'lib-components/white-space'

import { financeDecisionHandlersQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import type { FinanceDecisionHandlerOption } from '../../state/invoicing-ui'
import { InvoicingUiContext } from '../../state/invoicing-ui'
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

export default React.memo(function VoucherValueDecisionFilters() {
  const {
    valueDecisions: {
      searchFilters,
      setSearchFilters,
      confirmSearchFilters,
      clearSearchFilters
    },
    shared: { availableAreas, allDaycareUnits: units }
  } = useContext(InvoicingUiContext)

  const { i18n } = useTranslation()

  const financeDecisionHandlers: Result<FinanceDecisionHandlerOption[]> =
    useQueryResult(financeDecisionHandlersQuery()).map((employees) =>
      employees.map((e) => ({
        value: e.id,
        label: formatPersonName(e, 'First Last')
      }))
    )

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

  const setStartDate = (startDate: LocalDate | null) => {
    setSearchFilters({
      ...searchFilters,
      startDate: startDate
    })
  }

  const setEndDate = (endDate: LocalDate | null) => {
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
