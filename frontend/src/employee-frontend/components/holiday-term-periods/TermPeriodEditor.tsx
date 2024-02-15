// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

import { PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import PreschoolTermForm from './PreschoolTermForm'
import { preschoolTermsQuery } from './queries'

export default React.memo(function TermPeriodEditor() {
  const navigate = useNavigate()
  const { termId } = useNonNullableParams<{ termId: string }>()

  const preschoolTerms = useQueryResult(preschoolTermsQuery())

  const navigateToList = useCallback(
    () => navigate('/holiday-periods'),
    [navigate]
  )

  const findPreschoolTermById = (
    id: string,
    allPreschoolTerms: PreschoolTerm[]
  ) =>
    termId !== 'new'
      ? allPreschoolTerms.find((term) => term.id === termId)
      : undefined

  return (
    <Container>
      <ContentArea opaque>
        {renderResult(preschoolTerms, (preschoolTerms) => (
          <PreschoolTermForm
            term={findPreschoolTermById(termId, preschoolTerms)}
            allTerms={preschoolTerms}
            onSuccess={navigateToList}
            onCancel={navigateToList}
          />
        ))}
      </ContentArea>
    </Container>
  )
})
