// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine, Failure, Success } from 'lib-common/api'
import { boolean, localDate, string } from 'lib-common/form/fields'
import {
  array,
  mapped,
  object,
  oneOf,
  required,
  value
} from 'lib-common/form/form'
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import {
  AssistanceNeedDecisionLanguage,
  AssistanceNeedPreschoolDecision,
  AssistanceNeedPreschoolDecisionForm,
  AssistanceNeedPreschoolDecisionType
} from 'lib-common/generated/api-types/assistanceneed'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { AssistanceNeedDecisionStatusChip } from 'lib-components/assistance-need-decision/AssistanceNeedDecisionStatusChip'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import Container, { ContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { fi } from 'lib-customizations/defaults/employee/i18n/fi'

import { Unit } from '../../../../api/daycare'
import { useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'
import {
  assistanceNeedPreschoolDecisionQuery,
  preschoolUnitsQuery
} from '../../queries'

import { FooterContainer } from './common'

const WidthLimiter = styled.div`
  max-width: 700px;
`

const SectionSpacer = styled(FixedSpaceColumn).attrs({ spacing: 'L' })``

const guardianForm = object({
  id: string(),
  name: string(),
  personId: string(),
  isHeard: boolean(),
  details: string()
})

const form = mapped(
  object({
    language: required(oneOf<AssistanceNeedDecisionLanguage>()),

    type: oneOf<AssistanceNeedPreschoolDecisionType>(),
    validFrom: localDate,

    extendedCompulsoryEducation: boolean(),
    extendedCompulsoryEducationInfo: string(),

    grantedAssistanceService: boolean(),
    grantedInterpretationService: boolean(),
    grantedAssistiveDevices: boolean(),
    grantedServicesBasis: string(),

    selectedUnit: value<UUID | null>(),
    primaryGroup: string(),
    decisionBasis: string(),

    basisDocumentPedagogicalReport: boolean(),
    basisDocumentPsychologistStatement: boolean(),
    basisDocumentSocialReport: boolean(),
    basisDocumentDoctorStatement: boolean(),
    basisDocumentOtherOrMissing: boolean(),
    basisDocumentOtherOrMissingInfo: string(),
    basisDocumentsInfo: string(),

    guardiansHeardOn: localDate,
    guardianInfo: array(guardianForm),
    otherRepresentativeHeard: boolean(),
    otherRepresentativeDetails: string(),
    viewOfGuardians: string(),

    preparer1EmployeeId: value<UUID | null>(),
    preparer1PhoneNumber: string(),
    preparer1Title: string(),
    preparer2EmployeeId: value<UUID | null>(),
    preparer2PhoneNumber: string(),
    preparer2Title: string(),
    decisionMakerEmployeeId: value<UUID | null>(),
    decisionMakerTitle: string()
  }),
  (output): AssistanceNeedPreschoolDecisionForm => ({
    ...output,
    type: output.type ?? null
  })
)

const types: AssistanceNeedPreschoolDecisionType[] = [
  'NEW',
  'CONTINUING',
  'TERMINATED'
]

const GuardianForm = React.memo(function GuardianForm({
  bind,
  displayValidation,
  validationErrors
}: {
  bind: BoundForm<typeof guardianForm>
  displayValidation: boolean
  validationErrors: {
    isHeard: keyof typeof fi.validationErrors | undefined
    details: keyof typeof fi.validationErrors | undefined
  }
}) {
  const { i18n } = useTranslation()
  const { isHeard, details, name } = useFormFields(bind)
  return (
    <FixedSpaceColumn spacing="zero">
      <FixedSpaceRow alignItems="center">
        <CheckboxF bind={isHeard} label={name.value()} />
        {displayValidation && validationErrors.isHeard && (
          <AlertBox
            message={i18n.validationErrors[validationErrors.isHeard]}
            thin
          />
        )}
      </FixedSpaceRow>
      <WidthLimiter>
        <InputFieldF
          bind={details}
          info={
            displayValidation && validationErrors.details
              ? {
                  status: 'warning',
                  text: i18n.validationErrors[validationErrors.details]
                }
              : undefined
          }
        />
      </WidthLimiter>
    </FixedSpaceColumn>
  )
})

const DecisionEditor = React.memo(function DecisionEditor({
  decision,
  units
}: {
  decision: AssistanceNeedPreschoolDecision
  units: Unit[]
}) {
  const { i18n, lang: uiLang } = useTranslation()
  const t = i18n.childInformation.assistanceNeedPreschoolDecision
  const navigate = useNavigate()
  const [displayValidation, setDisplayValidation] = useState(false)

  const bind = useForm(
    form,
    () => ({
      ...decision.form,
      language: {
        domValue: decision.form.language || '',
        options: ['FI' as const, 'SV' as const].map((lang) => ({
          domValue: lang,
          value: lang,
          label: lang // todo
        }))
      },
      type: {
        domValue: decision.form.type || '',
        options: types.map((type) => ({
          domValue: type,
          value: type,
          label: t.types[type]
        }))
      }
    }),
    i18n.validationErrors
  )

  const validationErrors = useMemo(
    () => ({
      type: bind.state.type.domValue
        ? undefined
        : ('requiredSelection' as const),
      validFrom: bind.state.validFrom ? undefined : ('required' as const),
      extendedCompulsoryEducationInfo:
        bind.state.extendedCompulsoryEducation &&
        bind.state.extendedCompulsoryEducationInfo.trim() === ''
          ? ('required' as const)
          : undefined,
      selectedUnit: bind.state.selectedUnit ? undefined : ('required' as const),
      primaryGroup:
        bind.state.primaryGroup.trim() === ''
          ? ('required' as const)
          : undefined,
      decisionBasis:
        bind.state.decisionBasis.trim() === ''
          ? ('required' as const)
          : undefined,
      documentBasis:
        bind.state.basisDocumentPedagogicalReport ||
        bind.state.basisDocumentPsychologistStatement ||
        bind.state.basisDocumentDoctorStatement ||
        bind.state.basisDocumentSocialReport ||
        bind.state.basisDocumentOtherOrMissing
          ? undefined
          : ('requiredSelection' as const),
      basisDocumentOtherOrMissingInfo:
        bind.state.basisDocumentOtherOrMissing &&
        bind.state.basisDocumentOtherOrMissingInfo.trim() === ''
          ? ('requiredSelection' as const)
          : undefined,
      guardiansHeardOn: bind.state.guardiansHeardOn
        ? undefined
        : ('required' as const),
      guardianInfo: bind.state.guardianInfo.map((guardian) => ({
        isHeard: guardian.isHeard
          ? undefined
          : ('guardianMustBeHeard' as const),
        details:
          guardian.details.trim() === '' ? ('required' as const) : undefined
      })),
      otherRepresentativeDetails:
        bind.state.otherRepresentativeHeard &&
        bind.state.otherRepresentativeDetails.trim() === ''
          ? ('required' as const)
          : undefined,
      viewOfGuardians:
        bind.state.viewOfGuardians.trim() === ''
          ? ('required' as const)
          : undefined
    }),
    [bind.state]
  )

  const isValid = useMemo(
    () => Object.values(validationErrors).every((e) => e === undefined),
    [validationErrors]
  )

  const {
    type,
    validFrom,
    extendedCompulsoryEducation,
    extendedCompulsoryEducationInfo,
    grantedAssistanceService,
    grantedAssistiveDevices,
    grantedInterpretationService,
    grantedServicesBasis,
    selectedUnit,
    primaryGroup,
    decisionBasis,
    basisDocumentPedagogicalReport,
    basisDocumentPsychologistStatement,
    basisDocumentSocialReport,
    basisDocumentDoctorStatement,
    basisDocumentOtherOrMissing,
    basisDocumentOtherOrMissingInfo,
    basisDocumentsInfo,
    guardiansHeardOn,
    guardianInfo,
    otherRepresentativeHeard,
    otherRepresentativeDetails,
    viewOfGuardians
  } = useFormFields(bind)

  const guardians = useFormElems(guardianInfo)

  return (
    <div>
      <Container>
        <ContentArea opaque>
          <FixedSpaceRow justifyContent="space-between" alignItems="flex-start">
            <FixedSpaceColumn>
              <H1 noMargin>{t.pageTitle}</H1>
              <H2>{decision.child.name}</H2>
            </FixedSpaceColumn>
            <FixedSpaceColumn>
              <span>
                {i18n.childInformation.assistanceNeedDecision.decisionNumber}{' '}
                {decision.decisionNumber}
              </span>
              <AssistanceNeedDecisionStatusChip
                decisionStatus={decision.status}
                texts={i18n.childInformation.assistanceNeedDecision.statuses}
              />
              <span>
                {i18n.childInformation.assistanceNeedDecision.confidential}
              </span>
              <span>
                {i18n.childInformation.assistanceNeedDecision.lawReference}
              </span>
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <FixedSpaceColumn spacing="XL">
            <SectionSpacer>
              <H2>Päätettävä tuki</H2>

              <FixedSpaceColumn>
                <FixedSpaceRow alignItems="center">
                  <Label>Erityisen tuen tila *</Label>
                  {displayValidation && validationErrors.type && (
                    <AlertBox
                      message={i18n.validationErrors[validationErrors.type]}
                      thin
                    />
                  )}
                </FixedSpaceRow>
                {type.state.options.map((opt) => (
                  <Radio
                    key={opt.domValue}
                    label={opt.label}
                    checked={type.value() == opt.value}
                    onChange={() =>
                      type.update((prev) => ({
                        ...prev,
                        domValue: opt.domValue
                      }))
                    }
                  />
                ))}
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <Label>Voimassa alkaen *</Label>
                <DatePickerF
                  bind={validFrom}
                  locale={uiLang}
                  info={
                    displayValidation && validationErrors.validFrom
                      ? {
                          status: 'warning',
                          text: i18n.validationErrors[
                            validationErrors.validFrom
                          ]
                        }
                      : undefined
                  }
                />
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <Label>Pidennetty oppivelvollisuus</Label>
                <CheckboxF
                  bind={extendedCompulsoryEducation}
                  label="Lapsella on pidennetty oppivelvollisuus"
                />
                {extendedCompulsoryEducation.value() && (
                  <FixedSpaceColumn>
                    <Label>
                      Lisätiedot pidennetystä oppivelvollisuudesta *
                    </Label>
                    <WidthLimiter>
                      <TextAreaF
                        bind={extendedCompulsoryEducationInfo}
                        info={
                          displayValidation &&
                          validationErrors.extendedCompulsoryEducationInfo
                            ? {
                                status: 'warning',
                                text: i18n.validationErrors[
                                  validationErrors
                                    .extendedCompulsoryEducationInfo
                                ]
                              }
                            : undefined
                        }
                      />
                    </WidthLimiter>
                  </FixedSpaceColumn>
                )}
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <Label>
                  Myönnettävät tulkitsemis- ja avustajapalvelut tai erityiset
                  apuvälineet
                </Label>
                <CheckboxF
                  bind={grantedAssistanceService}
                  label="Lapselle myönnetään avustajapalveluita"
                />
                <CheckboxF
                  bind={grantedInterpretationService}
                  label="Lapselle myönnetään tulkitsemispalveluita"
                />
                <CheckboxF
                  bind={grantedAssistiveDevices}
                  label="Lapselle myönnetään erityisiä apuvälineitä"
                />
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <Label>
                  Perustelut myönnettäville tulkitsemis- ja avustajapalveluille
                  ja apuvälineille
                </Label>
                <WidthLimiter>
                  <TextAreaF bind={grantedServicesBasis} />
                </WidthLimiter>
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <Label>Esiopetuksen järjestämispaikka *</Label>
                <Select<Unit | null>
                  items={[null, ...units]}
                  selectedItem={
                    selectedUnit
                      ? units.find((u) => u.id === selectedUnit.value()) ?? null
                      : null
                  }
                  getItemLabel={(u) => u?.name ?? ''}
                  getItemValue={(u: Unit | undefined) => u?.id ?? ''}
                  onChange={(u) => selectedUnit.set(u?.id ?? null)}
                />
                {displayValidation && validationErrors.selectedUnit && (
                  <AlertBox
                    message={
                      i18n.validationErrors[validationErrors.selectedUnit]
                    }
                    thin
                  />
                )}
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <Label>Pääsääntöinen opetusryhmä *</Label>
                <InputFieldF
                  bind={primaryGroup}
                  width="L"
                  info={
                    displayValidation && validationErrors.primaryGroup
                      ? {
                          status: 'warning',
                          text: i18n.validationErrors[
                            validationErrors.primaryGroup
                          ]
                        }
                      : undefined
                  }
                />
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <Label>Perustelut päätökselle *</Label>
                <WidthLimiter>
                  <TextAreaF
                    bind={decisionBasis}
                    info={
                      displayValidation && validationErrors.decisionBasis
                        ? {
                            status: 'warning',
                            text: i18n.validationErrors[
                              validationErrors.decisionBasis
                            ]
                          }
                        : undefined
                    }
                  />
                </WidthLimiter>
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <FixedSpaceRow alignItems="center">
                  <Label>Asiakirjat, joihin päätös perustuu *</Label>
                  {displayValidation && validationErrors.documentBasis && (
                    <AlertBox
                      message={
                        i18n.validationErrors[validationErrors.documentBasis]
                      }
                      thin
                    />
                  )}
                </FixedSpaceRow>
                <CheckboxF
                  bind={basisDocumentPedagogicalReport}
                  label="Pedagoginen selvitys"
                />
                <CheckboxF
                  bind={basisDocumentPsychologistStatement}
                  label="Psykologin lausunto"
                />
                <CheckboxF
                  bind={basisDocumentDoctorStatement}
                  label="Sosiaalinen selvitys"
                />
                <CheckboxF
                  bind={basisDocumentSocialReport}
                  label="Lääkärin lausunto"
                />
                <CheckboxF
                  bind={basisDocumentOtherOrMissing}
                  label="Liite puuttuu, tai muu liite, mikä?"
                />
                <InputFieldF
                  bind={basisDocumentOtherOrMissingInfo}
                  info={
                    displayValidation &&
                    validationErrors.basisDocumentOtherOrMissingInfo
                      ? {
                          status: 'warning',
                          text: i18n.validationErrors[
                            validationErrors.basisDocumentOtherOrMissingInfo
                          ]
                        }
                      : undefined
                  }
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn>
                <Label>Lisätiedot liitteistä</Label>
                <WidthLimiter>
                  <TextAreaF bind={basisDocumentsInfo} />
                </WidthLimiter>
              </FixedSpaceColumn>
            </SectionSpacer>

            <SectionSpacer>
              <H2>Huoltajien kanssa tehty yhteistyö</H2>

              <FixedSpaceColumn>
                <Label>Huoltajien kuulemisen päivämäärä *</Label>
                <DatePickerF
                  bind={guardiansHeardOn}
                  locale={uiLang}
                  info={
                    displayValidation && validationErrors.guardiansHeardOn
                      ? {
                          status: 'warning',
                          text: i18n.validationErrors[
                            validationErrors.guardiansHeardOn
                          ]
                        }
                      : undefined
                  }
                />
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <Label>Huoltajat, joita on kuultu, ja kuulemistapa *</Label>
                {guardians.map((guardian, i) => (
                  <GuardianForm
                    key={guardian.state.id}
                    bind={guardian}
                    displayValidation={displayValidation}
                    validationErrors={validationErrors.guardianInfo[i]}
                  />
                ))}
                <FixedSpaceColumn spacing="zero">
                  <CheckboxF
                    bind={otherRepresentativeHeard}
                    label="Muu laillinen edustaja (nimi, puhelinnumero ja kuulemistapa)"
                  />
                  <WidthLimiter>
                    <InputFieldF
                      bind={otherRepresentativeDetails}
                      info={
                        displayValidation &&
                        validationErrors.otherRepresentativeDetails
                          ? {
                              status: 'warning',
                              text: i18n.validationErrors[
                                validationErrors.otherRepresentativeDetails
                              ]
                            }
                          : undefined
                      }
                    />
                  </WidthLimiter>
                </FixedSpaceColumn>
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <Label>Huoltajien näkemys esitetystä tuesta *</Label>
                <WidthLimiter>
                  <TextAreaF
                    bind={viewOfGuardians}
                    info={
                      displayValidation && validationErrors.viewOfGuardians
                        ? {
                            status: 'warning',
                            text: i18n.validationErrors[
                              validationErrors.viewOfGuardians
                            ]
                          }
                        : undefined
                    }
                  />
                </WidthLimiter>
              </FixedSpaceColumn>
            </SectionSpacer>
          </FixedSpaceColumn>
        </ContentArea>
      </Container>

      <Gap size="m" />

      <StickyFooter>
        <FooterContainer>
          <FixedSpaceRow justifyContent="space-between" alignItems="center">
            <FixedSpaceRow>
              <AsyncButton
                primary
                text={i18n.childInformation.assistanceNeedDecision.leavePage}
                onClick={() => Promise.resolve(Success.of(null))} // todo
                onSuccess={() =>
                  navigate(`/child-information/${decision.child.id}`)
                }
                data-qa="leave-page-button"
                hideSuccess
              />
              <span>todo saving ...</span>
            </FixedSpaceRow>

            <AsyncButton
              primary
              text={i18n.childInformation.assistanceNeedDecision.preview}
              disabled={displayValidation && !isValid}
              onClick={async () => {
                if (isValid) {
                  return Promise.resolve(
                    Failure.of({
                      message: 'todo'
                    })
                  )
                } else {
                  setDisplayValidation(true)
                  return Promise.resolve(
                    Failure.of({
                      message: 'Invalid form'
                    })
                  )
                }
              }}
              onSuccess={() =>
                navigate(
                  `/child-information/${decision.child.id}/assistance-need-decision/${decision.id}`
                )
              }
              data-qa="preview-button"
              hideSuccess
            />
          </FixedSpaceRow>
        </FooterContainer>
      </StickyFooter>
    </div>
  )
})

export default React.memo(function AssistanceNeedPreschoolDecisionEditPage() {
  const { decisionId } = useNonNullableParams<{ decisionId: UUID }>()
  const decisionResult = useQueryResult(
    assistanceNeedPreschoolDecisionQuery(decisionId)
  )
  const unitsResult = useQueryResult(preschoolUnitsQuery)

  return renderResult(
    combine(decisionResult, unitsResult),
    ([decisionResponse, units]) => (
      <DecisionEditor decision={decisionResponse.decision} units={units} />
    )
  )
})
