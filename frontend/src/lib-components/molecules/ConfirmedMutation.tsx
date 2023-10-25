// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { cancelMutation, MutationDescription } from 'lib-common/query'

import Button from '../atoms/buttons/Button'
import { useTranslations } from '../i18n'
import { BaseProps } from '../utils'

import { MutateFormModal } from './modals/FormModal'

export interface ConfirmedMutationProps<Arg, Data> extends BaseProps {
  buttonText: string
  confirmationTitle: string
  mutation: MutationDescription<Arg, Data>
  onClick: () => Arg | typeof cancelMutation
  primary?: boolean
  disabled?: boolean
  confirmationText?: React.ReactNode
  confirmLabel?: string
  cancelLabel?: string
  onSuccess?: (value: Data) => void
}

function ConfirmedMutation_<Arg, Data>({
  buttonText,
  primary,
  disabled,
  confirmationTitle,
  confirmationText,
  mutation,
  onClick,
  confirmLabel,
  onSuccess,
  cancelLabel,
  'data-qa': dataQa,
  className
}: ConfirmedMutationProps<Arg, Data>) {
  const i18n = useTranslations()
  const [confirming, setConfirming] = useState(false)
  return (
    <div className={className}>
      <Button
        text={buttonText}
        onClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
          setConfirming(true)
        }}
        primary={primary}
        disabled={disabled}
        data-qa={dataQa}
        type="button"
      />
      {confirming && (
        <MutateFormModal
          title={confirmationTitle}
          text={confirmationText}
          resolveMutation={mutation}
          resolveAction={onClick}
          resolveLabel={confirmLabel ?? i18n.common.confirm}
          onSuccess={(value) => {
            setConfirming(false)
            if (onSuccess) onSuccess(value)
          }}
          rejectAction={() => setConfirming(false)}
          rejectLabel={cancelLabel ?? i18n.common.cancel}
        />
      )}
    </div>
  )
}

export const ConfirmedMutation = React.memo(
  ConfirmedMutation_
) as typeof ConfirmedMutation_
