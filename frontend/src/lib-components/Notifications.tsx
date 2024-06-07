// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { onlineManager } from '@tanstack/react-query'
import omit from 'lodash/omit'
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react'
import styled, { useTheme } from 'styled-components'

import { appVersion } from 'lib-common/globals'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { faExclamation, faInfo, faRedo } from 'lib-icons'

import { Button } from './atoms/buttons/Button'
import { useTranslations } from './i18n'
import { FixedSpaceColumn } from './layout/flex-helpers'
import Toast from './molecules/Toast'
import { defaultMargins } from './white-space'

interface NotificationsState {
  notifications: Record<string, Notification>
  addNotification: (n: Notification, customId?: string) => void
  removeNotification: (id: string) => void
}

interface Notification {
  icon: IconDefinition
  iconColor: string
  children: React.ReactNode | React.ReactNode[]
  onClick?: (close: () => void) => void
  onClose?: () => void
  dataQa?: string
}

export const NotificationsContext = createContext<NotificationsState>({
  notifications: {},
  addNotification: () => undefined,
  removeNotification: () => undefined
})

let idCounter = 1

export const NotificationsContextProvider = React.memo(
  function NotificationsContextProvider({
    children
  }: {
    children: React.ReactNode | React.ReactNode[]
  }) {
    const [notifications, setNotifications] = useState({})
    const addNotification = useCallback(
      (notification: Notification, customId?: string) =>
        setNotifications((notifications) => ({
          ...notifications,
          [customId ?? (idCounter++).toString(10)]: notification
        })),
      []
    )

    const removeNotification = useCallback((id: string) => {
      setNotifications((notifications) => omit(notifications, id))
    }, [])

    const value = useMemo(
      () => ({
        notifications,
        addNotification,
        removeNotification
      }),
      [addNotification, notifications, removeNotification]
    )

    return (
      <NotificationsContext.Provider value={value}>
        {children}
      </NotificationsContext.Provider>
    )
  }
)

export const Notifications = React.memo(function Notifications({
  apiVersion
}: {
  apiVersion: string | undefined
}) {
  const i18n = useTranslations()
  const { notifications, addNotification, removeNotification } =
    useContext(NotificationsContext)
  useOfflineNotification(
    i18n.offlineNotification,
    addNotification,
    removeNotification
  )
  useReloadNotification(
    apiVersion,
    i18n.reloadNotification.title,
    i18n.reloadNotification.buttonText,
    addNotification
  )
  return (
    <OuterContainer>
      <ColumnContainer spacing="s" alignItems="flex-end">
        {Object.entries(notifications).map(
          ([id, { children, onClose, onClick, dataQa, ...props }]) => (
            <Toast
              key={id}
              {...props}
              closeLabel={i18n.notifications.close}
              onClick={() => onClick?.(() => removeNotification(id))}
              onClose={() => {
                removeNotification(id)
                onClose?.()
              }}
              data-qa={dataQa}
            >
              {children}
            </Toast>
          )
        )}
      </ColumnContainer>
    </OuterContainer>
  )
})

const OuterContainer = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 0;
`

const ColumnContainer = styled(FixedSpaceColumn)`
  margin: ${defaultMargins.s};
  pointer-events: none;
`

// If the user closes the toast, remind every 5 minutes
const REMIND_INTERVAL = 5 * 60 * 1000

function useOfflineNotification(
  title: string,
  addNotification: (n: Notification, id: string) => void,
  removeNotification: (id: string) => void
) {
  const theme = useTheme()
  const [onlineStatus, setOnlineStatus] = useState(() => ({
    isOnline: onlineManager.isOnline(),
    since: HelsinkiDateTime.now()
  }))

  useEffect(() => {
    const updateNotification = () => {
      const wasOnline = onlineStatus.isOnline
      const isOnline = onlineManager.isOnline()
      if (wasOnline === isOnline) return

      const now = HelsinkiDateTime.now()
      if (isOnline) {
        removeNotification('offline')
      } else {
        addNotification(
          {
            icon: faExclamation,
            iconColor: theme.colors.status.warning,
            children: title
          },
          'offline'
        )
      }
      setOnlineStatus({ isOnline, since: now })
    }

    const unsubscribe = onlineManager.subscribe(updateNotification)
    const interval = setInterval(updateNotification, REMIND_INTERVAL)
    return () => {
      unsubscribe()
      clearInterval(interval)
    }
  }, [
    addNotification,
    onlineStatus,
    removeNotification,
    theme.colors.status.warning,
    title
  ])
}

function useReloadNotification(
  apiVersion: string | undefined,
  title: string,
  buttonText: string,
  addNotification: (n: Notification) => void
) {
  const theme = useTheme()
  const timer = useRef<number>()
  const [show, setShow] = useState(false)

  const maybeShow = useCallback(() => {
    if (apiVersion !== undefined && apiVersion !== appVersion) {
      setShow(true)
    }
  }, [apiVersion])

  const onClose = useCallback(() => {
    if (timer.current) clearInterval(timer.current)
    timer.current = window.setTimeout(maybeShow, REMIND_INTERVAL)
  }, [maybeShow])

  useEffect(() => {
    maybeShow()
    return () => (timer.current ? clearInterval(timer.current) : undefined)
  }, [maybeShow])

  useEffect(() => {
    if (show) {
      addNotification({
        icon: faInfo,
        iconColor: theme.colors.main.m1,
        children: <ReloadNotification title={title} buttonText={buttonText} />,
        onClose
      })
      setShow(false)
    }
  }, [addNotification, buttonText, onClose, show, theme.colors.main.m1, title])
}

const ReloadNotification = React.memo(function ReloadNotification({
  title,
  buttonText
}: {
  title: string
  buttonText: string
}) {
  const reload = () => window.location.reload()

  return (
    <FixedSpaceColumn spacing="xs" onClick={reload}>
      <div>{title}</div>
      <div>
        <ReloadButton
          appearance="inline"
          icon={faRedo}
          text={buttonText}
          onClick={reload}
        />
      </div>
    </FixedSpaceColumn>
  )
})

const ReloadButton = styled(Button)`
  white-space: normal;
  text-align: left;
`
