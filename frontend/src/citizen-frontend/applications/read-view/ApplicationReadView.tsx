// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { combine } from 'lib-common/api'
import { apiDataToFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Container from 'lib-components/layout/Container'

import Footer from '../../Footer'
import ApplicationReadViewContents from '../../applications/read-view/ApplicationReadViewContents'
import { renderResult } from '../../async-rendering'
import { useTranslation } from '../../localization'
import useTitle from '../../useTitle'
import { applicationChildrenQuery, applicationQuery } from '../queries'

export default React.memo(function ApplicationReadView() {
  const { applicationId } = useRouteParams(['applicationId'])
  const t = useTranslation()
  const application = useQueryResult(applicationQuery(applicationId))
  const children = useQueryResult(applicationChildrenQuery())

  useTitle(
    t,
    application
      .map(({ type }) => t.applications.editor.heading.title[type])
      .getOrElse('')
  )

  return (
    <>
      <Container>
        {renderResult(combine(application, children), ([apiData, children]) => (
          <ApplicationReadViewContents
            application={apiData}
            formData={apiDataToFormData(apiData, children)}
          />
        ))}
      </Container>
      <Footer />
    </>
  )
})
