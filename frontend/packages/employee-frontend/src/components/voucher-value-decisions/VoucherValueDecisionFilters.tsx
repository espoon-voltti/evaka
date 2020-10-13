// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect } from 'react'
import {
  AreaFilter,
  Filters,
  ValueDecisionStatusFilter,
  UnitFilter
} from '../common/Filters'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { isSuccess } from '../../api'
import { getAreas, getUnits } from '../../api/daycare'
import { VoucherValueDecisionStatus } from '../../types/invoicing'
import { Gap } from '~components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'

export default React.memo(function VoucherValueDecisionFilters() {
  const {
    valueDecisions: {
      searchFilters,
      setSearchFilters,
      searchTerms,
      setSearchTerms,
      clearSearchFilters
    },
    shared: { units, setUnits, availableAreas, setAvailableAreas }
  } = useContext(InvoicingUiContext)

  const { i18n } = useTranslation()

  useEffect(() => {
    void getAreas().then(setAvailableAreas)
  }, [])

  useEffect(() => {
    void getUnits([]).then(setUnits)
  }, [])

  // remove selected unit filter if the unit is not included in the selected areas
  useEffect(() => {
    if (
      searchFilters.unit &&
      isSuccess(units) &&
      !units.data.map(({ id }) => id).includes(searchFilters.unit)
    ) {
      setSearchFilters({ ...searchFilters, unit: undefined })
    }
  }, [units])

  const toggleArea = useCallback(
    (code: string) => () => {
      setSearchFilters(({ area, ...filters }) => ({
        ...filters,
        area: area.includes(code)
          ? searchFilters.area.filter((v) => v !== code)
          : [...searchFilters.area, code]
      }))
    },
    [setSearchFilters]
  )

  const selectUnit = useCallback(
    (unit: string) => setSearchFilters((filters) => ({ ...filters, unit })),
    [setSearchFilters]
  )

  const toggleStatus = useCallback(
    (status: VoucherValueDecisionStatus) => () => {
      setSearchFilters({
        ...searchFilters,
        status
      })
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
            areas={isSuccess(availableAreas) ? availableAreas.data : []}
            toggled={searchFilters.area}
            toggle={toggleArea}
          />
          <Gap size="L" />
          <UnitFilter
            units={
              isSuccess(units)
                ? units.data.map(({ id, name }) => ({ id, label: name }))
                : []
            }
            selected={
              isSuccess(units)
                ? units.data
                    .map(({ id, name }) => ({ id, label: name }))
                    .filter((unit) => unit.id === searchFilters.unit)[0]
                : undefined
            }
            select={selectUnit}
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
