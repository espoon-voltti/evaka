// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { wrapResult } from 'lib-common/api'
import {
  FeeDecisionDetailed,
  FeeDecisionType
} from 'lib-common/generated/api-types/invoicing'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { featureFlags } from 'lib-customizations/employee'

import {
  confirmFeeDecisionDrafts,
  setFeeDecisionSent,
  setFeeDecisionType
} from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'

const confirmFeeDecisionDraftsResult = wrapResult(confirmFeeDecisionDrafts)
const setFeeDecisionSentResult = wrapResult(setFeeDecisionSent)
const setFeeDecisionTypeResult = wrapResult(setFeeDecisionType)

interface Props {
  decision: FeeDecisionDetailed
  goToDecisions(): void
  loadDecision(): Promise<void>
  modified: boolean
  setModified: (value: boolean) => void
  newDecisionType: FeeDecisionType
  onHandlerSelectModal: () => void
}

const Actions = React.memo(function Actions({
  decision,
  goToDecisions,
  loadDecision,
  modified,
  setModified,
  newDecisionType,
  onHandlerSelectModal
}: Props) {
  const { i18n } = useTranslation()
  const updateType = useCallback(
    () =>
      setFeeDecisionTypeResult({
        id: decision.id,
        body: { type: newDecisionType }
      }),
    [decision.id, newDecisionType]
  )
  const confirmDecision = useCallback(
    () => confirmFeeDecisionDraftsResult({ body: [decision.id] }),
    [decision.id]
  )
  const markSent = useCallback(
    () => setFeeDecisionSentResult({ body: [decision.id] }),
    [decision.id]
  )

  const isDraft = decision.status === 'DRAFT'
  const isWaiting = decision.status === 'WAITING_FOR_MANUAL_SENDING'

  return (
    <FixedSpaceRow justifyContent="flex-end">
      {isDraft ? (
        <>
          <Button
            onClick={goToDecisions}
            disabled={!modified}
            data-qa="decision-actions-close"
            text={i18n.feeDecisions.buttons.close}
          />
          <AsyncButton
            text={i18n.common.save}
            textInProgress={i18n.common.saving}
            textDone={i18n.common.saved}
            onClick={updateType}
            onSuccess={() => loadDecision().then(() => setModified(false))}
            disabled={!modified}
            data-qa="decision-actions-save"
          />
          {featureFlags.financeDecisionHandlerSelect ? (
            <Button
              primary
              text={i18n.feeDecisions.buttons.createDecision(1)}
              disabled={modified}
              onClick={() => onHandlerSelectModal()}
              data-qa="open-decision-handler-select-modal"
            />
          ) : (
            <AsyncButton
              primary
              text={i18n.feeDecisions.buttons.createDecision(1)}
              onClick={confirmDecision}
              onSuccess={loadDecision}
              disabled={modified}
              data-qa="decision-actions-confirm-decision"
            />
          )}
        </>
      ) : null}
      {isWaiting ? (
        <AsyncButton
          primary
          text={i18n.feeDecisions.buttons.markSent}
          onClick={markSent}
          onSuccess={loadDecision}
          data-qa="decision-actions-mark-sent"
        />
      ) : null}
    </FixedSpaceRow>
  )
})

export default Actions
