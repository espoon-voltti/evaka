// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useTranslation } from '~localization'
import React, { useEffect, useState } from 'react'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import Container, {
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { getGuardianApplications } from './api'
import { Gap } from '@evaka/lib-components/src/white-space'
import { H1, P } from '@evaka/lib-components/src/typography'
import _ from 'lodash'
import ChildApplicationsBlock from '~applications/ChildApplicationsBlock'
import { ApplicationsOfChild } from '@evaka/lib-common/src/api-types/application/ApplicationsOfChild'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import Footer from '~Footer'

export default React.memo(function Applications() {
  const t = useTranslation()
  const [guardianApplications, setGuardianApplications] = useState<
    Result<ApplicationsOfChild[]>
  >(Loading.of())

  const loadGuardianApplications = useRestApi(
    getGuardianApplications,
    setGuardianApplications
  )

  useEffect(() => {
    loadGuardianApplications()
  }, [loadGuardianApplications])

  useEffect(() => {
    document.title = t.applicationsList.title
    return () => {
      document.title = t.common.title
    }
  }, [])

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.applicationsList.title}</H1>
          <P
            width="800px"
            dangerouslySetInnerHTML={{ __html: t.applicationsList.summary }}
          />
        </ContentArea>
        <Gap size="s" />

        {guardianApplications.isLoading && <SpinnerSegment />}
        {guardianApplications.isFailure && (
          <ErrorSegment title={t.applicationsList.pageLoadError} />
        )}
        {guardianApplications.isSuccess &&
          _.sortBy(guardianApplications.value, (a) => a.childName).map(
            (childApplications) => (
              <React.Fragment key={childApplications.childId}>
                <ChildApplicationsBlock
                  childId={childApplications.childId}
                  childName={childApplications.childName}
                  applicationSummaries={childApplications.applicationSummaries}
                  reload={loadGuardianApplications}
                />
                <Gap size="s" />
              </React.Fragment>
            )
          )}
      </Container>
      <Footer />
    </>
  )
})
