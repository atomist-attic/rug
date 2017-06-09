package com.atomist.rug.runtime.js.nashorn

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
      |        this.__kind = "editor"
      |        this.__name = "Simple";
      |        this.__description = "My simple editor";
      |        this.__parameters = [{"name": "content", "description": "desc", "pattern": "@any"}]
      |    }
      |    SimpleEditor.prototype.edit = function (project) {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |    };
      |    return SimpleEditor;
      |}());
      |var editor = new SimpleEditor();
      |exports.editor = editor;
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
