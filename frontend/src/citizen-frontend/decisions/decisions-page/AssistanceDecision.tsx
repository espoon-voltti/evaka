// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'

import DateRange from 'lib-common/date-range'
import {
  AssistanceLevel,
  AssistanceNeedDecisionStatus,
  UnitInfoBasics
} from 'lib-common/generated/api-types/assistanceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { AssistanceNeedDecisionStatusChip } from 'lib-components/assistance-need-decision/AssistanceNeedDecisionStatusChip'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faFileAlt } from 'lib-icons'

import { useTranslation } from '../../localization'

interface Props {
  id: UUID
  decisionMade: LocalDate
  assistanceLevels: AssistanceLevel[]
  validityPeriod: DateRange
  selectedUnit: UnitInfoBasics | null
  status: AssistanceNeedDecisionStatus
  annulmentReason: string
  isUnread: boolean
}

export default React.memo(function AssistanceDecision({
  id,
  decisionMade,
  assistanceLevels,
  validityPeriod,
  selectedUnit,
  status,
  annulmentReason,
  isUnread
}: Props) {
  const t = useTranslation()
  const [open, setOpen] = useState(isUnread)
  const toggleOpen = useCallback(() => setOpen((o) => !o), [])

  return (
    <CollapsibleContentArea
      opaque={false}
      open={open}
      toggleOpen={toggleOpen}
      title={
        <H3
          noMargin
          data-qa={`title-decision-type-${id}`}
          aria-label={
            `${
              t.decisions.assistanceDecisions.title
            } ${decisionMade.format()}` +
            (isUnread ? ' - ' + t.decisions.unreadDecision : '')
          }
        >
          {t.decisions.assistanceDecisions.title} {decisionMade.format()}
        </H3>
      }
      countIndicator={isUnread ? 1 : 0}
      paddingHorizontal="0"
      paddingVertical="0"
      data-qa={`assistance-decision-${id}`}
    >
      <Gap size="xs" />
      <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
        <Label>{t.decisions.assistanceDecisions.assistanceLevel}</Label>
        <span data-qa="assistance-level">
          {sortBy(assistanceLevels)
            .map((level, i) => {
              const text = t.decisions.assistanceDecisions.level[level]
              return i == 0 ? text : text.toLocaleLowerCase()
            })
            .join(', ')}
        </span>
        <Label>{t.decisions.assistanceDecisions.validityPeriod}</Label>
        <span data-qa="validity-period">{validityPeriod.format()}</span>
        {selectedUnit !== null && (
          <>
            <Label>{t.decisions.assistanceDecisions.unit}</Label>
            <span data-qa="selected-unit">{selectedUnit.name}</span>
          </>
        )}
        <Label>{t.decisions.assistanceDecisions.decisionMade}</Label>
        <span data-qa="decision-made">{decisionMade.format()}</span>
        <Label>{t.decisions.assistanceDecisions.statusLabel}</Label>
        <AssistanceNeedDecisionStatusChip
          decisionStatus={status}
          texts={t.decisions.assistanceDecisions.decision.statuses}
          data-qa="decision-status"
        />
        {status === 'ANNULLED' ? (
          <>
            <Label>
              {t.decisions.assistanceDecisions.decision.annulmentReason}
            </Label>
            <span data-qa="annulment-reason">{annulmentReason}</span>
          </>
        ) : null}
      </ListGrid>
      <Gap size="m" />
      <Link to={`/decisions/assistance/${id}`} data-qa="open-decision">
        <InlineButton
          icon={faFileAlt}
          text={t.decisions.assistanceDecisions.openDecision}
          onClick={() => undefined}
        />
      </Link>
    </CollapsibleContentArea>
  )
})
