// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { useTheme } from 'styled-components'

import { constantQuery, useQueryResult } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { faBell, faChevronDown, faChevronUp, faTimes } from 'lib-icons'

import { guardianApplicationsQuery } from '../applications/queries'
import { useUser } from '../auth/state'
import { childrenQuery } from '../children/queries'
import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'

import { InstallInstructions } from './InstallInstructions'
import { Banner, Note, Panel, Row, Texts, Title } from './SuggestionBanner'
import { dismissSuggestion, isSuggestionDismissed } from './dismissal'
import { useInstallAvailability } from './installAvailability'

export const InstallSuggestion = React.memo(function InstallSuggestion() {
  const i18n = useTranslation()
  const { colors } = useTheme()
  const user = useUser()
  const availability = useInstallAvailability()
  const { modalOpen } = useContext(OverlayContext)
  const [expanded, setExpanded] = useState(false)
  const [stage, setStage] = useState<'suggestion' | 'note' | 'gone'>(
    'suggestion'
  )

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
  if (stage === 'gone' || isSuggestionDismissed('install', user.id)) return null
  if (availability.kind === 'unavailable') return null

  // Hide until the "Application sent" modal is dismissed. This also hides
  // the install suggestion when any modal is open.
  if (modalOpen) return null

  if (stage === 'note') {
    return (
      <Banner data-qa="pwa-install-suggestion-note">
        <Note
          onClick={() => {
            dismissSuggestion('install', user.id)
            setStage('gone')
          }}
          data-qa="pwa-install-suggestion-note-action"
        >
          {i18n.pwa.installSuggestion.dismissedNote}
        </Note>
      </Banner>
    )
  }

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
          onClick={() => setStage('note')}
          data-qa="pwa-install-suggestion-close"
        />
      </Row>
      {expanded && (
        <Panel data-qa="pwa-install-suggestion-instructions">
          <InstallInstructions />
        </Panel>
      )}
    </Banner>
  )
})
