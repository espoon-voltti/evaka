// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'

import { UUID } from 'lib-common/types'
import { desktopMin } from 'lib-components/breakpoints'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../localization'

import { archiveThread } from './api'

export const MessageContainer = styled.div`
  background-color: ${colors.grayscale.g0};
  padding: ${defaultMargins.s};

  @media (min-width: ${desktopMin}) {
    padding: ${defaultMargins.L};
  }

  margin-bottom: ${defaultMargins.s};

  h2 {
    margin: 0;
  }
`

export interface ConfirmDeleteThreadProps {
  threadId: UUID
  onClose: () => void
  onSuccess: () => void
}

export const ConfirmDeleteThread = React.memo(function ConfirmDeleteThread({
  threadId,
  onClose,
  onSuccess
}: ConfirmDeleteThreadProps) {
  const t = useTranslation()
  const onArchive = useCallback(() => archiveThread(threadId), [threadId])
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
