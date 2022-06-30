// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'

import { Paged, Result } from 'lib-common/api'
import {
  FeeDecisionSortParam,
  FeeDecisionSummary,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import { getFeeDecisions, FeeDecisionSearchParams } from '../../api/invoicing'
import { useCheckedState } from '../../state/invoicing'
import { InvoicingUiContext } from '../../state/invoicing-ui'

import Actions from './Actions'
import FeeDecisionFilters from './FeeDecisionFilters'
import FeeDecisions from './FeeDecisions'

const pageSize = 200

export type PagedFeeDecisions = {
  [k: number]: Result<FeeDecisionSummary[]>
}

export default React.memo(function FeeDecisionsPage() {
  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] = useState<FeeDecisionSortParam>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')
  const [totalDecisions, setTotalDecisions] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [decisions, setDecisions] = useState<PagedFeeDecisions>({})
  const setDecisionsResult = useCallback(
    (result: Result<Paged<FeeDecisionSummary>>) => {
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
  const reloadDecisions = useRestApi(getFeeDecisions, setDecisionsResult)

  const {
    feeDecisions: { searchFilters, debouncedSearchTerms }
  } = useContext(InvoicingUiContext)

  const checkedState = useCheckedState()

  const loadDecisions = useCallback(() => {
    const status = searchFilters.status
    const params: FeeDecisionSearchParams = {
      status: status.length > 0 ? [status] : undefined,
      area: searchFilters.area,
      unit: searchFilters.unit,
      distinctions: searchFilters.distinctiveDetails,
      searchTerms: debouncedSearchTerms ? debouncedSearchTerms : undefined,
      startDate: searchFilters.startDate,
      endDate: searchFilters.endDate,
      searchByStartDate: searchFilters.searchByStartDate,
      financeDecisionHandlerId: searchFilters.financeDecisionHandlerId
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
    if (currentPage.isSuccess) {
      checkedState.checkIds(currentPage.value.map((decision) => decision.id))
    }
  }, [decisions, checkedState.checkIds]) // eslint-disable-line react-hooks/exhaustive-deps

  const checkedIds = Object.keys(checkedState.checked).filter(
    (id) => !!checkedState.checked[id]
  )

  return (
    <Container data-qa="fee-decisions-page">
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
          showCheckboxes={searchFilters.status === 'DRAFT'}
          checked={checkedState.checked}
          toggleChecked={checkedState.toggleChecked}
          checkAll={checkAll}
          clearChecked={checkedState.clearChecked}
        />
      </ContentArea>
      <Actions
        status={searchFilters.status}
        checkedIds={checkedIds}
        clearChecked={checkedState.clearChecked}
        loadDecisions={loadDecisions}
      />
    </Container>
  )
})
