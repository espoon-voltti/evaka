// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { storiesOf } from '@storybook/react'
import Pagination from '@evaka/lib-components/src/Pagination'

storiesOf('evaka/atoms/pagination', module)
  .add('Basic', () => {
    function Parent() {
      const [page, setPage] = useState(1)
      return (
        <Pagination
          pages={5}
          currentPage={page}
          setPage={(page) => setPage(page)}
          label="Page"
        />
      )
    }

    return <Parent />
  })
  .add('more than fits', () => {
    function Parent() {
      const [page, setPage] = useState(1)
      return (
        <Pagination
          pages={20}
          currentPage={page}
          setPage={(page) => setPage(page)}
          label="Page"
        />
      )
    }

    return <Parent />
  })
