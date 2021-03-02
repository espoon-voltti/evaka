// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { ApplicationDecisions } from '../../decisions/types'
import Container, {
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { H1 } from '@evaka/lib-components/src/typography'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'
import { Gap } from '@evaka/lib-components/src/white-space'
import { useTranslation } from '../../localization'
import { getDecisions } from '../api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
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
