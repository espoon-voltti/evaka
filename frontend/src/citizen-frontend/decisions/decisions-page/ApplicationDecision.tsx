// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { Action } from 'lib-common/generated/action'
import {
  DecisionStatus,
  DecisionType
} from 'lib-common/generated/api-types/decision'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { StaticChip } from 'lib-components/atoms/Chip'
import Button from 'lib-components/atoms/buttons/Button'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { H3, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { theme } from 'lib-customizations/common'

import { useTranslation } from '../../localization'
import { PdfLink } from '../PdfLink'

const preschoolInfoTypes: DecisionType[] = [
  'PRESCHOOL',
  'PRESCHOOL_DAYCARE',
  'PRESCHOOL_CLUB',
  'PREPARATORY_EDUCATION'
]

interface Props {
  id: UUID
  applicationId: UUID
  type: DecisionType
  sentDate: LocalDate
  resolved: LocalDate | null
  status: DecisionStatus
  permittedActions: Set<Action.Citizen.Decision>
}

export default React.memo(function ApplicationDecision({
  id,
  applicationId,
  type,
  sentDate,
  resolved,
  status,
  permittedActions
}: Props) {
  const t = useTranslation()
  const [open, setOpen] = useState(resolved === null)
  const toggleOpen = useCallback(() => setOpen((o) => !o), [])

  return (
    <CollapsibleContentArea
      opaque={false}
      open={open}
      toggleOpen={toggleOpen}
      title={
        <H3
          noMargin
          data-qa="title-decision-type"
          aria-label={`${t.decisions.applicationDecisions.decision} ${
            t.decisions.applicationDecisions.type[type]
          } ${sentDate.format()} - ${
            t.decisions.applicationDecisions.status[status]
          }`}
        >
          {`${t.decisions.applicationDecisions.decision} ${
            t.decisions.applicationDecisions.type[type]
          } ${sentDate.format()}`}
        </H3>
      }
      countIndicator={resolved === null ? 1 : 0}
      paddingHorizontal="0"
      paddingVertical="0"
      data-qa={`application-decision-${id}`}
    >
      <Gap size="xs" />
      <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
        <Label>{t.decisions.applicationDecisions.sentDate}</Label>
        <span data-qa="decision-sent-date">{sentDate.format()}</span>
        {resolved !== null && (
          <>
            <Label>{t.decisions.applicationDecisions.resolved}</Label>
            <span data-qa="decision-resolved-date">{resolved.format()}</span>
          </>
        )}
        <Label>{t.decisions.applicationDecisions.statusLabel}</Label>
        <StaticChip
          color={decisionStatusColors[status]}
          fitContent
          data-qa="decision-status"
          data-qa-status={status}
        >
          {t.decisions.applicationDecisions.status[status]}
        </StaticChip>
      </ListGrid>
      {status === 'PENDING' ? (
        <ConfirmationDialog applicationId={applicationId} type={type} />
      ) : (
        permittedActions.has('DOWNLOAD_PDF') && (
          <>
            <Gap size="m" />
            <PdfLink decisionId={id} />
          </>
        )
      )}
    </CollapsibleContentArea>
  )
})

const decisionStatusColors: Record<DecisionStatus, string> = {
  PENDING: theme.colors.accents.a5orangeLight,
  ACCEPTED: theme.colors.accents.a3emerald,
  REJECTED: theme.colors.status.danger
}

const ConfirmationDialog = React.memo(function ConfirmationDialog({
  applicationId,
  type
}: {
  applicationId: string
  type: DecisionType
}) {
  const t = useTranslation()
  const navigate = useNavigate()

  return (
    <>
      <P width="800px">
        {
          t.decisions.applicationDecisions.confirmationInfo[
            preschoolInfoTypes.includes(type) ? 'preschool' : 'default'
          ]
        }
      </P>
      <P width="800px">
        <strong>{t.decisions.applicationDecisions.goToConfirmation}</strong>
      </P>
      <ButtonContainer>
        <Button
          primary
          text={t.decisions.applicationDecisions.confirmationLink}
          onClick={() => navigate(`/decisions/by-application/${applicationId}`)}
          data-qa={`button-confirm-decisions-${applicationId}`}
        />
      </ButtonContainer>
    </>
  )
})
