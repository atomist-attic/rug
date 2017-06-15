var exports = {};
var module = {};

function require(id) {

  var endsWith = function (str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
  }

  var startsWith = function (str, prefix) {
    return str.lastIndexOf(prefix, 0) === 0;
  }

  if ('undefined' === typeof arguments.callee.require_stack) { arguments.callee.require_stack = []; }
  var require_stack = arguments.callee.require_stack;
  if ('undefined' === typeof arguments.callee.modules) { arguments.callee.modules = {}; }
  var modules = arguments.callee.modules;

  // if currently requiring module 'id', return partial exports
  if (require_stack.indexOf(id) >= 0) {
    return modules[id].exports;
  }

  // if already required module 'id', return finished exports
  if (modules[id] && modules[id].exports) {
    return modules[id].exports;
  }

  // do the require of module 'id'
  // - if currently requiring a module, push global exports/module objects into arguments.callee.modules
  if (require_stack.length > 0) {
    var currently_requiring_id = require_stack[require_stack.length - 1];
    modules[currently_requiring_id] = {
      exports: exports,
      module: module
    };
  }

  require_stack.push(id);
  exports = {};
  module = {};

  //relative paths;
  var module_str;
  var filename;
  var root = "";
  if (arguments.callee.filename !== undefined) {
    root = arguments.callee.filename;
  }
  if (startsWith(id, "./")) {
    if (endsWith(id, ".js")) {
      if (fileExists(root, id)) {
        module_str = load(root, id)
        filename = root + "/" + id
      }
    } else {
      var jid = id + ".js"
      if (fileExists(root, jid)) {
        module_str = load(root, jid)
        filename = root + "/" + jid
      }
    }
  } else {
    var jid = "node_modules/" + id
    if (endsWith(jid, ".js")) {
      if (fileExists(root, jid)) {
        module_str = load(root, jid)
        filename = root + "/" + jid
      }
    } else {
      var jid = jid + ".js"
      if (fileExists(root, jid)) {
        module_str = load(root, jid)
        filename = root + "/" + jid
      }
    }
  }

  if (module_str === undefined) {
    throw new Error("Unable to load module: " + id)
  }

  exports[id] = eval(module_str);
  module["id"] = id
  module["filename"] = filename

  modules[id] = {
    exports: exports,
    module: module
  };
  require_stack.pop();

  // restore last required modules' partial exports to the global space, or clear them
  if (require_stack.length > 0) {
    var currently_requiring_id = require_stack[require_stack.length - 1];
    exports = modules[currently_requiring_id].exports;
    module = modules[currently_requiring_id].module;
  } else {
    exports = {};
    module = {};
  }

  // return arguments.callee.modules[id].exports;
  return arguments.callee.modules[id].exports;
}
