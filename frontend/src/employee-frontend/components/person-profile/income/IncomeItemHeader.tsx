// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useRef } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Action } from 'lib-common/generated/action'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import Title from 'lib-components/atoms/Title'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'
import { faChevronDown, faChevronUp, faPen, faTrash } from 'lib-icons'

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

interface Props {
  title: string
  isOpen: boolean
  toggle: () => void
  editable: boolean
  toggleable?: boolean
  startEditing: () => void
  startDeleting: () => void
  permittedActions: Action.Income[]
  children?: React.JSX.Element[] | React.JSX.Element
}

const IncomeItemHeader = React.memo(function IncomeItemHeader({
  title: period,
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
    </Container>
  )
})

export default IncomeItemHeader
