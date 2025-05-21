// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ReactNode } from 'react'
import React, { useCallback, useRef, useState } from 'react'
import styled from 'styled-components'

import { scrollToRef } from 'lib-common/utils/scrolling'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useLang, useTranslation } from '../../localization'

type Props = {
  title: string
  validationErrors: number
  openInitially?: boolean
  children: ReactNode
  'data-qa'?: string
}

export default React.memo(function EditorSection(props: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()
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
          <TitleWrapper>
            <H2 noMargin>{props.title}</H2>
            {props.validationErrors !== 0 && (
              <div>
                <ErrorsIcon
                  content={props.validationErrors.toString()}
                  size="m"
                  color={colors.status.warning}
                  aria-hidden="true"
                />
                <ErrorsDescription lang={lang}>
                  {i18n.applications.editor.heading.invalidFields(
                    props.validationErrors
                  )}
                </ErrorsDescription>
              </div>
            )}
          </TitleWrapper>
        }
        opaque
        paddingVertical="L"
      >
        {props.children}
      </CollapsibleContentArea>
    </div>
  )
})

const TitleWrapper = styled.div`
  flex-grow: 1;
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

const ErrorsDescription = styled.p`
  border: 0;
  clip: rect(0 0 0 0);
  height: 1px;
  width: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  position: absolute;
`
