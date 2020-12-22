// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import {
  AreaFilter,
  Filters,
  FeeDecisionStatusFilter,
  FeeDecisionDistinctionsFilter,
  UnitFilter,
  FeeDecisionDateFilter,
  FinanceDecisionHandlerFilter
} from '../common/Filters'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { getAreas, getUnits } from '../../api/daycare'
import { getFinanceDecisionHandlers } from '../../api/employees'
import {
  DecisionDistinctiveDetails,
  FeeDecisionStatus
} from '../../types/invoicing'
import { Gap } from '@evaka/lib-components/src/white-space'
import { useTranslation } from '~state/i18n'
import { useMemo } from 'react'

function FeeDecisionFilters() {
  const {
    feeDecisions: {
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
  }, [])

  useEffect(() => {
    void getUnits([], 'DAYCARE').then(setUnits)
  }, [])

  useEffect(() => {
    void getFinanceDecisionHandlers().then(setFinanceDecisionHandlers)
  }, [])

  const selectedFinanceDecisionHandler = useMemo(
    () =>
      financeDecisionHandlers
        .getOrElse([])
        .find(
          (handler) => handler.id === searchFilters.financeDecisionHandlerId
        ),
    [searchFilters.financeDecisionHandlerId]
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
  }, [units])

  const toggleArea = (code: string) => () => {
    searchFilters.area.includes(code)
      ? setSearchFilters({
          ...searchFilters,
          area: searchFilters.area.filter((v) => v !== code)
        })
      : setSearchFilters({
          ...searchFilters,
          area: [...searchFilters.area, code]
        })
  }

  const selectUnit = (id: string) =>
    setSearchFilters((filters) => ({ ...filters, unit: id }))

  const selectFinanceDecisionHandler = (id: string) =>
    setSearchFilters((filters) => ({
      ...filters,
      financeDecisionHandlerId: id
    }))

  const toggleStatus = (id: FeeDecisionStatus) => () => {
    setSearchFilters({
      ...searchFilters,
      status: id
    })
  }

  const toggleServiceNeed = (id: DecisionDistinctiveDetails) => () => {
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
          <Gap size="s" />
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
          <Gap size="s" />
          <FeeDecisionDistinctionsFilter
            toggled={searchFilters.distinctiveDetails}
            toggle={toggleServiceNeed}
          />
        </Fragment>
      }
      column3={
        <Fragment>
          <Gap size="s" />
          <FeeDecisionStatusFilter
            toggled={searchFilters.status}
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
