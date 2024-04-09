// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { isValidTime } from 'lib-common/date'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1, H4, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faArrowLeft } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { Actions, BackButtonInline } from '../common/components'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup } from '../common/unit-or-group'
import { TallContentArea } from '../pairing/components'
import { unitInfoQuery } from '../units/queries'

import { externalStaffArrivalMutation } from './queries'

interface FormState {
  arrived: string
  name: string
  group: GroupInfo | null
  hasStaffOccupancyEffect: boolean
}

export default function MarkExternalStaffMemberArrivalPage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()
  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const [form, setForm] = useState<FormState>(() => ({
    arrived: HelsinkiDateTime.now().toLocalTime().format(),
    group:
      unitOrGroup.type === 'group'
        ? unitInfoResponse
            .map(
              ({ groups }) =>
                groups.find(({ id }) => id === unitOrGroup.id) ?? null
            )
            .getOrElse(null)
        : null,
    name: '',
    hasStaffOccupancyEffect: true
  }))

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
          onClick={() => navigate(-1)}
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
        {renderResult(unitInfoResponse, (unit) => (
          <>
            {unit.isOperationalDate ? (
              <ListGrid>
                <Label>{i18n.attendances.arrivalTime}</Label>
                <TimeInput
                  value={form.arrived}
                  onChange={(arrived) =>
                    setForm((old) => ({ ...old, arrived }))
                  }
                  data-qa="input-arrived"
                />

                <Label>{i18n.common.name}</Label>
                <InputField
                  type="text"
                  value={form.name}
                  onChange={(name) => setForm((old) => ({ ...old, name }))}
                  width="full"
                  data-qa="input-name"
                  placeholder={`${i18n.common.lastName} ${i18n.common.firstName}`}
                />

                <Label>{i18n.common.group}</Label>
                <Combobox
                  items={unit.groups}
                  selectedItem={form.group}
                  getItemLabel={({ name }) => name}
                  getItemDataQa={(group) => group.id}
                  onChange={(group) => setForm((old) => ({ ...old, group }))}
                  data-qa="input-group"
                />
                <Checkbox
                  label={i18n.staff.staffOccupancyEffect}
                  checked={form.hasStaffOccupancyEffect}
                  onChange={(state) =>
                    setForm((prev) => ({
                      ...prev,
                      hasStaffOccupancyEffect: state
                    }))
                  }
                />
              </ListGrid>
            ) : (
              <H4 centered={true}>{i18n.attendances.notOperationalDate}</H4>
            )}
            <Gap size="xs" />
            <Actions>
              <FixedSpaceRow fullWidth>
                <Button
                  text={i18n.common.cancel}
                  onClick={() => navigate(-1)}
                />
                <MutateButton
                  primary
                  text={i18n.common.confirm}
                  disabled={!unit.isOperationalDate || !formIsValid()}
                  mutation={externalStaffArrivalMutation}
                  onClick={() =>
                    form.group
                      ? {
                          unitId,
                          request: {
                            arrived: LocalTime.parse(form.arrived),
                            groupId: form.group.id,
                            name: form.name.trim(),
                            hasStaffOccupancyEffect:
                              form.hasStaffOccupancyEffect
                          }
                        }
                      : cancelMutation
                  }
                  onSuccess={() => {
                    history.go(-1)
                  }}
                  data-qa="mark-arrived-btn"
                />
              </FixedSpaceRow>
            </Actions>
          </>
        ))}
      </ContentArea>
    </TallContentArea>
  )
}
