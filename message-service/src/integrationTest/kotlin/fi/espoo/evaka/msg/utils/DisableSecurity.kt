// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.utils

import org.springframework.test.context.TestPropertySource

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@TestPropertySource(value = ["classpath:disable-security.properties"])
annotation class DisableSecurity
