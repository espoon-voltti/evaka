import { useEffect } from 'react'
import { useHistory } from 'react-router-dom'

/**
 * Keeps current URL's query params up to date with the queryParams object
 * Use a memoized value for the queryParams object because history.replace causes a rerender
 */
export function useSyncQueryParams(queryParams: Record<string, string>) {
  const history = useHistory()
  useEffect(() => {
    const { pathname } = history.location
    const query = new URLSearchParams(queryParams).toString()
    history.replace(`${pathname}?${query}`)
  }, [history, queryParams])
}
