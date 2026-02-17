// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import { Redirect } from 'wouter'

import type { FeeDecisionType } from 'lib-common/generated/api-types/invoicing'
import type { FeeDecisionId } from 'lib-common/generated/api-types/shared'
import { formatPersonName } from 'lib-common/names'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Gap } from 'lib-components/white-space'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { useTitle } from '../../utils/useTitle'
import MetadataSection from '../archive-metadata/MetadataSection'
import { renderResult } from '../async-rendering'
import { feeDecisionMetadataQuery } from '../fee-decisions/fee-decision-queries'
import FinanceDecisionHandlerSelectModal from '../finance-decisions/FinanceDecisionHandlerSelectModal'

import Actions from './Actions'
import ChildSection from './ChildSection'
import Heading from './Heading'
import Summary from './Summary'
import { confirmFeeDecisionDraftsMutation, feeDecisionQuery } from './queries'

const FeeDecisionMetadataSection = React.memo(
  function FeeDecisionMetadataSection({
    feeDecisionId
  }: {
    feeDecisionId: FeeDecisionId
  }) {
    const result = useQueryResult(feeDecisionMetadataQuery({ feeDecisionId }))
    return <MetadataSection metadataResult={result} />
  }
)

export default React.memo(function FeeDecisionDetailsPage() {
  const [showHandlerSelectModal, setShowHandlerSelectModal] = useState(false)
  const id = useIdRouteParam<FeeDecisionId>('id')
  const { i18n } = useTranslation()
  const decisionResponse = useQueryResult(feeDecisionQuery({ id }))
  const { mutateAsync: doConfirmFeeDecisionDrafts } = useMutationResult(
    confirmFeeDecisionDraftsMutation
  )
  const [modified, setModified] = useState<boolean>(false)
  const [newDecisionType, setNewDecisionType] =
    useState<FeeDecisionType>('NORMAL')
  const [confirmingBack, setConfirmingBack] = useState<boolean>(false)

  useEffect(() => {
    if (decisionResponse.isSuccess) {
      const decision = decisionResponse.value.data
      setNewDecisionType(decision.decisionType)
    }
  }, [decisionResponse])

  useTitle(
    decisionResponse.map(
      (value) =>
        `${formatPersonName(value.data.headOfFamily, 'Last First')} | ${value.data.status === 'DRAFT' ? i18n.titles.feeDecisionDraft : i18n.titles.feeDecision}`
    )
  )

  const decisionType = decisionResponse.map(({ data }) => data.decisionType)
  const changeDecisionType = useCallback(
    (type: FeeDecisionType) => {
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
    return <Redirect replace to="/finance/fee-decisions" />
  }

  return (
    <>
      <Container
        className="fee-decision-details-page"
        data-qa="fee-decision-details-page"
      >
        <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
        {renderResult(
          decisionResponse,
          ({ data: decision, permittedActions }) => (
            <>
              {showHandlerSelectModal && (
                <FinanceDecisionHandlerSelectModal
                  onResolve={async (decisionHandlerId) => {
                    const result = await doConfirmFeeDecisionDrafts({
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
                <Heading
                  {...decision}
                  changeDecisionType={changeDecisionType}
                  newDecisionType={newDecisionType}
                />
                {decision.children.map(
                  ({
                    child,
                    placementType,
                    placementUnit,
                    serviceNeedDescriptionFi
                  }) => (
                    <ChildSection
                      key={child.id}
                      child={child}
                      placementType={placementType}
                      placementUnit={placementUnit}
                      serviceNeedDescription={serviceNeedDescriptionFi}
                    />
                  )
                )}
                <Summary decision={decision} />
                <Actions
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
                    <FeeDecisionMetadataSection feeDecisionId={id} />
                  </>
                )}
            </>
          )
        )}
      </Container>
      {confirmingBack && (
        <InfoModal
          title={i18n.feeDecision.modal.title}
          type="warning"
          icon={faQuestion}
          reject={{ action: goBack, label: i18n.feeDecision.modal.cancel }}
          resolve={{
            action: () => {
              setConfirmingBack(false)
            },
            label: i18n.feeDecision.modal.confirm
          }}
        />
      )}
    </>
  )
})
