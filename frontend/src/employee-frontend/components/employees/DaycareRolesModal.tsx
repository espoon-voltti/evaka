// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useMemo } from 'react'

import { scopedRoles } from 'lib-common/api-types/employee-auth'
import { boolean, localDate, string } from 'lib-common/form/fields'
import {
  array,
  object,
  oneOf,
  recursive,
  required,
  transformed,
  value
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type { Form } from 'lib-common/form/types'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import type { Daycare } from 'lib-common/generated/api-types/daycare'
import type {
  EmployeeWithDaycareRoles,
  UpsertEmployeeDaycareRolesRequest
} from 'lib-common/generated/api-types/pis'
import type {
  AreaId,
  DaycareId,
  UserRole
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import type { TreeNode } from 'lib-components/atoms/dropdowns/TreeDropdown'
import TreeDropdown, {
  sortTreeByText
} from 'lib-components/atoms/dropdowns/TreeDropdown'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'

import { upsertEmployeeDaycareRolesMutation } from './queries'

interface DaycareTreeNode extends TreeNode {
  key: AreaId | DaycareId
  children: DaycareTreeNode[]
}

const treeNode = (): Form<DaycareTreeNode, never, DaycareTreeNode, unknown> =>
  object({
    text: string(),
    key: value<AreaId | DaycareId>(),
    checked: boolean(),
    children: array(recursive(treeNode))
  })

const unitsFromTree = (tree: DaycareTreeNode[]): DaycareId[] =>
  tree
    .flatMap((careArea) => careArea.children)
    .filter((u) => u.checked)
    .map((u) => u.key as DaycareId)

const form = transformed(
  object({
    daycareTree: array(treeNode()),
    role: required(oneOf<UserRole>()),
    startDate: required(localDate()),
    endDate: localDate()
  }),
  (res) => {
    const daycareIds = unitsFromTree(res.daycareTree)
    if (daycareIds.length === 0) return ValidationError.of('required')

    if (res.endDate && res.endDate.isBefore(res.startDate))
      return ValidationError.field('endDate', 'dateTooEarly')

    return ValidationSuccess.of<UpsertEmployeeDaycareRolesRequest>({
      daycareIds,
      role: res.role,
      startDate: res.startDate,
      endDate: res.endDate ?? null
    })
  }
)

export default React.memo(function DaycareRolesModal({
  employee: { id: employeeId, daycareRoles, scheduledDaycareRoles },
  units,
  onClose
}: {
  employee: EmployeeWithDaycareRoles
  units: Daycare[]
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()

  const boundForm = useForm(
    form,
    () => ({
      daycareTree: sortTreeByText(
        units.reduce<DaycareTreeNode[]>((acc, unit) => {
          const unitNode: DaycareTreeNode = {
            key: unit.id,
            text: unit.name,
            checked: false,
            children: []
          }
          const careArea = acc.find((a) => a.key === unit.area.id)
          if (careArea) {
            careArea.children.push(unitNode)
          } else {
            acc.push({
              key: unit.area.id,
              text: unit.area.name,
              checked: false,
              children: [unitNode]
            })
          }
          return acc
        }, [])
      ),
      role: {
        domValue: scopedRoles[0],
        options: scopedRoles.map((r) => ({
          value: r,
          domValue: r,
          label: i18n.roles.adRoles[r]
        }))
      },
      startDate: localDate.fromDate(LocalDate.todayInHelsinkiTz(), {
        minDate: LocalDate.todayInHelsinkiTz()
      }),
      endDate: localDate.fromDate(null, {
        minDate: LocalDate.todayInHelsinkiTz()
      })
    }),
    i18n.validationErrors
  )

  const { daycareTree, role, startDate, endDate } = useFormFields(boundForm)

  const selectedUnits = useMemo(
    () => (daycareTree.isValid() ? unitsFromTree(daycareTree.value()) : []),
    [daycareTree]
  )

  const currentAclWarning = useMemo(() => {
    if (!startDate.isValid()) return null
    const isScheduling = startDate
      .value()
      .isAfter(LocalDate.todayInHelsinkiTz())
    const overlapping = sortBy(
      daycareRoles.filter(
        (r) =>
          selectedUnits.includes(r.daycareId) &&
          (r.endDate === null || r.endDate.isEqualOrAfter(startDate.value()))
      ),
      (r) => r.daycareName
    )
    if (overlapping.length === 0) return null
    return (
      <div>
        <span>{i18n.employees.editor.unitRoles.warnings.hasCurrent}:</span>
        <ul>
          {overlapping.map((r) => (
            <li key={r.daycareId}>
              {i18n.roles.adRoles[r.role]}: {r.daycareName}
            </li>
          ))}
        </ul>
        <span>
          {isScheduling
            ? i18n.employees.editor.unitRoles.warnings.currentEnding(
                startDate.value()
              )
            : i18n.employees.editor.unitRoles.warnings.currentRemoved}
        </span>
      </div>
    )
  }, [i18n, daycareRoles, startDate, selectedUnits])

  const scheduledAclWarning = useMemo(() => {
    const removed = scheduledDaycareRoles.filter((r) =>
      selectedUnits.includes(r.daycareId)
    )
    if (removed.length === 0) return null
    return (
      <div>
        <span>{i18n.employees.editor.unitRoles.warnings.hasScheduled}:</span>
        <ul>
          {removed.map((r) => (
            <li key={r.daycareId}>
              {i18n.roles.adRoles[r.role]}: {r.daycareName} (
              {r.startDate.format()} -)
            </li>
          ))}
        </ul>
        <span>{i18n.employees.editor.unitRoles.warnings.scheduledRemoved}</span>
      </div>
    )
  }, [i18n, scheduledDaycareRoles, selectedUnits])

  return (
    <MutateFormModal
      title={i18n.employees.editor.unitRoles.addRolesModalTitle}
      resolveMutation={upsertEmployeeDaycareRolesMutation}
      resolveAction={() => ({ id: employeeId, body: boundForm.value() })}
      resolveLabel={i18n.common.save}
      resolveDisabled={!boundForm.isValid()}
      onSuccess={onClose}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn spacing="m">
        <FixedSpaceColumn spacing="xs">
          <Label>{i18n.employees.editor.unitRoles.units}</Label>
          <TreeDropdown
            tree={daycareTree.state}
            onChange={daycareTree.set}
            placeholder={i18n.employees.editor.unitRoles.units}
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="xs">
          <Label>{i18n.employees.editor.unitRoles.role}</Label>
          <SelectF bind={role} />
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="xs">
          <Label>{i18n.employees.editor.unitRoles.startDate}</Label>
          <DatePickerF bind={startDate} locale={lang} />
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="xs">
          <Label>{i18n.employees.editor.unitRoles.endDate}</Label>
          <DatePickerF bind={endDate} locale={lang} />
        </FixedSpaceColumn>
        {currentAclWarning && <AlertBox message={currentAclWarning} />}
        {scheduledAclWarning && <AlertBox message={scheduledAclWarning} />}
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
