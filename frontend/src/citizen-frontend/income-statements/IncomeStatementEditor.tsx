// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation } from 'wouter'

import type { Result } from 'lib-common/api'
import { combine, Loading } from 'lib-common/api'
import type { IncomeStatementStatus } from 'lib-common/generated/api-types/incomestatement'
import type { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import { fromBody } from 'lib-common/income-statements/body'
import * as Form from 'lib-common/income-statements/form'
import { emptyIncomeStatementForm } from 'lib-common/income-statements/form'
import type LocalDate from 'lib-common/local-date'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import { scrollToElement } from 'lib-common/utils/scrolling'
import Main from 'lib-components/atoms/Main'

import { renderResult } from '../async-rendering'

import IncomeStatementForm from './IncomeStatementForm'
import {
  createIncomeStatementMutation,
  incomeStatementQuery,
  incomeStatementStartDatesQuery,
  updateIncomeStatementMutation
} from './queries'

interface EditorState {
  id: string | undefined
  status: IncomeStatementStatus
  startDates: LocalDate[]
  formData: Form.IncomeStatementForm
}

function useInitialEditorState(
  id: IncomeStatementId | undefined
): Result<EditorState> {
  const incomeStatement = useQueryResult(
    id ? incomeStatementQuery({ incomeStatementId: id }) : constantQuery(null)
  )
  const startDates = useQueryResult(incomeStatementStartDatesQuery())

  return combine(incomeStatement, startDates).map(
    ([incomeStatement, startDates]) => ({
      id,
      status: incomeStatement?.status ?? 'DRAFT',
      startDates,
      formData:
        incomeStatement === null
          ? emptyIncomeStatementForm
          : Form.fromIncomeStatement(incomeStatement)
    })
  )
}

export type ErrorDisplayType = 'NONE' | 'DRAFT' | 'SAVE'

export default React.memo(function IncomeStatementEditor() {
  const params = useRouteParams(['incomeStatementId'])
  const [, navigate] = useLocation()
  const incomeStatementId =
    params.incomeStatementId === 'new'
      ? undefined
      : fromUuid<IncomeStatementId>(params.incomeStatementId)

  const [state, setState] = useState<Result<EditorState>>(Loading.of())
  const initialEditorState = useInitialEditorState(incomeStatementId)
  if (
    state.isLoading &&
    initialEditorState.isSuccess &&
    !initialEditorState.isReloading
  ) {
    setState(initialEditorState)
  }

  const [showFormErrors, setShowFormErrors] = useState<ErrorDisplayType>('NONE')
  const [shouldScrollToError, setShouldScrollToError] = useState(false)

  const navigateToList = useCallback(() => {
    navigate('/income')
  }, [navigate])

  const updateFormData = useCallback(
    (fn: (prev: Form.IncomeStatementForm) => Form.IncomeStatementForm): void =>
      setState((prev) =>
        prev.map((state) => ({ ...state, formData: fn(state.formData) }))
      ),
    []
  )

  const { mutateAsync: createIncomeStatement } = useMutationResult(
    createIncomeStatementMutation
  )
  const { mutateAsync: updateIncomeStatement } = useMutationResult(
    updateIncomeStatementMutation
  )

  const draftBody = useMemo(
    () => state.map((state) => fromBody('adult', state.formData, true)),
    [state]
  )

  const validatedBody = useMemo(
    () => state.map((state) => fromBody('adult', state.formData, false)),
    [state]
  )

  useEffect(() => {
    if (shouldScrollToError) {
      const firstInvalidElement = document.querySelector(
        '[aria-invalid="true"]'
      )
      if (firstInvalidElement instanceof HTMLElement) {
        scrollToElement(firstInvalidElement, 0, 'center')
        firstInvalidElement.focus()
      }
      setShouldScrollToError(false)
    }
  }, [shouldScrollToError])

  if (
    (showFormErrors === 'SAVE' &&
      validatedBody.isSuccess &&
      validatedBody.value) ||
    (showFormErrors === 'DRAFT' && draftBody.isSuccess && draftBody.value)
  ) {
    setShowFormErrors('NONE')
  }

  return renderResult(
    combine(state, draftBody, validatedBody),
    ([{ status, formData, startDates }, draftBody, validatedBody]) => {
      if (status !== 'DRAFT') {
        navigateToList()
        return null
      }

      const save = (draft: boolean) => {
        const body = draft ? draftBody : validatedBody
        if (body) {
          if (incomeStatementId) {
            return updateIncomeStatement({ incomeStatementId, body, draft })
          } else {
            return createIncomeStatement({ body, draft })
          }
        } else {
          setShowFormErrors(draft ? 'DRAFT' : 'SAVE')
          setShouldScrollToError(true)
          return
        }
      }

      return (
        <Main>
          <IncomeStatementForm
            incomeStatementId={incomeStatementId}
            status={status}
            formData={formData}
            showFormErrors={showFormErrors}
            otherStartDates={startDates}
            onChange={updateFormData}
            onSave={save}
            onSuccess={navigateToList}
            onCancel={navigateToList}
          />
        </Main>
      )
    }
  )
})
