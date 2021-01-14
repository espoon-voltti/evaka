// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Loading, Result } from '@evaka/lib-common/src/api'
import DaycareApplicationEditor from '~applications/editor/DaycareApplicationEditor'
import { getApplication } from '~applications/api'
import { Application } from '~applications/types'

export default React.memo(function ApplicationEditor() {
  const { applicationId } = useParams<{ applicationId: string }>()
  const [application, setApplication] = useState<Result<Application>>(
    Loading.of()
  )

  useEffect(() => {
    void getApplication(applicationId).then(setApplication)
  }, [])

  console.log(application)

  return <DaycareApplicationEditor />
})
