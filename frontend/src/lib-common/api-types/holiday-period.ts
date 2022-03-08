// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  FixedPeriodQuestionnaire,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'

import FiniteDateRange from '../finite-date-range'
import { JsonOf } from '../json'
import LocalDate from '../local-date'

export const deserializeHolidayPeriod = ({
  period,
  reservationDeadline,
  ...rest
}: JsonOf<HolidayPeriod>): HolidayPeriod => ({
  ...rest,
  reservationDeadline: reservationDeadline
    ? LocalDate.parseIso(reservationDeadline)
    : null,
  period: FiniteDateRange.parseJson(period)
})

export const deserializeFixedPeriodQuestionnaire = ({
  conditions: { continuousPlacement },
  active,
  period,
  periodOptions,
  ...rest
}: JsonOf<FixedPeriodQuestionnaire>): FixedPeriodQuestionnaire => ({
  ...rest,
  conditions: {
    continuousPlacement: continuousPlacement
      ? FiniteDateRange.parseJson(continuousPlacement)
      : null
  },
  active: FiniteDateRange.parseJson(active),
  period: FiniteDateRange.parseJson(period),
  periodOptions: periodOptions.map((o) => FiniteDateRange.parseJson(o))
})
