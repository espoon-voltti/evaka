// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router'

import { combine, Loading, Result } from 'lib-common/api'
import { IncomeStatementStatus } from 'lib-common/generated/api-types/incomestatement'
import LocalDate from 'lib-common/local-date'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import Main from 'lib-components/atoms/Main'

import { renderResult } from '../async-rendering'

import ChildIncomeStatementForm from './ChildIncomeStatementForm'
import { IncomeStatementFormAPI } from './IncomeStatementComponents'
import {
  childIncomeStatementQuery,
  childIncomeStatementStartDatesQuery,
  createChildIncomeStatementMutation,
  updateChildIncomeStatementMutation
} from './queries'
import { fromBody } from './types/body'
import * as Form from './types/form'
import { emptyIncomeStatementForm } from './types/form'

interface EditorState {
  id: string | undefined
  status: IncomeStatementStatus
  startDates: LocalDate[]
  formData: Form.IncomeStatementForm
}

function useInitialEditorState(
  childId: UUID,
  id: UUID | undefined
): Result<EditorState> {
  const incomeStatement = useQueryResult(
    id
      ? childIncomeStatementQuery({ childId, incomeStatementId: id })
      : constantQuery(null)
  )
  const startDates = useQueryResult(
    childIncomeStatementStartDatesQuery({ childId })
  )

  return combine(incomeStatement, startDates).map(
    ([incomeStatement, startDates]) => ({
      id,
      status: incomeStatement?.status ?? 'DRAFT',
      startDates,
      formData:
        incomeStatement === null
          ? {
              ...emptyIncomeStatementForm,
              childIncome: true,
              highestFee: false
            }
          : Form.fromIncomeStatement(incomeStatement)
    })
  )
}

export default React.memo(function ChildIncomeStatementEditor() {
  const params = useRouteParams(['incomeStatementId', 'childId'])
  const navigate = useNavigate()
  const incomeStatementId =
    params.incomeStatementId === 'new' ? undefined : params.incomeStatementId

  const childId = params.childId

  const [state, setState] = useState<Result<EditorState>>(Loading.of())
  const initialEditorState = useInitialEditorState(childId, incomeStatementId)
  if (
    state.isLoading &&
    initialEditorState.isSuccess &&
    !initialEditorState.isReloading
  ) {
    setState(initialEditorState)
  }

  const [showFormErrors, setShowFormErrors] = useState(false)

  const navigateToList = useCallback(() => {
    void navigate('/income')
  }, [navigate])

  const form = useRef<IncomeStatementFormAPI | null>(null)

  const updateFormData = useCallback(
    (fn: (prev: Form.IncomeStatementForm) => Form.IncomeStatementForm): void =>
      setState((prev) =>
        prev.map((state) => ({ ...state, formData: fn(state.formData) }))
      ),
    []
  )

  const { mutateAsync: createChildIncomeStatement } = useMutationResult(
    createChildIncomeStatementMutation
  )
  const { mutateAsync: updateChildIncomeStatement } = useMutationResult(
    updateChildIncomeStatementMutation
  )

  const draftBody = useMemo(
    () => state.map((state) => fromBody('child', state.formData, true)),
    [state]
  )

  const validatedBody = useMemo(
    () => state.map((state) => fromBody('child', state.formData, false)),
    [state]
  )

  return renderResult(
    combine(state, draftBody, validatedBody),
    ([{ status, formData, startDates }, draftBody, validatedBody]) => {
      const save = (draft: boolean) => {
        const body = draft ? draftBody : validatedBody
        if (body) {
          if (incomeStatementId) {
            return updateChildIncomeStatement({
              childId,
              incomeStatementId,
              body,
              draft
            })
          } else {
            return createChildIncomeStatement({ childId, body, draft })
          }
        } else {
          setShowFormErrors(true)
          if (form.current) form.current.scrollToErrors()
          return
        }
      }

      return (
        <Main>
          <ChildIncomeStatementForm
            incomeStatementId={incomeStatementId}
            status={status}
            formData={formData}
            showFormErrors={showFormErrors}
            otherStartDates={startDates}
            draftSaveEnabled={draftBody !== null}
            onChange={updateFormData}
            onSave={save}
            onSuccess={navigateToList}
            onCancel={navigateToList}
            ref={form}
          />
        </Main>
      )
    }
  )
})
