// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import styled from 'styled-components'
import { v4 as uuid } from 'uuid'

import { renderResult } from 'citizen-frontend/async-rendering'
import { postHolidayAbsences } from 'citizen-frontend/holiday-periods/api'
import { useLang, useTranslation } from 'citizen-frontend/localization'
import { Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { HolidayAbsenceRequest } from 'lib-common/generated/api-types/holidayperiod'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { formatPreferredName } from 'lib-common/names'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2 } from 'lib-components/typography'

import { useHolidayPeriods } from '../../holiday-periods/state'

import { FreeHolidaySelector } from './FreeHolidaySelector'
import { HolidayRowState, HolidaySelector } from './HolidaySelector'

const emptyHolidayRow = (): HolidayRowState => ({
  end: '',
  errorKey: undefined,
  key: uuid(),
  parsedEnd: null,
  parsedStart: null,
  start: ''
})

type SelectedHolidays = {
  selectedFreePeriod: FiniteDateRange | null
  otherHolidays: HolidayRowState[]
}
type HolidayForm = {
  childHolidays: Record<string, SelectedHolidays>
}

const initializeForm = (children: ReservationChild[]): HolidayForm => ({
  childHolidays: children.reduce(
    (map, child) => ({
      ...map,
      [child.id]: {
        selectedFreePeriod: null,
        otherHolidays: [emptyHolidayRow()]
      }
    }),
    {}
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

  const selectFreePeriod = useMemo(
    () => (childId: string) => (selectedFreePeriod: FiniteDateRange | null) =>
      setForm((prev) => ({
        childHolidays: {
          ...prev.childHolidays,
          [childId]: { ...prev.childHolidays[childId], selectedFreePeriod }
        }
      })),
    [setForm]
  )

  const updateHolidayRow = useMemo(
    () => (childId: string) => (holiday: HolidayRowState) =>
      setForm((prev) => ({
        childHolidays: {
          ...prev.childHolidays,
          [childId]: {
            ...prev.childHolidays[childId],
            otherHolidays: prev.childHolidays[childId].otherHolidays.map((h) =>
              h.key === holiday.key ? holiday : h
            )
          }
        }
      })),
    [setForm]
  )

  const addHolidayRow = useMemo(
    () => (childId: string) => () =>
      setForm((prev) => ({
        childHolidays: {
          ...prev.childHolidays,
          [childId]: {
            ...prev.childHolidays[childId],
            otherHolidays: [
              ...prev.childHolidays[childId].otherHolidays,
              emptyHolidayRow()
            ]
          }
        }
      })),
    []
  )

  const removeHolidayRow = useMemo(
    () => (childId: string) => (key: string) =>
      setForm((prev) => ({
        childHolidays: {
          ...prev.childHolidays,
          [childId]: {
            ...prev.childHolidays[childId],
            otherHolidays: prev.childHolidays[childId].otherHolidays.filter(
              (h) => h.key !== key
            )
          }
        }
      })),
    []
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
                    freeAbsencePeriod={holidayPeriod.freePeriod}
                    value={form.childHolidays[child.id].selectedFreePeriod}
                    onSelectPeriod={selectFreePeriod(child.id)}
                  />
                )}

                <HolidaySelector
                  period={holidayPeriod.period}
                  rows={form.childHolidays[child.id].otherHolidays}
                  updateHoliday={updateHolidayRow(child.id)}
                  addRow={addHolidayRow(child.id)}
                  removeRow={removeHolidayRow(child.id)}
                />
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

const postHolidays = ({
  childHolidays
}: HolidayForm): Promise<Result<void>> => {
  return postHolidayAbsences({
    childHolidays: Object.entries(childHolidays).reduce<
      HolidayAbsenceRequest['childHolidays']
    >(
      (acc, [childId, { otherHolidays, selectedFreePeriod }]) => ({
        ...acc,
        [childId]: {
          holidays: otherHolidays.flatMap(({ parsedEnd, parsedStart }) =>
            parsedStart && parsedEnd && parsedStart.isEqualOrBefore(parsedEnd)
              ? [new FiniteDateRange(parsedStart, parsedEnd)]
              : []
          ),
          freePeriod: selectedFreePeriod
        }
      }),
      {}
    )
  })
}

export default HolidayModal
