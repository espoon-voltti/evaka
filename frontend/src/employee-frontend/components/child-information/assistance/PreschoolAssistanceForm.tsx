// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'

import type { Result } from 'lib-common/api'
import type FiniteDateRange from 'lib-common/finite-date-range'
import { localDateRange } from 'lib-common/form/fields'
import type { OneOfOption } from 'lib-common/form/form'
import {
  object,
  oneOf,
  required,
  transformed,
  value
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import type {
  PreschoolAssistance,
  PreschoolAssistanceLevel,
  PreschoolAssistanceResponse,
  PreschoolAssistanceUpdate
} from 'lib-common/generated/api-types/assistance'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { Td, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { preschoolAssistanceLevels } from 'lib-customizations/employee'

import type { Translations } from '../../../state/i18n'
import { useTranslation } from '../../../state/i18n'
import { getStatusLabelByDateRange } from '../../../utils/date'
import StatusLabel from '../../common/StatusLabel'

type LevelConfig = {
  minStartDate?: LocalDate
  maxEndDate?: LocalDate
}
export const levelConfigs: Record<PreschoolAssistanceLevel, LevelConfig> = {
  INTENSIFIED_SUPPORT: { maxEndDate: LocalDate.of(2025, 7, 31) },
  SPECIAL_SUPPORT: { maxEndDate: LocalDate.of(2025, 7, 31) },
  SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1: {
    maxEndDate: LocalDate.of(2026, 7, 31)
  },
  SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2: {
    maxEndDate: LocalDate.of(2026, 7, 31)
  },
  CHILD_SUPPORT: { minStartDate: LocalDate.of(2025, 8, 1) },
  CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION: {
    minStartDate: LocalDate.of(2026, 8, 1)
  },
  GROUP_SUPPORT: { minStartDate: LocalDate.of(2025, 8, 1) }
}

const preschoolAssistanceForm = transformed(
  object({
    level: required(oneOf<PreschoolAssistanceLevel>()),
    validDuring: required(localDateRange()),
    allRows: value<PreschoolAssistanceResponse[]>(),
    ignoredId: value<UUID | undefined>()
  }),
  ({ level, validDuring, allRows, ignoredId }) => {
    if (
      allRows.some(
        ({ data }) =>
          data.id !== ignoredId && data.validDuring.overlaps(validDuring)
      )
    ) {
      return ValidationError.of('overlap')
    }
    const success: PreschoolAssistanceUpdate = { level, validDuring }
    return ValidationSuccess.of(success)
  }
)

interface Props {
  preschoolAssistance?: PreschoolAssistance
  allRows: PreschoolAssistanceResponse[]
  onClose: () => void
  onSubmit: (factor: PreschoolAssistanceUpdate) => Promise<Result<void>>
}

const levelOptions = (
  validity: FiniteDateRange | undefined,
  i18n: Translations
): OneOfOption<PreschoolAssistanceLevel>[] =>
  preschoolAssistanceLevels
    .filter((level) => {
      // hide options that are no longer valid, but keep future options visible
      const date = validity?.start ?? LocalDate.todayInSystemTz()
      const maxEndDate = levelConfigs[level]?.maxEndDate
      return !maxEndDate || date.isEqualOrBefore(maxEndDate)
    })
    .map((level) => ({
      value: level,
      domValue: level,
      label:
        i18n.childInformation.assistance.types.preschoolAssistanceLevel[level]
    }))

export const PreschoolAssistanceForm = React.memo(
  function PreschoolAssistanceForm(props: Props) {
    const initialData = props.preschoolAssistance
    const { i18n, lang } = useTranslation()

    const form = useForm(
      preschoolAssistanceForm,
      () => ({
        level: {
          domValue: initialData?.level ?? '',
          options: levelOptions(initialData?.validDuring, i18n)
        },
        validDuring: localDateRange.fromRange(initialData?.validDuring),
        allRows: props.allRows,
        ignoredId: initialData?.id
      }),
      {
        ...i18n.validationErrors,
        ...i18n.childInformation.assistance.validationErrors
      },
      {
        onUpdate: (_, next, form) => {
          const shape = form.shape()
          const validDuring = shape.validDuring.validate(next.validDuring)
          if (validDuring.isValid) {
            const options = levelOptions(validDuring.value, i18n)
            return {
              ...next,
              level: {
                options,
                domValue: options.some(
                  (o) => o.domValue === next.level.domValue
                )
                  ? next.level.domValue
                  : ''
              }
            }
          } else {
            return next
          }
        }
      }
    )

    const { level, validDuring } = useFormFields(form)

    const validityError = useMemo(() => {
      if (!level.isValid() || !validDuring.isValid()) return undefined

      const { minStartDate, maxEndDate } = levelConfigs[level.value()]
      const start = validDuring.value().start
      const end = validDuring.value().end
      if (minStartDate && start.isBefore(minStartDate)) {
        return i18n.childInformation.assistance.validationErrors.startBeforeMinDate(
          minStartDate
        )
      }
      if (maxEndDate && end.isAfter(maxEndDate)) {
        return i18n.childInformation.assistance.validationErrors.endAfterMaxDate(
          maxEndDate
        )
      }
      return undefined
    }, [level, validDuring, i18n.childInformation.assistance.validationErrors])

    const onSubmit = useCallback(
      () =>
        props.onSubmit(form.value()).then((result) => {
          if (result.isSuccess) props.onClose()
        }),
      [props, form]
    )

    return (
      <Tr data-qa="preschool-assistance-form">
        <Td>
          <DateRangePickerF
            bind={validDuring}
            locale={lang}
            data-qa="valid-during"
            info={form.inputInfo()}
          />
        </Td>
        <Td>
          <FixedSpaceColumn spacing="s">
            <SelectF
              data-qa="level"
              bind={level}
              placeholder={i18n.common.select}
            />
            {!!validityError && (
              <AlertBox thin noMargin message={validityError} />
            )}
          </FixedSpaceColumn>
        </Td>
        <Td />
        <Td>
          {validDuring.isValid() ? (
            <StatusLabel
              status={getStatusLabelByDateRange({
                startDate: validDuring.value().start,
                endDate: validDuring.value().end
              })}
            />
          ) : undefined}
        </Td>
        <Td>
          <FixedSpaceRow justifyContent="flex-end" spacing="m">
            <Button
              appearance="inline"
              onClick={props.onClose}
              text={i18n.common.cancel}
              data-qa="cancel"
            />
            <Button
              appearance="inline"
              onClick={onSubmit}
              text={i18n.common.save}
              disabled={!form.isValid() || validityError !== undefined}
              data-qa="save"
            />
          </FixedSpaceRow>
        </Td>
      </Tr>
    )
  }
)
