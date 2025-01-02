// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'

import {
  SearchVoucherValueDecisionRequest,
  VoucherValueDecisionSortParam
} from 'lib-common/generated/api-types/invoicing'
import { VoucherValueDecisionId } from 'lib-common/generated/api-types/shared'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import { useCheckedState } from '../../state/invoicing'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { SearchOrder } from '../../types'
import { renderResult } from '../async-rendering'
import FinanceDecisionHandlerSelectModal from '../finance-decisions/FinanceDecisionHandlerSelectModal'

import VoucherValueDecisionActions from './VoucherValueDecisionActions'
import VoucherValueDecisionFilters from './VoucherValueDecisionFilters'
import VoucherValueDecisions from './VoucherValueDecisions'
import {
  searchVoucherValueDecisionsQuery,
  sendVoucherValueDecisionDraftsMutation
} from './voucher-value-decision-queries'

export default React.memo(function VoucherValueDecisionsPage() {
  const [showHandlerSelectModal, setShowHandlerSelectModal] = useState(false)
  const [sortBy, setSortBy] =
    useState<VoucherValueDecisionSortParam>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')

  const {
    valueDecisions: { confirmedSearchFilters: searchFilters, page }
  } = useContext(InvoicingUiContext)

  const searchParams: SearchVoucherValueDecisionRequest | undefined =
    useMemo(() => {
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
        searchTerms: searchFilters.searchTerms
          ? searchFilters.searchTerms
          : null,
        financeDecisionHandlerId: searchFilters.financeDecisionHandlerId
          ? searchFilters.financeDecisionHandlerId
          : null,
        difference: searchFilters.difference,
        startDate: startDate ?? null,
        endDate: endDate ?? null,
        searchByStartDate: searchFilters.searchByStartDate
      }
    }, [page, sortBy, sortDirection, searchFilters])

  const searchResult = useQueryResult(
    searchParams
      ? searchVoucherValueDecisionsQuery({ body: searchParams })
      : constantQuery({ data: [], pages: 0, total: 0 })
  )

  const checkedState = useCheckedState<VoucherValueDecisionId>()

  const checkAll = useCallback(() => {
    if (searchResult && searchResult.isSuccess) {
      checkedState.checkIds(
        searchResult.value.data.map((decision) => decision.id)
      )
    }
  }, [searchResult, checkedState.checkIds]) // eslint-disable-line react-hooks/exhaustive-deps

  const checkedIds = checkedState.getCheckedIds()

  const { mutateAsync: sendVoucherValueDecisionDrafts } = useMutationResult(
    sendVoucherValueDecisionDraftsMutation
  )

  return (
    <Container data-qa="voucher-value-decisions-page">
      {showHandlerSelectModal && (
        <FinanceDecisionHandlerSelectModal
          onResolve={async (decisionHandlerId) => {
            const result = await sendVoucherValueDecisionDrafts({
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
        <VoucherValueDecisionFilters />
      </ContentArea>
      <Gap size="XL" />
      {searchFilters &&
        searchParams &&
        renderResult(searchResult, (result) => (
          <>
            <ContentArea opaque>
              <VoucherValueDecisions
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
            <VoucherValueDecisionActions
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
