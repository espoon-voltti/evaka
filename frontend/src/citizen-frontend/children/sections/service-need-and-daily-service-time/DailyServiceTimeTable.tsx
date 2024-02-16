// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { getTimesOnWeekday } from 'lib-common/api-types/daily-service-times'
import { Tense } from 'lib-common/date-range'
import {
  DailyServiceTimes,
  DailyServiceTimesValue
} from 'lib-common/generated/api-types/dailyservicetimes'
import LocalDate from 'lib-common/local-date'
import { StaticChip } from 'lib-components/atoms/Chip'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  MobileOnly,
  TabletAndDesktop
} from 'lib-components/layout/responsive-layout'
import colors from 'lib-customizations/common'

const weekDays = [1, 2, 3, 4, 5, 6, 7] as const
const colorsByTense: Record<Tense, string> = {
  past: colors.grayscale.g15,
  present: colors.main.m1,
  future: colors.grayscale.g15
}

interface DailyServiceTimeTableProps {
  dailyServiceTimes: DailyServiceTimes[]
}

export default React.memo(function DailyServiceTimeTable({
  dailyServiceTimes
}: DailyServiceTimeTableProps) {
  const t = useTranslation()

  return dailyServiceTimes.length > 0 ? (
    <>
      <TabletAndDesktop>
        <DailyServiceTimeTableDesktop dailyServiceTimes={dailyServiceTimes} />
      </TabletAndDesktop>
      <MobileOnly>
        <DailyServiceTimeTableMobile dailyServiceTimes={dailyServiceTimes} />
      </MobileOnly>
    </>
  ) : (
    <>{t.children.dailyServiceTime.empty}</>
  )
})

const DailyServiceTimeTableDesktop = ({
  dailyServiceTimes
}: DailyServiceTimeTableProps) => {
  const t = useTranslation()

  return (
    <Table data-qa="daily-service-time-table-desktop">
      <Thead>
        <Tr>
          <Th minimalWidth>{t.children.dailyServiceTime.validity}</Th>
          <Th>{t.children.dailyServiceTime.description}</Th>
          <Th minimalWidth>{t.children.dailyServiceTime.status}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {dailyServiceTimes
          .sort((a, b) =>
            b.times.validityPeriod.start.compareTo(a.times.validityPeriod.start)
          )
          .map((dailyServiceTime) => {
            const dateRange = dailyServiceTime.times.validityPeriod
            const tense = dateRange.tenseAt(LocalDate.todayInHelsinkiTz())
            return (
              <Tr
                key={dailyServiceTime.id}
                data-qa="daily-service-time-table-row-desktop"
              >
                <Td minimalWidth data-qa="daily-service-time-date-range">
                  {dailyServiceTime.times.validityPeriod.format()}
                </Td>
                <Td data-qa="daily-service-time-description">
                  <DailyServiceTimeValue value={dailyServiceTime.times} />
                </Td>
                <Td minimalWidth>
                  <StaticChip color={colorsByTense[tense]}>
                    {t.common.tense[tense]}
                  </StaticChip>
                </Td>
              </Tr>
            )
          })}
      </Tbody>
    </Table>
  )
}

const DailyServiceTimeTableMobile = ({
  dailyServiceTimes
}: DailyServiceTimeTableProps) => {
  const t = useTranslation()

  return (
    <FixedSpaceColumn data-qa="daily-service-time-table-mobile">
      {dailyServiceTimes
        .sort((a, b) =>
          b.times.validityPeriod.start.compareTo(a.times.validityPeriod.start)
        )
        .map((dailyServiceTime) => {
          const dateRange = dailyServiceTime.times.validityPeriod
          const tense = dateRange.tenseAt(LocalDate.todayInHelsinkiTz())
          return (
            <FixedSpaceColumn
              key={dailyServiceTime.id}
              data-qa="daily-service-time-table-row-mobile"
            >
              <FixedSpaceRow justifyContent="space-between">
                <strong data-qa="daily-service-time-date-range">
                  {dailyServiceTime.times.validityPeriod.format()}
                </strong>
                <StaticChip color={colorsByTense[tense]}>
                  {t.common.tense[tense]}
                </StaticChip>
              </FixedSpaceRow>
              <FixedSpaceRow data-qa="daily-service-time-description">
                <DailyServiceTimeValue value={dailyServiceTime.times} />
              </FixedSpaceRow>
            </FixedSpaceColumn>
          )
        })}
    </FixedSpaceColumn>
  )
}

const DailyServiceTimeValue = React.memo(function DailyServiceTimeValue({
  value
}: {
  value: DailyServiceTimesValue
}) {
  const t = useTranslation()

  switch (value.type) {
    case 'REGULAR':
      return value.regularTimes.format()
    case 'IRREGULAR':
      return (
        <>
          {weekDays
            .reduce<string[]>((data, weekDay) => {
              const range = getTimesOnWeekday(value, weekDay)
              if (range === null) {
                return data
              }
              return [
                ...data,
                `${
                  t.common.datetime.weekdaysShort[weekDay - 1]
                } ${range.format()}`
              ]
            }, [])
            .join(', ')}
        </>
      )
    case 'VARIABLE_TIME':
      return <>{t.children.dailyServiceTime.variableTime}</>
  }
})
