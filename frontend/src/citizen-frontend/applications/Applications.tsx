// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useTranslation } from '../localization'
import React, { Fragment } from 'react'
import { useApiState } from 'lib-common/utils/useRestApi'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { getGuardianApplications } from './api'
import { Gap } from 'lib-components/white-space'
import { H1 } from 'lib-components/typography'
import _ from 'lodash'
import ChildApplicationsBlock from '../applications/ChildApplicationsBlock'
import Footer from '../Footer'
import useTitle from '../useTitle'
import { renderResult } from '../async-rendering'

export default React.memo(function Applications() {
  const t = useTranslation()
  const [guardianApplications, loadGuardianApplications] = useApiState(
    getGuardianApplications
  )

  useTitle(t, t.applicationsList.title)

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.applicationsList.title}</H1>
          {t.applicationsList.summary}
        </ContentArea>
        <Gap size="s" />

        {renderResult(guardianApplications, (guardianApplications) => (
          <>
            {_.sortBy(guardianApplications, (a) => a.childName).map(
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
