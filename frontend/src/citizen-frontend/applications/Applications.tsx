// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import sortBy from 'lodash/sortBy'
import React, { Fragment } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { isLoading } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { fontWeights, H1, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { farMap } from 'lib-icons'

import ChildApplicationsBlock from '../applications/ChildApplicationsBlock'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'

import { guardianApplicationsQuery } from './queries'

export default React.memo(function Applications() {
  const t = useTranslation()
  const guardianApplications = useQueryResult(guardianApplicationsQuery())

  useTitle(t, t.applicationsList.title)

  return (
    <Container
      data-qa="applications-list"
      data-isloading={isLoading(guardianApplications)}
    >
      <Gap size="s" />
      <ContentArea opaque paddingVertical="L">
        <H1 noMargin>{t.applicationsList.title}</H1>
        {t.applicationsList.summary}
        <P noMargin>{t.loginPage.applying.mapText}</P>
        <Gap size="xs" />
        <MapLink to="/map">
          <FontAwesomeIcon icon={farMap} />
          <Gap size="xs" horizontal />
          {t.loginPage.applying.mapLink}
        </MapLink>
      </ContentArea>
      <Gap size="s" />

      {renderResult(guardianApplications, (guardianApplications) => (
        <>
          {sortBy(guardianApplications, (a) => a.childName).map(
            (childApplications) =>
              childApplications.duplicateOf === null && (
                <Fragment key={childApplications.childId}>
                  <ChildApplicationsBlock data={childApplications} />
                  <Gap size="s" />
                </Fragment>
              )
          )}
        </>
      ))}
    </Container>
  )
})

const MapLink = styled(Link)`
  font-weight: ${fontWeights.semibold};
`
