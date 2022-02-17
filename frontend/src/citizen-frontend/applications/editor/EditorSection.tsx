// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode, useCallback, useRef, useState } from 'react'
import styled from 'styled-components'

import { scrollToRef } from 'lib-common/utils/scrolling'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

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
    scrollToRef(ref, 50)
  }, [])

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
                  color={colors.status.warning}
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
