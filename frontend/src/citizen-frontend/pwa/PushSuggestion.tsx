// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3, P } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faLaptop, faMobileButton, faTabletButton } from 'lib-icons'

import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'

import { Instructions } from './InstallInstructions'
import { PushStatusChip } from './PushStatusChip'
import { Banner, Note } from './SuggestionBanner'
import { dismissSuggestion, isSuggestionDismissed } from './dismissal'
import type { Platform } from './platform'
import { platform } from './platform'
import {
  usePushAvailability,
  useSubscribeToPush,
  useThisPushDevice
} from './pushNotifications'
import { sendTestPushNotificationMutation } from './queries'

const deviceIcons = (p: Platform) =>
  p === 'other' ? [faLaptop] : [faMobileButton, faTabletButton]

/**
 * Suggests enabling push notifications inside the installed app. Unlike the
 * install suggestion the card stays after it is answered and reports the
 * permission state, so the citizen can test a fresh subscription or read how
 * to recover a blocked one.
 */
export const PushSuggestion = React.memo(function PushSuggestion() {
  const i18n = useTranslation()
  const t = i18n.pwa.pushSuggestion
  const user = useUser()
  const availability = usePushAvailability()
  const thisDevice = useThisPushDevice()
  const subscribe = useSubscribeToPush()
  const { modalOpen } = useContext(OverlayContext)
  const [stage, setStage] = useState<'suggestion' | 'note' | 'gone'>(
    'suggestion'
  )

  if (!user) return null
  if (stage === 'gone' || isSuggestionDismissed('push', user.id)) return null
  if (availability.kind === 'unavailable') return null
  if (modalOpen) return null

  const dismiss = () => {
    dismissSuggestion('push', user.id)
    setStage('gone')
  }

  if (stage === 'note') {
    return (
      <Banner data-qa="push-suggestion-note">
        <Note onClick={dismiss} data-qa="push-suggestion-note-action">
          {t.dismissedNote}
        </Note>
      </Banner>
    )
  }

  const status =
    availability.kind === 'subscribed'
      ? 'enabled'
      : availability.kind === 'blocked'
        ? 'blocked'
        : 'disabled'

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
              <Button
                primary
                text={t.enable}
                onClick={() => void subscribe()}
                data-qa="push-suggestion-enable"
              />
              <Button
                appearance="inline"
                text={t.later}
                onClick={() => setStage('note')}
                data-qa="push-suggestion-later"
              />
            </Actions>
          </>
        )}
        {availability.kind === 'subscribed' && (
          <>
            <P $noMargin>{t.enabledText}</P>
            <Actions>
              {thisDevice && (
                <MutateButton
                  appearance="inline"
                  text={t.sendTest}
                  mutation={sendTestPushNotificationMutation}
                  onClick={() => ({ body: { deviceId: thisDevice } })}
                  data-qa="push-suggestion-test"
                />
              )}
              <Button
                appearance="inline"
                text={t.close}
                onClick={dismiss}
                data-qa="push-suggestion-close"
              />
            </Actions>
          </>
        )}
        {availability.kind === 'blocked' && (
          <>
            <div data-qa="push-suggestion-instructions">
              <Instructions
                deviceIcons={deviceIcons(platform())}
                device={t.device[platform()]}
                steps={t.blockedSteps[platform()]}
              />
            </div>
            <Actions>
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
