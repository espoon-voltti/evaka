// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createRef, RefObject, useEffect } from 'react'
import styled from 'styled-components'
import { faChevronLeft, faChevronDown, faPen, faTrash } from 'lib-icons'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Title from 'lib-components/atoms/Title'
import colors from 'lib-customizations/common'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

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
  toggled: boolean
  toggle: () => void
  editable: boolean
  toggleable?: boolean
  startEditing: () => void
  startDeleting: () => void
  children?: JSX.Element[] | JSX.Element
}

const IncomeItemHeader = React.memo(function IncomeItemHeader({
  title: period,
  toggled,
  toggle,
  editable,
  toggleable,
  startEditing,
  startDeleting
}: Props) {
  const elRef: RefObject<HTMLDivElement> = createRef()

  useEffect(() => {
    if (toggled && elRef.current) {
      elRef.current.scrollIntoView({
        behavior: 'smooth'
      })
    }
  }, [toggled])

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
            !toggled && toggle()
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
          icon={toggled ? faChevronDown : faChevronLeft}
          onClick={toggle}
          disabled={!toggleable}
          data-qa="toggle-income-item"
        />
      </Row>
    </Container>
  )
})

export default IncomeItemHeader
