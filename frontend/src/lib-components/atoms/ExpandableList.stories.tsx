// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { storiesOf } from '@storybook/react'
import { Gap } from 'lib-components/white-space'
import { range } from 'lodash'
import React from 'react'
import { ExpandableList } from './ExpandableList'

storiesOf('evaka/atoms/ExpandableList', module).add('all', () => {
  function Story({ lorems, show }: { lorems: number; show: number }) {
    return (
      <ExpandableList show={show} i18n={{ others: 'muuta' }}>
        {range(lorems).map((i) => (
          <div key={i}>Lorem ipsum</div>
        ))}
      </ExpandableList>
    )
  }

  return (
    <>
      <Story lorems={0} show={3} />
      <Gap size={'m'} />
      <Story lorems={3} show={3} />
      <Gap size={'m'} />
      <Story lorems={4} show={3} />
      <Gap size={'m'} />
      <Story lorems={10} show={3} />
    </>
  )
})
