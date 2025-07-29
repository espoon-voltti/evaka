// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext } from 'react'

import type { ChildId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'

import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import BackupCareForm from './backup-care/BackupCareForm'
import BackupCareRow from './backup-care/BackupCareRow'
import { backupCaresQuery } from './queries'
import { ChildContext } from './state'

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
