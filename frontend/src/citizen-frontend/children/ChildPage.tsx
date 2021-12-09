// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useParams } from 'react-router-dom'

export default React.memo(function Page() {
  const { childId } = useParams<{ childId: string }>()

  return <div>Lapsi {childId}</div>
})
