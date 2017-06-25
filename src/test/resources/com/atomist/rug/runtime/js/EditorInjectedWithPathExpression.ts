import {Project, File} from '@atomist/rug/model/Core'
import {Match, PathExpression, PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Editor, Parameter, Tags} from '@atomist/rug/operations/Decorators'

@Tags("java", "maven")
@Editor("Constructed", "A nice little editor")
class ConstructedEditor {

    @Parameter({description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100})
    packageName: string

    edit(project: Project) {

      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
      let pe = new PathExpression<Project,File>(`/File()[@name='pom.xml']`)
      let m: Match<Project,File> = eng.evaluate(project, pe)

      var t: string = `param=${this.packageName},filecount=${m.root.fileCount}`
      console.log("Length: " + m.matches.length)
      for (let n of m.matches) {
        t += `Matched file=${n.path}`;
        n.append("randomness")
      }

        let s: string = ""

        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        for (let f of project.files)
            s = s + `File [${f.path}] containing [${f.content}]\n`
    }
  }
  export let myeditor = new ConstructedEditor()
