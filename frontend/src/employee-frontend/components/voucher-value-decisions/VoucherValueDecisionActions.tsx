// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { VoucherValueDecisionStatus } from 'lib-common/generated/api-types/invoicing'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { featureFlags } from 'lib-customizations/employee'

import {
  ignoreVoucherValueDecisionDrafts,
  sendVoucherValueDecisionDrafts,
  unignoreVoucherValueDecisionDrafts
} from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import { CheckedRowsInfo } from '../common/CheckedRowsInfo'
import StickyActionBar from '../common/StickyActionBar'
import { IgnoreDraftModal } from '../finance-decisions/IgnoreDraftModal'

const sendVoucherValueDecisionDraftsResult = wrapResult(
  sendVoucherValueDecisionDrafts
)
const ignoreVoucherValueDecisionDraftsResult = wrapResult(
  ignoreVoucherValueDecisionDrafts
)
const unignoreVoucherValueDecisionDraftsResult = wrapResult(
  unignoreVoucherValueDecisionDrafts
)

const ErrorMessage = styled.div`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
  margin: 0 20px;
`

type Props = {
  statuses: VoucherValueDecisionStatus[]
  checkedIds: string[]
  clearChecked: () => void
  loadDecisions: () => void
  onHandlerSelectModal: () => void
}

const Actions = React.memo(function Actions({
  statuses,
  checkedIds,
  clearChecked,
  loadDecisions,
  onHandlerSelectModal
}: Props) {
  const { i18n } = useTranslation()
  const [error, setError] = useState<string>()
  const [showIgnoreModal, setShowIgnoreModal] = useState(false)

  if (statuses.length === 1 && statuses[0] === 'IGNORED') {
    return (
      <StickyActionBar align="right">
        <AsyncButton
          text={i18n.valueDecisions.buttons.unignoreDrafts(checkedIds.length)}
          disabled={checkedIds.length === 0}
          onClick={() =>
            unignoreVoucherValueDecisionDraftsResult({ body: checkedIds })
          }
          onSuccess={() => {
            setError(undefined)
            clearChecked()
            loadDecisions()
          }}
          data-qa="unignore-decisions"
        />
      </StickyActionBar>
    )
  }

  if (statuses.length === 1 && statuses[0] === 'DRAFT') {
    return (
      <>
        <StickyActionBar align="right">
          {error ? <ErrorMessage>{error}</ErrorMessage> : null}
          {checkedIds.length > 0 ? (
            <CheckedRowsInfo>
              {i18n.valueDecisions.buttons.checked(checkedIds.length)}
            </CheckedRowsInfo>
          ) : null}
          <Button
            text={i18n.feeDecisions.buttons.ignoreDraft}
            disabled={checkedIds.length !== 1}
            onClick={() => setShowIgnoreModal(true)}
            data-qa="open-ignore-draft-modal"
          />
          {featureFlags.financeDecisionHandlerSelect ? (
            <Button
              primary
              text={i18n.feeDecisions.buttons.createDecision(checkedIds.length)}
              disabled={checkedIds.length === 0}
              onClick={() => onHandlerSelectModal()}
              data-qa="open-decision-handler-select-modal"
            />
          ) : (
            <AsyncButton
              primary
              text={i18n.valueDecisions.buttons.createDecision(
                checkedIds.length
              )}
              disabled={checkedIds.length === 0}
              onClick={() =>
                sendVoucherValueDecisionDraftsResult({ body: checkedIds })
              }
              onSuccess={() => {
                setError(undefined)
                clearChecked()
                loadDecisions()
              }}
              onFailure={(result) => {
                setError(
                  result.errorCode === 'WAITING_FOR_MANUAL_SENDING'
                    ? i18n.valueDecisions.buttons.errors
                        .WAITING_FOR_MANUAL_SENDING
                    : i18n.common.error.unknown
                )
              }}
              data-qa="send-decisions"
            />
          )}
        </StickyActionBar>
        {showIgnoreModal && (
          <IgnoreDraftModal
            onConfirm={() =>
              ignoreVoucherValueDecisionDraftsResult({ body: checkedIds })
            }
            onCancel={() => setShowIgnoreModal(false)}
            onSuccess={() => {
              setShowIgnoreModal(false)
              setError(undefined)
              clearChecked()
              loadDecisions()
            }}
          />
        )}
      </>
    )
  }

  return null
})

export default Actions
