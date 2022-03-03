// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { postFreePeriods } from 'citizen-frontend/holiday-periods/api'
import { useLang, useTranslation } from 'citizen-frontend/localization'
import FiniteDateRange from 'lib-common/finite-date-range'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { formatPreferredName } from 'lib-common/names'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2 } from 'lib-components/typography'

import { FreeHolidaySelector } from './FreeHolidaySelector'

interface FreePeriodForm {
  freePeriods: Record<string, FiniteDateRange | null>
}

const initializeForm = (children: ReservationChild[]): FreePeriodForm => ({
  freePeriods: children.reduce(
    (acc, child) => ({ ...acc, [child.id]: null }),
    {}
  )
})

interface Props {
  close: () => void
  reload: () => void
  availableChildren: ReservationChild[]
  activePeriod: HolidayPeriod
}

export default React.memo(function FreePeriodSelectionModal({
  close,
  reload,
  availableChildren,
  activePeriod
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [form, setForm] = useState<FreePeriodForm>(() =>
    initializeForm(availableChildren)
  )

  const selectFreePeriod = useCallback(
    (childId: string) => (selectedFreePeriod: FiniteDateRange | null) =>
      setForm((prev) => ({
        ...prev,
        freePeriods: { ...prev.freePeriods, [childId]: selectedFreePeriod }
      })),
    [setForm]
  )

  const onSubmit = useCallback(() => postFreePeriods(form), [form])
  const closeAndReload = useCallback(() => {
    close()
    reload()
  }, [close, reload])

  if (!activePeriod.freePeriod) return null

  return (
    <AsyncFormModal
      mobileFullScreen
      title={i18n.calendar.holidayModal.title}
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
            <H2>{formatPreferredName(child)}</H2>
            {activePeriod.freePeriod && (
              <FreeHolidaySelector
                freeAbsencePeriod={activePeriod.freePeriod}
                value={form.freePeriods[child.id]}
                onSelectPeriod={selectFreePeriod(child.id)}
              />
            )}
          </HolidaySection>
        ))}
      </FixedSpaceColumn>
    </AsyncFormModal>
  )
})

const HolidaySection = styled.div`
  background: white;
`
