// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Chart, ChartData, ChartOptions, TooltipModel } from 'chart.js'
import ceil from 'lodash/ceil'
import isEqual from 'lodash/isEqual'
import max from 'lodash/max'
import React, { useMemo, useCallback, useState } from 'react'
import { Line } from 'react-chartjs-2'
import styled from 'styled-components'

import { useBoolean } from 'lib-common/form/hooks'
import { AttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { Translations, useTranslation } from '../../../../state/i18n'

import { ChartTooltip } from './ChartTooltip'
import { LegendSquare } from './OccupanciesForSingleDay'
import { DatePoint, defaultChartOptions, hidden, line } from './chart-commons'

interface Props {
  queryDate: LocalDate
  rows: AttendanceReservationReportRow[]
}

export default React.memo(function OccupancyDayGraphPlanned({
  queryDate,
  rows
}: Props) {
  const { i18n } = useTranslation()
  return (
    <FixedSpaceColumn>
      <Graph queryDate={queryDate} rows={rows} />
      <H3>{i18n.unit.occupancy.realtime.legendTitle}</H3>
      <FixedSpaceColumn>
        <FixedSpaceRow>
          <LegendSquare color={colors.grayscale.g100} />
          <Label>{i18n.unit.occupancy.realtime.children}</Label>
        </FixedSpaceRow>
        <FixedSpaceRow>
          <LegendSquare color={colors.main.m3} />
          <Label>{i18n.unit.occupancy.realtime.staffRequired}</Label>
        </FixedSpaceRow>
      </FixedSpaceColumn>
    </FixedSpaceColumn>
  )
})

const Graph = React.memo(function Graph({ queryDate, rows }: Props) {
  const { i18n } = useTranslation()
  const [tooltipParams, setTooltipParams] = useState<{
    position?: {
      x: number
      y: number
      xAlign: string
      yAlign: string
      caretX: number
      caretY: number
    }
    data?: {
      date: Date
      children: number
      childrenPresent: number
      staffRequired: number
    }
  }>({})
  const [tooltipVisible, { on: showTooltip, off: hideTooltip }] =
    useBoolean(false)

  const tooltipHandler = useCallback(
    ({
      chart: {
        data: { datasets }
      },
      tooltip
    }: {
      chart: Chart<'line', DatePoint[]>
      tooltip: TooltipModel<'line'>
    }) => {
      const date = datasets[0].data[tooltip.dataPoints?.[0]?.dataIndex]?.x
      if (!date) {
        setTooltipParams((previous) => (isEqual(previous, {}) ? previous : {}))
        return
      }

      const children = datasets[0].data[tooltip.dataPoints[0].dataIndex]?.y ?? 0
      const childrenPresent =
        datasets[1].data[tooltip.dataPoints[0].dataIndex]?.y ?? 0
      const staffRequired =
        datasets[2].data[tooltip.dataPoints[0].dataIndex]?.y ?? 0

      setTooltipParams((previous) => {
        const { x, y, xAlign, yAlign, caretX, caretY } = tooltip
        const position = { x, y, xAlign, yAlign, caretX, caretY }
        const data = {
          date,
          children,
          childrenPresent,
          staffRequired
        }
        const updated = { position, data }
        return isEqual(previous, updated) ? previous : updated
      })
    },
    []
  )

  const { data, graphOptions } = useMemo(
    () => graphData(queryDate, rows, i18n, tooltipHandler),
    [queryDate, rows, i18n, tooltipHandler]
  )

  const tooltipContent = useMemo(
    () =>
      tooltipParams.data ? (
        <>
          <span>
            {HelsinkiDateTime.fromSystemTzDate(tooltipParams.data.date)
              .toLocalTime()
              .format()}
          </span>
          <Gap size="xs" />
          <table>
            <tbody>
              <tr>
                <td>
                  <FixedSpaceRow alignItems="center" spacing="xs">
                    <LegendSquare small color={colors.grayscale.g100} />
                    <span>{i18n.unit.occupancy.realtime.children}</span>
                  </FixedSpaceRow>
                </td>
                <td align="right">
                  {parseFloat(tooltipParams.data.children.toFixed(1))}
                </td>
              </tr>
              <tr>
                <td>
                  <FixedSpaceRow alignItems="center" spacing="xs">
                    <LegendSquare small color="transparent" />
                    <span>{i18n.unit.occupancy.realtime.childrenPresent}</span>
                  </FixedSpaceRow>
                </td>
                <td align="right">
                  {parseFloat(tooltipParams.data.childrenPresent.toFixed(1))}
                </td>
              </tr>
              <tr>
                <td>
                  <FixedSpaceRow alignItems="center" spacing="xs">
                    <LegendSquare small color={colors.main.m3} />
                    <span>{i18n.unit.occupancy.realtime.staffRequired}</span>
                  </FixedSpaceRow>
                </td>
                <td align="right">
                  {parseFloat(tooltipParams.data.staffRequired.toFixed(1))}
                </td>
              </tr>
            </tbody>
          </table>
        </>
      ) : null,
    [i18n, tooltipParams.data]
  )

  return (
    <GraphContainer onMouseOver={showTooltip} onMouseOut={hideTooltip}>
      <Line
        data={data}
        options={graphOptions}
        width="auto"
        height={undefined}
      />
      <ChartTooltip
        position={tooltipParams.position}
        content={tooltipContent}
        visible={tooltipVisible}
      />
    </GraphContainer>
  )
})

const GraphContainer = styled.div`
  position: relative;
  width: 100%;
  height: 400px;
  max-height: 40vh;
`

function graphData(
  queryDate: LocalDate,
  rows: AttendanceReservationReportRow[],
  i18n: Translations,
  tooltipHandler: (args: {
    chart: Chart<'line', DatePoint[]>
    tooltip: TooltipModel<'line'>
  }) => void
): {
  data: ChartData<'line', DatePoint[]>
  graphOptions: ChartOptions<'line'>
} {
  const data: ChartData<'line', DatePoint[]> = {
    datasets: [
      line(
        i18n.unit.occupancy.realtime.children,
        rows.map((row) => ({
          x: row.dateTime.toSystemTzDate(),
          y: row.capacityFactor
        })),
        colors.grayscale.g100
      ),
      hidden(
        rows.map((row) => ({
          x: row.dateTime.toSystemTzDate(),
          y: row.childCount
        }))
      ),
      line(
        i18n.unit.occupancy.realtime.staffRequired,
        rows.map((row) => ({
          x: row.dateTime.toSystemTzDate(),
          y: ceil(row.staffCountRequired)
        })),
        colors.main.m3,
        'y1'
      )
    ]
  }

  const now = HelsinkiDateTime.now()
  const maxY = max(rows.map((row) => row.capacityFactor))

  const graphOptions: ChartOptions<'line'> = {
    ...defaultChartOptions,
    scales: {
      ...defaultChartOptions.scales,
      y: {
        min: 0,
        ticks: {
          maxTicksLimit: 5
        },
        title: {
          display: true,
          text: i18n.unit.occupancy.realtime.chartYAxisTitle
        }
      },
      y1: {
        min: 0,
        position: 'right',
        title: {
          display: true,
          text: i18n.unit.occupancy.realtime.chartY1AxisTitle
        },
        grid: {
          drawOnChartArea: false
        }
      }
    },
    plugins: {
      ...defaultChartOptions.plugins,
      tooltip: {
        ...defaultChartOptions.plugins?.tooltip,
        // The types in the chart package are bad so we need this...
        external: tooltipHandler as unknown as (
          this: TooltipModel<'line'>,
          args: { chart: Chart; tooltip: TooltipModel<'line'> }
        ) => void
      },
      ...(now.toLocalDate().isEqual(queryDate)
        ? {
            annotation: {
              annotations: {
                currentTime: {
                  type: 'line',
                  xMin: now.toSystemTzDate().getTime(),
                  xMax: now.toSystemTzDate().getTime(),
                  yMin: 0,
                  yMax: maxY !== undefined && maxY > 0 ? maxY : 1,
                  borderColor: colors.status.success,
                  arrowHeads: {
                    display: true
                  }
                }
              }
            }
          }
        : undefined)
    }
  }

  return { data, graphOptions }
}
