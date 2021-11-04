// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useParams } from 'react-router-dom'
import { useApiState } from 'lib-common/utils/useRestApi'
import { getApplication } from '../api'
import Container from 'lib-components/layout/Container'
import { apiDataToFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { useUser } from '../../auth/state'
import { useTranslation } from '../../localization'
import Footer from '../../Footer'
import useTitle from '../../useTitle'
import ApplicationReadViewContents from '../../applications/read-view/ApplicationReadViewContents'
import { renderResult } from '../../async-rendering'
import { UUID } from 'lib-common/types'

export default React.memo(function ApplicationReadView() {
  const { applicationId } = useParams<{ applicationId: UUID }>()
  const t = useTranslation()
  const user = useUser()
  const [apiData] = useApiState(
    () => getApplication(applicationId),
    [applicationId]
  )

  useTitle(
    t,
    apiData
      .map(({ type }) => t.applications.editor.heading.title[type])
      .getOrElse('')
  )

  return (
    <>
      <Container>
        {renderResult(apiData, (value) => (
          <ApplicationReadViewContents
            application={value}
            formData={apiDataToFormData(value, user)}
          />
        ))}
      </Container>
      <Footer />
    </>
  )
})
