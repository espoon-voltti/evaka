// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Chart,
  ChartData,
  ChartDataset,
  ChartOptions,
  TooltipModel
} from 'chart.js'
import { compareAsc, isBefore, max, min, roundToNearestMinutes } from 'date-fns'
import { fi } from 'date-fns/locale'
import ceil from 'lodash/ceil'
import first from 'lodash/first'
import sortBy from 'lodash/sortBy'
import React, { useMemo, useCallback, useState } from 'react'
import { Line } from 'react-chartjs-2'
import styled from 'styled-components'

import { formatTime } from 'lib-common/date'
import { RealtimeOccupancy } from 'lib-common/generated/api-types/occupancy'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { Translations, useTranslation } from '../../../../state/i18n'

import { ChartTooltip, ChartTooltipData } from './ChartTooltip'

type DatePoint = { x: Date; y: number | null }

interface Props {
  queryDate: LocalDate
  occupancy: RealtimeOccupancy
  shiftCareUnit: boolean
}

export default React.memo(function OccupancyDayGraph({
  queryDate,
  occupancy,
  shiftCareUnit
}: Props) {
  const { i18n } = useTranslation()
  return occupancy.occupancySeries.length === 0 ? (
    <GraphPlaceholder data-qa="no-data-placeholder">
      {i18n.unit.occupancy.realtime.noData}
    </GraphPlaceholder>
  ) : (
    <Graph
      queryDate={queryDate}
      occupancy={occupancy}
      shiftCareUnit={shiftCareUnit}
    />
  )
})

const Graph = React.memo(function Graph({
  queryDate,
  occupancy,
  shiftCareUnit
}: Props) {
  const { i18n } = useTranslation()
  const [tooltipData, setTooltipData] = useState<ChartTooltipData>({
    children: <span></span>
  })
  const [tooltipVisible, setTooltipVisible] = useState<boolean>(false)
  const currentMinute = getCurrentMinute()

  const showTooltip = useCallback(() => {
    if (!tooltipVisible) {
      setTooltipVisible(true)
    }
  }, [tooltipVisible, setTooltipVisible])
  const hideTooltip = useCallback(() => {
    setTooltipVisible(false)
  }, [setTooltipVisible])

  const getChildrenPresentAtTime = useCallback(
    (time: Date): number =>
      occupancy.childAttendances.filter(
        (a) =>
          compareAsc(a.arrived.toSystemTzDate(), time) <= 0 &&
          (a.departed === null ||
            compareAsc(time, a.departed.toSystemTzDate()) < 0)
      ).length,
    [occupancy]
  )

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
      const date = new Date(datasets[0].data[tooltip.dataPoints[0].dataIndex].x)
      const child = datasets[0].data[tooltip.dataPoints[0].dataIndex].y ?? 0
      const staff = datasets[1].data[tooltip.dataPoints[0].dataIndex].y
      const utilization = child === 0 ? 0 : ceil((child / (staff ?? 1)) * 100)

      const staffRequired = ceil(child / 7)

      const staffPresent = (staff ?? 0) / 7

      const childrenPresent = getChildrenPresentAtTime(date)
      const body = (
        <>
          <span>{formatTime(date)}</span>
          <Gap size="xs" />
          <table>
            <tbody>
              <tr>
                <td>{i18n.unit.occupancy.realtime.utilization}</td>
                <td>{utilization}%</td>
              </tr>
              <tr>
                <td>{i18n.unit.occupancy.realtime.children}</td>
                <td>{parseFloat(child.toFixed(1))}</td>
              </tr>
              <tr>
                <td>{i18n.unit.occupancy.realtime.childrenPresent}</td>
                <td>{parseFloat(childrenPresent.toFixed(1))}</td>
              </tr>
              <tr>
                <td>{i18n.unit.occupancy.realtime.staffPresent}</td>
                <td>{parseFloat(staffPresent.toFixed(1))}</td>
              </tr>
              <tr>
                <td>{i18n.unit.occupancy.realtime.staffRequired}</td>
                <td>{parseFloat(staffRequired.toFixed(1))}</td>
              </tr>
            </tbody>
          </table>
        </>
      )
      setTooltipData({
        tooltip,
        children: body
      })
    },
    [setTooltipData, getChildrenPresentAtTime, i18n]
  )

  const { data, graphOptions } = useMemo(
    () =>
      graphData(
        queryDate,
        currentMinute,
        occupancy,
        i18n,
        tooltipHandler,
        shiftCareUnit
      ),
    [queryDate, currentMinute, occupancy, i18n, tooltipHandler, shiftCareUnit]
  )

  if (data.datasets.length === 0)
    return (
      <GraphPlaceholder data-qa="no-data-placeholder">
        {i18n.unit.occupancy.realtime.noData}
      </GraphPlaceholder>
    )

  return (
    <div onMouseOver={showTooltip} onMouseOut={hideTooltip}>
      <Line data={data} options={graphOptions} height={100} />
      <ChartTooltip visible={tooltipVisible} data={tooltipData} />
    </div>
  )
})

function graphData(
  queryDate: LocalDate,
  now: HelsinkiDateTime,
  occupancy: RealtimeOccupancy,
  i18n: Translations,
  tooltipHandler: (args: {
    chart: Chart<'line', DatePoint[]>
    tooltip: TooltipModel<'line'>
  }) => void,
  shiftCareUnit: boolean
): {
  data: ChartData<'line', DatePoint[]>
  graphOptions: ChartOptions<'line'>
} {
  const minTime = queryDate.isEqual(now.toLocalDate())
    ? shiftCareUnit
      ? now.subHours(16)
      : now.withTime(LocalTime.of(0, 0))
    : HelsinkiDateTime.fromLocal(queryDate, LocalTime.of(0, 0))
  const filterData = (p: { time: HelsinkiDateTime }) =>
    !p.time.isBefore(minTime)

  const childData = occupancy.occupancySeries.filter(filterData).map((p) => ({
    x: p.time.toSystemTzDate(),
    y: p.childCapacity
  }))

  const staffData = occupancy.occupancySeries.filter(filterData).map((p) => ({
    x: p.time.toSystemTzDate(),
    y: p.staffCapacity
  }))

  if (shiftCareUnit) {
    const lastDataPointBeforeMin = first(
      sortBy(
        occupancy.occupancySeries.filter(({ time }) => time.isBefore(minTime)),
        ({ time }) => -1 * time.timestamp
      )
    )
    if (lastDataPointBeforeMin) {
      staffData.splice(0, 0, {
        x: minTime.toSystemTzDate(),
        y: lastDataPointBeforeMin.staffCapacity
      })
      childData.splice(0, 0, {
        x: minTime.toSystemTzDate(),
        y: lastDataPointBeforeMin.childCapacity
      })
    }
  }

  const firstStaffAttendance = staffData[0]
  const lastStaffAttendance = staffData[staffData.length - 1]
  const lastChildAttendance = childData[childData.length - 1]

  // If staff is still present, extend the graph up to current time
  if (
    lastStaffAttendance &&
    lastStaffAttendance.y > 0 &&
    isBefore(lastStaffAttendance.x, now.toSystemTzDate())
  ) {
    staffData.push({ ...lastStaffAttendance, x: now.toSystemTzDate() })
    childData.push({ ...lastChildAttendance, x: now.toSystemTzDate() })
  }

  const queryDateIsCurrent = queryDate.isEqual(now.toLocalDate())
  const xMin = shiftCareUnit
    ? (queryDateIsCurrent
        ? now.subHours(16)
        : HelsinkiDateTime.fromLocal(queryDate, LocalTime.of(0, 0))
      ).toSystemTzDate()
    : min(
        [
          (queryDateIsCurrent
            ? now.withTime(LocalTime.of(6, 0))
            : HelsinkiDateTime.fromLocal(queryDate, LocalTime.of(6, 0))
          ).toSystemTzDate(),
          firstStaffAttendance?.x
        ].filter((time): time is Date => !!time)
      )
  const xMax = shiftCareUnit
    ? (queryDateIsCurrent
        ? now
        : HelsinkiDateTime.fromLocal(queryDate, LocalTime.of(23, 59, 59))
      ).toSystemTzDate()
    : max(
        [
          (queryDateIsCurrent
            ? now.withTime(LocalTime.of(18, 0))
            : HelsinkiDateTime.fromLocal(queryDate, LocalTime.of(18, 0))
          ).toSystemTzDate(),
          lastStaffAttendance?.x
        ].filter((time): time is Date => !!time)
      )

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

  const graphOptions: ChartOptions<'line'> = {
    scales: {
      x: {
        type: 'time',
        min: xMin.getTime(),
        max: xMax.getTime(),
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
        enabled: false,
        position: 'nearest',
        intersect: false,
        // The types in the chart package are bad so we need this...
        external: tooltipHandler as unknown as (
          this: TooltipModel<'line'>,
          args: { chart: Chart; tooltip: TooltipModel<'line'> }
        ) => void
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

function getCurrentMinute(): HelsinkiDateTime {
  return HelsinkiDateTime.fromSystemTzDate(roundToNearestMinutes(new Date()))
}

const GraphPlaceholder = styled.div`
  margin: ${defaultMargins.XXL} 0;
`
