// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useContext, useEffect } from 'react'

import { getAreas } from '../../api/daycare'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { AreaFilter, Filters } from '../common/Filters'

export default React.memo(function IncomeStatementsFilters() {
  const {
    invoiceStatements: { searchFilters, setSearchFilters, clearSearchFilters },
    shared: { availableAreas, setAvailableAreas }
  } = useContext(InvoicingUiContext)

  useEffect(() => {
    void getAreas().then(setAvailableAreas)
  }, [setAvailableAreas])

  const toggleArea = useCallback(
    (code: string) => () => {
      setSearchFilters((old) =>
        old.area.includes(code)
          ? {
              ...old,
              area: old.area.filter((v) => v !== code)
            }
          : {
              ...old,
              area: [...old.area, code]
            }
      )
    },
    [setSearchFilters]
  )

  /*
  const setSentStartDate = useCallback(
    (sentStartDate: LocalDate | undefined) =>
      setSearchFilters((old) => ({ ...old, startDate: sentStartDate })),
    [setSearchFilters]
  )

  const setSentEndDate = useCallback(
    (sentEndDate: LocalDate | undefined) =>
      setSearchFilters((old) => ({ ...old, endDate: sentEndDate })),
    [setSearchFilters]
  )
*/
  return (
    <Filters
      clearFilters={clearSearchFilters}
      column1={
        <AreaFilter
          areas={availableAreas.getOrElse([])}
          toggled={searchFilters.area}
          toggle={toggleArea}
        />
      }
      column2={
        <Fragment>
        </Fragment>
      }
      column3={
        <Fragment>
        </Fragment>
      }
    />
  )
})
