// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { Fragment } from 'react'

import { isLoading } from 'lib-common/api'
import { useApiState } from 'lib-common/utils/useRestApi'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import ChildApplicationsBlock from '../applications/ChildApplicationsBlock'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'

import { getGuardianApplications } from './api'

export default React.memo(function Applications() {
  const t = useTranslation()
  const [guardianApplications, loadGuardianApplications] = useApiState(
    getGuardianApplications,
    []
  )

  useTitle(t, t.applicationsList.title)

  return (
    <>
      <Container
        data-qa="applications-list"
        data-isloading={isLoading(guardianApplications)}
      >
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.applicationsList.title}</H1>
          {t.applicationsList.summary}
        </ContentArea>
        <Gap size="s" />

        {renderResult(guardianApplications, (guardianApplications) => (
          <>
            {sortBy(guardianApplications, (a) => a.childName).map(
              (childApplications) => (
                <Fragment key={childApplications.childId}>
                  <ChildApplicationsBlock
                    childId={childApplications.childId}
                    childName={childApplications.childName}
                    applicationSummaries={
                      childApplications.applicationSummaries
                    }
                    reload={loadGuardianApplications}
                  />
                  <Gap size="s" />
                </Fragment>
              )
            )}
          </>
        ))}
      </Container>
      <Footer />
    </>
  )
})
