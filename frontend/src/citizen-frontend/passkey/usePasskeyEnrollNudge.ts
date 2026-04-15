// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faInfo } from '@fortawesome/free-solid-svg-icons'
import { useContext, useEffect, useEffectEvent } from 'react'

import { useQueryResult } from 'lib-common/query'
import { NotificationsContext } from 'lib-components/Notifications'
import { colors } from 'lib-customizations/common'

import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import { passkeysQuery } from './queries'
import { useWebAuthnSupported } from './usePasskeyAuth'

const NUDGE_ID = 'passkey-enroll-nudge'
const COOKIE_NAME_PREFIX = 'evaka-passkey-enroll-dismissed-'
const DISMISS_DAYS = 30

function isPasskeyEnrollNudgeDismissed(userId: string): boolean {
  const cookieName = `${COOKIE_NAME_PREFIX}${userId}`
  return document.cookie
    .split('; ')
    .some((cookie) => cookie.startsWith(`${cookieName}=`))
}

function dismissPasskeyEnrollNudge(userId: string): void {
  const cookieName = `${COOKIE_NAME_PREFIX}${userId}`
  const expires = new Date()
  expires.setDate(expires.getDate() + DISMISS_DAYS)
  document.cookie = `${cookieName}=true; expires=${expires.toUTCString()}; path=/; SameSite=Strict`
}

export function usePasskeyEnrollNudge(): void {
  const i18n = useTranslation()
  const t = i18n.personalDetails.passkeySection.enrollNudge

  const user = useUser()
  const { addNotification, removeNotification } =
    useContext(NotificationsContext)
  const supported = useWebAuthnSupported()

  const listResult = useQueryResult(passkeysQuery(), {
    enabled: user !== undefined
  })

  const onDataLoaded = useEffectEvent(
    (userId: string, authLevel: string, hasPasskeys: boolean) => {
      if (!supported) {
        removeNotification(NUDGE_ID)
        return
      }
      if (isPasskeyEnrollNudgeDismissed(userId)) {
        removeNotification(NUDGE_ID)
        return
      }
      if (hasPasskeys) {
        removeNotification(NUDGE_ID)
        return
      }

      const isStrong = authLevel === 'STRONG'
      const targetUrl = '/personal-details?enroll=1#passkeys'

      addNotification(
        {
          icon: faInfo,
          iconColor: colors.main.m2,
          children: t.body,
          onClose: () => dismissPasskeyEnrollNudge(userId),
          onClick: () => {
            window.location.href = isStrong
              ? targetUrl
              : getStrongLoginUri(targetUrl)
          },
          dataQa: NUDGE_ID
        },
        NUDGE_ID
      )
    }
  )

  useEffect(() => {
    if (listResult.isSuccess && user) {
      onDataLoaded(user.id, user.authLevel, listResult.value.length > 0)
    }
  }, [listResult, user])
}
