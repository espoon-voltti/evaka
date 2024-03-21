// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'
import orderBy from 'lodash/orderBy'
import sortBy from 'lodash/sortBy'
import React, { Fragment, useMemo } from 'react'

import { renderResult } from 'citizen-frontend/async-rendering'
import { combine } from 'lib-common/api'
import {
  AssistanceNeedDecisionCitizenListItem,
  AssistanceNeedPreschoolDecisionCitizenListItem
} from 'lib-common/generated/api-types/assistanceneed'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
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
import { decisionsQuery, financeDecisionsQuery } from '../queries'
import { applicationDecisionIsUnread } from '../shared'

import ApplicationDecision from './ApplicationDecision'
import AssistanceDecision from './AssistanceDecision'
import AssistancePreschoolDecision from './AssistancePreschoolDecision'
import FinanceDecision from './FinanceDecision'

export default React.memo(function Decisions() {
  const t = useTranslation()
  const children = useQueryResult(childrenQuery())
  const applicationDecisions = useQueryResult(decisionsQuery())
  const assistanceDecisions = useQueryResult(assistanceDecisionsQuery())
  const assistancePreschoolDecisions = useQueryResult(
    assistanceNeedPreschoolDecisionsQuery()
  )

  const financeDecisions = useQueryResult(financeDecisionsQuery())

  useTitle(t, t.decisions.title)

  const getAriaLabelForChild = (child: {
    decisions: (
      | AssistanceNeedDecisionCitizenListItem
      | AssistanceNeedPreschoolDecisionCitizenListItem
      | {
          applicationId: UUID
          resolved: LocalDate | null
        }
    )[]
    firstName: string
    lastName: string
  }) => {
    const unconfirmedDecisionsCount = child.decisions.filter(
      (decision) => 'applicationId' in decision && decision.resolved === null
    ).length
    return (
      `${child.firstName} ${child.lastName}` +
      (unconfirmedDecisionsCount > 0
        ? ' - ' + t.decisions.unconfirmedDecisions(unconfirmedDecisionsCount)
        : ' - ' + t.decisions.noUnconfirmedDecisions)
    )
  }

  const unconfirmedDecisionsCount = useMemo(
    () =>
      applicationDecisions
        .map(
          ({ decisions }) =>
            decisions.filter(applicationDecisionIsUnread).length
        )
        .getOrElse(0),
    [applicationDecisions]
  )

  const sortedFinanceDecisions = useMemo(
    () =>
      financeDecisions.map((results) =>
        orderBy(results, ['sentAt', 'validFrom'], ['desc', 'desc'])
      ),
    [financeDecisions]
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
                  ...applicationDecisions.decisions.filter(
                    ({ childId }) => child.id === childId
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
                    ? [decision.decisionMade.formatIso(), '']
                    : [decision.sentDate.formatIso(), decision.type]
              ).reverse()
              return {
                ...child,
                decisions: childDecisions,
                decisionNotifications: childDecisions.filter((decision) =>
                  'applicationId' in decision
                    ? applicationDecisionIsUnread(decision)
                    : decision.isUnread
                ).length,
                applicationDecisionPermittedActions: mapValues(
                  applicationDecisions.permittedActions,
                  (actions) => new Set(actions)
                ),
                canDecide: applicationDecisions.canDecide
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
      <ContentArea opaque paddingVertical="L" id="main">
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

      {renderResult(sortedFinanceDecisions, (decisions) => (
        <FixedSpaceColumn>
          <ContentArea opaque paddingVertical="L">
            <H2 noMargin>{t.decisions.financeDecisions.title}</H2>
            {decisions.length > 0 && <Gap size="xs" />}
            {decisions.map((decision, index) => (
              <Fragment key={decision.id}>
                <HorizontalLine dashed slim />
                <FinanceDecision
                  decisionData={decision}
                  startOpen={index === 0}
                />
              </Fragment>
            ))}
          </ContentArea>
        </FixedSpaceColumn>
      ))}

      {renderResult(
        childrenWithSortedDecisions,
        (childrenWithSortedDecisions) => (
          <>
            <Gap size="s" />
            <ContentArea opaque paddingVertical="L">
              <H2 noMargin>{t.decisions.childhoodEducationTitle}</H2>
            </ContentArea>
            <Gap size="s" />
            <FixedSpaceColumn>
              {childrenWithSortedDecisions.map((child) => (
                <ContentArea
                  key={child.id}
                  opaque
                  paddingVertical="L"
                  data-qa={`child-decisions-${child.id}`}
                >
                  <H2 noMargin aria-label={getAriaLabelForChild(child)}>
                    {child.firstName} {child.lastName}
                  </H2>
                  {child.decisions.map((decision) => (
                    <Fragment key={decision.id}>
                      <HorizontalLine dashed slim />
                      {'applicationId' in decision ? (
                        <ApplicationDecision
                          {...decision}
                          permittedActions={
                            child.applicationDecisionPermittedActions[
                              decision.id
                            ] ?? new Set()
                          }
                          canDecide={child.canDecide.includes(
                            decision.applicationId
                          )}
                        />
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
          </>
        )
      )}

      <Gap size="s" />
    </Container>
  )
})
