// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect } from 'react'
import {
  AreaFilter,
  Filters,
  ValueDecisionStatusFilter,
  UnitFilter,
  FinanceDecisionHandlerFilter,
  ValueDecisionDateFilter
} from '../common/Filters'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { getAreas, getUnits } from '../../api/daycare'
import { VoucherValueDecisionStatus } from '../../types/invoicing'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { getFinanceDecisionHandlers } from '../../api/employees'
import { useMemo } from 'react'
import LocalDate from 'lib-common/local-date'

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
      availableAreas,
      setAvailableAreas
    }
  } = useContext(InvoicingUiContext)

  const { i18n } = useTranslation()
  useEffect(() => {
    void getAreas().then(setAvailableAreas)
  }, [setAvailableAreas])

  useEffect(() => {
    void getFinanceDecisionHandlers().then(setFinanceDecisionHandlers)
  }, [setFinanceDecisionHandlers])

  useEffect(() => {
    void getUnits([], 'DAYCARE').then(setUnits)
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

  const toggleStatus = useCallback(
    (status: VoucherValueDecisionStatus) => () => {
      setSearchFilters((filters) => ({
        ...filters,
        status
      }))
    },
    [setSearchFilters]
  )
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
      column2={<></>}
      column3={
        <>
          <ValueDecisionStatusFilter
            toggled={searchFilters.status}
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
