// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  volttiEnv,
  serviceName,
  appCommit,
  tracingEnabled,
  traceAgentHostname,
  traceAgentPort,
  profilingEnabled
} from './shared/config.js'
import tracer from 'dd-trace'

if (tracingEnabled) {
  tracer.tracer.init({
    env: volttiEnv,
    service: serviceName,
    version: appCommit,
    hostname: traceAgentHostname,
    port: traceAgentPort,
    profiling: profilingEnabled,
    logInjection: true,
    runtimeMetrics: true
  }) // initialized in a different file to avoid hoisting.
}

export default tracer.tracer
