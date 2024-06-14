// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChartDataset, ChartOptions, ScatterDataPoint } from 'chart.js'
import React, { useEffect, useState } from 'react'
import { Line } from 'react-chartjs-2'

import { Failure, Loading, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { client } from '../../api/client'

export default React.memo(function AIPage() {
  const [status, setStatus] = useState<Result<Status>>(Loading.of())

  const start = () => {
    void client.post('/ai/start')
    setStatus(Loading.of())
  }

  const stop = () => {
    void client.post('/ai/stop')
    setStatus(Loading.of())
  }

  useEffect(() => {
    const interval = setInterval(() => {
      void getStatus().then((res) => setStatus(res))
    }, 2000)

    return () => clearInterval(interval)
  }, [setStatus])

  const datasets: ChartDataset<'line', ScatterDataPoint[]>[] = [
    {
      label: 'childrenInFirstPreferencePercentage',
      data: status.isSuccess
        ? status.value.generations.map((g) => ({
            x: g.generation,
            y: g.value.childrenInFirstPreferencePercentage / 100
          }))
        : [],
      stepped: false,
      fill: false,
      pointBackgroundColor: colors.status.danger,
      borderColor: colors.status.danger
    },
    {
      label: 'childrenInOneOfPreferencesPercentage',
      data: status.isSuccess
        ? status.value.generations.map((g) => ({
            x: g.generation,
            y: g.value.childrenInOneOfPreferencesPercentage / 100
          }))
        : [],
      stepped: false,
      fill: false,
      pointBackgroundColor: colors.status.warning,
      borderColor: colors.status.warning
    },
    {
      label: 'maxCapacityPercentage',
      data: status.isSuccess
        ? status.value.generations.map((g) => ({
            x: g.generation,
            y: g.value.maxCapacityPercentage / 100
          }))
        : [],
      stepped: false,
      fill: false,
      pointBackgroundColor: colors.main.m1,
      borderColor: colors.main.m1
    }
  ]

  return (
    <Container>
      <ContentArea opaque>
        <H1>AI Test</H1>
        {status.isFailure && <ErrorSegment />}
        {status.isLoading && <SpinnerSegment />}
        {status.isSuccess && (
          <div>
            {status.value.running ? (
              <LegacyButton text="Lopeta" primary onClick={stop} />
            ) : (
              <LegacyButton text="Käynnistä" primary onClick={start} />
            )}

            <Gap />

            <div style={{ height: '400px', width: '800px' }}>
              <Line data={{ datasets }} options={getGraphOptions()} redraw />
            </div>

            <Gap />

            <div>
              cost:{' '}
              {status.value.generations.length > 0
                ? status.value.generations[status.value.generations.length - 1]
                    .cost
                : '-'}
            </div>
          </div>
        )}
      </ContentArea>
    </Container>
  )
})

async function getStatus(): Promise<Result<Status>> {
  return client
    .get<JsonOf<Status>>('/ai/status')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

interface Status {
  running: boolean
  generations: Generation[]
}

interface Generation {
  generation: number
  value: {
    maxCapacityPercentage: number
    minCapacityPercentage: number
    childrenInFirstPreferencePercentage: number
    childrenInOneOfPreferencesPercentage: number
    childrenInOneOfPreferencesWhen3GivenPercentage: number
  }
  cost: number
}

function getGraphOptions(): ChartOptions<'line'> {
  return {
    scales: {
      x: {
        type: 'linear',
        min: 1,
        suggestedMax: 200
      },
      y: {
        min: 0,
        max: 1.3,
        ticks: {
          maxTicksLimit: 10,
          callback: function (value) {
            return `${String(100 * Number(value))} %`
          }
        }
      }
    },
    plugins: {
      legend: {
        display: true
      }
    }
  }
}
