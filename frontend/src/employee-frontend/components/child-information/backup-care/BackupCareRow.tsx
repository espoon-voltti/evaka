// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { ChildBackupCare } from 'lib-common/generated/api-types/backupcare'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faQuestion } from 'lib-icons'

import BackupCareForm from '../../../components/child-information/backup-care/BackupCareForm'
import Toolbar from '../../../components/common/Toolbar'
import { deleteBackupCare } from '../../../generated/api-clients/backupcare'
import { ChildContext } from '../../../state'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'

export interface Props {
  childId: UUID
  backupCare: ChildBackupCare
  permittedActions: Action.BackupCare[]
}

const Row = styled.section`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin: 20px 0;

  .title {
    margin: 0 5px;
  }
`

const UnitName = styled(Title)`
  width: 50%;
  a {
    color: inherit;
  }
`

const Period = styled(Title)`
  max-width: 280px;
  width: 50%;
`

export default function BackupCareRow({
  childId,
  backupCare,
  permittedActions
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const { loadBackupCares } = useContext(ChildContext)

  return (
    <div>
      {uiMode === `remove-backup-care-${backupCare.id}` && (
        <InfoModal
          type="warning"
          title={i18n.childInformation.backupCares.remove}
          text={`${
            i18n.common.period
          } ${backupCare.period.start.format()} - ${backupCare.period.end.format()}`}
          icon={faQuestion}
          reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
          resolve={{
            action: () =>
              wrapResult(deleteBackupCare)({ id: backupCare.id })
                .then(() => clearUiMode())
                .then(() => loadBackupCares()),
            label: i18n.common.remove
          }}
        />
      )}
      {uiMode === `edit-backup-care-${backupCare.id}` ? (
        <BackupCareForm childId={childId} backupCare={backupCare} />
      ) : (
        <Row data-qa="backup-care-row">
          <UnitName size={4} noMargin data-qa="unit">
            <a href={`/employee/units/${backupCare.unit.id}`}>
              {backupCare.unit.name}
            </a>
          </UnitName>
          <Period size={4} noMargin data-qa="period">
            {`${backupCare.period.start.format()} - ${backupCare.period.end.format()}`}
          </Period>

          <Toolbar
            dateRange={{
              startDate: backupCare.period.start,
              endDate: backupCare.period.end
            }}
            onEdit={() => toggleUiMode(`edit-backup-care-${backupCare.id}`)}
            dataQaEdit="btn-edit-backup-care"
            onDelete={() => toggleUiMode(`remove-backup-care-${backupCare.id}`)}
            editable={permittedActions.includes('UPDATE')}
            deletable={permittedActions.includes('DELETE')}
            dataQaDelete="btn-remove-backup-care"
          />
        </Row>
      )}
    </div>
  )
}
