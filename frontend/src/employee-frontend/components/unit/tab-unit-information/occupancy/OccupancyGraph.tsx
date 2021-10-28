// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { fi } from 'date-fns/locale'
import { defaults, Line } from 'react-chartjs-2'
import { OccupancyResponse } from '../../../../api/unit'
import colors from 'lib-customizations/common'
import { formatDate } from 'lib-common/date'
import { ChartOptions } from 'chart.js'

defaults.animation = false
defaults.font = {
  family: '"Open Sans", "Arial", sans-serif'
}
defaults.color = colors.greyscale.darkest

type DatePoint = { x: Date; y: number | null | undefined }

function getGraphData(occupancies: OccupancyResponse) {
  const data: DatePoint[] = []
  occupancies.occupancies.forEach((occupancy) => {
    data.push({
      x: occupancy.period.start.toSystemTzDate(),
      y: occupancy.percentage
    })
    data.push({
      x: occupancy.period.end.toSystemTzDate(),
      y: occupancy.percentage
    })
  })

  const lastOccupancy =
    occupancies.occupancies.length > 0
      ? occupancies.occupancies[occupancies.occupancies.length - 1]
      : null
  if (lastOccupancy)
    data.push({
      x: lastOccupancy.period.end.toSystemTzDate(),
      y: lastOccupancy.percentage
    })

  return data
}

function getGraphOptions(startDate: Date, endDate: Date): ChartOptions<'line'> {
  return {
    scales: {
      xAxis: {
        type: 'time',
        min: startDate.getDate(),
        max: endDate.getDate(),
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
      yAxis: {
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
            return formatDate(date)
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
  const datasets = []
  if (confirmed)
    datasets.push({
      label: 'Vahvistettu täyttöaste',
      data: getGraphData(occupancies),
      steppedLine: true,
      fill: false,
      pointBackgroundColor: colors.brandEspoo.espooBlue,
      borderColor: colors.brandEspoo.espooBlue
    })
  if (planned)
    datasets.push({
      label: 'Suunniteltu täyttöaste',
      data: getGraphData(plannedOccupancies),
      steppedLine: true,
      fill: false,
      pointBackgroundColor: colors.accents.water,
      borderColor: colors.accents.water
    })
  if (realized)
    datasets.push({
      label: 'Käyttöaste',
      data: getGraphData(realizedOccupancies),
      steppedLine: true,
      fill: false,
      pointBackgroundColor: colors.accents.green,
      borderColor: colors.accents.green
    })

  return (
    <Line
      data={{ datasets }}
      options={getGraphOptions(startDate, endDate)}
      height={300}
    />
  )
})
