import { PathExpression } from "../tree/PathExpression";
import { BaseParameter } from "./RugOperation";

function set_metadata(obj: any, key: string, value: any) {
  let target = obj;
  if (obj.prototype !== undefined) {
    // should only be true for class Decorators
    target = obj.prototype;
  }
  Object.defineProperty(target, key,
    {
      value,
      writable: false,
      enumerable: false,
      configurable: false,
    });
}

function get_metadata(obj: any, key: string) {
  if (obj == null) {
    return null;
  }
  let desc = Object.getOwnPropertyDescriptor(obj, key);
  if ((desc == null || desc === undefined) && (Object.getPrototypeOf(obj) !== undefined)) {
    desc = get_metadata(Object.getPrototypeOf(obj), key);
  }
  if (desc != null && desc !== undefined) {
    return desc.value;
  }
  return null;
}

/**
 * Decorator for parameters. Adds to object properties
 */
export function Parameter(details: BaseParameter) {
  return (target: any, propertyKey: string) => {
    declareParameter(target, propertyKey, details);
  };
}

export function declareParameter(target: any, propertyKey: string, details: BaseParameter) {
  let params: any[] = get_metadata(target, "__parameters");
  if (params == null) {
    params = [];
  } else {
    // remove any that have the same name already (i.e. if folk are calling declareParameter)
    // use a cheeky method so that we can reuse the same array
    const found: any[] = params.filter((p) => p.name === propertyKey);
    if (found != null && found.length > 0) {
      const index = params.indexOf(found[0]);
      params.splice(index, 1);
    }
  }
  const copy: any = { ...details };
  copy.name = propertyKey;
  copy.decorated = true;
  params.push(copy);

  // merge parameters from parent if it has some
  const protoParams: any[] = get_metadata(Object.getPrototypeOf(target), "__parameters");
  if (protoParams != null) {
    protoParams.forEach((protoParam) => {
      // if we don't already have a parameter with the same name
      if (!params.some((param) => param.name === protoParam.name)) {
        params.push(protoParam);
      }
    });
  }

  set_metadata(target, "__parameters", params);
  return target;
}
/**
 * Map a local field to some other configuration item in a different system
 */
export function MappedParameter(foreignKey: string) {
  return (target: any, localKey: string) => {
    declareMappedParameter(target, localKey, foreignKey);
  };
}

export function declareMappedParameter(target: any, localKey: string, foreignKey: string) {
  let params = get_metadata(target, "__mappedParameters");
  if (params == null) {
    params = [];
  } else {
    // remove any that have the same name already (i.e. if folk are calling declareParameter)
    // use a cheeky method so that we can reuse the same array
    const found: any[] = params.filter((p) => p.localKey === localKey);
    if (found != null && found.length > 0) {
      const index = params.indexOf(found[0]);
      params.splice(index, 1);
    }
  }
  const param = { localKey, foreignKey };
  params.push(param);

  // merge parameters from parent if it has some
  const protoParams: any[] = get_metadata(Object.getPrototypeOf(target), "__mappedParameters");
  if (protoParams != null) {
    protoParams.forEach((protoParam) => {
      // if we don't already have a parameter with the same name
      if (!params.some((p) => p.localKey === protoParam.localKey)) {
        params.push(protoParam);
      }
    });
  }

  set_metadata(target, "__mappedParameters", params);
  return target;
}

export function ResponseHandler(nameOrDescription: string, description?: string) {
  return (obj: object) => {
    // for backwards compatibility
    if (description === undefined) {
      declareResponseHandler(obj, nameOrDescription);
    } else {
      declareResponseHandler(obj, description, nameOrDescription);
    }
  };
}

export function declareResponseHandler(obj: object, description: string, name?: string) {
  declareRug(obj, "response-handler", description, name);
  return obj;
}

export function CommandHandler(nameOrDescription: string, description?: string) {
  return (obj: any) => {
    // for backwards compatibility
    if (description === undefined) {
      declareCommandHandler(obj, nameOrDescription);
    } else {
      declareCommandHandler(obj, description, nameOrDescription);
    }
  };
}

type TestKind = "integration";

interface TestDescriptor {
  description: string;
  kind: TestKind;
}

export function IntegrationTest(description: string | TestDescriptor) {
  return (obj: any) => {
    if (typeof description === "string") {
      declareCommandHandler(obj, description);
      declareIntegrationTest(obj, { description, kind: "integration" });
    } else {
      declareCommandHandler(obj, description.description);
      declareIntegrationTest(obj, description);
    }
  };
}

/**
 * Describe to whom this rug is visible (not to be confused by ACLs)
 * This stuff is not enforced, but specifies developer intent.
 *
 * @param obj - a rug
 * @param scope
 */
export function setScope(obj: any, scope: Scope) {
  set_metadata(obj, "__scope", scope);
  return obj;
}
type Scope = "archive";

export function declareIntegrationTest(obj: any, descriptor: TestDescriptor) {
  set_metadata(obj, "__test", descriptor);
}

export function declareCommandHandler(obj: any, description: string, name?: string) {
  declareRug(obj, "command-handler", description, name);
  return obj;
}

export function Generator(nameOrDescription: string, description?: string) {
  return (obj: any) => {
    // for backwards compatibility
    if (description === undefined) {
      declareGenerator(obj, nameOrDescription);
    } else {
      declareGenerator(obj, description, nameOrDescription);
    }
  };
}

export function declareGenerator(obj: any, description: string, name?: string) {
  declareRug(obj, "generator", description, name);
  return obj;
}

export function Editor(nameOrDescription: string, description?: string) {
  return (obj: object) => {
    // for backwards compatibility
    if (description === undefined) {
      declareEditor(obj, nameOrDescription);
    } else {
      declareEditor(obj, description, nameOrDescription);
    }
  };
}

export function declareEditor(obj: object, description: string, name?: string) {
  declareRug(obj, "editor", description, name);
  return obj;
}

type RugKind = "editor" | "generator" | "command-handler" | "response-handler" | "event-handler";

function declareRug(obj: any, kind: RugKind, description: string, name?: string) {
  if (name === undefined) {
    set_metadata(obj, "__description", description);
  } else {
    set_metadata(obj, "__description", description);
    set_metadata(obj, "__name", name);
  }
  set_metadata(obj, "__kind", kind);
}

export function EventHandler(
  nameOrDescription: string,
  descriptionOrExpression: string | PathExpression<any, any>,
  expression?: PathExpression<any, any> | string) {
  return (obj: object) => {
    if (expression !== undefined) {
      if (typeof descriptionOrExpression === "string") {
        declareEventHandler(obj, descriptionOrExpression, expression, nameOrDescription);
      } else {
        throw Error("When three parameters are passed, the second must be a description");
      }
    } else {
      declareEventHandler(obj, nameOrDescription, descriptionOrExpression);
    }
  };
}

export function declareEventHandler(
  obj: any, description: string, expression: string | PathExpression<any, any>, name?: string) {
  declareRug(obj, "event-handler", description, name);
  if (typeof expression === "string") {
    set_metadata(obj, "__expression", expression);
  } else {
    set_metadata(obj, "__expression", expression.expression);
  }
  return obj;
}

/**
 * Decorator for tags. Sets tags on the class
 */

export function Tags(...tags: string[]) {
  return (target: any) => {
    declareTags(target, tags);
  };
}

export function declareTags(target: any, tags: string[]) {
  set_metadata(target, "__tags", tags);
  return target;
}

export function Intent(...intent: string[]) {
  return (target: any) => {
    declareIntent(target, intent);
  };
}

export function declareIntent(target: any, intent: string[]) {
  set_metadata(target, "__intent", intent);
  return target;
}

export function Secrets(...secrets: string[]) {
  return (target: any) => {
    declareSecrets(target, secrets);
  };
}

export function declareSecrets(target: any, secrets: string[]) {
  set_metadata(target, "__secrets", secrets);
  return target;
}

// for parameters to ResponseHandlers to do response body coercion
export function ParseJson(target: object, propertyKey: string | symbol, parameterIndex: number) {
  set_metadata(target, "__coercion", "JSON");
}
