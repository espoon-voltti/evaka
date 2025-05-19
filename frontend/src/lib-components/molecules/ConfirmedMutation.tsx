// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import type { MutationDescription } from 'lib-common/query'
import { type cancelMutation } from 'lib-common/query'

import { Button } from '../atoms/buttons/Button'
import { IconOnlyButton } from '../atoms/buttons/IconOnlyButton'
import { LegacyButton } from '../atoms/buttons/LegacyButton'
import { useTranslations } from '../i18n'
import type { BaseProps } from '../utils'

import type { ModalType } from './modals/BaseModal'
import { MutateFormModal } from './modals/FormModal'

type LargeButtonProps = {
  buttonStyle?: 'BUTTON'
  buttonText: string
  primary?: boolean
}

type InlineButtonProps = {
  buttonStyle: 'INLINE'
  buttonText: string
  icon?: IconDefinition
}

type IconButtonProps = {
  buttonStyle: 'ICON'
  buttonAltText: string
  icon: IconDefinition
}

export type ConfirmedMutationProps<Arg, Data> = BaseProps & {
  confirmationTitle: string
  mutation: MutationDescription<Arg, Data>
  onClick: () => Arg | typeof cancelMutation
  disabled?: boolean
  confirmationText?: React.ReactNode
  confirmLabel?: string
  cancelLabel?: string
  onSuccess?: (value: Data) => void
  modalIcon?: IconDefinition
  modalType?: ModalType
  'data-qa-modal'?: string
} & (LargeButtonProps | InlineButtonProps | IconButtonProps)

function ConfirmedMutation_<Arg, Data>(
  props: ConfirmedMutationProps<Arg, Data>
) {
  const {
    disabled,
    confirmationTitle,
    confirmationText,
    mutation,
    onClick,
    confirmLabel,
    onSuccess,
    cancelLabel,
    modalIcon,
    modalType,
    'data-qa': dataQa,
    'data-qa-modal': dataQaModal,
    className
  } = props
  const i18n = useTranslations()
  const [confirming, setConfirming] = useState(false)
  const openConfirmation = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setConfirming(true)
  }
  return (
    <div className={className}>
      {(props.buttonStyle === undefined || props.buttonStyle === 'BUTTON') && (
        <LegacyButton
          text={props.buttonText}
          onClick={openConfirmation}
          primary={props.primary}
          disabled={disabled}
          data-qa={dataQa}
          type="button"
        />
      )}
      {props.buttonStyle === 'INLINE' && (
        <Button
          appearance="inline"
          text={props.buttonText}
          icon={props.icon}
          onClick={openConfirmation}
          disabled={disabled}
          data-qa={dataQa}
        />
      )}
      {props.buttonStyle === 'ICON' && (
        <IconOnlyButton
          icon={props.icon}
          aria-label={props.buttonAltText}
          onClick={openConfirmation}
          disabled={disabled}
          data-qa={dataQa}
        />
      )}
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
          icon={modalIcon}
          type={modalType}
          data-qa={dataQaModal}
        />
      )}
    </div>
  )
}

export const ConfirmedMutation = React.memo(
  ConfirmedMutation_
) as typeof ConfirmedMutation_
