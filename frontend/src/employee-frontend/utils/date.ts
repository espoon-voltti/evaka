// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import type { StatusLabelType } from '../components/common/StatusLabel'
import { MAX_DATE } from '../constants'

export function isActiveDateRange(
  startDate: LocalDate,
  endDate: LocalDate | null
): boolean {
  const today = LocalDate.todayInSystemTz()
  return (
    (startDate.isBefore(today) || startDate.isEqual(today)) &&
    !!(endDate?.isAfter(today) || endDate?.isEqual(today))
  )
}

export function isComingDateRange(
  startDate: LocalDate,
  endDate: LocalDate
): boolean {
  const today = LocalDate.todayInSystemTz()
  return startDate.isAfter(today) && endDate.isAfter(today)
}

export function isCompletedDateRange(
  startDate: LocalDate,
  endDate: LocalDate
): boolean {
  const today = LocalDate.todayInSystemTz()
  return startDate.isBefore(today) && endDate.isBefore(today)
}

export function getStatusLabelByDateRange({
  startDate,
  endDate
}: DateRangeOpen): StatusLabelType {
  if (isActiveDateRange(startDate, endDate || MAX_DATE)) return 'active'
  else if (isComingDateRange(startDate, endDate || MAX_DATE)) return 'coming'
  else if (isCompletedDateRange(startDate, endDate || MAX_DATE))
    return 'completed'
  else throw Error('bug in time ranges')
}

export interface DateRange {
  startDate: LocalDate
  endDate: LocalDate
}

export interface DateRangeOpen {
  startDate: LocalDate
  endDate: LocalDate | null
}

export function rangeContainsDate(
  range: DateRange | DateRangeOpen,
  date: LocalDate
): boolean {
  return range.endDate
    ? !date.isBefore(range.startDate) && !date.isAfter(range.endDate)
    : !date.isBefore(range.startDate)
}

export function rangeCoversAnother(
  range: DateRange,
  another: DateRange
): boolean {
  return (
    !range.startDate.isAfter(another.startDate) &&
    !range.endDate.isBefore(another.endDate)
  )
}

export function rangesOverlap(r1: DateRange, r2: DateRange) {
  return !r1.endDate.isBefore(r2.startDate) && !r1.startDate.isAfter(r2.endDate)
}

function addPeriod(str: string): string {
  return /^\d{2}(\.\d{2})?$/.test(str) ? `${str}.` : str
}

function addMillenium(str: string): string {
  return /^\d{2}\.\d{2}.$/.test(str) ? `${str}2` : str
}

function addCentury(str: string): string {
  return /^\d{2}\.\d{2}.2$/.test(str) ? `${str}0` : str
}

export function autoComplete(str: string): string {
  return addCentury(addMillenium(addPeriod(str)))
}
