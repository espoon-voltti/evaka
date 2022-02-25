// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useCallback } from 'react'
import styled from 'styled-components'

import { renderResult } from 'citizen-frontend/async-rendering'
import { postHolidayAbsences } from 'citizen-frontend/holiday-periods/api'
import { useLang, useTranslation } from 'citizen-frontend/localization'
import { Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  HolidayAbsenceRequest,
  FreeAbsencePeriod
} from 'lib-common/generated/api-types/holidayperiod'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { formatPreferredName } from 'lib-common/names'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import Select from 'lib-components/atoms/dropdowns/Select'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label } from 'lib-components/typography'

import { useHolidayPeriods } from '../holiday-periods/state'

type FreeHolidaySelectorProps = {
  child: ReservationChild
  freeAbsencePeriod: FreeAbsencePeriod
  value: FiniteDateRange | null
  onSelectPeriod: (selection: FiniteDateRange | null) => void
}

const FreeHolidaySelector = React.memo(function FreeHolidaySelector({
  child,
  freeAbsencePeriod,
  value,
  onSelectPeriod
}: FreeHolidaySelectorProps) {
  const [lang] = useLang()

  type HolidayOption = {
    name: string
    period: FiniteDateRange | null
  }
  const emptySelection = {
    name: '',
    period: null
  }
  const options: HolidayOption[] = [
    emptySelection,
    ...freeAbsencePeriod.periodOptions.map((period) => ({
      name: period.format(),
      period
    }))
  ]

  return (
    <div>
      <Label>{freeAbsencePeriod.periodOptionLabel[lang]}</Label>
      <Select
        items={options}
        selectedItem={
          options.find(({ period }) => period == value) ?? emptySelection
        }
        onChange={(item) => onSelectPeriod(item?.period ?? null)}
        getItemValue={({ name }) => name}
        getItemLabel={({ name }) => name}
        data-qa={`holiday-period-select-${child.id}`}
      />
    </div>
  )
})

type HolidayModalProps = {
  close: () => void
  reload: () => void
  availableChildren: ReservationChild[]
}

export const HolidayModal = React.memo(function HolidayModal({
  close,
  reload,
  availableChildren
}: HolidayModalProps) {
  const i18n = useTranslation()
  const [lang] = useLang()
  const { holidayPeriods } = useHolidayPeriods()

  const [form, setForm] = useState<HolidayForm>(() =>
    initializeForm(availableChildren)
  )

  const selectFreePeriod = useCallback(
    (childId: string, dateRange: FiniteDateRange | null) => {
      setForm((previous) => ({
        selections: {
          ...previous.selections,
          [childId]: { selectedFreePeriod: dateRange }
        }
      }))
    },
    [setForm]
  )

  return (
    <AsyncFormModal
      mobileFullScreen
      title={i18n.calendar.holidayModal.title}
      resolveAction={(_cancel) => {
        return postHolidays(form)
      }}
      onSuccess={() => {
        close()
        reload()
      }}
      resolveLabel={i18n.common.confirm}
      rejectAction={close}
      rejectLabel={i18n.common.cancel}
    >
      {renderResult(
        holidayPeriods,
        ([holidayPeriod]) =>
          holidayPeriod && (
            <FixedSpaceColumn>
              <HolidaySection>
                <div>{holidayPeriod.description[lang]}</div>
                <ExternalLink
                  text={i18n.calendar.holidayModal.additionalInformation}
                  href={holidayPeriod.descriptionLink[lang]}
                  newTab
                />
              </HolidaySection>
              {availableChildren.map((child) => {
                return (
                  <HolidaySection key={child.id}>
                    <H2>
                      {i18n.calendar.holidayModal.holidayFor}{' '}
                      {formatPreferredName(child)}
                    </H2>
                    {holidayPeriod.freePeriod && (
                      <FreeHolidaySelector
                        key={child.id}
                        child={child}
                        freeAbsencePeriod={holidayPeriod.freePeriod}
                        value={form.selections[child.id].selectedFreePeriod}
                        onSelectPeriod={(dateRange) =>
                          selectFreePeriod(child.id, dateRange)
                        }
                      />
                    )}
                  </HolidaySection>
                )
              })}
            </FixedSpaceColumn>
          )
      )}
    </AsyncFormModal>
  )
})

const HolidaySection = styled.div`
  background: white;
`

type SelectedHolidays = {
  selectedFreePeriod: FiniteDateRange | null
}
type HolidayForm = {
  selections: Record<string, SelectedHolidays>
}

const initializeForm = (children: ReservationChild[]): HolidayForm => ({
  selections: children.reduce(
    (map, child) => ({ ...map, [child.id]: { selectedFreePeriod: null } }),
    {}
  )
})

const postHolidays = (form: HolidayForm): Promise<Result<void>> => {
  const request: HolidayAbsenceRequest = {
    childHolidays: Object.entries(form.selections).reduce(
      (acc, [childId, selectedHolidays]) => ({
        ...acc,
        [childId]: selectedHolidays.selectedFreePeriod
      }),
      {}
    )
  }

  return postHolidayAbsences(request)
}

export default HolidayModal
