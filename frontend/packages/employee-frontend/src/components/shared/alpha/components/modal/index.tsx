// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { Title, Button } from '../../elements'

interface Props {
  title: React.ReactNode
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  icon?: any
  type?: ModalType
  text?: React.ReactNode
  className?: string
  dataQa?: string
  content?: React.ReactNode
  stack?: number
  onConfirm: () => unknown
  onCancel?: () => unknown
  confirmButtonLabel: React.ReactNode
  cancelButtonLabel: React.ReactNode
  confirmDisabled?: boolean
}

export type ModalType = 'action' | 'success' | 'danger' | 'error' | 'warning'

export const Modal = ({
  className,
  dataQa,
  icon,
  title,
  type,
  text,
  content,
  stack,
  onConfirm,
  onCancel,
  confirmButtonLabel,
  cancelButtonLabel,
  confirmDisabled
}: Props) => (
  <div
    style={{ zIndex: stack }}
    className={classNames(
      'modal-wrapper modal is-active',
      {
        [`is-${type ?? ''}`]: type
      },
      className
    )}
    data-qa={dataQa}
  >
    <div className="modal-background" />
    <div className="modal-container" data-qa="modal">
      <div className="modal-content">
        <div className="modal-icon">
          {icon && (
            <FontAwesomeIcon
              icon={
                // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
                icon
              }
            />
          )}
        </div>
        {title && (
          <Title
            size={2}
            dataQa="title"
            className="modal-title has-text-centered"
          >
            {title}
          </Title>
        )}
        {text && (
          <div className="text has-text-centered" data-qa="text">
            {text}
          </div>
        )}
        {content}
      </div>
      <div className="modal-buttons">
        {onCancel && (
          <Button plain={true} onClick={onCancel} dataQa="modal-cancelBtn">
            {cancelButtonLabel}
          </Button>
        )}
        <Button
          plain={true}
          onClick={onConfirm}
          disabled={confirmDisabled}
          dataQa="modal-okBtn"
        >
          {confirmButtonLabel}
        </Button>
      </div>
    </div>
  </div>
)
