// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { localDateRange } from 'lib-common/form/fields'
import { object, oneOf, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { DaycarePlacementWithDetails } from 'lib-common/generated/api-types/placement'
import {
  ServiceNeed,
  ServiceNeedOption,
  ShiftCareType,
  shiftCareType
} from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { Td, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { featureFlags } from 'lib-customizations/employee'
import { faExclamation } from 'lib-icons'

import {
  postServiceNeed,
  putServiceNeed
} from '../../../../generated/api-clients/serviceneed'
import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../../common/RetroactiveConfirmation'

const postServiceNeedResult = wrapResult(postServiceNeed)
const putServiceNeedResult = wrapResult(putServiceNeed)

const serviceNeedForm = object({
  range: required(localDateRange()),
  option: required(oneOf<UUID>()),
  shiftCare: required(oneOf<ShiftCareType>())
})

interface ServiceNeedCreateRowProps {
  placement: DaycarePlacementWithDetails
  options: ServiceNeedOption[]
  editedServiceNeed?: ServiceNeed
  initialRange?: FiniteDateRange
  onSuccess: () => void
  onCancel: () => void
  editingId?: string
}

function ServiceNeedEditorRow({
  placement,
  options,
  editedServiceNeed,
  initialRange,
  onSuccess,
  onCancel,
  editingId
}: ServiceNeedCreateRowProps) {
  const { i18n, lang } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds

  const bind = useForm(
    serviceNeedForm,
    () => ({
      range: localDateRange.fromDates(
        editedServiceNeed?.startDate ??
          initialRange?.start ??
          placement.startDate,
        editedServiceNeed?.endDate ?? initialRange?.end ?? placement.endDate,
        {
          minDate: placement.startDate,
          maxDate: placement.endDate
        }
      ),
      option: {
        options: options.map((opt) => ({
          label: opt.nameFi,
          value: opt.id,
          domValue: opt.id
        })),
        domValue: editedServiceNeed?.option?.id ?? ''
      },
      shiftCare: {
        options: shiftCareType.map((type) => ({
          label: t.shiftCareTypes[type],
          value: type,
          domValue: type
        })),
        domValue: editedServiceNeed?.shiftCare ?? 'NONE'
      }
    }),
    i18n.validationErrors
  )
  const { range, option, shiftCare } = useFormFields(bind)

  const { setErrorMessage } = useContext(UIContext)
  const [overlapWarning, setOverlapWarning] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const retroactive = useMemo(() => {
    if (!bind.isValid()) return false

    const newRange = range.value()
    const prevRange = editedServiceNeed
      ? new DateRange(
          editedServiceNeed.startDate,
          editedServiceNeed.endDate ?? null
        )
      : null
    const contentChanged = editedServiceNeed
      ? option.value() !== editedServiceNeed.option.id ||
        shiftCare.value() !== editedServiceNeed.shiftCare
      : true
    return isChangeRetroactive(
      newRange,
      prevRange,
      contentChanged,
      LocalDate.todayInHelsinkiTz()
    )
  }, [bind, range, option, shiftCare, editedServiceNeed])
  const [confirmedRetroactive, setConfirmedRetroactive] = useState(false)

  const formIsValid = bind.isValid() && (!retroactive || confirmedRetroactive)

  const onSubmit = () => {
    if (!formIsValid) return

    if (
      placement.serviceNeeds.some(
        (sn) =>
          sn.id !== editingId &&
          new FiniteDateRange(sn.startDate, sn.endDate).overlaps(range.value())
      )
    ) {
      setOverlapWarning(true)
    } else {
      onConfirmSave()
    }
  }

  const onConfirmSave = () => {
    if (!formIsValid) return

    setSubmitting(true)

    const request = editingId
      ? putServiceNeedResult({
          id: editingId,
          body: {
            startDate: range.value().start,
            endDate: range.value().end,
            optionId: option.value(),
            shiftCare: shiftCare.value()
          }
        })
      : postServiceNeedResult({
          body: {
            placementId: placement.id,
            startDate: range.value().start,
            endDate: range.value().end,
            optionId: option.value(),
            shiftCare: shiftCare.value()
          }
        })

    void request
      .then((res) => {
        if (res.isSuccess) {
          onSuccess()
        } else {
          setErrorMessage({
            type: 'error',
            title: i18n.common.error.unknown,
            text: i18n.common.error.saveFailed,
            resolveLabel: i18n.common.ok
          })
        }
      })
      .finally(() => setSubmitting(false))
  }

  return (
    <>
      <StyledTr hideBottomBorder={retroactive}>
        <Td>
          <DateRangePickerF bind={range} locale={lang} />
        </Td>
        <Td>
          <SelectF
            bind={option}
            placeholder={t.optionPlaceholder}
            data-qa="service-need-option-select"
          />
        </Td>
        <Td verticalAlign="top">
          {featureFlags.intermittentShiftCare ? (
            <FixedSpaceColumn spacing="xs">
              {shiftCareType.map((type) => (
                <Radio
                  key={type}
                  data-qa={`shift-care-type-radio-${type}`}
                  label={t.shiftCareTypes[type]}
                  checked={shiftCare.value() === type}
                  onChange={() =>
                    shiftCare.update((s) => ({
                      ...s,
                      domValue: type
                    }))
                  }
                />
              ))}
            </FixedSpaceColumn>
          ) : (
            <Checkbox
              label={t.shiftCare}
              data-qa="shift-care-toggle"
              hiddenLabel
              checked={shiftCare.value() === 'FULL'}
              onChange={(checked) =>
                shiftCare.update((s) => ({
                  ...s,
                  domValue: checked ? 'FULL' : 'NONE'
                }))
              }
            />
          )}
        </Td>
        <Td />
        <Td>
          <FixedSpaceRow justifyContent="flex-end" spacing="m">
            <InlineButton
              onClick={onCancel}
              text={i18n.common.cancel}
              disabled={submitting}
              data-qa="service-need-cancel"
            />
            <InlineButton
              onClick={onSubmit}
              text={i18n.common.save}
              disabled={submitting || !formIsValid}
              data-qa="service-need-save"
            />
          </FixedSpaceRow>
        </Td>
      </StyledTr>

      {retroactive && (
        <StyledTr hideTopBorder>
          <Td colSpan={2}>
            <RetroactiveConfirmation
              confirmed={confirmedRetroactive}
              setConfirmed={setConfirmedRetroactive}
            />
          </Td>
          <Td />
          <Td />
          <Td />
        </StyledTr>
      )}

      {overlapWarning && (
        <InfoModal
          title={t.overlapWarning.title}
          text={t.overlapWarning.message}
          type="warning"
          icon={faExclamation}
          resolve={{
            action: onConfirmSave,
            label: i18n.common.confirm
          }}
          reject={{
            action: () => setOverlapWarning(false),
            label: i18n.common.cancel
          }}
        />
      )}
    </>
  )
}

const StyledTr = styled(Tr)<{
  hideTopBorder?: boolean
  hideBottomBorder?: boolean
}>`
  td {
    ${(p) => (p.hideTopBorder ? 'border-top: none;' : '')}
    ${(p) => (p.hideBottomBorder ? 'border-bottom: none;' : '')}
  }
`

export default ServiceNeedEditorRow
