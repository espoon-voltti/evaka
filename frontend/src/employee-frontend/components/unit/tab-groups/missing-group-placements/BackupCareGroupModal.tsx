// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { faChild, faExchange } from 'lib-icons'
import Select, { SelectOption } from '../../../../components/common/Select'
import { formatName } from '../../../../utils'
import { UnitBackupCare } from '../../../../types/child'
import { DaycareGroup } from '../../../../types/unit'
import { updateBackupCare } from '../../../../api/child/backup-care'

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

  const [group, setGroup] = useState<SelectOption | null>(
    backupCare.group?.id && openGroups[0]
      ? { label: openGroups[0].name, value: openGroups[0].id }
      : null
  )

  const submitForm = () => {
    if (group == null) return

    void updateBackupCare(backupCare.id, {
      period,
      groupId: group.value
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
      iconColour={'blue'}
      resolve={{
        action: submitForm,
        label: i18n.common.confirm,
        disabled: !group
      }}
      reject={{
        action: clearUiMode,
        label: i18n.common.cancel
      }}
    >
      <section>
        <div className="bold">{i18n.unit.placements.modal.child}</div>
        <span>{formatName(child.firstName, child.lastName, i18n)}</span>
      </section>
      <section>
        <div className="bold">{i18n.unit.placements.modal.group}</div>
        <Select
          items={openGroups.map((group) => ({
            value: group.id,
            label: group.name
          }))}
          onChange={setGroup}
          selectedItem={group}
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
