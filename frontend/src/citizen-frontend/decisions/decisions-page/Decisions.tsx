// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { ApplicationDecisions } from '../../decisions/types'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../../localization'
import { getDecisions } from '../api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Loading, Result } from 'lib-common/api'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import ApplicationDecisionsBlock from '../../decisions/decisions-page/ApplicationDecisionsBlock'
import _ from 'lodash'
import Footer from '../../Footer'
import useTitle from '../../useTitle'

export default React.memo(function Decisions() {
  const t = useTranslation()
  const [applicationDecisions, setApplicationDecisions] = useState<
    Result<ApplicationDecisions[]>
  >(Loading.of())

  const loadDecisions = useRestApi(getDecisions, setApplicationDecisions)
  useEffect(() => {
    loadDecisions()
  }, [loadDecisions])

  useTitle(t, t.decisions.title)

  const unconfirmedDecisionsCount = applicationDecisions.isSuccess
    ? applicationDecisions.value.reduce(
        (sum, { decisions }) =>
          sum + decisions.filter(({ status }) => status === 'PENDING').length,
        0
      )
    : 0

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.decisions.title}</H1>
          <Gap size="xs" />
          {t.decisions.summary()}
          {unconfirmedDecisionsCount > 0 && (
            <>
              <Gap size="s" />
              <AlertBox
                message={t.decisions.unconfirmedDecisions(
                  unconfirmedDecisionsCount
                )}
                data-qa="alert-box-unconfirmed-decisions-count"
              />
            </>
          )}
        </ContentArea>
        <Gap size="s" />

        {applicationDecisions.isLoading && <SpinnerSegment />}
        {applicationDecisions.isFailure && (
          <ErrorSegment title={t.decisions.pageLoadError} />
        )}
        {applicationDecisions.isSuccess && (
          <>
            {_.sortBy(applicationDecisions.value, (d) => d.childName).map(
              (applicationDecision) => (
                <React.Fragment key={applicationDecision.applicationId}>
                  <ApplicationDecisionsBlock {...applicationDecision} />
                  <Gap size="s" />
                </React.Fragment>
              )
            )}
          </>
        )}
      </Container>
      <Footer />
    </>
  )
})
