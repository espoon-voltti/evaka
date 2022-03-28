// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChartData, ChartOptions } from 'chart.js'
import { setHours, setMinutes } from 'date-fns'
import { fi } from 'date-fns/locale'
import { ceil } from 'lodash'
import React from 'react'
import { Line } from 'react-chartjs-2'

import { formatTime } from 'lib-common/date'
import { RealtimeOccupancy } from 'lib-common/generated/api-types/occupancy'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../../../state/i18n'

type DatePoint = { x: Date; y: number | null }

interface Props {
  occupancy: RealtimeOccupancy
}

export default React.memo(function OccupancyDayGraph({ occupancy }: Props) {
  const { i18n } = useTranslation()

  if (occupancy.occupancySeries.length === 0) return null

  const childData: DatePoint[] = occupancy.occupancySeries.map((p) => ({
    x: p.time,
    y: p.childCapacity
  }))
  const staffData: DatePoint[] = occupancy.occupancySeries.map((p) => ({
    x: p.time,
    y: p.staffCount * 7
  }))

  const data: ChartData<'line', DatePoint[]> = {
    datasets: [
      {
        label: i18n.unit.occupancy.realtime.children,
        data: childData,
        spanGaps: true,
        stepped: 'before',
        fill: false,
        pointBackgroundColor: colors.grayscale.g100,
        borderColor: colors.grayscale.g100
      },
      {
        label: i18n.unit.occupancy.realtime.childrenMax,
        data: staffData,
        spanGaps: true,
        stepped: 'before',
        fill: false,
        pointBackgroundColor: colors.status.danger,
        borderColor: colors.status.danger
      }
    ]
  }

  const firstAttendance = staffData[0].x.getTime()
  const at06 = setMinutes(setHours(firstAttendance, 6), 0).getTime()

  const lastAttendance = staffData[staffData.length - 1].x.getTime()
  const at18 = setMinutes(setHours(lastAttendance, 18), 0).getTime()

  const graphOptions: ChartOptions<'line'> = {
    scales: {
      x: {
        type: 'time',
        min: Math.min(at06, firstAttendance),
        max: Math.max(at18, lastAttendance),
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
            return i18n.unit.occupancy.realtime.tooltipFooter(
              utilization,
              staffPresent,
              staffRequired
            )
          }
        }
      }
    }
  }

  return (
    <div>
      <Line data={data} options={graphOptions} height={100} />
    </div>
  )
})
