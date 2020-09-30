// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faInfo } from 'icon-set'
import { EspooColours } from '~utils/colours'
import React from 'react'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'

interface MessageBoxContainerProps {
  color: string
  width: string
  thin?: boolean
}

const MessageBoxContainer = styled.div<MessageBoxContainerProps>`
  width: ${(props) => props.width};
  margin: ${(props) => (props.thin ? '8px 0' : '24px 0')};
  padding: ${(props) => (props.thin ? '8px 20px' : '20px')};
  border-style: solid;
  border-width: 1px;
  border-color: ${(props) => props.color};
  border-radius: 4px;

  .message-container {
    display: flex;
  }

  .icon-wrapper {
    margin-top: 4px;
    margin-bottom: 4px;
    margin-right: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    height: fit-content;

    &.circle {
      width: 30px;
      min-width: 30px;
      height: 30px;
      background: ${(props) => props.color};
      border-radius: 100%;
    }
  }

  .message-title {
    font-weight: bold;
  }
`

interface MessageBoxProps {
  title?: string
  message?: string
  icon: IconProp
  color: string
  width?: string
  thin?: boolean
  circleIcon: boolean
}

export function MessageBox({
  title,
  message,
  icon,
  color,
  width,
  thin,
  circleIcon
}: MessageBoxProps) {
  return (
    <MessageBoxContainer
      color={color}
      width={width ?? 'fit-content'}
      thin={thin}
    >
      <div className="message-container">
        <div className={`icon-wrapper ${circleIcon ? 'circle' : ''}`}>
          <FontAwesomeIcon
            icon={icon}
            size={circleIcon || thin ? 'lg' : '2x'}
            color={color}
            inverse={circleIcon}
          />
        </div>
        <div>
          {title && <p className="message-title">{title}</p>}
          {message && <p>{message}</p>}
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
}

export function InfoBox({ title, message, icon, wide, thin }: InfoBoxProps) {
  // without this hacking compiler gives an error because IconProp type is already super complex
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-explicit-any
  const notNullIcon: IconProp = (icon as any) ?? faInfo
  return (
    <MessageBox
      title={title}
      message={message}
      icon={notNullIcon}
      circleIcon={true}
      color={EspooColours.water}
      width={wide ? '100%' : 'fit-content'}
      thin={thin}
    />
  )
}

interface AlertBoxProps {
  title?: string
  message?: string
  wide?: boolean
  thin?: boolean
}

export function AlertBox({ title, message, wide, thin }: AlertBoxProps) {
  return (
    <MessageBox
      title={title}
      message={message}
      icon={faExclamationTriangle}
      circleIcon={false}
      color={EspooColours.orange}
      width={wide ? '100%' : 'fit-content'}
      thin={thin}
    />
  )
}
