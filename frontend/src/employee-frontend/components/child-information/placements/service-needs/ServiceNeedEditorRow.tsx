// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { boolean, localDateRange } from 'lib-common/form/fields'
import { object, oneOf, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type { DaycarePlacementWithDetails } from 'lib-common/generated/api-types/placement'
import type {
  ServiceNeed,
  ServiceNeedOption,
  ShiftCareType
} from 'lib-common/generated/api-types/serviceneed'
import { shiftCareType } from 'lib-common/generated/api-types/serviceneed'
import type {
  ServiceNeedId,
  ServiceNeedOptionId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import Checkbox, { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { Td, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { featureFlags } from 'lib-customizations/employee'
import { faExclamation } from 'lib-icons'

import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../../common/RetroactiveConfirmation'
import {
  createServiceNeedMutation,
  updateServiceNeedMutation
} from '../../queries'

const serviceNeedForm = object({
  range: required(localDateRange()),
  option: required(oneOf<ServiceNeedOptionId>()),
  shiftCare: required(oneOf<ShiftCareType>()),
  partWeek: required(boolean())
})

interface ServiceNeedCreateRowProps {
  placement: DaycarePlacementWithDetails
  options: ServiceNeedOption[]
  editedServiceNeed?: ServiceNeed
  initialRange?: FiniteDateRange
  onSuccess: () => void
  onCancel: () => void
  editingId?: ServiceNeedId
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

  const getOptions = useCallback(
    (range: FiniteDateRange) =>
      options
        .filter((opt) =>
          range.overlaps(new DateRange(opt.validFrom, opt.validTo))
        )
        .map((opt) => ({
          label: opt.nameFi,
          value: opt.id,
          domValue: opt.id,
          partWeek: opt.partWeek
        })),
    [options]
  )

  const bind = useForm(
    serviceNeedForm,
    () => {
      const range = new FiniteDateRange(
        editedServiceNeed?.startDate ??
          initialRange?.start ??
          placement.startDate,
        editedServiceNeed?.endDate ?? initialRange?.end ?? placement.endDate
      )
      const options = getOptions(range)
      const selectedOption = editedServiceNeed
        ? options.find((o) => o.domValue === editedServiceNeed.option.id)
        : undefined
      return {
        range: localDateRange.fromRange(range, {
          minDate: placement.startDate,
          maxDate: placement.endDate
        }),
        option: {
          options,
          domValue: selectedOption?.domValue ?? ''
        },
        shiftCare: {
          options: shiftCareType.map((type) => ({
            label: t.shiftCareTypes[type],
            value: type,
            domValue: type
          })),
          domValue: editedServiceNeed?.shiftCare ?? 'NONE'
        },
        partWeek:
          selectedOption && selectedOption.partWeek !== null
            ? selectedOption.partWeek
            : (editedServiceNeed?.partWeek ?? false)
      }
    },
    i18n.validationErrors,
    {
      onUpdate: (_, nextState, form) => {
        const shape = form.shape()
        const range = shape.range.validate(nextState.range)
        if (range.isValid) {
          const newOptions = getOptions(range.value)
          const selectedOption = nextState.option.domValue
            ? newOptions.find((o) => o.domValue === nextState.option.domValue)
            : undefined
          return {
            ...nextState,
            option: {
              options: newOptions,
              domValue: newOptions.some(
                (o) => o.domValue === nextState.option.domValue
              )
                ? nextState.option.domValue
                : ''
            },
            partWeek:
              selectedOption === undefined
                ? false
                : selectedOption.partWeek === null
                  ? nextState.partWeek
                  : selectedOption.partWeek
          }
        } else {
          return nextState
        }
      }
    }
  )
  const { range, option, shiftCare, partWeek } = useFormFields(bind)

  const partWeekEditable = useMemo(() => {
    if (!range.isValid() || !option.isValid()) return false
    const selectedOption = getOptions(range.value()).find(
      (o) => o.domValue === option.value()
    )
    return selectedOption && selectedOption.partWeek === null
  }, [getOptions, range, option])

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

  const partiallyInvalidOptionError = useMemo(() => {
    if (!bind.isValid()) return null

    const optionDetails = options.find((opt) => opt.id === option.value())
    if (!optionDetails) return null

    const { start, end } = range.value()
    if (optionDetails.validTo && optionDetails.validTo.isBefore(end)) {
      if (optionDetails.validFrom.isAfter(start)) {
        return t.optionStartEndNotValidWarningTitle(
          new FiniteDateRange(optionDetails.validFrom, optionDetails.validTo)
        )
      } else {
        return t.optionEndNotValidWarningTitle(optionDetails.validTo)
      }
    } else {
      if (optionDetails.validFrom.isAfter(start)) {
        return t.optionStartNotValidWarningTitle(optionDetails.validFrom)
      } else {
        return null
      }
    }
  }, [bind, range, option, options, t])

  const formIsValid =
    bind.isValid() &&
    !partiallyInvalidOptionError &&
    (!retroactive || confirmedRetroactive)

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

  const { mutateAsync: createServiceNeed } = useMutationResult(
    createServiceNeedMutation
  )
  const { mutateAsync: updateServiceNeed } = useMutationResult(
    updateServiceNeedMutation
  )

  const onConfirmSave = () => {
    if (!formIsValid) return

    setSubmitting(true)

    const request = editingId
      ? updateServiceNeed({
          id: editingId,
          body: {
            startDate: range.value().start,
            endDate: range.value().end,
            optionId: option.value(),
            shiftCare: shiftCare.value(),
            partWeek: partWeek.value()
          }
        })
      : createServiceNeed({
          body: {
            placementId: placement.id,
            startDate: range.value().start,
            endDate: range.value().end,
            optionId: option.value(),
            shiftCare: shiftCare.value(),
            partWeek: partWeek.value()
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
          <DateRangePickerF
            bind={range}
            locale={lang}
            data-qa="service-need-range"
          />
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
        <Td>
          {option.isValid() && partWeekEditable && (
            <CheckboxF
              label={t.partWeek}
              bind={partWeek}
              data-qa="part-week-checkbox"
            />
          )}
          {option.isValid() && !partWeekEditable && (
            <div>{partWeek.value() ? i18n.common.yes : i18n.common.no}</div>
          )}
        </Td>
        <Td>
          <FixedSpaceRow justifyContent="flex-end" spacing="m">
            <Button
              appearance="inline"
              onClick={onCancel}
              text={i18n.common.cancel}
              disabled={submitting}
              data-qa="service-need-cancel"
            />
            <Button
              appearance="inline"
              onClick={onSubmit}
              text={i18n.common.save}
              disabled={submitting || !formIsValid}
              data-qa="service-need-save"
            />
          </FixedSpaceRow>
        </Td>
      </StyledTr>

      {partiallyInvalidOptionError ? (
        <StyledTr hideTopBorder>
          <Td colSpan={2}>
            <AlertBox
              wide
              title={partiallyInvalidOptionError}
              message={t.notFullyValidOptionWarning}
              noMargin
              data-qa="partially-invalid-warning"
            />
          </Td>
          <Td />
          <Td />
          <Td />
        </StyledTr>
      ) : retroactive ? (
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
      ) : null}

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
