// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H4 } from 'lib-components/typography'
import React, { useContext, useState } from 'react'
import { NewServiceNeed, Placement } from '../../../types/child'
import _ from 'lodash'
import { useTranslation } from '../../../state/i18n'
import Toolbar from '../../common/Toolbar'
import styled from 'styled-components'
import InlineButton from '../../../../lib-components/atoms/buttons/InlineButton'
import { faPlus } from 'lib-icons'
import LocalDate from '../../../../lib-common/local-date'
import { UUID } from '../../../types'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from '../../../../lib-components/molecules/DatePickerDeprecated'
import Checkbox from '../../../../lib-components/atoms/form/Checkbox'
import SimpleSelect from '../../../../lib-components/atoms/form/SimpleSelect'
import { ChildContext } from '../../../state'
import {
  createNewServiceNeed,
  deleteNewServiceNeed,
  updateNewServiceNeed
} from '../../../api/child/new-service-needs'
import { UIContext } from '../../../state/ui'

interface DropdownOption {
  label: string
  value: string
}

interface Props {
  placement: Placement
  reload: () => void
}

function NewServiceNeeds({ placement, reload }: Props) {
  const { serviceNeeds, type: placementType } = placement

  const { i18n } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds

  const [creatingNew, setCreatingNew] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)

  const { serviceNeedOptions } = useContext(ChildContext)
  const options = serviceNeedOptions.isSuccess
    ? serviceNeedOptions.value
        .filter((opt) => opt.validPlacementType === placementType)
        .map((opt) => ({
          label: opt.name,
          value: opt.id
        }))
    : []

  const onDelete = (id: UUID) => {
    void deleteNewServiceNeed(id).then(reload)
  }

  return (
    <div>
      <HeaderRow>
        <H4 noMargin>{t.title}</H4>
        <InlineButton
          onClick={() => setCreatingNew(true)}
          text={t.createNewBtn}
          icon={faPlus}
          disabled={creatingNew}
        />
      </HeaderRow>
      {serviceNeeds.length === 0 && !creatingNew ? (
        <div>{t.noServiceNeeds}</div>
      ) : (
        <Table>
          <Thead>
            <Tr>
              <Th>{t.period}</Th>
              <Th>{t.description}</Th>
              <Th>{t.shiftCare}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {creatingNew && (
              <NewServiceNeedEditorRow
                placement={placement}
                options={options}
                initialForm={{
                  startDate: serviceNeeds.length
                    ? undefined
                    : placement.startDate,
                  endDate: serviceNeeds.length ? undefined : placement.endDate,
                  optionId: undefined,
                  shiftCare: false
                }}
                onSuccess={() => {
                  setCreatingNew(false)
                  reload()
                }}
                onCancel={() => setCreatingNew(false)}
              />
            )}

            {_.orderBy(serviceNeeds, ['startDate'], ['desc']).map((sn) =>
              editingId === sn.id ? (
                <NewServiceNeedEditorRow
                  placement={placement}
                  options={options}
                  initialForm={{
                    startDate: sn.startDate,
                    endDate: sn.endDate,
                    optionId: sn.option.id,
                    shiftCare: sn.shiftCare
                  }}
                  onSuccess={() => {
                    setEditingId(null)
                    reload()
                  }}
                  onCancel={() => setEditingId(null)}
                  editingId={editingId}
                />
              ) : (
                <NewServiceNeedReadRow
                  key={sn.id}
                  serviceNeed={sn}
                  onEdit={() => setEditingId(sn.id)}
                  onDelete={() => onDelete(sn.id)}
                  disabled={creatingNew || editingId !== null}
                />
              )
            )}
          </Tbody>
        </Table>
      )}
    </div>
  )
}

interface NewServiceNeedReadRowProps {
  serviceNeed: NewServiceNeed
  onEdit: () => void
  onDelete: () => void
  disabled?: boolean
}
function NewServiceNeedReadRow({
  serviceNeed,
  onEdit,
  onDelete,
  disabled
}: NewServiceNeedReadRowProps) {
  const { i18n } = useTranslation()
  return (
    <Tr>
      <Td>
        {serviceNeed.startDate.format()} - {serviceNeed.endDate.format()}
      </Td>
      <Td>{serviceNeed.option.name}</Td>
      <Td>{serviceNeed.shiftCare ? i18n.common.yes : i18n.common.no}</Td>
      <Td>
        <Toolbar
          dateRange={serviceNeed}
          onEdit={onEdit}
          editableFor={['ADMIN', 'UNIT_SUPERVISOR']}
          onDelete={onDelete}
          deletableFor={['ADMIN', 'UNIT_SUPERVISOR']}
          disableAll={disabled}
        />
      </Td>
    </Tr>
  )
}

interface FormData {
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  optionId: UUID | undefined
  shiftCare: boolean
}

interface NewServiceNeedCreateRowProps {
  placement: Placement
  options: DropdownOption[]
  initialForm: FormData
  onSuccess: () => void
  onCancel: () => void
  editingId?: string
}
function NewServiceNeedEditorRow({
  placement,
  options,
  initialForm,
  onSuccess,
  onCancel,
  editingId
}: NewServiceNeedCreateRowProps) {
  const { i18n } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds

  const { setErrorMessage } = useContext(UIContext)
  const [form, setForm] = useState<FormData>(initialForm)
  const [submitting, setSubmitting] = useState(false)

  const formIsValid =
    form.startDate &&
    form.endDate &&
    form.optionId &&
    !form.endDate.isBefore(form.startDate)

  const onSubmit = () => {
    if (form.startDate && form.endDate && form.optionId) {
      setSubmitting(true)

      if (editingId) {
        void updateNewServiceNeed(editingId, {
          startDate: form.startDate,
          endDate: form.endDate,
          optionId: form.optionId,
          shiftCare: form.shiftCare
        })
          .then((res) => {
            if (res.isSuccess) {
              onSuccess()
            } else {
              setErrorMessage({
                type: 'error',
                title: i18n.common.error.unknown,
                text: i18n.common.error.saveFailed,
                resolveLabel: i18n.common.ok
              })
            }
          })
          .finally(() => setSubmitting(false))
      } else {
        void createNewServiceNeed({
          placementId: placement.id,
          startDate: form.startDate,
          endDate: form.endDate,
          optionId: form.optionId,
          shiftCare: form.shiftCare
        })
          .then((res) => {
            if (res.isSuccess) {
              onSuccess()
            } else {
              setErrorMessage({
                type: 'error',
                title: i18n.common.error.unknown,
                text: i18n.common.error.saveFailed,
                resolveLabel: i18n.common.ok
              })
            }
          })
          .finally(() => setSubmitting(false))
      }
    }
  }

  return (
    <Tr>
      <Td>
        <FixedSpaceRow spacing="xs">
          <DatePickerDeprecated
            date={form.startDate}
            onChange={(date) => setForm({ ...form, startDate: date })}
            minDate={placement.startDate}
            maxDate={placement.endDate}
          />
          <span>-</span>
          <DatePickerDeprecated
            date={form.endDate}
            onChange={(date) => setForm({ ...form, endDate: date })}
            minDate={placement.startDate}
            maxDate={placement.endDate}
          />
        </FixedSpaceRow>
      </Td>
      <Td>
        <SimpleSelect
          options={options}
          value={form.optionId}
          onChange={(e) => setForm({ ...form, optionId: e.target.value })}
          placeholder={t.optionPlaceholder}
        />
      </Td>
      <Td>
        <Checkbox
          label={t.shiftCare}
          hiddenLabel
          checked={form.shiftCare}
          onChange={(checked) => setForm({ ...form, shiftCare: checked })}
        />
      </Td>
      <Td>
        <FixedSpaceRow justifyContent="flex-end">
          <InlineButton
            onClick={onCancel}
            text={i18n.common.cancel}
            disabled={submitting}
          />
          <InlineButton
            onClick={onSubmit}
            text={i18n.common.save}
            disabled={submitting || !formIsValid}
          />
        </FixedSpaceRow>
      </Td>
    </Tr>
  )
}

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

export default NewServiceNeeds
