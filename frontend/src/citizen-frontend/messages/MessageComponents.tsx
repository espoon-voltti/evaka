// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled, { css } from 'styled-components'

import { MessageType } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { desktopMin } from 'lib-components/breakpoints'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../localization'

import { archiveBulletin, archiveThread } from './api'

const messageContainerStyles = css`
  background-color: ${colors.grayscale.g0};
  padding: ${defaultMargins.s};

  @media (min-width: ${desktopMin}) {
    padding: ${defaultMargins.L};
  }

  margin: ${defaultMargins.xxs} ${defaultMargins.xxs} ${defaultMargins.s}
    ${defaultMargins.xxs};
`

export const MessageContainer = styled.li`
  ${messageContainerStyles}

  h2 {
    margin: 0;
  }
`

export const ReplyEditorContainer = styled.div`
  ${messageContainerStyles}
`

export interface ConfirmDeleteThreadProps {
  id: UUID
  type: MessageType
  onClose: () => void
  onSuccess: () => void
}

export const ConfirmDeleteThread = React.memo(function ConfirmDeleteThread({
  id,
  type,
  onClose,
  onSuccess
}: ConfirmDeleteThreadProps) {
  const t = useTranslation()
  const onArchive = useCallback(
    () => (type === 'MESSAGE' ? archiveThread(id) : archiveBulletin(id)),
    [id, type]
  )
  return (
    <AsyncFormModal
      resolveAction={onArchive}
      rejectAction={onClose}
      title={t.messages.confirmDelete.title}
      text={t.messages.confirmDelete.text}
      onSuccess={onSuccess}
      rejectLabel={t.messages.confirmDelete.cancel}
      resolveLabel={t.messages.confirmDelete.confirm}
      type="warning"
      icon={faQuestion}
    />
  )
})
