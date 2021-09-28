// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useRef, useState } from 'react'
import IncomeStatementForm, {
  IncomeStatementFormAPI
} from './IncomeStatementForm'
import * as Form from './types/form'
import { fromBody } from './types/body'
import {
  createIncomeStatement,
  getIncomeStatements,
  updateIncomeStatement
} from './api'
import { combine, Failure, Loading, Result, Success } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import LocalDate from 'lib-common/local-date'
import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import { initialFormData } from './types/form'
import { renderResult } from 'lib-components/async-rendering'
import { RouteComponentProps } from 'react-router'

interface EditorState {
  id: string | undefined
  startDates: LocalDate[]
  formData: Form.IncomeStatementForm
}

function isValidStartDate(otherStartDates: LocalDate[]) {
  return (date: LocalDate) => otherStartDates.every((d) => !d.isEqual(date))
}

async function initializeEditorState(
  id: UUID | undefined
): Promise<Result<EditorState>> {
  const incomeStatements = await getIncomeStatements()

  const startDates = incomeStatements.map((i) => i.map((s) => s.startDate))

  const incomeStatement: Result<IncomeStatement | undefined> =
    id === undefined
      ? Success.of(undefined)
      : incomeStatements.chain((i): Result<IncomeStatement> => {
          const found = i.find((s) => s.id === id)
          if (found === undefined) return Failure.of({ message: 'Not found' })
          return Success.of(found)
        })

  return combine(incomeStatement, startDates).map(
    ([incomeStatement, startDates]) => ({
      id,
      startDates,
      formData:
        incomeStatement === undefined
          ? initialFormData(isValidStartDate(startDates))
          : Form.fromIncomeStatement(incomeStatement)
    })
  )
}

export default function IncomeStatementEditor({
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

  return renderResult(state, (state) => {
    const { id, formData, startDates } = state

    const updateFormData = (
      fn: (prev: Form.IncomeStatementForm) => Form.IncomeStatementForm
    ): void =>
      setState((prev) =>
        prev.map((state) => ({ ...state, formData: fn(state.formData) }))
      )

    const save = () => {
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
        return Promise.resolve('AsyncButton.cancel' as const)
      }
    }

    return (
      <IncomeStatementForm
        incomeStatementId={id}
        formData={formData}
        showFormErrors={showFormErrors}
        isValidStartDate={isValidStartDate(startDates)}
        onChange={updateFormData}
        onSave={save}
        onSuccess={navigateToList}
        onCancel={navigateToList}
        ref={form}
      />
    )
  })
}
