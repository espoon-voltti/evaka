// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useRef, useState } from 'react'
import styled, { useTheme } from 'styled-components'

import { appVersion } from 'lib-common/globals'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import { faInfo, faRedo } from 'lib-icons'

import InlineButton from '../atoms/buttons/InlineButton'

import Toast from './Toast'

// If the user closes the toast, remind every 5 minutes
const REMIND_INTERVAL = 5 * 60 * 1000

interface ReloadNotificationTranslations {
  title: string
  buttonText: string
  closeLabel: string
}

interface Props {
  apiVersion: string | undefined
  i18n: ReloadNotificationTranslations
}

export const ReloadNotification = React.memo(function ReloadNotification({
  apiVersion,
  i18n
}: Props) {
  const theme = useTheme()
  const [show, setShow] = useState(false)
  const timer = useRef<number>()

  const maybeShow = useCallback(() => {
    if (timer.current) {
      clearInterval(timer.current)
      timer.current = undefined
    }

    if (apiVersion !== undefined && apiVersion !== appVersion) {
      setShow(true)
    }
  }, [apiVersion])

  useEffect(maybeShow, [maybeShow])

  const close = useCallback(() => {
    if (timer.current) clearInterval(timer.current)
    timer.current = setTimeout(maybeShow, REMIND_INTERVAL)
    setShow(false)
  }, [maybeShow])

  return show ? (
    <Toast
      icon={faInfo}
      iconColor={theme.colors.main.m1}
      onClose={close}
      closeLabel={i18n.closeLabel}
    >
      <FixedSpaceColumn spacing="xs">
        <div>{i18n.title}</div>
        <div>
          <ReloadButton
            icon={faRedo}
            text={i18n.buttonText}
            onClick={() => {
              window.location.reload()
            }}
          />
        </div>
      </FixedSpaceColumn>
    </Toast>
  ) : null
})

export default React.memo(function ReloadNotificationBar(props: Props) {
  return (
    <OuterContainer>
      <ReloadNotification {...props} />
    </OuterContainer>
  )
})

export const OuterContainer = styled.div`
  position: sticky;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;

  > * {
    margin: ${defaultMargins.s};
    margin-bottom: 0;
  }
`

const ReloadButton = styled(InlineButton)`
  white-space: normal;
  text-align: left;
`
