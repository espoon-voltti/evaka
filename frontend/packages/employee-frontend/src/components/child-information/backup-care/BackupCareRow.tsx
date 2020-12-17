// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { useTranslation } from '~/state/i18n'
import { UIContext } from '~state/ui'
import { faQuestion } from '@evaka/lib-icons'
import Title from '~components/shared/atoms/Title'
import InfoModal from '~components/common/InfoModal'
import { ChildContext } from '~state'
import { UUID } from '~types'
import BackupCareForm from '~components/child-information/backup-care/BackupCareForm'
import { getChildBackupCares, removeBackupCare } from 'api/child/backup-care'
import styled from 'styled-components'
import { ChildBackupCare } from '~types/child'
import Toolbar from 'components/shared/molecules/Toolbar'
import { ALL_ROLES_BUT_STAFF } from 'utils/roles'

export interface Props {
  childId: UUID
  backupCare: ChildBackupCare
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

export default function BackupCareRow({ childId, backupCare }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const { setBackupCares } = useContext(ChildContext)

  return (
    <div>
      {uiMode === `remove-backup-care-${backupCare.id}` && (
        <InfoModal
          iconColour={'orange'}
          title={i18n.childInformation.backupCares.remove}
          text={`${
            i18n.common.period
          } ${backupCare.period.start.format()} - ${backupCare.period.end.format()}`}
          resolveLabel={i18n.common.remove}
          rejectLabel={i18n.common.cancel}
          icon={faQuestion}
          reject={() => clearUiMode()}
          resolve={() =>
            removeBackupCare(backupCare.id)
              .then(() => clearUiMode())
              .then(() =>
                getChildBackupCares(childId).then((sn) => setBackupCares(sn))
              )
          }
        />
      )}
      {uiMode === `edit-backup-care-${backupCare.id}` ? (
        <BackupCareForm childId={childId} backupCare={backupCare} />
      ) : (
        <Row data-qa="backup-care-row">
          <UnitName size={4} data-qa="unit">
            <a href={`/employee/units/${backupCare.unit.id}`}>
              {backupCare.unit.name}
            </a>
          </UnitName>
          <Period size={4} data-qa="period">
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
            editableFor={ALL_ROLES_BUT_STAFF}
            deletableFor={ALL_ROLES_BUT_STAFF}
            dataQaDelete="btn-remove-backup-care"
          />
        </Row>
      )}
    </div>
  )
}
