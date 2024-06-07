// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { Failure } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Action } from 'lib-common/generated/action'
import { Decision } from 'lib-common/generated/api-types/decision'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import Radio from 'lib-components/atoms/form/Radio'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, H3, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faExclamation } from 'lib-icons'

import ModalAccessibilityWrapper from '../../ModalAccessibilityWrapper'
import { useLang, useTranslation } from '../../localization'
import { OverlayContext } from '../../overlay/state'
import { PdfLink } from '../PdfLink'
import { acceptDecisionMutation, rejectDecisionMutation } from '../queries'
import { decisionStatusIcon, Status } from '../shared'

interface SingleDecisionProps {
  decision: Decision
  validRequestedStartDatePeriod: FiniteDateRange
  blocked: boolean
  rejectCascade: boolean
  handleReturnToPreviousPage: () => void
  permittedActions: Set<Action.Citizen.Decision>
  canDecide: boolean
}

export default React.memo(function DecisionResponse({
  decision,
  validRequestedStartDatePeriod,
  blocked,
  rejectCascade,
  handleReturnToPreviousPage,
  permittedActions,
  canDecide
}: SingleDecisionProps) {
  const t = useTranslation()
  const [lang] = useLang()
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
  const [requestedStartDate, setRequestedStartDate] =
    useState<LocalDate | null>(startDate)
  const [submitting, setSubmitting] = useState<boolean>(false)
  const [displayCascadeWarning, setDisplayCascadeWarning] =
    useState<boolean>(false)
  const [dateErrorMessage, setDateErrorMessage] = useState<string>('')
  const { setErrorMessage } = useContext(OverlayContext)
  const getUnitName = () => {
    switch (decision.type) {
      case 'DAYCARE':
      case 'DAYCARE_PART_TIME':
        return decision.unit.daycareDecisionName
      case 'PRESCHOOL':
      case 'PREPARATORY_EDUCATION':
      case 'PRESCHOOL_DAYCARE':
      case 'PRESCHOOL_CLUB':
        return decision.unit.preschoolDecisionName
      case 'CLUB':
        return decision.unit.name
    }
  }

  const { mutateAsync: acceptDecision } = useMutationResult(
    acceptDecisionMutation
  )
  const { mutateAsync: rejectDecision } = useMutationResult(
    rejectDecisionMutation
  )

  const handleAcceptDecision = async () => {
    if (requestedStartDate === null) {
      return Failure.of({
        message: 'Missing field'
      })
    }
    setSubmitting(true)
    return acceptDecision({
      applicationId,
      body: {
        decisionId,
        requestedStartDate
      }
    })
  }

  const handleRejectDecision = async () => {
    setSubmitting(true)
    return rejectDecision({ applicationId, body: { decisionId } })
  }

  const onSubmit = async () => {
    if (acceptChecked) {
      return handleAcceptDecision()
    } else {
      return handleRejectDecision()
    }
  }

  const onSuccess = () => {
    setSubmitting(false)
  }

  const onFailure = () => {
    setErrorMessage({
      type: 'error',
      title: t.decisions.applicationDecisions.errors.submitFailure,
      resolveLabel: t.common.ok
    })
    setSubmitting(false)
  }

  useEffect(() => {
    if (requestedStartDate === null) {
      setDateErrorMessage(t.validationErrors.validDate)
    } else {
      setDateErrorMessage('')
    }
  }, [startDate, decisionType]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div data-qa={`decision-${decision.id}`}>
      <H2 data-qa="title-decision-type">
        {`${t.decisions.applicationDecisions.decision} ${
          t.decisions.applicationDecisions.type[decision.type]
        }`}
      </H2>
      <Gap size="xs" />
      {permittedActions.has('DOWNLOAD_PDF') && (
        <PdfLink decisionId={decision.id} />
      )}
      <Gap size="m" />
      <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
        <Label>{t.decisions.applicationDecisions.childName}</Label>
        <span data-qa="decision-child-name">{childName}</span>
        <Label>{t.decisions.applicationDecisions.unit}</Label>
        <span data-qa="decision-unit">{getUnitName()}</span>
        <Label>{t.decisions.applicationDecisions.period}</Label>
        <span data-qa="decision-period">
          {startDate.format()} - {endDate.format()}
        </span>
        <Label>{t.decisions.applicationDecisions.sentDate}</Label>
        <span data-qa="decision-sent-date">{sentDate?.format() ?? ''}</span>
        <Label>{t.decisions.applicationDecisions.statusLabel}</Label>
        <Status data-qa="decision-status">
          <RoundIcon
            content={decisionStatusIcon[status].icon}
            color={decisionStatusIcon[status].color}
            size="s"
          />
          <Gap size="xs" horizontal />
          {t.decisions.applicationDecisions.status[status]}
        </Status>
      </ListGrid>
      {decision.status === 'PENDING' && canDecide && (
        <>
          <Gap size="s" />
          <H3 fitted>{t.decisions.applicationDecisions.response.title}</H3>
          <Gap size="xs" />
          <FixedSpaceColumn>
            <FixedSpaceRow alignItems="center" spacing="xs">
              <Radio
                id={`${decision.id}-accept`}
                checked={acceptChecked}
                onChange={() => setAcceptChecked(true)}
                name={`${decision.id}-accept`}
                label={
                  <FixedSpaceFlexWrap horizontalSpacing="xs">
                    <div>
                      {t.decisions.applicationDecisions.response.accept1}
                    </div>
                    {['PRESCHOOL', 'PREPARATORY_EDUCATION'].includes(
                      decisionType
                    ) ? (
                      <div>{startDate.format()}</div>
                    ) : (
                      <DatePickerContainer onClick={(e) => e.stopPropagation()}>
                        <DatePicker
                          date={requestedStartDate}
                          onChange={(date) => setRequestedStartDate(date)}
                          minDate={validRequestedStartDatePeriod.start}
                          maxDate={validRequestedStartDatePeriod.end}
                          locale={lang}
                          info={
                            dateErrorMessage !== ''
                              ? {
                                  text: dateErrorMessage,
                                  status: 'warning'
                                }
                              : undefined
                          }
                        />
                      </DatePickerContainer>
                    )}
                    <div>
                      {t.decisions.applicationDecisions.response.accept2}
                    </div>
                  </FixedSpaceFlexWrap>
                }
                ariaLabel={`${
                  t.decisions.applicationDecisions.response.accept1
                } ${requestedStartDate?.format() ?? ''} ${
                  t.decisions.applicationDecisions.response.accept2
                }`}
                disabled={blocked || submitting}
                data-qa="radio-accept"
              />
            </FixedSpaceRow>
            <Radio
              id={`${decision.id}-reject`}
              checked={!acceptChecked}
              onChange={() => setAcceptChecked(false)}
              name={`${decision.id}-reject`}
              label={t.decisions.applicationDecisions.response.reject}
              disabled={blocked || submitting}
              data-qa="radio-reject"
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
          <ButtonContainer>
            <AsyncButton
              text={t.decisions.applicationDecisions.response.submit}
              primary
              onClick={() => {
                if (!acceptChecked && rejectCascade) {
                  setDisplayCascadeWarning(true)
                  return
                } else {
                  return onSubmit()
                }
              }}
              onSuccess={onSuccess}
              onFailure={onFailure}
              disabled={
                blocked ||
                (dateErrorMessage !== '' && acceptChecked) ||
                submitting
              }
              data-qa="submit-response"
            />
            <LegacyButton
              text={t.decisions.applicationDecisions.response.cancel}
              onClick={handleReturnToPreviousPage}
              disabled={blocked || submitting}
            />
          </ButtonContainer>
        </>
      )}

      {displayCascadeWarning && (
        <ModalAccessibilityWrapper>
          <AsyncFormModal
            title={
              t.decisions.applicationDecisions.warnings.doubleRejectWarning
                .title
            }
            icon={faExclamation}
            type="warning"
            text={
              t.decisions.applicationDecisions.warnings.doubleRejectWarning.text
            }
            resolveLabel={
              t.decisions.applicationDecisions.warnings.doubleRejectWarning
                .resolveLabel
            }
            resolveAction={onSubmit}
            onSuccess={() => {
              setDisplayCascadeWarning(false)
              onSuccess()
            }}
            onFailure={onFailure}
            rejectLabel={
              t.decisions.applicationDecisions.warnings.doubleRejectWarning
                .rejectLabel
            }
            rejectAction={() => setDisplayCascadeWarning(false)}
            data-qa="cascade-warning-modal"
          />
        </ModalAccessibilityWrapper>
      )}
    </div>
  )
})

const DatePickerContainer = styled.div`
  margin-top: -${defaultMargins.s};
`
