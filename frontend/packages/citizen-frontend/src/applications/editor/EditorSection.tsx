import React, { ReactNode, useCallback, useState } from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { H2 } from '@evaka/lib-components/src/typography'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { CollapsibleContentArea } from '@evaka/lib-components/src/layout/Container'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'

type Props = {
  title: string
  validationErrors: number
  openInitially?: boolean
  children: ReactNode
}

export default React.memo(function EditorSection(props: Props) {
  const [open, setOpen] = useState(props.openInitially === true)
  const toggleOpen = useCallback(() => setOpen((previous) => !previous), [])

  return (
    <CollapsibleContentArea
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
              />
            ) : null}
          </TitleWrapper>
        </>
      }
      opaque
      paddingVertical="L"
    >
      {props.children}
    </CollapsibleContentArea>
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
