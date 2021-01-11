// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import FormModal from '@evaka/lib-components/src/molecules/modals/FormModal'
import { Gap } from '@evaka/lib-components/src/white-space'
import { faPen } from '@evaka/lib-icons'
import { DaycareGroup } from '~types/unit'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import { editGroup } from '~api/unit'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import {
  DatePicker,
  DatePickerClearable
} from '@evaka/lib-components/src/molecules/DatePicker'
import { InfoBox } from '@evaka/lib-components/src/molecules/MessageBoxes'

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
      iconColour={'blue'}
      resolve={{
        action: submitForm,
        label: i18n.common.confirm,
        disabled: data.name.trim().length === 0
      }}
      reject={{
        action: clearUiMode,
        label: i18n.common.cancel
      }}
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
          <DatePicker
            date={data.startDate}
            onChange={(startDate) =>
              setData((state) => ({ ...state, startDate }))
            }
            type="full-width"
            dataQa="start-date-input"
          />
          <Gap size="s" />
          <div className="bold">{i18n.unit.groups.updateModal.endDate}</div>
          <DatePickerClearable
            date={data.endDate}
            onChange={(endDate) => setData((state) => ({ ...state, endDate }))}
            onCleared={() => setData((state) => ({ ...state, endDate: null }))}
            type="full-width"
            dataQa="end-date-input"
          />
        </section>
        <InfoBox message={i18n.unit.groups.updateModal.info} thin />
      </FixedSpaceColumn>
    </FormModal>
  )
})
