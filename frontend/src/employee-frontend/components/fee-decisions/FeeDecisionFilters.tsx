// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect } from 'react'
import { useMemo } from 'react'

import { wrapResult } from 'lib-common/api'
import {
  DistinctiveParams,
  FeeDecisionDifference,
  FeeDecisionStatus
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { Gap } from 'lib-components/white-space'

import { getUnits } from '../../generated/api-clients/daycare'
import { getFinanceDecisionHandlers } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
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

const getUnitsResult = wrapResult(getUnits)
const getFinanceDecisionHandlersResult = wrapResult(getFinanceDecisionHandlers)

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
      availableAreas
    }
  } = useContext(InvoicingUiContext)

  const { i18n } = useTranslation()

  useEffect(() => {
    void getUnitsResult({ areaIds: null, type: 'DAYCARE', from: null }).then(
      setUnits
    )
  }, [setUnits])

  useEffect(() => {
    void getFinanceDecisionHandlersResult().then(setFinanceDecisionHandlers)
  }, [setFinanceDecisionHandlers])

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

  const selectUnit = (id?: string) =>
    setSearchFilters((filters) => ({ ...filters, unit: id }))

  const selectFinanceDecisionHandler = (id?: string) =>
    setSearchFilters((filters) => ({
      ...filters,
      financeDecisionHandlerId: id
    }))

  const toggleDifference = (difference: FeeDecisionDifference) => {
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

  const toggleStatus = (status: FeeDecisionStatus) => () => {
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

  const toggleServiceNeed = (id: DistinctiveParams) => () => {
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
