// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode, useCallback, useRef, useState } from 'react'
import styled from 'styled-components'
import colors from 'lib-components/colors'
import { H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import RoundIcon from 'lib-components/atoms/RoundIcon'

type Props = {
  title: string
  validationErrors: number
  openInitially?: boolean
  children: ReactNode
  'data-qa'?: string
}

export default React.memo(function EditorSection(props: Props) {
  const [open, setOpen] = useState(props.openInitially === true)
  const ref = useRef<HTMLDivElement>(null)
  const toggleOpen = useCallback(() => {
    setOpen((previous) => !previous)
    setTimeout(() => {
      if (ref.current) {
        window.scrollTo({
          left: 0,
          top: ref.current.offsetTop,
          behavior: 'smooth'
        })
      }
    }, 50)
  }, [ref.current])

  return (
    <div ref={ref}>
      <CollapsibleContentArea
        data-qa={props['data-qa']}
        open={open}
        toggleOpen={toggleOpen}
        title={
          <>
            <TitleWrapper>
              <H2 noMargin>{props.title}</H2>
              {props.validationErrors !== 0 ? (
                <ErrorsIcon
                  content={props.validationErrors.toString()}
                  size="m"
                  color={colors.accents.orange}
                  aria-hidden="true"
                />
              ) : null}
            </TitleWrapper>
          </>
        }
        opaque
        paddingVertical="L"
        validationErrors={props.validationErrors}
      >
        {props.children}
      </CollapsibleContentArea>
    </div>
  )
})

const TitleWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
`

const ErrorsIcon = styled(RoundIcon)`
  margin: 0 ${defaultMargins.s};
`
