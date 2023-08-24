// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { FeeDecisionStatus } from 'lib-common/generated/api-types/invoicing'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import {
  confirmFeeDecisions,
  ignoreFeeDecisionDrafts
} from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import { CheckedRowsInfo } from '../common/CheckedRowsInfo'
import StickyActionBar from '../common/StickyActionBar'

const ErrorMessage = styled.div`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
  margin: 0 20px;
`

type Props = {
  statuses: FeeDecisionStatus[]
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

  return statuses.length === 1 && statuses[0] === 'DRAFT' ? (
    <>
      <StickyActionBar align="right">
        <FixedSpaceRow alignItems="center">
          {error ? <ErrorMessage>{error}</ErrorMessage> : null}
          {checkedIds.length > 0 ? (
            <CheckedRowsInfo>
              {i18n.feeDecisions.buttons.checked(checkedIds.length)}
            </CheckedRowsInfo>
          ) : null}
          {featureFlags.experimental?.feeDecisionIgnoredStatus && (
            <Button
              text={i18n.feeDecisions.buttons.ignoreDraft}
              disabled={checkedIds.length !== 1}
              onClick={() => setShowIgnoreModal(true)}
              data-qa="open-ignore-draft-modal"
            />
          )}
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
              text={i18n.feeDecisions.buttons.createDecision(checkedIds.length)}
              disabled={checkedIds.length === 0}
              onClick={() => confirmFeeDecisions(checkedIds)}
              onSuccess={() => {
                setError(undefined)
                clearChecked()
                loadDecisions()
              }}
              onFailure={(result) => {
                setError(
                  result.errorCode === 'WAITING_FOR_MANUAL_SENDING'
                    ? i18n.feeDecisions.buttons.errors
                        .WAITING_FOR_MANUAL_SENDING
                    : i18n.common.error.unknown
                )
              }}
              data-qa="confirm-decisions"
            />
          )}
        </FixedSpaceRow>
      </StickyActionBar>
      {showIgnoreModal && (
        <IgnoreDraftModal
          id={checkedIds[0]}
          onCancel={() => setShowIgnoreModal(false)}
          onSuccess={() => {
            setShowIgnoreModal(false)
            loadDecisions()
          }}
        />
      )}
    </>
  ) : null
})

const IgnoreDraftModal = React.memo(function IgnoreDraftModal({
  id,
  onCancel,
  onSuccess
}: {
  id: UUID
  onCancel: () => void
  onSuccess: () => void
}) {
  const { i18n } = useTranslation()
  const [confirm, setConfirm] = useState(false)

  return (
    <AsyncFormModal
      title={i18n.feeDecisions.ignoreDraftModal.title}
      resolveAction={() => ignoreFeeDecisionDrafts([id])}
      resolveLabel={i18n.common.confirm}
      onSuccess={onSuccess}
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!confirm}
    >
      {i18n.feeDecisions.ignoreDraftModal.content}
      <Gap />
      <Checkbox
        label={i18n.feeDecisions.ignoreDraftModal.confirm}
        checked={confirm}
        onChange={setConfirm}
      />
    </AsyncFormModal>
  )
})

export default Actions
