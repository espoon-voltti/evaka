// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useRef } from 'react'
import styled from 'styled-components'

import { Action } from 'lib-common/generated/action'
import { EvakaUser } from 'lib-common/generated/api-types/user'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'
import { faChevronDown, faChevronUp, faPen, faTrash } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

const Container = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const ItemTitle = styled.div`
  cursor: pointer;
`

const Button = styled(IconOnlyButton)`
  &.button {
    margin-left: 20px;
  }
`

const ToggleButton = styled(Button)`
  color: ${colors.grayscale.g70};
`

const Row = styled(FixedSpaceRow)`
  align-items: center;
`

const RightHandSide = styled(Row)`
  min-width: 50%;
  justify-content: space-between;
`

interface Props {
  title: string
  isOpen: boolean
  toggle: () => void
  editable: boolean
  toggleable?: boolean
  startEditing: () => void
  startDeleting: () => void
  permittedActions: Action.Income[]
  modifiedBy?: EvakaUser
  modifiedAt?: HelsinkiDateTime
  children?: React.JSX.Element[] | React.JSX.Element
}

const IncomeItemHeader = React.memo(function IncomeItemHeader({
  title: period,
  modifiedBy,
  modifiedAt,
  isOpen,
  toggle,
  editable,
  toggleable,
  startEditing,
  permittedActions,
  startDeleting
}: Props) {
  const { i18n } = useTranslation()
  const elRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (isOpen) {
      scrollRefIntoView(elRef)
    }
  }, [isOpen])

  return (
    <Container ref={elRef}>
      <ItemTitle onClick={toggle}>
        <Title size={4}>
          <span>{period}</span>
        </Title>
      </ItemTitle>
      <RightHandSide>
        {modifiedAt && (
          <Row>
            <Tooltip
              tooltip={
                modifiedBy &&
                i18n.personProfile.income.lastModifiedBy(modifiedBy.name)
              }
            >
              {i18n.personProfile.income.lastModifiedAt(modifiedAt.format())}
            </Tooltip>
          </Row>
        )}
        <Row>
          {permittedActions.includes('UPDATE') && (
            <Button
              icon={faPen}
              onClick={() => {
                startEditing()
                if (!isOpen) toggle()
              }}
              disabled={!editable}
              data-qa="edit-income-item"
              aria-label={i18n.common.edit}
            />
          )}
          {permittedActions.includes('DELETE') && (
            <Button
              icon={faTrash}
              onClick={() => {
                startDeleting()
              }}
              disabled={!editable}
              data-qa="delete-income-item"
              aria-label={i18n.common.remove}
            />
          )}
          <ToggleButton
            icon={isOpen ? faChevronUp : faChevronDown}
            onClick={toggle}
            disabled={!toggleable}
            data-qa="toggle-income-item"
            aria-label={isOpen ? i18n.common.close : i18n.common.open}
          />
        </Row>
      </RightHandSide>
    </Container>
  )
})

export default IncomeItemHeader
