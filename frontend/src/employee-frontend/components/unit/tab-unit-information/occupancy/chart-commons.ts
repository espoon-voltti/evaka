// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChartDataset, ChartOptions } from 'chart.js'
import { fi } from 'date-fns/locale'

export type DatePoint = { x: Date; y: number | null }

export function line(
  label: string,
  data: DatePoint[],
  color: string,
  yAxisID = 'y'
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
    borderJoinStyle: 'miter',
    yAxisID
  }
}

export function hidden(data: DatePoint[]): ChartDataset<'line', DatePoint[]> {
  return {
    data,
    hidden: true
  }
}

export const defaultChartOptions: ChartOptions<'line'> = {
  responsive: true,
  maintainAspectRatio: false,
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
    }
  },
  plugins: {
    legend: {
      display: false
    },
    tooltip: {
      enabled: false,
      position: 'nearest',
      intersect: false
    }
  }
}
