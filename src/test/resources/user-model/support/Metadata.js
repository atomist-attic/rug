"use strict";
//used by annotation functions below
function set_metadata(obj, key, value) {
    Object.defineProperty(obj, key, { value: value, writable: false, enumerable: false });
}
function get_metadata(obj, key) {
    var desc = Object.getOwnPropertyDescriptor(obj, key);
    if ((desc == null || desc == undefined) && (obj.prototype != undefined)) {
        desc = Object.getOwnPropertyDescriptor(obj.prototype, key);
    }
    if (desc != null || desc != undefined) {
        return desc.value;
    }
    return null;
}
//exported to annotate rugs
function editor(description) {
    return function (cons) {
        set_metadata(cons, "rug-type", "editor");
        set_metadata(cons, "editor-description", description);
    };
}
exports.editor = editor;
function generator(description) {
    return function (cons) {
        set_metadata(cons, "rug-type", "generator");
        set_metadata(cons, "generator-description", description);
    };
}
exports.generator = generator;
function tag(name) {
    return function (cons) {
        var tags = get_metadata(cons, "tags");
        if (tags == null) {
            tags = [name];
        }
        else if (tags.indexOf(name) < 0) {
            tags.push(name);
        }
        set_metadata(cons, "tags", tags);
    };
}
exports.tag = tag;
function parameter(details) {
    return function (target, propertyKey) {
        var params = get_metadata(target, "params");
        if (params == null) {
            params = {};
        }
        params[propertyKey] = details;
        set_metadata(target, "params", params);
    };
}
exports.parameter = parameter;
function inject(typeToInject) {
    return function (target, propertyKey, parameterIndex) {
        var injects = get_metadata(target, "injects");
        if (injects == null) {
            injects = [];
        }
        injects.push({ propertyKey: propertyKey, parameterIndex: parameterIndex, typeToInject: typeToInject });
        set_metadata(target, "injects", injects);
    };
}
exports.inject = inject;
function parameters(name) {
    return function (target, propertyKey, parameterIndex) {
        set_metadata(target, "parameter-class", name);
    };
}
exports.parameters = parameters;
