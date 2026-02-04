// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { Failure } from 'lib-common/api'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type { Action } from 'lib-common/generated/action'
import type {
  Decision,
  DecisionStatus
} from 'lib-common/generated/api-types/decision'
import type { DecisionId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import IconChip from 'lib-components/atoms/IconChip'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import Radio from 'lib-components/atoms/form/Radio'
import { tabletMin } from 'lib-components/breakpoints'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, H3, H4, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faExclamation } from 'lib-icons'

import ModalAccessibilityWrapper from '../../ModalAccessibilityWrapper'
import { useLang, useTranslation } from '../../localization'
import { OverlayContext } from '../../overlay/state'
import { PdfLink } from '../PdfLink'
import { acceptDecisionMutation, rejectDecisionMutation } from '../queries'
import { iconPropsByStatus } from '../shared'

interface SingleDecisionProps {
  decision: Decision
  validRequestedStartDatePeriod: FiniteDateRange
  blocked: boolean
  rejectCascade: boolean
  handleReturnToPreviousPage: () => void
  permittedActions: Set<Action.Citizen.Decision>
  headerCounter: string
  onDecisionHandled: (decisionId: DecisionId, status: DecisionStatus) => void
}

export default React.memo(function DecisionResponse({
  decision,
  validRequestedStartDatePeriod,
  blocked,
  rejectCascade,
  handleReturnToPreviousPage,
  permittedActions,
  headerCounter,
  onDecisionHandled
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
    onDecisionHandled(decisionId, acceptChecked ? 'ACCEPTED' : 'REJECTED')
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
      <DecisionHeaderRow alignItems="center" spacing="s">
        <FirstColumnLabel>
          <Label>
            {t.decisions.applicationDecisions.decision} {headerCounter}
          </Label>
        </FirstColumnLabel>
        <IconChip
          {...iconPropsByStatus[status]}
          label={t.decisions.applicationDecisions.status[status]}
          data-qa="decision-status"
        />
      </DecisionHeaderRow>
      <ThinHorizontalLine />
      <H4 data-qa="decision-child-name" translate="no" noMargin>
        {childName}
      </H4>
      <Gap size="xxs" />
      <H2 data-qa="title-decision-type" noMargin>
        {t.decisions.applicationDecisions.type[decision.type]}
      </H2>
      <ThinHorizontalLine />
      <DataTable>
        <H3 noMargin>{t.decisions.applicationDecisions.data}</H3>
        {permittedActions.has('DOWNLOAD_PDF') && (
          <PdfLink decisionId={decision.id} />
        )}
        <FixedSpaceColumn spacing="s">
          <div>
            <Label>{t.decisions.applicationDecisions.unit}</Label>
            <Gap size="xxs" />
            <div data-qa="decision-unit" translate="no">
              {getUnitName()}
            </div>
          </div>
          <div>
            <Label>{t.decisions.applicationDecisions.period}</Label>
            <Gap size="xxs" />
            <div data-qa="decision-period">
              {startDate.format()} - {endDate.format()}
            </div>
          </div>
          <div>
            <Label>{t.decisions.applicationDecisions.sentDate}</Label>
            <Gap size="xxs" />
            <div data-qa="decision-sent-date">{sentDate?.format() ?? ''}</div>
          </div>
        </FixedSpaceColumn>
      </DataTable>
      {decision.status === 'PENDING' && (
        <Fragment>
          <ThinHorizontalLine />
          <DecisionListGrid
            labelWidth="144px"
            columnGap="s"
            mobileMaxWidth={tabletMin}
          >
            <H3 noMargin id={`decision-${decision.id}-confirmation`}>
              {t.decisions.applicationDecisions.confirmation}
            </H3>
            <FixedSpaceColumn>
              {blocked && (
                <InfoBoxWrapper>
                  <InfoBox
                    message={
                      t.decisions.applicationDecisions.response.disabledInfo
                    }
                    noMargin
                  />
                </InfoBoxWrapper>
              )}
              <div
                role="group"
                aria-labelledby={`decision-${decision.id}-confirmation`}
              >
                <FixedSpaceRow alignItems="center" spacing="xs">
                  <Radio
                    id={`${decision.id}-accept`}
                    checked={acceptChecked}
                    onChange={() => setAcceptChecked(true)}
                    name={`${decision.id}-confirmation`}
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
                          <DatePickerContainer
                            onClick={(e) => e.stopPropagation()}
                          >
                            <DatePicker
                              date={requestedStartDate}
                              onChange={(date) => setRequestedStartDate(date)}
                              minDate={validRequestedStartDatePeriod.start}
                              maxDate={validRequestedStartDatePeriod.end}
                              locale={lang}
                              disabled={blocked || submitting}
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
                <Gap size="s" />
                <Radio
                  id={`${decision.id}-reject`}
                  checked={!acceptChecked}
                  onChange={() => setAcceptChecked(false)}
                  name={`${decision.id}-confirmation`}
                  label={t.decisions.applicationDecisions.response.reject}
                  disabled={blocked || submitting}
                  data-qa="radio-reject"
                />
              </div>
              <LeftAlignedButtonsContainer>
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
                <Button
                  text={t.decisions.applicationDecisions.response.cancel}
                  onClick={handleReturnToPreviousPage}
                  disabled={blocked || submitting}
                />
              </LeftAlignedButtonsContainer>
            </FixedSpaceColumn>
          </DecisionListGrid>
        </Fragment>
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
const ThinHorizontalLine = styled(HorizontalLine)`
  margin-block: ${defaultMargins.s};
`
const DecisionListGrid = styled(ListGrid)`
  @media (max-width: ${tabletMin}) {
    row-gap: ${defaultMargins.s};

    > *:nth-child(2n):last-child {
      margin-bottom: 0;
    }
  }
`
const InfoBoxWrapper = styled.div`
  margin-bottom: ${defaultMargins.s};
`
const DataTable = styled.div`
  display: grid;
  gap: ${defaultMargins.s};
  grid-column: 1 / -1;
  align-items: start;
  grid-template-columns: 144px 1fr auto;

  > :first-child {
    grid-column: 1;
    grid-row: 1;
  }

  > :nth-child(2) {
    grid-column: 3;
    grid-row: 1;
  }

  > :nth-child(3) {
    grid-column: 2;
    grid-row: 1;
  }

  @media (max-width: ${tabletMin}) {
    grid-template-columns: 1fr auto;

    > :nth-child(2) {
      grid-column: 2;
      grid-row: 1;
    }

    > :nth-child(3) {
      grid-column: 1 / -1;
      grid-row: 2;
    }
  }
`
const FirstColumnLabel = styled.div`
  width: 144px;
`
const DecisionHeaderRow = styled(FixedSpaceRow)`
  @media (max-width: ${tabletMin}) {
    justify-content: space-between;
  }
`
const LeftAlignedButtonsContainer = styled.div`
  display: flex;
  justify-content: flex-start;
  gap: ${defaultMargins.s};
  @media (max-width: ${tabletMin}) {
    flex-direction: column;
  }
`
