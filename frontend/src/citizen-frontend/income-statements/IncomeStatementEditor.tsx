// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useRef, useState } from 'react'
import { RouteComponentProps } from 'react-router'
import { combine, Loading, Result, Success } from 'lib-common/api'
import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { renderResult } from '../async-rendering'
import { IncomeStatementFormAPI } from './IncomeStatementComponents'
import IncomeStatementForm from './IncomeStatementForm'
import {
  createIncomeStatement,
  getIncomeStatement,
  getIncomeStatementStartDates,
  updateIncomeStatement
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
  id: UUID | undefined
): Promise<Result<EditorState>> {
  const incomeStatementPromise: Promise<Result<IncomeStatement | undefined>> =
    id ? getIncomeStatement(id) : Promise.resolve(Success.of(undefined))
  const [incomeStatement, startDates] = await Promise.all([
    incomeStatementPromise,
    getIncomeStatementStartDates()
  ])

  return combine(incomeStatement, startDates).map(
    ([incomeStatement, startDates]) => ({
      id,
      startDates,
      formData:
        incomeStatement === undefined
          ? initialFormData(startDates)
          : Form.fromIncomeStatement(incomeStatement)
    })
  )
}

export default React.memo(function IncomeStatementEditor({
  history,
  match
}: RouteComponentProps<{ incomeStatementId: string }>) {
  const incomeStatementId =
    match.params.incomeStatementId === 'new'
      ? undefined
      : match.params.incomeStatementId
  const [state, setState] = useState<Result<EditorState>>(Loading.of())

  useEffect(() => {
    void initializeEditorState(incomeStatementId).then(setState)
  }, [incomeStatementId])

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
          return updateIncomeStatement(id, validatedData)
        } else {
          return createIncomeStatement(validatedData)
        }
      } else {
        setShowFormErrors(true)
        if (form.current) form.current.scrollToErrors()
        return cancel()
      }
    }

    return (
      <IncomeStatementForm
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
