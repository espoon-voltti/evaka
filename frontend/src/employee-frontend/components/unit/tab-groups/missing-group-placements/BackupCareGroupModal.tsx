// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { UnitBackupCare } from 'lib-common/generated/api-types/backupcare'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import { UUID } from 'lib-common/types'
import { cancelMutation } from 'lib-components/atoms/buttons/MutateButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { faChild, faExchange } from 'lib-icons'

import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import { formatName } from '../../../../utils'
import { updateBackupCareMutation } from '../../queries'

interface Props {
  unitId: UUID
  backupCare: UnitBackupCare
  groups: DaycareGroup[]
}

export default React.memo(function BackupCareGroupModal({
  unitId,
  backupCare,
  groups
}: Props) {
  const { period, child } = backupCare
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  // filter out groups which are not active on any day during the maximum placement time range
  const openGroups = groups
    .filter((group) => !group.startDate.isAfter(period.end))
    .filter((group) => !group.endDate || !group.endDate.isBefore(period.start))

  const [group, setGroup] = useState(
    backupCare.group?.id && openGroups[0]
      ? { name: openGroups[0].name, id: openGroups[0].id }
      : null
  )

  const isTransfer = !!backupCare.group
  return (
    <MutateFormModal
      data-qa="backup-care-group-modal"
      title={
        isTransfer
          ? i18n.unit.placements.modal.transferTitle
          : i18n.unit.placements.modal.createTitle
      }
      icon={isTransfer ? faExchange : faChild}
      type="info"
      resolveMutation={updateBackupCareMutation}
      resolveAction={() =>
        group !== null
          ? {
              id: backupCare.id,
              unitId,
              body: {
                period,
                groupId: group.id
              }
            }
          : cancelMutation
      }
      resolveLabel={i18n.common.confirm}
      resolveDisabled={!group}
      onSuccess={clearUiMode}
      rejectAction={clearUiMode}
      rejectLabel={i18n.common.cancel}
    >
      <section>
        <div className="bold">{i18n.unit.placements.modal.child}</div>
        <span>{formatName(child.firstName, child.lastName, i18n)}</span>
      </section>
      <section>
        <div className="bold">{i18n.unit.placements.modal.group}</div>
        <Select
          items={openGroups}
          onChange={setGroup}
          selectedItem={group}
          getItemValue={({ id }) => id}
          getItemLabel={({ name }) => name}
        />
      </section>
      <section>
        <div className="bold">{i18n.common.form.startDate}</div>
        <div>{period.start.format()}</div>
      </section>
      <section>
        <div className="bold">{i18n.common.form.endDate}</div>
        <div>{period.end.format()}</div>
      </section>
      {!group && (
        <section>
          <div className="error">
            {i18n.unit.placements.modal.errors.noGroup}
          </div>
        </section>
      )}
    </MutateFormModal>
  )
})
