// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import Container from '@evaka/lib-components/src/layout/Container'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import DaycareApplicationEditor from '~applications/editor/DaycareApplicationEditor'
import { getApplication } from '~applications/api'
import { Application } from '~applications/types'

export default React.memo(function ApplicationEditor() {
  const { applicationId } = useParams<{ applicationId: string }>()
  const [apiData, setApiData] = useState<Result<Application>>(Loading.of())

  const loadApplication = useRestApi(getApplication, setApiData)
  useEffect(() => {
    loadApplication(applicationId)
  }, [applicationId])

  return (
    <Container>
      {apiData.isLoading && <SpinnerSegment />}
      {apiData.isFailure && <ErrorSegment />}
      {apiData.isSuccess && (
        <>
          {apiData.value.type === 'daycare' ? (
            <DaycareApplicationEditor apiData={apiData.value} />
          ) : apiData.value.type === 'preschool' ? (
            <ErrorSegment title={'Hakemustyyppiä ei ole vielä toteutettu'} />
          ) : apiData.value.type === 'club' ? (
            <ErrorSegment title={'Hakemustyyppiä ei ole vielä toteutettu'} />
          ) : (
            <ErrorSegment title={'Tuntematon hakemustyyppi'} />
          )}
        </>
      )}
    </Container>
  )
})
