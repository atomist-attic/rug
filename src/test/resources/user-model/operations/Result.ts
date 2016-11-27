
/**
 * Status of an operation.
 */
export enum Status {
    Success,
    NoChange,
    Error
}

/**
 * Result of running an editor
 */
export class Result {

    constructor(
        public status: Status,
        public message: string = "") {}

}
