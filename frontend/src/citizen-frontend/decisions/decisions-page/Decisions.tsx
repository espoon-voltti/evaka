// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { Fragment, useMemo } from 'react'

import { renderResult } from 'citizen-frontend/async-rendering'
import { combine } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { childrenQuery } from '../../children/queries'
import { useTranslation } from '../../localization'
import useTitle from '../../useTitle'
import { assistanceDecisionsQuery } from '../assistance-decision-page/queries'
import { assistanceNeedPreschoolDecisionsQuery } from '../assistance-decision-page/queries-preschool'
import { decisionsQuery } from '../queries'
import { applicationDecisionIsUnread } from '../shared'

import ApplicationDecision from './ApplicationDecision'
import AssistanceDecision from './AssistanceDecision'
import AssistancePreschoolDecision from './AssistancePreschoolDecision'

export default React.memo(function Decisions() {
  const t = useTranslation()
  const children = useQueryResult(childrenQuery())
  const applicationDecisions = useQueryResult(decisionsQuery())
  const assistanceDecisions = useQueryResult(assistanceDecisionsQuery())
  const assistancePreschoolDecisions = useQueryResult(
    assistanceNeedPreschoolDecisionsQuery()
  )

  useTitle(t, t.decisions.title)

  const unconfirmedDecisionsCount = useMemo(
    () =>
      applicationDecisions
        .map(
          (decisions) =>
            decisions.flatMap(({ decisions }) =>
              decisions.filter(applicationDecisionIsUnread)
            ).length
        )
        .getOrElse(0),
    [applicationDecisions]
  )

  const childrenWithSortedDecisions = useMemo(
    () =>
      combine(
        children,
        applicationDecisions,
        assistanceDecisions,
        assistancePreschoolDecisions
      ).map(
        ([
          children,
          applicationDecisions,
          assistanceDecisions,
          assistancePreschoolDecisions
        ]) =>
          children
            .map((child) => {
              const childDecisions = sortBy(
                [
                  ...applicationDecisions
                    .filter(({ childId }) => child.id === childId)
                    .flatMap(({ applicationId, decisions }) =>
                      decisions.map((decision) => ({
                        ...decision,
                        applicationId
                      }))
                    ),
                  ...assistanceDecisions.filter(
                    ({ childId }) => child.id === childId
                  ),
                  ...assistancePreschoolDecisions.filter(
                    ({ childId }) => child.id === childId
                  )
                ],
                (decision) =>
                  'decisionMade' in decision
                    ? decision.decisionMade?.formatIso()
                    : [decision.sentDate.formatIso(), decision.type]
              )
              return {
                ...child,
                decisions: childDecisions,
                decisionNotifications: childDecisions.filter((decision) =>
                  'applicationId' in decision
                    ? applicationDecisionIsUnread(decision)
                    : decision.isUnread
                ).length
              }
            })
            .filter((child) => child.decisions.length > 0)
      ),
    [
      applicationDecisions,
      assistanceDecisions,
      assistancePreschoolDecisions,
      children
    ]
  )

  return (
    <Container data-qa="decisions-page">
      <Gap size="s" />
      <ContentArea opaque paddingVertical="L">
        <H1 noMargin>{t.decisions.title}</H1>
        <Gap size="xs" />
        {t.decisions.summary}
        {unconfirmedDecisionsCount > 0 && (
          <>
            <Gap size="s" />
            <AlertBox
              message={t.decisions.unconfirmedDecisions(
                unconfirmedDecisionsCount
              )}
              data-qa="alert-box-unconfirmed-decisions-count"
            />
          </>
        )}
      </ContentArea>
      <Gap size="s" />
      {renderResult(
        childrenWithSortedDecisions,
        (childrenWithSortedDecisions) => (
          <FixedSpaceColumn>
            {childrenWithSortedDecisions.map((child) => (
              <ContentArea
                key={child.id}
                opaque
                paddingVertical="L"
                data-qa={`child-decisions-${child.id}`}
              >
                <H2 noMargin>
                  {child.firstName} {child.lastName}
                </H2>
                {child.decisions.map((decision) => (
                  <Fragment key={decision.id}>
                    <HorizontalLine dashed slim />
                    {'applicationId' in decision ? (
                      <ApplicationDecision {...decision} />
                    ) : 'assistanceLevels' in decision ? (
                      <AssistanceDecision {...decision} />
                    ) : (
                      <AssistancePreschoolDecision decision={decision} />
                    )}
                  </Fragment>
                ))}
              </ContentArea>
            ))}
          </FixedSpaceColumn>
        )
      )}
    </Container>
  )
})
