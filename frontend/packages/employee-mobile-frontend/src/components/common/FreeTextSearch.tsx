// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faSearch, faTimes } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'

const SearchInputContainer = styled.div`
  height: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
`

const SearchInput = styled.input<{ background?: string }>`
  width: 100%;
  border: none;
  font-size: 1rem;
  background: ${(p) => p.background ?? colors.greyscale.lightest};
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  padding: 0.75rem;
  padding-left: 55px;
  font-size: 17px;
  outline: none;
  margin-left: -38px;
  margin-right: -25px;
  color: ${colors.greyscale.darkest};

  &::placeholder {
    font-style: italic;
    font-size: 17px;
    color: ${colors.greyscale.dark};
  }

  &:focus {
    border-width: 2px;
    border-radius: 2px;
    border-style: solid;
    border-color: ${colors.accents.petrol};
    margin-top: -2px;
    padding-left: 53px;
    margin-bottom: -2px;
  }
`

const CustomIcon = styled(FontAwesomeIcon)`
  color: ${colors.greyscale.dark};
  margin: 0 0.5rem;
  position: relative;
  left: 10px;
  font-size: 22px;
`

const CustomIconButton = styled(IconButton)`
  float: right;
  position: relative;
  color: ${colors.greyscale.medium};
  right: 20px;
`

type FreeTextSearchProps = {
  value: string
  setValue: (s: string) => void
  placeholder: string
  background?: string
}

export default function FreeTextSearch({
  value,
  setValue,
  placeholder,
  background
}: FreeTextSearchProps) {
  const clear = useCallback(() => setValue(''), [setValue])

  return (
    <SearchInputContainer>
      <CustomIcon icon={faSearch} />
      <SearchInput
        placeholder={placeholder}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        data-qa="free-text-search-input"
        background={background}
      ></SearchInput>
      <CustomIconButton icon={faTimes} onClick={clear} size={'m'} />
    </SearchInputContainer>
  )
}
