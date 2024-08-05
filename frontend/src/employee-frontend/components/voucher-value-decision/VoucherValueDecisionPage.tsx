// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'

import { Loading, Result, wrapResult } from 'lib-common/api'
import {
  VoucherValueDecisionDetailed,
  VoucherValueDecisionType
} from 'lib-common/generated/api-types/invoicing'
import useRouteParams from 'lib-common/useRouteParams'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'

import {
  getVoucherValueDecision,
  sendVoucherValueDecisionDrafts
} from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import FinanceDecisionHandlerSelectModal from '../finance-decisions/FinanceDecisionHandlerSelectModal'

import VoucherValueDecisionActionBar from './VoucherValueDecisionActionBar'
import VoucherValueDecisionChildSection from './VoucherValueDecisionChildSection'
import VoucherValueDecisionHeading from './VoucherValueDecisionHeading'
import VoucherValueDecisionSummary from './VoucherValueDecisionSummary'

const getVoucherValueDecisionResult = wrapResult(getVoucherValueDecision)
const sendVoucherValueDecisionDraftsResult = wrapResult(
  sendVoucherValueDecisionDrafts
)

export default React.memo(function VoucherValueDecisionPage() {
  const [showHandlerSelectModal, setShowHandlerSelectModal] = useState(false)
  const navigate = useNavigate()
  const { id } = useRouteParams(['id'])
  const { i18n } = useTranslation()
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)
  const [decision, setDecision] = useState<
    Result<VoucherValueDecisionDetailed>
  >(Loading.of())
  const [modified, setModified] = useState<boolean>(false)
  const [newDecisionType, setNewDecisionType] =
    useState<VoucherValueDecisionType>('NORMAL')

  const loadDecision = useCallback(
    () => getVoucherValueDecisionResult({ id }).then(setDecision),
    [id]
  )
  useEffect(() => void loadDecision(), [loadDecision])

  useEffect(() => {
    if (decision.isSuccess) {
      const name = formatTitleName(
        decision.value.headOfFamily.firstName,
        decision.value.headOfFamily.lastName
      )
      if (decision.value.status === 'DRAFT') {
        setTitle(`${name} | ${i18n.titles.valueDecisionDraft}`)
      } else {
        setTitle(`${name} | ${i18n.titles.valueDecision}`)
      }
      setNewDecisionType(decision.value.decisionType)
    }
  }, [decision, formatTitleName, setTitle, i18n])

  const decisionType = decision.map(({ decisionType }) => decisionType)
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

  if (decision.isFailure) {
    return <Navigate replace to="/finance/value-decisions" />
  }

  return (
    <Container data-qa="voucher-value-decision-page">
      <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
      {decision.isSuccess && (
        <>
          {showHandlerSelectModal && (
            <FinanceDecisionHandlerSelectModal
              onResolve={async (decisionHandlerId) => {
                const result = await sendVoucherValueDecisionDraftsResult({
                  decisionHandlerId,
                  body: [decision.value.id]
                })
                if (result.isSuccess) {
                  await loadDecision()
                  setShowHandlerSelectModal(false)
                }
                return result
              }}
              onReject={() => setShowHandlerSelectModal(false)}
              checkedIds={[decision.value.id]}
            />
          )}
          <ContentArea opaque>
            <VoucherValueDecisionHeading
              decision={decision.value}
              changeDecisionType={changeDecisionType}
              newDecisionType={newDecisionType}
            />
            <VoucherValueDecisionChildSection
              key={decision.value.child.id}
              child={decision.value.child}
              placement={decision.value.placement}
              serviceNeed={decision.value.serviceNeed}
            />
            <VoucherValueDecisionSummary decision={decision.value} />
            <VoucherValueDecisionActionBar
              decision={decision.value}
              goToDecisions={goBack}
              loadDecision={loadDecision}
              modified={modified}
              setModified={setModified}
              newDecisionType={newDecisionType}
              onHandlerSelectModal={() => setShowHandlerSelectModal(true)}
            />
          </ContentArea>
        </>
      )}
    </Container>
  )
})
