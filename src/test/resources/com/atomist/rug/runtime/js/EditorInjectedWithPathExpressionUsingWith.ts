import {Project} from '@atomist/rug/model/Core'
import {PathExpression} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {File} from '@atomist/rug/model/Core'
import {Editor, Parameter, Tags} from '@atomist/rug/operations/Decorators'


@Tags("java", "maven")
@Editor("Constructed", "A nice little editor")
class ConstructedEditor {

    @Parameter({description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100})
    packageName: string

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
      project.files.filter(t => false)
      var t: string = `param=${this.packageName},filecount=${project.fileCount}`

      eng.with<File>(project, "/*[@name='pom.xml']", n => {
        t += `Matched file=${n.path}`;
        n.append("randomness")
      })

        var s: string = ""

        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        for (let f of project.files)
            s = s + `File [${f.path}] containing [${f.content}]\n`

        //`${t}\n\nEdited Project containing ${project.fileCount} files: \n${s}`)
    }
  }

  export let myeditor = new ConstructedEditor()