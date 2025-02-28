import { CitizenMessageBody } from 'lib-common/generated/api-types/messaging';
import { GetReceiversResponse } from 'lib-common/generated/api-types/messaging';
import { MessageId } from 'lib-common/generated/api-types/shared';
import { MessageThreadId } from 'lib-common/generated/api-types/shared';
import { MyAccountResponse } from 'lib-common/generated/api-types/messaging';
import { PagedCitizenMessageThreads } from 'lib-common/generated/api-types/messaging';
import { ReplyToMessageBody } from 'lib-common/generated/api-types/messaging';
import { ThreadReply } from 'lib-common/generated/api-types/messaging';
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.archiveThread
*/
export declare function archiveThread(request: {
    threadId: MessageThreadId;
}): Promise<void>;
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getMyAccount
*/
export declare function getMyAccount(): Promise<MyAccountResponse>;
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getReceivedMessages
*/
export declare function getReceivedMessages(request: {
    page: number;
}): Promise<PagedCitizenMessageThreads>;
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getReceivers
*/
export declare function getReceivers(): Promise<GetReceiversResponse>;
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getUnreadMessages
*/
export declare function getUnreadMessages(): Promise<number>;
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.markThreadRead
*/
export declare function markThreadRead(request: {
    threadId: MessageThreadId;
}): Promise<void>;
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.newMessage
*/
export declare function newMessage(request: {
    body: CitizenMessageBody;
}): Promise<MessageThreadId>;
/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.replyToThread
*/
export declare function replyToThread(request: {
    messageId: MessageId;
    body: ReplyToMessageBody;
}): Promise<ThreadReply>;
