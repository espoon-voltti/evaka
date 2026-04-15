// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../localization'
import { detectBrowser } from '../pwa/detectPlatform'

import { PermissionGuide } from './PermissionGuide'
import type { CitizenPushCategory } from './webpush-api'
import { useWebPushState } from './webpush-state'

const ALL_CATEGORIES: CitizenPushCategory[] = [
  'URGENT_MESSAGE',
  'MESSAGE',
  'BULLETIN'
]

export const WebPushSettingsSection = React.memo(
  function WebPushSettingsSection() {
    const t = useTranslation().personalDetails.webPushSection
    const {
      status,
      categories,
      subscribe,
      updateCategories,
      unsubscribe,
      sendTest
    } = useWebPushState()
    const browser = useMemo(() => detectBrowser(), [])

    const masterOn = status === 'subscribed'
    const masterDisabled =
      status === 'unsupported' || status === 'denied' || status === 'loading'

    const handleMasterToggle = useCallback(async () => {
      if (status === 'subscribed') {
        await unsubscribe()
      } else if (status === 'unregistered') {
        await subscribe(new Set(ALL_CATEGORIES))
      }
    }, [status, subscribe, unsubscribe])

    const toggleCategory = useCallback(
      async (category: CitizenPushCategory, nowChecked: boolean) => {
        const next = new Set(categories)
        if (nowChecked) next.add(category)
        else next.delete(category)
        await updateCategories(next)
      },
      [categories, updateCategories]
    )

    return (
      <div data-qa="webpush-settings-section">
        <H2 $noMargin>{t.title}</H2>
        {t.info}
        <Checkbox
          checked={masterOn}
          disabled={masterDisabled}
          label={t.enable}
          onChange={() => void handleMasterToggle()}
          data-qa="webpush-master-toggle"
        />
        <Gap $size="m" />
        <ExpandingInfo info={t.categoryUrgent.description}>
          <Checkbox
            checked={categories.has('URGENT_MESSAGE')}
            disabled={!masterOn}
            label={t.categoryUrgent.label}
            onChange={(checked) =>
              void toggleCategory('URGENT_MESSAGE', checked)
            }
            data-qa="webpush-cat-urgent"
          />
        </ExpandingInfo>
        <Gap $size="s" />
        <ExpandingInfo info={t.categoryMessage.description}>
          <Checkbox
            checked={categories.has('MESSAGE')}
            disabled={!masterOn}
            label={t.categoryMessage.label}
            onChange={(checked) => void toggleCategory('MESSAGE', checked)}
            data-qa="webpush-cat-message"
          />
        </ExpandingInfo>
        <Gap $size="s" />
        <ExpandingInfo info={t.categoryBulletin.description}>
          <Checkbox
            checked={categories.has('BULLETIN')}
            disabled={!masterOn}
            label={t.categoryBulletin.label}
            onChange={(checked) => void toggleCategory('BULLETIN', checked)}
            data-qa="webpush-cat-bulletin"
          />
        </ExpandingInfo>
        <Gap $size="m" />
        <Button
          text={t.sendTest}
          onClick={() => void sendTest()}
          disabled={!masterOn}
          data-qa="webpush-send-test"
        />
        {(status === 'unsupported' || status === 'denied') && (
          <PermissionGuide browser={browser} />
        )}
      </div>
    )
  }
)
