// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useTranslation } from '~localization'
import React, { useEffect, useState } from 'react'
import { GuardianApplications } from '~applications/types'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import Container, {
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { getGuardianApplications } from './api'
import { Gap } from '@evaka/lib-components/src/white-space'
import { H1, P } from '@evaka/lib-components/src/typography'
import { SpinnerSegment } from '../../../lib-components/src/atoms/state/Spinner'
import ErrorSegment from '../../../lib-components/src/atoms/state/ErrorSegment'
import _ from 'lodash'
import ChildApplicationsBlock from '~applications/ChildApplicationsBlock'
import styled from 'styled-components'

const MobileFriendlyH1 = styled(H1)`
  @media (max-width: 600px) {
    font-size: 24px;
    font-weight: 600;
    line-height: 36px;
  }
`

export default React.memo(function Applications() {
  const t = useTranslation()
  const [guardianApplications, setGuardianApplications] = useState<
    Result<GuardianApplications[]>
  >(Loading.of())

  const loadGuardianApplications = useRestApi(
    getGuardianApplications,
    setGuardianApplications
  )

  useEffect(() => {
    loadGuardianApplications()
  }, [loadGuardianApplications])

  return (
    <Container>
      <Gap size="s" />
      <ContentArea opaque paddingVertical="L">
        <MobileFriendlyH1 noMargin>{t.applicationsList.title}</MobileFriendlyH1>
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
      {guardianApplications.isSuccess && (
        <>
          {_.sortBy(guardianApplications.value, (a) => a.childName).map(
            (childApplications) => (
              <React.Fragment key={childApplications.childId}>
                <ChildApplicationsBlock {...childApplications} />
                <Gap size="s" />
              </React.Fragment>
            )
          )}
        </>
      )}
    </Container>
  )
})
