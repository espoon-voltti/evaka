// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router'

import { Loading, Result, wrapResult } from 'lib-common/api'
import {
  VoucherValueDecisionResponse,
  VoucherValueDecisionType
} from 'lib-common/generated/api-types/invoicing'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import {
  getVoucherValueDecision,
  sendVoucherValueDecisionDrafts
} from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import MetadataSection from '../archive-metadata/MetadataSection'
import { renderResult } from '../async-rendering'
import FinanceDecisionHandlerSelectModal from '../finance-decisions/FinanceDecisionHandlerSelectModal'
import { voucherValueDecisionMetadataQuery } from '../voucher-value-decisions/voucher-value-decision-queries'

import VoucherValueDecisionActionBar from './VoucherValueDecisionActionBar'
import VoucherValueDecisionChildSection from './VoucherValueDecisionChildSection'
import VoucherValueDecisionHeading from './VoucherValueDecisionHeading'
import VoucherValueDecisionSummary from './VoucherValueDecisionSummary'

const getVoucherValueDecisionResult = wrapResult(getVoucherValueDecision)
const sendVoucherValueDecisionDraftsResult = wrapResult(
  sendVoucherValueDecisionDrafts
)

const VoucherValueDecisionMetadataSection = React.memo(
  function VoucherValueDecisionMetadataSection({
    voucherValueDecisionId
  }: {
    voucherValueDecisionId: UUID
  }) {
    const result = useQueryResult(
      voucherValueDecisionMetadataQuery({ voucherValueDecisionId })
    )
    return <MetadataSection metadataResult={result} />
  }
)

export default React.memo(function VoucherValueDecisionPage() {
  const [showHandlerSelectModal, setShowHandlerSelectModal] = useState(false)
  const navigate = useNavigate()
  const { id } = useRouteParams(['id'])
  const { i18n } = useTranslation()
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)
  const [decisionResponse, setDecisionResponse] = useState<
    Result<VoucherValueDecisionResponse>
  >(Loading.of())
  const [modified, setModified] = useState<boolean>(false)
  const [newDecisionType, setNewDecisionType] =
    useState<VoucherValueDecisionType>('NORMAL')

  const loadDecision = useCallback(
    () => getVoucherValueDecisionResult({ id }).then(setDecisionResponse),
    [id]
  )
  useEffect(() => void loadDecision(), [loadDecision])

  useEffect(() => {
    if (decisionResponse.isSuccess) {
      const decision = decisionResponse.value.data
      const name = formatTitleName(
        decision.headOfFamily.firstName,
        decision.headOfFamily.lastName
      )
      if (decision.status === 'DRAFT') {
        setTitle(`${name} | ${i18n.titles.valueDecisionDraft}`)
      } else {
        setTitle(`${name} | ${i18n.titles.valueDecision}`)
      }
      setNewDecisionType(decision.decisionType)
    }
  }, [decisionResponse, formatTitleName, setTitle, i18n])

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

  const goBack = useCallback(() => navigate(-1), [navigate])

  if (decisionResponse.isFailure) {
    return <Navigate replace to="/finance/value-decisions" />
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
                  const result = await sendVoucherValueDecisionDraftsResult({
                    decisionHandlerId,
                    body: [decision.id]
                  })
                  if (result.isSuccess) {
                    await loadDecision()
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
                loadDecision={loadDecision}
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
