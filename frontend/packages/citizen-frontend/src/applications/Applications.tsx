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
import { faPlusCircle } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

const TitleContainer = styled.div``

const NewApplicationLink = styled.div`
  & a {
    color: ${colors.blues.primary};
    text-decoration: none;
  }
`

const Icon = styled(FontAwesomeIcon)`
  height: 1rem !important;
  width: 1rem !important;
  margin-right: 10px;
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
        <TitleContainer>
          <H1 noMargin>{t.decisions.title}</H1>
          <NewApplicationLink>
            <a href={`/citizen/applications/new`}>
              <Icon icon={faPlusCircle} color={colors.blues.primary} />{' '}
              {t.applicationsList.confirmationLink}
            </a>
          </NewApplicationLink>
        </TitleContainer>
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
