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
import { faExclamation, faPlus, faQuestion } from 'lib-icons'
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
import InfoModal from '../../../../lib-components/molecules/modals/InfoModal'
import { formatDate } from '../../../utils/date'
import { DATE_FORMAT_DATE_TIME } from '../../../constants'
import Tooltip from '../../../../lib-components/atoms/Tooltip'

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
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const { serviceNeedOptions } = useContext(ChildContext)

  if (
    serviceNeedOptions.isSuccess &&
    serviceNeedOptions.value.find(
      (opt) => opt.validPlacementType === placementType && !opt.defaultOption
    ) === undefined
  ) {
    return null
  }

  const options = serviceNeedOptions.isSuccess
    ? serviceNeedOptions.value
        .filter((opt) => opt.validPlacementType === placementType)
        .map((opt) => ({
          label: opt.name,
          value: opt.id
        }))
    : []

  return (
    <div>
      <HeaderRow>
        <H4 noMargin>{t.title}</H4>
        <InlineButton
          onClick={() => setCreatingNew(true)}
          text={t.createNewBtn}
          icon={faPlus}
          disabled={creatingNew || editingId !== null}
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
              <Th>{t.confirmed}</Th>
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
                  onDelete={() => setDeletingId(sn.id)}
                  disabled={creatingNew || editingId !== null}
                />
              )
            )}
          </Tbody>
        </Table>
      )}

      {deletingId && (
        <InfoModal
          title={t.deleteServiceNeed.confirmTitle}
          iconColour={'orange'}
          icon={faQuestion}
          resolve={{
            action: () => deleteNewServiceNeed(deletingId).then(reload),
            label: t.deleteServiceNeed.btn
          }}
          reject={{
            action: () => setDeletingId(null),
            label: i18n.common.cancel
          }}
        />
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
        <Tooltip
          tooltip={
            <span>
              {serviceNeed.confirmed.lastName} {serviceNeed.confirmed.firstName}
            </span>
          }
        >
          {formatDate(serviceNeed.confirmed.at, DATE_FORMAT_DATE_TIME)}
        </Tooltip>
      </Td>
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
  const [overlapWarning, setOverlapWarning] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const formIsValid =
    form.startDate &&
    form.endDate &&
    form.optionId &&
    !form.endDate.isBefore(form.startDate)

  const onSubmit = () => {
    if (
      form.startDate !== undefined &&
      form.endDate !== undefined &&
      form.optionId
    ) {
      if (
        placement.serviceNeeds.find(
          (sn) =>
            sn.id !== editingId &&
            sn.startDate.isEqualOrBefore(form.endDate!) &&
            sn.endDate.isEqualOrAfter(form.startDate!)
        ) !== undefined
      ) {
        setOverlapWarning(true)
      } else {
        onConfirmSave()
      }
    }
  }

  const onConfirmSave = () => {
    if (form.startDate && form.endDate && form.optionId) {
      setSubmitting(true)

      const request = editingId
        ? updateNewServiceNeed(editingId, {
            startDate: form.startDate,
            endDate: form.endDate,
            optionId: form.optionId,
            shiftCare: form.shiftCare
          })
        : createNewServiceNeed({
            placementId: placement.id,
            startDate: form.startDate,
            endDate: form.endDate,
            optionId: form.optionId,
            shiftCare: form.shiftCare
          })

      void request
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

  return (
    <>
      <Tr>
        <StyledTd>
          <FixedSpaceRow spacing="xs" alignItems="center">
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
        </StyledTd>
        <StyledTd>
          <SimpleSelect
            options={options}
            value={form.optionId}
            onChange={(e) => setForm({ ...form, optionId: e.target.value })}
            placeholder={t.optionPlaceholder}
          />
        </StyledTd>
        <StyledTd>
          <Checkbox
            label={t.shiftCare}
            hiddenLabel
            checked={form.shiftCare}
            onChange={(checked) => setForm({ ...form, shiftCare: checked })}
          />
        </StyledTd>
        <StyledTd />
        <StyledTd>
          <FixedSpaceRow justifyContent="flex-end" spacing="m">
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
        </StyledTd>
      </Tr>

      {overlapWarning && (
        <InfoModal
          title={t.overlapWarning.title}
          text={t.overlapWarning.message}
          iconColour={'orange'}
          icon={faExclamation}
          resolve={{
            action: onConfirmSave,
            label: i18n.common.confirm
          }}
          reject={{
            action: () => setOverlapWarning(false),
            label: i18n.common.cancel
          }}
        />
      )}
    </>
  )
}

const StyledTd = styled(Td)`
  vertical-align: middle;
`

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

export default NewServiceNeeds
