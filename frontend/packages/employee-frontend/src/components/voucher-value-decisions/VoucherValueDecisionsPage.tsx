// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Gap } from '@evaka/lib-components/src/white-space'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import VoucherValueDecisions from './VoucherValueDecisions'
import VoucherValueDecisionFilters from './VoucherValueDecisionFilters'
import VoucherValueDecisionActions from './VoucherValueDecisionActions'
import { Result } from '~api'
import {
  getVoucherValueDecisions,
  VoucherValueDecisionSearchParams,
  SortByVoucherValueDecisions,
  VoucherValueDecisionSearchResponse
} from '~api/invoicing'
import { InvoicingUiContext } from '~state/invoicing-ui'
import { VoucherValueDecisionSummary } from '~types/invoicing'
import { SearchOrder } from '~types'
import { useRestApi } from '~utils/useRestApi'

const pageSize = 200

export type PagedValueDecisions = {
  [k: number]: Result<VoucherValueDecisionSummary[]>
}

export default React.memo(function VoucherValueDecisionsPage() {
  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] = useState<SortByVoucherValueDecisions>(
    'HEAD_OF_FAMILY'
  )
  const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')
  const [totalDecisions, setTotalDecisions] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [decisions, setDecisions] = useState<PagedValueDecisions>({})
  const setDecisionsResult = useCallback(
    (result: Result<VoucherValueDecisionSearchResponse>) => {
      setDecisions((prev) => ({
        ...prev,
        [page]: result.map((r) => r.data)
      }))
      if (result.isSuccess) {
        setTotalDecisions(result.value.total)
        setTotalPages(result.value.pages)
      }
    },
    [page, setTotalDecisions, setTotalPages, setDecisions]
  )
  const reloadDecisions = useRestApi(
    getVoucherValueDecisions,
    setDecisionsResult
  )

  const {
    valueDecisions: {
      searchFilters,
      debouncedSearchTerms,
      checked,
      toggleChecked,
      checkIds,
      clearChecked
    }
  } = useContext(InvoicingUiContext)

  const loadDecisions = useCallback(() => {
    const status = searchFilters.status
    const area = searchFilters.area.join(',')
    const params: VoucherValueDecisionSearchParams = {
      status: status.length > 0 ? status : undefined,
      area: area.length > 0 ? area : undefined,
      unit: searchFilters.unit ? searchFilters.unit : undefined,
      searchTerms: debouncedSearchTerms ? debouncedSearchTerms : undefined,
      financeDecisionHandlerId: searchFilters.financeDecisionHandlerId
        ? searchFilters.financeDecisionHandlerId
        : undefined
    }
    reloadDecisions(page, pageSize, sortBy, sortDirection, params)
  }, [
    page,
    pageSize,
    sortBy,
    sortDirection,
    searchFilters,
    debouncedSearchTerms,
    reloadDecisions
  ])

  useEffect(() => {
    loadDecisions()
  }, [loadDecisions])

  useEffect(() => {
    setPage(1)
    setTotalDecisions(undefined)
    setTotalPages(undefined)
  }, [setPage, setTotalPages, setTotalDecisions, searchFilters, debouncedSearchTerms])

  const checkAll = useCallback(() => {
    const currentPage = decisions[page]
    if (currentPage?.isSuccess) {
      checkIds(currentPage.value.map((decision) => decision.id))
    }
  }, [decisions, checkIds])

  return (
    <Container data-qa="voucher-value-decisions-page">
      <ContentArea opaque>
        <VoucherValueDecisionFilters />
      </ContentArea>
      <Gap size={'XL'} />
      <ContentArea opaque>
        <VoucherValueDecisions
          decisions={decisions[page]}
          total={totalDecisions}
          pages={totalPages}
          currentPage={page}
          setPage={setPage}
          sortBy={sortBy}
          setSortBy={setSortBy}
          sortDirection={sortDirection}
          setSortDirection={setSortDirection}
          showCheckboxes={searchFilters.status === 'DRAFT'}
          checked={checked}
          toggleChecked={toggleChecked}
          checkAll={checkAll}
          clearChecked={clearChecked}
        />
      </ContentArea>
      <VoucherValueDecisionActions
        status={searchFilters.status}
        checkedIds={Object.keys(checked).filter((id) => !!checked[id])}
        clearChecked={clearChecked}
        loadDecisions={loadDecisions}
      />
    </Container>
  )
})
