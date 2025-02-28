export declare const receivedMessagesQuery: import("lib-common/query").PagedInfiniteQueriesQuery<[], import("../../lib-common/generated/api-types/messaging").CitizenMessageThread, import("../../lib-common/generated/api-types/shared").MessageThreadId>;
export declare const receiversQuery: import("lib-common/query").QueriesQuery<[], import("../../lib-common/generated/api-types/messaging").GetReceiversResponse>;
export declare const messageAccountQuery: import("lib-common/query").QueriesQuery<[], import("../../lib-common/generated/api-types/messaging").MyAccountResponse>;
export declare const unreadMessagesCountQuery: import("lib-common/query").QueriesQuery<[], number>;
export declare const markThreadReadMutation: import("lib-common/query").MutationDescription<{
    threadId: import("../../lib-common/generated/api-types/shared").MessageThreadId;
}, void>;
export declare const sendMessageMutation: import("lib-common/query").MutationDescription<{
    body: import("../../lib-common/generated/api-types/messaging").CitizenMessageBody;
}, import("../../lib-common/generated/api-types/shared").MessageThreadId>;
export declare const replyToThreadMutation: import("lib-common/query").MutationDescription<{
    messageId: import("../../lib-common/generated/api-types/shared").MessageId;
    body: import("../../lib-common/generated/api-types/messaging").ReplyToMessageBody;
}, import("../../lib-common/generated/api-types/messaging").ThreadReply>;
export declare const archiveThreadMutation: import("lib-common/query").MutationDescription<{
    threadId: import("../../lib-common/generated/api-types/shared").MessageThreadId;
}, void>;
