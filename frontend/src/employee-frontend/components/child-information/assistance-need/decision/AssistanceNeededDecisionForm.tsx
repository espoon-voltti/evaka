// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { SetStateAction } from 'react'
import React, { useCallback, useMemo } from 'react'

import { getUnits } from 'employee-frontend/api/daycare'
import { getEmployees } from 'employee-frontend/api/employees'
import { renderResult } from 'employee-frontend/components/async-rendering'
import DateRangeInput from 'employee-frontend/components/common/DateRangeInput'
import { useTranslation } from 'employee-frontend/state/i18n'
import type { Employee } from 'employee-frontend/types/employee'
import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  AssistanceLevel,
  AssistanceNeedDecisionForm,
  AssistanceNeedDecisionGuardian
} from 'lib-common/generated/api-types/assistanceneed'
import LocalDate from 'lib-common/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import type { InputInfo } from 'lib-components/atoms/form/InputField'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H2, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

const FieldWithInfo = React.memo(function FieldWithInfo({
  info,
  label,
  children,
  spacing
}: {
  info: string
  label: string
  spacing?: string
  children: React.ReactNode
}) {
  return (
    <FixedSpaceColumn spacing={spacing || defaultMargins.s}>
      <ExpandingInfo info={info} ariaLabel={info}>
        <Label>{label}</Label>
      </ExpandingInfo>
      {children}
    </FixedSpaceColumn>
  )
})

const GuardianHeardField = React.memo(function GuardianHeardField({
  guardian,
  placeholder,
  onChange
}: {
  guardian: AssistanceNeedDecisionGuardian
  placeholder: string
  onChange: (guardian: AssistanceNeedDecisionGuardian) => void
}) {
  return (
    <div>
      <Checkbox
        checked={guardian.isHeard ?? false}
        label={guardian.name ?? ''}
        onChange={(checked) => onChange({ ...guardian, isHeard: checked })}
      />
      <InputField
        value={guardian.details ?? ''}
        placeholder={placeholder}
        onChange={(val) => onChange({ ...guardian, details: val })}
      />
    </div>
  )
})

const formatName = (someone: {
  firstName: string | null
  lastName: string | null
}) => `${someone.lastName ?? ''} ${someone.firstName ?? ''}`

interface EmployeeSelectProps {
  employeeLabel: string
  employees: Employee[]
  selectedEmployee: Employee | null
  titleLabel: string
  selectedTitle: string | null
  phoneLabel?: string
  selectedPhone?: string | null
  hasPhoneField?: boolean
  onChange: (
    employee: Employee | null,
    title: string | null,
    phone: string | null
  ) => void
  'data-qa'?: string
}

const EmployeeSelectWithTitle = React.memo(function EmployeeSelectWithTitle({
  employeeLabel,
  employees,
  selectedEmployee,
  titleLabel,
  selectedTitle,
  phoneLabel,
  selectedPhone,
  hasPhoneField,
  onChange,
  'data-qa': dataQa
}: EmployeeSelectProps) {
  return (
    <FixedSpaceRow>
      <FixedSpaceColumn spacing="zero">
        <Label>{employeeLabel}</Label>
        <Combobox
          items={employees}
          getItemLabel={formatName}
          filterItems={(inputValue: string, items: Employee[]) =>
            items.filter(
              (emp) =>
                formatName(emp)
                  .toLowerCase()
                  .indexOf(inputValue.toLowerCase()) >= 0
            )
          }
          selectedItem={selectedEmployee}
          onChange={(employee) =>
            onChange(employee, selectedTitle, selectedPhone ?? null)
          }
          data-qa={dataQa ? `${dataQa}-select` : undefined}
        />
      </FixedSpaceColumn>
      <FixedSpaceColumn spacing="zero">
        <Label>{titleLabel}</Label>
        <InputField
          value={selectedTitle ?? ''}
          onChange={(title) =>
            onChange(selectedEmployee, title, selectedPhone ?? null)
          }
          data-qa={dataQa ? `${dataQa}-title` : undefined}
        />
      </FixedSpaceColumn>
      {hasPhoneField && (
        <FixedSpaceColumn spacing="zero">
          <Label>{phoneLabel}</Label>
          <InputField
            value={selectedPhone ?? ''}
            onChange={(phone) =>
              onChange(selectedEmployee, selectedTitle, phone)
            }
            data-qa={dataQa ? `${dataQa}-phone` : undefined}
          />
        </FixedSpaceColumn>
      )}
    </FixedSpaceRow>
  )
})

export type FieldInfos = Record<
  keyof AssistanceNeedDecisionForm,
  InputInfo | undefined
>

type AssistanceNeedDecisionFormProps = {
  formState: AssistanceNeedDecisionForm
  setFormState: React.Dispatch<
    SetStateAction<AssistanceNeedDecisionForm | undefined>
  >
  fieldInfos: FieldInfos
}

export default React.memo(function AssistanceNeedDecisionForm({
  formState,
  setFormState,
  fieldInfos
}: AssistanceNeedDecisionFormProps) {
  const [units] = useApiState(() => getUnits([], 'ALL'), [])
  const [employees] = useApiState(() => getEmployees(), [])

  const { i18n, lang } = useTranslation()
  const t = useMemo(() => i18n.childInformation.assistanceNeedDecision, [i18n])

  const setFieldVal = useCallback(
    (val: Partial<AssistanceNeedDecisionForm>) =>
      setFormState({ ...formState, ...val }),
    [formState, setFormState]
  )

  const renderAssistanceLevelRadio = useCallback(
    (level: AssistanceLevel, label: string) => (
      <Radio
        checked={formState.assistanceLevel === level}
        label={label}
        onChange={() => setFieldVal({ assistanceLevel: level })}
      />
    ),
    [formState, setFieldVal]
  )

  return (
    <FixedSpaceColumn spacing={defaultMargins.m}>
      <H2 noMargin>{t.neededTypesOfAssistance}</H2>

      <FieldWithInfo
        info={t.pedagogicalMotivationInfo}
        label={t.pedagogicalMotivation}
        spacing="zero"
      >
        <InputField
          value={formState.pedagogicalMotivation ?? ''}
          onChange={(val) => setFieldVal({ pedagogicalMotivation: val })}
          placeholder={t.genericPlaceholder}
          data-qa="pedagogical-motivation-field"
          info={fieldInfos.pedagogicalMotivation}
        />
      </FieldWithInfo>

      <FieldWithInfo
        info={t.structuralMotivationInfo}
        label={t.structuralMotivation}
      >
        <Checkbox
          label={t.structuralMotivationOptions.smallerGroup}
          checked={formState.structuralMotivationOptions.smallerGroup}
          onChange={(checked: boolean) =>
            setFieldVal({
              structuralMotivationOptions: {
                ...formState.structuralMotivationOptions,
                smallerGroup: checked
              }
            })
          }
        />
        <Checkbox
          label={t.structuralMotivationOptions.specialGroup}
          checked={formState.structuralMotivationOptions.specialGroup}
          onChange={(checked: boolean) =>
            setFieldVal({
              structuralMotivationOptions: {
                ...formState.structuralMotivationOptions,
                specialGroup: checked
              }
            })
          }
        />
        <Checkbox
          label={t.structuralMotivationOptions.smallGroup}
          checked={formState.structuralMotivationOptions.smallGroup}
          onChange={(checked: boolean) =>
            setFieldVal({
              structuralMotivationOptions: {
                ...formState.structuralMotivationOptions,
                smallGroup: checked
              }
            })
          }
        />
        <Checkbox
          label={t.structuralMotivationOptions.groupAssistant}
          checked={formState.structuralMotivationOptions.groupAssistant}
          onChange={(checked: boolean) =>
            setFieldVal({
              structuralMotivationOptions: {
                ...formState.structuralMotivationOptions,
                groupAssistant: checked
              }
            })
          }
        />
        <Checkbox
          label={t.structuralMotivationOptions.childAssistant}
          checked={formState.structuralMotivationOptions.childAssistant}
          onChange={(checked: boolean) =>
            setFieldVal({
              structuralMotivationOptions: {
                ...formState.structuralMotivationOptions,
                childAssistant: checked
              }
            })
          }
        />
        <Checkbox
          label={t.structuralMotivationOptions.additionalStaff}
          checked={formState.structuralMotivationOptions.additionalStaff}
          onChange={(checked: boolean) =>
            setFieldVal({
              structuralMotivationOptions: {
                ...formState.structuralMotivationOptions,
                additionalStaff: checked
              }
            })
          }
        />
        <InputField
          value={formState.structuralMotivationDescription ?? ''}
          onChange={(val) =>
            setFieldVal({ structuralMotivationDescription: val })
          }
          placeholder={t.structuralMotivationPlaceholder}
          info={fieldInfos.structuralMotivationDescription}
        />
      </FieldWithInfo>

      <FieldWithInfo
        info={t.careMotivationInfo}
        label={t.careMotivation}
        spacing="zero"
      >
        <InputField
          value={formState.careMotivation ?? ''}
          onChange={(val) => setFieldVal({ careMotivation: val })}
          placeholder={t.genericPlaceholder}
          info={fieldInfos.careMotivation}
        />
      </FieldWithInfo>

      <FieldWithInfo info={t.servicesInfo} label={t.services}>
        <Checkbox
          label={t.serviceOptions.consultationSpecialEd}
          checked={formState.serviceOptions.consultationSpecialEd}
          onChange={(checked: boolean) =>
            setFieldVal({
              serviceOptions: {
                ...formState.serviceOptions,
                consultationSpecialEd: checked
              }
            })
          }
        />
        <Checkbox
          label={t.serviceOptions.partTimeSpecialEd}
          checked={formState.serviceOptions.partTimeSpecialEd}
          onChange={(checked: boolean) =>
            setFieldVal({
              serviceOptions: {
                ...formState.serviceOptions,
                partTimeSpecialEd: checked
              }
            })
          }
        />
        <Checkbox
          label={t.serviceOptions.fullTimeSpecialEd}
          checked={formState.serviceOptions.fullTimeSpecialEd}
          onChange={(checked: boolean) =>
            setFieldVal({
              serviceOptions: {
                ...formState.serviceOptions,
                fullTimeSpecialEd: checked
              }
            })
          }
        />
        <Checkbox
          label={t.serviceOptions.interpretationAndAssistanceServices}
          checked={formState.serviceOptions.interpretationAndAssistanceServices}
          onChange={(checked: boolean) =>
            setFieldVal({
              serviceOptions: {
                ...formState.serviceOptions,
                interpretationAndAssistanceServices: checked
              }
            })
          }
        />
        <Checkbox
          label={t.serviceOptions.specialAides}
          checked={formState.serviceOptions.specialAides}
          onChange={(checked: boolean) =>
            setFieldVal({
              serviceOptions: {
                ...formState.serviceOptions,
                specialAides: checked
              }
            })
          }
        />
        <InputField
          value={formState.servicesMotivation ?? ''}
          onChange={(val) => setFieldVal({ servicesMotivation: val })}
          placeholder={t.servicesPlaceholder}
          info={fieldInfos.servicesMotivation}
        />
      </FieldWithInfo>

      <H2>{t.collaborationWithGuardians}</H2>

      <FixedSpaceColumn spacing="zero">
        <Label>{t.guardiansHeardAt}</Label>
        <DatePicker
          locale={lang}
          date={formState.guardiansHeardOn}
          onChange={(date) => {
            setFieldVal({ guardiansHeardOn: date })
          }}
          errorTexts={i18n.validationErrors}
          hideErrorsBeforeTouched
          info={fieldInfos.guardiansHeardOn}
        />
      </FixedSpaceColumn>

      <FieldWithInfo info={t.guardiansHeardInfo} label={t.guardiansHeard}>
        {formState.guardianInfo.map((guardian) => (
          <GuardianHeardField
            key={guardian.personId}
            guardian={guardian}
            placeholder={t.genericPlaceholder}
            onChange={(g: AssistanceNeedDecisionGuardian) => {
              setFieldVal({
                guardianInfo: formState.guardianInfo.map((gi) =>
                  gi.name === g.name ? g : gi
                )
              })
            }}
          />
        ))}
        <GuardianHeardField
          guardian={{
            id: null,
            personId: null,
            name: t.otherLegalRepresentation,
            isHeard: formState.otherRepresentativeHeard,
            details: formState.otherRepresentativeDetails
          }}
          placeholder={t.genericPlaceholder}
          onChange={(g: AssistanceNeedDecisionGuardian) => {
            setFieldVal({
              otherRepresentativeHeard: g.isHeard,
              otherRepresentativeDetails: g.details
            })
          }}
        />
      </FieldWithInfo>
      <FieldWithInfo
        info={t.viewOfTheGuardiansInfo}
        label={t.viewOfTheGuardians}
        spacing="zero"
      >
        <InputField
          value={formState.viewOfGuardians ?? ''}
          onChange={(val) => setFieldVal({ viewOfGuardians: val })}
          placeholder={t.genericPlaceholder}
          info={fieldInfos.viewOfGuardians}
        />
      </FieldWithInfo>

      <H2>{t.decisionAndValidity}</H2>
      <FixedSpaceColumn spacing={defaultMargins.s}>
        <Label>{t.futureLevelOfAssistance}</Label>
        {renderAssistanceLevelRadio(
          'ASSISTANCE_ENDS',
          t.assistanceLevel.assistanceEnds
        )}
        <FixedSpaceRow alignItems="center">
          {renderAssistanceLevelRadio(
            'ASSISTANCE_SERVICES_FOR_TIME',
            t.assistanceLevel.assistanceServicesForTime
          )}
          <DateRangeInput
            start={
              formState.assistanceServicesTime?.start ??
              LocalDate.todayInHelsinkiTz()
            }
            end={
              formState.assistanceServicesTime?.end ??
              LocalDate.todayInHelsinkiTz().addYears(1)
            }
            onValidationResult={(_hasErrors: boolean) => {
              // implement error handling here
            }}
            onChange={(start: LocalDate, end: LocalDate) =>
              setFieldVal({
                assistanceServicesTime: new FiniteDateRange(start, end)
              })
            }
          />
        </FixedSpaceRow>
        {renderAssistanceLevelRadio(
          'ENHANCED_ASSISTANCE',
          t.assistanceLevel.enhancedAssistance
        )}
        {renderAssistanceLevelRadio(
          'SPECIAL_ASSISTANCE',
          t.assistanceLevel.specialAssistance
        )}
      </FixedSpaceColumn>

      <FieldWithInfo info={t.startDateInfo} label={t.startDate} spacing="zero">
        <DatePicker
          locale={lang}
          date={formState.startDate}
          onChange={(date) => {
            setFieldVal({ startDate: date })
          }}
          errorTexts={i18n.validationErrors}
          hideErrorsBeforeTouched
          info={fieldInfos.startDate}
        />
      </FieldWithInfo>

      <FixedSpaceColumn spacing="zero">
        <Label>{t.selectedUnit}</Label>
        {renderResult(units, (units) => (
          <Combobox
            items={units}
            getItemLabel={(unit) => unit.name}
            selectedItem={
              units.find((u) => u.id === formState.selectedUnit?.id) ?? null
            }
            onChange={(unit) =>
              setFieldVal({ selectedUnit: unit && { id: unit.id } })
            }
            info={fieldInfos.selectedUnit}
            data-qa="unit-select"
          />
        ))}
        <Gap size="s" />
        <span>{t.unitMayChange}</span>
      </FixedSpaceColumn>

      <FixedSpaceColumn spacing="zero">
        <Label>{t.motivationForDecision}</Label>
        <InputField
          value={formState.motivationForDecision ?? ''}
          onChange={(val) => setFieldVal({ motivationForDecision: val })}
          placeholder={t.genericPlaceholder}
          info={fieldInfos.motivationForDecision}
        />
      </FixedSpaceColumn>

      <H2>{t.personsResponsible}</H2>
      {renderResult(employees, (employees) => (
        <FixedSpaceColumn>
          <EmployeeSelectWithTitle
            employeeLabel={t.preparator}
            employees={employees}
            selectedEmployee={
              employees.find(
                (employee) => employee.id === formState.preparedBy1?.employeeId
              ) || null
            }
            titleLabel={t.title}
            selectedTitle={formState.preparedBy1?.title ?? ''}
            onChange={(employee, title, phoneNumber) =>
              setFieldVal({
                preparedBy1: {
                  employeeId: employee?.id ?? null,
                  title,
                  phoneNumber
                }
              })
            }
            phoneLabel={i18n.common.form.phone}
            selectedPhone={formState.preparedBy1?.phoneNumber}
          />
          <EmployeeSelectWithTitle
            employeeLabel={t.preparator}
            employees={employees}
            selectedEmployee={
              employees.find(
                (employee) => employee.id === formState.preparedBy2?.employeeId
              ) || null
            }
            titleLabel={t.title}
            selectedTitle={formState.preparedBy2?.title ?? ''}
            onChange={(employee, title, phoneNumber) =>
              setFieldVal({
                preparedBy2: {
                  employeeId: employee?.id ?? null,
                  title,
                  phoneNumber
                }
              })
            }
            phoneLabel={i18n.common.form.phone}
            selectedPhone={formState.preparedBy2?.phoneNumber}
          />
          <EmployeeSelectWithTitle
            employeeLabel={t.decisionMaker}
            employees={employees}
            selectedEmployee={
              employees.find(
                (employee) =>
                  employee.id === formState.decisionMaker?.employeeId
              ) || null
            }
            titleLabel={t.title}
            selectedTitle={formState.decisionMaker?.title ?? ''}
            onChange={(employee, title) =>
              setFieldVal({
                decisionMaker: {
                  employeeId: employee?.id ?? null,
                  title
                }
              })
            }
            data-qa="decision-maker"
          />
        </FixedSpaceColumn>
      ))}

      <P>{t.disclaimer}</P>
    </FixedSpaceColumn>
  )
})
