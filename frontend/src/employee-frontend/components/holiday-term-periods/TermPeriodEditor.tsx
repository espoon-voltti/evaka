// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import PreschoolTermForm from './PreschoolTermForm'
import { preschoolTermQuery, preschoolTermsQuery } from './queries'

export default React.memo(function TermPeriodEditor() {
  const navigate = useNavigate()
  const { termId } = useNonNullableParams<{ termId: string }>()

  const preschoolTerms = useQueryResult(preschoolTermsQuery())

  const preschoolTerm = useQueryResult(preschoolTermQuery(termId), {
    enabled: termId !== 'new'
  })

  const navigateToList = useCallback(
    () => navigate('/holiday-periods'),
    [navigate]
  )

  return (
    <Container>
      <ContentArea opaque>
        {termId === 'new'
          ? renderResult(preschoolTerms, (preschoolTerms) => (
              <PreschoolTermForm
                allTerms={preschoolTerms}
                onSuccess={navigateToList}
                onCancel={navigateToList}
              />
            ))
          : renderResult(
              combine(preschoolTerms, preschoolTerm),
              ([preschoolTerms, preschoolTerm]) => (
                <PreschoolTermForm
                  term={preschoolTerm}
                  allTerms={preschoolTerms}
                  onSuccess={navigateToList}
                  onCancel={navigateToList}
                />
              )
            )}
      </ContentArea>
    </Container>
  )
})
