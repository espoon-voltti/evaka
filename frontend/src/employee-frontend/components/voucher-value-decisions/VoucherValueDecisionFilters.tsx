// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useContext, useEffect } from 'react'
import { useMemo } from 'react'

import { wrapResult } from 'lib-common/api'
import {
  VoucherValueDecisionDifference,
  VoucherValueDecisionDistinctiveParams,
  VoucherValueDecisionStatus
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { Gap } from 'lib-components/white-space'

import { getUnits } from '../../generated/api-clients/daycare'
import { getFinanceDecisionHandlers } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
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

const getUnitsResult = wrapResult(getUnits)
const getFinanceDecisionHandlersResult = wrapResult(getFinanceDecisionHandlers)

export default React.memo(function VoucherValueDecisionFilters() {
  const {
    valueDecisions: {
      searchFilters,
      setSearchFilters,
      searchTerms,
      setSearchTerms,
      clearSearchFilters
    },
    shared: {
      units,
      setUnits,
      financeDecisionHandlers,
      setFinanceDecisionHandlers,
      availableAreas
    }
  } = useContext(InvoicingUiContext)

  const { i18n } = useTranslation()

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
    (unit?: string) => setSearchFilters((filters) => ({ ...filters, unit })),
    [setSearchFilters]
  )

  const selectFinanceDecisionHandler = useCallback(
    (financeDecisionHandlerId?: string) =>
      setSearchFilters((filters) => ({ ...filters, financeDecisionHandlerId })),
    [setSearchFilters]
  )

  const toggleDifference = (difference: VoucherValueDecisionDifference) => {
    searchFilters.difference.includes(difference)
      ? setSearchFilters({
          ...searchFilters,
          difference: searchFilters.difference.filter((v) => v !== difference)
        })
      : setSearchFilters({
          ...searchFilters,
          difference: [...searchFilters.difference, difference]
        })
  }

  const toggleStatus = (status: VoucherValueDecisionStatus) => () => {
    searchFilters.statuses.includes(status)
      ? setSearchFilters({
          ...searchFilters,
          statuses: searchFilters.statuses.filter((v) => v !== status)
        })
      : setSearchFilters({
          ...searchFilters,
          statuses: [...searchFilters.statuses, status]
        })
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
      searchFilters.distinctiveDetails.includes(id)
        ? setSearchFilters({
            ...searchFilters,
            distinctiveDetails: searchFilters.distinctiveDetails.filter(
              (v) => v !== id
            )
          })
        : setSearchFilters({
            ...searchFilters,
            distinctiveDetails: [...searchFilters.distinctiveDetails, id]
          })
    }

  return (
    <Filters
      searchPlaceholder={i18n.filters.freeTextPlaceholder}
      freeText={searchTerms}
      setFreeText={setSearchTerms}
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
            units={units
              .map((us) => us.map(({ id, name }) => ({ id, label: name })))
              .getOrElse([])}
            selected={units
              .map(
                (us) =>
                  us
                    .map(({ id, name }) => ({ id, label: name }))
                    .filter((unit) => unit.id === searchFilters.unit)[0]
              )
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
