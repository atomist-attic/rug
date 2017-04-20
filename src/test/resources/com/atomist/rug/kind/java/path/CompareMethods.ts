import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {DecoratingPathExpressionEngine} from '@atomist/rug/ast/DecoratingPathExpressionEngine'
import {RichTextTreeNode} from '@atomist/rug/ast/TextTreeNodeOps'
import {Parameter} from '@atomist/rug/operations/Decorators'

export class CompareMethods implements ProjectEditor {

    name = "CompareMethods"

    description = "Compare methods"

    edit(project: Project) {
      const eng = 
      new DecoratingPathExpressionEngine(project.context.pathExpressionEngine);

      const methodsA = eng.save(project,
        "//JavaFile()[//normalClassDeclaration/*/annotation//typeName[@value='RestController']]//methodDeclaration");
      console.log(methodsA.join(","));

    }
}

export const editor = new CompareMethods()