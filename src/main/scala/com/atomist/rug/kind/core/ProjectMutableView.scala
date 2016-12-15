package com.atomist.rug.kind.core

import java.util.{Collections, Objects}

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.common.template._
import com.atomist.project.edit.{NoModificationNeeded, ProjectEditor, SuccessfulModification}
import com.atomist.project.{ProjectOperation, ProjectOperationArguments, SimpleProjectOperationArguments}
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.runtime.js.interop.{BidirectionalParametersProxy, DefaultAtomistFacade, PathExpressionExposer, UserModelContext}
import com.atomist.rug.runtime.rugdsl.FunctionInvocationContext
import com.atomist.rug.spi._
import com.atomist.rug.ts.NashornUtils
import com.atomist.source._
import com.atomist.util.BinaryDecider
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.util.Properties

object ProjectMutableView {

  /**
    * There should be a MergeToolCreator for each template engine we support.
    */
  val MergeToolCreators: Seq[MergeToolCreator] = Seq(
    new MustacheMergeToolCreator,
    new VelocityMergeToolCreator
  )
}

/**
  * Operations.
  *
  * @param rugAs                 backing store of the editor
  * @param originalBackingObject original backing object (ArtifactSource). Changed on modification
  * @param atomistConfig         Atomist configuration used to determine where we look for files.
  */
class ProjectMutableView(
                          val rugAs: ArtifactSource,
                          originalBackingObject: ArtifactSource,
                          atomistConfig: AtomistConfig,
                          po: Seq[ProjectOperation] = Nil,
                          ctx: UserModelContext = DefaultAtomistFacade)
  extends ArtifactContainerMutableView[ArtifactSource](originalBackingObject, null) {

  // We need this, rather than merely a default, for Java subclasses
  def this(rugAs: ArtifactSource, originalBackingObject: ArtifactSource) =
      this(rugAs, originalBackingObject, DefaultAtomistConfig)

  import ProjectMutableView._

  val templateContent = atomistConfig.templateContentIn(rugAs)

  private lazy val mergeTool =
    new CombinedMergeToolCreator(MergeToolCreators: _*).createMergeTool(templateContent)

  override def nodeType: String = "project"

  override def children(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case "project" =>
      // Special case. We don't want a "project" directory to confuse us
      Seq(this)
    case _ => kids(fieldName, this)
  }

  @ExportFunction(readOnly = true,
    description = "Return the name of the project. If it's in GitHub, it will be the repo name." +
      "If it's on the local filesystem it will be the directory name")
  override def name: String = {
    val segments = currentBackingObject.id.name.split("/")
    segments.reverse(0)
  }

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
                      path: String): Unit = updateTo(currentBackingObject.filter(d => !d.path.equals(path), f => true))

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

  @ExportFunction(readOnly = false,
    description = "Add the given file to the project. Path can contain /s. Content is a literal string")
  def addFile(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
              path: String,
              @ExportFunctionParameterDescription(name = "content",
                description = "The content to be placed in the new file")
              content: String): Unit = {
    val desiredContent = content.replace("\\n", Properties.lineSeparator)
    val exactSameFileIsAlreadyThere = currentBackingObject.findFile(path).exists(_.content == desiredContent)
    if(!exactSameFileIsAlreadyThere) {
      updateTo(currentBackingObject + StringFileArtifact(path, desiredContent))
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
      fail(s"Attempt to copy file [$sourcePath] to existing path [$destinationPath]")
    val sourceFileO = currentBackingObject.findFile(sourcePath)
    sourceFileO match {
      case Some(sourceFile) =>
        updateTo(currentBackingObject + sourceFile.withPath(destinationPath))
      case None =>
        fail(s"Attempt to copy file [$sourcePath], which does not exist")
    }
  }

  @ExportFunction(readOnly = false,
    description = "Copy the given file from the editor's backing archive to the same path in project being edited. Fail the editor if it isn't found or if the destination already exists")
  def copyEditorBackingFileOrFail(@ExportFunctionParameterDescription(name = "sourcePath",
    description = "Source path")
                                  sourcePath: String): Unit =
    copyEditorBackingFileOrFail(sourcePath, sourcePath)

  @ExportFunction(readOnly = false,
    description = "Copy the given file from the editor's backing archive. Fail the editor if it isn't found or if the destination already exists")
  def copyEditorBackingFileOrFail(@ExportFunctionParameterDescription(name = "sourcePath",
    description = "Source path")
                                  sourcePath: String,
                                  @ExportFunctionParameterDescription(name = "destinationPath",
                                    description = "Destination path")
                                  destinationPath: String): Unit = {
    if (fileExists(destinationPath))
      fail(s"Attempt to copy file [$sourcePath] to existing path [$destinationPath]")
    val sourceFileO = rugAs.findFile(sourcePath)
    sourceFileO match {
      case Some(sourceFile) =>
        updateTo(currentBackingObject + sourceFile.withPath(destinationPath))
      case None =>
        fail(s"Attempt to copy editor backing file [$sourcePath], which does not exist")
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
      throw new InstantEditorFailureException(s"Path [$sourceDir] is a file, not a directory")
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
      throw new InstantEditorFailureException(s"Path [$sourceDir] is a file, not a directory")
    val underDir = rugAs.filter(d => true, f => f.path.startsWith(sourceDir))
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
      fail(s"No files found in editor backing object [$sourceDir]")
    else
      updateTo(currentBackingObject + underDir)
  }

  @ExportFunction(readOnly = false,
    description =
      """
        |Merge the given template to the given output path.
        |
      """)
  def merge(@ExportFunctionParameterDescription(name = "template",
    description = "The name of the template within the backing Rug archive, under /.atomist / templates")
            template: String,
            @ExportFunctionParameterDescription(name = "path",
              description = "The path that will be the merged path within the output project.")
            path: String,
            @ExportFunctionParameterDescription(name = "parameters",
              description = "Parameters")
            parametersToTemplate: Any) = {
    val mc = MergeContext(mapToUse(parametersToTemplate))
    val newFile = mergeTool.mergeToFile(mc, template).withPath(path)
    updateTo(currentBackingObject + newFile)
  }

  private def mapToUse(arg: Any): Map[String, Object] = arg match {
    case ic: FunctionInvocationContext[_] => ic.identifierMap
    case som: ScriptObjectMirror =>
      NashornUtils.extractProperties(som)
  }

  @ExportFunction(readOnly = false,
    description =
      """
        |Merge templates from the specified directory in the backing archive,
        |under /.atomist/templates, to the given output path in the project being
        |edited
      """)
  def mergeTemplates(@ExportFunctionParameterDescription(name = "templatesPath",
    description = "Source template path where content will be used to merge into target project")
                     templatesPath: String,
                     @ExportFunctionParameterDescription(name = "outputPath",
                       description = "The destination path within the destination project")
                     outputPath: String,
                     @ExportFunctionParameterDescription(name = "ic",
                       description = "Parameters to the template")
                     parametersToTemplate: Any) = {
    val mc = MergeContext(mapToUse(parametersToTemplate))
    val directoryToMerge = templateContent / templatesPath
    val outputContent = mergeTool.processTemplateFiles(mc, directoryToMerge).withPathAbove(outputPath)
    updateTo(currentBackingObject + outputContent)
  }

  @ExportFunction(readOnly = false,
    description =
      """
        |Don't use. Merely intended to simplify the life of the Rug to TypeScript transpiler.
      """)
  def projects = Collections.singletonList(this)

  @ExportFunction(readOnly = false,
    description = "Files in this archive")
  def files: java.util.List[FileArtifactBackedMutableView] = {
    import scala.collection.JavaConverters._
    val files = currentBackingObject.allFiles.map(f => new FileArtifactMutableView(f, this)).asJava
    files.asInstanceOf[java.util.List[FileArtifactBackedMutableView]]
  }

  /**
    * For use by scripts. Edit the project with the given
    * map of string arguments.
    *
    * @param editorName name of editor to use
    * @param params     parameters to pass to the editor
    * @return
    */
  protected def editWith(editorName: String,
                         params: Map[String, Object],
                         context: Seq[ProjectOperation]): Unit = {
    val ed: ProjectEditor = context.collect {
      case pe: ProjectEditor if pe.name.equals(editorName) =>
        pe
    }.headOption.getOrElse(
      throw new RugRuntimeException(name, s"Cannot find project editor [$editorName]")
    )
    ed.modify(currentBackingObject, SimpleProjectOperationArguments("invocation", params)) match {
      case sm: SuccessfulModification =>
        updateTo(sm.result)
      case nmn: NoModificationNeeded => currentBackingObject
      case wtf =>
        throw new RugRuntimeException(ed.name, s"Unexpected editor failure: $wtf", null)
    }
  }

  protected def editWith(editorName: String, params: Map[String, Object]): Unit = {
    editWith(editorName, params, this.po)
  }

  @ExportFunction(readOnly = false, description = "Edit with the given editor")
  protected def editWith(
                          @ExportFunctionParameterDescription(name = "editorName",
                            description = "Name of the editor to invoke")
                          editorName: String,
                          @ExportFunctionParameterDescription(name = "params",
                            description = "Parameters to pass to the editor")
                          params: Any): Unit = {
    val m: Map[String, Object] = params match {
      case bdp: BidirectionalParametersProxy =>
        // The user is probably passing their operation's parameter object
        // back up as an argument to the invoked editor
        bdp.allMemberValues
      case som: ScriptObjectMirror =>
        // The user has created a new JavaScript object, as in { foo: "bar" },
        // to pass up as an argument to the invoked editor. Extract its properties
        NashornUtils.extractProperties(som)
    }
    editWith(editorName, m)
  }

  @ExportFunction(readOnly = true, description="Return a new Project View based on the original backing object (normally the .atomist/ directory)")
  def backingArchiveProject(): ProjectMutableView ={
    new ProjectMutableView(EmptyArtifactSource.apply(),rugAs,atomistConfig,po)
  }
  /**
    * Convenient method to apply an editor.
    */
  protected def applyEditor(
                             ed: ProjectEditor,
                             poa: ProjectOperationArguments = SimpleProjectOperationArguments.Empty): ArtifactSource = {
    ed.modify(currentBackingObject, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification => sm.result
      case nmn: NoModificationNeeded => currentBackingObject
      case wtf =>
        throw new RugRuntimeException(ed.name, s"Unexpected editor failure: $wtf", null)
    }
  }
  @ExportFunction(readOnly = true,
    description = "Provides access additional context, such as the PathExpressionEngine")
  def context = new ProjectContext(ctx)
}
class ProjectContext(ctx: UserModelContext) {

  def pathExpressionEngine() : PathExpressionExposer = {
    ctx.registry("PathExpressionEngine").asInstanceOf[PathExpressionExposer]
  }
}
