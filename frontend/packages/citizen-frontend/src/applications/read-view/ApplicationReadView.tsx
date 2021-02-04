// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { ApplicationDetails } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { getApplication } from '~applications/api'
import Container from '@evaka/lib-components/src/layout/Container'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import { apiDataToFormData } from '~applications/editor/ApplicationFormData'
import { useUser } from '~auth'
import ApplicationReadViewDaycare from '~applications/read-view/ApplicationReadViewDaycare'
import Footer from '~Footer'

export default React.memo(function ApplicationReadView() {
  const { applicationId } = useParams<{ applicationId: string }>()
  const user = useUser()

  const [apiData, setApiData] = useState<Result<ApplicationDetails>>(
    Loading.of()
  )

  const loadApplication = useRestApi(getApplication, setApiData)
  useEffect(() => {
    loadApplication(applicationId)
  }, [applicationId])

  return (
    <>
      <Container>
        {apiData.isLoading && <SpinnerSegment />}
        {apiData.isFailure && <ErrorSegment />}
        {apiData.isSuccess && (
          <>
            {apiData.value.type === 'DAYCARE' ? (
              <ApplicationReadViewDaycare
                application={apiData.value}
                formData={apiDataToFormData(apiData.value, user)}
              />
            ) : apiData.value.type === 'PRESCHOOL' ? (
              <ErrorSegment title={'Hakemustyyppiä ei ole vielä toteutettu'} />
            ) : apiData.value.type === 'CLUB' ? (
              <ErrorSegment title={'Hakemustyyppiä ei ole vielä toteutettu'} />
            ) : (
              <ErrorSegment title={'Tuntematon hakemustyyppi'} />
            )}
          </>
        )}
      </Container>
      <Footer />
    </>
  )
})
