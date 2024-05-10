// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useEffect, useState } from 'react'

import { UUID } from 'lib-common/types'
import Loader from 'lib-components/atoms/Loader'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'

import BackupCareForm from '../../components/child-information/backup-care/BackupCareForm'
import BackupCareRow from '../../components/child-information/backup-care/BackupCareRow'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'

export interface Props {
  childId: UUID
  startOpen: boolean
}

export default function BackupCare({ childId, startOpen }: Props) {
  const { i18n } = useTranslation()
  const { backupCares, loadBackupCares, permittedActions } =
    useContext(ChildContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)

  const [open, setOpen] = useState(startOpen)

  useEffect(() => {
    void loadBackupCares()
  }, [childId, loadBackupCares])

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.backupCares.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="backup-cares-collapsible"
    >
      {backupCares.isLoading && <Loader />}
      {backupCares.isFailure && <div>{i18n.common.loadingFailed}</div>}
      {backupCares.isSuccess && (
        <div data-qa="backup-cares">
          {permittedActions.has('CREATE_BACKUP_CARE') && (
            <AddButtonRow
              text={i18n.childInformation.backupCares.create}
              onClick={() => toggleUiMode('create-new-backup-care')}
              disabled={uiMode === 'create-new-backup-care'}
              data-qa="backup-care-create-btn"
            />
          )}
          {uiMode === 'create-new-backup-care' && (
            <BackupCareForm childId={childId} />
          )}
          {orderBy(
            backupCares.value,
            (x) => x.backupCare.period.start,
            'desc'
          ).map((backupCare) => (
            <BackupCareRow
              childId={childId}
              key={backupCare.backupCare.id}
              backupCare={backupCare.backupCare}
              permittedActions={backupCare.permittedActions}
            />
          ))}
        </div>
      )}
    </CollapsibleContentArea>
  )
}
