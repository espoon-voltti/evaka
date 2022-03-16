// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  volttiEnv,
  serviceName,
  appCommit,
  tracingEnabled,
  traceAgentHostname,
  traceAgentPort
} from './shared/config'
import tracer from 'dd-trace'

if (tracingEnabled) {
  tracer.init({
    env: volttiEnv,
    service: serviceName,
    version: appCommit,
    hostname: traceAgentHostname,
    port: traceAgentPort,
    logInjection: true,
    runtimeMetrics: true
  }) // initialized in a different file to avoid hoisting.
}

export default tracer
