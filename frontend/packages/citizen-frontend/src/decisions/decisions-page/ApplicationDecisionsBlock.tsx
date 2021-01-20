// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import LinkWrapperInlineBlock from '@evaka/lib-components/src/atoms/LinkWrapperInlineBlock'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import { H2, H3, Label, P } from '@evaka/lib-components/src/typography'
import { Gap } from '@evaka/lib-components/src/white-space'
import { ApplicationDecisions, DecisionType } from '~decisions/types'
import { useTranslation } from '~localization'
import {
  decisionOrderComparator,
  Status,
  decisionStatusIcon
} from '~decisions/shared'
import { PdfLink } from '~decisions/PdfLink'

const noop = () => undefined

const preschoolInfoTypes: DecisionType[] = [
  'PRESCHOOL',
  'PRESCHOOL_DAYCARE',
  'PREPARATORY_EDUCATION'
]

export default React.memo(function ApplicationDecisionsBlock({
  applicationId,
  childName,
  decisions
}: ApplicationDecisions) {
  const t = useTranslation()

  return (
    <ContentArea
      opaque
      paddingVertical="L"
      data-qa={`application-${applicationId}`}
    >
      <H2 noMargin data-qa={`title-decision-child-name-${applicationId}`}>
        {childName}
      </H2>
      {decisions
        .sort(decisionOrderComparator)
        .map(({ decisionId, type, status, sentDate, resolved }) => (
          <React.Fragment key={decisionId}>
            <Gap size="L" />
            <H3 noMargin data-qa={`title-decision-type-${decisionId}`}>
              {`${t.decisions.applicationDecisions.decision} ${t.decisions.applicationDecisions.type[type]}`}
            </H3>
            <Gap size="m" />
            <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
              <Label>{t.decisions.applicationDecisions.sentDate}</Label>
              <span data-qa={`decision-sent-date-${decisionId}`}>
                {sentDate.format()}
              </span>
              {resolved && (
                <>
                  <Label>{t.decisions.applicationDecisions.resolved}</Label>
                  <span data-qa={`decision-resolved-date-${decisionId}`}>
                    {resolved.format()}
                  </span>
                </>
              )}
              <Label>{t.decisions.applicationDecisions.statusLabel}</Label>
              <Status data-qa={`decision-status-${decisionId}`}>
                <RoundIcon
                  content={decisionStatusIcon[status].icon}
                  color={decisionStatusIcon[status].color}
                  size="s"
                />
                <Gap size="xs" horizontal />
                {t.decisions.applicationDecisions.status[status]}
              </Status>
            </ListGrid>
            <Gap size="m" />
            {status === 'PENDING' ? (
              <ConfirmationDialog applicationId={applicationId} type={type} />
            ) : (
              <PdfLink decisionId={decisionId} />
            )}
          </React.Fragment>
        ))}
    </ContentArea>
  )
})

const ConfirmationDialog = React.memo(function ConfirmationDialog({
  applicationId,
  type
}: {
  applicationId: string
  type: DecisionType
}) {
  const t = useTranslation()

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
      <Gap size="s" />
      <LinkWrapperInlineBlock
        to={`/decisions/by-application/${applicationId}`}
        data-qa={`response-link-${applicationId}`}
      >
        <Button
          primary
          text={t.decisions.applicationDecisions.confirmationLink}
          onClick={noop}
          dataQa="button-confirm-decisions"
        />
      </LinkWrapperInlineBlock>
    </>
  )
})
