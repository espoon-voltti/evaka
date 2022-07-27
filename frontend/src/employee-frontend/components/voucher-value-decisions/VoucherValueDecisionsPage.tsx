// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'

import type { Paged, Result } from 'lib-common/api'
import type {
  VoucherValueDecisionSortParam,
  VoucherValueDecisionSummary
} from 'lib-common/generated/api-types/invoicing'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import type { VoucherValueDecisionSearchParams } from '../../api/invoicing'
import { getVoucherValueDecisions } from '../../api/invoicing'
import { useCheckedState } from '../../state/invoicing'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import type { SearchOrder } from '../../types'

import VoucherValueDecisionActions from './VoucherValueDecisionActions'
import VoucherValueDecisionFilters from './VoucherValueDecisionFilters'
import VoucherValueDecisions from './VoucherValueDecisions'

const pageSize = 200

export type PagedValueDecisions = {
  [k: number]: Result<VoucherValueDecisionSummary[]>
}

export default React.memo(function VoucherValueDecisionsPage() {
  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] =
    useState<VoucherValueDecisionSortParam>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')
  const [totalDecisions, setTotalDecisions] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [decisions, setDecisions] = useState<PagedValueDecisions>({})
  const setDecisionsResult = useCallback(
    (result: Result<Paged<VoucherValueDecisionSummary>>) => {
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
    valueDecisions: { searchFilters, debouncedSearchTerms }
  } = useContext(InvoicingUiContext)

  const checkedState = useCheckedState()

  const loadDecisions = useCallback(() => {
    const status = searchFilters.status
    const params: VoucherValueDecisionSearchParams = {
      status: status.length > 0 ? status : undefined,
      area: searchFilters.area,
      unit: searchFilters.unit,
      searchTerms: debouncedSearchTerms ? debouncedSearchTerms : undefined,
      financeDecisionHandlerId: searchFilters.financeDecisionHandlerId
        ? searchFilters.financeDecisionHandlerId
        : undefined,
      startDate: searchFilters.startDate,
      endDate: searchFilters.endDate,
      searchByStartDate: searchFilters.searchByStartDate
    }
    void reloadDecisions(page, pageSize, sortBy, sortDirection, params)
  }, [
    page,
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
      checkedState.checkIds(currentPage.value.map((decision) => decision.id))
    }
  }, [decisions, checkedState.checkIds]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <Container data-qa="voucher-value-decisions-page">
      <ContentArea opaque>
        <VoucherValueDecisionFilters />
      </ContentArea>
      <Gap size="XL" />
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
          checked={checkedState.checked}
          toggleChecked={checkedState.toggleChecked}
          checkAll={checkAll}
          clearChecked={checkedState.clearChecked}
        />
      </ContentArea>
      <VoucherValueDecisionActions
        status={searchFilters.status}
        checkedIds={Object.keys(checkedState.checked).filter(
          (id) => !!checkedState.checked[id]
        )}
        clearChecked={checkedState.clearChecked}
        loadDecisions={loadDecisions}
      />
    </Container>
  )
})
