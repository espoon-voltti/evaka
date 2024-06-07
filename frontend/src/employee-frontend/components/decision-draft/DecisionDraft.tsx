// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, {
  Fragment,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { NavigateFunction, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result, wrapResult } from 'lib-common/api'
import { DecisionDraftGroup } from 'lib-common/generated/api-types/application'
import {
  DecisionDraft,
  DecisionDraftUpdate,
  DecisionType,
  DecisionUnit
} from 'lib-common/generated/api-types/decision'
import useRouteParams from 'lib-common/useRouteParams'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { faEnvelope } from 'lib-icons'

import LabelValueList from '../../components/common/LabelValueList'
import {
  getDecisionDrafts,
  updateDecisionDrafts
} from '../../generated/api-clients/application'
import { getDecisionUnits } from '../../generated/api-clients/decision'
import { Translations, useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { formatName } from '../../utils'

const getDecisionUnitsResult = wrapResult(getDecisionUnits)
const updateDecisionDraftsResult = wrapResult(updateDecisionDrafts)
const getDecisionDraftsResult = wrapResult(getDecisionDrafts)

const ColumnTitle = styled.div`
  font-weight: ${fontWeights.semibold};
  margin-bottom: 1em;
`

const DateRangeContainer = styled.span`
  display: flex;
  margin-bottom: 0.25em;
`

const DateRangeSpacer = styled.span`
  margin: auto 15px;
`

const UnitSelectContainer = styled.div`
  .input {
    border: none;
    margin: 0 -12px;
  }
`

const Heading = styled(Title)`
  margin-top: 0.5rem;
`

const WarningContainer = styled.div<{ visible: boolean }>`
  visibility: ${({ visible }) => (visible ? 'visible' : 'hidden')};
`

const WarningText = styled.span<{ smaller?: boolean }>`
  color: ${colors.grayscale.g70};
  font-style: italic;
  margin-left: 12px;
  font-size: ${(props) => (props.smaller === true ? '0.8em' : '1em')};
`

const WarningIcon = () => (
  <FontAwesomeIcon
    icon={faExclamationTriangle}
    size="1x"
    color={colors.status.warning}
  />
)

const MissingValuePlaceholder = React.memo(function MissingValuePlaceholder({
  i18n,
  values
}: {
  i18n: Translations
  values: string[]
}) {
  if (values.every((v) => !v)) {
    return (
      <>
        <WarningIcon />
        <WarningText>{i18n.decisionDraft.missingValue}</WarningText>
      </>
    )
  } else {
    return <span>{values.join(' ')}</span>
  }
})

const DefaultValuePlaceholder = React.memo(function DefaultValuePlaceholder({
  i18n,
  values
}: {
  i18n: Translations
  values: string[]
}) {
  if (values.every((v) => !v)) {
    return <span>{i18n.decisionDraft.noOtherGuardian}</span>
  } else {
    return <span>{values.join(' ')}</span>
  }
})

const SendButtonContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  margin-top: 40px;
`

const InfoContainer = styled.div`
  margin-bottom: 40px;
`

const InfoParagraph = styled.p`
  margin-bottom: 24px;
`

const decisionTypesRequiringDaycareDecisionName: DecisionType[] = [
  'DAYCARE',
  'DAYCARE_PART_TIME'
]

const decisionTypesRequiringPreschoolDecisionName: DecisionType[] = [
  'PRESCHOOL',
  'PREPARATORY_EDUCATION',
  'PRESCHOOL_DAYCARE',
  'PRESCHOOL_CLUB'
]

function redirectToMainPage(navigate: NavigateFunction) {
  navigate('/applications')
}

export default React.memo(function Decision() {
  const { id: applicationId } = useRouteParams(['id'])
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const [decisionDraftGroup, setDecisionDraftGroup] = useState<
    Result<DecisionDraftGroup>
  >(Loading.of())
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)
  const [decisions, setDecisions] = useState<DecisionDraft[]>([])
  const [units, setUnits] = useState<Result<DecisionUnit[]>>(Loading.of())

  useEffect(() => {
    setDecisionDraftGroup(Loading.of())
    void getDecisionDraftsResult({ applicationId: applicationId }).then(
      (result) => {
        setDecisionDraftGroup(result)
        if (result.isSuccess) {
          setDecisions(result.value.decisions)
        }

        // Application has already changed its status
        if (result.isFailure && result.statusCode === 409) {
          redirectToMainPage(navigate)
        }
      }
    )
  }, [applicationId, navigate])

  useEffect(() => {
    if (decisionDraftGroup.isSuccess) {
      const name = formatTitleName(
        decisionDraftGroup.value.child.firstName,
        decisionDraftGroup.value.child.lastName
      )
      setTitle(`${name} | ${i18n.titles.decision}`)
    }
  }, [decisionDraftGroup, formatTitleName, i18n.titles.decision, setTitle])

  useEffect(() => {
    void getDecisionUnitsResult().then(setUnits)
  }, [setUnits])

  const unitOptions = useMemo(
    (): DecisionUnit[] => units.getOrElse([]),
    [units]
  )

  function updateState(
    type: DecisionType | null,
    values: Partial<DecisionDraft>
  ) {
    setDecisions(
      decisions.map((decision) =>
        decision.type === type || type === null
          ? {
              ...decision,
              ...values
            }
          : decision
      )
    )
  }

  const minDate = (type: DecisionType) =>
    type === 'PREPARATORY_EDUCATION'
      ? decisions.find(({ type }) => type === 'PRESCHOOL')?.startDate
      : undefined

  const maxDate = (type: DecisionType) =>
    type === 'PREPARATORY_EDUCATION'
      ? decisions.find(({ type }) => type === 'PRESCHOOL')?.endDate
      : undefined

  const requiresDaycareDecisionName = decisions.some(({ type }) =>
    decisionTypesRequiringDaycareDecisionName.includes(type)
  )

  const requiresPreschoolDecisionName = decisions.some(({ type }) =>
    decisionTypesRequiringPreschoolDecisionName.includes(type)
  )

  const isClubDecision = decisions.some(({ type }) => type === 'CLUB')

  const unitDataIsComplete = decisions.map((decision) => {
    const selectedUnit = unitOptions.find(
      (unitOption) => unitOption.id === decision.unitId
    )
    return (
      !!selectedUnit &&
      !!(!requiresDaycareDecisionName || selectedUnit.daycareDecisionName) &&
      !!(
        !requiresPreschoolDecisionName || selectedUnit.preschoolDecisionName
      ) &&
      !!selectedUnit.manager &&
      !!selectedUnit.streetAddress &&
      !!selectedUnit.postalCode &&
      !!selectedUnit.postOffice &&
      (isClubDecision ||
        (!!selectedUnit.decisionHandler &&
          !!selectedUnit.decisionHandlerAddress))
    )
  })

  const noDecisionsPlanned =
    decisions.filter(({ planned }) => planned).length === 0

  return (
    <Container>
      <ContentArea opaque>
        <Title size={1}>{i18n.decisionDraft.title}</Title>
        <InfoContainer>
          <InfoParagraph>{i18n.decisionDraft.info1}</InfoParagraph>
          <InfoParagraph>{i18n.decisionDraft.info2}</InfoParagraph>
        </InfoContainer>
        {decisionDraftGroup.isFailure && <p>{i18n.common.loadingFailed}</p>}
        {decisionDraftGroup.isLoading && <Loader />}
        {decisionDraftGroup.isSuccess && (
          <Fragment>
            <Title size={3}>
              {formatName(
                decisionDraftGroup.value.child.firstName,
                decisionDraftGroup.value.child.lastName,
                i18n,
                true
              )}
            </Title>
            <LabelValueList
              spacing="large"
              contents={[
                {
                  label: i18n.decisionDraft.placementUnit,
                  value: decisionDraftGroup.value.placementUnitName
                },
                {
                  label: i18n.decisionDraft.receiver,
                  value: (
                    <MissingValuePlaceholder
                      i18n={i18n}
                      values={[
                        decisionDraftGroup.value.guardian.firstName,
                        decisionDraftGroup.value.guardian.lastName
                      ]}
                    />
                  ),
                  dataQa: 'guardian-name'
                },
                ...(!isClubDecision
                  ? [
                      {
                        label: i18n.decisionDraft.otherReceiver,
                        value: (
                          <DefaultValuePlaceholder
                            i18n={i18n}
                            values={[
                              decisionDraftGroup.value.otherGuardian
                                ? decisionDraftGroup.value.otherGuardian
                                    .firstName
                                : '',
                              decisionDraftGroup.value.otherGuardian
                                ? decisionDraftGroup.value.otherGuardian
                                    .lastName
                                : ''
                            ]}
                          />
                        ),
                        dataQa: 'other-guardian-name'
                      }
                    ]
                  : []),
                { label: null, value: null },
                {
                  label: (
                    <ColumnTitle>
                      {i18n.decisionDraft.decisionLabelHeading}
                    </ColumnTitle>
                  ),
                  value: (
                    <ColumnTitle>
                      {i18n.decisionDraft.decisionValueHeading}
                    </ColumnTitle>
                  )
                },
                ...decisions.flatMap((decision, index) => {
                  const selectedUnit = unitOptions.find(
                    (unitOption) => unitOption.id === decision.unitId
                  )
                  return [
                    {
                      label: (
                        <Checkbox
                          label={
                            i18n.decisionDraft.types[
                              decisionTypeForLabel(decision.type, decisions)
                            ]
                          }
                          checked={decision.planned}
                          onChange={(planned: boolean) => {
                            updateState(decision.type, { planned })
                          }}
                          data-qa={`toggle-service-need-part-day-${decision.type}`}
                        />
                      ),
                      value: (
                        <DateRangeContainer>
                          <DatePickerDeprecated
                            date={decision.startDate}
                            type="full-width"
                            disabled={decision.type !== 'PREPARATORY_EDUCATION'}
                            onChange={(startDate) =>
                              updateState(decision.type, { startDate })
                            }
                            minDate={minDate(decision.type)}
                            maxDate={maxDate(decision.type)}
                          />
                          <DateRangeSpacer>-</DateRangeSpacer>
                          <DatePickerDeprecated
                            date={decision.endDate}
                            disabled={decision.type !== 'PREPARATORY_EDUCATION'}
                            type="full-width"
                            onChange={(endDate) =>
                              updateState(decision.type, { endDate })
                            }
                            minDate={minDate(decision.type)}
                            maxDate={maxDate(decision.type)}
                          />
                        </DateRangeContainer>
                      )
                    },
                    ...(featureFlags.decisionDraftMultipleUnits ||
                    index === decisions.length - 1
                      ? [
                          {
                            label: i18n.decisionDraft.selectedUnit,
                            value: (
                              <UnitSelectContainer>
                                {units.isSuccess && (
                                  <Combobox
                                    onChange={(unit) =>
                                      unit
                                        ? updateState(
                                            featureFlags.decisionDraftMultipleUnits
                                              ? decision.type
                                              : null,
                                            {
                                              unitId: unit.id
                                            }
                                          )
                                        : undefined
                                    }
                                    items={unitOptions}
                                    selectedItem={selectedUnit}
                                    getItemLabel={(unit) => unit?.name ?? ''}
                                    getItemDataQa={(unit) => unit?.id ?? ''}
                                    data-qa={`unit-selector-${decision.type}`}
                                  />
                                )}
                                <WarningContainer
                                  visible={
                                    decisionDraftGroup.value.unit.id !==
                                    decision.unitId
                                  }
                                >
                                  <WarningIcon />
                                  <WarningText smaller>
                                    {i18n.decisionDraft.differentUnit}
                                  </WarningText>
                                </WarningContainer>
                              </UnitSelectContainer>
                            )
                          },
                          ...(selectedUnit
                            ? [
                                {
                                  value: (
                                    <Heading size={4}>
                                      {i18n.decisionDraft.unitDetailsHeading}
                                    </Heading>
                                  )
                                },
                                {
                                  label: null,
                                  value: (
                                    <LabelValueList
                                      spacing="small"
                                      contents={[
                                        ...(requiresDaycareDecisionName
                                          ? [
                                              {
                                                label:
                                                  i18n.decisionDraft
                                                    .daycareDecisionName,
                                                value: (
                                                  <MissingValuePlaceholder
                                                    i18n={i18n}
                                                    values={[
                                                      selectedUnit.daycareDecisionName
                                                    ]}
                                                  />
                                                )
                                              }
                                            ]
                                          : []),
                                        ...(requiresPreschoolDecisionName
                                          ? [
                                              {
                                                label:
                                                  i18n.decisionDraft
                                                    .preschoolDecisionName,
                                                value: (
                                                  <MissingValuePlaceholder
                                                    i18n={i18n}
                                                    values={[
                                                      selectedUnit.preschoolDecisionName
                                                    ]}
                                                  />
                                                )
                                              }
                                            ]
                                          : []),
                                        {
                                          label: i18n.decisionDraft.unitManager,
                                          value: (
                                            <MissingValuePlaceholder
                                              i18n={i18n}
                                              values={[
                                                selectedUnit.manager || ''
                                              ]}
                                            />
                                          )
                                        },
                                        {
                                          label: i18n.decisionDraft.unitAddress,
                                          value: (
                                            <MissingValuePlaceholder
                                              i18n={i18n}
                                              values={[
                                                selectedUnit.streetAddress,
                                                selectedUnit.postalCode,
                                                selectedUnit.postOffice
                                              ]}
                                            />
                                          )
                                        },
                                        ...(!isClubDecision
                                          ? [
                                              {
                                                label:
                                                  i18n.decisionDraft
                                                    .handlerName,
                                                value: (
                                                  <MissingValuePlaceholder
                                                    i18n={i18n}
                                                    values={[
                                                      selectedUnit.decisionHandler
                                                    ]}
                                                  />
                                                )
                                              },
                                              {
                                                label:
                                                  i18n.decisionDraft
                                                    .handlerAddress,
                                                value: (
                                                  <MissingValuePlaceholder
                                                    i18n={i18n}
                                                    values={[
                                                      selectedUnit.decisionHandlerAddress
                                                    ]}
                                                  />
                                                )
                                              }
                                            ]
                                          : []),
                                        {
                                          value: !unitDataIsComplete[index] && (
                                            <AlertBox
                                              title={
                                                i18n.decisionDraft.unitInfo1
                                              }
                                              message={
                                                i18n.decisionDraft.unitInfo2
                                              }
                                            />
                                          ),
                                          onlyValue: true
                                        }
                                      ]}
                                    />
                                  )
                                }
                              ]
                            : [])
                        ]
                      : [])
                  ]
                })
              ]}
            />
            {!(
              decisionDraftGroup.value.guardian.ssn &&
              decisionDraftGroup.value.child.ssn
            ) && (
              <InfoBox
                title={i18n.decisionDraft.ssnInfo1}
                message={i18n.decisionDraft.ssnInfo2}
                icon={faEnvelope}
              />
            )}

            {!decisionDraftGroup.value.guardian.isVtjGuardian && (
              <AlertBox
                title={i18n.decisionDraft.notGuardianInfo1}
                message={i18n.decisionDraft.notGuardianInfo2}
              />
            )}
            <SendButtonContainer>
              <FixedSpaceRow>
                <LegacyButton
                  data-qa="cancel-decisions-button"
                  onClick={() => redirectToMainPage(navigate)}
                  text={i18n.common.cancel}
                />
                <AsyncButton
                  primary
                  data-qa="save-decisions-button"
                  disabled={
                    unitDataIsComplete.some((complete) => !complete) ||
                    noDecisionsPlanned
                  }
                  onClick={() => {
                    const updatedDrafts: DecisionDraftUpdate[] = decisions.map(
                      (decisionDraft) => ({
                        id: decisionDraft.id,
                        unitId: decisionDraft.unitId,
                        startDate: decisionDraft.startDate,
                        endDate: decisionDraft.endDate,
                        planned: decisionDraft.planned
                      })
                    )
                    return updateDecisionDraftsResult({
                      applicationId,
                      body: updatedDrafts
                    })
                  }}
                  onSuccess={() => redirectToMainPage(navigate)}
                  text={i18n.common.save}
                />
              </FixedSpaceRow>
            </SendButtonContainer>
          </Fragment>
        )}
      </ContentArea>
    </Container>
  )
})

/*
  Since there is no separate decision type for preparatory daycare but it needs
  its own label, infer it from existence of a preparatory decision
*/
const decisionTypeForLabel = (
  type: DecisionType,
  decisions: DecisionDraft[]
) =>
  (type === 'PRESCHOOL_DAYCARE' || type === 'PRESCHOOL_CLUB') &&
  decisions.some((d) => d.type === 'PREPARATORY_EDUCATION')
    ? 'PREPARATORY_DAYCARE'
    : type
