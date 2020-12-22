// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { format, startOfMonth, subMonths } from 'date-fns'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import Title from '@evaka/lib-components/src/atoms/Title'
import { generateFeeDecisions } from '../../../api/invoicing'
import styled from 'styled-components'

const Form = styled.div`
  display: flex;
  flex-direction: column;
`

function GeneratorButton(props: { reload: () => void }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)
  const [date, setDate] = useState(
    format(startOfMonth(subMonths(new Date(), 1)), 'yyyy-MM-dd')
  )
  const [targetHeads, setTargetHeads] = useState<string[]>([])

  return (
    <ContentArea opaque>
      <Title size={3}>Luonnosten generointi</Title>
      <Form>
        <label>Päämiehet (erottimena \s tai ,)</label>
        <textarea
          onChange={(e) => setTargetHeads(e.target.value.split(/[\s,]+/))}
          value={targetHeads}
          style={{ marginBottom: '1rem' }}
        />

        <label>Alkaen pvm</label>
        <input
          value={date}
          onChange={(e) => setDate(e.target.value)}
          style={{ marginBottom: '1rem' }}
        />

        <button
          disabled={loading}
          onClick={async () => {
            setLoading(true)
            try {
              await generateFeeDecisions(date, targetHeads)
              setError(false)
              props.reload()
            } catch (e) {
              console.error(e)
              setError(true)
            }
            setLoading(false)
          }}
          style={{ fontSize: '1rem', padding: '10px' }}
        >
          LUO MAKSUPÄÄTÖSLUONNOKSET
        </button>
        {error ? (
          <div style={{ color: 'red', textAlign: 'center', margin: '20px' }}>
            Maksupäätösten generointi epäonnistui
          </div>
        ) : null}
      </Form>
    </ContentArea>
  )
}

export default GeneratorButton
