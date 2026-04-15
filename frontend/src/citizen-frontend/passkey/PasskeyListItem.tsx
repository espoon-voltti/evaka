// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import InputField from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label, P } from 'lib-components/typography'

import { useTranslation } from '../localization'

import type { CitizenPasskeyCredentialSummary } from './queries'

interface Props {
  credential: CitizenPasskeyCredentialSummary
  isCurrent: boolean
  onRename: (label: string) => Promise<void>
  onRevoke: () => void
}

export const PasskeyListItem = React.memo(function PasskeyListItem({
  credential,
  isCurrent,
  onRename,
  onRevoke
}: Props) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.passkeySection
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(credential.label)

  return (
    <FixedSpaceColumn
      $spacing="xs"
      data-qa={`passkey-${credential.credentialId}`}
    >
      <FixedSpaceRow $alignItems="center" $spacing="s">
        {editing ? (
          <>
            <InputField
              value={draft}
              onChange={setDraft}
              data-qa="passkey-label-input"
            />
            <Button
              text={i18n.common.save}
              primary
              onClick={async () => {
                const trimmed = draft.trim()
                if (trimmed.length > 0 && trimmed.length <= 80) {
                  await onRename(trimmed)
                  setEditing(false)
                }
              }}
            />
            <Button
              text={i18n.common.cancel}
              onClick={() => {
                setDraft(credential.label)
                setEditing(false)
              }}
            />
          </>
        ) : (
          <>
            <Label>{credential.label}</Label>
            {isCurrent && (
              <span data-qa="passkey-this-device">{t.thisDevice}</span>
            )}
            <Button text={t.rename} onClick={() => setEditing(true)} />
          </>
        )}
      </FixedSpaceRow>
      {!!credential.deviceHint && <P>{credential.deviceHint}</P>}
      <P>
        {t.createdAt} {credential.createdAt.slice(0, 10)}
      </P>
      <P>
        {t.lastUsedAt}{' '}
        {credential.lastUsedAt
          ? credential.lastUsedAt.slice(0, 10)
          : t.neverUsed}
      </P>
      <Button text={t.revoke} onClick={onRevoke} />
    </FixedSpaceColumn>
  )
})
