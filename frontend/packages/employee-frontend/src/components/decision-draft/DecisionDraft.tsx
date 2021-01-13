// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  memo,
  useCallback,
  useEffect,
  useMemo,
  useState,
  Fragment,
  useContext
} from 'react'
import { RouteComponentProps, useHistory } from 'react-router'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import Title from '@evaka/lib-components/src/atoms/Title'
import { Gap } from '@evaka/lib-components/src/white-space'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import {
  getDecisionDrafts,
  getDecisionUnits,
  updateDecisionDrafts
} from '~api/decision-draft'
import { DecisionDraftState, DecisionDraftContext } from '~state/decision'
import { useTranslation, Translations } from '~state/i18n'
import { UUID } from '~types'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { DatePicker } from '@evaka/lib-components/src/molecules/DatePicker'
import LabelValueList from '~components/common/LabelValueList'
import Select from '~components/common/Select'
import {
  DecisionDraft,
  DecisionDraftUpdate,
  DecisionType,
  DecisionUnit
} from '~types/decision'
import { faEnvelope } from '@evaka/lib-icons'
import {
  AlertBox,
  InfoBox
} from '@evaka/lib-components/src/molecules/MessageBoxes'
import { TitleContext, TitleState } from '~state/title'
import { formatName } from '~utils'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import colors from '@evaka/lib-components/src/colors'

const ColumnTitle = styled.div`
  font-weight: 600;
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
  color: ${colors.greyscale.dark};
  font-style: italic;
  margin-left: 12px;
  font-size: ${(props) => (props.smaller === true ? '0.8em' : '1em')};
`

const WarningIcon = () => (
  <FontAwesomeIcon
    icon={faExclamationTriangle}
    size={'1x'}
    color={colors.accents.orange}
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
  'PRESCHOOL_DAYCARE'
]

const Decision = memo(function Decision({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { id: applicationId } = match.params
  const { i18n } = useTranslation()
  const history = useHistory()
  const { decisionDraftGroup, setDecisionDraftGroup } = useContext<
    DecisionDraftState
  >(DecisionDraftContext)
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)
  const [decisions, setDecisions] = useState<DecisionDraft[]>([])
  const [selectedUnit, setSelectedUnit] = useState<DecisionUnit>()
  const [units, setUnits] = useState<Result<DecisionUnit[]>>(Loading.of())
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    setDecisionDraftGroup(Loading.of())
    void getDecisionDrafts(applicationId).then((result) => {
      setDecisionDraftGroup(result)
      if (result.isSuccess) {
        setDecisions(result.value.decisions)
        setSelectedUnit(result.value.unit)
      }
    })
  }, [applicationId])

  useEffect(() => {
    if (decisionDraftGroup.isSuccess) {
      const name = formatTitleName(
        decisionDraftGroup.value.child.firstName,
        decisionDraftGroup.value.child.lastName
      )
      setTitle(`${name} | ${i18n.titles.decision}`)
    }
  }, [decisionDraftGroup])

  useEffect(() => {
    void getDecisionUnits().then(setUnits)
  }, [setUnits])

  const unitOptions = useMemo(
    () =>
      units
        .map((us) =>
          us.map(({ id, name }) => ({
            value: id,
            label: name
          }))
        )
        .getOrElse([]),
    [units]
  )

  const onUnitSelect = useCallback(
    (value) => {
      if (decisionDraftGroup.isSuccess && units.isSuccess) {
        const selected = units.value.find(({ id }) => id === value)
        setSelectedUnit(selected ?? decisionDraftGroup.value.unit)
      }
    },
    [decisionDraftGroup, units, setSelectedUnit]
  )

  function RedirectToMainPage() {
    history.push('/applications')
    return null
  }

  function updateState(type: DecisionType, values: Partial<DecisionDraft>) {
    setDecisions(
      decisions.map((decision) =>
        decision.type === type
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

  const unitDataIsComplete =
    !!selectedUnit &&
    !!(!requiresDaycareDecisionName || selectedUnit.daycareDecisionName) &&
    !!(!requiresPreschoolDecisionName || selectedUnit.preschoolDecisionName) &&
    !!selectedUnit.manager &&
    !!selectedUnit.streetAddress &&
    !!selectedUnit.postalCode &&
    !!selectedUnit.postOffice &&
    (isClubDecision ||
      (!!selectedUnit.decisionHandler && !!selectedUnit.decisionHandlerAddress))

  const noDecisionsPlanned =
    decisions.filter(({ planned }) => planned).length === 0

  return (
    <Container>
      <Gap size={'L'} />
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
                ...decisions.map((decision) => ({
                  label: (
                    <Checkbox
                      label={i18n.decisionDraft.types[decision.type]}
                      checked={decision.planned}
                      onChange={(planned: boolean) => {
                        updateState(decision.type, { planned })
                      }}
                      dataQa={`toggle-service-need-part-day-${decision.type}`}
                    />
                  ),
                  value: (
                    <DateRangeContainer>
                      <DatePicker
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
                      <DatePicker
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
                })),
                { label: null, value: null },
                ...(selectedUnit
                  ? [
                      {
                        label: i18n.decisionDraft.placementUnit,
                        value: decisionDraftGroup.value.placementUnitName
                      },
                      {
                        label: i18n.decisionDraft.selectedUnit,
                        value: (
                          <UnitSelectContainer>
                            {units.isSuccess && (
                              <Select
                                onChange={(value) =>
                                  value && 'value' in value
                                    ? onUnitSelect(value.value)
                                    : undefined
                                }
                                options={unitOptions}
                                value={units.value
                                  .filter((elem) => selectedUnit.id === elem.id)
                                  .map((elem) => ({
                                    label: elem.name,
                                    value: elem.id
                                  }))}
                              />
                            )}
                            <WarningContainer
                              visible={
                                decisionDraftGroup.value.unit.id !==
                                selectedUnit.id
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
                      {
                        label: (
                          <Heading size={4}>
                            {i18n.decisionDraft.unitDetailsHeading}
                          </Heading>
                        ),
                        value: null
                      },
                      ...(requiresDaycareDecisionName
                        ? [
                            {
                              label: i18n.decisionDraft.daycareDecisionName,
                              value: (
                                <MissingValuePlaceholder
                                  i18n={i18n}
                                  values={[selectedUnit.daycareDecisionName]}
                                />
                              )
                            }
                          ]
                        : []),
                      ...(requiresPreschoolDecisionName
                        ? [
                            {
                              label: i18n.decisionDraft.preschoolDecisionName,
                              value: (
                                <MissingValuePlaceholder
                                  i18n={i18n}
                                  values={[selectedUnit.preschoolDecisionName]}
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
                            values={[selectedUnit.manager || '']}
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
                              label: i18n.decisionDraft.handlerName,
                              value: (
                                <MissingValuePlaceholder
                                  i18n={i18n}
                                  values={[selectedUnit.decisionHandler]}
                                />
                              )
                            },
                            {
                              label: i18n.decisionDraft.handlerAddress,
                              value: (
                                <MissingValuePlaceholder
                                  i18n={i18n}
                                  values={[selectedUnit.decisionHandlerAddress]}
                                />
                              )
                            }
                          ]
                        : []),
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
                        : [])
                    ]
                  : [])
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

            {!unitDataIsComplete && (
              <AlertBox
                title={i18n.decisionDraft.unitInfo1}
                message={i18n.decisionDraft.unitInfo2}
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
                <Button
                  dataQa="cancel-decisions-button"
                  onClick={RedirectToMainPage}
                  text={i18n.common.cancel}
                />
                <Button
                  primary
                  dataQa="save-decisions-button"
                  disabled={
                    !unitDataIsComplete || noDecisionsPlanned || submitting
                  }
                  onClick={() => {
                    setSubmitting(true)
                    const updatedDrafts: DecisionDraftUpdate[] = decisions.map(
                      (decisionDraft) => ({
                        id: decisionDraft.id,
                        unitId: selectedUnit?.id ?? '',
                        startDate: decisionDraft.startDate,
                        endDate: decisionDraft.endDate,
                        planned: decisionDraft.planned
                      })
                    )
                    void updateDecisionDrafts(applicationId, updatedDrafts)
                      .then(RedirectToMainPage)
                      .catch(() => setSubmitting(false))
                  }}
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

export default Decision
