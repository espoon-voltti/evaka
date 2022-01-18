// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChartData, ChartOptions } from 'chart.js'
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

  const graphData: DatePoint[] = occupancy.occupancySeries.map((p) => ({
    x: p.time,
    y: p.occupancyRatio !== null ? p.occupancyRatio * 100 : null
  }))

  const graphOptions: ChartOptions<'line'> = {
    scales: {
      x: {
        type: 'time',
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
          maxTicksLimit: 5,
          callback: function (value) {
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
            return formatTime(date)
          },
          label: function (tooltipItem) {
            return `${String(ceil(tooltipItem.parsed.y * 100) / 100)} %`
          }
        }
      }
    }
  }

  const data: ChartData<'line', DatePoint[]> = {
    datasets: [
      {
        label: i18n.unit.occupancy.subtitles.realized,
        data: graphData,
        spanGaps: true,
        stepped: 'before',
        fill: false,
        pointBackgroundColor: colors.status.success,
        borderColor: colors.status.success
      }
    ]
  }

  return (
    <div>
      <Line data={data} options={graphOptions} height={100} />
    </div>
  )
})
