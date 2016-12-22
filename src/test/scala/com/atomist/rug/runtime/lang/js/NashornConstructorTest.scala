package com.atomist.rug.runtime.lang.js

import javax.script.ScriptEngineManager

import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}
import org.scalatest.{FlatSpec, Matchers}

object NashornConstructorTest {

  val SimpleJavascriptEditor: String =
    """
      |"use strict";
      |var RugOperation_1 = require('@atomist/rug/operations/RugOperation');
      |var SimpleEditor = (function () {
      |    function SimpleEditor() {
      |        this.name = "Simple";
      |        this.description = "My simple editor";
      |        this.parameters = [{"name": "content", "description": "desc"}]
      |    }
      |    SimpleEditor.prototype.edit = function (project) {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        return new RugOperation_1.Result(RugOperation_1.Status.Success, "Edited Project now containing " + project.fileCount() + " files: \n");
      |    };
      |    return SimpleEditor;
      |}());
      |var editor = new SimpleEditor();
      |
      |""".stripMargin
}

class NashornConstructorTest extends FlatSpec with Matchers {

  private val engine = new ScriptEngineManager(null).getEngineByName("nashorn")

  it should "inject a constructor param" in {
    val withConstructor =
      """var ConstructedEditor = (function () {
        |    function ConstructedEditor(eng) {
        |        //print("cons:" + eng);
        |        this._eng = eng;
        |    }
        |    ConstructedEditor.prototype.edit = function () {
        |        //print("blah:" + this._eng);
        |        return this._eng.split(".");
        |    };
        |    return ConstructedEditor;
        |}());
        |
        |""".stripMargin
    engine.eval(withConstructor)
    val eObj = engine.eval("ConstructedEditor").asInstanceOf[JSObject]
    val newEditor = eObj.newObject("blah")
    engine.put("X", newEditor)
    engine.get("X").asInstanceOf[ScriptObjectMirror].callMember("edit")
  }
}
