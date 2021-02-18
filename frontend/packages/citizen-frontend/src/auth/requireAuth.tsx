import React, { useContext } from 'react'
import { AuthContext } from '~auth/state'
import { RouteComponentProps } from 'react-router'
import { Redirect } from 'react-router-dom'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'

export default function requireAuth<T>(
  WrappedComponent: React.ComponentType<RouteComponentProps<T>>
) {
  return function WithRequireAuth(props: RouteComponentProps<T>) {
    const { user, loading } = useContext(AuthContext)

    return user ? (
      <WrappedComponent {...props} />
    ) : loading ? (
      <SpinnerSegment />
    ) : (
      <Redirect to={'/'} />
    )
  }
}
