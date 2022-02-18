// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useCallback } from 'react'
import styled from 'styled-components'

import { renderResult } from 'citizen-frontend/async-rendering'
import { useLang, useTranslation } from 'citizen-frontend/localization'
import { Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { FreeAbsencePeriod } from 'lib-common/generated/api-types/holidayperiod'
import {
  AbsenceRequest,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import Select from 'lib-components/atoms/dropdowns/Select'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { useHolidayPeriods } from '../holiday-periods/state'

import { postAbsences } from './api'

const formatChildName = (child: ReservationChild) =>
  child.preferredName || child.firstName.split(' ')[0]

type HolidaySelectorProps = {
  child: ReservationChild
  freeAbsencePeriod: FreeAbsencePeriod | null
  value: FiniteDateRange | null
  onSelectPeriod: (selection: FiniteDateRange | null) => void
}

const HolidaySelector = React.memo(function HolidaySelector({
  child,
  freeAbsencePeriod,
  value,
  onSelectPeriod
}: HolidaySelectorProps) {
  const i18n = useTranslation()
  const [lang] = useLang()

  type HolidayOption = {
    name: string
    period: FiniteDateRange | null
  }
  const options: HolidayOption[] = [
    {
      name: '',
      period: null
    },
    ...(freeAbsencePeriod
      ? freeAbsencePeriod.periodOptions.map((period) => ({
          name: period.format(),
          period
        }))
      : [])
  ]

  return (
    <div>
      <H2>
        {i18n.calendar.holidayModal.holidayFor} {formatChildName(child)}
      </H2>
      {freeAbsencePeriod && (
        <>
          <Label>{freeAbsencePeriod.periodOptionLabel[lang]}</Label>
          <Select
            items={options}
            selectedItem={
              options.find(({ period }) => period == value) ?? options[0]
            }
            onChange={(item) => onSelectPeriod(item?.period ?? null)}
            getItemValue={({ name }) => name}
            getItemLabel={({ name }) => name}
            data-qa={`holiday-period-select-${child.id}`}
          />
        </>
      )}
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

  const childWithId = useCallback(
    (childId: string) =>
      availableChildren.find((child) => child.id === childId),
    [availableChildren]
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
        return postHolidays(form).then(() => Promise.resolve())
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
            <HolidayContainer>
              <HolidaySection>
                <div>{holidayPeriod.description[lang]}</div>
                <ExternalLink
                  text={i18n.calendar.holidayModal.additionalInformation}
                  href={holidayPeriod.descriptionLink[lang]}
                  newTab
                />
              </HolidaySection>
              {Object.keys(form.selections).map((childId) => {
                const child = childWithId(childId)
                return (
                  child && (
                    <HolidaySection key={childId}>
                      <HolidaySelector
                        key={childId}
                        child={child}
                        freeAbsencePeriod={holidayPeriod?.freePeriod}
                        value={form.selections[childId].selectedFreePeriod}
                        onSelectPeriod={(dateRange) =>
                          selectFreePeriod(childId, dateRange)
                        }
                      />
                    </HolidaySection>
                  )
                )
              })}
            </HolidayContainer>
          )
      )}
    </AsyncFormModal>
  )
})

const HolidayContainer = styled(FixedSpaceColumn)`
  gap: ${defaultMargins.s};
`
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

const postHolidays = (form: HolidayForm): Promise<Array<Result<void>>> => {
  const requests: AbsenceRequest[] = Object.keys(form.selections)
    .filter((childId) => form.selections[childId].selectedFreePeriod !== null)
    .map((childId) => ({
      childIds: [childId],
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      dateRange: form.selections[childId].selectedFreePeriod!,
      absenceType: 'FREE_ABSENCE'
    }))

  return Promise.all(requests.map((request) => postAbsences(request)))
}

export default HolidayModal
