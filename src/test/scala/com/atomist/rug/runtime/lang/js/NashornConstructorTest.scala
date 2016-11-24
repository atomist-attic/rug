package com.atomist.rug.runtime.lang.js

import javax.script.ScriptEngineManager

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.SuccessfulModification
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by kipz on 23/11/2016.
  */
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
