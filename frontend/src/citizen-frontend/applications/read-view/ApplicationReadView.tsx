// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Loading, Result } from 'lib-common/api'
import { ApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { getApplication } from '../../applications/api'
import Container from 'lib-components/layout/Container'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { apiDataToFormData } from '../../applications/editor/ApplicationFormData'
import { useUser } from '../../auth'
import { useTranslation } from '../../localization'
import Footer from '../../Footer'
import useTitle from '../../useTitle'
import ApplicationReadViewContents from '../../applications/read-view/ApplicationReadViewContents'

export default React.memo(function ApplicationReadView() {
  const { applicationId } = useParams<{ applicationId: string }>()
  const t = useTranslation()
  const user = useUser()

  const [apiData, setApiData] = useState<Result<ApplicationDetails>>(
    Loading.of()
  )

  const loadApplication = useRestApi(getApplication, setApiData)
  useEffect(() => {
    loadApplication(applicationId)
  }, [applicationId])

  useTitle(
    t,
    apiData
      .map(({ type }) => t.applications.editor.heading.title[type])
      .getOrElse(''),
    [apiData]
  )

  return (
    <>
      <Container>
        {apiData.isLoading && <SpinnerSegment />}
        {apiData.isFailure && <ErrorSegment />}
        {apiData.isSuccess && (
          <ApplicationReadViewContents
            application={apiData.value}
            formData={apiDataToFormData(apiData.value, user)}
          />
        )}
      </Container>
      <Footer />
    </>
  )
})
