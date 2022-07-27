// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import type { Result } from 'lib-common/api'
import { combine, Loading, Success } from 'lib-common/api'
import type { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Main from 'lib-components/atoms/Main'

import { renderResult } from '../async-rendering'

import ChildIncomeStatementForm from './ChildIncomeStatementForm'
import type { IncomeStatementFormAPI } from './IncomeStatementComponents'
import {
  createChildIncomeStatement,
  getChildIncomeStatement,
  getChildIncomeStatementStartDates,
  updateChildIncomeStatement
} from './api'
import { fromBody } from './types/body'
import * as Form from './types/form'
import { initialFormData } from './types/form'

interface EditorState {
  id: string | undefined
  startDates: LocalDate[]
  formData: Form.IncomeStatementForm
}

async function initializeEditorState(
  childId: UUID,
  id: UUID | undefined
): Promise<Result<EditorState>> {
  const incomeStatementPromise: Promise<Result<IncomeStatement | undefined>> =
    id
      ? getChildIncomeStatement(childId, id)
      : Promise.resolve(Success.of(undefined))

  const [incomeStatement, startDates] = await Promise.all([
    incomeStatementPromise,
    getChildIncomeStatementStartDates(childId)
  ])

  return combine(incomeStatement, startDates).map(
    ([incomeStatement, startDates]) => ({
      id,
      startDates,
      formData:
        incomeStatement === undefined
          ? { ...initialFormData(startDates), childIncome: true }
          : Form.fromIncomeStatement(incomeStatement)
    })
  )
}

export default React.memo(function ChildIncomeStatementEditor() {
  const params = useNonNullableParams<{
    incomeStatementId: string
    childId: string
  }>()
  const navigate = useNavigate()
  const incomeStatementId =
    params.incomeStatementId === 'new' ? undefined : params.incomeStatementId

  const childId = params.childId

  const [state, setState] = useState<Result<EditorState>>(Loading.of())

  useEffect(() => {
    void initializeEditorState(childId, incomeStatementId).then(setState)
  }, [incomeStatementId, childId])

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

  return renderResult(state, (state) => {
    const { id, formData, startDates } = state

    const save = () => {
      const validatedData = formData ? fromBody('child', formData) : undefined
      if (validatedData) {
        if (id) {
          return updateChildIncomeStatement(childId, id, validatedData)
        } else {
          return createChildIncomeStatement(childId, validatedData)
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
          incomeStatementId={id}
          formData={formData}
          showFormErrors={showFormErrors}
          otherStartDates={startDates}
          onChange={updateFormData}
          onSave={save}
          onSuccess={navigateToList}
          onCancel={navigateToList}
          ref={form}
        />
      </Main>
    )
  })
})
