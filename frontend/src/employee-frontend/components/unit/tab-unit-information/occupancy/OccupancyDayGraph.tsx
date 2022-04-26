// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChartData, ChartDataset, ChartOptions } from 'chart.js'
import {
  compareAsc,
  isBefore,
  max,
  min,
  roundToNearestMinutes,
  setHours,
  setMinutes
} from 'date-fns'
import { fi } from 'date-fns/locale'
import { ceil } from 'lodash'
import React, { useMemo } from 'react'
import { Line } from 'react-chartjs-2'
import styled from 'styled-components'

import { formatTime } from 'lib-common/date'
import { RealtimeOccupancy } from 'lib-common/generated/api-types/occupancy'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { Translations, useTranslation } from '../../../../state/i18n'

type DatePoint = { x: Date; y: number | null }

interface Props {
  occupancy: RealtimeOccupancy
}

export default React.memo(function OccupancyDayGraph({ occupancy }: Props) {
  const { i18n } = useTranslation()
  return occupancy.occupancySeries.length === 0 ? (
    <GraphPlaceholder>{i18n.unit.occupancy.realtime.noData}</GraphPlaceholder>
  ) : (
    <Graph occupancy={occupancy} />
  )
})

const Graph = React.memo(function Graph({ occupancy }: Props) {
  const { i18n } = useTranslation()

  const currentMinute = getCurrentMinute().getTime()
  const { data, graphOptions } = useMemo(
    () => graphData(new Date(currentMinute), occupancy, i18n),
    [occupancy, currentMinute, i18n]
  )

  if (occupancy.occupancySeries.length === 0) return null

  return (
    <div>
      <Line data={data} options={graphOptions} height={100} />
    </div>
  )
})

function graphData(
  now: Date,
  occupancy: RealtimeOccupancy,
  i18n: Translations
): {
  data: ChartData<'line', DatePoint[]>
  graphOptions: ChartOptions<'line'>
} {
  const childData = occupancy.occupancySeries.map((p) => ({
    x: p.time,
    y: p.childCapacity
  }))

  const staffData = occupancy.occupancySeries.map((p) => ({
    x: p.time,
    y: p.staffCapacity
  }))

  const firstStaffAttendance = staffData[0]
  const lastStaffAttendance = staffData[staffData.length - 1]
  const lastChildAttendance = childData[childData.length - 1]

  const sixAm = setTime(firstStaffAttendance.x, 6, 0)
  const sixPm = setTime(lastStaffAttendance.x, 18, 0)

  // If staff is still present, extend the graph up to current time
  if (lastStaffAttendance.y > 0 && isBefore(lastStaffAttendance.x, now)) {
    staffData.push({ ...lastStaffAttendance, x: new Date(now) })
    childData.push({ ...lastChildAttendance, x: new Date(now) })
  }

  const data: ChartData<'line', DatePoint[]> = {
    datasets: [
      line(
        i18n.unit.occupancy.realtime.children,
        childData,
        colors.grayscale.g100
      ),
      line(
        i18n.unit.occupancy.realtime.childrenMax,
        staffData,
        colors.status.danger
      )
    ]
  }

  const getChildrenPresentAtTime = (time: Date): number =>
    occupancy.childAttendances.filter(
      (a) =>
        compareAsc(a.arrived, time) <= 0 &&
        compareAsc(time, a.departed || time) < 0
    ).length

  const graphOptions: ChartOptions<'line'> = {
    scales: {
      x: {
        type: 'time',
        min: min([sixAm, firstStaffAttendance.x]).getTime(),
        max: max([sixPm, lastStaffAttendance.x]).getTime(),
        time: {
          displayFormats: {
            hour: 'HH:00'
          },
          minUnit: 'hour'
        },
        adapters: {
          date: {
            locale: fi
          }
        }
      },
      y: {
        min: 0,
        ticks: {
          maxTicksLimit: 5
        },
        title: {
          display: true,
          text: i18n.unit.occupancy.realtime.chartYAxisTitle
        }
      }
    },
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        mode: 'index',
        intersect: false,
        position: 'nearest',
        callbacks: {
          title: (items) => formatTime(new Date(items[0].parsed.x)),
          footer: (items) => {
            const child = items.find((i) => i.datasetIndex === 0)?.parsed.y ?? 0
            const staff = items.find((i) => i.datasetIndex === 1)?.parsed.y
            const utilization =
              child === 0 ? 0 : ceil((child / (staff ?? 1)) * 100)

            const staffRequired = ceil(child / 7)

            const staffPresent = (staff ?? 0) / 7

            const childrenPresent = getChildrenPresentAtTime(
              new Date(items[0].parsed.x)
            )
            return i18n.unit.occupancy.realtime.tooltipFooter(
              utilization,
              staffPresent,
              staffRequired,
              childrenPresent
            )
          }
        }
      }
    }
  }

  return { data, graphOptions }
}

function line(
  label: string,
  data: DatePoint[],
  color: string
): ChartDataset<'line', DatePoint[]> {
  return {
    label,
    data,
    spanGaps: true,
    stepped: 'before',
    fill: false,
    pointRadius: 0,
    pointBackgroundColor: color,
    borderWidth: 2,
    borderColor: color,
    borderJoinStyle: 'miter'
  }
}

function getCurrentMinute(): Date {
  return roundToNearestMinutes(new Date())
}

function setTime(date: Date, hours: number, minutes: number): Date {
  return setHours(setMinutes(date, minutes), hours)
}

const GraphPlaceholder = styled.div`
  margin: ${defaultMargins.XXL} 0;
`
