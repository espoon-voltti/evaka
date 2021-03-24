// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { useTranslation } from '../../state/i18n'
import { FeeDecisionDetailed } from '../../types/invoicing'
import {
  confirmFeeDecisions,
  markFeeDecisionSent,
  setDecisionType
} from '../../api/invoicing'
import { ErrorMessage } from '../../components/fee-decision-details/FeeDecisionDetailsPage'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

interface Props {
  decision: FeeDecisionDetailed
  goToDecisions(): void
  loadDecision(): Promise<void>
  modified: boolean
  setModified: (value: boolean) => void
  newDecisionType: string
}

const Actions = React.memo(function Actions({
  decision,
  goToDecisions,
  loadDecision,
  modified,
  setModified,
  newDecisionType
}: Props) {
  const { i18n } = useTranslation()
  const [error, setError] = useState(false)

  const updateType = () =>
    setDecisionType(decision.id, newDecisionType)
      .then(() => void setError(false))
      .catch(() => setError(true))

  const confirmDecision = () =>
    confirmFeeDecisions([decision.id])
      .then(() => void setError(false))
      .catch(() => void setError(true))

  const markSent = () =>
    markFeeDecisionSent([decision.id])
      .then(() => void setError(false))
      .catch(() => void setError(true))

  const isDraft = decision.status === 'DRAFT'
  const isWaiting = decision.status === 'WAITING_FOR_MANUAL_SENDING'

  return (
    <FixedSpaceRow justifyContent="flex-end">
      {error ? <ErrorMessage>{i18n.common.error.unknown}</ErrorMessage> : null}
      {isDraft ? (
        <>
          <Button
            onClick={goToDecisions}
            disabled={!modified}
            dataQa="decision-actions-close"
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
          <AsyncButton
            primary
            text={i18n.feeDecisions.buttons.createDecision(1)}
            onClick={confirmDecision}
            onSuccess={loadDecision}
            disabled={modified}
            data-qa="decision-actions-confirm-decision"
          />
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
