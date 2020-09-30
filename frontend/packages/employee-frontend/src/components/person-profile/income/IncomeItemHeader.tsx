// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createRef, RefObject, useEffect } from 'react'
import styled from 'styled-components'
import { faChevronLeft, faChevronDown, faPen, faTrash } from 'icon-set'
import { IconButton, Title } from '~components/shared/alpha'
import { EspooColours } from '~utils/colours'

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
  &.button {
    color: ${EspooColours.greyDark};
  }
`

interface Props {
  title: string
  toggled: boolean
  toggle: () => void
  editable: boolean
  startEditing: () => void
  startDeleting: () => void
  children?: JSX.Element[] | JSX.Element
}

const IncomeItemHeader = React.memo(function IncomeItemHeader({
  title: period,
  toggled,
  toggle,
  editable,
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
      <div>
        <Button
          icon={faPen}
          onClick={() => {
            startEditing()
            !toggled && toggle()
          }}
          disabled={!editable}
          dataQa="edit-income-item"
        />
        <Button
          icon={faTrash}
          onClick={() => {
            startDeleting()
          }}
          disabled={!editable}
          dataQa="delete-income-item"
        />
        <ToggleButton
          icon={toggled ? faChevronDown : faChevronLeft}
          onClick={toggle}
          dataQa="toggle-income-item"
        />
      </div>
    </Container>
  )
})

export default IncomeItemHeader
