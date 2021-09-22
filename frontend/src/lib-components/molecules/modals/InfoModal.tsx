// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Gap } from 'lib-components/white-space'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import BaseModal, { ModalBaseProps, ModalButtons } from './BaseModal'

type Props = Omit<ModalBaseProps, 'mobileFullScreen'> &
  (
    | {
        resolve: {
          action: () => void
          label: string
          disabled?: boolean
        }
        reject?: {
          action: () => void
          label: string
        }
      }
    | { close: () => void }
  )

function InfoModal({ children, ...props }: Props) {
  return (
    <BaseModal
      {...props}
      close={
        'close' in props
          ? props.close
          : props.reject
          ? props.reject.action
          : props.resolve.action
      }
      mobileFullScreen={false}
    >
      {children}
      {'resolve' in props ? (
        <ModalButtons $singleButton={!props.reject}>
          {props.reject && (
            <>
              <InlineButton
                onClick={props.reject.action}
                data-qa="modal-cancelBtn"
                text={props.reject.label}
              />
              <Gap horizontal size={'xs'} />
            </>
          )}
          <InlineButton
            data-qa="modal-okBtn"
            onClick={props.resolve.action}
            disabled={props.resolve.disabled}
            text={props.resolve.label}
          />
        </ModalButtons>
      ) : (
        <Gap size="X3L" />
      )}
    </BaseModal>
  )
}

export default InfoModal
