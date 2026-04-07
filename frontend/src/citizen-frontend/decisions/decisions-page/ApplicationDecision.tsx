// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import type {
  DecisionStatus,
  DecisionType
} from 'lib-common/generated/api-types/decision'
import type {
  ApplicationId,
  DecisionId
} from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import IconChip from 'lib-components/atoms/IconChip'
import { ResponsiveLinkButton } from 'lib-components/atoms/buttons/LinkButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3, H4, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import { applicationMetadataQuery } from '../../applications/queries'
import { renderResult } from '../../async-rendering'
import { useTranslation } from '../../localization'
import { MetadataResultSection } from '../../metadata/MetadataSection'
import { PdfLink } from '../PdfLink'
import { decisionDetailsQuery } from '../queries'
import { iconPropsByStatus } from '../shared'

interface Props {
  id: DecisionId
  applicationId: ApplicationId
  type: DecisionType
  sentDate: LocalDate
  status: DecisionStatus
  canDecide: boolean
}

export default React.memo(function ApplicationDecision({
  id,
  applicationId,
  type,
  sentDate,
  status,
  canDecide
}: Props) {
  const t = useTranslation()
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((o) => !o), [])

  return (
    <CollapsibleContentArea
      $opaque={false}
      open={open}
      toggleOpen={toggleOpen}
      title={
        <div
          aria-label={`${
            t.decisions.applicationDecisions.type[type]
          } ${sentDate.format()} - ${
            t.decisions.applicationDecisions.status[status]
          }`}
        >
          <H4 $noMargin data-qa="decision-sent-date">
            {sentDate.format()}
          </H4>
          <Gap $size="xxs" />
          <H3 $noMargin data-qa="title-decision-type">
            {t.decisions.applicationDecisions.type[type]}
          </H3>
        </div>
      }
      alwaysShownContent={
        <AlwaysShownCollapseContent>
          <IconChip
            {...iconPropsByStatus[status]}
            label={t.decisions.applicationDecisions.status[status]}
            data-qa="decision-status"
          />
          {canDecide && (
            <ResponsiveLinkButton
              $style="secondary"
              href="/decisions/pending"
              data-qa={`button-confirm-decisions-${applicationId}`}
            >
              {t.decisions.applicationDecisions.confirmationLink}
            </ResponsiveLinkButton>
          )}
        </AlwaysShownCollapseContent>
      }
      $paddingHorizontal="0"
      $paddingVertical="0"
      data-qa={`application-decision-${id}`}
    >
      {open && <DecisionDetails id={id} applicationId={applicationId} />}
    </CollapsibleContentArea>
  )
})

const DecisionDetails = React.memo(function DecisionDetails({
  id,
  applicationId
}: {
  id: DecisionId
  applicationId: ApplicationId
}) {
  const t = useTranslation()
  const detailsResult = useQueryResult(decisionDetailsQuery({ id }))
  const [metadataOpen, setMetadataOpen] = useState(false)
  const toggleMetadata = useCallback(() => setMetadataOpen((o) => !o), [])

  return renderResult(detailsResult, (details) => (
    <FixedSpaceColumn $spacing="s">
      <PaddedRow>
        <PdfLink decisionId={id} />
      </PaddedRow>
      <div>
        <Label>{t.decisions.applicationDecisions.unit}</Label>
        <Gap $size="xxs" />
        <div data-qa="decision-unit" translate="no">
          {details.unitName}
        </div>
      </div>
      <div>
        <Label>{t.decisions.applicationDecisions.period}</Label>
        <Gap $size="xxs" />
        <div data-qa="decision-period">
          {details.startDate.format()} – {details.endDate.format()}
        </div>
      </div>
      <div>
        <Label>{t.decisions.applicationDecisions.sentDate}</Label>
        <Gap $size="xxs" />
        <div data-qa="decision-sent-date-detail">
          {details.sentDate.format()}
        </div>
      </div>
      <div>
        <Label>{t.decisions.applicationDecisions.resolved}</Label>
        <Gap $size="xxs" />
        <div data-qa="decision-resolved">
          {details.resolved?.format() ?? '–'}
        </div>
      </div>
      {featureFlags.showMetadataToCitizen && (
        <MetadataAccordion
          open={metadataOpen}
          toggleOpen={toggleMetadata}
          title={
            <Label>{t.decisions.applicationDecisions.additionalDetails}</Label>
          }
          $opaque={false}
          $paddingHorizontal="s"
          $paddingVertical="s"
          data-qa="metadata-section"
        >
          <MetadataResultSection
            query={applicationMetadataQuery({ applicationId })}
          />
        </MetadataAccordion>
      )}
    </FixedSpaceColumn>
  ))
})

const AlwaysShownCollapseContent = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: ${defaultMargins.s};
  margin-top: ${defaultMargins.s};
`
const PaddedRow = styled.div`
  padding: ${defaultMargins.xs};
  word-break: break-word;
`
const MetadataAccordion = styled(CollapsibleContentArea)`
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  padding: ${defaultMargins.s};
`
