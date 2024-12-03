declare const id: unique symbol
export type Id<B extends string> = string & { [id]: B }
