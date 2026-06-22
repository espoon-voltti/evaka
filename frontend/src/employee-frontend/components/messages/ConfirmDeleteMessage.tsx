// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import type {
  MessageAccountId,
  MessageContentId
} from 'lib-common/generated/api-types/shared'
import { invalidateDependencies } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { DangerMutateButton } from 'lib-components/atoms/buttons/MutateButton'
import BaseModal, {
  ModalButtons
} from 'lib-components/molecules/modals/BaseModal'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H3, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'

import { deleteMessageContentMutation } from './queries'

const Blockquote = styled.blockquote`
  margin: ${defaultMargins.s} 0;
  padding-left: ${defaultMargins.s};
  border-left: 4px solid ${(p) => p.theme.colors.grayscale.g35};
  color: ${(p) => p.theme.colors.grayscale.g70};
  white-space: pre-line;
`

export interface Props {
  accountId: MessageAccountId
  contentId: MessageContentId
  onClose: () => void
  onDeleted: () => void
}

export const ConfirmDeleteMessage = React.memo(function ConfirmDeleteMessage({
  accountId,
  contentId,
  onClose,
  onDeleted
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.messages.deletion.modal
  const { featureConfig } = useContext(UserContext)
  const queryClient = useQueryClient()
  const [alreadyDeleted, setAlreadyDeleted] = useState(false)

  if (alreadyDeleted) {
    // Shown when a concurrent delete was detected on confirm (see onFailure below).
    return (
      <InfoModal
        data-qa="message-already-deleted-modal"
        title={i18n.messages.deletion.alreadyDeleted}
        icon={faTrash}
        type="info"
        resolve={{ action: onClose, label: i18n.common.ok }}
      />
    )
  }

  return (
    <BaseModal
      data-qa="delete-message-modal"
      title={t.title}
      icon={faTrash}
      type="danger"
      width="wide"
      close={onClose}
      closeLabel={t.cancel}
    >
      <P>{t.intro}</P>
      <Blockquote data-qa="placeholder-quote">
        {featureConfig?.deletedMessagePlaceholderBody}
      </Blockquote>
      <H3>{t.stepsHeader}</H3>
      <P>{t.stepsBody1}</P>
      <P>{t.stepsBody2}</P>
      <ModalButtons $justifyContent="center">
        <DangerMutateButton
          primary
          mutation={deleteMessageContentMutation}
          text={t.confirm}
          onClick={() => ({ accountId, contentId })}
          onSuccess={() => {
            onDeleted()
            onClose()
          }}
          onFailure={(failure) => {
            // 409 = the content was already deleted (e.g. concurrently by another
            // user on a shared account). Refresh the views and switch to the
            // "already deleted" acknowledgment — without marking it as deleted by
            // this user, so the "you deleted this" banner is not shown.
            if (failure.statusCode === 409) {
              void invalidateDependencies(
                queryClient,
                deleteMessageContentMutation,
                { accountId, contentId }
              )
              setAlreadyDeleted(true)
            }
          }}
          preventDefault
          stopPropagation
          data-qa="modal-okBtn"
        />
        <Gap $horizontal $size="s" />
        <Button
          onClick={(e) => {
            e.preventDefault()
            e.stopPropagation()
            onClose()
          }}
          data-qa="modal-cancelBtn"
          text={t.cancel}
        />
      </ModalButtons>
    </BaseModal>
  )
})
