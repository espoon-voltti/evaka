// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect } from 'react'
import {
  AreaFilter,
  Filters,
  ValueDecisionStatusFilter,
  UnitFilter,
  FinanceDecisionHandlerFilter
} from '../common/Filters'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { getAreas, getUnits } from '../../api/daycare'
import { VoucherValueDecisionStatus } from '../../types/invoicing'
import { Gap } from '@evaka/lib-components/src/white-space'
import { useTranslation } from '~state/i18n'
import { getFinanceDecisionHandlers } from '~api/employees'

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
  }, [])

  useEffect(() => {
    void getFinanceDecisionHandlers().then(setFinanceDecisionHandlers)
  }, [])

  useEffect(() => {
    void getUnits([], 'DAYCARE').then(setUnits)
  }, [])

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
  }, [units])

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
    (unit: string) => setSearchFilters((filters) => ({ ...filters, unit })),
    [setSearchFilters]
  )

  const selectFinanceDecisionHandler = useCallback(
    (financeDecisionHandlerId: string) =>
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
            financeDecisionHandlers={financeDecisionHandlers
              .map((e) =>
                e.map(({ id, firstName, lastName }) => ({
                  id,
                  label: [firstName, lastName].join(' ')
                }))
              )
              .getOrElse([])}
            selected={financeDecisionHandlers
              .map(
                (es) =>
                  es
                    .map(({ id, firstName, lastName }) => ({
                      id,
                      label: [firstName, lastName].join(' ')
                    }))
                    .filter(
                      (financeDecisionHandler) =>
                        financeDecisionHandler.id ===
                        searchFilters.financeDecisionHandlerId
                    )[0]
              )
              .getOrElse(undefined)}
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
        </>
      }
    />
  )
})
