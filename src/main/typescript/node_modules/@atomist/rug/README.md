# Rug TypeScript Typings

Rug is a programming model and runtime for development automation
created by [Atomist][www].  See the [Atomist documentation][doc] for
comprehensive documentation of Rug.  This is the reference
documentation for the Rug [TypeScript][ts] typings.

[www]: https://www.atomist.com/
[doc]: http://docs.atomist.com/
[ts]: https://www.typescriptlang.org/

If you have come to find developer information about Rug extensions
and the operations they provide, the interfaces (empty green cubes) on
the right will be of most interest.  Here are some direct links to
some of the more commonly used Rug extension interfaces:

-   [CSharpFile][] - C# files
-   [File][] - text files
-   [JavaProject][] - Java projects
-   [JavaSource][] - Java source files
-   [JavaType][] - Java classes
-   [Line][] - manipulate specific lines in a project's files
-   [Pom][] - projects containing Maven POM files
-   [Project][] - execute operations across a project
-   [Properties][] - Java properties files
-   [PythonFile][] - Python source files
-   [ScalaFile][] - Scala files
-   [SpringBootProject][] - Spring Boot projects
-   [Xml][] - XML files
-   [YamlFile][] - YAML files

[CSharpFile]: http://apidocs.atomist.com/typedoc/rug/interfaces/csharpfile.html
[File]: http://apidocs.atomist.com/typedoc/rug/interfaces/file.html
[JavaProject]: http://apidocs.atomist.com/typedoc/rug/interfaces/javaproject.html
[JavaSource]: http://apidocs.atomist.com/typedoc/rug/interfaces/javasource.html
[JavaType]: http://apidocs.atomist.com/typedoc/rug/interfaces/javatype.html
[Line]: http://apidocs.atomist.com/typedoc/rug/interfaces/line.html
[Pom]: http://apidocs.atomist.com/typedoc/rug/interfaces/pom.html
[Project]: http://apidocs.atomist.com/typedoc/rug/interfaces/project.html
[Properties]: http://apidocs.atomist.com/typedoc/rug/interfaces/properties.html
[PythonFile]: http://apidocs.atomist.com/typedoc/rug/interfaces/pythonfile.html
[ScalaFile]: http://apidocs.atomist.com/typedoc/rug/interfaces/scalafile.html
[SpringBootProject]: http://apidocs.atomist.com/typedoc/rug/interfaces/springbootproject.html
[Xml]: http://apidocs.atomist.com/typedoc/rug/interfaces/xml.html
[YamlFile]: http://apidocs.atomist.com/typedoc/rug/interfaces/yamlfile.html

The following links contain information on testing classes,
interfaces, and functions:

-   [CommandHandlerScenarioWorld][] - "world" in which command handler tests run
-   [EventHandlerScenarioWorld][] - "world" in which event handler tests run
-   [ProjectScenarioWorld][] - "world" in which editor and generator tests run
-   [Steps][] - [Gherkin][] steps
-   [Helper Functions][helpers] - debugging helper functions

[CommandHandlerScenarioWorld]: http://apidocs.atomist.com/typedoc/rug/interfaces/commandhandlerscenarioworld.html
[EventHandlerScenarioWorld]: http://apidocs.atomist.com/typedoc/rug/interfaces/eventhandlerscenarioworld.html
[ProjectScenarioWorld]: http://apidocs.atomist.com/typedoc/rug/interfaces/projectscenarioworld.html
[Steps]: http://apidocs.atomist.com/typedoc/rug/interfaces/definitions.html
[Gherkin]: https://github.com/cucumber/cucumber/wiki/Given-When-Then
[helpers]: http://apidocs.atomist.com/typedoc/rug/globals.html#dump
