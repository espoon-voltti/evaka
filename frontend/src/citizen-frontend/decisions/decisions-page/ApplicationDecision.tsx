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
import IconChip from 'lib-components/atoms/IconChip'
import { ResponsiveLinkButton } from 'lib-components/atoms/buttons/LinkButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H3, H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import { applicationMetadataQuery } from '../../applications/queries'
import { useTranslation } from '../../localization'
import { MetadataResultSection } from '../../metadata/MetadataSection'
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
      opaque={false}
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
          <H4 noMargin data-qa="decision-sent-date">
            {sentDate.format()}
          </H4>
          <H3 noMargin data-qa="title-decision-type">
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
      paddingHorizontal="0"
      paddingVertical="0"
      data-qa={`application-decision-${id}`}
    >
      {featureFlags.showMetadataToCitizen && (
        <MetadataResultSection
          query={applicationMetadataQuery({ applicationId })}
        />
      )}
    </CollapsibleContentArea>
  )
})

const AlwaysShownCollapseContent = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: ${defaultMargins.s};
  margin-top: ${defaultMargins.s};
`
