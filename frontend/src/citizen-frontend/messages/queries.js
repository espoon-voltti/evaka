// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { Queries } from 'lib-common/query';
import { archiveThread, getMyAccount, getReceivedMessages, getReceivers, getUnreadMessages, markThreadRead, newMessage, replyToThread } from '../generated/api-clients/messaging';
const q = new Queries();
export const receivedMessagesQuery = q.pagedInfiniteQuery(() => (page) => getReceivedMessages({ page }), (thread) => thread.id);
export const receiversQuery = q.query(getReceivers);
export const messageAccountQuery = q.query(getMyAccount);
export const unreadMessagesCountQuery = q.query(getUnreadMessages);
export const markThreadReadMutation = q.mutation(markThreadRead, [
    unreadMessagesCountQuery
]);
export const sendMessageMutation = q.mutation(newMessage, [
    receivedMessagesQuery
]);
export const replyToThreadMutation = q.mutation(replyToThread, [
    receivedMessagesQuery
]);
export const archiveThreadMutation = q.mutation(archiveThread, [
    receivedMessagesQuery
]);
//# sourceMappingURL=queries.js.map