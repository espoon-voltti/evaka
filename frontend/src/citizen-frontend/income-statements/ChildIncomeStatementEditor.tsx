// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useRef, useState } from 'react'
import { useLocation } from 'wouter'

import type { Result } from 'lib-common/api'
import { combine, Loading } from 'lib-common/api'
import type { IncomeStatementStatus } from 'lib-common/generated/api-types/incomestatement'
import type {
  ChildId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
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
import useRouteParams, { useIdRouteParam } from 'lib-common/useRouteParams'
import Main from 'lib-components/atoms/Main'

import { renderResult } from '../async-rendering'

import ChildIncomeStatementForm from './ChildIncomeStatementForm'
import type { IncomeStatementFormAPI } from './IncomeStatementComponents'
import {
  childIncomeStatementStartDatesQuery,
  createChildIncomeStatementMutation,
  incomeStatementQuery,
  updateIncomeStatementMutation
} from './queries'

interface EditorState {
  id: string | undefined
  status: IncomeStatementStatus
  startDates: LocalDate[]
  formData: Form.IncomeStatementForm
}

function useInitialEditorState(
  childId: ChildId,
  id: IncomeStatementId | undefined
): Result<EditorState> {
  const incomeStatement = useQueryResult(
    id ? incomeStatementQuery({ incomeStatementId: id }) : constantQuery(null)
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
  const params = useRouteParams(['incomeStatementId'])
  const childId = useIdRouteParam<ChildId>('childId')
  const [, navigate] = useLocation()
  const incomeStatementId =
    params.incomeStatementId === 'new'
      ? undefined
      : fromUuid<IncomeStatementId>(params.incomeStatementId)

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
    navigate('/income')
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
  const { mutateAsync: updateIncomeStatement } = useMutationResult(
    updateIncomeStatementMutation
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
      if (status !== 'DRAFT') {
        navigateToList()
        return null
      }

      const save = (draft: boolean) => {
        const body = draft ? draftBody : validatedBody
        if (body) {
          if (incomeStatementId) {
            return updateIncomeStatement({
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
