// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { RealtimeOccupancy } from 'lib-common/generated/api-types/occupancy'
import { fi } from 'date-fns/locale'
import { Line } from 'react-chartjs-2'
import colors from 'lib-customizations/common'
import { formatTime } from 'lib-common/date'
import { ceil } from 'lodash'
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

  const graphOptions = {
    scales: {
      xAxes: [
        {
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
          return formatTime(date)
        },
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        label: function (tooltipItem: any) {
          // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
          return `${String(ceil(tooltipItem.yLabel * 100) / 100)} %`
        }
      }
    }
  }

  return (
    <div>
      <Line
        data={{
          datasets: [
            {
              label: i18n.unit.occupancy.subtitles.realized,
              data: graphData,
              spanGaps: true,
              steppedLine: true,
              stepped: 'after',
              fill: false,
              pointBackgroundColor: colors.accents.green,
              borderColor: colors.accents.green
            }
          ]
        }}
        options={graphOptions}
        height={100}
      />
    </div>
  )
})
