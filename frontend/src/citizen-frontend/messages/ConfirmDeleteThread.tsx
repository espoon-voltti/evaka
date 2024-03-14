// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { UUID } from 'lib-common/types'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../localization'

import { archiveThreadMutation } from './queries'

export interface Props {
  threadId: UUID
  onClose: () => void
  onSuccess: () => void
}

export const ConfirmDeleteThread = React.memo(function ConfirmDeleteThread({
  threadId,
  onClose,
  onSuccess
}: Props) {
  const t = useTranslation()
  return (
    <MutateFormModal
      resolveMutation={archiveThreadMutation}
      resolveAction={() => ({ threadId })}
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
