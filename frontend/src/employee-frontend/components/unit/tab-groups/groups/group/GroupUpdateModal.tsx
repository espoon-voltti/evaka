// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext } from 'react'
import LocalDate from 'lib-common/local-date'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import {
  DatePickerDeprecated,
  DatePickerClearableDeprecated
} from 'lib-components/molecules/DatePickerDeprecated'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import { faPen } from 'lib-icons'
import { editGroup } from '../../../../../api/unit'
import { useTranslation } from '../../../../../state/i18n'
import { UIContext } from '../../../../../state/ui'
import { DaycareGroup } from '../../../../../types/unit'

interface Props {
  group: DaycareGroup
  reload: () => void
}

export default React.memo(function GroupUpdateModal({ group, reload }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  const [data, setData] = useState<{
    name: string
    startDate: LocalDate
    endDate: LocalDate | null
  }>({ name: group.name, startDate: group.startDate, endDate: group.endDate })

  const submitForm = () => {
    void editGroup(group.daycareId, group.id, {
      ...data,
      name: data.name.trim()
    }).then(() => {
      clearUiMode()
      reload()
    })
  }

  return (
    <FormModal
      data-qa="group-update-modal"
      title={i18n.unit.groups.updateModal.title}
      icon={faPen}
      iconColor="blue"
      resolveAction={submitForm}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={
        data.name.trim().length === 0 || data.endDate?.isBefore(data.startDate)
      }
      rejectAction={clearUiMode}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn>
        <section>
          <div className="bold">{i18n.unit.groups.updateModal.name}</div>
          <InputField
            value={data.name}
            onChange={(name) => setData((state) => ({ ...state, name }))}
            data-qa="name-input"
          />
          <Gap size="s" />
          <div className="bold">{i18n.unit.groups.updateModal.startDate}</div>
          <DatePickerDeprecated
            date={data.startDate}
            onChange={(startDate) =>
              setData((state) => ({ ...state, startDate }))
            }
            type="full-width"
            data-qa="start-date-input"
          />
          <Gap size="s" />
          <div className="bold">{i18n.unit.groups.updateModal.endDate}</div>
          <DatePickerClearableDeprecated
            date={data.endDate}
            onChange={(endDate) => setData((state) => ({ ...state, endDate }))}
            onCleared={() => setData((state) => ({ ...state, endDate: null }))}
            type="full-width"
            data-qa="end-date-input"
          />
        </section>
        <InfoBox message={i18n.unit.groups.updateModal.info} thin />
      </FixedSpaceColumn>
    </FormModal>
  )
})
