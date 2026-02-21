// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import type {
  FeeDecisionDetailed,
  FeeDecisionType
} from 'lib-common/generated/api-types/invoicing'
import { useMutationResult } from 'lib-common/query'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'

import {
  confirmFeeDecisionDraftsMutation,
  setFeeDecisionSentMutation,
  setFeeDecisionTypeMutation
} from './queries'

interface Props {
  decision: FeeDecisionDetailed
  goToDecisions: () => void
  modified: boolean
  setModified: (value: boolean) => void
  newDecisionType: FeeDecisionType
  onHandlerSelectModal: () => void
}

const Actions = React.memo(function Actions({
  decision,
  goToDecisions,
  modified,
  setModified,
  newDecisionType,
  onHandlerSelectModal
}: Props) {
  const { i18n } = useTranslation()
  const { mutateAsync: doSetFeeDecisionType } = useMutationResult(
    setFeeDecisionTypeMutation
  )
  const { mutateAsync: doConfirmFeeDecisionDrafts } = useMutationResult(
    confirmFeeDecisionDraftsMutation
  )
  const { mutateAsync: doSetFeeDecisionSent } = useMutationResult(
    setFeeDecisionSentMutation
  )
  const updateType = useCallback(
    () =>
      doSetFeeDecisionType({
        id: decision.id,
        body: { type: newDecisionType }
      }),
    [decision.id, newDecisionType, doSetFeeDecisionType]
  )
  const confirmDecision = useCallback(
    () => doConfirmFeeDecisionDrafts({ body: [decision.id] }),
    [decision.id, doConfirmFeeDecisionDrafts]
  )
  const markSent = useCallback(
    () => doSetFeeDecisionSent({ body: [decision.id] }),
    [decision.id, doSetFeeDecisionSent]
  )

  const isDraft = decision.status === 'DRAFT'
  const isWaiting = decision.status === 'WAITING_FOR_MANUAL_SENDING'

  return (
    <FixedSpaceRow justifyContent="flex-end">
      {isDraft ? (
        <>
          <LegacyButton
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
            onSuccess={() => setModified(false)}
            disabled={!modified}
            data-qa="decision-actions-save"
          />
          {featureFlags.financeDecisionHandlerSelect ? (
            <LegacyButton
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
              onSuccess={() => undefined}
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
          onSuccess={() => undefined}
          data-qa="decision-actions-mark-sent"
        />
      ) : null}
    </FixedSpaceRow>
  )
})

export default Actions
