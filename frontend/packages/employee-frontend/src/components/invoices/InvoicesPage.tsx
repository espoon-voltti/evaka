// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Gap } from 'components/shared/layout/white-space'
import { Container, ContentArea } from 'components/shared/layout/Container'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import Invoices from './Invoices'
import InvoiceFilters from './InvoiceFilters'
import Actions from './Actions'
import { isSuccess, Result, Success } from '../../api'
import { SearchOrder } from '~types'
import { useRestApi } from '~utils/useRestApi'
import {
  createInvoices,
  getInvoices,
  InvoiceSearchParams,
  SortByInvoices
} from '~api/invoicing'
import { InvoiceSearchResult, InvoiceSummary } from '~types/invoicing'

const pageSize = 200

export type PagedInvoices = {
  [k: number]: Result<InvoiceSummary[]>
}

const InvoicesPage = React.memo(function InvoicesPage() {
  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] = useState<SortByInvoices>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')
  const [totalInvoices, setTotalInvoices] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [invoices, setInvoices] = useState<PagedInvoices>({})
  const setInvoicesResult = useCallback(
    (result: Result<InvoiceSearchResult>) => {
      if (isSuccess(result)) {
        setTotalInvoices(result.data.total)
        setTotalPages(result.data.pages)
        setInvoices((prev) => ({
          ...prev,
          [page]: Success(result.data.data)
        }))
      } else {
        setInvoices((prev) => ({ ...prev, [page]: result }))
      }
    },
    [page, setTotalInvoices, setTotalPages, setInvoices]
  )
  const reloadInvoices = useRestApi(getInvoices, setInvoicesResult)
  const {
    invoices: {
      searchFilters,
      debouncedSearchTerms,
      checked,
      toggleChecked,
      checkIds,
      clearChecked
    }
  } = useContext(InvoicingUiContext)

  const loadInvoices = useCallback(() => {
    const area = searchFilters.area.join(',')
    const status = searchFilters.status
    const distinctiveDetails = searchFilters.distinctiveDetails.join(',')
    const params: InvoiceSearchParams = {
      area: area.length > 0 ? area : undefined,
      unit: searchFilters.unit ? searchFilters.unit : undefined,
      status: status.length > 0 ? status : undefined,
      distinctions: distinctiveDetails ? distinctiveDetails : undefined,
      searchTerms: debouncedSearchTerms ? debouncedSearchTerms : undefined,
      periodStart: searchFilters.startDate?.formatIso(),
      periodEnd: searchFilters.endDate?.formatIso()
    }
    reloadInvoices(page, pageSize, sortBy, sortDirection, params)
  }, [
    page,
    pageSize,
    sortBy,
    sortDirection,
    searchFilters,
    debouncedSearchTerms,
    reloadInvoices
  ])

  useEffect(() => {
    loadInvoices()
  }, [loadInvoices])

  const checkAll = useCallback(() => {
    const currentPage = invoices[page]
    if (isSuccess(currentPage)) {
      checkIds(currentPage.data.map((invoice) => invoice.id))
    }
  }, [invoices, checkIds])

  const checkedIds = Object.keys(checked).filter((id) => !!checked[id])

  return (
    <div className="invoices-page" data-qa="invoices-page">
      <Container>
        <Gap size={'L'} />
        <ContentArea opaque>
          <InvoiceFilters />
        </ContentArea>
        <Gap size={'XL'} />
        <ContentArea opaque>
          <Invoices
            invoices={invoices[page]}
            createInvoices={() => createInvoices().then(loadInvoices)}
            total={totalInvoices}
            pages={totalPages}
            currentPage={page}
            setPage={setPage}
            sortBy={sortBy}
            setSortBy={setSortBy}
            sortDirection={sortDirection}
            setSortDirection={setSortDirection}
            showCheckboxes={
              searchFilters.status === 'DRAFT' ||
              searchFilters.status === 'WAITING_FOR_SENDING'
            }
            checked={checked}
            toggleChecked={toggleChecked}
            checkAll={checkAll}
            clearChecked={clearChecked}
          />
          <Actions
            status={searchFilters.status}
            checkedIds={checkedIds}
            checkedAreas={searchFilters.area}
            clearChecked={clearChecked}
            loadInvoices={loadInvoices}
            periodStart={searchFilters.startDate}
            periodEnd={searchFilters.endDate}
            useCustomDatesForInvoiceSending={
              searchFilters.useCustomDatesForInvoiceSending
            }
          />
        </ContentArea>
      </Container>
    </div>
  )
})

export default InvoicesPage
