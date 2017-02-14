package com.atomist.rug.kind.json

import org.scalatest.{FlatSpec, Matchers}

class JsonParserTest extends FlatSpec with Matchers {

  import JsonParserTest._

  it should "parse simple JSON" in {
    val jsp = (new JsonType).parser
    val parsed = jsp.parse(Simple)
  }

}

object JsonParserTest {

  val Simple =
    """
      |{
      |    "glossary": {
      |        "title": "example glossary",
      |		"GlossDiv": {
      |            "title": "S",
      |			"GlossList": {
      |                "GlossEntry": {
      |                    "ID": "SGML",
      |					"SortAs": "SGML",
      |					"GlossTerm": "Standard Generalized Markup Language",
      |					"Acronym": "SGML",
      |					"Abbrev": "ISO 8879:1986",
      |					"GlossDef": {
      |                        "para": "A meta-markup language, used to create markup languages such as DocBook.",
      |						"GlossSeeAlso": ["GML", "XML"]
      |                    },
      |					"GlossSee": "markup"
      |                }
      |            }
      |        }
      |    }
      |}
    """.stripMargin
}