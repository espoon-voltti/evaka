// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DATE_FORMAT_DEFAULT, DATE_FORMAT_HOURS } from '@/constants'
import { addDays, addMonths, format, isBefore } from 'date-fns'

export const today = (date: any, dateFormat = DATE_FORMAT_DEFAULT) =>
  date ? format(new Date(), dateFormat) : null

export const formatDate = (date: any, dateFormat = DATE_FORMAT_DEFAULT) =>
  date ? format(new Date(date), dateFormat) : null

export const formatTime = (time: any) =>
  time ? format(new Date(time), DATE_FORMAT_HOURS) : null

export const minimumDaycareStartdate = (): string =>
  format(addDays(new Date(), 14), 'yyyy-MM-dd')

export const isUnderFourMonths = (date: string): boolean =>
  isBefore(new Date(date), addMonths(new Date(), 4))

export const datepickerTodayFormat = (): string =>
  format(new Date(), 'yyyy-MM-dd')
