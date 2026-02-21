// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import { Redirect } from 'wouter'

import type { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'
import type { VoucherValueDecisionId } from 'lib-common/generated/api-types/shared'
import { formatPersonName } from 'lib-common/names'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { useTitle } from '../../utils/useTitle'
import MetadataSection from '../archive-metadata/MetadataSection'
import { renderResult } from '../async-rendering'
import FinanceDecisionHandlerSelectModal from '../finance-decisions/FinanceDecisionHandlerSelectModal'
import { voucherValueDecisionMetadataQuery } from '../voucher-value-decisions/voucher-value-decision-queries'

import VoucherValueDecisionActionBar from './VoucherValueDecisionActionBar'
import VoucherValueDecisionChildSection from './VoucherValueDecisionChildSection'
import VoucherValueDecisionHeading from './VoucherValueDecisionHeading'
import VoucherValueDecisionSummary from './VoucherValueDecisionSummary'
import {
  sendVoucherValueDecisionDraftsMutation,
  voucherValueDecisionQuery
} from './queries'

const VoucherValueDecisionMetadataSection = React.memo(
  function VoucherValueDecisionMetadataSection({
    voucherValueDecisionId
  }: {
    voucherValueDecisionId: VoucherValueDecisionId
  }) {
    const result = useQueryResult(
      voucherValueDecisionMetadataQuery({ voucherValueDecisionId })
    )
    return <MetadataSection metadataResult={result} />
  }
)

export default React.memo(function VoucherValueDecisionPage() {
  const [showHandlerSelectModal, setShowHandlerSelectModal] = useState(false)
  const id = useIdRouteParam<VoucherValueDecisionId>('id')
  const { i18n } = useTranslation()
  const decisionResponse = useQueryResult(voucherValueDecisionQuery({ id }))
  const { mutateAsync: doSendVoucherValueDecisionDrafts } = useMutationResult(
    sendVoucherValueDecisionDraftsMutation
  )
  const [modified, setModified] = useState<boolean>(false)
  const [newDecisionType, setNewDecisionType] =
    useState<VoucherValueDecisionType>('NORMAL')

  useEffect(() => {
    if (decisionResponse.isSuccess) {
      const decision = decisionResponse.value.data
      setNewDecisionType(decision.decisionType)
    }
  }, [decisionResponse])

  useTitle(
    decisionResponse.map(
      (value) =>
        `${formatPersonName(value.data.headOfFamily, 'Last First')} | ${value.data.status === 'DRAFT' ? i18n.titles.valueDecisionDraft : i18n.titles.valueDecision}`
    )
  )

  const decisionType = decisionResponse.map(({ data }) => data.decisionType)
  const changeDecisionType = useCallback(
    (type: VoucherValueDecisionType) => {
      if (decisionType.isSuccess) {
        setNewDecisionType(type)
        if (decisionType.value === type) setModified(false)
        else setModified(true)
      }
    },
    [decisionType]
  )

  const goBack = useCallback(() => history.go(-1), [])

  if (decisionResponse.isFailure) {
    return <Redirect replace to="/finance/value-decisions" />
  }

  return (
    <Container data-qa="voucher-value-decision-page">
      <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
      {renderResult(
        decisionResponse,
        ({ data: decision, permittedActions }) => (
          <>
            {showHandlerSelectModal && (
              <FinanceDecisionHandlerSelectModal
                onResolve={async (decisionHandlerId) => {
                  const result = await doSendVoucherValueDecisionDrafts({
                    decisionHandlerId,
                    body: [decision.id]
                  })
                  if (result.isSuccess) {
                    setShowHandlerSelectModal(false)
                  }
                  return result
                }}
                onReject={() => setShowHandlerSelectModal(false)}
                checkedIds={[decision.id]}
              />
            )}
            <ContentArea opaque>
              <VoucherValueDecisionHeading
                decision={decision}
                changeDecisionType={changeDecisionType}
                newDecisionType={newDecisionType}
              />
              <VoucherValueDecisionChildSection
                key={decision.child.id}
                child={decision.child}
                placement={decision.placement}
                serviceNeed={decision.serviceNeed}
              />
              <VoucherValueDecisionSummary decision={decision} />
              <VoucherValueDecisionActionBar
                decision={decision}
                goToDecisions={goBack}
                modified={modified}
                setModified={setModified}
                newDecisionType={newDecisionType}
                onHandlerSelectModal={() => setShowHandlerSelectModal(true)}
              />
            </ContentArea>
            {decision.status !== 'DRAFT' &&
              permittedActions.includes('READ_METADATA') && (
                <>
                  <Gap />
                  <VoucherValueDecisionMetadataSection
                    voucherValueDecisionId={id}
                  />
                </>
              )}
          </>
        )
      )}
    </Container>
  )
})
