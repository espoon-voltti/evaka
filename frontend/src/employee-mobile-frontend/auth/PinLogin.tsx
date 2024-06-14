// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, {
  FormEventHandler,
  useCallback,
  useContext,
  useMemo,
  useState
} from 'react'
import { useNavigate } from 'react-router-dom'

import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { PlainPinInput } from 'lib-components/molecules/PinInput'
import { H1, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { childrenQuery } from '../child-attendance/queries'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { unitInfoQuery } from '../units/queries'

import { pinLogin } from './api'
import { UserContext } from './state'

interface EmployeeOption {
  name: string
  id: string
}

const PinLoginForm = React.memo(function PinLoginForm({
  unitId
}: {
  unitId: UUID
}) {
  const { i18n } = useTranslation()
  const { user, refreshAuthStatus } = useContext(UserContext)
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))
  const employeeId = user.map((u) => u?.employeeId ?? null).getOrElse(null)
  const showEmployeeSelection = employeeId === null

  const employeeOptions = useMemo<EmployeeOption[]>(
    () =>
      showEmployeeSelection
        ? sortBy(
            unitInfoResponse
              .map(({ staff }) => staff.filter(({ pinSet }) => pinSet))
              .getOrElse([]),
            ({ lastName }) => lastName,
            ({ firstName }) => firstName
          ).map((staff) => ({
            name: `${staff.lastName} ${staff.firstName}`,
            id: staff.id
          }))
        : [],
    [showEmployeeSelection, unitInfoResponse]
  )

  const [employee, setEmployee] = useState<EmployeeOption | null>(null)
  const selectEmployee = useCallback(
    (e: EmployeeOption | null) => setEmployee(e),
    []
  )
  const [pin, setPin] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const selectedEmployeeId = showEmployeeSelection ? employee?.id : employeeId
  const valid = pin.length === 4 && selectedEmployeeId

  const submit = useCallback<FormEventHandler>(
    (e) => {
      e.preventDefault()
      if (!valid) {
        return
      }
      setSubmitting(true)
      setError('')
      return pinLogin(selectedEmployeeId, pin).then((res) => {
        setSubmitting(false)
        if (res.isSuccess) {
          if (res.value.status === 'SUCCESS') {
            refreshAuthStatus()
          } else {
            setError(i18n.pin.status[res.value.status])
          }
        } else if (res.isFailure) {
          setError(i18n.pin.unknownError)
        }
      })
    },
    [
      selectedEmployeeId,
      i18n.pin.status,
      i18n.pin.unknownError,
      pin,
      refreshAuthStatus,
      valid
    ]
  )

  return (
    <>
      <H1 centered noMargin>
        {i18n.pin.header}
      </H1>
      <Gap />
      <form onSubmit={submit}>
        <FixedSpaceColumn>
          {showEmployeeSelection && (
            <>
              <Label htmlFor="employee">{i18n.pin.staff}</Label>
              <Select
                id="employee"
                items={employeeOptions}
                selectedItem={employee}
                onChange={selectEmployee}
                placeholder={i18n.pin.selectStaff}
                getItemValue={({ id }) => id}
                getItemLabel={({ name }) => name}
                data-qa="select-staff"
              />
            </>
          )}

          <Label htmlFor="pin">{i18n.pin.pinCode}</Label>
          <PlainPinInput
            id="pin"
            value={pin}
            onChange={setPin}
            info={error ? { text: error, status: 'warning' } : undefined}
          />

          <Gap size="s" />

          <LegacyButton
            primary
            disabled={!valid || submitting}
            text={i18n.pin.login}
            onClick={submit}
            data-qa="pin-submit"
          />
        </FixedSpaceColumn>
      </form>
    </>
  )
})

export const PinLogin = React.memo(function PinLogin({
  unitId,
  childId
}: {
  unitId: UUID
  childId?: UUID
}) {
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))
  const unitChildren = useQueryResult(childrenQuery(unitId))

  const navigate = useNavigate()
  const onClose = useCallback(() => navigate(-1), [navigate])

  const title = childId
    ? unitChildren
        .map((children) => children.find((c) => c.id === childId))
        .map((c) => (c ? `${c.firstName} ${c.lastName}` : ''))
        .getOrElse('')
    : unitInfoResponse.map((u) => u.name).getOrElse('')

  return (
    <>
      <TopBar title={title} onClose={onClose} unitId={unitId} />
      <ContentArea
        opaque
        paddingHorizontal={defaultMargins.s}
        paddingVertical={defaultMargins.s}
      >
        <PinLoginForm unitId={unitId} />
      </ContentArea>
    </>
  )
})
