// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Decision } from '~decisions/types'
import React, { useContext, useState } from 'react'
import { useTranslation } from '~localization'
import LocalDate from '@evaka/lib-common/src/local-date'
import { OverlayContext } from '~overlay/state'
import { H2, H3, Label, P } from '@evaka/lib-components/src/typography'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import { DatePicker } from '@evaka/lib-components/src/molecules/DatePicker'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { acceptDecision, rejectDecision } from '~decisions/api'
import { PdfLink } from '~decisions/PdfLink'
import { Status, statusIcon } from '~decisions/shared'
import { AsyncFormModal } from '@evaka/lib-components/src/molecules/modals/FormModal'
import { faExclamation } from '@evaka/lib-icons'
import styled from 'styled-components'

interface SingleDecisionProps {
  decision: Decision
  blocked: boolean
  rejectCascade: boolean
  refreshDecisionList: () => void
  handleReturnToPreviousPage: () => void
}

export default React.memo(function DecisionResponse({
  decision,
  blocked,
  rejectCascade,
  refreshDecisionList,
  handleReturnToPreviousPage
}: SingleDecisionProps) {
  const t = useTranslation()
  const {
    id: decisionId,
    applicationId,
    childName,
    sentDate,
    status,
    startDate,
    endDate,
    type: decisionType
  } = decision
  const [acceptChecked, setAcceptChecked] = useState<boolean>(true)
  const [requestedStartDate, setRequestedStartDate] = useState<LocalDate>(
    startDate
  )
  const [submitting, setSubmitting] = useState<boolean>(false)
  const [displayCascadeWarning, setDisplayCascadeWarning] = useState<boolean>(
    false
  )
  const { setErrorMessage } = useContext(OverlayContext)
  const getUnitName = () => {
    switch (decision.type) {
      case 'DAYCARE':
      case 'DAYCARE_PART_TIME':
        return decision.unit.daycareDecisionName
      case 'PRESCHOOL':
      case 'PREPARATORY_EDUCATION':
      case 'PRESCHOOL_DAYCARE':
        return decision.unit.preschoolDecisionName
      case 'CLUB':
        return decision.unit.name
    }
  }

  const onSubmit = async () => {
    setSubmitting(true)
    return (acceptChecked
      ? acceptDecision(applicationId, decisionId, requestedStartDate)
      : rejectDecision(applicationId, decisionId)
    ).finally(() => {
      setSubmitting(false)
      refreshDecisionList()
    })
  }

  return (
    <React.Fragment>
      <H2 data-qa="title-decision-type">
        {`${t.decisions.applicationDecisions.decision} ${
          t.decisions.applicationDecisions.type[decision.type]
        }`}
      </H2>
      <Gap size="xs" />
      <PdfLink decisionId={decision.id} />
      <Gap size="m" />
      <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
        <Label>{t.decisions.applicationDecisions.childName}</Label>
        <span data-qa="decision-sent-date">{childName}</span>
        <Label>{t.decisions.applicationDecisions.unit}</Label>
        <span data-qa="decision-sent-date">{getUnitName()}</span>
        <Label>{t.decisions.applicationDecisions.period}</Label>
        <span data-qa="decision-sent-date">
          {startDate.format()} - {endDate.format()}
        </span>
        <Label>{t.decisions.applicationDecisions.sentDate}</Label>
        <span data-qa="decision-sent-date">{sentDate.format()}</span>
        <Label>{t.decisions.applicationDecisions.statusLabel}</Label>
        <Status data-qa={`decision-status-${decisionId}`}>
          <RoundIcon
            content={statusIcon[status].icon}
            color={statusIcon[status].color}
            size="s"
          />
          <Gap size="xs" horizontal />
          {t.decisions.applicationDecisions.status[status]}
        </Status>
      </ListGrid>
      {decision.status === 'PENDING' && (
        <>
          <Gap size="L" />
          <H3 data-qa="title-decision-type" fitted>
            {t.decisions.applicationDecisions.response.title}
          </H3>
          <Gap size="xs" />
          <FixedSpaceColumn>
            <FixedSpaceRow alignItems="center" spacing="xs">
              <Radio
                checked={acceptChecked}
                onChange={() => setAcceptChecked(true)}
                name={`${decision.id}-accept`}
                label={t.decisions.applicationDecisions.response.accept1}
                disabled={blocked || submitting}
              />
              <DatePicker
                date={requestedStartDate}
                onChange={setRequestedStartDate}
                type="short"
                minDate={startDate}
                maxDate={
                  ['PRESCHOOL', 'PREPARATORY_EDUCATION'].includes(decisionType)
                    ? startDate
                    : startDate.addDays(14)
                }
              />
              <div>{t.decisions.applicationDecisions.response.accept2}</div>
            </FixedSpaceRow>
            <Radio
              checked={!acceptChecked}
              onChange={() => setAcceptChecked(false)}
              name={`${decision.id}-reject`}
              label={t.decisions.applicationDecisions.response.reject}
              disabled={blocked || submitting}
            />
          </FixedSpaceColumn>
          {blocked ? (
            <>
              <Gap size="s" />
              <P width="800px">
                {t.decisions.applicationDecisions.response.disabledInfo}
              </P>
              <Gap size="s" />
            </>
          ) : (
            <Gap size="L" />
          )}
          <FixedSpaceRow>
            <ButtonContainer>
              <SingleButtonContainer>
                <Button
                  text={t.decisions.applicationDecisions.response.cancel}
                  onClick={handleReturnToPreviousPage}
                  disabled={blocked || submitting}
                />
              </SingleButtonContainer>
              <SingleButtonContainer>
                <Button
                  text={t.decisions.applicationDecisions.response.submit}
                  primary
                  onClick={() => {
                    if (!acceptChecked && rejectCascade) {
                      setDisplayCascadeWarning(true)
                    } else {
                      void onSubmit().then((res) => {
                        if (res.isFailure) {
                          setErrorMessage({
                            type: 'error',
                            title:
                              t.decisions.applicationDecisions.errors
                                .submitFailure
                          })
                        }
                      })
                    }
                  }}
                  disabled={blocked || submitting}
                />
              </SingleButtonContainer>
            </ButtonContainer>
          </FixedSpaceRow>
        </>
      )}

      {displayCascadeWarning && (
        <AsyncFormModal
          title={
            t.decisions.applicationDecisions.warnings.doubleRejectWarning.title
          }
          icon={faExclamation}
          iconColour={'orange'}
          text={
            t.decisions.applicationDecisions.warnings.doubleRejectWarning.text
          }
          resolve={{
            label:
              t.decisions.applicationDecisions.warnings.doubleRejectWarning
                .resolveLabel,
            action: onSubmit,
            onSuccess: () => setDisplayCascadeWarning(false)
          }}
          reject={{
            label:
              t.decisions.applicationDecisions.warnings.doubleRejectWarning
                .rejectLabel,
            action: () => setDisplayCascadeWarning(false)
          }}
          size={'md'}
        />
      )}
    </React.Fragment>
  )
})

const ButtonContainer = styled.div`
  display: flex;
  width: 100%;
  flex-wrap: wrap-reverse;
  margin: -${defaultMargins['s']} 0 0 0;
`

const SingleButtonContainer = styled.div`
  margin: ${defaultMargins['s']} 0 0 ${defaultMargins['s']};
`
