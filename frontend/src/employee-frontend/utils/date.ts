// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format } from 'date-fns'
import LocalDate from 'lib-common/local-date'
import { DATE_FORMAT_DEFAULT, MAX_DATE } from '../constants'
import { StatusLabelType } from '../components/common/StatusLabel'
import _ from 'lodash'

export function formatDate(
  date: Date | null | undefined,
  dateFormat = DATE_FORMAT_DEFAULT
): string {
  return date ? format(date, dateFormat) : ''
}

export function isActiveDateRange(
  startDate: LocalDate,
  endDate: LocalDate | null
): boolean {
  const today = LocalDate.today()
  return (
    (startDate.isBefore(today) || startDate.isEqual(today)) &&
    !!(endDate?.isAfter(today) || endDate?.isEqual(today))
  )
}

export function isComingDateRange(
  startDate: LocalDate,
  endDate: LocalDate
): boolean {
  const today = LocalDate.today()
  return startDate.isAfter(today) && endDate.isAfter(today)
}

export function isCompletedDateRange(
  startDate: LocalDate,
  endDate: LocalDate
): boolean {
  const today = LocalDate.today()
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

export function getGaps(children: DateRange[], parent: DateRange): DateRange[] {
  const sortedDateMillis = [
    parent.startDate,
    ...children.map((r) => r.startDate.addDays(-1)),
    ...children.map((r) => r.startDate),
    ...children.map((r) => r.endDate),
    ...children.map((r) => r.endDate.addDays(1)),
    parent.endDate
  ]
    .filter((d) => d.isBetween(parent.startDate, parent.endDate))
    .map((d) => d.toSystemTzDate().getTime())
    .sort()

  const sortedUniqueDates = _.sortedUniq(sortedDateMillis).map((millis) =>
    LocalDate.fromSystemTzDate(new Date(millis))
  )

  const ranges = []
  for (let i = 1; i < sortedUniqueDates.length; i++) {
    ranges.push({
      startDate: sortedUniqueDates[i - 1],
      endDate: sortedUniqueDates[i]
    })
  }

  return ranges.filter(
    (gap) => !children.find((child) => rangesOverlap(gap, child))
  )
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
