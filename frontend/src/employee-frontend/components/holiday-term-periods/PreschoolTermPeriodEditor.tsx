// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine, Failure, Result, Success } from 'lib-common/api'
import { PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import PreschoolTermForm from './PreschoolTermForm'
import { preschoolTermsQuery } from './queries'

export default React.memo(function PreschoolTermPeriodEditor() {
  const navigate = useNavigate()
  const { termId } = useRequiredParams('termId')

  const preschoolTerms = useQueryResult(preschoolTermsQuery())

  const navigateToList = useCallback(
    () => navigate('/holiday-periods'),
    [navigate]
  )

  const term: Result<PreschoolTerm | undefined> = useMemo(() => {
    if (termId === 'new') return Success.of(undefined)

    return preschoolTerms
      .map((allTerms) => allTerms.find((t) => t.id === termId))
      .chain((t) =>
        t ? Success.of(t) : Failure.of({ message: 'Term not found' })
      )
  }, [termId, preschoolTerms])

  return (
    <Container>
      <ContentArea opaque>
        {renderResult(
          combine(preschoolTerms, term),
          ([preschoolTerms, term]) => (
            <PreschoolTermForm
              term={term}
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
