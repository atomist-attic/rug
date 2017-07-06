package com.atomist.rug.kind.core

import java.util.{Collections, Objects}

import com.atomist.param.{ParameterValues, SimpleParameterValues}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, RugResolver}
import com.atomist.project.common.template._
import com.atomist.project.edit.{NoModificationNeeded, ProjectEditor, SuccessfulModification}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.Rug
import com.atomist.rug.runtime.js.interop.{ExposeAsFunction, jsGitProjectLoader, jsPathExpressionEngine}
import com.atomist.rug.runtime.js.{JavaScriptObject, LocalRugContext, RugContext}
import com.atomist.rug.spi.{ExportFunctionParameterDescription, _}
import com.atomist.rug.{EditorNotFoundException, RugRuntimeException}
import com.atomist.source._
import com.atomist.tree.content.text.{LineInputPositionImpl, OverwritableTextTreeNode}
import com.atomist.tree.{AddressableTreeNode, TreeMaterializer, TreeNode}
import com.atomist.util.BinaryDecider
import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConverters._
import scala.util.Properties

object ProjectMutableView {

  /**
    * There should be a MergeToolCreator for each template engine we support.
    */
  val MergeToolCreators: Seq[MergeToolCreator] = Seq(
    new MustacheMergeToolCreator
  )
}

/**
  * Operations on a project. Backed by an immutable ArtifactSource,
  * using copy on write.
  *
  * @param rugAs backing store of the editor
  * @param originalBackingObject original backing object (ArtifactSource). Changed on modification
  * @param atomistConfig Atomist configuration used to determine where we look for files.
  */
class ProjectMutableView(
                          val rugAs: ArtifactSource,
                          originalBackingObject: ArtifactSource,
                          atomistConfig: AtomistConfig,
                          creator: Option[Rug] = None,
                          ctx: RugContext = LocalRugContext,
                          rugResolver: Option[RugResolver] = None)
  extends ArtifactContainerMutableView[ArtifactSource](originalBackingObject, null)
    with ProjectView
    with ChangeLogging[ArtifactSource] {

  // We need this, rather than merely a default, for Java subclasses
  def this(rugAs: ArtifactSource, originalBackingObject: ArtifactSource) =
    this(rugAs, originalBackingObject, DefaultAtomistConfig)

  /**
    * Create a new ProjectMutableView with an empty Rug backing archive
    */
  def this(originalBackingObject: ArtifactSource) =
    this(EmptyArtifactSource(), originalBackingObject, DefaultAtomistConfig)

  import ProjectMutableView._

  @ExportFunction(readOnly = true, description = "Node content")
  override def value: String = s"project:[$name]"

  override def toString: String =
    s"ProjectMutableView[name='$name'] around $currentBackingObject\n" +
      s"${ArtifactSourceUtils.prettyListFiles(currentBackingObject)}"

  /**
    * Content used only for templates.
    */
  val templateContent: ArtifactSource = atomistConfig.templateContentIn(rugAs)

  private lazy val mergeTool =
    new CombinedMergeToolCreator(MergeToolCreators: _*).createMergeTool(templateContent)

  private val typeName: String = Typed.typeToTypeName(classOf[ProjectMutableView])

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case `typeName` =>
      // Special case. We don't want a "project" directory to confuse us
      Seq(this)
    case _ => kids(fieldName, this)
  }

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Return the name of the project. If it's in GitHub, it will be the repo name. " +
      "If it's on the local filesystem it will be the directory name")
  override def name: String = {
    val segments = currentBackingObject.id.name.split("/")
    segments.reverse(0)
  }

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "The total number of files in this project")
  def fileCount: Int = currentBackingObject.totalFileCount

  @ExportFunction(readOnly = true, description = "Create a directory")
  def addDirectory(@ExportFunctionParameterDescription(name = "name",
    description = "The name of the directory being added")
                   name: String,
                   @ExportFunctionParameterDescription(name = "parentPath",
                     description = "The path under which the directory should be created")
                   parentPath: String): Unit = {
    updateTo(currentBackingObject + EmptyDirectoryArtifact(name, parentPath.split("/")))
  }

  @ExportFunction(readOnly = true, description = "Create a directory")
  def addDirectoryAndIntermediates(
                                    @ExportFunctionParameterDescription(name = "directoryPath",
                                      description = "The path under which the directory and any missing intermediate directories will be created")
                                    directoryPath: String): Unit = {
    val splitPath = directoryPath.split("/")
    val splitLength = splitPath.length
    val directoryName = splitPath(splitLength - 1)
    updateTo(currentBackingObject + EmptyDirectoryArtifact(directoryName, splitPath))
  }

  @ExportFunction(readOnly = true, description = "Deletes a directory with the given path")
  def deleteDirectory(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                      path: String): Unit = updateTo(currentBackingObject.filter(!_.path.equals(path), _ => true))

  @ExportFunction(readOnly = true,
    description = "Does a file with the given path exist and have the expected content?")
  def fileHasContent(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                     path: String,
                     @ExportFunctionParameterDescription(name = "content",
                       description = "The content to check against the given file")
                     content: String): Boolean =
    currentBackingObject.findFile(path).exists(f => Objects.equals(content, f.content))

  @ExportFunction(readOnly = true,
    description = "The number of files directly in this directory")
  def countFilesInDirectory(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                            path: String): Int = {
    currentBackingObject.findDirectory(path) match {
      case None => 0
      case Some(d) => d.files.size
    }
  }

  @ExportFunction(readOnly = true,
    description = "Does a file with the given path exist and have the expected content?")
  def fileContains(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                   path: String,
                   @ExportFunctionParameterDescription(name = "content",
                     description = "The content to check")
                   content: String): Boolean =
    currentBackingObject.findFile(path).exists(f => f.content != null && f.content.contains(content))

  @ExportFunction(readOnly = false,
    description = "Replace all occurrences of the given string literal in this project. Use with care!")
  def replace(@ExportFunctionParameterDescription(name = "literal",
    description = "The string to look for")
              literal: String,
              @ExportFunctionParameterDescription(name = "replaceWith",
                description = "The string to replace matches with")
              replaceWith: String): Unit = {
    val fe = SimpleFileEditor(f => !BinaryDecider.isBinaryContent(f.content),
      f => StringFileArtifact.updated(f, f.content.replace(literal, replaceWith)))
    updateTo(currentBackingObject ✎ fe)
  }

  def updateFile(oldFile: FileArtifact, newFile: FileArtifact): Unit = {
    val fe = SimpleFileEditor(f => f.path.equals(oldFile.path), f => newFile)
    updateTo(currentBackingObject ✎ fe)
  }

  @ExportFunction(readOnly = false,
    description = "Replace all occurrences of the given regular expression in this project")
  def regexpReplace(@ExportFunctionParameterDescription(name = "regexp",
    description = "The regular expression to search for")
                    regexp: String,
                    @ExportFunctionParameterDescription(name = "replacement",
                      description = "The string to replace matches with")
                    replacement: String): Unit = {
    regexpReplaceWithFilter(f => true, regexp, replacement)
  }

  /**
    * Perform a regexp replace with the given file filter.
    *
    * @param filter file filter
    * @param regexp regexp
    * @param replacement replacement for the regexp
    */
  def regexpReplaceWithFilter(
                               filter: FileArtifact => Boolean,
                               regexp: String,
                               replacement: String): Unit = {
    val fe = SimpleFileEditor(f => filter(f) && !BinaryDecider.isBinaryContent(f.content),
      f => StringFileArtifact.updated(f, f.content.replaceAll(regexp, replacement)))
    updateTo(currentBackingObject ✎ fe)
  }

  @ExportFunction(readOnly = false,
    description =
      """Globally replace all occurrences of the given string literal in file paths in this project""",
    example = """`replace "com/foo/bar" "com/foo/baz"` """)
  def replaceInPath(@ExportFunctionParameterDescription(name = "literal",
    description = "The string to search for")
                    literal: String,
                    @ExportFunctionParameterDescription(name = "replacement",
                      description = "The string to replace in the paths if found")
                    replacement: String): Unit = {
    val fe = SimpleFileEditor(f => f.path.contains(literal), f => f.withPath(f.path.replace(literal, replacement)))
    updateTo(currentBackingObject ✎ fe)
  }

  @ExportFunction(readOnly = false,
    description = "Move the contents of this project under the given path, preserving its present path under that",
    example = """`moveUnder "src/main/java"` moves this file under the `src/main/java` directory """)
  def moveUnder(@ExportFunctionParameterDescription(name = "path",
    description = "The root path to move the file to")
                path: String): Unit = {
    val moved = currentBackingObject withPathAbove path
    updateTo(moved)
  }

  @ExportFunction(readOnly = false, description = "Add the given file to the project. Path can contain /s. Content is a literal string")
  def addFile(@ExportFunctionParameterDescription(name = "path", description = "The path to use") path: String,
              @ExportFunctionParameterDescription(name = "content",
                description = "The content to be placed in the new file") content: String): Unit =
    addFile(path, content, FileArtifact.DefaultMode)

  @ExportFunction(readOnly = false, description = "Add the given executable file to the project. Path can contain /s. Content is a literal string")
  def addExecutableFile(@ExportFunctionParameterDescription(name = "path", description = "The path to use") path: String,
                        @ExportFunctionParameterDescription(name = "content",
                          description = "The content to be placed in the new file") content: String): Unit =
    addFile(path, content, FileArtifact.ExecutableMode)

  private def addFile(path: String, content: String, mode: Int): Unit = {
    val desiredContent = content.replace("\\n", Properties.lineSeparator)
    val exactSameFileIsAlreadyThere = currentBackingObject.findFile(path).exists(_.content == desiredContent)
    if (!exactSameFileIsAlreadyThere) {
      updateTo(currentBackingObject + StringFileArtifact(path, desiredContent, mode, None))
    }
  }

  @ExportFunction(readOnly = false,
    description = "Makes a file executable")
  def makeExecutable(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                     path: String): Unit = {
    val file = currentBackingObject.findFile(path)
    file match {
      case Some(sourceFile) =>
        val executableFile = sourceFile.withMode(FileArtifact.ExecutableMode)
        updateTo(currentBackingObject.delete(path) + executableFile)
      case None =>
    }
  }

  @ExportFunction(readOnly = false,
    description = "Delete the given file from the project. Path can contain /s.")
  def deleteFile(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                 path: String): Unit = {
    updateTo(currentBackingObject.delete(path))
  }

  @ExportFunction(readOnly = false,
    description = "Copy the given file in the target project. It is not an error if it doesn't exist")
  def copyFile(@ExportFunctionParameterDescription(name = "sourcePath",
    description = "Source path")
               sourcePath: String,
               @ExportFunctionParameterDescription(name = "destinationPath",
                 description = "Destination path")
               destinationPath: String): Unit = {
    val sourceFileO = currentBackingObject.findFile(sourcePath)
    sourceFileO match {
      case Some(sourceFile) =>
        updateTo(currentBackingObject + sourceFile.withPath(destinationPath))
      case None =>
    }
  }

  @ExportFunction(readOnly = false,
    description = "Copy the given file in the target project. Fail the editor if it isn't found or if the destination already exists")
  def copyFileOrFail(@ExportFunctionParameterDescription(name = "sourcePath",
    description = "Source path")
                     sourcePath: String,
                     @ExportFunctionParameterDescription(name = "destinationPath",
                       description = "Destination path")
                     destinationPath: String): Unit = {
    if (fileExists(destinationPath))
      throw new IllegalArgumentException(s"Attempt to copy file [$sourcePath] to existing path [$destinationPath]")

    val sourceFileO = currentBackingObject.findFile(sourcePath)
    sourceFileO match {
      case Some(sourceFile) =>
        updateTo(currentBackingObject + sourceFile.withPath(destinationPath))
      case None =>
        throw new IllegalArgumentException(s"Attempt to copy file [$sourcePath], which does not exist")
    }
  }

  @ExportFunction(readOnly = false,
    description = "Copy the given file from the editor's backing archive to the same path in project being edited. Fail the editor if it isn't found or if the destination already exists")
  def copyEditorBackingFileOrFail(@ExportFunctionParameterDescription(name = "sourcePath", description = "Source path") sourcePath: String): Unit =
    copyEditorBackingFileOrFailToDestination(sourcePath, sourcePath)

  @ExportFunction(readOnly = false,
    description = "Copy the given file from the editor's backing archive. Fail the editor if it isn't found or if the destination already exists")
  def copyEditorBackingFileOrFailToDestination(@ExportFunctionParameterDescription(name = "sourcePath", description = "Source path") sourcePath: String,
                                               @ExportFunctionParameterDescription(name = "destinationPath", description = "Destination path") destinationPath: String): Unit = {
    if (fileExists(destinationPath))
      throw new IllegalArgumentException(s"Attempt to copy file [$sourcePath] to existing path [$destinationPath]")

    val sourceFileO = rugAs.findFile(sourcePath)
    sourceFileO match {
      case Some(sourceFile) =>
        updateTo(currentBackingObject + sourceFile.withPath(destinationPath))
      case None =>
        throw new IllegalArgumentException(s"Attempt to copy editor backing file [$sourcePath], which does not exist")
    }
  }

  @ExportFunction(readOnly = false,
    description = "Copy the given files from the editor's backing archive under the given path. Take the relative paths and place under new destination path")
  def copyEditorBackingFilesWithNewRelativePath(@ExportFunctionParameterDescription(name = "sourcePath",
    description = "Source directory")
                                                sourceDir: String,
                                                @ExportFunctionParameterDescription(name = "destinationPath",
                                                  description = "Destination path")
                                                destinationPath: String): Unit = {
    if (rugAs.findFile(sourceDir).isDefined)
      throw new IllegalArgumentException(s"Path [$sourceDir] is a file, not a directory")

    val underDir = (rugAs / sourceDir) withPathAbove destinationPath
    updateTo(currentBackingObject + underDir)
  }

  @ExportFunction(readOnly = false,
    description = "Copy the given files from the editor's backing archive under the given directory into the same directory in the project being edited.")
  def copyEditorBackingFilesPreservingPath(@ExportFunctionParameterDescription(name = "sourcePath",
    description = "Source directory")
                                           sourceDir: String
                                          ): Unit = {
    if (rugAs.findFile(sourceDir).isDefined)
      throw new IllegalArgumentException(s"Path [$sourceDir] is a file, not a directory")

    val underDir = rugAs.filter(_ => true, f => f.path.startsWith(sourceDir))
    updateTo(currentBackingObject + underDir)
  }

  @ExportFunction(readOnly = false,
    description = "Copy the given files from the editor's backing archive project to the project being edited. Doesn't copy Atomist content.")
  def copyEditorBackingProject(): Unit = {
    val underDir = rugAs.filter(_ => true, f => !f.path.startsWith(atomistConfig.atomistRoot))
    updateTo(currentBackingObject + underDir)
  }

  @ExportFunction(readOnly = false,
    description = "Copy the given file from the editor's backing archive. Fail the editor if it isn't found or if the destination already exists")
  def copyEditorBackingFilesOrFail(@ExportFunctionParameterDescription(name = "sourcePath",
    description = "Source directory")
                                   sourceDir: String,
                                   @ExportFunctionParameterDescription(name = "destinationPath",
                                     description = "Destination path")
                                   destinationPath: String): Unit = {
    val underDir = rugAs / sourceDir
    if (underDir.totalFileCount == 0)
      throw new IllegalArgumentException(s"No files found in editor backing object [$sourceDir]")
    else
      updateTo(currentBackingObject + underDir)
  }

  @ExportFunction(readOnly = false,
    description = "Merge the given template to the given output path.")
  def merge(@ExportFunctionParameterDescription(name = "template",
    description = "The name of the template within the backing Rug archive, under /.atomist / templates")
            template: String,
            @ExportFunctionParameterDescription(name = "path",
              description = "The path that will be the merged path within the output project.")
            path: String,
            @ExportFunctionParameterDescription(name = "parameters",
              description = "Parameters")
            parametersToTemplate: Any): Unit = {
    val mc = MergeContext(mapToUse(parametersToTemplate))
    val newFile = mergeTool.mergeToFile(mc, template).withPath(path)
    updateTo(currentBackingObject + newFile)
  }

  private def mapToUse(arg: Any): Map[String, Object] = arg match {
    case som: JavaScriptObject =>
      som.extractProperties()
    case m: Map[String, Object]@unchecked => m
    case m: java.util.Map[String, Object]@unchecked =>
      import scala.collection.JavaConverters._
      m.asScala.toMap
    case null => Map()
  }

  @ExportFunction(readOnly = false,
    description = "Merge templates from the specified directory in the backing archive, under /.atomist/templates, to the given output path in the project being edited.")
  def mergeTemplates(@ExportFunctionParameterDescription(name = "templatesPath",
    description = "Source template path where content will be used to merge into target project")
                     templatesPath: String,
                     @ExportFunctionParameterDescription(name = "outputPath",
                       description = "The destination path within the destination project")
                     outputPath: String,
                     @ExportFunctionParameterDescription(name = "ic",
                       description = "Parameters to the template")
                     parametersToTemplate: Any): Unit = {
    val mc = MergeContext(mapToUse(parametersToTemplate))
    val directoryToMerge = templateContent / templatesPath
    val outputContent = mergeTool.processTemplateFiles(mc, directoryToMerge).withPathAbove(outputPath)
    updateTo(currentBackingObject + outputContent)
  }

  @ExportFunction(readOnly = false,
    description = "Don't use. Merely intended to simplify the life of the Rug to TypeScript transpiler.")
  def projects: java.util.List[ProjectMutableView] = Collections.singletonList(this)

  @ExportFunction(readOnly = false,
    exposeAsProperty = true,
    description = "Files in this project")
  def files: java.util.List[FileMutableView] =
    currentBackingObject.allFiles.map(FileMutableView(_, this)).asJava

  /**
    * For use by scripts. Edit the project with the given
    * map of string arguments.
    *
    * @param editorName name of editor to use
    * @param params parameters to pass to the editor
    */
  protected def editWith(editorName: String,
                         params: Map[String, Object]): Unit = {
    (creator, rugResolver) match {
      case (Some(rug), Some(resolver)) => resolver.resolve(rug, editorName) match {
        case Some(ed: ProjectEditor) =>
          ed.modify(currentBackingObject, SimpleParameterValues(params)) match {
            case sm: SuccessfulModification =>
              updateTo(sm.result)
            case _: NoModificationNeeded => currentBackingObject
            case wtf =>
              throw new RugRuntimeException(ed.name, s"Unexpected editor failure: $wtf", null)
          }
        case None =>
          if (editorName.contains(":")) {
            val shortName = StringUtils.substringAfterLast(editorName, ":")
            if (resolver.resolve(rug, shortName).nonEmpty) {
              throw new EditorNotFoundException(s"Could not find editor: $editorName. Did you mean: $shortName?")
            }
          }
          throw new EditorNotFoundException(editorName, resolver.resolvedDependencies.rugs.allRugs);
        case _ =>
          throw new EditorNotFoundException(editorName, resolver.resolvedDependencies.rugs.allRugs);
      }
      case _ => throw new RugRuntimeException(name, s"No Rug context in which to find other editor: [$editorName]")
    }
  }

  @ExportFunction(readOnly = false, description = "Edit with the given editor")
  def editWith(
                @ExportFunctionParameterDescription(name = "editorName",
                  description = "Name of the editor to invoke")
                editorName: String,
                @ExportFunctionParameterDescription(name = "params",
                  description = "Parameters to pass to the editor")
                params: Any): Unit = {
    val m: Map[String, Object] = params match {
      case som: JavaScriptObject =>
        // The user has created a new JavaScript object, as in { foo: "bar" },
        // to pass up as an argument to the invoked editor. Extract its properties
        som.extractProperties()
      case _ => Map.empty
    }
    editWith(editorName, m)
  }

  @ExportFunction(readOnly = true, description = "Return a new Project View based on the original backing object (normally the .atomist/ directory)")
  def backingArchiveProject(): ProjectMutableView = {
    new ProjectMutableView(EmptyArtifactSource.apply(), rugAs, atomistConfig, creator)
  }

  /**
    * Convenient method to apply an editor.
    */
  protected def applyEditor(
                             ed: ProjectEditor,
                             poa: ParameterValues = SimpleParameterValues.Empty): ArtifactSource = {
    ed.modify(currentBackingObject, poa) match {
      case sm: SuccessfulModification => sm.result
      case _: NoModificationNeeded => currentBackingObject
      case wtf =>
        throw new RugRuntimeException(ed.name, s"Unexpected editor failure: $wtf", null)
    }
  }

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    exposeResultDirectlyToNashorn = true,
    description = "Provides access additional context, such as the PathExpressionEngine")
  def context: ProjectContext = ctx match {
    case pc: ProjectContext => pc
    case _ => new ProjectContext(ctx)
  }

  import com.atomist.tree.pathexpression.PathExpressionParser._

  @ExportFunction(readOnly = true,
    description = "Return the path expression to this point in the given file")
  def pathTo(path: String, kind: String, lineFrom1: Int, colFrom1: Int): String = {
    val nodeO = nodeAt(path, kind, lineFrom1, colFrom1)
    nodeO.collect {
      case n: AddressableTreeNode => n.address.dropWhile(_ != '/')
    }.getOrElse {
      // Return the path to the file if it's valid, even if we can't resolve a structure within it
      if (fileExists(path) && DefaultTypeRegistry.findByName(kind).isDefined)
        pathToRootContainer(path, kind)
      else null
    }
  }

  private def pathToRootContainer(path: String, kind: String) = s"/File()[@path='$path']/$kind()"

  private def nodeAt(path: String, kind: String, lineFrom1: Int, colFrom1: Int): Option[TreeNode] = {
    val pexpr = pathToRootContainer(path, kind)
    ctx.pathExpressionEngine.ee.evaluate(this, pexpr) match {
      case Right(nodes) if nodes.size == 1 =>
        val theNode = nodes.head
        theNode match {
          case ow: OverwritableTextTreeNode =>
            val pos = LineInputPositionImpl(ow.file.content, lineFrom1, colFrom1)
            ow.nodeAt(pos)
          case _ => None
        }
      case Right(nodes) if nodes.size > 1 =>
        throw new IllegalArgumentException(s"Found ${nodes.size} hits for [$pexpr], not 1")
      case x =>
        None
    }
  }
}

/**
  * For backwards compatibility
  */
class ProjectContext(ctx: RugContext) extends RugContext {

  override def typeRegistry: TypeRegistry = DefaultTypeRegistry

  @ExportFunction(readOnly = true, exposeAsProperty = true, description="Access the path expression engine")
  override def pathExpressionEngine: jsPathExpressionEngine = ctx.pathExpressionEngine

  /**
    * Id of the team we're working on behalf of
    */
  override def teamId: String = ctx.teamId

  /**
    * Used to hydrate nodes before running a path expression
    */
  override def treeMaterializer: TreeMaterializer = ctx.treeMaterializer

  override def contextRoot(): AnyRef = ctx.contextRoot()

  @ExportFunction(readOnly = true, description = "Load projects from git", exposeAsProperty = true)
  override def gitProjectLoader: AnyRef = new jsGitProjectLoader(ctx.repoResolver)

  @ExportFunction(readOnly = true, description = "Get an empty project")
  def emptyProject() = new ProjectMutableView(originalBackingObject = EmptyArtifactSource("!!ThisValueWillBeOverwritten"))
}
