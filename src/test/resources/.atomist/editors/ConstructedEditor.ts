import {Project} from 'user-model/model/Core'
import {Config} from 'user-model/operations/Config'
import {ParametersSupport} from 'user-model/operations/ProjectEditor'
import {ProjectEditor} from 'user-model/operations/ProjectEditor'
import {Parameters} from 'user-model/operations/ProjectEditor'
import {PathExpression} from 'user-model/operations/PathExpression'
import {Match} from 'user-model/operations/PathExpression'
import {File} from 'user-model/model/Core'

interface ConstructedEditorConfig extends Config {

  defaultX(): string

}


abstract class JavaInfo extends ParametersSupport {

  packageName: string = null

}

class ConstructedEditor implements ProjectEditor<Parameters> {

    config: ConstructedEditorConfig

    constructor(config: ConstructedEditorConfig) { this.config = config }

    edit(project: Project, ji: JavaInfo) {

      let pe = new PathExpression<Project,File>(`/*:file[name='pom.xml']`)
      let m: Match<Project,File> = this.config.eng().evaluate(project, pe)

      var t: string = `param=${ji.packageName},filecount=${m.root().fileCount()}`
      for (let n of m.matches())
        t += `Matched file=${n.path()}`;

        var s: string = ""

        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        for (let f of project.files())
            s = s + `File [${f.path()}] containing [${f.content()}]\n`
        return `${t}\n\nEdited Project containing ${project.fileCount()} files: \n${s}`;
    }
  }
