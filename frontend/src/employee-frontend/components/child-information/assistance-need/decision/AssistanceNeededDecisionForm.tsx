// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { SetStateAction, useCallback, useMemo } from 'react'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import { AutosaveStatus } from 'employee-frontend/utils/use-autosave'
import { Result } from 'lib-common/api'
import {
  AssistanceLevel,
  AssistanceNeedDecisionForm,
  AssistanceNeedDecisionGuardian
} from 'lib-common/generated/api-types/assistanceneed'
import { Employee } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { ParagraphDiv } from 'lib-components/assistance-need-decision/AssistanceNeedDecisionReadOnly'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField, { InputInfo } from 'lib-components/atoms/form/InputField'
import TextArea from 'lib-components/atoms/form/TextArea'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H2, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { getEmployeesQuery } from '../../../../queries'
import { unitsQuery } from '../../queries'

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
      <ExpandingInfo info={info}>
        <Label>{label}</Label>
      </ExpandingInfo>
      {children}
    </FixedSpaceColumn>
  )
})

const GuardianHeardField = React.memo(function GuardianHeardField({
  guardian,
  placeholder,
  onChange,
  info
}: {
  guardian: AssistanceNeedDecisionGuardian
  placeholder: string
  onChange: (guardian: AssistanceNeedDecisionGuardian) => void
  info?: InputInfo
}) {
  return (
    <div>
      <Checkbox
        checked={guardian.isHeard ?? false}
        label={guardian.name ?? ''}
        onChange={(checked) => onChange({ ...guardian, isHeard: checked })}
      />
      <TextArea
        value={guardian.details ?? ''}
        placeholder={placeholder}
        onChange={(val) => onChange({ ...guardian, details: val })}
        info={info}
      />
    </div>
  )
})

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
  info?: InputInfo
  titleInfo?: InputInfo
  disabled?: boolean
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
  'data-qa': dataQa,
  info,
  titleInfo,
  disabled
}: EmployeeSelectProps) {
  return (
    <FixedSpaceRow>
      <FixedSpaceColumn spacing="zero">
        <Label>{employeeLabel}</Label>
        <Combobox
          items={employees}
          getItemLabel={(employee: Employee) =>
            `${employee.lastName ?? ''} ${
              employee.preferredFirstName ?? employee.firstName ?? ''
            } (${employee.email ?? ''})`
          }
          filterItems={(inputValue: string, items: readonly Employee[]) =>
            items.filter(
              (emp) =>
                (emp.preferredFirstName ?? emp.firstName)
                  .toLowerCase()
                  .startsWith(inputValue.toLowerCase()) ||
                emp.lastName.toLowerCase().startsWith(inputValue.toLowerCase())
            )
          }
          selectedItem={selectedEmployee}
          onChange={(employee) =>
            onChange(employee, selectedTitle, selectedPhone ?? null)
          }
          data-qa={dataQa ? `${dataQa}-select` : undefined}
          info={info}
          clearable
          disabled={disabled}
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
          width="L"
          info={titleInfo}
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

export interface FieldInfos {
  startDate?: InputInfo
  endDate?: InputInfo
  assistanceLevels?: InputInfo
  preparator?: InputInfo
  pedagogicalMotivation?: InputInfo
  careMotivation?: InputInfo
  guardiansHeardOn?: InputInfo
  guardianInfo?: InputInfo
  viewOfGuardians?: InputInfo
  servicesMotivation?: InputInfo
  selectedUnit?: InputInfo
  motivationForDecision?: InputInfo
  decisionMaker?: InputInfo
  decisionMakerTitle?: InputInfo
  preparator1Title?: InputInfo
  preparator2Title?: InputInfo
  guardianDetails?: Record<string, InputInfo>
  otherRepresentative?: InputInfo
}

type AssistanceNeedDecisionFormProps = {
  status: AutosaveStatus
  decisionMakerOptions: Result<Employee[]>
  formState: AssistanceNeedDecisionForm
  setFormState: React.Dispatch<
    SetStateAction<AssistanceNeedDecisionForm | undefined>
  >
  fieldInfos: FieldInfos
}

export default React.memo(function AssistanceNeedDecisionForm({
  status,
  decisionMakerOptions,
  formState,
  setFormState,
  fieldInfos
}: AssistanceNeedDecisionFormProps) {
  const units = useQueryResult(
    unitsQuery({
      areaIds: null,
      type: 'ALL',
      from: LocalDate.todayInHelsinkiTz()
    })
  )
  const employees = useQueryResult(getEmployeesQuery())

  const { i18n, lang } = useTranslation()
  const t = useMemo(() => i18n.childInformation.assistanceNeedDecision, [i18n])

  const setFieldVal = useCallback(
    (val: Partial<AssistanceNeedDecisionForm>) =>
      setFormState({ ...formState, ...val }),
    [formState, setFormState]
  )

  const renderAssistanceLevelMultiCheckbox = useCallback(
    (level: AssistanceLevel, label: string, dataQa: string) => (
      <Checkbox
        checked={formState.assistanceLevels.includes(level)}
        label={label}
        onChange={(checked) =>
          setFieldVal({
            assistanceLevels: checked
              ? [...formState.assistanceLevels, level]
              : formState.assistanceLevels.filter((level2) => level !== level2)
          })
        }
        data-qa={dataQa}
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
        <TextArea
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
        <TextArea
          value={formState.structuralMotivationDescription ?? ''}
          onChange={(val) =>
            setFieldVal({ structuralMotivationDescription: val })
          }
          placeholder={t.structuralMotivationPlaceholder}
        />
      </FieldWithInfo>

      <FieldWithInfo
        info={t.careMotivationInfo}
        label={t.careMotivation}
        spacing="zero"
      >
        <TextArea
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
        <TextArea
          value={formState.servicesMotivation ?? ''}
          onChange={(val) => setFieldVal({ servicesMotivation: val })}
          placeholder={t.servicesPlaceholder}
          info={fieldInfos.servicesMotivation}
        />
      </FieldWithInfo>

      <H2>{t.collaborationWithGuardians}</H2>

      <FixedSpaceColumn spacing="zero">
        <Label>{t.guardiansHeardOn}</Label>
        <DatePicker
          locale={lang}
          date={formState.guardiansHeardOn}
          onChange={(date) => {
            setFieldVal({ guardiansHeardOn: date })
          }}
          hideErrorsBeforeTouched={!fieldInfos.guardiansHeardOn}
          info={fieldInfos.guardiansHeardOn}
          data-qa="guardians-heard-on"
        />
      </FixedSpaceColumn>

      <FieldWithInfo info={t.guardiansHeardInfo} label={t.guardiansHeard}>
        {fieldInfos.guardianInfo && (
          <>
            <AlertBox message={fieldInfos.guardianInfo.text} noMargin />
            <Gap size="s" />
          </>
        )}
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
            info={
              guardian.id
                ? fieldInfos.guardianDetails?.[guardian.id]
                : undefined
            }
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
          info={fieldInfos.otherRepresentative}
        />
      </FieldWithInfo>
      <FieldWithInfo
        info={t.viewOfTheGuardiansInfo}
        label={t.viewOfTheGuardians}
        spacing="zero"
      >
        <TextArea
          value={formState.viewOfGuardians ?? ''}
          onChange={(val) => setFieldVal({ viewOfGuardians: val })}
          placeholder={t.genericPlaceholder}
          info={fieldInfos.viewOfGuardians}
        />
      </FieldWithInfo>

      <H2>{t.decisionAndValidity}</H2>
      <FixedSpaceColumn spacing={defaultMargins.s}>
        <Label>{t.futureLevelOfAssistance}</Label>
        {renderAssistanceLevelMultiCheckbox(
          'SPECIAL_ASSISTANCE',
          t.assistanceLevel.specialAssistance,
          'special-assistance'
        )}
        {renderAssistanceLevelMultiCheckbox(
          'ENHANCED_ASSISTANCE',
          t.assistanceLevel.enhancedAssistance,
          'enchanced-assistance'
        )}
        {renderAssistanceLevelMultiCheckbox(
          'ASSISTANCE_SERVICES_FOR_TIME',
          t.assistanceLevel.assistanceServicesForTime,
          'assistance-services-for-time'
        )}
        {renderAssistanceLevelMultiCheckbox(
          'ASSISTANCE_ENDS',
          t.assistanceLevel.assistanceEnds,
          'assistance-ends'
        )}
        {fieldInfos.assistanceLevels &&
          fieldInfos.assistanceLevels.status === 'warning' && (
            <AlertBox message={fieldInfos.assistanceLevels.text} thin />
          )}
      </FixedSpaceColumn>

      <FieldWithInfo
        info={
          formState.assistanceLevels.includes('ASSISTANCE_SERVICES_FOR_TIME')
            ? t.startDateInfo
            : `${t.startDateIndefiniteInfo} ${t.startDateInfo}`
        }
        label={t.startDate}
        spacing="zero"
      >
        <DatePicker
          locale={lang}
          date={formState.validityPeriod.start}
          onChange={(date) => {
            if (date) {
              setFieldVal({
                validityPeriod: formState.validityPeriod.withStart(date)
              })
            } else {
              // Setting an invalid date doesn't have any effect
            }
          }}
          hideErrorsBeforeTouched={!fieldInfos.startDate}
          info={fieldInfos.startDate}
          maxDate={formState.validityPeriod.end ?? undefined}
          required
          data-qa="validity-start-date"
        />
      </FieldWithInfo>

      {formState.assistanceLevels.includes('ASSISTANCE_SERVICES_FOR_TIME') && (
        <FixedSpaceColumn spacing="zero">
          <Label>{t.endDateServices}</Label>
          <DatePicker
            locale={lang}
            date={formState.validityPeriod.end}
            onChange={(date) => {
              setFieldVal({
                validityPeriod: formState.validityPeriod.withEnd(date)
              })
            }}
            hideErrorsBeforeTouched={!fieldInfos.endDate}
            info={fieldInfos.endDate}
            minDate={formState.validityPeriod.start}
            data-qa="validity-end-date"
          />
          <Gap size="xs" />
          <Checkbox
            label={t.endDateNotKnown}
            checked={formState.endDateNotKnown}
            onChange={(checked) => setFieldVal({ endDateNotKnown: checked })}
            data-qa="end-date-not-known-checkbox"
          />
        </FixedSpaceColumn>
      )}

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
        <TextArea
          value={formState.motivationForDecision ?? ''}
          onChange={(val) => setFieldVal({ motivationForDecision: val })}
          placeholder={t.genericPlaceholder}
          info={fieldInfos.motivationForDecision}
        />
      </FixedSpaceColumn>

      <H2>{t.legalInstructions}</H2>
      <P noMargin>{t.legalInstructionsText}</P>

      <H2>{t.jurisdiction}</H2>
      <ParagraphDiv>{t.jurisdictionText()}</ParagraphDiv>

      <H2>{t.personsResponsible}</H2>
      <FixedSpaceColumn>
        {renderResult(employees, (employees) => (
          <>
            <EmployeeSelectWithTitle
              employeeLabel={t.preparator}
              employees={employees}
              selectedEmployee={
                employees.find(
                  (employee) =>
                    employee.id === formState.preparedBy1?.employeeId
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
              phoneLabel={t.tel}
              selectedPhone={formState.preparedBy1?.phoneNumber}
              hasPhoneField
              info={fieldInfos.preparator}
              titleInfo={fieldInfos.preparator1Title}
              data-qa="prepared-by-1"
            />
            <EmployeeSelectWithTitle
              employeeLabel={t.preparator}
              employees={employees}
              selectedEmployee={
                employees.find(
                  (employee) =>
                    employee.id === formState.preparedBy2?.employeeId
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
              phoneLabel={t.tel}
              selectedPhone={formState.preparedBy2?.phoneNumber}
              hasPhoneField
              titleInfo={fieldInfos.preparator2Title}
            />
          </>
        ))}
        {renderResult(decisionMakerOptions, (employees) => (
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
            info={fieldInfos.decisionMaker}
            titleInfo={fieldInfos.decisionMakerTitle}
            disabled={status.state !== 'clean'}
          />
        ))}
      </FixedSpaceColumn>

      <P>{t.disclaimer}</P>
    </FixedSpaceColumn>
  )
})
