// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext } from 'react'

import type { ChildId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'

import BackupCareForm from '../../components/child-information/backup-care/BackupCareForm'
import BackupCareRow from '../../components/child-information/backup-care/BackupCareRow'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import { backupCaresQuery } from './queries'

export interface Props {
  childId: ChildId
}

export default function BackupCare({ childId }: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(ChildContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)

  const backupCares = useQueryResult(backupCaresQuery({ childId }))

  return renderResult(backupCares, ({ backupCares }) => (
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
        <BackupCareForm childId={childId} backupCares={backupCares} />
      )}
      {orderBy(backupCares, (x) => x.backupCare.period.start, 'desc').map(
        (backupCare) => (
          <BackupCareRow
            key={backupCare.backupCare.id}
            childId={childId}
            backupCares={backupCares}
            backupCare={backupCare.backupCare}
            permittedActions={backupCare.permittedActions}
          />
        )
      )}
    </div>
  ))
}
