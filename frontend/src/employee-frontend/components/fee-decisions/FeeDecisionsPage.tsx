// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'

import type {
  FeeDecisionSortParam,
  SearchFeeDecisionRequest,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import type { FeeDecisionId } from 'lib-common/generated/api-types/shared'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import { useCheckedState } from '../../state/invoicing'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { renderResult } from '../async-rendering'
import FinanceDecisionHandlerSelectModal from '../finance-decisions/FinanceDecisionHandlerSelectModal'

import Actions from './Actions'
import FeeDecisionFilters from './FeeDecisionFilters'
import FeeDecisions from './FeeDecisions'
import {
  confirmFeeDecisionDraftsMutation,
  searchFeeDecisionsQuery
} from './fee-decision-queries'

export default React.memo(function FeeDecisionsPage() {
  const [showHandlerSelectModal, setShowHandlerSelectModal] = useState(false)
  const [sortBy, setSortBy] = useState<FeeDecisionSortParam>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const {
    feeDecisions: { confirmedSearchFilters: searchFilters, page }
  } = useContext(InvoicingUiContext)

  const searchParams: SearchFeeDecisionRequest | undefined = useMemo(() => {
    if (searchFilters === undefined) return undefined

    const { startDate, endDate } = searchFilters
    if (startDate && endDate && startDate.isAfter(endDate)) {
      return undefined
    }

    return {
      page: page - 1,
      sortBy,
      sortDirection,
      statuses: searchFilters.statuses,
      area: searchFilters.area,
      unit: searchFilters.unit ?? null,
      distinctions: searchFilters.distinctiveDetails,
      searchTerms: searchFilters.searchTerms ? searchFilters.searchTerms : null,
      startDate: startDate ?? null,
      endDate: endDate ?? null,
      searchByStartDate: searchFilters.searchByStartDate,
      financeDecisionHandlerId: searchFilters.financeDecisionHandlerId ?? null,
      difference: searchFilters.difference
    }
  }, [page, sortBy, sortDirection, searchFilters])

  const searchResult = useQueryResult(
    searchParams
      ? searchFeeDecisionsQuery({ body: searchParams })
      : constantQuery({ data: [], pages: 0, total: 0 })
  )

  const checkedState = useCheckedState<FeeDecisionId>()

  const checkAll = useCallback(() => {
    if (searchResult && searchResult.isSuccess) {
      checkedState.checkIds(
        searchResult.value.data.map((decision) => decision.id)
      )
    }
  }, [searchResult, checkedState.checkIds]) // eslint-disable-line react-hooks/exhaustive-deps

  const checkedIds = checkedState.getCheckedIds()

  const { mutateAsync: confirmFeeDecisionDrafts } = useMutationResult(
    confirmFeeDecisionDraftsMutation
  )

  return (
    <Container data-qa="fee-decisions-page">
      {showHandlerSelectModal && (
        <FinanceDecisionHandlerSelectModal
          onResolve={async (decisionHandlerId) => {
            const result = await confirmFeeDecisionDrafts({
              decisionHandlerId,
              body: checkedIds
            })
            if (result.isSuccess) {
              checkedState.clearChecked()
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
      {searchFilters &&
        searchParams &&
        renderResult(searchResult, (result) => (
          <>
            <ContentArea opaque>
              <FeeDecisions
                decisions={result.data}
                total={result.total}
                pages={result.pages}
                sortBy={sortBy}
                setSortBy={setSortBy}
                sortDirection={sortDirection}
                setSortDirection={setSortDirection}
                showCheckboxes={
                  searchFilters.statuses.length === 1 &&
                  ['DRAFT', 'IGNORED'].includes(searchFilters.statuses[0])
                }
                isChecked={checkedState.isChecked}
                toggleChecked={checkedState.toggleChecked}
                checkAll={checkAll}
                clearChecked={checkedState.clearChecked}
              />
            </ContentArea>
            <Actions
              statuses={searchFilters.statuses}
              checkedIds={checkedIds}
              clearChecked={checkedState.clearChecked}
              onHandlerSelectModal={() => setShowHandlerSelectModal(true)}
            />
          </>
        ))}
    </Container>
  )
})
