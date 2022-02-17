// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'

import { Result } from 'lib-common/api'
import { formatTime, isValidTime } from 'lib-common/date'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import InputField from 'lib-components/atoms/form/InputField'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faArrowLeft } from 'lib-icons'

import { postExternalStaffArrival } from '../../api/realtimeStaffAttendances'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'
import { Actions, BackButtonInline } from '../attendances/components'
import { TallContentArea } from '../mobile/components'

interface FormState {
  arrived: string
  name: string
  group: GroupInfo | null
}

export default function MarkExternalStaffMemberArrivalPage() {
  const history = useHistory()
  const { groupId } = useParams<{ groupId: string }>()
  const { i18n } = useTranslation()
  const { unitInfoResponse } = useContext(UnitContext)
  const { reloadStaffAttendance } = useContext(StaffAttendanceContext)

  const [form, setForm] = useState<FormState>({
    arrived: formatTime(new Date()),
    group: unitInfoResponse
      .map(({ groups }) => groups.find(({ id }) => id === groupId) ?? null)
      .getOrElse(null),
    name: ''
  })

  const onSubmit = (): Promise<Result<void>> =>
    form.group
      ? postExternalStaffArrival({
          arrived: form.arrived,
          groupId: form.group.id,
          name: form.name
        })
      : Promise.reject()

  const formIsValid = () =>
    !!(isValidTime(form.arrived) && form.name.trim() && form.group)

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <div>
        <BackButtonInline
          onClick={() => history.goBack()}
          icon={faArrowLeft}
          text={i18n.attendances.staff.markExternalPerson}
        />
      </div>
      <ContentArea
        shadow
        opaque={true}
        paddingHorizontal="s"
        paddingVertical="m"
      >
        <H1 centered>{i18n.attendances.staff.markExternalPersonTitle}</H1>
        <HorizontalLine />
        <ListGrid>
          <Label>{i18n.attendances.arrivalTime}</Label>
          <TimeInput
            value={form.arrived}
            onChange={(arrived) => setForm((old) => ({ ...old, arrived }))}
            data-qa="input-arrived"
          />

          <Label>{i18n.common.name}</Label>
          <InputField
            type="text"
            value={form.name}
            onChange={(name) => setForm((old) => ({ ...old, name }))}
            width="full"
            data-qa="input-name"
          />

          <Label>{i18n.common.group}</Label>
          {renderResult(unitInfoResponse, (unit) => (
            <Combobox
              items={unit.groups}
              selectedItem={form.group}
              getItemLabel={({ name }) => name}
              onChange={(group) => setForm((old) => ({ ...old, group }))}
              data-qa="input-group"
            />
          ))}
        </ListGrid>
        <Gap size="xs" />
        <Actions>
          <FixedSpaceRow fullWidth>
            <Button
              text={i18n.common.cancel}
              onClick={() => history.goBack()}
            />
            <AsyncButton
              primary
              text={i18n.common.confirm}
              disabled={!formIsValid()}
              onClick={onSubmit}
              onSuccess={() => {
                reloadStaffAttendance()
                history.go(-1)
              }}
              data-qa="mark-arrived-btn"
            />
          </FixedSpaceRow>
        </Actions>
      </ContentArea>
    </TallContentArea>
  )
}
