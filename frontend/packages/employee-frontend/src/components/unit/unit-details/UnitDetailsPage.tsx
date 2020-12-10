// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import UnitEditor from '~components/unit/unit-details/UnitEditor'
import { Loading, Result } from '~api'
import { CareArea } from '~types/unit'
import { getAreas } from '~api/daycare'
import { getEmployees } from '~api/employees'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { Gap } from '@evaka/lib-components/src/white-space'
import { useParams } from 'react-router-dom'
import {
  DaycareFields,
  getDaycare,
  UnitResponse,
  updateDaycare
} from '~api/unit'
import { TitleContext, TitleState } from '~state/title'
import { useTranslation } from '~state/i18n'
import { Employee } from '~types/employee'

export default function UnitDetailsPage(): JSX.Element {
  const { id } = useParams<{ id: string }>()
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const [unit, setUnit] = useState<Result<UnitResponse>>(Loading.of())
  const [areas, setAreas] = useState<Result<CareArea[]>>(Loading.of())
  const [employees, setEmployees] = useState<Result<Employee[]>>(Loading.of())
  const [editable, setEditable] = useState(false)
  const [submitState, setSubmitState] = useState<Result<void> | undefined>(
    undefined
  )
  useEffect(() => {
    if (unit.isSuccess) {
      setTitle(unit.value.daycare.name)
    }
  }, [unit])

  useEffect(() => {
    void getAreas().then(setAreas)
    void getEmployees().then(setEmployees)
    void getDaycare(id).then(setUnit)
  }, [])

  const onSubmit = (fields: DaycareFields, currentUnit: UnitResponse) => {
    if (!id) return
    setSubmitState(Loading.of())
    void updateDaycare(id, fields).then((result) => {
      if (result.isSuccess) {
        setUnit(result.map((r) => ({ ...currentUnit, daycare: r })))
        setEditable(false)
      }
      setSubmitState(result.map((_r) => undefined))
    })
  }

  const loading = areas.isLoading || unit.isLoading || employees.isLoading
  const failure = areas.isFailure || unit.isFailure || employees.isFailure

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        {loading && <Loader />}
        {!loading && failure && <div>{i18n.common.error.unknown}</div>}
        {areas.isSuccess && unit.isSuccess && employees.isSuccess && (
          <UnitEditor
            editable={editable}
            areas={areas.value}
            employees={employees.value}
            unit={unit.value.daycare}
            submit={submitState}
            onSubmit={(fields) => onSubmit(fields, unit.value)}
            onClickCancel={() => setEditable(false)}
            onClickEdit={() => setEditable(true)}
          />
        )}
      </ContentArea>
    </Container>
  )
}
