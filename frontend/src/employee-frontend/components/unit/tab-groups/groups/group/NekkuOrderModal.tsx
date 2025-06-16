// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import type { GroupId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { cancelMutation } from 'lib-common/query'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { faUtensils } from 'lib-icons'

import { useTranslation } from '../../../../../state/i18n'
import { UIContext } from '../../../../../state/ui'
import { nekkuManualOrderMutation } from '../../../queries'

export default React.memo(function NekkuOrderModal({
  groupId,
  groupName
}: {
  groupId: GroupId
  groupName: string
}) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  const [date, setDate] = useState<LocalDate | null>(null)

  const lastOrderedDate = (today: LocalDate) => {
    return today.addDays(21 - today.getIsoDayOfWeek())
  }

  return (
    <MutateFormModal
      data-qa="nekku-order-modal"
      title={i18n.unit.groups.nekkuOrderModal.title + ' - ' + groupName}
      icon={faUtensils}
      type="info"
      resolveMutation={nekkuManualOrderMutation}
      resolveAction={() =>
        date !== null
          ? {
              groupId: groupId,
              date: date
            }
          : cancelMutation
      }
      resolveLabel={i18n.common.confirm}
      resolveDisabled={date === null}
      onSuccess={clearUiMode}
      rejectAction={clearUiMode}
      rejectLabel={i18n.common.cancel}
    >
      <DatePicker
        date={null}
        onChange={(selectedDate) => setDate(selectedDate)}
        minDate={LocalDate.todayInSystemTz().addDays(1)}
        maxDate={lastOrderedDate(LocalDate.todayInSystemTz())}
        locale="fi"
        data-qa="input-order-date"
      />
    </MutateFormModal>
  )
})
