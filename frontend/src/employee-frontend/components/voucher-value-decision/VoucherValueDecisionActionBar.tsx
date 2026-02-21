// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext } from 'react'

import type {
  VoucherValueDecisionDetailed,
  VoucherValueDecisionType
} from 'lib-common/generated/api-types/invoicing'
import { useMutationResult } from 'lib-common/query'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import StickyActionBar from '../common/StickyActionBar'

import {
  markVoucherValueDecisionSentMutation,
  sendVoucherValueDecisionDraftsMutation,
  setVoucherValueDecisionTypeMutation
} from './queries'

type Props = {
  decision: VoucherValueDecisionDetailed
  goToDecisions: () => void
  modified: boolean
  setModified: (value: boolean) => void
  newDecisionType: VoucherValueDecisionType
  onHandlerSelectModal: () => void
}

export default React.memo(function VoucherValueDecisionActionBar({
  decision,
  goToDecisions,
  modified,
  setModified,
  newDecisionType,
  onHandlerSelectModal
}: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage, clearErrorMessage } = useContext(UIContext)
  const { mutateAsync: doSetVoucherValueDecisionType } = useMutationResult(
    setVoucherValueDecisionTypeMutation
  )
  const { mutateAsync: doSendVoucherValueDecisionDrafts } = useMutationResult(
    sendVoucherValueDecisionDraftsMutation
  )
  const { mutateAsync: doMarkVoucherValueDecisionSent } = useMutationResult(
    markVoucherValueDecisionSentMutation
  )
  const updateType = useCallback(
    () =>
      doSetVoucherValueDecisionType({
        id: decision.id,
        body: { type: newDecisionType }
      }),
    [decision.id, newDecisionType, doSetVoucherValueDecisionType]
  )
  const sendDecision = useCallback(
    () => doSendVoucherValueDecisionDrafts({ body: [decision.id] }),
    [decision.id, doSendVoucherValueDecisionDrafts]
  )

  const isDraft = decision.status === 'DRAFT'
  const isWaiting = decision.status === 'WAITING_FOR_MANUAL_SENDING'

  return (
    <FixedSpaceRow justifyContent="flex-end">
      {isDraft && (
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
            onSuccess={() => {
              clearErrorMessage()
              setModified(false)
            }}
            onFailure={() => {
              setErrorMessage({
                title: i18n.common.error.unknown,
                text: i18n.common.error.saveFailed,
                type: 'error',
                resolveLabel: i18n.common.ok
              })
            }}
            disabled={!modified}
            data-qa="button-save-decision"
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
              data-qa="button-send-decision"
              disabled={modified}
              text={i18n.common.send}
              onClick={sendDecision}
              onSuccess={clearErrorMessage}
              onFailure={(result) => {
                setErrorMessage({
                  title: i18n.common.error.unknown,
                  text:
                    result.errorCode === 'WAITING_FOR_MANUAL_SENDING'
                      ? i18n.valueDecisions.buttons.errors
                          .WAITING_FOR_MANUAL_SENDING
                      : i18n.common.error.saveFailed,
                  type: 'error',
                  resolveLabel: i18n.common.ok
                })
              }}
            />
          )}
        </>
      )}
      {isWaiting && (
        <StickyActionBar align="right">
          <AsyncButton
            data-qa="button-mark-decision-sent"
            primary
            text={i18n.valueDecisions.buttons.markSent}
            onClick={() =>
              doMarkVoucherValueDecisionSent({ body: [decision.id] })
            }
            onSuccess={() => undefined}
            disabled={modified}
          />
        </StickyActionBar>
      )}
    </FixedSpaceRow>
  )
})
