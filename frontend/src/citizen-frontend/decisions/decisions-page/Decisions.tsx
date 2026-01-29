// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import mapValues from 'lodash/mapValues'
import orderBy from 'lodash/orderBy'
import sortBy from 'lodash/sortBy'
import React, { Fragment, useMemo } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import type { DecisionSummary } from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { useQueryResult } from 'lib-common/query'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { ResponsiveLinkButton } from 'lib-components/atoms/buttons/LinkButton'
import { tabletMin } from 'lib-components/breakpoints'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faGavel } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import { childrenQuery } from '../../children/queries'
import { useTranslation } from '../../localization'
import useTitle from '../../useTitle'
import { decisionsQuery, financeDecisionsQuery } from '../queries'

import ApplicationDecision from './ApplicationDecision'
import FinanceDecision from './FinanceDecision'

export default React.memo(function Decisions() {
  const t = useTranslation()
  const children = useQueryResult(childrenQuery())
  const applicationDecisions = useQueryResult(decisionsQuery())

  const financeDecisions = useQueryResult(financeDecisionsQuery())

  useTitle(t, t.decisions.title)

  const getAriaLabelForChild = (child: {
    decisions: {
      applicationId: ApplicationId
      resolved: LocalDate | null
    }[]
    firstName: string
    lastName: string
    decidableApplications: ApplicationId[]
  }) => {
    const unconfirmedDecisionsCount = child.decisions.filter(
      (decision) =>
        decision.resolved === null &&
        child.decidableApplications.includes(decision.applicationId)
    ).length
    return (
      formatPersonName(child, 'First Last') +
      (unconfirmedDecisionsCount > 0
        ? ' - ' + t.decisions.unconfirmedDecisions(unconfirmedDecisionsCount)
        : ' - ' + t.decisions.noUnconfirmedDecisions)
    )
  }

  const unconfirmedDecisionsCount = useMemo(
    () =>
      applicationDecisions
        .map(
          ({ decisions, decidableApplications }) =>
            decisions
              .filter(applicationDecisionIsUnread)
              .filter((decision) =>
                decidableApplications.includes(decision.applicationId)
              ).length
        )
        .getOrElse(0),
    [applicationDecisions]
  )

  const sortedFinanceDecisions = useMemo(
    () =>
      financeDecisions.map((results) =>
        orderBy(results, ['validFrom', 'sentAt'], ['desc', 'desc'])
      ),
    [financeDecisions]
  )

  const childrenWithSortedDecisions = useMemo(
    () =>
      combine(children, applicationDecisions).map(
        ([children, applicationDecisions]) =>
          children
            .map((child) => {
              const childDecisions = sortBy(
                [
                  ...applicationDecisions.decisions.filter(
                    ({ childId }) => child.id === childId
                  )
                ],
                (decision) => [decision.sentDate.formatIso(), decision.type]
              ).reverse()
              return {
                ...child,
                decisions: childDecisions,
                decisionNotifications: childDecisions.filter((decision) =>
                  applicationDecisionIsUnread(decision)
                ).length,
                applicationDecisionPermittedActions: mapValues(
                  applicationDecisions.permittedActions,
                  (actions) => new Set(actions)
                ),
                decidableApplications:
                  applicationDecisions.decidableApplications
              }
            })
            .filter((child) => child.decisions.length > 0)
      ),
    [applicationDecisions, children]
  )

  const unconfirmedDecisionTypesPerChild = useMemo(() => {
    return combine(children, applicationDecisions).map(
      ([children, applicationDecisions]) =>
        children
          .map((child) => {
            const undecidedTypes = applicationDecisions.decisions
              .filter(applicationDecisionIsUnread)
              .filter(
                ({ childId, applicationId }) =>
                  child.id === childId &&
                  applicationDecisions.decidableApplications.includes(
                    applicationId
                  )
              )
              .map((decision) => decision.type)
            return { child, undecidedTypes }
          })
          .filter(({ undecidedTypes }) => undecidedTypes.length > 0)
    )
  }, [applicationDecisions, children])

  return (
    <Container data-qa="decisions-page">
      <Gap size="s" />
      <ContentArea opaque paddingVertical="s" paddingHorizontal="s" id="main">
        <H1 noMargin>{t.decisions.title}</H1>
        <Gap size="s" />
        {t.decisions.summary}
        {unconfirmedDecisionsCount > 0 && (
          <>
            <Gap size="s" />
            <UnconfirmedDecisionsBox>
              <IconContainer>
                <GavelIcon icon={faGavel} color={colors.grayscale.g0} />
              </IconContainer>
              <UnconfirmedColumn>
                <UnconfirmedHeader data-qa="alert-box-unconfirmed-decisions-count">
                  {t.decisions.unconfirmedDecisions(unconfirmedDecisionsCount)}
                </UnconfirmedHeader>
                {renderResult(
                  unconfirmedDecisionTypesPerChild,
                  (unconfirmedDecisionTypesPerChild) => (
                    <>
                      {unconfirmedDecisionTypesPerChild.map(
                        ({ child, undecidedTypes }) => (
                          <div key={child.id}>
                            <Label>
                              <PersonName person={child} format="First Last" />
                            </Label>
                            <DecisionTypeNameList>
                              {undecidedTypes.map((type) => (
                                <li key={type}>
                                  {t.decisions.applicationDecisions.type[type]}
                                </li>
                              ))}
                            </DecisionTypeNameList>
                          </div>
                        )
                      )}
                      <ResponsiveLinkButton href="/decisions/pending">
                        {t.decisions.applicationDecisions.confirmationLink}
                      </ResponsiveLinkButton>
                    </>
                  )
                )}
                <SmallLabel>
                  {t.decisions.applicationDecisions.information}
                </SmallLabel>
              </UnconfirmedColumn>
            </UnconfirmedDecisionsBox>
          </>
        )}
      </ContentArea>
      {renderResult(
        childrenWithSortedDecisions,
        (childrenWithSortedDecisions) => (
          <>
            {childrenWithSortedDecisions.map((child) => (
              <Fragment key={child.id}>
                <Gap size="s" />
                <ContentArea
                  opaque
                  paddingVertical="s"
                  paddingHorizontal="s"
                  data-qa={`child-decisions-${child.id}`}
                >
                  <H2 noMargin aria-label={getAriaLabelForChild(child)}>
                    <PersonName person={child} format="First Last" />
                  </H2>
                  {child.decisions.map((decision) => (
                    <Fragment key={decision.id}>
                      <FullWidthSeparator />
                      <ApplicationDecision
                        {...decision}
                        canDecide={child.decidableApplications.includes(
                          decision.applicationId
                        )}
                      />
                    </Fragment>
                  ))}
                </ContentArea>
              </Fragment>
            ))}
          </>
        )
      )}
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
      <Gap size="s" />{' '}
    </Container>
  )
})

const applicationDecisionIsUnread = (decision: DecisionSummary) =>
  decision.status === 'PENDING'

const UnconfirmedDecisionsBox = styled.div`
  display: flex;
  gap: ${defaultMargins.s};
  background-color: #ffeee0;
  border: 1px solid ${colors.status.warning};
  border-radius: ${defaultMargins.xxs};
  padding: ${defaultMargins.s};

  @media (max-width: ${tabletMin}) {
    flex-direction: column;
    align-items: center;
  }
`

const IconContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: ${defaultMargins.XL};
  min-width: ${defaultMargins.XL};
  height: ${defaultMargins.XL};
  background: ${colors.status.warning};
  border-radius: 100%;
`
const GavelIcon = styled(FontAwesomeIcon)`
  width: ${defaultMargins.m};
  height: ${defaultMargins.m};
`
const UnconfirmedColumn = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
`
const UnconfirmedHeader = styled(H2)`
  margin-block: ${defaultMargins.xs};
`
const DecisionTypeNameList = styled.ul`
  margin-block-start: ${defaultMargins.xxs};
  margin-block-end: ${defaultMargins.s};
  padding-inline-start: ${defaultMargins.m};
`

const FullWidthSeparator = styled.hr`
  width: calc(100% + ${defaultMargins.s} * 2);
  border: none;
  border-bottom-width: 1px;
  border-bottom-style: solid;
  border-bottom-color: ${colors.grayscale.g15};
  margin: ${defaultMargins.s} -${defaultMargins.s};
`
const SmallLabel = styled(Label)`
  font-size: 14px;
`
