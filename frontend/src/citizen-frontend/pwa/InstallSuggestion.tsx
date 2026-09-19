// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
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

import { InstallInstructions } from './InstallInstructions'
import {
  Banner,
  Panel,
  Row,
  SuggestionNote,
  Texts,
  Title,
  useSuggestionStage
} from './SuggestionBanner'
import { useInstallAvailability } from './installAvailability'

export const InstallSuggestion = React.memo(function InstallSuggestion() {
  const i18n = useTranslation()
  const { colors } = useTheme()
  const user = useUser()
  const availability = useInstallAvailability()
  const [expanded, setExpanded] = useState(false)
  const { stage, showNote, dismiss } = useSuggestionStage('install')

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
  if (stage === 'hidden') return null
  if (availability.kind === 'unavailable') return null

  if (stage === 'note') {
    return (
      <SuggestionNote
        text={i18n.pwa.installSuggestion.dismissedNote}
        onClick={dismiss}
        data-qa="pwa-install-suggestion-note"
      />
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
          onClick={showNote}
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
