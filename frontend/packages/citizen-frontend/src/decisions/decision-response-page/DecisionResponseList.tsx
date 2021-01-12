// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { getApplicationDecisions } from '~decisions/api'
import { UUID } from '@evaka/employee-frontend/src/types'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { Decision } from '~decisions/types'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import { H1, P } from '@evaka/lib-components/src/typography'
import { useTranslation } from '~localization'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import { faChevronLeft, faExclamation } from '@evaka/lib-icons'
import FormModal from '@evaka/lib-components/src/molecules/modals/FormModal'
import DecisionResponse from './DecisionResponse'
import { decisionOrderComparator } from '~decisions/shared'

const HorizontalLine = styled.hr`
  margin-block-start: ${defaultMargins.XL};
  margin-block-end: ${defaultMargins.XL};
  border: 1px solid ${colors.greyscale.lighter};
`

export default React.memo(function DecisionResponseList() {
  const { applicationId } = useParams<{ applicationId: UUID }>()
  const t = useTranslation()
  const router = useHistory()

  const [decisionsRequest, setDecisionsRequest] = useState<Result<Decision[]>>(
    Loading.of()
  )
  const [
    displayDecisionWithNoResponseWarning,
    setDisplayDecisionWithNoResponseWarning
  ] = useState<boolean>(false)

  const loadDecisions = useRestApi(getApplicationDecisions, setDecisionsRequest)
  useEffect(() => loadDecisions(applicationId), [applicationId])

  const unconfirmedDecisionsCount = decisionsRequest.isSuccess
    ? decisionsRequest.value.filter(({ status }) => status === 'PENDING').length
    : 0

  const handleReturnToPreviousPage = () => {
    const warnAboutMissingResponse =
      decisionsRequest.isSuccess &&
      decisionsRequest.value.length > 1 &&
      !!decisionsRequest.value.find((d) => d.status === 'PENDING')

    if (warnAboutMissingResponse) {
      setDisplayDecisionWithNoResponseWarning(true)
    } else {
      router.push('/decisions')
    }
  }

  return (
    <Container>
      <Gap size="s" />
      <InlineButton
        text={t.decisions.applicationDecisions.returnToPreviousPage}
        onClick={handleReturnToPreviousPage}
        icon={faChevronLeft}
      />
      <Gap size="s" />
      <ContentArea opaque>
        <H1>{t.decisions.title}</H1>
        {decisionsRequest.isLoading && <SpinnerSegment />}
        {decisionsRequest.isFailure && (
          <ErrorSegment
            title={t.decisions.applicationDecisions.errors.pageLoadError}
          />
        )}
        {decisionsRequest.isSuccess && (
          <div>
            <P width="800px">{t.decisions.applicationDecisions.summary}</P>
            {unconfirmedDecisionsCount > 0 ? (
              <AlertBox
                message={t.decisions.unconfirmedDecisions(
                  unconfirmedDecisionsCount
                )}
                thin
                data-qa="alert-box-unconfirmed-decisions-count"
              />
            ) : null}
            <Gap size="L" />
            {decisionsRequest.value
              .sort(decisionOrderComparator)
              .map((decision, i) => (
                <React.Fragment key={decision.id}>
                  <DecisionResponse
                    decision={decision}
                    blocked={isDecisionBlocked(
                      decision,
                      decisionsRequest.value
                    )}
                    rejectCascade={isRejectCascaded(
                      decision,
                      decisionsRequest.value
                    )}
                    refreshDecisionList={() => {
                      loadDecisions(applicationId)
                    }}
                    handleReturnToPreviousPage={handleReturnToPreviousPage}
                  />
                  {i < decisionsRequest.value.length - 1 ? (
                    <HorizontalLine />
                  ) : null}
                </React.Fragment>
              ))}
          </div>
        )}
        <Gap size="m" />
      </ContentArea>
      {displayDecisionWithNoResponseWarning && (
        <FormModal
          title={
            t.decisions.applicationDecisions.warnings
              .decisionWithNoResponseWarning.title
          }
          icon={faExclamation}
          iconColour={'orange'}
          text={
            t.decisions.applicationDecisions.warnings
              .decisionWithNoResponseWarning.text
          }
          resolve={{
            label:
              t.decisions.applicationDecisions.warnings
                .decisionWithNoResponseWarning.resolveLabel,
            action: () => {
              router.push('/decisions')
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
          size={'md'}
        />
      )}
    </Container>
  )
})

const isDecisionBlocked = (decision: Decision, allDecisions: Decision[]) =>
  decision.type === 'PRESCHOOL_DAYCARE' &&
  !allDecisions.find(
    (decision) =>
      ['PRESCHOOL', 'PREPARATORY_EDUCATION'].includes(decision.type) &&
      decision.status === 'ACCEPTED'
  )

const isRejectCascaded = (decision: Decision, allDecisions: Decision[]) =>
  ['PRESCHOOL', 'PREPARATORY_EDUCATION'].includes(decision.type) &&
  allDecisions.find(
    (d) => d.type === 'PRESCHOOL_DAYCARE' && d.status === 'PENDING'
  ) !== undefined
