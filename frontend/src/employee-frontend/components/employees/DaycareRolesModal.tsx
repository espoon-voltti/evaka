// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { scopedRoles } from 'lib-common/api-types/employee-auth'
import { boolean, string } from 'lib-common/form/fields'
import {
  array,
  object,
  oneOf,
  recursive,
  required,
  transformed
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { Form, ValidationError, ValidationSuccess } from 'lib-common/form/types'
import { Daycare } from 'lib-common/generated/api-types/daycare'
import { UpsertEmployeeDaycareRolesRequest } from 'lib-common/generated/api-types/pis'
import { UserRole } from 'lib-common/generated/api-types/shared'
import { UUID } from 'lib-common/types'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import TreeDropdown, {
  sortTreeByText,
  TreeNode
} from 'lib-components/atoms/dropdowns/TreeDropdown'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'

import { upsertEmployeeDaycareRolesMutation } from './queries'

const treeNode = (): Form<TreeNode, never, TreeNode, unknown> =>
  object({
    text: string(),
    key: string(),
    checked: boolean(),
    children: array(recursive(treeNode))
  })

const form = transformed(
  object({
    daycareTree: array(treeNode()),
    role: required(oneOf<UserRole>())
  }),
  (res) => {
    const daycareIds = res.daycareTree
      .flatMap((careArea) => careArea.children)
      .filter((u) => u.checked)
      .map((u) => u.key)

    if (daycareIds.length === 0) return ValidationError.of('required')

    return ValidationSuccess.of<UpsertEmployeeDaycareRolesRequest>({
      daycareIds,
      role: res.role
    })
  }
)

export default React.memo(function DaycareRolesModal({
  employeeId,
  units,
  onClose
}: {
  employeeId: UUID
  units: Daycare[]
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const boundForm = useForm(
    form,
    () => ({
      daycareTree: sortTreeByText(
        units.reduce<TreeNode[]>((acc, unit) => {
          const unitNode: TreeNode = {
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
      }
    }),
    i18n.validationErrors
  )

  const { daycareTree, role } = useFormFields(boundForm)

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
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
