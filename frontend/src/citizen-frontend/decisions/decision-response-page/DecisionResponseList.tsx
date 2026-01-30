// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { Fragment, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation } from 'wouter'

import type { DecisionWithValidStartDatePeriod } from 'lib-common/generated/api-types/application'
import type { DecisionStatus } from 'lib-common/generated/api-types/decision'
import type { DecisionId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import Main from 'lib-components/atoms/Main'
import { Button } from 'lib-components/atoms/buttons/Button'
import { Container, ContentArea } from 'lib-components/layout/Container'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faChevronLeft, faExclamation } from 'lib-icons'

import Footer from '../../Footer'
import { useTranslation } from '../../localization'
import useTitle from '../../useTitle'
import { pendingDecisionsQuery } from '../queries'

import DecisionResponse from './DecisionResponse'

export default React.memo(function DecisionResponseList() {
  const t = useTranslation()
  const [, navigate] = useLocation()

  const decisionsRequest = useQueryResult(pendingDecisionsQuery())

  // Local state to keep decisions visible after handling them
  const [localDecisionStatuses, setLocalDecisionStatuses] = useState<
    Map<DecisionId, DecisionStatus>
  >(new Map())

  const initializedRef = useRef(false)
  const initialDecisionsRef = useRef<DecisionWithValidStartDatePeriod[]>([])

  useEffect(() => {
    if (decisionsRequest.isSuccess && !initializedRef.current) {
      initialDecisionsRef.current = sortDecisions(decisionsRequest.value)
      initializedRef.current = true
    }
  }, [decisionsRequest])

  const displayedDecisions = useMemo(() => {
    if (!initializedRef.current) {
      return decisionsRequest.isSuccess
        ? sortDecisions(decisionsRequest.value)
        : []
    }

    return initialDecisionsRef.current.map((item) => {
      const localStatus = localDecisionStatuses.get(item.decision.id)
      if (localStatus) {
        return {
          ...item,
          decision: {
            ...item.decision,
            status: localStatus
          }
        }
      }
      return item
    })
  }, [decisionsRequest, localDecisionStatuses])

  const handleDecisionHandled = (
    decisionId: DecisionId,
    status: DecisionStatus
  ) => {
    setLocalDecisionStatuses((prev) => {
      const next = new Map(prev).set(decisionId, status)

      const handledDecision = displayedDecisions.find(
        ({ decision }) => decision.id === decisionId
      )
      if (handledDecision && status === 'REJECTED') {
        const connectedToReject = findRejectCascadedDecision(
          handledDecision,
          displayedDecisions
        )
        if (connectedToReject) {
          next.set(connectedToReject.decision.id, 'REJECTED')
        }
      }
      return next
    })
  }

  const [
    displayDecisionWithNoResponseWarning,
    setDisplayDecisionWithNoResponseWarning
  ] = useState<boolean>(false)

  useTitle(t, t.decisions.title)

  const unconfirmedDecisionsCount = displayedDecisions.filter(
    ({ decision: { status } }) => status === 'PENDING'
  ).length

  const handleReturnToPreviousPage = () => {
    const warnAboutMissingResponse =
      displayedDecisions.length > 1 &&
      !!displayedDecisions.find(
        ({ decision: { status } }) => status === 'PENDING'
      )

    if (warnAboutMissingResponse) {
      setDisplayDecisionWithNoResponseWarning(true)
    } else {
      navigate('/decisions')
    }
  }

  return (
    <>
      <Container>
        <Gap size="s" />
        <Button
          appearance="inline"
          text={t.decisions.applicationDecisions.returnToPreviousPage}
          onClick={handleReturnToPreviousPage}
          icon={faChevronLeft}
        />
        <Gap size="s" />
        <Main>
          <ContentArea opaque paddingVertical="s" paddingHorizontal="s">
            <H1 noMargin>
              {t.decisions.unconfirmedDecisions(unconfirmedDecisionsCount)}
            </H1>
            <Gap size="s" />
            <div>
              {unconfirmedDecisionsCount === 0
                ? t.decisions.applicationDecisions.allDecisionsConfirmed
                : t.decisions.applicationDecisions.summary}
            </div>
          </ContentArea>
          <Gap size="s" />
          {displayedDecisions.map((decision, i) => (
            <Fragment key={decision.decision.id}>
              <ContentArea opaque paddingVertical="s" paddingHorizontal="s">
                <DecisionResponse
                  decision={decision.decision}
                  permittedActions={new Set(decision.permittedActions)}
                  canDecide={decision.canDecide}
                  validRequestedStartDatePeriod={
                    decision.validRequestedStartDatePeriod
                  }
                  blocked={isDecisionBlocked(decision, displayedDecisions)}
                  rejectCascade={isRejectCascaded(decision, displayedDecisions)}
                  handleReturnToPreviousPage={handleReturnToPreviousPage}
                  headerCounter={`${i + 1}/${displayedDecisions.length}`}
                  onDecisionHandled={handleDecisionHandled}
                />
              </ContentArea>
              <Gap size="s" />
            </Fragment>
          ))}
          <Gap size="m" />
          {displayDecisionWithNoResponseWarning && (
            <InfoModal
              title={
                t.decisions.applicationDecisions.warnings
                  .decisionWithNoResponseWarning.title
              }
              icon={faExclamation}
              type="warning"
              text={
                t.decisions.applicationDecisions.warnings
                  .decisionWithNoResponseWarning.text
              }
              resolve={{
                label:
                  t.decisions.applicationDecisions.warnings
                    .decisionWithNoResponseWarning.resolveLabel,
                action: () => {
                  navigate('/decisions')
                }
              }}
              reject={{
                label:
                  t.decisions.applicationDecisions.warnings
                    .decisionWithNoResponseWarning.rejectLabel,
                action: () => {
                  setDisplayDecisionWithNoResponseWarning(false)
                }
              }}
            />
          )}
        </Main>
      </Container>
      <Footer />
    </>
  )
})

const sortDecisions = (
  decisions: DecisionWithValidStartDatePeriod[]
): DecisionWithValidStartDatePeriod[] =>
  orderBy(
    decisions,
    [
      ({ decision: { sentDate } }) => sentDate?.toSystemTzDate(),
      ({ decision: { type } }) => type
    ],
    ['desc', 'asc']
  )

const isDecisionBlocked = (
  { decision }: DecisionWithValidStartDatePeriod,
  allDecisions: DecisionWithValidStartDatePeriod[]
) =>
  (decision.type === 'PRESCHOOL_DAYCARE' ||
    decision.type === 'PRESCHOOL_CLUB') &&
  allDecisions.find(
    ({ decision: { type, status, applicationId } }) =>
      ['PRESCHOOL', 'PREPARATORY_EDUCATION'].includes(type) &&
      status !== 'ACCEPTED' &&
      applicationId === decision.applicationId
  ) !== undefined

const findRejectCascadedDecision = (
  { decision }: DecisionWithValidStartDatePeriod,
  allDecisions: DecisionWithValidStartDatePeriod[]
) =>
  ['PRESCHOOL', 'PREPARATORY_EDUCATION'].includes(decision.type)
    ? allDecisions.find(
        ({ decision: { type, status, applicationId } }) =>
          (type === 'PRESCHOOL_DAYCARE' || type === 'PRESCHOOL_CLUB') &&
          status === 'PENDING' &&
          applicationId === decision.applicationId
      )
    : undefined

const isRejectCascaded = (
  decision: DecisionWithValidStartDatePeriod,
  allDecisions: DecisionWithValidStartDatePeriod[]
) => findRejectCascadedDecision(decision, allDecisions) !== undefined
