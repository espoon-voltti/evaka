import axios, { AxiosInstance } from 'axios'
import { differenceInMinutes } from 'date-fns'

export interface IdleTrackerOptions {
  thresholdInMinutes: number
}

export function idleTracker(
  client: AxiosInstance,
  onVisibleAfterIdle: () => void,
  { thresholdInMinutes }: IdleTrackerOptions = {
    thresholdInMinutes: 20
  }
): () => void {
  let lastResponse = new Date()
  let visibilityState = document.visibilityState

  function onVisibilityChange() {
    if (visibilityState === document.visibilityState) return
    const now = new Date()
    visibilityState = document.visibilityState
    if (
      visibilityState === 'visible' &&
      differenceInMinutes(now, lastResponse) >= thresholdInMinutes
    ) {
      onVisibleAfterIdle()
    }
  }
  document.addEventListener('visibilitychange', onVisibilityChange)

  const id = client.interceptors.response.use(
    (res) => {
      lastResponse = new Date()
      return res
    },
    (err: unknown) => {
      if (axios.isAxiosError(err) && err.response) {
        lastResponse = new Date()
      }
      return Promise.reject(err)
    }
  )
  return () => {
    document.removeEventListener('visibilitychange', onVisibilityChange)
    client.interceptors.response.eject(id)
  }
}
