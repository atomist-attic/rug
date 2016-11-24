package com.atomist.rug.kind.elm

import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class ElmAccessoryTest
  extends FlatSpec
    with Matchers {

  it should "modify the build file" in {
    val prog = """editor AddElmCssBuildStep
                 |
                 |let buildGrammar = <
                 |    SOMETHING : 'target';
                 |    PLACEHOLDER : [];
                 |    expr : before=SOMETHING insert=PLACEHOLDER 'echo "Compiling..."';
                 |>
                 |
                 |with file f when name = "build"
                 |  with buildGrammar
                 |    with insert
                 |      do setValue 'echo "Building css..."\nelm-css -o target src/Stylesheets.elm\n'
                 |""".stripMargin

    val source = StringFileArtifact("build", ElmAccessoryTest.BuildFile)
    val r = executie(new SimpleFileBasedArtifactSource("", source), prog)
    val content = r.findFile("build").get.content

    content.contains("Building css") should be(true)
  }

  it should "add a link to the head of index.html" in {
    val prog = """editor AddLinkToHtml
                 |
                 |with file f when name = "index.html"
                 |     do regexpReplace
                 |      "</head>"
                 |      { '        <link type="text/css" href="elm-styles.css" rel="stylesheet"/>\n</head>' }
                 |""".stripMargin

    val source = StringFileArtifact("index.html", ElmAccessoryTest.IndexHtml)
    val r = executie(new SimpleFileBasedArtifactSource("", source), prog)
    val content = r.findFile("index.html").get.content
    content.contains("elm-styles.css") should be(true)

  }

  it should "add a dependency" in {
    val prog =
      """editor AddDependency
        |
        |param dependency_name: .*
        |
        |param dependency_version: .*
        |
        |let dep = <
        |    PLACEHOLDER : [];
        |    dep : '"dependencies":' '{' here=PLACEHOLDER;
        |>
        |
        |with file when path = "elm-package.json"
        |  with dep d
        |   do set 'here' {
        |      var computedRange = dependency_version
        |      var computedThing = '"' + dependency_name + '": "' + computedRange + '"\n'
        |      return computedThing + "        "
        |   }
        |
        |""".stripMargin
    val source = StringFileArtifact("elm-package.json", ElmAccessoryTest.ElmPackageJson)
    val r = executie(new SimpleFileBasedArtifactSource("", source), prog, Map(
      "dependency_name" -> "rtfeldman/elm-css",
      "dependency_version" -> "5.0.0 <= v < 6.0.0"
    ))
    val content = r.findFile("elm-package.json").get.content

    content.contains(""""rtfeldman/elm-css": "5.0.0 <= v < 6.0.0"""") should be(true)
  }

  private def executie(elmProject: ArtifactSource, program: String,
                         params: Map[String, String] = Map()): ArtifactSource = {
   ElmTypeUsageTest.elmExecute(elmProject, program, params)
  }
}

object ElmAccessoryTest {

  val BuildFile =
    """echo "Cleaning..."
      |mkdir -p target
      |rm -r target/*
      |
      |echo "Copying resources..."
      |cp -r resources/* target
      |
      |echo "Compiling..."
      |elm make --output target/elm.js src/Main.elm
      |""".stripMargin

  val IndexHtml =
    """<head>
      |       	<title>Counter</title>
      |       	<script src="elm.js"></script>
      |</head>
      |<body>
      |  <script type="text/javascript">Elm.Main.fullscreen()</script>
      |</body>
      |""".stripMargin

  val ElmPackageJson =
    """{
      |    "version": "1.0.0",
      |    "summary": "helpful summary of your project, less than 80 characters",
      |    "repository": "https://github.com/user/project.git",
      |    "license": "BSD3",
      |    "source-directories": [
      |        "src"
      |    ],
      |    "exposed-modules": [],
      |    "dependencies": {
      |        "elm-lang/core": "4.0.5 <= v < 5.0.0",
      |        "elm-lang/html": "1.1.0 <= v < 2.0.0"
      |    },
      |    "elm-version": "0.17.1 <= v < 0.18.0"
      |}
      |""".stripMargin
}
