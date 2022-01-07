// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  markVoucherValueDecisionSent,
  sendVoucherValueDecisions,
  setVoucherDecisionType
} from '../../api/invoicing'
import StickyActionBar from '../../components/common/StickyActionBar'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { VoucherValueDecisionDetailed } from '../../types/invoicing'

type Props = {
  decision: VoucherValueDecisionDetailed
  loadDecision: () => Promise<void>
  modified: boolean
  setModified: (value: boolean) => void
  newDecisionType: string
}

export default React.memo(function VoucherValueDecisionActionBar({
  decision,
  loadDecision,
  modified,
  setModified,
  newDecisionType
}: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage, clearErrorMessage } = useContext(UIContext)

  const updateType = () =>
    setVoucherDecisionType(decision.id, newDecisionType).then((result) => {
      if (result.isSuccess) {
        clearErrorMessage()
      } else if (result.isFailure) {
        setErrorMessage({
          title: i18n.common.error.unknown,
          text: i18n.common.error.saveFailed,
          type: 'error',
          resolveLabel: i18n.common.ok
        })
      }
    })

  const isDraft = decision.status === 'DRAFT'
  const isWaiting = decision.status === 'WAITING_FOR_MANUAL_SENDING'

  return (
    <FixedSpaceRow justifyContent="flex-end">
      {isDraft && (
        <>
          <AsyncButton
            text={i18n.common.save}
            textInProgress={i18n.common.saving}
            textDone={i18n.common.saved}
            onClick={updateType}
            onSuccess={() => loadDecision().then(() => setModified(false))}
            disabled={!modified}
            data-qa="button-save-decision"
          />
          <AsyncButton
            primary
            data-qa={'button-send-decision'}
            disabled={modified}
            text="Lähetä"
            onClick={() =>
              sendVoucherValueDecisions([decision.id]).then((result) => {
                if (result.isSuccess) {
                  clearErrorMessage()
                } else if (result.isFailure) {
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
                }
              })
            }
            onSuccess={loadDecision}
          />
        </>
      )}
      {isWaiting && (
        <StickyActionBar align={'right'}>
          <AsyncButton
            data-qa={'button-mark-decision-sent'}
            primary
            text={i18n.valueDecisions.buttons.markSent}
            onClick={() => markVoucherValueDecisionSent([decision.id])}
            disabled={modified}
            onSuccess={loadDecision}
          />
        </StickyActionBar>
      )}
    </FixedSpaceRow>
  )
})
