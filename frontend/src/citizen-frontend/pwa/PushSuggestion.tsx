// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { Success } from 'lib-common/api'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3, P } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../localization'

import { PushStatusChip } from './PushStatusChip'
import { Banner, SuggestionNote, useSuggestionStage } from './SuggestionBanner'
import { usePushAvailability, useSubscribeToPush } from './pushNotifications'
import { sendTestPushNotificationMutation } from './queries'

export const PushSuggestion = React.memo(function PushSuggestion() {
  const i18n = useTranslation()
  const t = i18n.pwa.pushSuggestion
  const availability = usePushAvailability()
  const subscribe = useSubscribeToPush()
  const { stage, showNote, dismiss } = useSuggestionStage('push')
  const [enableClicked, setEnableClicked] = useState(false)

  if (stage === 'hidden') return null
  if (availability.kind === 'unavailable') return null
  if (availability.kind === 'subscribed' && !enableClicked) return null

  if (stage === 'note' || availability.kind === 'blocked') {
    return (
      <SuggestionNote
        text={t.dismissedNote}
        onClick={dismiss}
        data-qa="push-suggestion-note"
      />
    )
  }

  const status = availability.kind === 'subscribed' ? 'enabled' : 'disabled'

  return (
    <Banner data-qa="push-suggestion" data-status={status}>
      <Card>
        <TitleRow>
          <H3 $smaller $noMargin>
            {t.title}
          </H3>
          <PushStatusChip status={status} data-qa="push-suggestion-status" />
        </TitleRow>
        {availability.kind === 'subscribable' && (
          <>
            <FixedSpaceColumn $spacing="m">
              <P $noMargin>{t.text}</P>
              <P $noMargin>{t.settingsHint}</P>
            </FixedSpaceColumn>
            <Actions>
              <AsyncButton
                primary
                text={t.enable}
                hideSuccess
                onClick={() => {
                  setEnableClicked(true)
                  return subscribe().then(() => Success.of())
                }}
                onSuccess={() => undefined}
                data-qa="push-suggestion-enable"
              />
              <Button
                appearance="inline"
                text={t.later}
                onClick={showNote}
                data-qa="push-suggestion-later"
              />
            </Actions>
          </>
        )}
        {availability.kind === 'subscribed' && (
          <>
            <P $noMargin>{t.enabledText}</P>
            <Actions>
              <MutateButton
                appearance="inline"
                text={t.sendTest}
                mutation={sendTestPushNotificationMutation}
                onClick={() => undefined}
                data-qa="push-suggestion-test"
              />
              <Button
                appearance="inline"
                text={t.close}
                onClick={dismiss}
                data-qa="push-suggestion-close"
              />
            </Actions>
          </>
        )}
      </Card>
    </Banner>
  )
})

const Card = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
  padding: ${defaultMargins.s} ${defaultMargins.s} ${defaultMargins.m};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  border-radius: 4px;
`

const TitleRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${defaultMargins.s};
`

const Actions = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${defaultMargins.L};
`
