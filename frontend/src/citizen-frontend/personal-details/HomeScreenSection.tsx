// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faChevronDown, faChevronUp } from 'lib-icons'

import { useTranslation } from '../localization'
import { InstallInstructions } from '../pwa/InstallInstructions'
import { useInstallAvailability } from '../pwa/installAvailability'

import { SectionTitle } from './components'

export default React.memo(
  React.forwardRef(function HomeScreenSection(
    _props: unknown,
    ref: React.Ref<HTMLDivElement>
  ) {
    const i18n = useTranslation()
    const availability = useInstallAvailability()
    const [expanded, setExpanded] = useState(false)

    if (availability.kind === 'unavailable') return null

    const action =
      availability.kind === 'instructions'
        ? {
            icon: expanded ? faChevronUp : faChevronDown,
            onClick: () => setExpanded(!expanded)
          }
        : { icon: undefined, onClick: () => void availability.show() }

    // The section owns its container, because a citizen who cannot install
    // anything here must not be left with an empty white box on the page.
    return (
      <>
        <Gap $size="s" />
        <ContentArea
          $opaque
          $paddingVertical="m"
          data-qa="home-screen-section"
          ref={ref}
        >
          <SectionTitle>{i18n.pwa.homeScreenSection.title}</SectionTitle>
          <P>{i18n.pwa.install.text}</P>
          <FixedSpaceColumn $spacing="s" $alignItems="flex-start">
            <Button
              appearance="inline"
              text={i18n.pwa.install.action}
              icon={action.icon}
              order="text-icon"
              onClick={action.onClick}
              data-qa="home-screen-action"
            />
            {expanded && <InstallInstructions />}
          </FixedSpaceColumn>
        </ContentArea>
      </>
    )
  })
)
