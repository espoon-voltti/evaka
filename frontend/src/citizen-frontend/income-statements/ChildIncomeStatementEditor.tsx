// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useRef, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'

import { combine, Loading, Result, Success } from 'lib-common/api'
import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { renderResult } from '../async-rendering'

import ChildIncomeStatementForm from './ChildIncomeStatementForm'
import { IncomeStatementFormAPI } from './IncomeStatementComponents'
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
          ? {
              ...initialFormData(startDates),
              childIncome: true,
              highestFee: true
            }
          : Form.fromIncomeStatement(incomeStatement)
    })
  )
}

export default React.memo(function ChildIncomeStatementEditor() {
  const params = useParams<{ incomeStatementId: string; childId: string }>()
  const history = useHistory()
  const incomeStatementId =
    params.incomeStatementId === 'new' ? undefined : params.incomeStatementId

  const childId = params.childId

  const [state, setState] = useState<Result<EditorState>>(Loading.of())

  useEffect(() => {
    void initializeEditorState(childId, incomeStatementId).then(setState)
  }, [incomeStatementId, childId])

  const [showFormErrors, setShowFormErrors] = useState(false)

  const navigateToList = useCallback(() => {
    history.push('/income')
  }, [history])

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

    const save = (cancel: () => Promise<void>) => {
      const validatedData = formData ? fromBody(formData) : undefined
      if (validatedData) {
        if (id) {
          return updateChildIncomeStatement(childId, id, validatedData)
        } else {
          return createChildIncomeStatement(childId, validatedData)
        }
      } else {
        setShowFormErrors(true)
        if (form.current) form.current.scrollToErrors()
        return cancel()
      }
    }

    return (
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
    )
  })
})
