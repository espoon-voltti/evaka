// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled, { useTheme } from 'styled-components'

import { constantQuery, useQueryResult } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { desktopMin } from 'lib-components/breakpoints'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faBell, faChevronDown, faChevronUp, faTimes } from 'lib-icons'

import { guardianApplicationsQuery } from '../applications/queries'
import { useUser } from '../auth/state'
import { childrenQuery } from '../children/queries'
import { useTranslation } from '../localization'
import { headerHeightMobile } from '../navigation/const'
import { OverlayContext } from '../overlay/state'

import { InstallInstructions } from './InstallInstructions'
import {
  dismissInstallSuggestion,
  isInstallSuggestionDismissed
} from './dismissal'
import { useInstallAvailability } from './installAvailability'

export const InstallSuggestion = React.memo(function InstallSuggestion() {
  const i18n = useTranslation()
  const { colors } = useTheme()
  const user = useUser()
  const availability = useInstallAvailability()
  const { modalOpen } = useContext(OverlayContext)
  const [expanded, setExpanded] = useState(false)
  const [dismissedNow, setDismissedNow] = useState(false)

  const children = useQueryResult(user ? childrenQuery() : constantQuery([]))
  const hasPlacedChild = children
    .map((cs) =>
      cs.some((c) => c.unit !== null || c.upcomingPlacementType !== null)
    )
    .getOrElse(false)

  const applications = useQueryResult(
    user &&
      availability.kind !== 'unavailable' &&
      children.isSuccess &&
      !hasPlacedChild
      ? guardianApplicationsQuery()
      : constantQuery([])
  )
  const hasSentApplication = applications
    .map((cs) =>
      cs.some((c) => c.applicationSummaries.some((a) => a.sentDate !== null))
    )
    .getOrElse(false)

  if (!user || (!hasPlacedChild && !hasSentApplication)) return null
  if (dismissedNow || isInstallSuggestionDismissed(user.id)) return null
  if (availability.kind === 'unavailable') return null

  // Hide until the "Application sent" modal is dismissed. This also hides
  // the install suggestion when any modal is open.
  if (modalOpen) return null

  const action =
    availability.kind === 'instructions'
      ? {
          icon: expanded ? faChevronUp : faChevronDown,
          onClick: () => setExpanded(!expanded)
        }
      : { icon: undefined, onClick: () => void availability.show() }

  return (
    <Banner data-qa="pwa-install-suggestion">
      <Row>
        <RoundIcon content={faBell} color={colors.main.m2} size="L" />
        <Texts>
          <Title>{i18n.pwa.installSuggestion.title}</Title>
          <span>{i18n.pwa.install.text}</span>
          <Button
            appearance="inline"
            text={i18n.pwa.install.action}
            icon={action.icon}
            order="text-icon"
            onClick={action.onClick}
            data-qa="pwa-install-suggestion-action"
          />
        </Texts>
        <IconOnlyButton
          icon={faTimes}
          aria-label={i18n.common.close}
          onClick={() => {
            dismissInstallSuggestion(user.id)
            setDismissedNow(true)
          }}
          data-qa="pwa-install-suggestion-close"
        />
      </Row>
      {expanded && (
        <InstructionsPanel data-qa="pwa-install-suggestion-instructions">
          <InstallInstructions />
        </InstructionsPanel>
      )}
    </Banner>
  )
})

const Banner = styled.div`
  position: sticky;
  top: ${headerHeightMobile}px;
  z-index: 10;
  background-color: ${(p) => p.theme.colors.main.m4};
  padding: ${defaultMargins.s};

  @media (min-width: ${desktopMin}) {
    top: 0;
  }
`

const Row = styled.div`
  display: flex;
  align-items: flex-start;
  gap: ${defaultMargins.s};
`

const Texts = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: ${defaultMargins.xs};
  flex-grow: 1;
`

const Title = styled.span`
  font-weight: ${fontWeights.semibold};
`

const InstructionsPanel = styled.div`
  margin-top: ${defaultMargins.s};
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  border-radius: 8px;
`
