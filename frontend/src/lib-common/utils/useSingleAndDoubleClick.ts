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
    []
  )

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    clicks.current++
    callFunction(e)
  }

  const handleDoubleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    clicks.current++
  }

  return { handleClick, handleDoubleClick }
}
