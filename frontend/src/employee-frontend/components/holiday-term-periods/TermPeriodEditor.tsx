// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

import { useQueryResult } from 'lib-common/query'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import PreschoolTermForm from './PreschoolTermForm'
import { preschoolTermsQuery } from './queries'

export default React.memo(function TermPeriodEditor() {
  const navigate = useNavigate()

  const preschoolTerms = useQueryResult(preschoolTermsQuery())

  const navigateToList = useCallback(
    () => navigate('/holiday-periods'),
    [navigate]
  )

  return (
    <Container>
      <ContentArea opaque>
        {renderResult(preschoolTerms, (preschoolTerms) => (
          <PreschoolTermForm
            allTerms={preschoolTerms}
            onSuccess={navigateToList}
            onCancel={navigateToList}
          />
        ))}
      </ContentArea>
    </Container>
  )
})
