// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-mobile-frontend/common/i18n'
import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft, faTimes } from 'lib-icons'

const SearchInputContainer = styled.div`
  height: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: ${defaultMargins.xs};
`

const SearchInput = styled.input<{ background?: string; showClose: boolean }>`
  width: 100%;
  border: none;
  font-size: 1rem;
  background: ${(p) => p.background ?? colors.grayscale.g4};
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  padding: 0.75rem;
  padding-left: 55px;
  font-size: 17px;
  outline: none;
  margin-left: -38px;
  margin-right: ${(p) => (p.showClose ? '-25px' : '0')};
  color: ${colors.grayscale.g100};
  height: 100%;

  &::placeholder {
    color: ${colors.grayscale.g70};
  }

  &:focus {
    border-width: 2px;
    border-radius: 2px;
    border-style: solid;
    border-color: ${colors.main.m2Focus};
    padding-left: 53px;
  }
`

const CustomIcon = styled(FontAwesomeIcon)`
  color: ${colors.grayscale.g70};
  margin: 0 0.5rem;
  position: relative;
  left: 10px;
  font-size: 22px;
  cursor: pointer;
`

const CustomIconButton = styled(IconButton)`
  float: right;
  position: relative;
  color: ${colors.grayscale.g35};
  right: 20px;
`

type FreeTextSearchProps = {
  value: string
  setValue: (s: string) => void
  placeholder: string
  background?: string
  setShowSearch: (show: boolean) => void
  searchResults: AttendanceChild[]
}

export default function FreeTextSearch({
  value,
  setValue,
  placeholder,
  background,
  setShowSearch,
  searchResults
}: FreeTextSearchProps) {
  const clear = useCallback(() => setValue(''), [setValue])
  const { i18n } = useTranslation()

  return (
    <SearchInputContainer>
      <CustomIcon icon={faArrowLeft} onClick={() => setShowSearch(false)} />
      <SearchInput
        placeholder={placeholder}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        data-qa="free-text-search-input"
        background={background}
        showClose={searchResults.length > 1}
      />
      {searchResults.length > 1 && (
        <CustomIconButton
          icon={faTimes}
          onClick={clear}
          size="m"
          aria-label={i18n.common.clear}
        />
      )}
    </SearchInputContainer>
  )
}
