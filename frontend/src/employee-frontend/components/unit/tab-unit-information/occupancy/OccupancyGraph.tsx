// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChartDataset, ChartOptions } from 'chart.js'
import { fi } from 'date-fns/locale'
import React from 'react'
import { Line } from 'react-chartjs-2'

import { OccupancyResponse } from 'lib-common/generated/api-types/occupancy'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import colors from 'lib-customizations/common'

type DatePoint = { x: Date; y: number | null | undefined }

function getGraphData({ occupancies }: OccupancyResponse) {
  return occupancies.flatMap((occupancy) => [
    {
      x: occupancy.period.start.toSystemTzDate(),
      y: occupancy.percentage
    },
    {
      x: occupancy.period.end.toSystemTzDate(),
      y: occupancy.percentage
    }
  ])
}

function getGraphOptions(startDate: Date, endDate: Date): ChartOptions<'line'> {
  return {
    scales: {
      x: {
        type: 'time',
        // chart.js typings say number, but 'time' scale wants Date
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-explicit-any
        min: startDate as any,
        // chart.js typings say number, but 'time' scale wants Date
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-explicit-any
        max: endDate as any,
        time: {
          displayFormats: {
            day: 'd.M'
          },
          minUnit: 'day'
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
          maxTicksLimit: 5,
          callback: function (value: unknown) {
            return `${String(value)} %`
          }
        }
      }
    },
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        callbacks: {
          title: function (tooltipItems) {
            const tooltipItem = tooltipItems[0]
            const date = new Date(tooltipItem.parsed.x)
            return HelsinkiDateTime.fromSystemTzDate(date)
              .toLocalDate()
              .format()
          },
          label: function (tooltipItem) {
            return `${String(tooltipItem.parsed.y)} %`
          }
        }
      }
    }
  }
}

interface Props {
  occupancies: OccupancyResponse
  plannedOccupancies: OccupancyResponse
  realizedOccupancies: OccupancyResponse
  confirmed: boolean
  planned: boolean
  realized: boolean
  startDate: Date
  endDate: Date
}

export default React.memo(function OccupancyGraph({
  occupancies,
  plannedOccupancies,
  realizedOccupancies,
  confirmed,
  planned,
  realized,
  startDate,
  endDate
}: Props) {
  const datasets: ChartDataset<'line', DatePoint[]>[] = []
  if (confirmed)
    datasets.push({
      label: 'Vahvistettu täyttöaste',
      data: getGraphData(occupancies),
      stepped: true,
      fill: false,
      pointBackgroundColor: colors.main.m1,
      borderColor: colors.main.m1
    })
  if (planned)
    datasets.push({
      label: 'Suunniteltu täyttöaste',
      data: getGraphData(plannedOccupancies),
      stepped: true,
      fill: false,
      pointBackgroundColor: colors.accents.a6turquoise,
      borderColor: colors.accents.a6turquoise
    })
  if (realized)
    datasets.push({
      label: 'Käyttöaste',
      data: getGraphData(realizedOccupancies),
      stepped: true,
      fill: false,
      pointBackgroundColor: colors.status.success,
      borderColor: colors.status.success
    })

  return (
    <Line
      data={{ datasets }}
      options={getGraphOptions(startDate, endDate)}
      height={300}
    />
  )
})
