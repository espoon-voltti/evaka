// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'

import { Result, wrapResult } from 'lib-common/api'
import {
  PagedVoucherValueDecisionSummaries,
  SearchVoucherValueDecisionRequest,
  VoucherValueDecisionSortParam,
  VoucherValueDecisionSummary
} from 'lib-common/generated/api-types/invoicing'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import {
  searchVoucherValueDecisions,
  sendVoucherValueDecisionDrafts
} from '../../generated/api-clients/invoicing'
import { useCheckedState } from '../../state/invoicing'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { SearchOrder } from '../../types'
import FinanceDecisionHandlerSelectModal from '../finance-decisions/FinanceDecisionHandlerSelectModal'

import VoucherValueDecisionActions from './VoucherValueDecisionActions'
import VoucherValueDecisionFilters from './VoucherValueDecisionFilters'
import VoucherValueDecisions from './VoucherValueDecisions'

const searchVoucherValueDecisionsResult = wrapResult(
  searchVoucherValueDecisions
)
const sendVoucherValueDecisionDraftsResult = wrapResult(
  sendVoucherValueDecisionDrafts
)

export type PagedValueDecisions = Record<
  number,
  Result<VoucherValueDecisionSummary[]>
>

export default React.memo(function VoucherValueDecisionsPage() {
  const [showHandlerSelectModal, setShowHandlerSelectModal] = useState(false)
  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] =
    useState<VoucherValueDecisionSortParam>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')
  const [totalDecisions, setTotalDecisions] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [decisions, setDecisions] = useState<PagedValueDecisions>({})
  const setDecisionsResult = useCallback(
    (result: Result<PagedVoucherValueDecisionSummaries>) => {
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
    searchVoucherValueDecisionsResult,
    setDecisionsResult
  )

  const {
    valueDecisions: { searchFilters, debouncedSearchTerms }
  } = useContext(InvoicingUiContext)

  const checkedState = useCheckedState()

  const loadDecisions = useCallback(() => {
    const { startDate, endDate } = searchFilters
    if (startDate && endDate && startDate.isAfter(endDate)) {
      return
    }

    const params: SearchVoucherValueDecisionRequest = {
      page: page - 1,
      sortBy,
      sortDirection,
      statuses: searchFilters.statuses,
      area: searchFilters.area,
      unit: searchFilters.unit ?? null,
      distinctions: searchFilters.distinctiveDetails,
      searchTerms: debouncedSearchTerms ? debouncedSearchTerms : null,
      financeDecisionHandlerId: searchFilters.financeDecisionHandlerId
        ? searchFilters.financeDecisionHandlerId
        : null,
      difference: searchFilters.difference,
      startDate: startDate ?? null,
      endDate: endDate ?? null,
      searchByStartDate: searchFilters.searchByStartDate
    }
    void reloadDecisions({ body: params })
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
  }, [
    setPage,
    setTotalPages,
    setTotalDecisions,
    searchFilters,
    debouncedSearchTerms
  ])

  const checkAll = useCallback(() => {
    const currentPage = decisions[page]
    if (currentPage?.isSuccess) {
      checkedState.checkIds(currentPage.value.map((decision) => decision.id))
    }
  }, [decisions, checkedState.checkIds]) // eslint-disable-line react-hooks/exhaustive-deps

  const checkedIds = Object.keys(checkedState.checked).filter(
    (id) => !!checkedState.checked[id]
  )

  return (
    <Container data-qa="voucher-value-decisions-page">
      {showHandlerSelectModal && (
        <FinanceDecisionHandlerSelectModal
          onResolve={async (decisionHandlerId) => {
            const result = await sendVoucherValueDecisionDraftsResult({
              decisionHandlerId,
              body: checkedIds
            })
            if (result.isSuccess) {
              checkedState.clearChecked()
              loadDecisions()
              setShowHandlerSelectModal(false)
            }
            return result
          }}
          onReject={() => setShowHandlerSelectModal(false)}
          checkedIds={checkedIds}
        />
      )}
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
          showCheckboxes={
            searchFilters.statuses.length === 1 &&
            ['DRAFT', 'IGNORED'].includes(searchFilters.statuses[0])
          }
          checked={checkedState.checked}
          toggleChecked={checkedState.toggleChecked}
          checkAll={checkAll}
          clearChecked={checkedState.clearChecked}
        />
      </ContentArea>
      <VoucherValueDecisionActions
        statuses={searchFilters.statuses}
        checkedIds={checkedIds}
        clearChecked={checkedState.clearChecked}
        loadDecisions={loadDecisions}
        onHandlerSelectModal={() => setShowHandlerSelectModal(true)}
      />
    </Container>
  )
})
