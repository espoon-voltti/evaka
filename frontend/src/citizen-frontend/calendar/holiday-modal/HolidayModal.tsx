// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'
import { v4 as uuid } from 'uuid'

import { useLang, useTranslation } from 'citizen-frontend/localization'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  HolidayAbsenceRequest,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatPreferredName } from 'lib-common/names'
import { UUID } from 'lib-common/types'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2 } from 'lib-components/typography'

import { postHolidayAbsences } from '../../holiday-periods/api'

import { FreeHolidaySelector } from './FreeHolidaySelector'
import {
  HolidayErrorKey,
  HolidayRowState,
  HolidaySelector
} from './HolidaySelector'

const emptyHolidayRow = (): HolidayRowState => ({
  key: uuid(),
  start: '',
  end: ''
})

interface SelectedHolidays {
  selectedFreePeriod: FiniteDateRange | null
  otherHolidays: HolidayRowState[]
}

interface HolidayForm {
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

interface Props {
  close: () => void
  reload: () => void
  availableChildren: ReservationChild[]
  activePeriod: HolidayPeriod
}

export const HolidayModal = React.memo(function HolidayModal({
  close,
  reload,
  availableChildren,
  activePeriod
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [form, setForm] = useState<HolidayForm>(() =>
    initializeForm(availableChildren)
  )

  const selectFreePeriod = useCallback(
    (childId: string) => (selectedFreePeriod: FiniteDateRange | null) =>
      setForm((prev) => ({
        childHolidays: {
          ...prev.childHolidays,
          [childId]: { ...prev.childHolidays[childId], selectedFreePeriod }
        }
      })),
    [setForm]
  )

  const updateHolidayRow = useCallback(
    (childId: string) => (holiday: HolidayRowState) =>
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

  const addHolidayRow = useCallback(
    (childId: string) => () =>
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

  const removeHolidayRow = useCallback(
    (childId: string) => (key: string) =>
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

  type ErrorsByChild = Record<UUID, HolidayErrorKey[]>
  type ValidatedData = {
    holidays: HolidayAbsenceRequest['childHolidays']
    errors: ErrorsByChild
    valid: boolean
  }
  type EmptyRow = HolidayRowState & { state: 'empty' }
  type InvalidRow = HolidayRowState & { state: 'invalid' }
  type ParsedRow = HolidayRowState & { state: 'parsed'; range: FiniteDateRange }
  type Row = EmptyRow | InvalidRow | ParsedRow
  const validatedForm = useMemo(
    () =>
      Object.entries(form.childHolidays).reduce<ValidatedData>(
        (acc, [childId, { otherHolidays, selectedFreePeriod }]) => {
          const parsedRows = otherHolidays.map<Row>((row) => {
            if (!row.end.trim() && !row.start.trim()) {
              return { ...row, state: 'empty' }
            }
            const parsedStart = LocalDate.parseFiOrNull(row.start)
            const parsedEnd = LocalDate.parseFiOrNull(row.end)
            const valid =
              parsedStart && parsedEnd && parsedStart.isEqualOrBefore(parsedEnd)
            return valid
              ? {
                  ...row,
                  state: 'parsed',
                  range: new FiniteDateRange(parsedStart, parsedEnd)
                }
              : { ...row, state: 'invalid' }
          })

          const errors = parsedRows.flatMap((r) => {
            switch (r.state) {
              case 'empty':
                return []
              case 'invalid':
                return [r.key]
              case 'parsed':
                return selectedFreePeriod?.overlaps(r.range) ||
                  parsedRows.find(
                    (r2) =>
                      r2.state === 'parsed' &&
                      r.key !== r2.key &&
                      r.range.overlaps(r2.range)
                  )
                  ? [r.key]
                  : []
            }
          })
          return {
            errors: {
              ...acc.errors,
              [childId]: errors
            },
            holidays: {
              ...acc.holidays,
              [childId]: {
                holidays: parsedRows.flatMap((r) =>
                  r.state === 'parsed' ? [r.range] : []
                ),
                freePeriod: selectedFreePeriod
              }
            },
            valid: acc.valid ? errors.length === 0 : false
          }
        },
        { holidays: {}, errors: {}, valid: true }
      ),
    [form]
  )

  const onSubmit = useCallback(
    () =>
      validatedForm.valid
        ? postHolidayAbsences({ childHolidays: validatedForm.holidays })
        : Promise.reject(),
    [validatedForm]
  )
  const closeAndReload = useCallback(() => {
    close()
    reload()
  }, [close, reload])

  return (
    <AsyncFormModal
      mobileFullScreen
      title={i18n.calendar.holidayModal.title}
      resolveDisabled={!validatedForm.valid}
      resolveAction={onSubmit}
      onSuccess={closeAndReload}
      resolveLabel={i18n.common.confirm}
      rejectAction={close}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn>
        <HolidaySection>
          <div>{activePeriod.description[lang]}</div>
          <ExternalLink
            text={i18n.calendar.holidayModal.additionalInformation}
            href={activePeriod.descriptionLink[lang]}
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
            {activePeriod.freePeriod && (
              <FreeHolidaySelector
                freeAbsencePeriod={activePeriod.freePeriod}
                value={form.childHolidays[child.id].selectedFreePeriod}
                onSelectPeriod={selectFreePeriod(child.id)}
              />
            )}

            <HolidaySelector
              period={activePeriod.period}
              selectedFreePeriod={
                form.childHolidays[child.id].selectedFreePeriod
              }
              rows={form.childHolidays[child.id].otherHolidays}
              errors={validatedForm.errors[child.id]}
              updateHoliday={updateHolidayRow(child.id)}
              addRow={addHolidayRow(child.id)}
              removeRow={removeHolidayRow(child.id)}
            />
          </HolidaySection>
        ))}
      </FixedSpaceColumn>
    </AsyncFormModal>
  )
})

const HolidaySection = styled.div`
  background: white;
`

export default HolidayModal
