// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, Fragment } from 'react'
import LocalDate from 'lib-common/local-date'
import {
  AreaFilter,
  Filters,
  InvoiceStatusFilter,
  UnitFilter,
  InvoiceDistinctionsFilter,
  InvoiceDateFilter
} from '../common/Filters'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { getAreas, getUnits } from '../../api/daycare'
import { InvoiceStatus, InvoiceDistinctiveDetails } from '../../types/invoicing'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'

function InvoiceFilters() {
  const {
    invoices: {
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
    void getUnits([], 'DAYCARE').then(setUnits)
  }, [])

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

  const toggleStatus = (id: InvoiceStatus) => () => {
    setSearchFilters({
      ...searchFilters,
      status: id
    })
  }

  const toggleServiceNeed = (id: InvoiceDistinctiveDetails) => () => {
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

  const setUseCustomDatesForInvoiceSending = (value: boolean) => {
    setSearchFilters({
      ...searchFilters,
      useCustomDatesForInvoiceSending: value
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
        </>
      }
      column2={
        <Fragment>
          <InvoiceDistinctionsFilter
            toggled={searchFilters.distinctiveDetails}
            toggle={toggleServiceNeed}
          />
        </Fragment>
      }
      column3={
        <Fragment>
          <InvoiceStatusFilter
            toggled={searchFilters.status}
            toggle={toggleStatus}
          />
          <Gap size="L" />
          <InvoiceDateFilter
            startDate={searchFilters.startDate}
            setStartDate={setStartDate}
            endDate={searchFilters.endDate}
            setEndDate={setEndDate}
            searchByStartDate={searchFilters.useCustomDatesForInvoiceSending}
            setUseCustomDatesForInvoiceSending={
              setUseCustomDatesForInvoiceSending
            }
          />
        </Fragment>
      }
    />
  )
}

export default InvoiceFilters
