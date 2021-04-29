// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import * as _ from 'lodash'
import { UUID } from '../../types'
import Loader from 'lib-components/atoms/Loader'
import { useTranslation } from '../../state/i18n'
import { ChildContext } from '../../state'
import { Loading } from 'lib-common/api'
import { getChildBackupCares } from '../../api/child/backup-care'
import { UIContext } from '../../state/ui'
import BackupCareForm from '../../components/child-information/backup-care/BackupCareForm'
import BackupCareRow from '../../components/child-information/backup-care/BackupCareRow'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { RequireRole } from '../../utils/roles'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { H2 } from '../../../lib-components/typography'

export interface Props {
  id: UUID
  startOpen: boolean
}

export default function BackupCare({ id, startOpen }: Props): JSX.Element {
  const { i18n } = useTranslation()
  const { backupCares, setBackupCares } = useContext(ChildContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)

  const [open, setOpen] = useState(startOpen)

  useEffect(() => {
    void getChildBackupCares(id).then((backupCares) => {
      setBackupCares(backupCares)
    })
    return () => {
      setBackupCares(Loading.of())
    }
  }, [id, setBackupCares])

  return (
    <CollapsibleContentArea
      //icon={faHiking}
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
              data-qa="backup-care-create-btn"
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
    </CollapsibleContentArea>
  )
}
