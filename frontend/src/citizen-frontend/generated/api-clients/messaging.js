// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { client } from '../../api-client';
import { createUrlSearchParams } from 'lib-common/api';
import { deserializeJsonGetReceiversResponse } from 'lib-common/generated/api-types/messaging';
import { deserializeJsonPagedCitizenMessageThreads } from 'lib-common/generated/api-types/messaging';
import { deserializeJsonThreadReply } from 'lib-common/generated/api-types/messaging';
import { uri } from 'lib-common/uri';
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.archiveThread
*/
export async function archiveThread(request) {
    const { data: json } = await client.request({
        url: uri `/citizen/messages/threads/${request.threadId}/archive`.toString(),
        method: 'PUT'
    });
    return json;
}
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getMyAccount
*/
export async function getMyAccount() {
    const { data: json } = await client.request({
        url: uri `/citizen/messages/my-account`.toString(),
        method: 'GET'
    });
    return json;
}
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getReceivedMessages
*/
export async function getReceivedMessages(request) {
    const params = createUrlSearchParams(['page', request.page.toString()]);
    const { data: json } = await client.request({
        url: uri `/citizen/messages/received`.toString(),
        method: 'GET',
        params
    });
    return deserializeJsonPagedCitizenMessageThreads(json);
}
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getReceivers
*/
export async function getReceivers() {
    const { data: json } = await client.request({
        url: uri `/citizen/messages/receivers`.toString(),
        method: 'GET'
    });
    return deserializeJsonGetReceiversResponse(json);
}
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getUnreadMessages
*/
export async function getUnreadMessages() {
    const { data: json } = await client.request({
        url: uri `/citizen/messages/unread-count`.toString(),
        method: 'GET'
    });
    return json;
}
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.markThreadRead
*/
export async function markThreadRead(request) {
    const { data: json } = await client.request({
        url: uri `/citizen/messages/threads/${request.threadId}/read`.toString(),
        method: 'PUT'
    });
    return json;
}
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.newMessage
*/
export async function newMessage(request) {
    const { data: json } = await client.request({
        url: uri `/citizen/messages`.toString(),
        method: 'POST',
        data: request.body
    });
    return json;
}
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.replyToThread
*/
export async function replyToThread(request) {
    const { data: json } = await client.request({
        url: uri `/citizen/messages/${request.messageId}/reply`.toString(),
        method: 'POST',
        data: request.body
    });
    return deserializeJsonThreadReply(json);
}
//# sourceMappingURL=messaging.js.map