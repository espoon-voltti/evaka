// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useRef } from 'react'
import { debounce } from 'lodash'

const DELAY = 100

export const useSingleAndDoubleClick = (
  onClick: (e: React.MouseEvent<HTMLDivElement>) => void,
  onDoubleClick: (e: React.MouseEvent<HTMLDivElement>) => void
) => {
  const clicks = useRef(0)

  const callFunction = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) =>
      debounce(() => {
        clicks.current === 3 ? onDoubleClick(e) : onClick(e)
        clicks.current = 0
      }, DELAY),
    [] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    clicks.current++
    callFunction(e)
  }

  const handleDoubleClick = () => {
    clicks.current++
  }

  return { handleClick, handleDoubleClick }
}
