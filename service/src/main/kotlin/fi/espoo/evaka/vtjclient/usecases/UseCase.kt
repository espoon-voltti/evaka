// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.usecases

interface IUseCase

interface UseCaseWithResult<R> : IUseCase {
    fun exec(): R
}
