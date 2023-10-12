// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import Checkbox from 'lib-components/atoms/form/Checkbox'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

export const IgnoreDraftModal = React.memo(function IgnoreDraftModal({
  onConfirm,
  onCancel,
  onSuccess
}: {
  onConfirm: () => void
  onCancel: () => void
  onSuccess: () => void
}) {
  const { i18n } = useTranslation()
  const [confirm, setConfirm] = useState(false)

  return (
    <AsyncFormModal
      title={i18n.ignoreDraftModal.title}
      resolveAction={onConfirm}
      resolveLabel={i18n.common.confirm}
      onSuccess={onSuccess}
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!confirm}
    >
      {i18n.ignoreDraftModal.content}
      <Gap />
      <Checkbox
        label={i18n.ignoreDraftModal.confirm}
        checked={confirm}
        onChange={setConfirm}
        data-qa="confirm-checkbox"
      />
    </AsyncFormModal>
  )
})
