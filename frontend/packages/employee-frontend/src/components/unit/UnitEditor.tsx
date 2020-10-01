// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import styled from 'styled-components'
import SelectWithIcon from '~components/common/Select'
import {
  CareArea,
  Coordinate,
  ProviderType,
  Unit,
  UnitLanguage,
  UnitTypes
} from '~types/unit'
import { DatePicker, DatePickerClearable } from '~components/common/DatePicker'
import {
  Button,
  Buttons,
  Checkbox,
  Input,
  Radio,
  RadioGroup,
  VerticalCheckboxGroup
} from '~components/shared/alpha'
import { DaycareFields } from '~api/unit'
import { UUID } from '~types'
import { isFailure, isLoading, Result } from '~api'
import { Translations, useTranslation } from '~state/i18n'
import { EspooColours } from '~utils/colours'
import InlineButton from 'components/shared/atoms/buttons/InlineButton'
import { faPen } from '@evaka/icons'
import { H1, H3 } from 'components/shared/Typography'
import { DefaultMargins } from 'components/shared/layout/white-space'

type CareType = 'DAYCARE' | 'PRESCHOOL' | 'PREPARATORY_EDUCATION' | 'CLUB'
type DaycareType = 'CENTRE' | 'FAMILY' | 'GROUP_FAMILY'

interface FormData {
  name: string
  openingDate: LocalDate | null
  closingDate: LocalDate | null
  areaId: string
  careTypes: Record<CareType, boolean>
  daycareType: DaycareType | undefined
  canApplyDaycare: boolean
  canApplyPreschool: boolean
  canApplyClub: boolean
  providerType: ProviderType
  roundTheClock: boolean
  capacity: string
  language: UnitLanguage
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
  margin-bottom: ${DefaultMargins.m};
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

const Url = styled.a`
  word-break: break-all;
`

function AddressEditor({
  editable,
  address,
  onChange
}: {
  editable: boolean
  address: Address
  onChange: (address: Address) => void
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
      <Input
        value={address.streetAddress}
        placeholder={i18n.unitEditor.placeholder.streetAddress}
        onChange={(e) => update({ streetAddress: e.currentTarget.value })}
      />
      <AddressSecondRowContainer>
        <Input
          value={address.postalCode}
          placeholder={i18n.unitEditor.placeholder.postalCode}
          onChange={(e) => update({ postalCode: e.currentTarget.value })}
        />
        <Input
          value={address.postOffice}
          placeholder={i18n.unitEditor.placeholder.postOffice}
          onChange={(e) => update({ postOffice: e.currentTarget.value })}
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
    (!form.careTypes.DAYCARE && form.canApplyDaycare) ||
    (!form.careTypes.PRESCHOOL && form.canApplyPreschool) ||
    (!form.careTypes.CLUB && form.canApplyClub)
  ) {
    errors.push(i18n.unitEditor.error.cannotApplyToDifferentType)
  }
  const {
    openingDate,
    closingDate,
    areaId,
    canApplyDaycare,
    canApplyPreschool,
    canApplyClub,
    providerType,
    roundTheClock,
    language,
    uploadToVarda,
    uploadToKoski,
    invoicedByMunicipality,
    ophUnitOid,
    ophOrganizerOid,
    ophOrganizationOid
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
        canApplyDaycare,
        canApplyPreschool,
        canApplyClub,
        providerType,
        roundTheClock,
        capacity,
        language,
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
        ophOrganizationOid
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
    canApplyDaycare: unit?.canApplyDaycare ?? false,
    canApplyPreschool: unit?.canApplyPreschool ?? false,
    canApplyClub: unit?.canApplyClub ?? false,
    providerType: unit?.providerType ?? 'MUNICIPAL',
    roundTheClock: unit?.roundTheClock ?? false,
    capacity: (unit?.capacity ?? 0).toString(),
    language: unit?.language ?? 'fi',
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
    }
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

  const areaOptions = props.areas.map(({ id, name }) => ({ id, label: name }))

  const onClickSubmit = (e: React.MouseEvent) => {
    e.preventDefault()
    const [fields, errors] = validateForm(i18n, form)
    setFormErrors(errors)
    if (fields) {
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
          <Input
            id="unit-name"
            placeholder={i18n.unitEditor.placeholder.name}
            value={form.name}
            onChange={(e) => updateForm({ name: e.currentTarget.value })}
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
          <SelectWithIcon
            value={form.areaId}
            options={areaOptions}
            placeholder={i18n.unitEditor.placeholder.area}
            onChange={(e) => updateForm({ areaId: e.currentTarget.value })}
          />
        ) : (
          props.areas.find((area) => area.id === form.areaId)?.name
        )}
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.careTypes)}</div>
        <VerticalCheckboxGroup label="">
          <DaycareTypeSelectContainer>
            <Checkbox
              name="care-type-daycare"
              disabled={!props.editable}
              checked={careTypes.DAYCARE}
              label={i18n.common.types.DAYCARE}
              onChange={(checked) =>
                updateForm({
                  careTypes: { ...form.careTypes, DAYCARE: checked },
                  daycareType: checked ? form.daycareType : undefined
                })
              }
            />
            {form.careTypes.DAYCARE && (
              <SelectWithIcon
                disabled={!props.editable}
                options={[
                  { id: 'CENTRE', label: i18n.common.types.CENTRE },
                  { id: 'FAMILY', label: i18n.common.types.FAMILY },
                  { id: 'GROUP_FAMILY', label: i18n.common.types.GROUP_FAMILY }
                ]}
                placeholder={showRequired(
                  i18n.unitEditor.placeholder.daycareType
                )}
                value={form.daycareType}
                onChange={(e) =>
                  updateForm({
                    daycareType:
                      e.currentTarget.value === 'CENTRE'
                        ? 'CENTRE'
                        : e.currentTarget.value === 'FAMILY'
                        ? 'FAMILY'
                        : e.currentTarget.value === 'GROUP_FAMILY'
                        ? 'GROUP_FAMILY'
                        : undefined
                  })
                }
              />
            )}
          </DaycareTypeSelectContainer>
          <Checkbox
            name="care-type-preschool"
            disabled={!props.editable}
            checked={form.careTypes.PRESCHOOL}
            label={i18n.common.types.PRESCHOOL}
            onChange={(checked) => updateCareTypes({ PRESCHOOL: checked })}
          />
          <Checkbox
            name="care-type-preparatory"
            disabled={!props.editable}
            checked={form.careTypes.PREPARATORY_EDUCATION}
            label={i18n.common.types.PREPARATORY_EDUCATION}
            onChange={(checked) =>
              updateCareTypes({ PREPARATORY_EDUCATION: checked })
            }
          />
          <Checkbox
            name="care-type-club"
            disabled={!props.editable}
            checked={form.careTypes.CLUB}
            label={i18n.common.types.CLUB}
            onChange={(checked) => updateCareTypes({ CLUB: checked })}
          />
        </VerticalCheckboxGroup>
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.canApply)}</div>
        <VerticalCheckboxGroup label="">
          <Checkbox
            name="can-apply-daycare"
            disabled={!props.editable}
            label={i18n.unitEditor.field.canApplyDaycare}
            checked={form.canApplyDaycare}
            onChange={(canApplyDaycare) => updateForm({ canApplyDaycare })}
          />
          <Checkbox
            name="can-apply-preschool"
            disabled={!props.editable}
            label={i18n.unitEditor.field.canApplyPreschool}
            checked={form.canApplyPreschool}
            onChange={(canApplyPreschool) => updateForm({ canApplyPreschool })}
          />
          <Checkbox
            name="can-apply-club"
            disabled={!props.editable}
            label={i18n.unitEditor.field.canApplyClub}
            checked={form.canApplyClub}
            onChange={(canApplyClub) => updateForm({ canApplyClub })}
          />
        </VerticalCheckboxGroup>
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.providerType)}</div>
        {props.editable ? (
          <RadioGroup label="">
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
                id={`provider-type-${value}`}
                model={form.providerType}
                value={value}
                onChange={(providerType) => updateForm({ providerType })}
              />
            ))}
          </RadioGroup>
        ) : (
          i18n.common.providerType[form.providerType]
        )}
      </FormPart>
      <FormPart>
        <div>{i18n.unitEditor.label.roundTheClock}</div>
        <Checkbox
          name="round-the-clock"
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
            <Input
              id="unit-capacity"
              value={form.capacity}
              onChange={(e) =>
                updateForm({
                  capacity: e.currentTarget.value
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
          <RadioGroup label="">
            {(['fi', 'sv'] as const).map((value) => (
              <Radio
                key={value}
                label={i18n.language[value]}
                id={`unit-language-${value}`}
                model={form.language}
                value={value}
                onChange={(language) => updateForm({ language })}
              />
            ))}
          </RadioGroup>
        ) : (
          i18n.language[form.language]
        )}
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.integrations)}</div>
        <VerticalCheckboxGroup label="">
          <Checkbox
            name="upload-to-varda"
            disabled={!props.editable}
            label={i18n.unitEditor.field.uploadToVarda}
            checked={form.uploadToVarda}
            onChange={(uploadToVarda) => updateForm({ uploadToVarda })}
          />
          <Checkbox
            name="upload-to-koski"
            disabled={!props.editable}
            label={i18n.unitEditor.field.uploadToKoski}
            checked={form.uploadToKoski}
            onChange={(uploadToKoski) => updateForm({ uploadToKoski })}
          />
          <Checkbox
            name="invoiced-by-municipality"
            disabled={!props.editable}
            label={i18n.unitEditor.field.invoicedByMunicipality}
            checked={form.invoicedByMunicipality}
            onChange={(invoicedByMunicipality) =>
              updateForm({ invoicedByMunicipality })
            }
          />
        </VerticalCheckboxGroup>
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.ophUnitOid)}</div>
        {props.editable ? (
          <Input
            id="oph-unit-oid"
            placeholder={showRequired(i18n.unitEditor.label.ophUnitOid)}
            value={form.ophUnitOid}
            onChange={(e) => updateForm({ ophUnitOid: e.currentTarget.value })}
          />
        ) : (
          form.ophUnitOid
        )}
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.ophOrganizerOid)}</div>
        {props.editable ? (
          <Input
            id="oph-organizer-oid"
            placeholder={showRequired(i18n.unitEditor.label.ophOrganizerOid)}
            value={form.ophOrganizerOid}
            onChange={(e) =>
              updateForm({ ophOrganizerOid: e.currentTarget.value })
            }
          />
        ) : (
          form.ophOrganizerOid
        )}
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.ophOrganizationOid)}</div>
        {props.editable ? (
          <Input
            id="oph-organization-oid"
            placeholder={showRequired(i18n.unitEditor.label.ophOrganizationOid)}
            value={form.ophOrganizationOid}
            onChange={(e) =>
              updateForm({ ophOrganizationOid: e.currentTarget.value })
            }
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
          <Input
            id="unit-cost-center"
            placeholder={showRequired(i18n.unitEditor.placeholder.costCenter)}
            value={form.costCenter}
            onChange={(e) => updateForm({ costCenter: e.currentTarget.value })}
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
          <Input
            id="unit-additional-info"
            placeholder={i18n.unitEditor.placeholder.additionalInfo}
            value={form.additionalInfo}
            onChange={(e) =>
              updateForm({ additionalInfo: e.currentTarget.value })
            }
          />
        ) : (
          form.additionalInfo
        )}
      </FormPart>
      <H3>{i18n.unitEditor.title.contact}</H3>
      <FormPart>
        <label htmlFor="unit-phone">{i18n.unitEditor.label.phone}</label>
        {props.editable ? (
          <Input
            id="unit-phone"
            placeholder={i18n.unitEditor.placeholder.phone}
            value={form.phone}
            onChange={(e) => updateForm({ phone: e.currentTarget.value })}
          />
        ) : (
          form.phone
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-email">{i18n.unitEditor.label.email}</label>
        {props.editable ? (
          <Input
            id="unit-email"
            placeholder={i18n.unitEditor.placeholder.email}
            value={form.email}
            onChange={(e) => updateForm({ email: e.currentTarget.value })}
          />
        ) : (
          form.email
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-url">{i18n.unitEditor.label.url}</label>
        {props.editable ? (
          <Input
            id="unit-url"
            placeholder={i18n.unitEditor.placeholder.url}
            value={form.url}
            onChange={(e) => updateForm({ url: e.currentTarget.value })}
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
        />
      </FormPart>
      <FormPart>
        <label htmlFor="unit-location">{i18n.unitEditor.label.location}</label>
        {props.editable ? (
          <Input
            id="unit-location"
            placeholder={i18n.unitEditor.placeholder.location}
            value={form.location}
            onChange={(e) => updateForm({ location: e.currentTarget.value })}
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
        />
      </FormPart>
      <H3>{i18n.unitEditor.title.unitManager}</H3>
      <FormPart>
        <label htmlFor="unit-manager-name">
          {showRequired(i18n.unitEditor.label.unitManager.name)}
        </label>
        {props.editable ? (
          <Input
            id="unit-manager-name"
            placeholder={i18n.unitEditor.placeholder.unitManager.name}
            value={unitManager.name}
            onChange={(e) => updateUnitManager({ name: e.currentTarget.value })}
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
          <Input
            id="unit-manager-phone"
            placeholder={i18n.unitEditor.placeholder.phone}
            value={unitManager.phone}
            onChange={(e) =>
              updateUnitManager({ phone: e.currentTarget.value })
            }
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
          <Input
            id="unit-manager-email"
            placeholder={i18n.unitEditor.placeholder.email}
            value={unitManager.email}
            onChange={(e) =>
              updateUnitManager({ email: e.currentTarget.value })
            }
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
          <Input
            id="unit-daycare-name"
            placeholder={i18n.unitEditor.placeholder.decisionCustomization.name}
            value={decisionCustomization.daycareName}
            onChange={(e) =>
              updateDecisionCustomization({
                daycareName: e.currentTarget.value
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
          <Input
            id="unit-preschool-name"
            placeholder={i18n.unitEditor.placeholder.decisionCustomization.name}
            value={decisionCustomization.preschoolName}
            onChange={(e) =>
              updateDecisionCustomization({
                preschoolName: e.currentTarget.value
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
          <RadioGroup label="">
            {([0, 1, 2, 3] as const).map((index) => (
              <Radio
                key={index}
                label={
                  i18n.unitEditor.field.decisionCustomization.handler[index]
                }
                value={
                  i18n.unitEditor.field.decisionCustomization.handler[index]
                }
                id={`decision-handler-${index}`}
                model={form.decisionCustomization.handler}
                onChange={(handler) => updateDecisionCustomization({ handler })}
              />
            ))}
          </RadioGroup>
        ) : (
          form.decisionCustomization.handler
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-handler-address">
          {i18n.unitEditor.label.decisionCustomization.handlerAddress}
        </label>
        {props.editable ? (
          <Input
            id="unit-handler-address"
            placeholder={i18n.unitEditor.placeholder.streetAddress}
            value={decisionCustomization.handlerAddress}
            onChange={(e) =>
              updateDecisionCustomization({
                handlerAddress: e.currentTarget.value
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
          <Buttons>
            <Button
              onClick={props.onClickCancel}
              disabled={props.submit && isLoading(props.submit)}
            >
              {i18n.common.cancel}
            </Button>
            <Button
              primary
              type="submit"
              onClick={onClickSubmit}
              disabled={props.submit && isLoading(props.submit)}
            >
              {isNewUnit ? i18n.unitEditor.submitNew : i18n.common.save}
            </Button>
          </Buttons>
          {props.submit && isFailure(props.submit) && (
            <div>{i18n.common.error.unknown}</div>
          )}
        </>
      )}
    </form>
  )
}
