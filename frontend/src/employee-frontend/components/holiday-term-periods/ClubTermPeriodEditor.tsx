// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine, Failure, Result, Success } from 'lib-common/api'
import { ClubTerm } from 'lib-common/generated/api-types/daycare'
import { useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import ClubTermForm from './ClubTermForm'
import { clubTermsQuery } from './queries'

export default React.memo(function ClubTermPeriodEditor() {
  const navigate = useNavigate()
  const { termId } = useRequiredParams('termId')

  const clubTerms = useQueryResult(clubTermsQuery())

  const navigateToList = useCallback(
    () => navigate('/holiday-periods'),
    [navigate]
  )

  const term: Result<ClubTerm | undefined> = useMemo(() => {
    if (termId === 'new') return Success.of(undefined)

    return clubTerms
      .map((allTerms) => allTerms.find((t) => t.id === termId))
      .chain((t) =>
        t ? Success.of(t) : Failure.of({ message: 'Term not found' })
      )
  }, [termId, clubTerms])

  return (
    <Container>
      <ContentArea opaque>
        {renderResult(combine(clubTerms, term), ([clubTerms, term]) => (
          <ClubTermForm
            clubTerm={term}
            allTerms={clubTerms}
            onSuccess={navigateToList}
            onCancel={navigateToList}
          />
        ))}
      </ContentArea>
    </Container>
  )
})
