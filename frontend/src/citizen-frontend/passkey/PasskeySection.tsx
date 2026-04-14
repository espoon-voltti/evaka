// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState
} from 'react'

import { useQueryClient } from '@tanstack/react-query'

import { useMutationResult, useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2, P } from 'lib-components/typography'

import { AuthContext } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import { PasskeyListItem } from './PasskeyListItem'
import {
  passkeysQuery,
  renamePasskeyMutation,
  revokePasskeyMutation
} from './queries'
import { usePasskeyAuth } from './usePasskeyAuth'

interface Props {
  autoEnroll?: boolean
}

export const PasskeySection = React.memo(function PasskeySection({
  autoEnroll = false
}: Props) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.passkeySection

  const { user, refreshAuthStatus } = useContext(AuthContext)
  const authUser = user.getOrElse(undefined) ?? null
  const currentCredentialId =
    (authUser as unknown as { passkeyCredentialId?: string | null })
      ?.passkeyCredentialId ?? null
  const isStrong = authUser?.authLevel === 'STRONG'

  const listResult = useQueryResult(passkeysQuery())
  const { mutateAsync: rename } = useMutationResult(renamePasskeyMutation)
  const { mutateAsync: revoke } = useMutationResult(revokePasskeyMutation)
  const { enroll } = usePasskeyAuth()
  const queryClient = useQueryClient()

  const [toRevoke, setToRevoke] = useState<string | null>(null)
  const autoEnrollDone = useRef(false)

  const onAdd = useCallback(async () => {
    if (isStrong) {
      const ok = await enroll()
      if (ok) {
        await queryClient.invalidateQueries({
          queryKey: passkeysQuery.prefix
        })
        await refreshAuthStatus()
      }
    } else {
      window.location.href = getStrongLoginUri(
        '/personal-details?enroll=1#passkeys'
      )
    }
  }, [enroll, isStrong, queryClient, refreshAuthStatus])

  useEffect(() => {
    if (!autoEnroll || autoEnrollDone.current || !isStrong) return
    autoEnrollDone.current = true
    void onAdd()
  }, [autoEnroll, isStrong, onAdd])

  return (
    <FixedSpaceColumn spacing="m" data-qa="passkey-section">
      <div id="passkeys">
        <H2>{t.title}</H2>
      </div>
      <P>{t.intro}</P>
      {listResult.isSuccess && listResult.value.length === 0 && (
        <P data-qa="passkey-empty">{t.empty}</P>
      )}
      {listResult.isSuccess &&
        listResult.value.map((c) => (
          <PasskeyListItem
            key={c.credentialId}
            credential={c}
            isCurrent={c.credentialId === currentCredentialId}
            onRename={async (label) => {
              await rename({ credentialId: c.credentialId, label })
            }}
            onRevoke={() => setToRevoke(c.credentialId)}
          />
        ))}
      <Button
        text={t.addButton}
        primary
        onClick={() => void onAdd()}
        data-qa="passkey-add"
      />
      {!isStrong && (
        <P data-qa="passkey-strong-required">{t.strongRequired}</P>
      )}
      {toRevoke && (
        <InfoModal
          title={t.revokeConfirmTitle}
          text={t.revokeConfirmText}
          resolve={{
            action: async () => {
              await revoke({ credentialId: toRevoke })
              if (toRevoke === currentCredentialId) {
                await refreshAuthStatus()
              }
              setToRevoke(null)
            },
            label: t.revoke
          }}
          reject={{
            action: () => setToRevoke(null),
            label: i18n.common.cancel
          }}
        />
      )}
    </FixedSpaceColumn>
  )
})
