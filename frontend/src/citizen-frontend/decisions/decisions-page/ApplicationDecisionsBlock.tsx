// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useHistory } from 'react-router-dom'
import Button from '@evaka/lib-components/atoms/buttons/Button'
import { ContentArea } from '@evaka/lib-components/layout/Container'
import ListGrid from '@evaka/lib-components/layout/ListGrid'
import ButtonContainer from '@evaka/lib-components/layout/ButtonContainer'
import RoundIcon from '@evaka/lib-components/atoms/RoundIcon'
import { H2, H3, Label, P } from '@evaka/lib-components/typography'
import { Gap } from '@evaka/lib-components/white-space'
import { ApplicationDecisions, DecisionType } from '../../decisions/types'
import { useTranslation } from '../../localization'
import {
  decisionOrderComparator,
  Status,
  decisionStatusIcon
} from '../../decisions/shared'
import { PdfLink } from '../../decisions/PdfLink'

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

            <Gap size="m" sizeOnMobile={'zero'} />

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
  const history = useHistory()

  return (
    <>
      <P width="800px">
        {
          t.decisions.applicationDecisions.confirmationInfo[
            preschoolInfoTypes.includes(type) ? 'PRESCHOOL' : 'default'
          ]
        }
      </P>
      <P width="800px">
        <strong>{t.decisions.applicationDecisions.goToConfirmation}</strong>
      </P>
      <Gap size="s" />
      <ButtonContainer>
        <Button
          primary
          text={t.decisions.applicationDecisions.confirmationLink}
          onClick={() =>
            history.push(`/decisions/by-application/${applicationId}`)
          }
          dataQa={`button-confirm-decisions-${applicationId}`}
        />
      </ButtonContainer>
    </>
  )
})
