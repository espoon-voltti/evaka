// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect } from 'react'
import { useMemo } from 'react'

import type { Result } from 'lib-common/api'
import type {
  DistinctiveParams,
  FeeDecisionDifference,
  FeeDecisionStatus
} from 'lib-common/generated/api-types/invoicing'
import type {
  DaycareId,
  EmployeeId
} from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { Gap } from 'lib-components/white-space'

import { financeDecisionHandlersQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import type { FinanceDecisionHandlerOption } from '../../state/invoicing-ui'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import {
  AreaFilter,
  FeeDecisionDifferenceFilter,
  FeeDecisionDateFilter,
  FeeDecisionDistinctionsFilter,
  FeeDecisionStatusFilter,
  Filters,
  FinanceDecisionHandlerFilter,
  UnitFilter
} from '../common/Filters'

function FeeDecisionFilters() {
  const {
    feeDecisions: {
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
        label: [e.firstName, e.lastName].join(' ')
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
      searchFilters.unit &&
      units.isSuccess &&
      !units.value.map(({ id }) => id).includes(searchFilters.unit)
    ) {
      setSearchFilters({ ...searchFilters, unit: undefined })
    }
  }, [units]) // eslint-disable-line react-hooks/exhaustive-deps

  const toggleArea = (code: string) => () => {
    if (searchFilters.area.includes(code)) {
      setSearchFilters({
        ...searchFilters,
        area: searchFilters.area.filter((v) => v !== code)
      })
    } else {
      setSearchFilters({
        ...searchFilters,
        area: [...searchFilters.area, code]
      })
    }
  }

  const selectUnit = (id?: DaycareId) =>
    setSearchFilters((filters) => ({ ...filters, unit: id }))

  const selectFinanceDecisionHandler = (id?: EmployeeId) =>
    setSearchFilters((filters) => ({
      ...filters,
      financeDecisionHandlerId: id
    }))

  const toggleDifference = (difference: FeeDecisionDifference) => {
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

  const toggleStatus = (status: FeeDecisionStatus) => () => {
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

  const toggleServiceNeed = (id: DistinctiveParams) => () => {
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
          <FeeDecisionDistinctionsFilter
            toggled={searchFilters.distinctiveDetails}
            toggle={toggleServiceNeed}
          />
          <Gap size="L" />
          <FeeDecisionDifferenceFilter
            toggled={searchFilters.difference}
            toggle={toggleDifference}
          />
        </Fragment>
      }
      column3={
        <Fragment>
          <FeeDecisionStatusFilter
            toggled={searchFilters.statuses}
            toggle={toggleStatus}
          />
          <Gap size="L" />
          <FeeDecisionDateFilter
            startDate={searchFilters.startDate}
            setStartDate={setStartDate}
            endDate={searchFilters.endDate}
            setEndDate={setEndDate}
            searchByStartDate={searchFilters.searchByStartDate}
            setSearchByStartDate={setSearchByStartDate}
          />
        </Fragment>
      }
    />
  )
}

export default FeeDecisionFilters
