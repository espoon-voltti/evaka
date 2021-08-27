import React from 'react'
import IncomeStatementForm, {
  IncomeStatementFormAPI
} from './IncomeStatementForm'
import * as Form from './types/form'
import { empty } from './types/form'
import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import { fromBody } from './types/body'
import {
  createIncomeStatement,
  getIncomeStatement,
  updateIncomeStatement
} from './api'
import { useHistory, useParams } from 'react-router-dom'
import { Loading, Result } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { UUID } from 'lib-common/types'

interface New {
  type: 'new'
  formData: Form.IncomeStatementForm
}

type IncomeStatementAndForm = {
  incomeStatement: IncomeStatement
  formData: Form.IncomeStatementForm
}

interface Existing {
  type: 'existing'
  id: string
  data: Result<IncomeStatementAndForm>
}

type EditorState = New | Existing

function getFormData(state: EditorState): Form.IncomeStatementForm | undefined {
  return state.type === 'new'
    ? state.formData
    : state.data.map((d) => d.formData).getOrElse(undefined)
}

async function loadIncomeStatement(
  id: UUID
): Promise<Result<IncomeStatementAndForm>> {
  return (await getIncomeStatement(id)).map((incomeStatement) => ({
    incomeStatement,
    formData: Form.fromIncomeStatement(incomeStatement)
  }))
}

export default function IncomeStatementEditor() {
  const history = useHistory()
  const { incomeStatementId } = useParams<{ incomeStatementId: string }>()
  const form = React.useRef<IncomeStatementFormAPI | null>(null)

  const [state, setState] = React.useState<EditorState>(
    incomeStatementId === 'new'
      ? { type: 'new', formData: empty }
      : { type: 'existing', id: incomeStatementId, data: Loading.of() }
  )
  const id = state.type === 'existing' ? state.id : null

  React.useEffect(() => {
    if (id) {
      void loadIncomeStatement(id).then((incomeStatementAndForm) =>
        setState((prev) =>
          prev.type === 'existing'
            ? { ...prev, data: incomeStatementAndForm }
            : prev
        )
      )
    }
  }, [id])

  const updateFormData = (
    fn: (prev: Form.IncomeStatementForm) => Form.IncomeStatementForm
  ): void =>
    setState((prev) =>
      prev.type === 'new'
        ? { ...prev, formData: fn(prev.formData) }
        : {
            ...prev,
            data: prev.data.map((prevData) => ({
              ...prevData,
              formData: fn(prevData.formData)
            }))
          }
    )

  const formData = getFormData(state)
  const [showFormErrors, setShowFormErrors] = React.useState(false)

  const save = React.useCallback(() => {
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
  }, [id, formData])

  const navigateToList = React.useCallback(() => {
    history.push('/income')
  }, [history])

  if (formData) {
    return (
      <IncomeStatementForm
        formData={formData}
        showFormErrors={showFormErrors}
        onChange={updateFormData}
        onSave={save}
        onSuccess={navigateToList}
        onCancel={navigateToList}
        ref={form}
      />
    )
  }
  if (state.type === 'existing' && state.data.isFailure) {
    return <ErrorSegment />
  }
  return <Loader />
}
