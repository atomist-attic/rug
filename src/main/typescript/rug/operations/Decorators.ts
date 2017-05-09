import { PathExpression } from "../tree/PathExpression";
import { BaseParameter } from "./RugOperation";

// used by annotation functions

function set_metadata(obj: any, key: string, value: any) {
  Object.defineProperty(obj, key,
    {
      value,
      writable: false,
      enumerable: false,
      configurable: true,
    });
}

function get_metadata(obj: any, key: string) {
  let desc = Object.getOwnPropertyDescriptor(obj, key);
  if ((desc == null || desc === undefined) && (obj.prototype !== undefined)) {
    desc = Object.getOwnPropertyDescriptor(obj.prototype, key);
  }
  if (desc != null || desc !== undefined) {
    return desc.value;
  }
  return null;
}

/**
 * Decorator for parameters. Adds to object properties
 */
function Parameter(details: BaseParameter) {
  return (target: any, propertyKey: string) => {
    let params = get_metadata(target, "__parameters");
    if (params == null) {
      params = [];
    }
    const copy: any = { ...details };
    copy.name = propertyKey;
    copy.decorated = true;
    params.push(copy);
    set_metadata(target, "__parameters", params);
  };
}
/**
 * Map a local field to some other configuration item in a different system
 */
function MappedParameter(foreignKey: string) {
  return (target: any, localKey: string) => {
    let params = get_metadata(target, "__mappedParameters");
    if (params == null) {
      params = [];
    }
    const param = { localKey, foreignKey };
    params.push(param);
    set_metadata(target, "__mappedParameters", params);
  };
}

/**
 * Decorator for editors. Use this instead of implementing the editor interface.
 */

function ruglike(fn: string, kind: string, msg: string) {
  return (nameOrDescription: string, description?: string) => {
    // tslint:disable-next-line:ban-types
    return (ctr: Function) => {
      if (typeof ctr.prototype[fn] !== "function") {
        throw new Error(`${msg}`);
      }
      if (description === undefined) {
        set_metadata(ctr.prototype, "__description", nameOrDescription);
      } else {
        set_metadata(ctr.prototype, "__description", description);
        set_metadata(ctr.prototype, "__name", nameOrDescription);
      }
      ctr.prototype.__kind = kind;
    };
  };
}

const Generator = ruglike(
  "populate",
  "generator",
  "populate must be a function with first parameter = Project");

const Editor = ruglike(
  "edit",
  "editor",
  "edit must be a function with first parameter = Project");

const CommandHandler = ruglike(
  "handle",
  "command-handler",
  "handle must be a function with first parameter = HandlerContext");
const ResponseHandler = ruglike(
  "handle",
  "response-handler",
  "handle must be a function with first parameter = Response<T>");

const EventHandler = (name: string, description: string, expression: PathExpression<any, any> | string) => {
  // tslint:disable-next-line:ban-types
  return (ctr: Function) => {
    if (typeof ctr.prototype.handle !== "function") {
      throw new Error("handle must be a function with first parameter = Match<R,N>");
    }
    set_metadata(ctr.prototype, "__name", name);
    set_metadata(ctr.prototype, "__description", description);
    set_metadata(ctr.prototype, "__kind", "event-handler");
    if (typeof expression === "string") {
      set_metadata(ctr.prototype, "__expression", expression);
    } else {
      set_metadata(ctr.prototype, "__expression", expression.expression);
    }
  };
};

/**
 * Decorator for tags. Sets tags on the class
 */

function Tags(...tags: string[]) {
  return (target: any) => {
    set_metadata(target.prototype, "__tags", tags);
  };
}

function Intent(...intent: string[]) {
  return (target: any) => {
    set_metadata(target.prototype, "__intent", intent);
  };
}

function Secrets(...secrets: string[]) {
  return (target: any) => {
    let current = get_metadata(target.prototype, "__secrets");
    if (current == null) {
      current = [];
    }
    set_metadata(target.prototype, "__secrets", current.concat(secrets));
  };
}

// for parameters to ResponseHandlers to do response body coercion
function ParseJson(target: object, propertyKey: string | symbol, parameterIndex: number) {
  set_metadata(target, "__coercion", "JSON");
}

export { Parameter, Secrets, Tags, Intent, MappedParameter };
export { Editor, Generator };
export { ParseJson };
export { ResponseHandler, CommandHandler, EventHandler };
