// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext } from 'react'

import type { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import type { NekkuUnitNumber } from 'lib-common/generated/api-types/nekku'
import type LocalDate from 'lib-common/local-date'
import { cancelMutation } from 'lib-common/query'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { InfoBox, MessageBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import { theme } from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { faPen, fasExclamation } from 'lib-icons'

import { useTranslation } from '../../../../../state/i18n'
import { UIContext } from '../../../../../state/ui'
import { updateGroupMutation } from '../../../queries'
import { AROMI_CUSTOMER_ID_MAX_LENGTH } from '../GroupModal'

interface Props {
  group: DaycareGroup
  nekkuUnits: NekkuUnitNumber[]
}

export default React.memo(function GroupUpdateModal({
  group,
  nekkuUnits
}: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  const [data, setData] = useState<{
    name: string
    startDate: LocalDate | null
    endDate: LocalDate | null
    jamixCustomerNumber: number | null
    aromiCustomerId: string | null
    nekkuCustomerNumber: string | null
  }>({
    name: group.name,
    startDate: group.startDate,
    endDate: group.endDate,
    jamixCustomerNumber: group.jamixCustomerNumber,
    aromiCustomerId: group.aromiCustomerId,
    nekkuCustomerNumber: group.nekkuCustomerNumber
  })

  return (
    <MutateFormModal
      data-qa="group-update-modal"
      title={i18n.unit.groups.updateModal.title}
      icon={faPen}
      type="info"
      resolveMutation={updateGroupMutation}
      resolveAction={() =>
        data.startDate !== null
          ? {
              daycareId: group.daycareId,
              groupId: group.id,
              body: {
                ...data,
                startDate: data.startDate,
                name: data.name.trim(),
                aromiCustomerId:
                  data.aromiCustomerId !== null &&
                  data.aromiCustomerId.trim().length > 0
                    ? data.aromiCustomerId.trim()
                    : null
              }
            }
          : cancelMutation
      }
      resolveLabel={i18n.common.confirm}
      resolveDisabled={
        data.name.trim().length === 0 ||
        data.startDate === null ||
        data.endDate?.isBefore(data.startDate) ||
        (featureFlags.aromiIntegration &&
          data.aromiCustomerId !== null &&
          data.aromiCustomerId.trim().length > AROMI_CUSTOMER_ID_MAX_LENGTH)
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
          <DatePicker
            date={data.startDate}
            onChange={(startDate) =>
              setData((state) => ({ ...state, startDate }))
            }
            locale="fi"
            data-qa="start-date-input"
          />
          <Gap size="s" />
          <div className="bold">{i18n.unit.groups.updateModal.endDate}</div>
          <DatePicker
            date={data.endDate}
            onChange={(endDate) => setData((state) => ({ ...state, endDate }))}
            locale="fi"
            data-qa="end-date-input"
          />
          {featureFlags.jamixIntegration && (
            <>
              <Gap size="s" />
              <div className="bold">
                {i18n.unit.groups.updateModal.jamixTitle}
              </div>
              <InputField
                value={data.jamixCustomerNumber?.toString() ?? ''}
                onChange={(value) => {
                  if (/^\d*$/.exec(value)) {
                    const parsedNumber = parseInt(value)
                    setData((state) => ({
                      ...state,
                      jamixCustomerNumber: isNaN(parsedNumber)
                        ? null
                        : parsedNumber
                    }))
                  }
                }}
                data-qa="jamix-customer-id-input"
                placeholder={i18n.unit.groups.updateModal.jamixPlaceholder}
              />
            </>
          )}
          {featureFlags.aromiIntegration && (
            <>
              <Gap size="s" />
              <div className="bold">
                {i18n.unit.groups.updateModal.aromiTitle}
              </div>
              <InputField
                value={data.aromiCustomerId ?? ''}
                onChange={(value) => {
                  const checkedValue =
                    value !== null && value.length > 0 ? value : null
                  setData((state) => ({
                    ...state,
                    aromiCustomerId: checkedValue
                  }))
                }}
              />
              {(data.aromiCustomerId === null ||
                data.aromiCustomerId.trim().length === 0) && (
                <>
                  <Gap size="s" />
                  <MessageBox
                    color={theme.colors.status.warning}
                    icon={fasExclamation}
                    message={i18n.unit.groups.createModal.errors.aromiWarning}
                    thin
                  />
                </>
              )}
            </>
          )}
          {featureFlags.nekkuIntegration && (
            <>
              <Gap size="s" />
              <div className="bold">
                {i18n.unit.groups.updateModal.nekkuUnitTitle}
              </div>
              <Combobox
                clearable
                items={nekkuUnits}
                selectedItem={nekkuUnits.find(
                  (item) => item.number === data.nekkuCustomerNumber
                )}
                getItemLabel={(item) => (item ? item.name : '')}
                onChange={(option) =>
                  setData((prev) => ({
                    ...prev,
                    nekkuCustomerNumber: option?.number || null
                  }))
                }
              />
              <Gap size="s" />
              <div className="bold">
                {i18n.unit.groups.updateModal.nekkuCustomerNumberTitle}
              </div>
              <div>{data.nekkuCustomerNumber || '-'}</div>
            </>
          )}
        </section>
        <InfoBox message={i18n.unit.groups.updateModal.info} thin />
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
