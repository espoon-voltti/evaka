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
import {
  compareAsc,
  isBefore,
  max,
  min,
  roundToNearestMinutes,
  setHours,
  setMinutes,
  subHours
} from 'date-fns'
import { fi } from 'date-fns/locale'
import ceil from 'lodash/ceil'
import first from 'lodash/first'
import sortBy from 'lodash/sortBy'
import React, { useMemo, useCallback, useState } from 'react'
import { Line } from 'react-chartjs-2'
import styled from 'styled-components'

import { formatTime } from 'lib-common/date'
import { RealtimeOccupancy } from 'lib-common/generated/api-types/occupancy'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { Translations, useTranslation } from '../../../../state/i18n'

import { ChartTooltip, ChartTooltipData } from './ChartTooltip'

type DatePoint = { x: Date; y: number | null }

interface Props {
  occupancy: RealtimeOccupancy
  shiftCareUnit: boolean
}

export default React.memo(function OccupancyDayGraph({
  occupancy,
  shiftCareUnit
}: Props) {
  const { i18n } = useTranslation()
  return occupancy.occupancySeries.length === 0 ? (
    <GraphPlaceholder data-qa="no-data-placeholder">
      {i18n.unit.occupancy.realtime.noData}
    </GraphPlaceholder>
  ) : (
    <Graph occupancy={occupancy} shiftCareUnit={shiftCareUnit} />
  )
})

const Graph = React.memo(function Graph({ occupancy, shiftCareUnit }: Props) {
  const { i18n } = useTranslation()
  const [tooltipData, setTooltipData] = useState<ChartTooltipData>({
    children: <span></span>
  })
  const [tooltipVisible, setTooltipVisible] = useState<boolean>(false)
  const currentMinute = getCurrentMinute().getTime()

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
          compareAsc(a.arrived, time) <= 0 &&
          (a.departed === null || compareAsc(time, a.departed) < 0)
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
                <td>{child}</td>
              </tr>
              <tr>
                <td>{i18n.unit.occupancy.realtime.childrenPresent}</td>
                <td>{childrenPresent}</td>
              </tr>
              <tr>
                <td>{i18n.unit.occupancy.realtime.staffPresent}</td>
                <td>{staffPresent}</td>
              </tr>
              <tr>
                <td>{i18n.unit.occupancy.realtime.staffRequired}</td>
                <td>{staffRequired}</td>
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
        new Date(currentMinute),
        occupancy,
        i18n,
        tooltipHandler,
        shiftCareUnit
      ),
    [currentMinute, occupancy, i18n, tooltipHandler, shiftCareUnit]
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
  now: Date,
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
  const shiftCareMinTime = subHours(now, 16)
  const filterData = (p: { time: Date }) =>
    !isBefore(p.time, shiftCareUnit ? shiftCareMinTime : setTime(now, 0, 0))

  const childData = occupancy.occupancySeries.filter(filterData).map((p) => ({
    x: p.time,
    y: p.childCapacity
  }))

  const staffData = occupancy.occupancySeries.filter(filterData).map((p) => ({
    x: p.time,
    y: p.staffCapacity
  }))

  if (shiftCareUnit) {
    const lastDataPointBeforeMin = first(
      sortBy(
        occupancy.occupancySeries.filter(({ time }) =>
          isBefore(time, shiftCareMinTime)
        ),
        ({ time }) => -1 * time.getTime()
      )
    )
    if (lastDataPointBeforeMin) {
      staffData.splice(0, 0, {
        x: shiftCareMinTime,
        y: lastDataPointBeforeMin.staffCapacity
      })
      childData.splice(0, 0, {
        x: shiftCareMinTime,
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
    isBefore(lastStaffAttendance.x, now)
  ) {
    staffData.push({ ...lastStaffAttendance, x: new Date(now) })
    childData.push({ ...lastChildAttendance, x: new Date(now) })
  }

  const xMin = shiftCareUnit
    ? firstStaffAttendance?.x.getTime() ?? 0
    : min([
        // 6 AM
        setTime(new Date(), 6, 0),
        staffData[0].x
      ]).getTime()
  const xMax = shiftCareUnit
    ? staffData[staffData.length - 1]?.x.getTime() ?? 0
    : max([
        // 6 PM
        setTime(new Date(), 18, 0),
        now
      ]).getTime()

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
        min: xMin,
        max: xMax,
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

function getCurrentMinute(): Date {
  return roundToNearestMinutes(new Date())
}

function setTime(date: Date, hours: number, minutes: number): Date {
  return setHours(setMinutes(date, minutes), hours)
}

const GraphPlaceholder = styled.div`
  margin: ${defaultMargins.XXL} 0;
`
