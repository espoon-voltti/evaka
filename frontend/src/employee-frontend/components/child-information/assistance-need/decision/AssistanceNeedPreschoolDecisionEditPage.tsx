// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import { formatInTimeZone } from 'date-fns-tz'
import isEqual from 'lodash/isEqual'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { boolean, localDate, string } from 'lib-common/form/fields'
import {
  array,
  mapped,
  nullBlank,
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
  AssistanceNeedPreschoolDecision,
  AssistanceNeedPreschoolDecisionForm,
  AssistanceNeedPreschoolDecisionType
} from 'lib-common/generated/api-types/assistanceneed'
import { UnitStub } from 'lib-common/generated/api-types/daycare'
import { Employee } from 'lib-common/generated/api-types/pis'
import { OfficialLanguage } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { AssistanceNeedDecisionStatusChip } from 'lib-components/assistance-need-decision/AssistanceNeedDecisionStatusChip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Button from 'lib-components/atoms/buttons/Button'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import Spinner from 'lib-components/atoms/state/Spinner'
import Container, { ContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { H1, H2, H3, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { fi } from 'lib-customizations/defaults/employee/i18n/fi'
import { featureFlags, translations } from 'lib-customizations/employee'

import { getEmployeesQuery } from '../../../../queries'
import { useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'
import {
  assistanceNeedPreschoolDecisionMakerOptionsQuery,
  assistanceNeedPreschoolDecisionQuery,
  queryKeys,
  unitsQuery,
  updateAssistanceNeedPreschoolDecisionMutation
} from '../../queries'

const WidthLimiter = styled.div`
  max-width: 700px;
`

const ComboboxWrapper = styled.div`
  width: 400px;
`

const SectionSpacer = styled(FixedSpaceColumn).attrs({ spacing: 'L' })``

const LabeledValue = styled(FixedSpaceColumn).attrs({ spacing: 'xs' })``

const guardianForm = object({
  id: string(),
  name: string(),
  personId: string(),
  isHeard: boolean(),
  details: string()
})

const form = mapped(
  object({
    language: required(oneOf<OfficialLanguage>()),

    type: oneOf<AssistanceNeedPreschoolDecisionType>(),
    validFrom: nullBlank(localDate()),
    validTo: nullBlank(localDate()),

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
    basisDocumentPedagogicalReportDate: nullBlank(localDate()),
    basisDocumentPsychologistStatementDate: nullBlank(localDate()),
    basisDocumentSocialReportDate: nullBlank(localDate()),
    basisDocumentDoctorStatementDate: nullBlank(localDate()),
    basisDocumentOtherOrMissing: boolean(),
    basisDocumentOtherOrMissingInfo: string(),
    basisDocumentsInfo: string(),

    guardiansHeardOn: nullBlank(localDate()),
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
    type: output.type ?? null,
    extendedCompulsoryEducationInfo: output.extendedCompulsoryEducation
      ? output.extendedCompulsoryEducationInfo
      : '',
    basisDocumentOtherOrMissingInfo: output.basisDocumentOtherOrMissing
      ? output.basisDocumentOtherOrMissingInfo
      : '',
    preparer2Title: output.preparer2EmployeeId ? output.preparer2Title : '',
    preparer2PhoneNumber: output.preparer2EmployeeId
      ? output.preparer2PhoneNumber
      : ''
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
        <CheckboxF
          bind={isHeard}
          label={name.value()}
          data-qa="guardian-heard"
        />
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
          data-qa="guardian-details"
        />
      </WidthLimiter>
    </FixedSpaceColumn>
  )
})

const pageTranslations = {
  FI: translations.fi.childInformation.assistanceNeedPreschoolDecision,
  SV: translations.sv.childInformation.assistanceNeedPreschoolDecision
}

const formatEmployeeName = (employee: Employee) =>
  `${employee.lastName ?? ''} ${
    employee.preferredFirstName ?? employee.firstName ?? ''
  } (${employee.email ?? ''})`

const filterEmployees = (inputValue: string, items: readonly Employee[]) =>
  items.filter(
    (emp) =>
      (emp.preferredFirstName ?? emp.firstName)
        .toLowerCase()
        .startsWith(inputValue.toLowerCase()) ||
      emp.lastName.toLowerCase().startsWith(inputValue.toLowerCase())
  )

const DecisionEditor = React.memo(function DecisionEditor({
  decision,
  units,
  employees
}: {
  decision: AssistanceNeedPreschoolDecision
  units: UnitStub[]
  employees: Employee[]
}) {
  const { i18n, lang: uiLang } = useTranslation()
  const navigate = useNavigate()
  const [displayValidation, setDisplayValidation] = useState(false)
  const [formLanguage, setFormLanguage] = useState(decision.form.language)
  const t = pageTranslations[formLanguage]

  useEffect(() => {
    const editable =
      decision.status === 'NEEDS_WORK' ||
      (decision.status === 'DRAFT' && !decision.sentForDecision)
    if (!editable)
      navigate(
        `/child-information/${decision.child.id}/assistance-need-preschool-decisions/${decision.id}`
      )
  }, [decision, navigate])

  const getTypeOptions = useCallback(
    (lang: OfficialLanguage) =>
      types.map((type) => ({
        domValue: type,
        value: type,
        label: pageTranslations[lang].types[type]
      })),
    []
  )

  const bind = useForm(
    form,
    () => ({
      ...decision.form,
      language: {
        domValue: decision.form.language || '',
        options: [
          {
            domValue: 'FI' as const,
            value: 'FI' as const,
            label: i18n.language.fi
          },
          {
            domValue: 'SV' as const,
            value: 'SV' as const,
            label: i18n.language.sv
          }
        ]
      },
      type: {
        domValue: decision.form.type || '',
        options: getTypeOptions(decision.form.language)
      },
      validFrom: localDate.fromDate(decision.form.validFrom),
      validTo: localDate.fromDate(decision.form.validTo),
      basisDocumentPedagogicalReportDate: localDate.fromDate(
        decision.form.basisDocumentPedagogicalReportDate
      ),
      basisDocumentPsychologistStatementDate: localDate.fromDate(
        decision.form.basisDocumentPsychologistStatementDate
      ),
      basisDocumentSocialReportDate: localDate.fromDate(
        decision.form.basisDocumentSocialReportDate
      ),
      basisDocumentDoctorStatementDate: localDate.fromDate(
        decision.form.basisDocumentDoctorStatementDate
      ),
      guardiansHeardOn: localDate.fromDate(decision.form.guardiansHeardOn)
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
      otherRepresentativeHeard:
        bind.state.guardianInfo.length === 0 &&
        !bind.state.otherRepresentativeHeard
          ? ('required' as const)
          : undefined,
      otherRepresentativeDetails:
        bind.state.otherRepresentativeHeard &&
        bind.state.otherRepresentativeDetails.trim() === ''
          ? ('required' as const)
          : undefined,
      viewOfGuardians:
        bind.state.viewOfGuardians.trim() === ''
          ? ('required' as const)
          : undefined,
      preparer1EmployeeId: bind.state.preparer1EmployeeId
        ? undefined
        : ('required' as const),
      preparer1Title:
        bind.state.preparer1Title.trim() === ''
          ? ('required' as const)
          : undefined,
      preparer2Title:
        bind.state.preparer2EmployeeId &&
        bind.state.preparer2Title.trim() === ''
          ? ('required' as const)
          : undefined,
      decisionMakerEmployeeId: bind.state.decisionMakerEmployeeId
        ? undefined
        : ('required' as const),
      decisionMakerTitle:
        bind.state.decisionMakerTitle.trim() === ''
          ? ('required' as const)
          : undefined
    }),
    [bind.state]
  )

  const isValid = useMemo(() => {
    const { guardianInfo, ...rest } = validationErrors
    return (
      Object.values(rest).every((e) => e === undefined) &&
      guardianInfo.every((guardianErrors) =>
        Object.values(guardianErrors).every((e) => e === undefined)
      )
    )
  }, [validationErrors])

  const {
    language,
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
    basisDocumentPedagogicalReportDate,
    basisDocumentPsychologistStatementDate,
    basisDocumentSocialReportDate,
    basisDocumentDoctorStatementDate,
    basisDocumentOtherOrMissing,
    basisDocumentOtherOrMissingInfo,
    basisDocumentsInfo,
    guardiansHeardOn,
    guardianInfo,
    otherRepresentativeHeard,
    otherRepresentativeDetails,
    viewOfGuardians,
    preparer1EmployeeId,
    preparer1Title,
    preparer1PhoneNumber,
    preparer2EmployeeId,
    preparer2Title,
    preparer2PhoneNumber,
    decisionMakerEmployeeId,
    decisionMakerTitle
  } = useFormFields(bind)

  if (language.value() !== formLanguage) {
    setFormLanguage(language.value())
    type.update((prev) => ({
      ...prev,
      options: getTypeOptions(language.value())
    }))
  }

  const guardians = useFormElems(guardianInfo)

  const debouncedValue = useDebounce(bind.isValid() ? bind.value() : null, 1000)
  const [savedValue, setSavedValue] = useState(() => bind.value())
  const [lastSavedAt, setLastSavedAt] = useState(HelsinkiDateTime.now())
  const {
    mutateAsync: updateAssistanceNeedPreschoolDecision,
    isPending: saving
  } = useMutationResult(updateAssistanceNeedPreschoolDecisionMutation)

  useEffect(() => {
    if (debouncedValue !== null && !isEqual(debouncedValue, savedValue)) {
      void updateAssistanceNeedPreschoolDecision({
        id: decision.id,
        body: debouncedValue
      }).then((res) => {
        if (res.isSuccess) {
          setSavedValue(debouncedValue)
          setLastSavedAt(HelsinkiDateTime.now())
        }
      })
    }
  }, [
    debouncedValue,
    savedValue,
    decision.id,
    updateAssistanceNeedPreschoolDecision
  ])

  const saved = !saving && bind.isValid() && bind.value() === savedValue

  const info = useCallback(
    (key: keyof Omit<typeof validationErrors, 'guardianInfo'>) => {
      const error = validationErrors[key]
      if (!displayValidation || !error) return undefined

      return {
        status: 'warning' as const,
        text: i18n.validationErrors[error]
      }
    },
    [displayValidation, validationErrors, i18n]
  )

  const decisionMakersResult = useQueryResult(
    assistanceNeedPreschoolDecisionMakerOptionsQuery({
      id: decision.id,
      unitId: savedValue.selectedUnit
    })
  )

  if (
    decisionMakersResult.isSuccess &&
    decisionMakerEmployeeId.value() !== null &&
    !decisionMakersResult.value.some(
      (e) => e.id === decisionMakerEmployeeId.value()
    )
  ) {
    decisionMakerEmployeeId.set(null)
  }

  return (
    <div>
      <Container>
        <ContentArea opaque>
          {featureFlags.assistanceNeedDecisionsLanguageSelect && (
            <>
              <FixedSpaceRow alignItems="baseline">
                <Label>
                  {i18n.childInformation.assistanceNeedDecision.formLanguage}
                </Label>
                <SelectF bind={language} />
              </FixedSpaceRow>
              <HorizontalLine />
            </>
          )}

          <FixedSpaceRow justifyContent="space-between" alignItems="flex-start">
            <FixedSpaceColumn>
              <H1 noMargin>{t.pageTitle}</H1>
              <H2>{decision.child.name}</H2>
            </FixedSpaceColumn>
            <FixedSpaceColumn>
              <span data-qa="decision-number">
                {t.decisionNumber} {decision.decisionNumber}
              </span>
              <AssistanceNeedDecisionStatusChip
                decisionStatus={decision.status}
                texts={t.statuses}
                data-qa="status"
              />
              <span>{t.confidential}</span>
              <span>{t.lawReference}</span>
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <FixedSpaceColumn spacing="XL">
            <SectionSpacer>
              <H2>{t.decidedAssistance}</H2>

              <FixedSpaceColumn>
                <FixedSpaceRow alignItems="center">
                  <Label>{t.type} *</Label>
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
                    data-qa={`type-radio-${opt.value}`}
                  />
                ))}
              </FixedSpaceColumn>

              <LabeledValue>
                <ExpandingInfo info={t.validFromInfo()}>
                  <Label>{t.validFrom} *</Label>
                </ExpandingInfo>
                <DatePickerF
                  bind={validFrom}
                  locale={uiLang}
                  info={info('validFrom')}
                  data-qa="valid-from"
                />
              </LabeledValue>

              <FixedSpaceColumn>
                <Label>{t.extendedCompulsoryEducationSection}</Label>
                <CheckboxF
                  bind={extendedCompulsoryEducation}
                  label={t.extendedCompulsoryEducation}
                />
                {extendedCompulsoryEducation.value() && (
                  <LabeledValue>
                    <ExpandingInfo
                      info={t.extendedCompulsoryEducationInfoInfo()}
                    >
                      <Label>{t.extendedCompulsoryEducationInfo} *</Label>
                    </ExpandingInfo>
                    <WidthLimiter>
                      <TextAreaF
                        bind={extendedCompulsoryEducationInfo}
                        info={info('extendedCompulsoryEducationInfo')}
                      />
                    </WidthLimiter>
                  </LabeledValue>
                )}
              </FixedSpaceColumn>

              <FixedSpaceColumn>
                <ExpandingInfo info={t.grantedAssistanceSectionInfo()}>
                  <Label>{t.grantedAssistanceSection}</Label>
                </ExpandingInfo>
                <CheckboxF
                  bind={grantedAssistanceService}
                  label={t.grantedAssistanceService}
                />
                <CheckboxF
                  bind={grantedInterpretationService}
                  label={t.grantedInterpretationService}
                />
                <CheckboxF
                  bind={grantedAssistiveDevices}
                  label={t.grantedAssistiveDevices}
                />
              </FixedSpaceColumn>

              <LabeledValue>
                <Label>{t.grantedServicesBasis}</Label>
                <WidthLimiter>
                  <TextAreaF bind={grantedServicesBasis} />
                </WidthLimiter>
              </LabeledValue>

              <LabeledValue>
                <Label>{t.selectedUnit} *</Label>
                <Combobox
                  items={units}
                  selectedItem={
                    selectedUnit
                      ? units.find((u) => u.id === selectedUnit.value()) ?? null
                      : null
                  }
                  getItemLabel={(u) => u?.name ?? ''}
                  onChange={(u) => selectedUnit.set(u?.id ?? null)}
                  info={info('selectedUnit')}
                  data-qa="unit-select"
                />
              </LabeledValue>

              <LabeledValue>
                <ExpandingInfo info={t.primaryGroupInfo()}>
                  <Label>{t.primaryGroup} *</Label>
                </ExpandingInfo>
                <InputFieldF
                  bind={primaryGroup}
                  width="L"
                  info={info('primaryGroup')}
                  data-qa="primary-group"
                />
              </LabeledValue>

              <LabeledValue>
                <ExpandingInfo info={t.decisionBasisInfo()}>
                  <Label>{t.decisionBasis} *</Label>
                </ExpandingInfo>
                <WidthLimiter>
                  <TextAreaF
                    bind={decisionBasis}
                    info={info('decisionBasis')}
                    data-qa="decision-basis"
                  />
                </WidthLimiter>
              </LabeledValue>

              <FixedSpaceColumn>
                <FixedSpaceRow alignItems="center">
                  <ExpandingInfo info={t.documentBasisInfo()}>
                    <Label>{t.documentBasis} *</Label>
                  </ExpandingInfo>
                  {displayValidation && validationErrors.documentBasis && (
                    <AlertBox
                      message={
                        i18n.validationErrors[validationErrors.documentBasis]
                      }
                      thin
                    />
                  )}
                </FixedSpaceRow>
                <FixedSpaceRow>
                  <CheckboxF
                    bind={basisDocumentPedagogicalReport}
                    label={t.basisDocumentPedagogicalReport}
                    data-qa="basis-pedagogical-report"
                  />
                  {basisDocumentPedagogicalReport.value() && (
                    <DatePickerF
                      bind={basisDocumentPedagogicalReportDate}
                      locale={uiLang}
                    />
                  )}
                </FixedSpaceRow>
                <FixedSpaceRow>
                  <CheckboxF
                    bind={basisDocumentPsychologistStatement}
                    label={t.basisDocumentPsychologistStatement}
                  />
                  {basisDocumentPsychologistStatement.value() && (
                    <DatePickerF
                      bind={basisDocumentPsychologistStatementDate}
                      locale={uiLang}
                    />
                  )}
                </FixedSpaceRow>
                <FixedSpaceRow>
                  <CheckboxF
                    bind={basisDocumentSocialReport}
                    label={t.basisDocumentSocialReport}
                  />
                  {basisDocumentSocialReport.value() && (
                    <DatePickerF
                      bind={basisDocumentSocialReportDate}
                      locale={uiLang}
                    />
                  )}
                </FixedSpaceRow>
                <FixedSpaceRow>
                  <CheckboxF
                    bind={basisDocumentDoctorStatement}
                    label={t.basisDocumentDoctorStatement}
                  />
                  {basisDocumentDoctorStatement.value() && (
                    <DatePickerF
                      bind={basisDocumentDoctorStatementDate}
                      locale={uiLang}
                    />
                  )}
                </FixedSpaceRow>
                <CheckboxF
                  bind={basisDocumentOtherOrMissing}
                  label={t.basisDocumentOtherOrMissing}
                />
                {basisDocumentOtherOrMissing.value() && (
                  <WidthLimiter>
                    <TextAreaF
                      bind={basisDocumentOtherOrMissingInfo}
                      info={info('basisDocumentOtherOrMissingInfo')}
                    />
                  </WidthLimiter>
                )}
              </FixedSpaceColumn>
              <LabeledValue>
                <Label>{t.basisDocumentsInfo}</Label>
                <WidthLimiter>
                  <TextAreaF bind={basisDocumentsInfo} />
                </WidthLimiter>
              </LabeledValue>
            </SectionSpacer>

            <SectionSpacer>
              <H2>{t.guardianCollaborationSection}</H2>

              <LabeledValue>
                <Label>{t.guardiansHeardOn} *</Label>
                <DatePickerF
                  bind={guardiansHeardOn}
                  locale={uiLang}
                  info={info('guardiansHeardOn')}
                  data-qa="guardians-heard"
                />
              </LabeledValue>

              <FixedSpaceColumn>
                <ExpandingInfo info={t.heardGuardiansInfo()}>
                  <Label>{t.heardGuardians} *</Label>
                </ExpandingInfo>
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
                    label={t.otherRepresentative}
                  />
                  <WidthLimiter>
                    <InputFieldF
                      bind={otherRepresentativeDetails}
                      info={info('otherRepresentativeDetails')}
                    />
                  </WidthLimiter>
                  {displayValidation &&
                    validationErrors.otherRepresentativeHeard && (
                      <AlertBox
                        message={
                          i18n.validationErrors[
                            validationErrors.otherRepresentativeHeard
                          ]
                        }
                        thin
                      />
                    )}
                </FixedSpaceColumn>
              </FixedSpaceColumn>

              <LabeledValue>
                <ExpandingInfo info={t.viewOfGuardiansInfo()}>
                  <Label>{t.viewOfGuardians} *</Label>
                </ExpandingInfo>
                <WidthLimiter>
                  <TextAreaF
                    bind={viewOfGuardians}
                    info={info('viewOfGuardians')}
                    data-qa="view-of-guardians"
                  />
                </WidthLimiter>
              </LabeledValue>
            </SectionSpacer>

            <SectionSpacer>
              <H3 noMargin>{t.legalInstructions}</H3>
              <P noMargin>{t.legalInstructionsText}</P>
              {bind.state.extendedCompulsoryEducation && (
                <P noMargin>
                  {t.legalInstructionsTextExtendedCompulsoryEducation}
                </P>
              )}
              <H3 noMargin>{t.jurisdiction}</H3>
              <P noMargin>{t.jurisdictionText}</P>
            </SectionSpacer>

            <SectionSpacer>
              <H2>{t.responsiblePeople}</H2>
              <FixedSpaceRow>
                <LabeledValue>
                  <Label>{t.preparer} *</Label>
                  <ComboboxWrapper>
                    <Combobox
                      items={employees}
                      getItemLabel={formatEmployeeName}
                      filterItems={filterEmployees}
                      selectedItem={
                        preparer1EmployeeId
                          ? employees.find(
                              (e) => e.id === preparer1EmployeeId.value()
                            ) ?? null
                          : null
                      }
                      onChange={(e) => preparer1EmployeeId.set(e?.id ?? null)}
                      clearable
                      info={info('preparer1EmployeeId')}
                      data-qa="preparer-1-select"
                    />
                  </ComboboxWrapper>
                </LabeledValue>
                <LabeledValue>
                  <Label>{t.employeeTitle} *</Label>
                  <InputFieldF
                    bind={preparer1Title}
                    width="L"
                    info={info('preparer1Title')}
                    data-qa="preparer-1-title"
                  />
                </LabeledValue>
                <LabeledValue>
                  <Label>{t.phone}</Label>
                  <InputFieldF bind={preparer1PhoneNumber} />
                </LabeledValue>
              </FixedSpaceRow>
              <FixedSpaceRow>
                <LabeledValue>
                  <Label>{t.preparer}</Label>
                  <ComboboxWrapper>
                    <Combobox
                      items={employees}
                      selectedItem={
                        preparer2EmployeeId
                          ? employees.find(
                              (e) => e.id === preparer2EmployeeId.value()
                            ) ?? null
                          : null
                      }
                      getItemLabel={formatEmployeeName}
                      filterItems={filterEmployees}
                      onChange={(e) => preparer2EmployeeId.set(e?.id ?? null)}
                      clearable
                    />
                  </ComboboxWrapper>
                </LabeledValue>
                {preparer2EmployeeId.value() && (
                  <>
                    <LabeledValue>
                      <Label>{t.employeeTitle} *</Label>
                      <InputFieldF
                        bind={preparer2Title}
                        width="L"
                        info={info('preparer2Title')}
                      />
                    </LabeledValue>
                    <LabeledValue>
                      <Label>{t.phone}</Label>
                      <InputFieldF bind={preparer2PhoneNumber} />
                    </LabeledValue>
                  </>
                )}
              </FixedSpaceRow>
              <FixedSpaceRow>
                <LabeledValue>
                  <Label>{t.decisionMaker} *</Label>
                  {renderResult(decisionMakersResult, (decisionMakers) => (
                    <ComboboxWrapper>
                      <Combobox
                        items={decisionMakers}
                        selectedItem={
                          decisionMakerEmployeeId
                            ? decisionMakers.find(
                                (e) => e.id === decisionMakerEmployeeId.value()
                              ) ?? null
                            : null
                        }
                        getItemLabel={formatEmployeeName}
                        filterItems={filterEmployees}
                        onChange={(e) =>
                          decisionMakerEmployeeId.set(e?.id ?? null)
                        }
                        clearable
                        info={info('decisionMakerEmployeeId')}
                        data-qa="decision-maker-select"
                      />
                    </ComboboxWrapper>
                  ))}
                </LabeledValue>
                <LabeledValue>
                  <Label>{t.employeeTitle} *</Label>
                  <InputFieldF
                    bind={decisionMakerTitle}
                    width="L"
                    info={info('decisionMakerTitle')}
                    data-qa="decision-maker-title"
                  />
                </LabeledValue>
              </FixedSpaceRow>
            </SectionSpacer>
          </FixedSpaceColumn>
        </ContentArea>
      </Container>

      <Gap size="m" />

      <StickyFooter>
        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <FixedSpaceRow alignItems="center">
            <Button
              text={i18n.childInformation.assistanceNeedDecision.leavePage}
              disabled={saving}
              onClick={() =>
                navigate(`/child-information/${decision.child.id}`)
              }
              data-qa="leave-page-button"
            />
            <FixedSpaceRow
              alignItems="center"
              spacing="xs"
              data-qa="autosave-indicator"
              data-status={!saved ? 'saving' : 'saved'}
            >
              <span>
                {i18n.common.saved}{' '}
                {formatInTimeZone(
                  lastSavedAt.timestamp,
                  'Europe/Helsinki',
                  'HH:mm:ss'
                )}
              </span>
              {!saved && (
                <Spinner size={defaultMargins.m} data-qa="saving-spinner" />
              )}
            </FixedSpaceRow>
          </FixedSpaceRow>

          <Button
            primary
            text={i18n.childInformation.assistanceNeedDecision.preview}
            disabled={!saved || (displayValidation && !isValid)}
            onClick={() => {
              if (isValid) {
                navigate(
                  `/child-information/${decision.child.id}/assistance-need-preschool-decisions/${decision.id}`
                )
              } else {
                setDisplayValidation(true)
              }
            }}
            data-qa="preview-button"
          />
        </FixedSpaceRow>
      </StickyFooter>
    </div>
  )
})

export default React.memo(function AssistanceNeedPreschoolDecisionEditPage() {
  const { childId, decisionId } = useRouteParams(['childId', 'decisionId'])
  const decisionResult = useQueryResult(
    assistanceNeedPreschoolDecisionQuery({ id: decisionId })
  )
  const unitsResult = useQueryResult(
    unitsQuery({ areaIds: null, type: 'ALL', from: null })
  ).map((units) =>
    units.filter((u) => u.careTypes.some((type) => type !== 'CLUB'))
  )
  const employeesResult = useQueryResult(getEmployeesQuery())

  // invalidate cached decision on onmount
  const queryClient = useQueryClient()
  useEffect(
    () => () => {
      void queryClient.invalidateQueries({
        queryKey: queryKeys.assistanceNeedPreschoolDecision(decisionId),
        type: 'all'
      })
      void queryClient.invalidateQueries({
        queryKey: queryKeys.assistanceNeedPreschoolDecisionBasics(childId),
        type: 'all'
      })
    },
    [queryClient, childId, decisionId]
  )

  return renderResult(
    combine(decisionResult, unitsResult, employeesResult),
    ([decisionResponse, units, employees]) => (
      <DecisionEditor
        decision={decisionResponse.decision}
        units={units}
        employees={employees}
      />
    )
  )
})
