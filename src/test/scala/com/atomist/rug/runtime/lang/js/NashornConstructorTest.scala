package com.atomist.rug.runtime.lang.js

import javax.script.ScriptEngineManager

import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}
import org.scalatest.{FlatSpec, Matchers}

object NashornConstructorTest {
   val SimpleJavascriptGenerator = """/*
                                     | * Copyright 2015-2016 Atomist Inc.
                                     | *
                                     | * Licensed under the Apache License, Version 2.0 (the "License");
                                     | * you may not use this file except in compliance with the License.
                                     | * You may obtain a copy of the License at
                                     | *
                                     | *      http://www.apache.org/licenses/LICENSE-2.0
                                     | *
                                     | * Unless required by applicable law or agreed to in writing, software
                                     | * distributed under the License is distributed on an "AS IS" BASIS,
                                     | * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                                     | * See the License for the specific language governing permissions and
                                     | * limitations under the License.
                                     | */
                                     |"use strict";
                                     |"use strict";
                                     |/**
                                     | * Convenient superclass for implementations, which validates
                                     | * all parameters OK.
                                     | */
                                     |var ParametersSupport = (function () {
                                     |    function ParametersSupport() {
                                     |    }
                                     |    ParametersSupport.prototype.validate = function () { return true; };
                                     |    return ParametersSupport;
                                     |}());
                                     |exports.ParametersSupport = ParametersSupport;
                                     |"use strict";
                                     |/**
                                     | * Status of an operation.
                                     | */
                                     |(function (Status) {
                                     |    Status[Status["Success"] = 0] = "Success";
                                     |    Status[Status["NoChange"] = 1] = "NoChange";
                                     |    Status[Status["Error"] = 2] = "Error";
                                     |})(exports.Status || (exports.Status = {}));
                                     |var Status = exports.Status;
                                     |/**
                                     | * Result of running an editor
                                     | */
                                     |var Result = (function () {
                                     |    function Result(status, message) {
                                     |        if (message === void 0) { message = ""; }
                                     |        this.status = status;
                                     |        this.message = message;
                                     |    }
                                     |    return Result;
                                     |}());
                                     |exports.Result = Result;
                                     |"use strict";
                                     |"use strict";
                                     |//used by annotation functions below
                                     |function set_metadata(obj, key, value) {
                                     |    Object.defineProperty(obj, key, { value: value, writable: false, enumerable: false });
                                     |}
                                     |function get_metadata(obj, key) {
                                     |    var desc = Object.getOwnPropertyDescriptor(obj, key);
                                     |    if ((desc == null || desc == undefined) && (obj.prototype != undefined)) {
                                     |        desc = Object.getOwnPropertyDescriptor(obj.prototype, key);
                                     |    }
                                     |    if (desc != null || desc != undefined) {
                                     |        return desc.value;
                                     |    }
                                     |    return null;
                                     |}
                                     |//exported to annotate rugs
                                     |function editor(description) {
                                     |    return function (cons) {
                                     |        set_metadata(cons, "rug-type", "editor");
                                     |        set_metadata(cons, "editor-description", description);
                                     |    };
                                     |}
                                     |exports.editor = editor;
                                     |function generator(description) {
                                     |    return function (cons) {
                                     |        set_metadata(cons, "rug-type", "generator");
                                     |        set_metadata(cons, "generator-description", description);
                                     |    };
                                     |}
                                     |exports.generator = generator;
                                     |function executor(description) {
                                     |    return function (cons) {
                                     |        set_metadata(cons, "rug-type", "executor");
                                     |        set_metadata(cons, "generator-description", description);
                                     |    };
                                     |}
                                     |exports.executor = executor;
                                     |function tag(name) {
                                     |    return function (cons) {
                                     |        var tags = get_metadata(cons, "tags");
                                     |        if (tags == null) {
                                     |            tags = [name];
                                     |        }
                                     |        else if (tags.indexOf(name) < 0) {
                                     |            tags.push(name);
                                     |        }
                                     |        set_metadata(cons, "tags", tags);
                                     |    };
                                     |}
                                     |exports.tag = tag;
                                     |function parameter(details) {
                                     |    return function (target, propertyKey) {
                                     |        var params = get_metadata(target, "params");
                                     |        if (params == null) {
                                     |            params = {};
                                     |        }
                                     |        params[propertyKey] = details;
                                     |        set_metadata(target, "params", params);
                                     |    };
                                     |}
                                     |exports.parameter = parameter;
                                     |function inject(typeToInject) {
                                     |    return function (target, propertyKey, parameterIndex) {
                                     |        var injects = get_metadata(target, "injects");
                                     |        if (injects == null) {
                                     |            injects = [];
                                     |        }
                                     |        injects.push({ propertyKey: propertyKey, parameterIndex: parameterIndex, typeToInject: typeToInject });
                                     |        set_metadata(target, "injects", injects);
                                     |    };
                                     |}
                                     |exports.inject = inject;
                                     |function parameters(name) {
                                     |    return function (target, propertyKey, parameterIndex) {
                                     |        set_metadata(target, "parameter-class", name);
                                     |    };
                                     |}
                                     |exports.parameters = parameters;
                                     |"use strict";
                                     |var __extends = (this && this.__extends) || function (d, b) {
                                     |    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
                                     |    function __() { this.constructor = d; }
                                     |    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
                                     |};
                                     |var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
                                     |    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
                                     |    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
                                     |    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
                                     |    return c > 3 && r && Object.defineProperty(target, key, r), r;
                                     |};
                                     |var __param = (this && this.__param) || function (paramIndex, decorator) {
                                     |    return function (target, key) { decorator(target, key, paramIndex); }
                                     |};
                                     |var Parameters_1 = require('user-model/operations/Parameters');
                                     |var Result_1 = require('user-model/operations/Result');
                                     |var Metadata_1 = require('user-model/support/Metadata');
                                     |var Metadata_2 = require('user-model/support/Metadata');
                                     |var Metadata_3 = require('user-model/support/Metadata');
                                     |var Metadata_4 = require('user-model/support/Metadata');
                                     |var ContentInfo = (function (_super) {
                                     |    __extends(ContentInfo, _super);
                                     |    function ContentInfo() {
                                     |        _super.apply(this, arguments);
                                     |        this.content = null;
                                     |    }
                                     |    __decorate([
                                     |        Metadata_1.parameter({ description: "Content", displayName: "content", pattern: "Anders .*", maxLength: 100 })
                                     |    ], ContentInfo.prototype, "content", void 0);
                                     |    return ContentInfo;
                                     |}(Parameters_1.ParametersSupport));
                                     |var SimpleEditor = (function () {
                                     |    function SimpleEditor() {
                                     |    }
                                     |    SimpleEditor.prototype.edit = function (project, p) {
                                     |        project.addFile("src/from/typescript", p.content);
                                     |        return new Result_1.Result(Result_1.Status.Success, "Edited Project now containing " + project.fileCount() + " files: \n");
                                     |    };
                                     |    __decorate([
                                     |        __param(1, Metadata_2.parameters("ContentInfo"))
                                     |    ], SimpleEditor.prototype, "edit", null);
                                     |    SimpleEditor = __decorate([
                                     |        Metadata_4.editor("A nice little editor"),
                                     |        Metadata_3.tag("java"),
                                     |        Metadata_3.tag("maven")
                                     |    ], SimpleEditor);
                                     |    return SimpleEditor;
                                     |}());
                                     |""".stripMargin
}

class NashornConstructorTest extends FlatSpec with Matchers{

   val engine = new ScriptEngineManager(null).getEngineByName("nashorn")

   it should "inject a constructor param" in {
      val withConstructor = """var ConstructedEditor = (function () {
                              |    function ConstructedEditor(eng) {
                              |        print("cons:" + eng);
                              |        this._eng = eng;
                              |    }
                              |    ConstructedEditor.prototype.edit = function () {
                              |        print("blah:" + this._eng);
                              |        return this._eng.split(".");
                              |    };
                              |    return ConstructedEditor;
                              |}());
                              |
                              |""".stripMargin
      engine.eval(withConstructor)
      val eObj = engine.eval("ConstructedEditor").asInstanceOf[JSObject]
      val newEditor = eObj.newObject("blah")
      engine.put("X",newEditor)
      engine.get("X").asInstanceOf[ScriptObjectMirror].callMember("edit")
   }
}
