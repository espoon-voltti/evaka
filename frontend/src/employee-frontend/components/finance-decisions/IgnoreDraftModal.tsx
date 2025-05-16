// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import type {
  FeeDecisionId,
  VoucherValueDecisionId
} from 'lib-common/generated/api-types/shared'
import type { MutationDescription } from 'lib-common/query'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

function IgnoreDraftModal_<
  DecisionId extends FeeDecisionId | VoucherValueDecisionId
>({
  decisionIds,
  mutation,
  onCancel,
  onSuccess
}: {
  decisionIds: DecisionId[]
  mutation: MutationDescription<{ body: DecisionId[] }, void>
  onCancel: () => void
  onSuccess: () => void
}) {
  const { i18n } = useTranslation()
  const [confirm, setConfirm] = useState(false)

  return (
    <MutateFormModal
      title={i18n.ignoreDraftModal.title}
      resolveMutation={mutation}
      resolveAction={() => ({ body: decisionIds })}
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
    </MutateFormModal>
  )
}

export const IgnoreDraftModal = React.memo(
  IgnoreDraftModal_
) as typeof IgnoreDraftModal_
