// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { DaycarePlacementWithDetails } from 'lib-common/generated/api-types/placement'
import {
  ServiceNeedOption,
  ShiftCareType,
  shiftCareType
} from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { Td, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
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

interface ServiceNeedCreateRowProps {
  placement: DaycarePlacementWithDetails
  options: ServiceNeedOption[]
  initialForm: FormData
  onSuccess: () => void
  onCancel: () => void
  editingId?: string
}

function ServiceNeedEditorRow({
  placement,
  options,
  initialForm,
  onSuccess,
  onCancel,
  editingId
}: ServiceNeedCreateRowProps) {
  const { i18n } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds

  const { setErrorMessage } = useContext(UIContext)
  const [form, setForm] = useState<FormData>(initialForm)
  const [overlapWarning, setOverlapWarning] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const retroactive = useMemo(
    () =>
      isChangeRetroactive(
        form.startDate &&
          (form.endDate === undefined ||
            form.endDate.isEqualOrAfter(form.startDate))
          ? new DateRange(form.startDate, form.endDate ?? null)
          : null,
        initialForm.startDate
          ? new DateRange(initialForm.startDate, initialForm.endDate ?? null)
          : null,
        form.optionId !== initialForm.optionId ||
          form.shiftCare !== initialForm.shiftCare,
        LocalDate.todayInHelsinkiTz()
      ),
    [form, initialForm]
  )
  const [confirmedRetroactive, setConfirmedRetroactive] = useState(false)

  const formIsValid =
    form.startDate &&
    form.endDate &&
    form.optionId &&
    !form.endDate.isBefore(form.startDate) &&
    (!retroactive || confirmedRetroactive)

  const optionIds = useMemo(() => options.map(({ id }) => id), [options])

  const onSubmit = () => {
    const startDate = form.startDate
    const endDate = form.endDate
    if (startDate !== undefined && endDate !== undefined && form.optionId) {
      if (
        placement.serviceNeeds.find(
          (sn) =>
            sn.id !== editingId &&
            sn.startDate.isEqualOrBefore(endDate) &&
            sn.endDate.isEqualOrAfter(startDate)
        ) !== undefined
      ) {
        setOverlapWarning(true)
      } else {
        onConfirmSave()
      }
    }
  }

  const onConfirmSave = () => {
    if (form.startDate && form.endDate && form.optionId) {
      setSubmitting(true)

      const request = editingId
        ? putServiceNeedResult({
            id: editingId,
            body: {
              startDate: form.startDate,
              endDate: form.endDate,
              optionId: form.optionId,
              shiftCare: form.shiftCare
            }
          })
        : postServiceNeedResult({
            body: {
              placementId: placement.id,
              startDate: form.startDate,
              endDate: form.endDate,
              optionId: form.optionId,
              shiftCare: form.shiftCare
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
  }

  return (
    <>
      <StyledTr hideBottomBorder={retroactive}>
        <Td>
          <FixedSpaceRow spacing="xs" alignItems="center">
            <DatePickerDeprecated
              date={form.startDate}
              onChange={(date) => setForm({ ...form, startDate: date })}
              minDate={placement.startDate}
              maxDate={placement.endDate}
              type="short"
            />
            <span>-</span>
            <DatePickerDeprecated
              date={form.endDate}
              onChange={(date) => setForm({ ...form, endDate: date })}
              minDate={placement.startDate}
              maxDate={placement.endDate}
              type="short"
            />
          </FixedSpaceRow>
        </Td>
        <Td>
          <Select
            items={optionIds}
            selectedItem={form.optionId}
            getItemLabel={(optionId) =>
              options.find(({ id }) => id === optionId)?.nameFi ?? ''
            }
            onChange={(optionId) =>
              setForm({ ...form, optionId: optionId ?? undefined })
            }
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
                  checked={form.shiftCare === type}
                  onChange={() => setForm({ ...form, shiftCare: type })}
                />
              ))}
            </FixedSpaceColumn>
          ) : (
            <Checkbox
              label={t.shiftCare}
              data-qa="shift-care-toggle"
              hiddenLabel
              checked={form.shiftCare === 'FULL'}
              onChange={(checked) =>
                setForm({ ...form, shiftCare: checked ? 'FULL' : 'NONE' })
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

interface FormData {
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  optionId: UUID | undefined
  shiftCare: ShiftCareType
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
