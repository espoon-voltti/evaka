import React, { useEffect, useState } from 'react'
import { H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { client } from '../../api/client'
import { Failure, Loading, Result, Success } from '../../../lib-common/api'
import { JsonOf } from '../../../lib-common/json'
import ErrorSegment from '../../../lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from '../../../lib-components/atoms/state/Spinner'
import Button from '../../../lib-components/atoms/buttons/Button'
import { Line } from 'react-chartjs-2'
import colors from '../../../lib-customizations/common'

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

  const datasets = [
    {
      label: 'childrenInFirstPreferencePercentage',
      data: status.isSuccess
        ? status.value.generations.map((g) => ({
            x: g.generation,
            y: g.value.childrenInFirstPreferencePercentage / 100
          }))
        : [],
      steppedLine: false,
      fill: false,
      pointBackgroundColor: colors.accents.red,
      borderColor: colors.accents.red
    },
    {
      label: 'childrenInOneOfPreferencesPercentage',
      data: status.isSuccess
        ? status.value.generations.map((g) => ({
            x: g.generation,
            y: g.value.childrenInOneOfPreferencesPercentage / 100
          }))
        : [],
      steppedLine: false,
      fill: false,
      pointBackgroundColor: colors.accents.orange,
      borderColor: colors.accents.orange
    },
    {
      label: 'maxCapacityPercentage',
      data: status.isSuccess
        ? status.value.generations.map((g) => ({
            x: g.generation,
            y: g.value.maxCapacityPercentage / 100
          }))
        : [],
      steppedLine: false,
      fill: false,
      pointBackgroundColor: colors.brandEspoo.espooBlue,
      borderColor: colors.brandEspoo.espooBlue
    }
  ]

  return (
    <Container verticalMargin={defaultMargins.L}>
      <ContentArea opaque>
        <H1>AI Test</H1>
        {status.isFailure && <ErrorSegment />}
        {status.isLoading && <SpinnerSegment />}
        {status.isSuccess && (
          <div>
            {status.value.running ? (
              <Button text={'Lopeta'} primary onClick={stop} />
            ) : (
              <Button text={'Käynnistä'} primary onClick={start} />
            )}

            <Gap />

            <div style={{ height: '400px', width: '800px' }}>
              <Line data={{ datasets }} options={getGraphOptions()} redraw />
            </div>

            <Gap />

            <div>
              cost:{' '}
              {`${
                status.value.generations.length > 0
                  ? status.value.generations[
                      status.value.generations.length - 1
                    ].cost
                  : '-'
              }`}
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

function getGraphOptions() {
  return {
    scales: {
      xAxes: [
        {
          type: 'linear',
          ticks: {
            min: 1,
            suggestedMax: 200
          }
        }
      ],
      yAxes: [
        {
          ticks: {
            min: 0,
            max: 1.3,
            maxTicksLimit: 10,
            callback: function (value: unknown) {
              return `${String(100 * Number(value))} %`
            }
          }
        }
      ]
    },
    legend: {
      display: true
    }
  }
}
