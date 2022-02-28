// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { renderResult } from 'citizen-frontend/async-rendering'
import { postHolidayAbsences } from 'citizen-frontend/holiday-periods/api'
import { useLang, useTranslation } from 'citizen-frontend/localization'
import { Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { FreeAbsencePeriod } from 'lib-common/generated/api-types/holidayperiod'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { formatPreferredName } from 'lib-common/names'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import Select from 'lib-components/atoms/dropdowns/Select'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label } from 'lib-components/typography'

import { useHolidayPeriods } from '../holiday-periods/state'

type FreeHolidaySelectorProps = {
  freeAbsencePeriod: FreeAbsencePeriod
  value: FiniteDateRange | null
  onSelectPeriod: (selection: FiniteDateRange | null) => void
}

const FreeHolidaySelector = React.memo(function FreeHolidaySelector({
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
        data-qa="free-period-select"
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
  const { activePeriod } = useHolidayPeriods()

  const [form, setForm] = useState<HolidayForm>(() =>
    initializeForm(availableChildren)
  )

  const selectFreePeriod = useCallback(
    (childId: string, selectedFreePeriod: FiniteDateRange | null) => {
      setForm((previous) => ({
        childHolidays: {
          ...previous.childHolidays,
          [childId]: { ...previous.childHolidays[childId], selectedFreePeriod }
        }
      }))
    },
    [setForm]
  )

  return (
    <AsyncFormModal
      mobileFullScreen
      title={i18n.calendar.holidayModal.title}
      resolveAction={(_cancel) => postHolidays(form)}
      onSuccess={() => {
        close()
        reload()
      }}
      resolveLabel={i18n.common.confirm}
      rejectAction={close}
      rejectLabel={i18n.common.cancel}
    >
      {renderResult(activePeriod, (holidayPeriod) =>
        holidayPeriod ? (
          <FixedSpaceColumn>
            <HolidaySection>
              <div>{holidayPeriod.description[lang]}</div>
              <ExternalLink
                text={i18n.calendar.holidayModal.additionalInformation}
                href={holidayPeriod.descriptionLink[lang]}
                newTab
              />
            </HolidaySection>
            {availableChildren.map((child) => (
              <HolidaySection
                key={child.id}
                data-qa={`holiday-section-${child.id}`}
              >
                <H2>
                  {i18n.calendar.holidayModal.holidayFor}{' '}
                  {formatPreferredName(child)}
                </H2>
                {holidayPeriod.freePeriod && (
                  <FreeHolidaySelector
                    key={child.id}
                    freeAbsencePeriod={holidayPeriod.freePeriod}
                    value={form.childHolidays[child.id].selectedFreePeriod}
                    onSelectPeriod={(dateRange) =>
                      selectFreePeriod(child.id, dateRange)
                    }
                  />
                )}
              </HolidaySection>
            ))}
          </FixedSpaceColumn>
        ) : null
      )}
    </AsyncFormModal>
  )
})

const HolidaySection = styled.div`
  background: white;
`

type SelectedHolidays = {
  selectedFreePeriod: FiniteDateRange | null
  otherHolidays: FiniteDateRange[]
}
type HolidayForm = {
  childHolidays: Record<string, SelectedHolidays>
}

const initializeForm = (children: ReservationChild[]): HolidayForm => ({
  childHolidays: children.reduce(
    (map, child) => ({
      ...map,
      [child.id]: { selectedFreePeriod: null, otherHolidays: [] }
    }),
    {}
  )
})

const postHolidays = ({
  childHolidays
}: HolidayForm): Promise<Result<void>> => {
  return postHolidayAbsences({
    childHolidays: Object.entries(childHolidays).reduce(
      (acc, [childId, { otherHolidays, selectedFreePeriod }]) => ({
        ...acc,
        [childId]: {
          holidays: otherHolidays,
          freePeriod: selectedFreePeriod
        }
      }),
      {}
    )
  })
}

export default HolidayModal
