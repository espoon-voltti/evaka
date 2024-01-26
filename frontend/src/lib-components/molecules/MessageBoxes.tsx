// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import { faInfo, faExclamation } from 'lib-icons'

import { fontWeights } from '../typography'
import { defaultMargins, Gap } from '../white-space'

interface MessageBoxContainerProps {
  color: string
  width: string
  thin?: boolean
  noMargin?: boolean
}

const MessageBoxContainer = styled.div<MessageBoxContainerProps>`
  width: ${(props) => props.width};
  margin: ${(props) => (props.thin || props.noMargin ? '0' : '24px 0')};
  padding: ${(props) =>
    props.thin ? `${defaultMargins.xs} ${defaultMargins.s}` : defaultMargins.s};
  border-style: solid;
  border-width: 1px;
  border-color: ${(props) => props.color};
  border-radius: 4px;

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
    font-weight: ${fontWeights.semibold};
  }
`

export interface MessageBoxProps {
  title?: string
  message?: string | React.ReactNode
  icon: IconProp
  color: string
  width?: string
  thin?: boolean
  noMargin?: boolean
  'data-qa'?: string
}

export const MessageBox = React.memo(function MessageBox({
  title,
  message,
  icon,
  color,
  width,
  thin,
  noMargin,
  ...props
}: MessageBoxProps) {
  if (!title && !message) {
    return null
  }

  return (
    <MessageBoxContainer
      color={color}
      width={width ?? 'fit-content'}
      thin={thin}
      noMargin={noMargin}
      data-qa={props['data-qa']}
    >
      <div className="message-container">
        <div className="icon-wrapper">
          <FontAwesomeIcon icon={icon} size="1x" color={color} inverse />
        </div>
        <div>
          {!!title && <span className="message-title">{title}</span>}
          {!!title && !!message && <Gap size={thin ? 'xxs' : 's'} />}
          {!!message &&
            (typeof message === 'string' ? <span>{message}</span> : message)}
        </div>
      </div>
    </MessageBoxContainer>
  )
})

interface InfoBoxProps {
  title?: string
  message?: string | React.ReactNode
  icon?: IconProp
  wide?: boolean
  thin?: boolean
  noMargin?: boolean
  'data-qa'?: string
}

export const InfoBox = React.memo(function InfoBox({
  title,
  message,
  icon,
  wide,
  thin,
  noMargin,
  ...props
}: InfoBoxProps) {
  const theme = useTheme()
  // without this hacking compiler gives an error because IconProp type is already super complex
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-explicit-any
  const notNullIcon: IconProp = (icon as any) ?? faInfo
  return (
    <MessageBox
      title={title}
      message={message}
      icon={notNullIcon}
      color={theme.colors.status.info}
      width={wide ? '100%' : 'fit-content'}
      thin={thin}
      noMargin={noMargin}
      data-qa={props['data-qa']}
    />
  )
})

interface AlertBoxProps {
  title?: string
  message?: string | React.ReactNode
  wide?: boolean
  thin?: boolean
  noMargin?: boolean
  'data-qa'?: string
}

export const AlertBox = React.memo(function AlertBox({
  title,
  message,
  wide,
  thin,
  noMargin,
  ...props
}: AlertBoxProps) {
  const { colors } = useTheme()
  return (
    <MessageBox
      title={title}
      message={message}
      icon={faExclamation}
      color={colors.status.warning}
      width={wide ? '100%' : 'fit-content'}
      thin={thin}
      noMargin={noMargin}
      data-qa={props['data-qa']}
    />
  )
})
