// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { FormEvent, useCallback } from 'react'
import AsyncButton from '../../atoms/buttons/AsyncButton'
import Button from '../../atoms/buttons/Button'
import { Gap } from '../../white-space'
import BaseModal, { ModalBaseProps, ModalButtons } from './BaseModal'

type FormModalProps = ModalBaseProps & {
  resolveAction: () => void
  resolveLabel: string
  resolveDisabled?: boolean
  rejectAction: () => void
  rejectLabel: string
}

export default React.memo(function FormModal({
  children,
  resolveAction,
  resolveLabel,
  resolveDisabled,
  rejectAction,
  rejectLabel,
  ...props
}: FormModalProps) {
  const onSubmit = useCallback(
    (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault()
      if (!resolveDisabled) resolveAction()
    },
    [resolveAction, resolveDisabled]
  )

  return (
    <BaseModal {...props} close={rejectAction}>
      <form onSubmit={onSubmit}>{children}</form>
      <ModalButtons $singleButton={!('reject' in props)}>
        <Button
          onClick={rejectAction}
          data-qa="modal-cancelBtn"
          text={rejectLabel}
        />
        <Gap horizontal size={'xs'} />
        <Button
          primary
          type="submit"
          data-qa="modal-okBtn"
          onClick={resolveAction}
          disabled={resolveDisabled}
          text={resolveLabel}
        />
      </ModalButtons>
    </BaseModal>
  )
})

type AsyncModalProps = ModalBaseProps & {
  resolveAction: (
    cancel: () => Promise<void>
  ) => Promise<void | { isFailure: boolean }>
  resolveLabel: string
  resolveDisabled?: boolean
  onSuccess: () => void
  rejectAction: () => void
  rejectLabel: string
}

export const AsyncFormModal = React.memo(function AsyncFormModal({
  children,
  resolveAction,
  resolveLabel,
  resolveDisabled,
  onSuccess,
  rejectAction,
  rejectLabel,
  ...props
}: AsyncModalProps) {
  return (
    <BaseModal {...props} close={rejectAction}>
      {children}
      <ModalButtons $singleButton={!('reject' in props)}>
        <Button
          onClick={rejectAction}
          data-qa="modal-cancelBtn"
          text={rejectLabel}
        />
        <Gap horizontal size={'xs'} />
        <AsyncButton
          primary
          text={resolveLabel}
          disabled={resolveDisabled}
          onClick={resolveAction}
          onSuccess={onSuccess}
          data-qa="modal-okBtn"
        />
      </ModalButtons>
    </BaseModal>
  )
})
