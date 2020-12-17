// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import styled from 'styled-components'
import Select from '~components/common/Select'
import {
  CareArea,
  Coordinate,
  OpenEndedPeriod,
  ProviderType,
  Unit,
  UnitLanguage,
  UnitTypes
} from '~types/unit'
import { DatePicker, DatePickerClearable } from '~components/common/DatePicker'
import InputField from 'components/shared/atoms/form/InputField'
import Button from '~components/shared/atoms/buttons/Button'
import Radio from '~components/shared/atoms/form/Radio'
import Checkbox from '~components/shared/atoms/form/Checkbox'
import { DaycareFields } from '~api/unit'
import { DayOfWeek, UUID } from '~types'
import { Result } from '~api'
import { Translations, useTranslation } from '~state/i18n'
import { EspooColours } from '~utils/colours'
import InlineButton from 'components/shared/atoms/buttons/InlineButton'
import { faPen } from '@evaka/lib-icons'
import { H1, H3 } from '@evaka/lib-components/src/typography'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '~components/shared/layout/flex-helpers'

type CareType = 'DAYCARE' | 'PRESCHOOL' | 'PREPARATORY_EDUCATION' | 'CLUB'
type DaycareType = 'CENTRE' | 'FAMILY' | 'GROUP_FAMILY'

interface FormData {
  name: string
  openingDate: LocalDate | null
  closingDate: LocalDate | null
  areaId: string
  careTypes: Record<CareType, boolean>
  daycareType: DaycareType | undefined
  daycareApplyPeriod: OpenEndedPeriod | null
  preschoolApplyPeriod: OpenEndedPeriod | null
  clubApplyPeriod: OpenEndedPeriod | null
  providerType: ProviderType
  roundTheClock: boolean
  capacity: string
  language: UnitLanguage
  ghostUnit: boolean
  uploadToVarda: boolean
  uploadToKoski: boolean
  invoicedByMunicipality: boolean
  costCenter: string
  additionalInfo: string
  phone: string
  email: string
  url: string
  visitingAddress: Address
  location: string
  mailingAddress: Address
  unitManager: UnitManager
  decisionCustomization: UnitDecisionCustomization
  ophUnitOid: string
  ophOrganizerOid: string
  ophOrganizationOid: string
  operationDays: DayOfWeek[]
}

interface UnitDecisionCustomization {
  daycareName: string
  preschoolName: string
  handler: string
  handlerAddress: string
}

interface UnitManager {
  name: string
  phone: string
  email: string
}

interface Address {
  streetAddress: string
  postalCode: string
  postOffice: string
}

const TopBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: ${defaultMargins.m};
`

const FormPart = styled.div`
  display: flex;
  margin-bottom: 20px;

  & > :first-child {
    font-weight: bold;
    width: 250px;
    flex: 0 0 auto;
    margin-right: 20px;
  }

  & .input-group .group-label {
    display: none;
  }

  & > :nth-child(2) {
    margin: 0;
  }
`

const FormError = styled.div`
  color: ${EspooColours.red};
  margin-bottom: 20px;
`

const DaycareTypeSelectContainer = styled.div`
  display: flex;

  & > :first-child {
    margin-right: 20px;
  }

  & > * {
    flex: 0 1 auto;
  }
`

const OperationDaysContainer = styled.div`
  display: flex;
  align-items: center;
`

const CapacityInputContainer = styled.div`
  display: flex;
  align-items: center;

  & input {
    max-width: 200px;
    display: inline-block;
    margin-right: 20px;
  }
`

const AddressSecondRowContainer = styled.div`
  display: flex;
  justify-content: space-between;
  & > :first-child {
    margin-right: 20px;
  }
  & input {
    width: 200px;
  }
`

const IndentChckboxLabel = styled.div`
  margin-left: 46px;
`

const Url = styled.a`
  word-break: break-all;
`

function AddressEditor({
  editable,
  address,
  onChange,
  dataQaPrefix
}: {
  editable: boolean
  address: Address
  onChange: (address: Address) => void
  dataQaPrefix: string
}) {
  const { i18n } = useTranslation()
  const update = (updates: Partial<Address>) =>
    onChange({ ...address, ...updates })
  if (!editable) {
    return (
      <div>
        <div>{address.streetAddress}</div>
        <div>
          {[address.postalCode, address.postOffice]
            .filter((x) => !!x)
            .join(' ')}
        </div>
      </div>
    )
  }
  return (
    <div>
      <InputField
        value={address.streetAddress}
        placeholder={i18n.unitEditor.placeholder.streetAddress}
        onChange={(value) => update({ streetAddress: value })}
        dataQa={`${dataQaPrefix}-street-input`}
      />
      <AddressSecondRowContainer>
        <InputField
          value={address.postalCode}
          placeholder={i18n.unitEditor.placeholder.postalCode}
          onChange={(value) => update({ postalCode: value })}
          dataQa={`${dataQaPrefix}-postal-code-input`}
        />
        <InputField
          value={address.postOffice}
          placeholder={i18n.unitEditor.placeholder.postOffice}
          onChange={(value) => update({ postOffice: value })}
          dataQa={`${dataQaPrefix}-post-office-input`}
        />
      </AddressSecondRowContainer>
    </div>
  )
}

function formatCoordinate(coordinate: Coordinate | null | undefined): string {
  if (!coordinate) return ''
  return `${coordinate.lat}, ${coordinate.lon}`
}

function parseLocation(value: string): Coordinate | undefined {
  const parts = value.split(',').map((str) => str.trim())
  if (parts.length != 2) return undefined
  const lat = parseFloat(parts[0])
  const lon = parseFloat(parts[1])
  // Rough coordinate limits for Finland
  if (!(lat >= 59.6 && lat < 70.0)) {
    return undefined
  }
  if (!(lon >= 20.5 && lon < 31.7)) {
    return undefined
  }
  return { lat, lon }
}

interface Props {
  areas: CareArea[]
  unit?: Unit
  editable: boolean
  onClickCancel?: () => void
  onClickEdit?: () => void
  onSubmit: (fields: DaycareFields, id: UUID | undefined) => void
  submit: Result<void> | undefined
}

function validateForm(
  i18n: Translations,
  form: FormData
): [DaycareFields | undefined, string[]] {
  const errors: string[] = []
  const typeMap: Record<UnitTypes, boolean> = {
    CLUB: form.careTypes.CLUB,
    CENTRE: form.daycareType === 'CENTRE',
    PRESCHOOL: form.careTypes.PRESCHOOL,
    PREPARATORY_EDUCATION: form.careTypes.PREPARATORY_EDUCATION,
    FAMILY: form.daycareType === 'FAMILY',
    GROUP_FAMILY: form.daycareType === 'GROUP_FAMILY'
  }
  const type = (Object.keys(typeMap) as UnitTypes[]).filter(
    (key) => typeMap[key]
  )
  const name = form.name.trim() || null
  const capacity = parseInt(form.capacity, 10)
  const costCenter = form.costCenter.trim() || null
  const additionalInfo = form.additionalInfo.trim() || null
  const phone = form.phone.trim() || null
  const email = form.email.trim() || null
  const url = form.url.trim() || null
  const visitingAddress = {
    streetAddress: form.visitingAddress.streetAddress.trim() || null,
    postalCode: form.visitingAddress.postalCode.trim() || null,
    postOffice: form.visitingAddress.postOffice.trim() || null
  }
  const location = parseLocation(form.location) ?? null
  const mailingAddress = {
    streetAddress: '',
    poBox: form.mailingAddress.streetAddress.trim() || null,
    postalCode: form.mailingAddress.postalCode.trim() || null,
    postOffice: form.mailingAddress.postOffice.trim() || null
  }
  const unitManager = {
    name: form.unitManager.name.trim() || null,
    email: form.unitManager.email.trim() || null,
    phone: form.unitManager.phone.trim() || null
  }
  const decisionCustomization = {
    daycareName: form.decisionCustomization.daycareName.trim(),
    preschoolName: form.decisionCustomization.preschoolName.trim(),
    handler: form.decisionCustomization.handler.trim(),
    handlerAddress: form.decisionCustomization.handlerAddress.trim()
  }

  if (!name) {
    errors.push(i18n.unitEditor.error.name)
  }
  if (!form.areaId) {
    errors.push(i18n.unitEditor.error.area)
  }
  if (Object.values(form.careTypes).every((v) => !v)) {
    errors.push(i18n.unitEditor.error.careType)
  }
  if (form.careTypes.DAYCARE && !form.daycareType) {
    errors.push(i18n.unitEditor.error.daycareType)
  }
  if (!Number.isSafeInteger(capacity)) {
    errors.push(i18n.unitEditor.error.capacity)
  }
  if (form.invoicedByMunicipality && !costCenter) {
    errors.push(i18n.unitEditor.error.costCenter)
  }
  if (!visitingAddress.streetAddress) {
    errors.push(i18n.unitEditor.error.visitingAddress.streetAddress)
  }
  if (!visitingAddress.postalCode) {
    errors.push(i18n.unitEditor.error.visitingAddress.postalCode)
  }
  if (!visitingAddress.postOffice) {
    errors.push(i18n.unitEditor.error.visitingAddress.postOffice)
  }
  if (form.location && !location) {
    errors.push(i18n.unitEditor.error.location)
  }
  if (!unitManager.name) {
    errors.push(i18n.unitEditor.error.unitManager.name)
  }
  if (!unitManager.phone) {
    errors.push(i18n.unitEditor.error.unitManager.phone)
  }
  if (!unitManager.email) {
    errors.push(i18n.unitEditor.error.unitManager.email)
  }
  if (
    (!form.careTypes.DAYCARE && form.daycareApplyPeriod != null) ||
    (!form.careTypes.PRESCHOOL && form.preschoolApplyPeriod != null) ||
    (!form.careTypes.CLUB && form.clubApplyPeriod != null)
  ) {
    errors.push(i18n.unitEditor.error.cannotApplyToDifferentType)
  }
  const {
    openingDate,
    closingDate,
    areaId,
    daycareApplyPeriod,
    preschoolApplyPeriod,
    clubApplyPeriod,
    providerType,
    roundTheClock,
    language,
    ghostUnit,
    uploadToVarda,
    uploadToKoski,
    invoicedByMunicipality,
    ophUnitOid,
    ophOrganizerOid,
    ophOrganizationOid,
    operationDays
  } = form

  if (
    name &&
    visitingAddress.streetAddress &&
    visitingAddress.postalCode &&
    visitingAddress.postOffice
  ) {
    return [
      {
        name,
        openingDate,
        closingDate,
        areaId,
        type,
        daycareApplyPeriod,
        preschoolApplyPeriod,
        clubApplyPeriod,
        providerType,
        roundTheClock,
        capacity,
        language,
        ghostUnit,
        uploadToVarda,
        uploadToKoski,
        invoicedByMunicipality,
        costCenter,
        additionalInfo,
        phone,
        email,
        url,
        visitingAddress: {
          streetAddress: visitingAddress.streetAddress,
          postalCode: visitingAddress.postalCode,
          postOffice: visitingAddress.postOffice
        },
        location,
        mailingAddress,
        unitManager,
        decisionCustomization: {
          daycareName: decisionCustomization.daycareName,
          preschoolName: decisionCustomization.preschoolName,
          handler: decisionCustomization.handler,
          handlerAddress: decisionCustomization.handlerAddress
        },
        ophUnitOid,
        ophOrganizerOid,
        ophOrganizationOid,
        operationDays
      },
      errors
    ]
  } else {
    return [undefined, errors]
  }
}

function toFormData(unit: Unit | undefined): FormData {
  const type = unit?.type
  return {
    name: unit?.name ?? '',
    openingDate: unit?.openingDate ?? null,
    closingDate: unit?.closingDate ?? null,
    areaId: unit?.area?.id ?? '',
    careTypes: {
      DAYCARE:
        type?.some(
          (x) => x === 'CENTRE' || x === 'FAMILY' || x === 'GROUP_FAMILY'
        ) ?? false,
      PREPARATORY_EDUCATION: type?.includes('PREPARATORY_EDUCATION') ?? false,
      PRESCHOOL: type?.includes('PRESCHOOL') ?? false,
      CLUB: type?.includes('CLUB') ?? false
    },
    daycareType: type?.includes('FAMILY')
      ? 'FAMILY'
      : type?.includes('GROUP_FAMILY')
      ? 'GROUP_FAMILY'
      : type?.includes('CENTRE')
      ? 'CENTRE'
      : undefined,
    daycareApplyPeriod: unit?.daycareApplyPeriod ?? null,
    preschoolApplyPeriod: unit?.preschoolApplyPeriod ?? null,
    clubApplyPeriod: unit?.clubApplyPeriod ?? null,
    providerType: unit?.providerType ?? 'MUNICIPAL',
    roundTheClock: unit?.roundTheClock ?? false,
    capacity: (unit?.capacity ?? 0).toString(),
    language: unit?.language ?? 'fi',
    ghostUnit: unit?.ghostUnit ?? false,
    uploadToVarda: unit?.uploadToVarda ?? false,
    uploadToKoski: unit?.uploadToKoski ?? false,
    invoicedByMunicipality: unit?.invoicedByMunicipality ?? false,
    costCenter: unit?.costCenter ?? '',
    additionalInfo: unit?.additionalInfo ?? '',
    phone: unit?.phone ?? '',
    email: unit?.email ?? '',
    url: unit?.url ?? '',
    ophUnitOid: unit?.ophUnitOid ?? '',
    ophOrganizerOid: unit?.ophOrganizerOid ?? '',
    ophOrganizationOid: unit?.ophOrganizationOid ?? '',
    visitingAddress: {
      streetAddress: unit?.visitingAddress?.streetAddress ?? '',
      postalCode: unit?.visitingAddress?.postalCode ?? '',
      postOffice: unit?.visitingAddress?.postOffice ?? ''
    },
    location: formatCoordinate(unit?.location),
    mailingAddress: {
      streetAddress: unit?.mailingAddress.poBox ?? '',
      postalCode: unit?.mailingAddress.postalCode ?? '',
      postOffice: unit?.mailingAddress.postOffice ?? ''
    },
    decisionCustomization: {
      daycareName: unit?.decisionCustomization.daycareName ?? '',
      handler: unit?.decisionCustomization.handler ?? '',
      handlerAddress: unit?.decisionCustomization.handlerAddress ?? '',
      preschoolName: unit?.decisionCustomization.preschoolName ?? ''
    },
    unitManager: {
      name: unit?.unitManager?.name ?? '',
      phone: unit?.unitManager?.phone ?? '',
      email: unit?.unitManager?.email ?? ''
    },
    operationDays: unit?.operationDays ?? []
  }
}

export default function UnitEditor(props: Props): JSX.Element {
  const { i18n } = useTranslation()

  const initialData = useMemo<FormData>(() => toFormData(props.unit), [
    props.unit
  ])
  const [form, setForm] = useState<FormData>(initialData)
  const [formErrors, setFormErrors] = useState<string[]>([])
  const { careTypes, decisionCustomization, unitManager } = form

  const updateForm = (updates: Partial<FormData>) => {
    const newForm = { ...form, ...updates }
    setForm(newForm)
    const [, errors] = validateForm(i18n, newForm)
    setFormErrors(errors)
  }
  const updateCareTypes = (updates: Partial<Record<CareType, boolean>>) =>
    updateForm({ careTypes: { ...form.careTypes, ...updates } })
  const updateDecisionCustomization = (
    updates: Partial<UnitDecisionCustomization>
  ) =>
    updateForm({
      decisionCustomization: { ...form.decisionCustomization, ...updates }
    })
  const updateUnitManager = (updates: Partial<UnitManager>) =>
    updateForm({ unitManager: { ...form.unitManager, ...updates } })

  const areaOptions = props.areas.map(({ id, name }) => ({
    value: id,
    label: name
  }))

  const onClickSubmit = (e: React.MouseEvent) => {
    e.preventDefault()
    const [fields, errors] = validateForm(i18n, form)
    setFormErrors(errors)
    if (fields && errors.length === 0) {
      props.onSubmit(fields, props.unit?.id)
    }
  }

  const onClickEditHandler = () => {
    const [, errors] = validateForm(i18n, form)
    setFormErrors(errors)
    if (props.onClickEdit) props.onClickEdit()
  }

  const isNewUnit = !props.unit
  const showRequired = props.editable
    ? (label: string) => `${label}*`
    : (label: string) => label

  return (
    <form action="#">
      {props.unit && (
        <TopBar>
          <H1 fitted>{props.unit.name}</H1>
          {!props.editable && (
            <InlineButton
              icon={faPen}
              onClick={onClickEditHandler}
              text={i18n.common.edit}
              dataQa={'enable-edit-button'}
            />
          )}
        </TopBar>
      )}
      {isNewUnit && <H1>{i18n.titles.createUnit}</H1>}
      <H3>{i18n.unit.info.title}</H3>
      <FormPart>
        <label htmlFor="unit-name">
          {showRequired(i18n.unitEditor.label.name)}
        </label>
        {props.editable ? (
          <InputField
            id="unit-name"
            placeholder={i18n.unitEditor.placeholder.name}
            value={form.name}
            onChange={(value) => updateForm({ name: value })}
            dataQa="unit-name-input"
          />
        ) : (
          <div>{form.name}</div>
        )}
      </FormPart>
      <FormPart>
        <div>{`${i18n.unitEditor.label.openingDate} / ${i18n.unitEditor.label.closingDate}`}</div>
        <div>
          {props.editable ? (
            <DatePicker
              date={form.openingDate ?? undefined}
              options={{
                placeholderText: i18n.unitEditor.placeholder.openingDate
              }}
              onChange={(openingDate) => updateForm({ openingDate })}
            />
          ) : (
            form.openingDate?.format()
          )}
          {' - '}
          {props.editable ? (
            <DatePickerClearable
              date={form.closingDate}
              options={{
                placeholderText: i18n.unitEditor.placeholder.closingDate
              }}
              onCleared={() => updateForm({ closingDate: null })}
              onChange={(closingDate) => updateForm({ closingDate })}
            />
          ) : (
            form.closingDate?.format()
          )}
        </div>
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.area)}</div>
        {props.editable ? (
          <Select
            options={areaOptions}
            placeholder={i18n.unitEditor.placeholder.area}
            onChange={(value) =>
              value && 'value' in value
                ? updateForm({ areaId: value.value })
                : undefined
            }
          />
        ) : (
          props.areas.find((area) => area.id === form.areaId)?.name
        )}
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.careTypes)}</div>
        <FixedSpaceColumn>
          <DaycareTypeSelectContainer>
            <Checkbox
              disabled={!props.editable}
              checked={careTypes.DAYCARE}
              label={i18n.common.types.DAYCARE}
              onChange={(checked) =>
                updateForm({
                  careTypes: { ...form.careTypes, DAYCARE: checked },
                  daycareType: checked ? form.daycareType : undefined
                })
              }
              dataQa="care-type-checkbox-DAYCARE"
            />
            {form.careTypes.DAYCARE && (
              <Select
                disabled={!props.editable}
                options={[
                  { value: 'CENTRE', label: i18n.common.types.CENTRE },
                  { value: 'FAMILY', label: i18n.common.types.FAMILY },
                  {
                    value: 'GROUP_FAMILY',
                    label: i18n.common.types.GROUP_FAMILY
                  }
                ]}
                placeholder={showRequired(
                  i18n.unitEditor.placeholder.daycareType
                )}
                onChange={(value) =>
                  value && 'value' in value
                    ? updateForm({
                        daycareType:
                          value.value === 'CENTRE'
                            ? 'CENTRE'
                            : value.value === 'FAMILY'
                            ? 'FAMILY'
                            : value.value === 'GROUP_FAMILY'
                            ? 'GROUP_FAMILY'
                            : undefined
                      })
                    : undefined
                }
              />
            )}
          </DaycareTypeSelectContainer>
          <Checkbox
            disabled={!props.editable}
            checked={form.careTypes.PRESCHOOL}
            label={i18n.common.types.PRESCHOOL}
            onChange={(checked) => updateCareTypes({ PRESCHOOL: checked })}
            dataQa="care-type-checkbox-PRESCHOOL"
          />
          <Checkbox
            disabled={!props.editable}
            checked={form.careTypes.PREPARATORY_EDUCATION}
            label={i18n.common.types.PREPARATORY_EDUCATION}
            onChange={(checked) =>
              updateCareTypes({ PREPARATORY_EDUCATION: checked })
            }
            dataQa="care-type-checkbox-PREPARATORY"
          />
          <Checkbox
            disabled={!props.editable}
            checked={form.careTypes.CLUB}
            label={i18n.common.types.CLUB}
            onChange={(checked) => updateCareTypes({ CLUB: checked })}
            dataQa="care-type-checkbox-CLUB"
          />
        </FixedSpaceColumn>
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.canApply)}</div>
        <FixedSpaceColumn>
          {[
            {
              type: 'daycare',
              checkboxI18n: 'canApplyDaycare',
              field: 'daycareApplyPeriod',
              period: form.daycareApplyPeriod
            },
            {
              type: 'preschool',
              checkboxI18n: 'canApplyPreschool',
              field: 'preschoolApplyPeriod',
              period: form.preschoolApplyPeriod
            },
            {
              type: 'club',
              checkboxI18n: 'canApplyClub',
              field: 'clubApplyPeriod',
              period: form.clubApplyPeriod
            }
          ].map(({ type, checkboxI18n, field, period }) => {
            return (
              <div key={type}>
                <Checkbox
                  disabled={!props.editable}
                  label={i18n.unitEditor.field[checkboxI18n] as string}
                  checked={period !== null}
                  onChange={(canApply) => {
                    updateForm({
                      [field]: canApply
                        ? { start: LocalDate.today(), end: null }
                        : null
                    })
                  }}
                  dataQa={`application-type-checkbox-${type.toUpperCase()}`}
                />
                {period != null && (
                  <>
                    <Gap size="xs" />
                    <IndentChckboxLabel>
                      <FixedSpaceRow alignItems="center">
                        <div>{i18n.unitEditor.field.applyPeriod}</div>
                        <div>
                          {props.editable ? (
                            <DatePicker
                              date={period?.start ?? LocalDate.today()}
                              onChange={(startDate) => {
                                if (
                                  !period ||
                                  (period.end !== null &&
                                    period.end.isBefore(startDate))
                                ) {
                                  return
                                }

                                updateForm({
                                  [field]: {
                                    start: startDate,
                                    end: period?.end
                                  }
                                })
                              }}
                            />
                          ) : (
                            period.start.format()
                          )}
                          {' - '}
                          {props.editable ? (
                            <DatePickerClearable
                              date={period?.end}
                              onChange={(endDate) => {
                                if (!period || endDate.isBefore(period.start)) {
                                  return
                                }

                                updateForm({
                                  [field]: {
                                    start: period?.start ?? LocalDate.today(),
                                    end: endDate
                                  }
                                })
                              }}
                              onCleared={() => {
                                updateForm({
                                  [field]: {
                                    start: period?.start ?? LocalDate.today(),
                                    end: null
                                  }
                                })
                              }}
                            />
                          ) : (
                            period.end?.format()
                          )}
                        </div>
                      </FixedSpaceRow>
                    </IndentChckboxLabel>
                  </>
                )}
              </div>
            )
          })}
        </FixedSpaceColumn>
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.providerType)}</div>
        {props.editable ? (
          <FixedSpaceColumn>
            {([
              'MUNICIPAL',
              'PURCHASED',
              'MUNICIPAL_SCHOOL',
              'PRIVATE',
              'PRIVATE_SERVICE_VOUCHER'
            ] as const).map((value) => (
              <Radio
                key={value}
                label={i18n.common.providerType[value]}
                checked={form.providerType === value}
                onChange={() => updateForm({ providerType: value })}
              />
            ))}
          </FixedSpaceColumn>
        ) : (
          i18n.common.providerType[form.providerType]
        )}
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.operationDays)}</div>
        <OperationDaysContainer>
          {[1, 2, 3, 4, 5, 6, 7].map((day) => (
            <FixedSpaceColumn
              key={`"weekday-${day}"`}
              spacing={'xs'}
              marginRight={'m'}
              alignItems={'center'}
            >
              <div>{i18n.unitEditor.label.operationDay[day]}</div>
              <Checkbox
                disabled={!props.editable}
                checked={form.operationDays.some(
                  (selectedDay) => selectedDay == day
                )}
                hiddenLabel={true}
                label={''}
                onChange={(checked) => {
                  updateForm({
                    operationDays: checked
                      ? [...form.operationDays, day as DayOfWeek]
                      : form.operationDays.filter((d) => d != day)
                  })
                }}
              />
            </FixedSpaceColumn>
          ))}
        </OperationDaysContainer>
      </FormPart>
      <FormPart>
        <div>{i18n.unitEditor.label.roundTheClock}</div>
        <Checkbox
          disabled={!props.editable}
          label={i18n.unitEditor.field.roundTheClock}
          checked={form.roundTheClock}
          onChange={(roundTheClock) => updateForm({ roundTheClock })}
        />
      </FormPart>
      <FormPart>
        <label htmlFor="unit-capacity">{i18n.unitEditor.label.capacity}</label>
        <CapacityInputContainer>
          {props.editable ? (
            <InputField
              id="unit-capacity"
              value={form.capacity}
              onChange={(value) =>
                updateForm({
                  capacity: value
                })
              }
            />
          ) : (
            form.capacity
          )}
          {` ${i18n.unitEditor.field.capacity}`}
        </CapacityInputContainer>
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.language)}</div>
        {props.editable ? (
          <FixedSpaceColumn>
            {(['fi', 'sv'] as const).map((value) => (
              <Radio
                key={value}
                label={i18n.language[value]}
                checked={form.language === value}
                onChange={() => updateForm({ language: value })}
              />
            ))}
          </FixedSpaceColumn>
        ) : (
          i18n.language[form.language]
        )}
      </FormPart>
      <FormPart>
        <div>{i18n.unitEditor.label.ghostUnit}</div>
        <Checkbox
          disabled={!props.editable}
          label={i18n.unitEditor.field.ghostUnit}
          checked={form.ghostUnit}
          onChange={(ghostUnit) => updateForm({ ghostUnit })}
        />
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.integrations)}</div>
        <FixedSpaceColumn>
          <Checkbox
            disabled={!props.editable}
            label={i18n.unitEditor.field.uploadToVarda}
            checked={form.uploadToVarda}
            onChange={(uploadToVarda) => updateForm({ uploadToVarda })}
          />
          <Checkbox
            disabled={!props.editable}
            label={i18n.unitEditor.field.uploadToKoski}
            checked={form.uploadToKoski}
            onChange={(uploadToKoski) => updateForm({ uploadToKoski })}
          />
          <Checkbox
            disabled={!props.editable}
            label={i18n.unitEditor.field.invoicedByMunicipality}
            checked={form.invoicedByMunicipality}
            onChange={(invoicedByMunicipality) =>
              updateForm({ invoicedByMunicipality })
            }
          />
        </FixedSpaceColumn>
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.ophUnitOid)}</div>
        {props.editable ? (
          <InputField
            id="oph-unit-oid"
            placeholder={showRequired(i18n.unitEditor.label.ophUnitOid)}
            value={form.ophUnitOid}
            onChange={(value) => updateForm({ ophUnitOid: value })}
          />
        ) : (
          form.ophUnitOid
        )}
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.ophOrganizerOid)}</div>
        {props.editable ? (
          <InputField
            id="oph-organizer-oid"
            placeholder={showRequired(i18n.unitEditor.label.ophOrganizerOid)}
            value={form.ophOrganizerOid}
            onChange={(value) => updateForm({ ophOrganizerOid: value })}
          />
        ) : (
          form.ophOrganizerOid
        )}
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.ophOrganizationOid)}</div>
        {props.editable ? (
          <InputField
            id="oph-organization-oid"
            placeholder={showRequired(i18n.unitEditor.label.ophOrganizationOid)}
            value={form.ophOrganizationOid}
            onChange={(value) => updateForm({ ophOrganizationOid: value })}
          />
        ) : (
          form.ophOrganizationOid
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-cost-center">
          {i18n.unitEditor.label.costCenter}
        </label>
        {props.editable ? (
          <InputField
            id="unit-cost-center"
            placeholder={showRequired(i18n.unitEditor.placeholder.costCenter)}
            value={form.costCenter}
            onChange={(value) => updateForm({ costCenter: value })}
          />
        ) : (
          form.costCenter
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-additional-info">
          {i18n.unitEditor.label.additionalInfo}
        </label>
        {props.editable ? (
          <InputField
            id="unit-additional-info"
            placeholder={i18n.unitEditor.placeholder.additionalInfo}
            value={form.additionalInfo}
            onChange={(value) => updateForm({ additionalInfo: value })}
          />
        ) : (
          form.additionalInfo
        )}
      </FormPart>
      <H3>{i18n.unitEditor.title.contact}</H3>
      <FormPart>
        <label htmlFor="unit-phone">{i18n.unitEditor.label.phone}</label>
        {props.editable ? (
          <InputField
            id="unit-phone"
            placeholder={i18n.unitEditor.placeholder.phone}
            value={form.phone}
            onChange={(value) => updateForm({ phone: value })}
          />
        ) : (
          form.phone
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-email">{i18n.unitEditor.label.email}</label>
        {props.editable ? (
          <InputField
            id="unit-email"
            placeholder={i18n.unitEditor.placeholder.email}
            value={form.email}
            onChange={(value) => updateForm({ email: value })}
          />
        ) : (
          form.email
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-url">{i18n.unitEditor.label.url}</label>
        {props.editable ? (
          <InputField
            id="unit-url"
            placeholder={i18n.unitEditor.placeholder.url}
            value={form.url}
            onChange={(value) => updateForm({ url: value })}
          />
        ) : (
          form.url && <Url href={form.url}>{form.url}</Url>
        )}
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.visitingAddress)}</div>
        <AddressEditor
          editable={props.editable}
          address={form.visitingAddress}
          onChange={(visitingAddress) => updateForm({ visitingAddress })}
          dataQaPrefix="visiting-address"
        />
      </FormPart>
      <FormPart>
        <label htmlFor="unit-location">{i18n.unitEditor.label.location}</label>
        {props.editable ? (
          <InputField
            id="unit-location"
            placeholder={i18n.unitEditor.placeholder.location}
            value={form.location}
            onChange={(value) => updateForm({ location: value })}
          />
        ) : (
          form.location && (
            <a href={`https://www.google.com/maps/place/${form.location}`}>
              {form.location}
            </a>
          )
        )}
      </FormPart>
      <FormPart>
        <div>{i18n.unitEditor.label.mailingAddress}</div>
        <AddressEditor
          editable={props.editable}
          address={form.mailingAddress}
          onChange={(mailingAddress) => updateForm({ mailingAddress })}
          dataQaPrefix="mailing-address"
        />
      </FormPart>
      <H3>{i18n.unitEditor.title.unitManager}</H3>
      <FormPart>
        <label htmlFor="unit-manager-name">
          {showRequired(i18n.unitEditor.label.unitManager.name)}
        </label>
        {props.editable ? (
          <InputField
            id="unit-manager-name"
            placeholder={i18n.unitEditor.placeholder.unitManager.name}
            value={unitManager.name}
            onChange={(value) => updateUnitManager({ name: value })}
            dataQa="manager-name-input"
          />
        ) : (
          unitManager.name
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-manager-phone">
          {showRequired(i18n.unitEditor.label.unitManager.phone)}
        </label>
        {props.editable ? (
          <InputField
            id="unit-manager-phone"
            placeholder={i18n.unitEditor.placeholder.phone}
            value={unitManager.phone}
            onChange={(value) => updateUnitManager({ phone: value })}
            dataQa={'qa-unit-manager-phone-input-field'}
          />
        ) : (
          unitManager.phone
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-manager-email">
          {showRequired(i18n.unitEditor.label.unitManager.email)}
        </label>
        {props.editable ? (
          <InputField
            id="unit-manager-email"
            placeholder={i18n.unitEditor.placeholder.email}
            value={unitManager.email}
            onChange={(value) => updateUnitManager({ email: value })}
            dataQa={'qa-unit-manager-email-input-field'}
          />
        ) : (
          unitManager.email
        )}
      </FormPart>
      <H3>{i18n.unitEditor.title.decisionCustomization}</H3>
      <FormPart>
        <label htmlFor="unit-daycare-name">
          {i18n.unitEditor.label.decisionCustomization.daycareName}
        </label>
        {props.editable ? (
          <InputField
            id="unit-daycare-name"
            placeholder={i18n.unitEditor.placeholder.decisionCustomization.name}
            value={decisionCustomization.daycareName}
            onChange={(value) =>
              updateDecisionCustomization({
                daycareName: value
              })
            }
          />
        ) : (
          decisionCustomization.daycareName
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-preschool-name">
          {i18n.unitEditor.label.decisionCustomization.preschoolName}
        </label>
        {props.editable ? (
          <InputField
            id="unit-preschool-name"
            placeholder={i18n.unitEditor.placeholder.decisionCustomization.name}
            value={decisionCustomization.preschoolName}
            onChange={(value) =>
              updateDecisionCustomization({
                preschoolName: value
              })
            }
          />
        ) : (
          decisionCustomization.preschoolName
        )}
      </FormPart>
      <FormPart>
        <div>{i18n.unitEditor.label.decisionCustomization.handler}</div>
        {props.editable ? (
          <FixedSpaceColumn>
            {([0, 1, 2, 3] as const).map((index) => (
              <Radio
                key={index}
                label={
                  i18n.unitEditor.field.decisionCustomization.handler[index]
                }
                checked={
                  form.decisionCustomization.handler ===
                  i18n.unitEditor.field.decisionCustomization.handler[index]
                }
                onChange={() =>
                  updateDecisionCustomization({
                    handler:
                      i18n.unitEditor.field.decisionCustomization.handler[index]
                  })
                }
              />
            ))}
          </FixedSpaceColumn>
        ) : (
          form.decisionCustomization.handler
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-handler-address">
          {i18n.unitEditor.label.decisionCustomization.handlerAddress}
        </label>
        {props.editable ? (
          <InputField
            id="unit-handler-address"
            placeholder={i18n.unitEditor.placeholder.streetAddress}
            value={decisionCustomization.handlerAddress}
            onChange={(value) =>
              updateDecisionCustomization({
                handlerAddress: value
              })
            }
          />
        ) : (
          decisionCustomization.handlerAddress
        )}
      </FormPart>
      {props.editable && (
        <>
          <>
            {formErrors.map((error, key) => (
              <FormError key={key}>{error}</FormError>
            ))}
          </>
          <FixedSpaceRow>
            <Button
              onClick={() =>
                props.onClickCancel ? props.onClickCancel() : undefined
              }
              disabled={props.submit?.isLoading}
              text={i18n.common.cancel}
            />
            <Button
              primary
              type="submit"
              onClick={(e) => onClickSubmit(e)}
              disabled={props.submit?.isLoading}
              text={isNewUnit ? i18n.unitEditor.submitNew : i18n.common.save}
            />
          </FixedSpaceRow>
          {props.submit?.isFailure && <div>{i18n.common.error.unknown}</div>}
        </>
      )}
    </form>
  )
}
