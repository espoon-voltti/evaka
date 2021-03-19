// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faInfo, faExclamation } from 'lib-icons'
import { accentColors } from '../colors'
import React from 'react'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { defaultMargins, Gap } from '../white-space'

interface MessageBoxContainerProps {
  color: string
  width: string
  thin?: boolean
}

const MessageBoxContainer = styled.div<MessageBoxContainerProps>`
  width: ${(props) => props.width};
  margin: ${(props) => (props.thin ? '0' : '24px 0')};
  padding: ${(props) =>
    props.thin
      ? `${defaultMargins.xxs} ${defaultMargins.s}`
      : defaultMargins.s};
  border-style: solid;
  border-width: 1px;
  border-color: ${(props) => props.color};
  border-radius: ${(props) => (props.thin ? '0' : '4px')};

  .message-container {
    display: flex;
    align-items: flex-start;
  }

  .icon-wrapper {
    margin-right: ${defaultMargins.s};
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    min-width: 24px;
    height: 24px;
    background: ${(props) => props.color};
    border-radius: 100%;
  }

  .message-title {
    font-weight: bold;
  }
`

export interface MessageBoxProps {
  title?: string
  message?: string | React.ReactNode
  icon: IconProp
  color: string
  width?: string
  thin?: boolean
  'data-qa'?: string
}

export function MessageBox({
  title,
  message,
  icon,
  color,
  width,
  thin,
  ...props
}: MessageBoxProps) {
  return (
    <MessageBoxContainer
      color={color}
      width={width ?? 'fit-content'}
      thin={thin}
      data-qa={props['data-qa']}
    >
      <div className="message-container">
        <div className="icon-wrapper">
          <FontAwesomeIcon icon={icon} size="1x" color={color} inverse />
        </div>
        <div>
          {title && <span className="message-title">{title}</span>}
          {title && message && <Gap size="s" />}
          {message &&
            (typeof message === 'string' ? <span>{message}</span> : message)}
        </div>
      </div>
    </MessageBoxContainer>
  )
}

interface InfoBoxProps {
  title?: string
  message?: string
  icon?: IconProp
  wide?: boolean
  thin?: boolean
  'data-qa'?: string
}

export function InfoBox({
  title,
  message,
  icon,
  wide,
  thin,
  ...props
}: InfoBoxProps) {
  // without this hacking compiler gives an error because IconProp type is already super complex
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-explicit-any
  const notNullIcon: IconProp = (icon as any) ?? faInfo
  return (
    <MessageBox
      title={title}
      message={message}
      icon={notNullIcon}
      color={accentColors.water}
      width={wide ? '100%' : 'fit-content'}
      thin={thin}
      data-qa={props['data-qa']}
    />
  )
}

interface AlertBoxProps {
  title?: string
  message?: string | React.ReactNode
  wide?: boolean
  thin?: boolean
  'data-qa'?: string
}

export function AlertBox({
  title,
  message,
  wide,
  thin,
  ...props
}: AlertBoxProps) {
  return (
    <MessageBox
      title={title}
      message={message}
      icon={faExclamation}
      color={accentColors.orange}
      width={wide ? '100%' : 'fit-content'}
      thin={thin}
      data-qa={props['data-qa']}
    />
  )
}
