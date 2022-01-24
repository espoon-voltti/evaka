// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { UnitBackupCare } from 'lib-common/generated/api-types/backupcare'
import Select from 'lib-components/atoms/dropdowns/Select'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { faChild, faExchange } from 'lib-icons'
import { updateBackupCare } from '../../../../api/child/backup-care'
import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import { DaycareGroup } from '../../../../types/unit'
import { formatName } from '../../../../utils'

interface Props {
  backupCare: UnitBackupCare
  groups: DaycareGroup[]
  reload: () => void
}

export default React.memo(function BackupCareGroupModal({
  backupCare,
  groups,
  reload
}: Props) {
  const { period, child } = backupCare
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)

  // filter out groups which are not active on any day during the maximum placement time range
  const openGroups = groups
    .filter((group) => !group.startDate.isAfter(period.end))
    .filter((group) => !group.endDate || !group.endDate.isBefore(period.start))

  const [group, setGroup] = useState(
    backupCare.group?.id && openGroups[0]
      ? { name: openGroups[0].name, id: openGroups[0].id }
      : null
  )

  const submitForm = () => {
    if (group == null) return

    void updateBackupCare(backupCare.id, {
      period,
      groupId: group.id
    }).then((res) => {
      if (res.isFailure) {
        clearUiMode()
        setErrorMessage({
          type: 'error',
          title: i18n.unit.error.placement.create,
          text: i18n.common.tryAgain,
          resolveLabel: i18n.common.ok
        })
      } else {
        clearUiMode()
        reload()
      }
    })
  }

  const isTransfer = !!backupCare.group
  return (
    <FormModal
      data-qa="backup-care-group-modal"
      title={
        isTransfer
          ? i18n.unit.placements.modal.transferTitle
          : i18n.unit.placements.modal.createTitle
      }
      icon={isTransfer ? faExchange : faChild}
      type="info"
      resolveAction={submitForm}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={!group}
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
    </FormModal>
  )
})
