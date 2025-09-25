// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import type DateRange from 'lib-common/date-range'
import { useBoolean } from 'lib-common/form/hooks'
import type { UpdateStateFn } from 'lib-common/form-state'
import { time } from 'lib-common/form-validation'
import type {
  CareType,
  Daycare,
  DaycareCareArea,
  DaycareFields,
  Language,
  ProviderType,
  UnitManager
} from 'lib-common/generated/api-types/daycare'
import type {
  AreaId,
  Coordinate,
  EmployeeId
} from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'
import TimeRangeEndpoint from 'lib-common/time-range-endpoint'
import { StaticChip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { DatePickerSpacer } from 'lib-components/molecules/date-picker/DateRangePicker'
import { fontWeights, H1, H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags, unitProviderTypes } from 'lib-customizations/employee'
import { faPen, farXmark } from 'lib-icons'

import type { Translations } from '../../../state/i18n'
import { useTranslation } from '../../../state/i18n'
import type { FinanceDecisionHandlerOption } from '../../../state/invoicing-ui'
import type { DayOfWeek } from '../../../types'
import { TimeRangeInput } from '../../child-information/daily-service-times/DailyServiceTimesForms'
import type { RangeValidationResult } from '../../child-information/daily-service-times/DailyServiceTimesForms'

import { UnitClosingDateModal } from './UnitClosingDateModal'
import { closingDateIsBeforeLastPlacementDate } from './utils'

// CareType is a mix of these two enums
type OnlyCareType = 'DAYCARE' | 'PRESCHOOL' | 'PREPARATORY_EDUCATION' | 'CLUB'
type OnlyDaycareType = 'CENTRE' | 'FAMILY' | 'GROUP_FAMILY'

interface MealtimeData {
  mealtimeBreakfast: EditableTimeRange
  mealtimeEveningSnack: EditableTimeRange
  mealtimeLunch: EditableTimeRange
  mealtimeSnack: EditableTimeRange
  mealtimeSupper: EditableTimeRange
}

type FormData = {
  name: string
  openingDate: LocalDate | null
  closingDate: LocalDate | null
  areaId: AreaId | null
  careTypes: Record<OnlyCareType, boolean>
  dailyPreschoolTime: EditableTimeRange
  dailyPreparatoryTime: EditableTimeRange
  daycareType: OnlyDaycareType | undefined
  daycareApplyPeriod: DateRange | null
  preschoolApplyPeriod: DateRange | null
  clubApplyPeriod: DateRange | null
  providerType: ProviderType
  capacity: string
  language: Language
  withSchool: boolean
  ghostUnit: boolean
  uploadToVarda: boolean
  uploadChildrenToVarda: boolean
  uploadToKoski: boolean
  invoicedByMunicipality: boolean
  costCenter: string
  dwCostCenter: string
  financeDecisionHandlerId: EmployeeId | null
  additionalInfo: string
  phone: string
  email: string
  url: string
  visitingAddress: Address
  location: string
  mailingAddress: Address
  unitManager: UnitManager
  preschoolManager: UnitManager
  decisionCustomization: UnitDecisionCustomization
  ophUnitOid: string
  ophOrganizerOid: string
  operationTimes: (EditableTimeRange | null)[]
  providesShiftCare: boolean
  shiftCareOperationTimes: (EditableTimeRange | null)[]
  shiftCareOpenOnHolidays: boolean
  businessId: string
  iban: string
  providerId: string
  partnerCode: string
  nekkuOrderReductionPercentage: string
} & MealtimeData

interface UnitDecisionCustomization {
  daycareName: string
  preschoolName: string
  handler: string
  handlerAddress: string
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
    font-weight: ${fontWeights.bold};
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
  color: ${colors.status.danger};
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

const FixedDayLabel = styled.div`
  width: 30px;
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
`

const IndentCheckboxLabel = styled.div`
  margin-left: 46px;
`

const IndentedTable = styled.div`
  margin-left: 46px;
  display: inline-grid;
  align-items: center;
  grid-template-columns: min-content min-content;
  column-gap: 80px;
  row-gap: 3px;
`

const Url = styled.a`
  word-break: break-all;
`

const AlertBoxContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
`
type EditableTimeRange = JsonOf<TimeRange>

const emptyTimeRange: EditableTimeRange = {
  start: '',
  end: ''
}

const roundTheClock: EditableTimeRange[] = Array(7)
  .fill(null)
  .map(() => ({
    start: '00:00',
    end: '23:59'
  }))

const emptyOperationWeek = [null, null, null, null, null, null, null]

type FormErrorItem = { key: string; text: string }

type UnitEditorErrors = {
  rangeErrors: {
    dailyPreschoolTime: RangeValidationResult
    dailyPreparatoryTime: RangeValidationResult
    operationTimes: RangeValidationResult[]
    shiftCareOperationTimes: RangeValidationResult[]
  }
  formErrors: FormErrorItem[]
}

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
  const update: UpdateStateFn<Address> = (updates) =>
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
        width="L"
        data-qa={`${dataQaPrefix}-street-input`}
      />
      <AddressSecondRowContainer>
        <InputField
          value={address.postalCode}
          placeholder={i18n.unitEditor.placeholder.postalCode}
          onChange={(value) => update({ postalCode: value })}
          width="s"
          data-qa={`${dataQaPrefix}-postal-code-input`}
        />
        <InputField
          value={address.postOffice}
          placeholder={i18n.unitEditor.placeholder.postOffice}
          onChange={(value) => update({ postOffice: value })}
          data-qa={`${dataQaPrefix}-post-office-input`}
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
  if (parts.length !== 2) return undefined
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
  areas: DaycareCareArea[]
  financeDecisionHandlerOptions: FinanceDecisionHandlerOption[]
  unit: Daycare | undefined
  lastPlacementDate: LocalDate | null
  editable: boolean
  onClickEdit?: () => void
  reservedOphUnitOIDs: string[]
  children: (
    getFormData: () => DaycareFields | undefined,
    isValid: boolean
  ) => React.ReactNode
}

function validateTimeRange({
  start,
  end
}: JsonOf<TimeRange>): RangeValidationResult {
  const errors: RangeValidationResult = {}

  if (start.trim().length === 0) {
    errors.start = 'timeRequired'
  } else {
    errors.start = time(start)
  }

  if (end.trim().length === 0) {
    errors.end = 'timeRequired'
  } else {
    errors.end = time(end)
  }

  if (
    !errors.start &&
    !errors.end &&
    LocalTime.parse(end).isEqualOrBefore(LocalTime.parse(start))
  ) {
    errors.end = 'timeRangeNotLinear'
  }

  return errors
}

function validateForm(
  i18n: Translations,
  lastPlacementDate: LocalDate | null,
  reservedOphUnitOIDs: string[],
  form: FormData
): [DaycareFields | undefined, UnitEditorErrors] {
  const errors: FormErrorItem[] = []
  const typeMap: Record<CareType, boolean> = {
    CLUB: form.careTypes.CLUB,
    CENTRE: form.daycareType === 'CENTRE',
    PRESCHOOL: form.careTypes.PRESCHOOL,
    PREPARATORY_EDUCATION: form.careTypes.PREPARATORY_EDUCATION,
    FAMILY: form.daycareType === 'FAMILY',
    GROUP_FAMILY: form.daycareType === 'GROUP_FAMILY'
  }
  const type = (Object.keys(typeMap) as CareType[]).filter(
    (key) => typeMap[key]
  )
  const name = form.name.trim() || null
  const capacity = parseInt(form.capacity, 10)
  const costCenter = form.costCenter.trim() || null
  const dwCostCenter = form.dwCostCenter.trim() || null
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
    name: form.unitManager.name.trim(),
    email: form.unitManager.email.trim(),
    phone: form.unitManager.phone.trim()
  }
  const preschoolManager = {
    name: form.preschoolManager.name.trim(),
    email: form.preschoolManager.email.trim(),
    phone: form.preschoolManager.phone.trim()
  }
  const decisionCustomization = {
    daycareName: form.decisionCustomization.daycareName.trim(),
    preschoolName: form.decisionCustomization.preschoolName.trim(),
    handler: form.decisionCustomization.handler.trim(),
    handlerAddress: form.decisionCustomization.handlerAddress.trim()
  }

  if (!name) {
    errors.push({ text: i18n.unitEditor.error.name, key: 'unit-name' })
  }
  if (form.areaId == null) {
    errors.push({ text: i18n.unitEditor.error.area, key: 'unit-area' })
  }
  if (Object.values(form.careTypes).every((v) => !v)) {
    errors.push({ text: i18n.unitEditor.error.careType, key: 'unit-caretype' })
  }
  if (form.careTypes.DAYCARE && !form.daycareType) {
    errors.push({
      text: i18n.unitEditor.error.daycareType,
      key: 'unit-daycaretype'
    })
  }
  if (!Number.isSafeInteger(capacity)) {
    errors.push({ text: i18n.unitEditor.error.capacity, key: 'unit-capacity' })
  }
  if (form.invoicedByMunicipality && !costCenter) {
    errors.push({
      text: i18n.unitEditor.error.costCenter,
      key: 'unit-costcenter'
    })
  }
  if (url && !(url.startsWith('https://') || url.startsWith('http://'))) {
    errors.push({ text: i18n.unitEditor.error.url, key: 'unit-url' })
  }
  if (!visitingAddress.streetAddress) {
    errors.push({
      text: i18n.unitEditor.error.visitingAddress.streetAddress,
      key: 'unit-streetaddress'
    })
  }
  if (!visitingAddress.postalCode) {
    errors.push({
      text: i18n.unitEditor.error.visitingAddress.postalCode,
      key: 'unit-postalcode'
    })
  }
  if (!visitingAddress.postOffice) {
    errors.push({
      text: i18n.unitEditor.error.visitingAddress.postOffice,
      key: 'unit-postoffice'
    })
  }
  if (form.location && !location) {
    errors.push({ text: i18n.unitEditor.error.location, key: 'unit-location' })
  }
  if (!unitManager.name) {
    errors.push({
      text: i18n.unitEditor.error.unitManager.name,
      key: 'unit-managername'
    })
  }
  if (!unitManager.phone) {
    errors.push({
      text: i18n.unitEditor.error.unitManager.phone,
      key: 'unit-managerphone'
    })
  }
  if (!unitManager.email) {
    errors.push({
      text: i18n.unitEditor.error.unitManager.email,
      key: 'unit-manageremail'
    })
  }
  if (
    (!form.careTypes.DAYCARE && form.daycareApplyPeriod != null) ||
    (!form.careTypes.PRESCHOOL && form.preschoolApplyPeriod != null) ||
    (!form.careTypes.CLUB && form.clubApplyPeriod != null)
  ) {
    errors.push({
      text: i18n.unitEditor.error.cannotApplyToDifferentType,
      key: 'unit-applicationtypeconflict'
    })
  }
  if (
    form.openingDate != null &&
    form.closingDate != null &&
    form.openingDate.isAfter(form.closingDate)
  ) {
    errors.push({
      text: i18n.unitEditor.error.openingDateIsAfterClosingDate,
      key: 'unit-openingclosingorder'
    })
  }
  if (
    closingDateIsBeforeLastPlacementDate(form.closingDate, lastPlacementDate)
  ) {
    errors.push({
      text: i18n.unitEditor.error.closingDateBeforeLastPlacementDate(
        lastPlacementDate!
      ),
      key: 'unit-closing-placement'
    })
  }
  if (
    featureFlags.voucherUnitPayments &&
    form.providerType === 'PRIVATE_SERVICE_VOUCHER'
  ) {
    if (!form.businessId)
      errors.push({
        text: i18n.unitEditor.error.businessId,
        key: 'unit-businessid'
      })
    if (!form.iban)
      errors.push({ text: i18n.unitEditor.error.iban, key: 'unit-iban' })
    if (!form.providerId)
      errors.push({
        text: i18n.unitEditor.error.providerId,
        key: 'unit-providerid'
      })
  }

  let dailyPreschoolTime: TimeRange | null = null
  let dailyPreschoolTimeRangeErrors: RangeValidationResult = {}
  if (form.careTypes.PRESCHOOL) {
    dailyPreschoolTimeRangeErrors = validateTimeRange(form.dailyPreschoolTime)
    if (
      dailyPreschoolTimeRangeErrors.start ||
      dailyPreschoolTimeRangeErrors.end
    ) {
      errors.push({
        text: i18n.unitEditor.error.dailyPreschoolTime,
        key: 'daily-preschool-time'
      })
    } else {
      dailyPreschoolTime = TimeRange.parse(form.dailyPreschoolTime)
    }
  }

  let dailyPreparatoryTime: TimeRange | null = null
  let dailyPreparatoryTimeRangeErrors: RangeValidationResult = {}
  if (form.careTypes.PREPARATORY_EDUCATION) {
    dailyPreparatoryTimeRangeErrors = validateTimeRange(
      form.dailyPreparatoryTime
    )
    if (
      dailyPreparatoryTimeRangeErrors.start ||
      dailyPreparatoryTimeRangeErrors.end
    ) {
      errors.push({
        text: i18n.unitEditor.error.dailyPreparatoryTime,
        key: 'daily-preparatory-time'
      })
    } else {
      dailyPreparatoryTime = TimeRange.parse(form.dailyPreparatoryTime)
    }
  }

  let operationTimes: (TimeRange | null)[] = []
  const operationTimesRangeErrors = form.operationTimes.map((tr) =>
    tr ? validateTimeRange(tr) : {}
  )
  if (!operationTimesRangeErrors.some((r) => r.start || r.end)) {
    operationTimes = form.operationTimes.map((tr) =>
      tr ? TimeRange.parse(tr) : null
    )
  } else {
    errors.push({
      text: i18n.unitEditor.error.operationTimes,
      key: 'unit-operationtimes'
    })
  }

  let shiftCareOperationTimes: (TimeRange | null)[] | null = null
  const shiftCareOperationTimesRangeErrors: RangeValidationResult[] =
    form.shiftCareOperationTimes.map((tr, i) => {
      if (!form.providesShiftCare) return {}

      if (tr) {
        const rangeErrors = validateTimeRange(tr)
        if (!rangeErrors.start && !rangeErrors.end) {
          if (
            operationTimes[i]?.start?.isBefore(
              new TimeRangeEndpoint.Start(LocalTime.parse(tr.start))
            )
          ) {
            rangeErrors.start = 'generic'
          }
          if (
            operationTimes[i]?.end?.isAfter(
              new TimeRangeEndpoint.End(LocalTime.parse(tr.end))
            )
          ) {
            rangeErrors.end = 'generic'
          }
        }
        return rangeErrors
      } else {
        if (operationTimes[i]) {
          return { start: 'required', end: 'required' }
        } else return {}
      }
    })

  if (form.providesShiftCare) {
    if (shiftCareOperationTimesRangeErrors.some((r) => r.start || r.end)) {
      errors.push({
        text: i18n.unitEditor.error.shiftCareOperationTimes,
        key: 'unit-shift-care-operationtimes'
      })
    } else {
      shiftCareOperationTimes = form.shiftCareOperationTimes.map((tr) =>
        tr ? TimeRange.parse(tr) : null
      )
    }
  }

  function parseMealTimeRange(
    timeRange: EditableTimeRange,
    errorKey: keyof MealtimeData
  ): TimeRange | null {
    const validationErrors = validateTimeRange(timeRange)
    if (
      validationErrors.start === 'timeRequired' &&
      validationErrors.end === 'timeRequired'
    ) {
      return null
    }
    if (validationErrors.start || validationErrors.end) {
      errors.push({
        text: i18n.unitEditor.error.mealTimes,
        key: errorKey
      })
      return null
    }
    return TimeRange.parse(timeRange)
  }

  const mealtimeBreakfast = parseMealTimeRange(
    form.mealtimeBreakfast,
    'mealtimeBreakfast'
  )
  const mealtimeLunch = parseMealTimeRange(form.mealtimeLunch, 'mealtimeLunch')
  const mealtimeSnack = parseMealTimeRange(form.mealtimeSnack, 'mealtimeSnack')
  const mealtimeSupper = parseMealTimeRange(
    form.mealtimeSupper,
    'mealtimeSupper'
  )
  const mealtimeEveningSnack = parseMealTimeRange(
    form.mealtimeEveningSnack,
    'mealtimeEveningSnack'
  )
  const nekkuOrderReductionPercentage = parseInt(
    form.nekkuOrderReductionPercentage,
    10
  )

  if (form.ophUnitOid !== '' && reservedOphUnitOIDs.includes(form.ophUnitOid)) {
    errors.push({
      text: i18n.unitEditor.error.reservedOphUnitOid,
      key: 'reserved-oph-unit-oid'
    })
  }

  const {
    openingDate,
    closingDate,
    areaId,
    daycareApplyPeriod,
    preschoolApplyPeriod,
    clubApplyPeriod,
    providerType,
    language,
    withSchool,
    ghostUnit,
    financeDecisionHandlerId,
    uploadToVarda,
    uploadChildrenToVarda,
    uploadToKoski,
    invoicedByMunicipality,
    ophUnitOid,
    ophOrganizerOid,
    businessId,
    iban,
    providerId,
    partnerCode,
    providesShiftCare,
    shiftCareOpenOnHolidays
  } = form

  if (
    name &&
    areaId &&
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
        dailyPreschoolTime,
        dailyPreparatoryTime,
        daycareApplyPeriod,
        preschoolApplyPeriod,
        clubApplyPeriod,
        providerType,
        capacity,
        language,
        withSchool,
        ghostUnit,
        uploadToVarda,
        uploadChildrenToVarda,
        uploadToKoski,
        invoicedByMunicipality,
        costCenter,
        dwCostCenter,
        financeDecisionHandlerId,
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
        preschoolManager,
        decisionCustomization: {
          daycareName: decisionCustomization.daycareName,
          preschoolName: decisionCustomization.preschoolName,
          handler: decisionCustomization.handler,
          handlerAddress: decisionCustomization.handlerAddress
        },
        ophUnitOid,
        ophOrganizerOid,
        operationTimes,
        shiftCareOperationTimes,
        shiftCareOpenOnHolidays: providesShiftCare && shiftCareOpenOnHolidays,
        businessId,
        iban,
        providerId,
        partnerCode,
        mealtimes: {
          breakfast: mealtimeBreakfast,
          lunch: mealtimeLunch,
          snack: mealtimeSnack,
          supper: mealtimeSupper,
          eveningSnack: mealtimeEveningSnack
        },
        nekkuOrderReductionPercentage
      },
      {
        formErrors: errors,
        rangeErrors: {
          dailyPreschoolTime: dailyPreschoolTimeRangeErrors,
          dailyPreparatoryTime: dailyPreparatoryTimeRangeErrors,
          operationTimes: operationTimesRangeErrors,
          shiftCareOperationTimes: shiftCareOperationTimesRangeErrors
        }
      }
    ]
  } else {
    return [
      undefined,
      {
        formErrors: errors,
        rangeErrors: {
          dailyPreschoolTime: dailyPreschoolTimeRangeErrors,
          dailyPreparatoryTime: dailyPreparatoryTimeRangeErrors,
          operationTimes: operationTimesRangeErrors,
          shiftCareOperationTimes: shiftCareOperationTimesRangeErrors
        }
      }
    ]
  }
}

function formatTimeRange(timeRange: TimeRange | null | undefined) {
  return timeRange
    ? {
        start: timeRange.formatStart(),
        end: timeRange.formatEnd()
      }
    : emptyTimeRange
}

function toFormData(unit: Daycare | undefined): FormData {
  const type = unit?.type
  return {
    name: unit?.name ?? '',
    openingDate: unit?.openingDate ?? null,
    closingDate: unit?.closingDate ?? null,
    areaId: unit?.area?.id ?? null,
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
    dailyPreschoolTime: formatTimeRange(unit?.dailyPreschoolTime),
    dailyPreparatoryTime: formatTimeRange(unit?.dailyPreparatoryTime),
    daycareApplyPeriod: unit?.daycareApplyPeriod ?? null,
    preschoolApplyPeriod: unit?.preschoolApplyPeriod ?? null,
    clubApplyPeriod: unit?.clubApplyPeriod ?? null,
    providerType: unit?.providerType ?? 'MUNICIPAL',
    capacity: (unit?.capacity ?? 0).toString(),
    language: unit?.language ?? 'fi',
    withSchool: unit?.withSchool ?? false,
    ghostUnit: unit?.ghostUnit ?? false,
    uploadToVarda: unit?.uploadToVarda ?? false,
    uploadChildrenToVarda: unit?.uploadChildrenToVarda ?? false,
    uploadToKoski: unit?.uploadToKoski ?? false,
    invoicedByMunicipality: unit?.invoicedByMunicipality ?? false,
    costCenter: unit?.costCenter ?? '',
    dwCostCenter: unit?.dwCostCenter ?? '',
    financeDecisionHandlerId: unit?.financeDecisionHandler?.id ?? null,
    additionalInfo: unit?.additionalInfo ?? '',
    phone: unit?.phone ?? '',
    email: unit?.email ?? '',
    url: unit?.url ?? '',
    ophUnitOid: unit?.ophUnitOid ?? '',
    ophOrganizerOid: unit?.ophOrganizerOid ?? '',
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
    preschoolManager: {
      name: unit?.preschoolManager?.name ?? '',
      phone: unit?.preschoolManager?.phone ?? '',
      email: unit?.preschoolManager?.email ?? ''
    },
    providesShiftCare: !!unit?.shiftCareOperationTimes,
    operationTimes: (unit?.operationTimes ?? emptyOperationWeek).map((range) =>
      range ? { start: range.formatStart(), end: range.formatEnd() } : null
    ),
    shiftCareOperationTimes: unit?.shiftCareOperationTimes
      ? unit.shiftCareOperationTimes.map((range) =>
          range ? { start: range.formatStart(), end: range.formatEnd() } : null
        )
      : roundTheClock,
    shiftCareOpenOnHolidays: unit?.shiftCareOpenOnHolidays ?? false,
    businessId: unit?.businessId ?? '',
    iban: unit?.iban ?? '',
    providerId: unit?.providerId ?? '',
    partnerCode: unit?.partnerCode ?? '',
    mealtimeBreakfast: formatTimeRange(unit?.mealTimes.breakfast),
    mealtimeLunch: formatTimeRange(unit?.mealTimes.lunch),
    mealtimeSnack: formatTimeRange(unit?.mealTimes.snack),
    mealtimeSupper: formatTimeRange(unit?.mealTimes.supper),
    mealtimeEveningSnack: formatTimeRange(unit?.mealTimes.eveningSnack),
    nekkuOrderReductionPercentage: (
      unit?.nekkuOrderReductionPercentage ?? 0
    ).toString()
  }
}

function MealtimeInput({
  mealtimeKey,
  form,
  updateForm,
  editable
}: {
  mealtimeKey: keyof MealtimeData
  form: MealtimeData
  updateForm: (arg0: Partial<FormData>) => void
  editable: boolean
}) {
  return editable ? (
    <TimeRangeInput
      dataQaPrefix={`${mealtimeKey}-input`}
      value={form[mealtimeKey]}
      onChange={(value) => updateForm({ [mealtimeKey]: value })}
      error={undefined}
    />
  ) : (
    <div data-qa={`${mealtimeKey}-value-display`}>
      {form[mealtimeKey].start} - {form[mealtimeKey].end}
    </div>
  )
}

export default function UnitEditor(props: Props) {
  const { i18n } = useTranslation()
  const initialData = useMemo<FormData>(
    () => toFormData(props.unit),
    [props.unit]
  )
  const [form, setForm] = useState<FormData>(initialData)

  // recompute form state after when it has been fetched from API. e.g. on form submit
  useEffect(() => {
    if (!props.editable) setForm(initialData)
  }, [initialData, props.editable])

  const [validationErrors, setValidationErrors] = useState<UnitEditorErrors>({
    rangeErrors: {
      dailyPreparatoryTime: {},
      dailyPreschoolTime: {},
      operationTimes: [],
      shiftCareOperationTimes: []
    },
    formErrors: []
  })
  const { careTypes, decisionCustomization, unitManager, preschoolManager } =
    form

  const canApplyTypes = [
    {
      type: 'daycare',
      checkboxI18n: 'canApplyDaycare' as const,
      field: 'daycareApplyPeriod',
      period: form.daycareApplyPeriod
    },
    ...(featureFlags.preschool
      ? [
          {
            type: 'preschool',
            checkboxI18n: 'canApplyPreschool' as const,
            field: 'preschoolApplyPeriod',
            period: form.preschoolApplyPeriod
          }
        ]
      : []),
    {
      type: 'club',
      checkboxI18n: 'canApplyClub' as const,
      field: 'clubApplyPeriod',
      period: form.clubApplyPeriod
    }
  ]

  const updateForm = (updates: Partial<FormData>) => {
    const newForm = { ...form, ...updates }
    setForm(newForm)
    const [, errors] = validateForm(
      i18n,
      props.lastPlacementDate,
      props.reservedOphUnitOIDs,
      newForm
    )
    setValidationErrors(errors)
  }
  const updateCareTypes = (updates: Partial<Record<CareType, boolean>>) =>
    updateForm({ careTypes: { ...form.careTypes, ...updates } })
  const updateDecisionCustomization: UpdateStateFn<
    UnitDecisionCustomization
  > = (updates) =>
    updateForm({
      decisionCustomization: { ...form.decisionCustomization, ...updates }
    })
  const updateUnitManager: UpdateStateFn<UnitManager> = (updates) =>
    updateForm({ unitManager: { ...form.unitManager, ...updates } })
  const updatePreschoolManager: UpdateStateFn<UnitManager> = (updates) =>
    updateForm({ preschoolManager: { ...form.preschoolManager, ...updates } })

  const selectedFinanceDecisionManager = useMemo(
    () =>
      props.financeDecisionHandlerOptions.find(
        (e) => e.value === form.financeDecisionHandlerId
      ),
    [form.financeDecisionHandlerId] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const [
    showClosingDateModal,
    { on: closingDateModalOn, off: closingDateModalOff }
  ] = useBoolean(false)

  const vardaIcon = useCallback(
    (uploadChildrenToVarda = false) =>
      props.editable && (form.uploadToVarda || uploadChildrenToVarda) ? (
        <>
          {' '}
          <StaticChip
            color={colors.accents.a1greenDark}
            title={i18n.unitEditor.info.varda}
          >
            V
          </StaticChip>
        </>
      ) : null,
    [form.uploadToVarda, i18n.unitEditor.info.varda, props.editable]
  )
  const koskiIcon = useCallback(
    () =>
      props.editable && form.uploadToKoski ? (
        <>
          {' '}
          <StaticChip
            color={colors.accents.a6turquoise}
            title={i18n.unitEditor.info.koski}
          >
            K
          </StaticChip>
        </>
      ) : null,
    [form.uploadToKoski, i18n.unitEditor.info.koski, props.editable]
  )

  const getFormData = () => {
    const [fields, errors] = validateForm(
      i18n,
      props.lastPlacementDate,
      props.reservedOphUnitOIDs,
      form
    )
    setValidationErrors(errors)
    if (fields && checkFormValidation()) {
      return fields
    } else {
      return undefined
    }
  }

  const onClickEditHandler = () => {
    const [, errors] = validateForm(
      i18n,
      props.lastPlacementDate,
      props.reservedOphUnitOIDs,
      form
    )
    setValidationErrors(errors)
    if (props.onClickEdit) props.onClickEdit()
  }

  const showRequiredIf = (condition: boolean, label: string) =>
    condition ? showRequired(label) : label

  const showRequired = props.editable
    ? (label: string) => `${label}*`
    : (label: string) => label

  const isMunicipalOrPurchasedOrServiceVoucherUnit = () =>
    form.providerType === 'MUNICIPAL' ||
    form.providerType === 'PURCHASED' ||
    form.providerType === 'PRIVATE_SERVICE_VOUCHER' ||
    form.providerType === 'MUNICIPAL_SCHOOL'

  const checkFormValidation = (): boolean =>
    validationErrors.formErrors.length === 0

  return (
    <form action="#" data-qa="unit-editor-container">
      {props.unit && (
        <TopBar>
          <H1 fitted>{props.unit.name}</H1>
          {!props.editable && (
            <FixedSpaceRow>
              <Button
                appearance="inline"
                icon={faPen}
                onClick={onClickEditHandler}
                text={i18n.common.edit}
                data-qa="enable-edit-button"
              />
              <Button
                appearance="inline"
                icon={farXmark}
                onClick={closingDateModalOn}
                text={i18n.unitEditor.closingDateModal}
                data-qa="open-closing-date-modal"
              />
            </FixedSpaceRow>
          )}
        </TopBar>
      )}
      {props.unit && showClosingDateModal && (
        <UnitClosingDateModal
          unit={props.unit}
          lastPlacementDate={props.lastPlacementDate}
          onClose={closingDateModalOff}
        />
      )}
      {!props.unit && <H1>{i18n.titles.createUnit}</H1>}
      <H3>{i18n.unit.info.title}</H3>
      <FormPart>
        <label htmlFor="unit-name">
          {showRequired(i18n.unitEditor.label.name)}
          {vardaIcon()}
        </label>
        {props.editable ? (
          <InputField
            id="unit-name"
            placeholder={i18n.unitEditor.placeholder.name}
            value={form.name}
            onChange={(value) => updateForm({ name: value })}
            width="L"
            data-qa="unit-name-input"
          />
        ) : (
          <div>{form.name}</div>
        )}
      </FormPart>
      <FormPart>
        <div>
          {`${i18n.unitEditor.label.openingDate} / ${i18n.unitEditor.label.closingDate}`}
          {vardaIcon()}
        </div>
        <AlertBoxContainer>
          <FixedSpaceRow data-qa="opening-and-closing-dates">
            {props.editable ? (
              <>
                <DatePicker
                  date={form.openingDate}
                  onChange={(openingDate) => updateForm({ openingDate })}
                  maxDate={form.closingDate ?? LocalDate.of(2100, 1, 1)}
                  locale="fi"
                  data-qa="opening-date-input"
                />
                <DatePickerSpacer />
                <DatePicker
                  date={form.closingDate}
                  onChange={(closingDate) => updateForm({ closingDate })}
                  minDate={form.openingDate ?? LocalDate.of(1960, 1, 1)}
                  locale="fi"
                  data-qa="closing-date-input"
                />
              </>
            ) : (
              <>
                {form.openingDate?.format()} - {form.closingDate?.format()}
              </>
            )}
          </FixedSpaceRow>
        </AlertBoxContainer>
      </FormPart>
      <FormPart>
        <div>{showRequired(i18n.unitEditor.label.area)}</div>
        {props.editable ? (
          <Combobox
            items={props.areas}
            selectedItem={
              props.areas.find(({ id }) => id === form.areaId) ?? null
            }
            placeholder={i18n.unitEditor.placeholder.area}
            onChange={(area) =>
              area ? updateForm({ areaId: area.id }) : undefined
            }
            fullWidth
            data-qa="area-select"
            getItemLabel={(area) => area.name}
          />
        ) : (
          props.areas.find((area) => area.id === form.areaId)?.name
        )}
      </FormPart>
      <FormPart>
        <div>
          {showRequired(i18n.unitEditor.label.careTypes)}
          {vardaIcon()}
        </div>
        <FixedSpaceColumn fullWidth={true}>
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
              data-qa="care-type-checkbox-DAYCARE"
            />
            {form.careTypes.DAYCARE && (
              <Combobox<OnlyDaycareType>
                disabled={!props.editable}
                fullWidth
                items={['CENTRE', 'FAMILY', 'GROUP_FAMILY']}
                selectedItem={form.daycareType ?? null}
                placeholder={showRequired(
                  i18n.unitEditor.placeholder.daycareType
                )}
                onChange={(value) =>
                  value
                    ? updateForm({
                        daycareType: value
                      })
                    : undefined
                }
                getItemLabel={(item) => i18n.common.types[item]}
              />
            )}
          </DaycareTypeSelectContainer>
          {featureFlags.preschool && (
            <>
              <Checkbox
                disabled={!props.editable}
                checked={form.careTypes.PRESCHOOL}
                label={i18n.common.types.PRESCHOOL}
                onChange={(checked) => updateCareTypes({ PRESCHOOL: checked })}
                data-qa="care-type-checkbox-PRESCHOOL"
              />
              {form.careTypes.PRESCHOOL && (
                <IndentCheckboxLabel>
                  <FixedSpaceRow alignItems="center">
                    <span>
                      {showRequired(i18n.unitEditor.label.dailyPreschoolTime)}
                    </span>
                    {props.editable ? (
                      <TimeRangeInput
                        value={form.dailyPreschoolTime}
                        onChange={(value) => {
                          updateForm({
                            dailyPreschoolTime: value
                          })
                        }}
                        error={validationErrors.rangeErrors.dailyPreschoolTime}
                        dataQaPrefix="daily-preschool-time"
                        hideErrorsBeforeTouched
                      />
                    ) : (
                      <div data-qa="daily-preschool-time-range">
                        {form.dailyPreschoolTime.start} -{' '}
                        {form.dailyPreschoolTime.end}
                      </div>
                    )}
                  </FixedSpaceRow>
                </IndentCheckboxLabel>
              )}
              <Checkbox
                disabled={!props.editable}
                checked={form.careTypes.PREPARATORY_EDUCATION}
                label={i18n.common.types.PREPARATORY_EDUCATION}
                onChange={(checked) =>
                  updateCareTypes({ PREPARATORY_EDUCATION: checked })
                }
                data-qa="care-type-checkbox-PREPARATORY"
              />
              {form.careTypes.PREPARATORY_EDUCATION && (
                <IndentCheckboxLabel>
                  <FixedSpaceRow alignItems="center">
                    <span>
                      {showRequired(i18n.unitEditor.label.dailyPreparatoryTime)}
                    </span>
                    {props.editable ? (
                      <TimeRangeInput
                        value={form.dailyPreparatoryTime}
                        onChange={(value) => {
                          updateForm({
                            dailyPreparatoryTime: value
                          })
                        }}
                        error={
                          validationErrors.rangeErrors.dailyPreparatoryTime
                        }
                        dataQaPrefix="daily-preparatory-time"
                        hideErrorsBeforeTouched
                      />
                    ) : (
                      <div data-qa="daily-preparatory-time-range">
                        {form.dailyPreparatoryTime.start} -{' '}
                        {form.dailyPreparatoryTime.end}
                      </div>
                    )}
                  </FixedSpaceRow>
                </IndentCheckboxLabel>
              )}
            </>
          )}
          <Checkbox
            disabled={!props.editable}
            checked={form.careTypes.CLUB}
            label={i18n.common.types.CLUB}
            onChange={(checked) => updateCareTypes({ CLUB: checked })}
            data-qa="care-type-checkbox-CLUB"
          />
        </FixedSpaceColumn>
      </FormPart>
      <FormPart>
        <div>{i18n.unitEditor.label.canApply}</div>
        <FixedSpaceColumn>
          {canApplyTypes.map(({ type, checkboxI18n, field, period }) => (
            <div key={type}>
              <Checkbox
                disabled={!props.editable}
                label={i18n.unitEditor.field[checkboxI18n]}
                checked={period !== null}
                onChange={(canApply) => {
                  updateForm({
                    [field]: canApply
                      ? { start: LocalDate.todayInSystemTz(), end: null }
                      : null
                  })
                }}
                data-qa={`application-type-checkbox-${type.toUpperCase()}`}
              />
              {period != null && (
                <>
                  <Gap size="xs" />
                  <IndentCheckboxLabel>
                    <FixedSpaceRow alignItems="center">
                      <div>{i18n.unitEditor.field.applyPeriod}</div>
                      <FixedSpaceRow>
                        {props.editable ? (
                          <>
                            <DatePicker
                              date={
                                period?.start ?? LocalDate.todayInSystemTz()
                              }
                              onChange={(startDate) => {
                                if (
                                  !startDate ||
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
                              locale="fi"
                            />
                            <DatePickerSpacer />
                            <DatePicker
                              date={period?.end}
                              onChange={(endDate) => {
                                updateForm({
                                  [field]: {
                                    start:
                                      period?.start ??
                                      LocalDate.todayInSystemTz(),
                                    end: endDate
                                  }
                                })
                              }}
                              locale="fi"
                            />
                          </>
                        ) : (
                          <>
                            {period.start.format()} - {period.end?.format()}
                          </>
                        )}
                      </FixedSpaceRow>
                    </FixedSpaceRow>
                  </IndentCheckboxLabel>
                </>
              )}
            </div>
          ))}
        </FixedSpaceColumn>
      </FormPart>
      <FormPart>
        <div>
          {showRequired(i18n.unitEditor.label.providerType)}
          {vardaIcon()}
          {koskiIcon()}
        </div>
        {props.editable ? (
          <FixedSpaceColumn>
            {unitProviderTypes.map((value) => (
              <div key={value}>
                <Radio
                  label={i18n.common.providerType[value]}
                  checked={form.providerType === value}
                  onChange={() => updateForm({ providerType: value })}
                  data-qa={`provider-type-${value}`}
                />
                {featureFlags.voucherUnitPayments &&
                  value === 'PRIVATE_SERVICE_VOUCHER' &&
                  form.providerType === value && (
                    <IndentedTable>
                      <label htmlFor="private-service-voucher-business-id">
                        {showRequired(i18n.unitEditor.label.businessId)}
                      </label>
                      <div>
                        <InputField
                          id="private-service-voucher-business-id"
                          value={form.businessId}
                          width="m"
                          onChange={(value) =>
                            updateForm({ businessId: value })
                          }
                        />
                      </div>
                      <label htmlFor="private-service-voucher-iban">
                        {showRequired(i18n.unitEditor.label.iban)}
                      </label>
                      <div>
                        <InputField
                          id="private-service-voucher-iban"
                          value={form.iban}
                          width="m"
                          onChange={(value) => updateForm({ iban: value })}
                        />
                      </div>
                      <label htmlFor="private-service-voucher-provider-id">
                        {showRequired(i18n.unitEditor.label.providerId)}
                      </label>
                      <div>
                        <InputField
                          id="private-service-voucher-provider-id"
                          value={form.providerId}
                          width="m"
                          onChange={(value) =>
                            updateForm({ providerId: value })
                          }
                        />
                      </div>
                      <label htmlFor="private-service-voucher-partner-code">
                        {i18n.unitEditor.label.partnerCode}
                      </label>
                      <div>
                        <InputField
                          id="private-service-voucher-partner-code"
                          value={form.partnerCode}
                          width="m"
                          onChange={(value) =>
                            updateForm({ partnerCode: value })
                          }
                        />
                      </div>
                    </IndentedTable>
                  )}
              </div>
            ))}
          </FixedSpaceColumn>
        ) : (
          <div>{i18n.common.providerType[form.providerType]}</div>
        )}
      </FormPart>
      {featureFlags.voucherUnitPayments &&
        !props.editable &&
        form.providerType === 'PRIVATE_SERVICE_VOUCHER' && (
          <>
            <FormPart>
              <div>{i18n.unitEditor.label.businessId}</div>
              <div>{form.businessId}</div>
            </FormPart>
            <FormPart>
              <div>{i18n.unitEditor.label.iban}</div>
              <div>{form.iban}</div>
            </FormPart>
            <FormPart>
              <div>{i18n.unitEditor.label.providerId}</div>
              <div>{form.providerId}</div>
            </FormPart>
            <FormPart>
              <div>{i18n.unitEditor.label.partnerCode}</div>
              <div>{form.partnerCode}</div>
            </FormPart>
          </>
        )}

      <FormPart>
        <div>{i18n.unitEditor.label.operationDays}</div>
        <FixedSpaceColumn spacing="xs">
          {form.operationTimes.map((timesToday, index) => {
            const dayOfWeek = (index + 1) as DayOfWeek
            return (
              <FixedSpaceRow
                key={`"weekday-${dayOfWeek}"`}
                spacing="s"
                alignItems="center"
              >
                <FixedDayLabel>
                  {i18n.unitEditor.label.operationDay[dayOfWeek]}
                </FixedDayLabel>
                <Checkbox
                  disabled={!props.editable}
                  checked={timesToday != null}
                  hiddenLabel={true}
                  label=""
                  data-qa={`operation-day-${dayOfWeek}`}
                  onChange={(checked) => {
                    const newOpTimes = [...form.operationTimes]
                    newOpTimes[index] = checked ? emptyTimeRange : null
                    updateForm({
                      operationTimes: newOpTimes
                    })
                  }}
                />
                {props.editable ? (
                  <TimeRangeInput
                    value={timesToday ?? emptyTimeRange}
                    onChange={(value) => {
                      const newOpTimes = [...form.operationTimes]
                      newOpTimes[index] = value
                      updateForm({
                        operationTimes: newOpTimes
                      })
                    }}
                    error={validationErrors.rangeErrors.operationTimes[index]}
                    dataQaPrefix={dayOfWeek.toString()}
                    hideErrorsBeforeTouched={false}
                  />
                ) : (
                  <div data-qa={`unit-timerange-detail-${dayOfWeek}`}>
                    {timesToday?.start && timesToday?.end
                      ? `${timesToday.start} - ${timesToday.end}`
                      : ''}
                  </div>
                )}
              </FixedSpaceRow>
            )
          })}
        </FixedSpaceColumn>
      </FormPart>

      <FormPart>
        <div>{i18n.unitEditor.label.shiftCare}</div>
        <Checkbox
          disabled={!props.editable}
          label={i18n.unitEditor.field.providesShiftCare}
          checked={form.providesShiftCare}
          onChange={(providesShiftCare) => updateForm({ providesShiftCare })}
          data-qa="provides-shift-care"
        />
      </FormPart>

      {form.providesShiftCare && (
        <FormPart>
          <div>{i18n.unitEditor.label.shiftCareOperationDays}</div>
          <FixedSpaceColumn spacing="xs">
            {form.shiftCareOperationTimes.map((timesToday, index) => {
              const dayOfWeek = (index + 1) as DayOfWeek
              return (
                <FixedSpaceRow
                  key={`"weekday-${dayOfWeek}"`}
                  spacing="s"
                  alignItems="center"
                >
                  <FixedDayLabel>
                    {i18n.unitEditor.label.operationDay[dayOfWeek]}
                  </FixedDayLabel>
                  <Checkbox
                    disabled={!props.editable}
                    checked={timesToday != null}
                    hiddenLabel={true}
                    label=""
                    data-qa={`shift-care-operation-day-${dayOfWeek}`}
                    onChange={(checked) => {
                      const newOpTimes = [...form.shiftCareOperationTimes]
                      newOpTimes[index] = checked ? emptyTimeRange : null
                      updateForm({
                        shiftCareOperationTimes: newOpTimes
                      })
                    }}
                  />
                  {props.editable ? (
                    <TimeRangeInput
                      value={timesToday ?? emptyTimeRange}
                      onChange={(value) => {
                        const newOpTimes = [...form.shiftCareOperationTimes]
                        newOpTimes[index] = value
                        updateForm({
                          shiftCareOperationTimes: newOpTimes
                        })
                      }}
                      error={
                        validationErrors.rangeErrors.shiftCareOperationTimes[
                          index
                        ]
                      }
                      dataQaPrefix={`shift-care-${dayOfWeek.toString()}`}
                      hideErrorsBeforeTouched={false}
                    />
                  ) : (
                    <div
                      data-qa={`shift-care-unit-timerange-detail-${dayOfWeek}`}
                    >
                      {timesToday?.start && timesToday?.end
                        ? `${timesToday.start} - ${timesToday.end}`
                        : ''}
                    </div>
                  )}
                </FixedSpaceRow>
              )
            })}
            <Checkbox
              disabled={!props.editable}
              label={i18n.unitEditor.field.shiftCareOpenOnHolidays}
              checked={form.shiftCareOpenOnHolidays}
              onChange={(shiftCareOpenOnHolidays) =>
                updateForm({ shiftCareOpenOnHolidays })
              }
              data-qa="shift-care-open-on-holidays"
            />
          </FixedSpaceColumn>
        </FormPart>
      )}

      <FormPart>
        <label htmlFor="unit-capacity">
          {i18n.unitEditor.label.capacity}
          {vardaIcon()}
        </label>
        <CapacityInputContainer>
          {props.editable ? (
            <InputField
              id="unit-capacity"
              value={form.capacity}
              width="xs"
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
        <div>
          {showRequired(i18n.unitEditor.label.language)}
          {vardaIcon()}
          {koskiIcon()}
        </div>
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
        <div>{i18n.unitEditor.label.withSchool}</div>
        <Checkbox
          disabled={!props.editable}
          label={i18n.unitEditor.field.withSchool}
          checked={form.withSchool}
          onChange={(withSchool) => updateForm({ withSchool })}
        />
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
        <div>{i18n.unitEditor.label.integrations}</div>
        <FixedSpaceColumn>
          <AlertBoxContainer>
            <Checkbox
              disabled={!props.editable}
              label={i18n.unitEditor.field.uploadToVarda}
              checked={form.uploadToVarda}
              onChange={(uploadToVarda) => updateForm({ uploadToVarda })}
            />
            {props.editable &&
              form.uploadToVarda &&
              !isMunicipalOrPurchasedOrServiceVoucherUnit() && (
                <AlertBox
                  message={
                    i18n.unitEditor.warning
                      .onlyMunicipalUnitsShouldBeSentToVarda
                  }
                  data-qa="send-to-varda-warning"
                />
              )}
          </AlertBoxContainer>
          <Checkbox
            disabled={!props.editable}
            label={i18n.unitEditor.field.uploadChildrenToVarda}
            checked={form.uploadChildrenToVarda}
            onChange={(uploadChildrenToVarda) =>
              updateForm({ uploadChildrenToVarda })
            }
          />
          {featureFlags.preschool && (
            <Checkbox
              disabled={!props.editable}
              label={i18n.unitEditor.field.uploadToKoski}
              checked={form.uploadToKoski}
              onChange={(uploadToKoski) => updateForm({ uploadToKoski })}
            />
          )}
        </FixedSpaceColumn>
      </FormPart>
      <FormPart>
        <div>{i18n.unitEditor.label.invoicedByMunicipality}</div>
        <Checkbox
          disabled={!props.editable}
          label={i18n.unitEditor.field.invoicingByEvaka}
          checked={form.invoicedByMunicipality}
          onChange={(invoicedByMunicipality) =>
            updateForm({ invoicedByMunicipality })
          }
          data-qa="check-invoice-by-municipality"
        />
      </FormPart>
      <FormPart>
        <label htmlFor="unit-cost-center">
          {showRequiredIf(
            form.invoicedByMunicipality,
            i18n.unitEditor.label.costCenter
          )}
        </label>
        {props.editable ? (
          <InputField
            id="unit-cost-center"
            placeholder={i18n.unitEditor.placeholder.costCenter}
            value={form.costCenter}
            onChange={(value) => updateForm({ costCenter: value })}
          />
        ) : (
          form.costCenter
        )}
      </FormPart>
      <FormPart>
        <div>{i18n.unitEditor.label.financeDecisionHandler}</div>
        {props.editable ? (
          <Combobox
            items={props.financeDecisionHandlerOptions}
            placeholder={i18n.unitEditor.placeholder.financeDecisionHandler}
            selectedItem={selectedFinanceDecisionManager ?? null}
            onChange={(value) =>
              value
                ? updateForm({ financeDecisionHandlerId: value.value })
                : updateForm({ financeDecisionHandlerId: undefined })
            }
            clearable
            fullWidth
            getItemLabel={(item) => item.label}
          />
        ) : (
          selectedFinanceDecisionManager?.label
        )}
      </FormPart>

      <FormPart>
        <div>
          {i18n.unitEditor.label.ophUnitOid}
          {vardaIcon(form.uploadChildrenToVarda)}
          {koskiIcon()}
        </div>
        {props.editable ? (
          <InputField
            id="oph-unit-oid"
            placeholder={i18n.unitEditor.label.ophUnitOid}
            value={form.ophUnitOid}
            onChange={(value) => updateForm({ ophUnitOid: value.trim() })}
            width="L"
            data-qa="oph-unit-oid-input-field"
          />
        ) : (
          form.ophUnitOid
        )}
      </FormPart>
      <FormPart>
        <div>
          {i18n.unitEditor.label.ophOrganizerOid}
          {vardaIcon(form.uploadChildrenToVarda)}
          {koskiIcon()}
        </div>
        {props.editable ? (
          <InputField
            id="oph-organizer-oid"
            placeholder={i18n.unitEditor.label.ophOrganizerOid}
            value={form.ophOrganizerOid}
            onChange={(value) => updateForm({ ophOrganizerOid: value.trim() })}
            width="L"
          />
        ) : (
          form.ophOrganizerOid
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
            width="L"
          />
        ) : (
          form.additionalInfo
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-dw-cost-center">
          {i18n.unitEditor.label.dwCostCenter}
        </label>
        {props.editable ? (
          <InputField
            id="unit-dw-cost-center"
            placeholder={i18n.unitEditor.placeholder.dwCostCenter}
            value={form.dwCostCenter}
            onChange={(value) => updateForm({ dwCostCenter: value })}
          />
        ) : (
          form.dwCostCenter
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
            width="L"
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
            width="L"
            onChange={(value) => updateForm({ url: value })}
          />
        ) : (
          form.url && <Url href={form.url}>{form.url}</Url>
        )}
      </FormPart>
      <FormPart>
        <div>
          {showRequired(i18n.unitEditor.label.visitingAddress)}
          {vardaIcon()}
        </div>
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
        <div>
          {i18n.unitEditor.label.mailingAddress}
          {vardaIcon()}
        </div>
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
          {koskiIcon()}
        </label>
        {props.editable ? (
          <InputField
            id="unit-manager-name"
            placeholder={i18n.unitEditor.placeholder.manager.name}
            value={unitManager.name}
            onChange={(value) => updateUnitManager({ name: value })}
            width="L"
            data-qa="manager-name-input"
          />
        ) : (
          <div data-qa="unit-manager-name">{unitManager.name}</div>
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-manager-phone">
          {showRequired(i18n.unitEditor.label.unitManager.phone)}
          {vardaIcon()}
        </label>
        {props.editable ? (
          <InputField
            id="unit-manager-phone"
            placeholder={i18n.unitEditor.placeholder.phone}
            value={unitManager.phone}
            onChange={(value) => updateUnitManager({ phone: value })}
            data-qa="qa-unit-manager-phone-input-field"
          />
        ) : (
          <div data-qa="unit-manager-phone">{unitManager.phone}</div>
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="unit-manager-email">
          {showRequired(i18n.unitEditor.label.unitManager.email)}
          {vardaIcon()}
        </label>
        {props.editable ? (
          <InputField
            id="unit-manager-email"
            placeholder={i18n.unitEditor.placeholder.email}
            value={unitManager.email}
            onChange={(value) => updateUnitManager({ email: value })}
            width="L"
            data-qa="qa-unit-manager-email-input-field"
          />
        ) : (
          <div data-qa="unit-manager-email">{unitManager.email}</div>
        )}
      </FormPart>
      <H3>{i18n.unitEditor.title.preschoolManager}</H3>
      <FormPart>
        <label htmlFor="preschool-manager-name">
          {i18n.unitEditor.label.preschoolManager.name}
        </label>
        {props.editable ? (
          <InputField
            id="preschool-manager-name"
            placeholder={i18n.unitEditor.placeholder.manager.name}
            value={preschoolManager.name}
            onChange={(value) => updatePreschoolManager({ name: value })}
            width="L"
            data-qa="preschool-manager-name-input"
          />
        ) : (
          <div data-qa="preschool-manager-name">{preschoolManager.name}</div>
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="preschool-manager-phone">
          {i18n.unitEditor.label.preschoolManager.phone}
        </label>
        {props.editable ? (
          <InputField
            id="preschool-manager-phone"
            placeholder={i18n.unitEditor.placeholder.phone}
            value={preschoolManager.phone}
            onChange={(value) => updatePreschoolManager({ phone: value })}
            data-qa="qa-preschool-manager-phone-input-field"
          />
        ) : (
          <div data-qa="preschool-manager-phone">{preschoolManager.phone}</div>
        )}
      </FormPart>
      <FormPart>
        <label htmlFor="preschool-manager-email">
          {i18n.unitEditor.label.preschoolManager.email}
        </label>
        {props.editable ? (
          <InputField
            id="preschool-manager-email"
            placeholder={i18n.unitEditor.placeholder.email}
            value={preschoolManager.email}
            onChange={(value) => updatePreschoolManager({ email: value })}
            width="L"
            data-qa="qa-preschool-manager-email-input-field"
          />
        ) : (
          <div data-qa="preschool-manager-email">{preschoolManager.email}</div>
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
            width="L"
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
      {featureFlags.preschool && (
        <>
          <FormPart>
            <label htmlFor="unit-preschool-name">
              {i18n.unitEditor.label.decisionCustomization.preschoolName}
            </label>
            {props.editable ? (
              <InputField
                id="unit-preschool-name"
                placeholder={
                  i18n.unitEditor.placeholder.decisionCustomization.name
                }
                value={decisionCustomization.preschoolName}
                width="L"
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
        </>
      )}
      <FormPart>
        <div>{i18n.unitEditor.label.decisionCustomization.handler}</div>
        {props.editable ? (
          <FixedSpaceColumn>
            {i18n.unitEditor.field.decisionCustomization.handler.map(
              (handler, index) => (
                <Radio
                  key={index}
                  label={handler}
                  checked={form.decisionCustomization.handler === handler}
                  onChange={() => updateDecisionCustomization({ handler })}
                />
              )
            )}
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
          <AlertBoxContainer>
            <InputField
              id="unit-handler-address"
              placeholder={i18n.unitEditor.placeholder.streetAddress}
              value={decisionCustomization.handlerAddress}
              width="L"
              onChange={(value) =>
                updateDecisionCustomization({
                  handlerAddress: value
                })
              }
            />
            {props.editable &&
              !form.decisionCustomization.handlerAddress &&
              isMunicipalOrPurchasedOrServiceVoucherUnit() && (
                <AlertBox
                  message={i18n.unitEditor.warning.handlerAddressIsMandatory}
                  data-qa="handler-address-mandatory-warning"
                />
              )}
          </AlertBoxContainer>
        ) : (
          decisionCustomization.handlerAddress
        )}
      </FormPart>
      {(featureFlags.jamixIntegration || featureFlags.nekkuIntegration) && (
        <>
          <H3>{i18n.unitEditor.title.mealOrderIntegration}</H3>
          <FormPart>
            <div>{i18n.unitEditor.title.mealtime}</div>
            <FixedSpaceColumn spacing="xs">
              <FixedSpaceRow alignItems="center">
                <label>{i18n.unitEditor.label.mealTime.breakfast}</label>
                <MealtimeInput
                  mealtimeKey="mealtimeBreakfast"
                  form={form}
                  updateForm={updateForm}
                  editable={props.editable}
                />
              </FixedSpaceRow>
              <FixedSpaceRow alignItems="center">
                <label>{i18n.unitEditor.label.mealTime.lunch}</label>
                <MealtimeInput
                  mealtimeKey="mealtimeLunch"
                  form={form}
                  updateForm={updateForm}
                  editable={props.editable}
                />
              </FixedSpaceRow>

              <FixedSpaceRow alignItems="center">
                <label>{i18n.unitEditor.label.mealTime.snack}</label>

                <MealtimeInput
                  mealtimeKey="mealtimeSnack"
                  form={form}
                  updateForm={updateForm}
                  editable={props.editable}
                />
              </FixedSpaceRow>
              <FixedSpaceRow alignItems="center">
                <label>{i18n.unitEditor.label.mealTime.supper}</label>
                <MealtimeInput
                  mealtimeKey="mealtimeSupper"
                  form={form}
                  updateForm={updateForm}
                  editable={props.editable}
                />
              </FixedSpaceRow>
              <FixedSpaceRow alignItems="center">
                <label>{i18n.unitEditor.label.mealTime.eveningSnack}</label>
                <MealtimeInput
                  mealtimeKey="mealtimeEveningSnack"
                  form={form}
                  updateForm={updateForm}
                  editable={props.editable}
                />
              </FixedSpaceRow>
            </FixedSpaceColumn>
          </FormPart>
        </>
      )}
      {featureFlags.nekkuIntegration && (
        <FormPart>
          <label>{i18n.unitEditor.label.nekkuMealReduction}</label>
          <InputField
            id="nekku-reduction"
            value={form.nekkuOrderReductionPercentage}
            width="xs"
            onChange={(value) =>
              updateForm({
                nekkuOrderReductionPercentage: value
              })
            }
          />
        </FormPart>
      )}
      {props.editable && (
        <>
          <>
            {validationErrors.formErrors.map((error, index) => (
              <FormError key={index} data-qa={error.key}>
                {error.text}
              </FormError>
            ))}
          </>
          <FixedSpaceRow>
            {props.children(getFormData, checkFormValidation())}
          </FixedSpaceRow>
        </>
      )}
    </form>
  )
}
