// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import * as _ from 'lodash'
import { UUID } from '~types'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { useTranslation } from '~state/i18n'
import { faHiking } from '@evaka/lib-icons'
import { ChildContext } from '~state'
import { Loading } from '~api'
import { getChildBackupCares } from 'api/child/backup-care'
import { UIContext } from '~state/ui'
import BackupCareForm from '~components/child-information/backup-care/BackupCareForm'
import BackupCareRow from '~components/child-information/backup-care/BackupCareRow'
import { AddButtonRow } from '@evaka/lib-components/src/atoms/buttons/AddButton'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'
import { RequireRole } from 'utils/roles'

export interface Props {
  id: UUID
  open: boolean
}

export default function BackupCare({ id, open }: Props): JSX.Element {
  const { i18n } = useTranslation()
  const { backupCares, setBackupCares } = useContext(ChildContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)

  useEffect(() => {
    void getChildBackupCares(id).then((backupCares) => {
      setBackupCares(backupCares)
    })
    return () => {
      setBackupCares(Loading.of())
    }
  }, [id, setBackupCares])

  return (
    <CollapsibleSection
      dataQa="backup-cares-collapsible"
      icon={faHiking}
      title={i18n.childInformation.backupCares.title}
      startCollapsed={!open}
    >
      {backupCares.isLoading && <Loader />}
      {backupCares.isFailure && <div>{i18n.common.loadingFailed}</div>}
      {backupCares.isSuccess && (
        <div data-qa="backup-cares">
          <RequireRole
            oneOf={[
              'SERVICE_WORKER',
              'UNIT_SUPERVISOR',
              'FINANCE_ADMIN',
              'ADMIN'
            ]}
          >
            <AddButtonRow
              text={i18n.childInformation.backupCares.create}
              onClick={() => toggleUiMode('create-new-backup-care')}
              disabled={uiMode === 'create-new-backup-care'}
              dataQa="backup-care-create-btn"
            />
          </RequireRole>
          {uiMode === 'create-new-backup-care' && (
            <BackupCareForm childId={id} />
          )}
          {_.orderBy(backupCares.value, (x) => x.period.start, 'desc').map(
            (backupCare) => (
              <BackupCareRow
                childId={id}
                key={backupCare.id}
                backupCare={backupCare}
              />
            )
          )}
        </div>
      )}
    </CollapsibleSection>
  )
}
