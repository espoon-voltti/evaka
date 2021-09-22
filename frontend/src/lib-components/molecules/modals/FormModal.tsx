// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Button from '../../atoms/buttons/Button'
import AsyncButton from '../../atoms/buttons/AsyncButton'
import { Gap } from '../../white-space'
import BaseModal, { ModalBaseProps, ModalButtons } from './BaseModal'

type FormModalProps = ModalBaseProps & {
  resolve: {
    action: () => void
    label: string
    disabled?: boolean
  }
  reject: {
    action: () => void
    label: string
  }
}

export default React.memo(function FormModal({
  children,
  resolve,
  reject,
  ...props
}: FormModalProps) {
  return (
    <BaseModal {...props} close={reject?.action}>
      <form
        onSubmit={(event) => {
          event.preventDefault()
          if (!resolve.disabled) resolve.action()
        }}
      >
        {children}
      </form>
      <ModalButtons $singleButton={!('reject' in props)}>
        <Button
          onClick={reject.action}
          data-qa="modal-cancelBtn"
          text={reject.label}
        />
        <Gap horizontal size={'xs'} />
        <Button
          primary
          data-qa="modal-okBtn"
          onClick={resolve.action}
          disabled={resolve.disabled}
          text={resolve.label}
        />
      </ModalButtons>
    </BaseModal>
  )
})

type AsyncModalProps = ModalBaseProps & {
  resolve: {
    //eslint-disable-next-line @typescript-eslint/no-explicit-any
    action: () => Promise<any>
    label: string
    disabled?: boolean
    onSuccess: () => void
  }
  reject: {
    action: () => void
    label: string
  }
}

export const AsyncFormModal = React.memo(function AsyncFormModal({
  children,
  resolve,
  reject,
  ...props
}: AsyncModalProps) {
  return (
    <BaseModal {...props} close={reject.action}>
      {children}
      <ModalButtons $singleButton={!('reject' in props)}>
        <Button
          onClick={reject.action}
          data-qa="modal-cancelBtn"
          text={reject.label}
        />
        <Gap horizontal size={'xs'} />
        <AsyncButton
          primary
          text={resolve.label}
          disabled={resolve.disabled}
          onClick={resolve.action}
          onSuccess={resolve.onSuccess}
          data-qa="modal-okBtn"
        />
      </ModalButtons>
    </BaseModal>
  )
})
