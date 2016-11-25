
export enum Status {
    Success,
    NoChange,
    Error
}

/**
 * Result of running an editor
 */
class Result {

    constructor(
        public status: Status,
        public message: string = "") {}

}

export {Result}
