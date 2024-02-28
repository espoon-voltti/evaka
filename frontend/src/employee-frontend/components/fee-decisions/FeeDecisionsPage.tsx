// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'

import { Result, wrapResult } from 'lib-common/api'
import {
  FeeDecisionSortParam,
  FeeDecisionSummary,
  PagedFeeDecisionSummaries,
  SearchFeeDecisionRequest,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import {
  confirmFeeDecisionDrafts,
  searchFeeDecisions
} from '../../generated/api-clients/invoicing'
import { useCheckedState } from '../../state/invoicing'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import FinanceDecisionHandlerSelectModal from '../finance-decisions/FinanceDecisionHandlerSelectModal'

import Actions from './Actions'
import FeeDecisionFilters from './FeeDecisionFilters'
import FeeDecisions from './FeeDecisions'

const confirmFeeDecisionDraftsResult = wrapResult(confirmFeeDecisionDrafts)
const searchFeeDecisionsResult = wrapResult(searchFeeDecisions)

const pageSize = 200

export type PagedFeeDecisions = Record<number, Result<FeeDecisionSummary[]>>

export default React.memo(function FeeDecisionsPage() {
  const [showHandlerSelectModal, setShowHandlerSelectModal] = useState(false)
  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] = useState<FeeDecisionSortParam>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')
  const [totalDecisions, setTotalDecisions] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [decisions, setDecisions] = useState<PagedFeeDecisions>({})
  const setDecisionsResult = useCallback(
    (result: Result<PagedFeeDecisionSummaries>) => {
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
    searchFeeDecisionsResult,
    setDecisionsResult
  )

  const {
    feeDecisions: { searchFilters, debouncedSearchTerms }
  } = useContext(InvoicingUiContext)

  const checkedState = useCheckedState()

  const loadDecisions = useCallback(() => {
    const { startDate, endDate } = searchFilters
    if (startDate && endDate && startDate.isAfter(endDate)) {
      return
    }

    const params: SearchFeeDecisionRequest = {
      page: page - 1,
      pageSize,
      sortBy,
      sortDirection,
      statuses: searchFilters.statuses,
      area: searchFilters.area,
      unit: searchFilters.unit ?? null,
      distinctions: searchFilters.distinctiveDetails,
      searchTerms: debouncedSearchTerms ? debouncedSearchTerms : null,
      startDate: startDate ?? null,
      endDate: endDate ?? null,
      searchByStartDate: searchFilters.searchByStartDate,
      financeDecisionHandlerId: searchFilters.financeDecisionHandlerId ?? null,
      difference: searchFilters.difference
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
    if (currentPage.isSuccess) {
      checkedState.checkIds(currentPage.value.map((decision) => decision.id))
    }
  }, [decisions, checkedState.checkIds]) // eslint-disable-line react-hooks/exhaustive-deps

  const checkedIds = Object.keys(checkedState.checked).filter(
    (id) => !!checkedState.checked[id]
  )

  return (
    <Container data-qa="fee-decisions-page">
      {showHandlerSelectModal && (
        <FinanceDecisionHandlerSelectModal
          onResolve={async (decisionHandlerId) => {
            const result = await confirmFeeDecisionDraftsResult({
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
        <FeeDecisionFilters />
      </ContentArea>
      <Gap size="XL" />
      <ContentArea opaque>
        <FeeDecisions
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
      <Actions
        statuses={searchFilters.statuses}
        checkedIds={checkedIds}
        clearChecked={checkedState.clearChecked}
        loadDecisions={loadDecisions}
        onHandlerSelectModal={() => setShowHandlerSelectModal(true)}
      />
    </Container>
  )
})
