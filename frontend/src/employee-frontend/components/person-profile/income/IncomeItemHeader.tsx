// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import IconButton from 'lib-components/atoms/buttons/IconButton'
import Title from 'lib-components/atoms/Title'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'
import { faChevronDown, faChevronUp, faPen, faTrash } from 'lib-icons'
import React, { useEffect, useRef } from 'react'
import styled from 'styled-components'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'

const Container = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const ItemTitle = styled.div`
  cursor: pointer;
`

const Button = styled(IconButton)`
  &.button {
    margin-left: 20px;
  }
`

const ToggleButton = styled(Button)`
  color: ${colors.greyscale.dark};
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
  children?: JSX.Element[] | JSX.Element
}

const IncomeItemHeader = React.memo(function IncomeItemHeader({
  title: period,
  isOpen,
  toggle,
  editable,
  toggleable,
  startEditing,
  startDeleting
}: Props) {
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
        <Button
          icon={faPen}
          onClick={() => {
            startEditing()
            !isOpen && toggle()
          }}
          disabled={!editable}
          data-qa="edit-income-item"
        />
        <Button
          icon={faTrash}
          onClick={() => {
            startDeleting()
          }}
          disabled={!editable}
          data-qa="delete-income-item"
        />
        <ToggleButton
          icon={isOpen ? faChevronUp : faChevronDown}
          onClick={toggle}
          disabled={!toggleable}
          data-qa="toggle-income-item"
        />
      </Row>
    </Container>
  )
})

export default IncomeItemHeader
