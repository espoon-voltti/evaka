// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faTrash } from 'lib-icons'

import { useTranslation } from '../localization'

import type { CitizenPasskeyCredentialSummary } from './queries'

interface Props {
  credential: CitizenPasskeyCredentialSummary
  isCurrent: boolean
  onRevoke: () => void
}

export const PasskeyListItem = React.memo(function PasskeyListItem({
  credential,
  isCurrent,
  onRevoke
}: Props) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.passkeySection

  const lastUsed = credential.lastUsedAt
    ? `${t.lastUsedAt} ${credential.lastUsedAt.slice(0, 10)}`
    : `${t.lastUsedAt} ${t.neverUsed}`
  const secondaryParts = [credential.deviceHint, lastUsed].filter(
    (s): s is string => !!s
  )

  return (
    <Item data-qa={`passkey-${credential.credentialId}`}>
      <Info>
        <Title>
          <Name>{credential.label}</Name>
          {isCurrent && (
            <ThisDeviceBadge data-qa="passkey-this-device">
              {t.thisDevice}
            </ThisDeviceBadge>
          )}
        </Title>
        {secondaryParts.length > 0 && (
          <Secondary>{secondaryParts.join(' · ')}</Secondary>
        )}
      </Info>
      <IconOnlyButton
        icon={faTrash}
        color="gray"
        onClick={onRevoke}
        aria-label={t.revoke}
        data-qa="passkey-revoke"
      />
    </Item>
  )
})

const Item = styled.li`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.s};
  padding: ${defaultMargins.xs} 0;
`

const Info = styled.div`
  flex: 1;
  min-width: 0;
`

const Title = styled.div`
  display: flex;
  align-items: baseline;
  gap: ${defaultMargins.xs};
  flex-wrap: wrap;
`

const Name = styled.span`
  font-weight: ${fontWeights.semibold};
  overflow-wrap: anywhere;
`

const ThisDeviceBadge = styled.span`
  font-size: 0.75rem;
  font-weight: ${fontWeights.semibold};
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 2px ${defaultMargins.xs};
  border-radius: 999px;
  background: ${(p) => p.theme.colors.main.m4};
  color: ${(p) => p.theme.colors.main.m1};
`

const Secondary = styled.div`
  font-size: 0.875rem;
  color: ${(p) => p.theme.colors.grayscale.g70};
  margin-top: 2px;
  overflow-wrap: anywhere;
`
