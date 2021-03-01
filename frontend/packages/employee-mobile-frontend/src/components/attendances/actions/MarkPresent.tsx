// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useHistory } from 'react-router-dom'

import { faArrowLeft } from '@evaka/lib-icons'

import { BackButton, TallContentArea } from '~components/mobile/components'

export default React.memo(function MarkPresent() {
  const history = useHistory()

  return (
    <>
      <TallContentArea opaque={false}>
        <BackButton onClick={() => history.goBack()} icon={faArrowLeft} />
      </TallContentArea>
    </>
  )
})
