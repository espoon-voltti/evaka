// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { fi } from 'date-fns/locale'
import { defaults, Line } from 'react-chartjs-2'
import { OccupancyResponse } from '../../../../api/unit'
import colors from '@evaka/lib-components/src/colors'
import { formatDate } from '../../../../utils/date'

// eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
defaults.global.animation = false
// eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
defaults.global.defaultFontFamily = '"Open Sans", "Arial", sans-serif'
// eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
defaults.global.defaultFontColor = colors.greyscale.darkest

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

function getGraphOptions(startDate: Date, endDate: Date) {
  return {
    scales: {
      xAxes: [
        {
          type: 'time',
          ticks: {
            min: startDate,
            max: endDate
          },
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
        }
      ],
      yAxes: [
        {
          ticks: {
            min: 0,
            maxTicksLimit: 5,
            callback: function (value: unknown) {
              return `${String(value)} %`
            }
          }
        }
      ]
    },
    legend: {
      display: false
    },
    tooltips: {
      callbacks: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        title: function (tooltipItems: unknown, data: any) {
          // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-explicit-any
          const tooltipItem = (tooltipItems as any[])[0]
          // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
          const date =
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index].x
          return formatDate(date)
        },
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        label: function (tooltipItem: any) {
          // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
          return `${String(tooltipItem.yLabel)} %`
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
