// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { fi } from 'date-fns/locale'
import { defaults, Line } from 'react-chartjs-2'
import { OccupancyResponse } from '../../../../api/unit'
import colors from 'lib-customizations/common'
import { ChartDataset, ChartOptions } from 'chart.js'
import { formatDate } from 'lib-common/date'

defaults.animation = false
defaults.font = {
  family: '"Open Sans", "Arial", sans-serif',
  ...defaults.font
}
defaults.color = colors.greyscale.darkest

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
  const datasets: ChartDataset<'line', DatePoint[]>[] = []
  if (confirmed)
    datasets.push({
      label: 'Vahvistettu täyttöaste',
      data: getGraphData(occupancies),
      stepped: true,
      fill: false,
      pointBackgroundColor: colors.brandEspoo.espooBlue,
      borderColor: colors.brandEspoo.espooBlue
    })
  if (planned)
    datasets.push({
      label: 'Suunniteltu täyttöaste',
      data: getGraphData(plannedOccupancies),
      stepped: true,
      fill: false,
      pointBackgroundColor: colors.accents.water,
      borderColor: colors.accents.water
    })
  if (realized)
    datasets.push({
      label: 'Käyttöaste',
      data: getGraphData(realizedOccupancies),
      stepped: true,
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
