// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { storiesOf } from '@storybook/react'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { range } from 'lodash'
import React from 'react'
import { ExpandableList } from './ExpandableList'

storiesOf('evaka/atoms/ExpandableList', module).add('all', () => {
  function Story({ lorems, rows }: { lorems: number; rows: number }) {
    return (
      <>
        <H3>
          {lorems} lorems, rows {rows}
        </H3>
        <ExpandableList rowsToOccupy={rows} i18n={{ others: 'muuta' }}>
          {range(lorems).map((i) => (
            <div key={i}>Lorem ipsum</div>
          ))}
        </ExpandableList>
        <Gap size={'m'} />
      </>
    )
  }

  return (
    <>
      <Story lorems={0} rows={3} />
      <Story lorems={3} rows={3} />
      <Story lorems={4} rows={3} />
      <Story lorems={10} rows={3} />
    </>
  )
})
