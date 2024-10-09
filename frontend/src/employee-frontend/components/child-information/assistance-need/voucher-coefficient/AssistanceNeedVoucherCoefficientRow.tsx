// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { UIContext } from 'employee-frontend/state/ui'
import { Action } from 'lib-common/generated/action'
import { AssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Tooltip from 'lib-components/atoms/Tooltip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { LabelLike } from 'lib-components/typography'
import { faPen, faTrash } from 'lib-icons'

import { TimeBasedStatusChip } from '../../TimeBasedStatusChip'

import AssistanceNeedVoucherCoefficientForm from './AssistanceNeedVoucherCoefficientForm'

interface Props {
  childId: UUID
  voucherCoefficient: AssistanceNeedVoucherCoefficient
  activeCoefficient?: AssistanceNeedVoucherCoefficient
  setActiveCoefficient: (
    c: AssistanceNeedVoucherCoefficient | undefined
  ) => void
  permittedActions: Action.AssistanceNeedVoucherCoefficient[]
  coefficients: AssistanceNeedVoucherCoefficient[]
}

const TdWithoutTopPadding = styled(Td)`
  padding-top: 0;
`

export default React.memo(function AssistanceNeedVoucherCoefficientRow({
  childId,
  voucherCoefficient,
  activeCoefficient,
  setActiveCoefficient,
  permittedActions,
  coefficients
}: Props) {
  const { toggleUiMode, uiMode, clearUiMode } = useContext(UIContext)

  const { i18n } = useTranslation()
  const t = i18n.childInformation.assistanceNeedVoucherCoefficient

  const { coefficient, id, modifiedAt, modifiedBy, validityPeriod } =
    voucherCoefficient

  const isUpdating =
    uiMode === 'modify-assistance-need-voucher-coefficient' &&
    activeCoefficient?.id === id

  return (
    <>
      <Tr key={id} data-qa="table-assistance-need-voucher-coefficient">
        <Td
          minimalWidth
          topBorder
          data-qa="assistance-need-voucher-coefficient-coefficient"
        >
          {isUpdating ? (
            <ExpandingInfo info={<div>{t.form.titleInfo}</div>} width="full">
              <LabelLike>{t.form.editTitle}</LabelLike>
            </ExpandingInfo>
          ) : (
            <LabelLike>
              {Intl.NumberFormat('fi-FI', {
                minimumFractionDigits: 1
              }).format(coefficient)}
            </LabelLike>
          )}
        </Td>
        <Td
          maximalWidth
          topBorder
          data-qa="assistance-need-voucher-coefficient-validity-period"
        >
          {!isUpdating && (
            <div>
              {validityPeriod.start.format()} – {validityPeriod.end.format()}
            </div>
          )}
        </Td>
        <Td
          maximalWidth
          topBorder
          data-qa="assistance-need-voucher-coefficient-modified"
        >
          {!isUpdating && modifiedAt ? (
            <Tooltip
              tooltip={modifiedBy ? t.lastModifiedBy(modifiedBy.name) : null}
              position="left"
            >
              {modifiedAt.format()}
            </Tooltip>
          ) : (
            t.unknown
          )}
        </Td>
        <Td
          minimalWidth
          verticalAlign="middle"
          topBorder
          data-qa="assistance-need-voucher-coefficient-actions"
        >
          <FixedSpaceRow spacing="s" alignItems="center">
            {permittedActions.includes('UPDATE') && (
              <IconOnlyButton
                icon={faPen}
                onClick={() => {
                  setActiveCoefficient(voucherCoefficient)
                  toggleUiMode('modify-assistance-need-voucher-coefficient')
                }}
                data-qa="assistance-need-voucher-coefficient-edit-btn"
                aria-label={i18n.common.edit}
              />
            )}
            {permittedActions.includes('DELETE') && (
              <IconOnlyButton
                icon={faTrash}
                onClick={() => {
                  setActiveCoefficient(voucherCoefficient)
                  toggleUiMode('delete-assistance-need-voucher-coefficient')
                }}
                data-qa="assistance-need-voucher-coefficient-delete-btn"
                aria-label={i18n.common.remove}
              />
            )}
          </FixedSpaceRow>
        </Td>
        <Td minimalWidth topBorder>
          <TimeBasedStatusChip
            status={
              validityPeriod.start.isAfter(LocalDate.todayInHelsinkiTz())
                ? 'UPCOMING'
                : validityPeriod.end.isBefore(LocalDate.todayInHelsinkiTz())
                  ? 'ENDED'
                  : 'ACTIVE'
            }
            data-qa="assistance-need-voucher-coefficient-status"
          />
        </Td>
      </Tr>
      {isUpdating && (
        <Tr data-qa="table-assistance-need-voucher-coefficient-editor">
          <TdWithoutTopPadding colSpan={4}>
            <AssistanceNeedVoucherCoefficientForm
              childId={childId}
              coefficient={voucherCoefficient}
              coefficients={coefficients}
              onSuccess={() => {
                clearUiMode()
                setActiveCoefficient(undefined)
              }}
            />
          </TdWithoutTopPadding>
        </Tr>
      )}
    </>
  )
})
