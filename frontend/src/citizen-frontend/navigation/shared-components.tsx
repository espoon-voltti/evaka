// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useCallback, useEffect, useRef, useState } from 'react'
import { NavLink } from 'react-router-dom'
import styled, { css } from 'styled-components'

import {
  Lang,
  langs,
  useLang,
  useTranslation
} from 'citizen-frontend/localization'
import { fontWeights } from 'lib-components/typography'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { fasChevronDown, fasChevronUp } from 'lib-icons'

export const CircledChar = styled.div.attrs({
  className: 'circled-char'
})`
  height: ${defaultMargins.m};
  border: 1px solid ${colors.grayscale.g100};
  padding: 0 5px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 12px;
  min-width: ${defaultMargins.m};
`

const dropDownButtonStyles = css`
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: ${defaultMargins.zero} ${defaultMargins.xs};
  align-items: center;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: Open Sans;
  color: ${colors.grayscale.g100};
  font-size: 1.125rem;
  font-weight: ${fontWeights.semibold};
  line-height: 2rem;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-bottom: 4px solid transparent;

  &:hover {
    color: ${colors.main.m2Hover};

    .circled-char {
      border-color: ${colors.main.m2Hover};
    }
  }

  &.active {
    color: ${colors.main.m2};
    border-bottom-color: ${colors.main.m2};

    .circled-char {
      border-color: ${colors.main.m2};
    }
  }
`

export const DropDownButton = styled.button<{ $alignRight?: boolean }>`
  ${dropDownButtonStyles}
`

export const DropDownLink = styled(NavLink)<{ $alignRight?: boolean }>`
  ${dropDownButtonStyles}
  ${(p) => p.$alignRight && 'justify-content: flex-end;'}
`

export const DropDownLocalLink = styled.a`
  ${dropDownButtonStyles}
`

export const LanguageMenu = React.memo(function LanguageMenu({
  useShortLanguageLabel = false,
  alignRight = false
}: {
  useShortLanguageLabel?: boolean
  alignRight?: boolean
}) {
  const t = useTranslation()
  const [lang, setLang] = useLang()
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((state) => !state), [setOpen])
  const dropDownContainerRef = useCloseOnOutsideClick<HTMLDivElement>(() =>
    setOpen(false)
  )

  const dropDownRef = useRef<HTMLUListElement | null>(null)
  useEffect(() => {
    if (open && dropDownRef.current) {
      const firstSubItem = dropDownRef.current.querySelector('button')
      if (firstSubItem) {
        firstSubItem.focus()
      }
    }
  }, [open])

  return (
    <DropDownContainer ref={dropDownContainerRef}>
      <DropDownButton
        $alignRight={alignRight}
        onClick={toggleOpen}
        data-qa="button-select-language"
        aria-expanded={open}
        aria-haspopup="true"
      >
        {useShortLanguageLabel ? lang.toUpperCase() : t.header.lang[lang]}
        <DropDownIcon icon={open ? fasChevronUp : fasChevronDown} />
      </DropDownButton>
      {open ? (
        <DropDown ref={dropDownRef} $align="right" data-qa="select-lang">
          {langs.map((l: Lang) => (
            <DropDownButton
              key={l}
              className={classNames({ active: lang === l })}
              onClick={() => {
                setLang(l)
                setOpen(false)
              }}
              data-qa={`lang-${l}`}
              lang={l}
              role="menuitemradio"
              aria-checked={lang === l}
            >
              <span>{t.header.lang[l]}</span>
            </DropDownButton>
          ))}
        </DropDown>
      ) : null}
    </DropDownContainer>
  )
})

export const DropDownContainer = styled.nav`
  position: relative;
`

export const DropDownIcon = styled(FontAwesomeIcon)`
  height: 1em !important;
  width: 0.625em !important;
`

export const DropDown = styled.ul<{ $align: 'left' | 'right' }>`
  position: absolute;
  z-index: 30;
  list-style: none;
  margin: 0;
  padding: ${defaultMargins.m};
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  background: ${colors.grayscale.g0};
  box-shadow: 0 2px 6px 0 ${colors.grayscale.g15};
  min-width: 240px;
  max-width: 600px;
  width: max-content;
  ${({ $align }) =>
    $align === 'left'
      ? css`
          left: 0;
          align-items: flex-start;
          text-align: left;
        `
      : css`
          right: 0;
          align-items: flex-end;
          text-align: right;
        `}
`

export const DropDownInfo = React.memo(function DropDownInfo({
  children
}: {
  children: React.ReactNode
}) {
  return (
    <>
      <DropDownInfoBreak />
      <DropDownInfoContent>{children}</DropDownInfoContent>
    </>
  )
})

const DropDownInfoBreak = styled.span`
  flex-basis: 100%;
  height: 0;
`

const DropDownInfoContent = styled.span`
  font-weight: normal;
`
