// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'

import { AssistanceNeedPreschoolDecisionCitizenListItem } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedDecisionStatusChip } from 'lib-components/assistance-need-decision/AssistanceNeedDecisionStatusChip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faFileAlt } from 'lib-icons'

import { useTranslation } from '../../localization'

export default React.memo(function AssistancePreschoolDecision({
  decision: {
    id,
    decisionMade,
    validityPeriod,
    unitName,
    status,
    type,
    annulmentReason,
    isUnread
  }
}: {
  decision: AssistanceNeedPreschoolDecisionCitizenListItem
}) {
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
              t.decisions.assistancePreschoolDecisions.title
            } ${decisionMade.format()}` +
            (isUnread ? ' - ' + t.decisions.unreadDecision : '')
          }
        >
          {t.decisions.assistancePreschoolDecisions.title}{' '}
          {decisionMade.format()}
        </H3>
      }
      countIndicator={isUnread ? 1 : 0}
      paddingHorizontal="0"
      paddingVertical="0"
      data-qa={`assistance-decision-${id}`}
    >
      <Gap size="xs" />
      <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
        <Label>{t.decisions.assistancePreschoolDecisions.type}</Label>
        <span data-qa="type">
          {t.decisions.assistancePreschoolDecisions.types[type]}
        </span>
        <Label>{t.decisions.assistanceDecisions.validityPeriod}</Label>
        <span data-qa="validity-period">{validityPeriod.format()}</span>
        <Label>{t.decisions.assistanceDecisions.unit}</Label>
        <span data-qa="selected-unit">{unitName}</span>
        <Label>{t.decisions.assistanceDecisions.decisionMade}</Label>
        <span data-qa="decision-made">{decisionMade.format()}</span>
        <Label>{t.decisions.assistanceDecisions.statusLabel}</Label>
        <AssistanceNeedDecisionStatusChip
          decisionStatus={status}
          texts={t.decisions.assistancePreschoolDecisions.statuses}
          data-qa="decision-status"
        />
        {status === 'ANNULLED' ? (
          <>
            <Label>
              {t.decisions.assistancePreschoolDecisions.annulmentReason}
            </Label>
            <span data-qa="annulment-reason">{annulmentReason}</span>
          </>
        ) : null}
      </ListGrid>
      <Gap size="m" />
      <Link
        to={`/decisions/assistance-preschool/${id}`}
        data-qa="open-decision"
      >
        <Button
          appearance="inline"
          icon={faFileAlt}
          text={t.decisions.assistanceDecisions.openDecision}
          onClick={() => undefined}
        />
      </Link>
    </CollapsibleContentArea>
  )
})
