// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext } from 'react'

import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import {
  DatePickerDeprecated,
  DatePickerClearableDeprecated
} from 'lib-components/molecules/DatePickerDeprecated'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import { faPen } from 'lib-icons'

import { useTranslation } from '../../../../../state/i18n'
import { UIContext } from '../../../../../state/ui'
import { updateGroupMutation } from '../../../queries'

interface Props {
  group: DaycareGroup
}

export default React.memo(function GroupUpdateModal({ group }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  const [data, setData] = useState<{
    name: string
    startDate: LocalDate
    endDate: LocalDate | null
    jamixCustomerId: string | null
  }>({
    name: group.name,
    startDate: group.startDate,
    endDate: group.endDate,
    jamixCustomerId: group.jamixCustomerId
  })

  return (
    <MutateFormModal
      data-qa="group-update-modal"
      title={i18n.unit.groups.updateModal.title}
      icon={faPen}
      type="info"
      resolveMutation={updateGroupMutation}
      resolveAction={() => ({
        daycareId: group.daycareId,
        groupId: group.id,
        body: {
          ...data,
          name: data.name.trim()
        }
      })}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={
        data.name.trim().length === 0 || data.endDate?.isBefore(data.startDate)
      }
      onSuccess={clearUiMode}
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
          {featureFlags.jamixIntegration && (
            <>
              <Gap size="s" />
              <div className="bold">
                {i18n.unit.groups.updateModal.jamixTitle}
              </div>
              <InputField
                value={data.jamixCustomerId ?? ''}
                onChange={(jamixCustomerId) =>
                  setData((state) => ({ ...state, jamixCustomerId }))
                }
                data-qa="jamix-customer-id-input"
                placeholder={i18n.unit.groups.updateModal.jamixPlaceholder}
              />
            </>
          )}
        </section>
        <InfoBox message={i18n.unit.groups.updateModal.info} thin />
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
