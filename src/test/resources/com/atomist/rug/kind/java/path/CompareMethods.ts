import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import { stringify, nodeReplacer } from '@atomist/rug/tree/TreeHelper'

import {DecoratingPathExpressionEngine} from '@atomist/rug/ast/DecoratingPathExpressionEngine'
import {RichTextTreeNode} from '@atomist/rug/ast/TextTreeNodeOps'
import {Parameter} from '@atomist/rug/operations/Decorators'

import { structurallyEquivalent } from "@atomist/rug/ast/TreeDiff"

import * as java from "@atomist/rug/ast/java/Structures"
import { withAnnotation, annotatedClass } from "@atomist/rug/ast/java/Expressions"

function springExportedMethodsIn(eng: DecoratingPathExpressionEngine, f: File): java.Method[] {
    return eng.save<RichTextTreeNode>(f,
        `/JavaFile()
            [//${annotatedClass('RestController')}]
            //methodDeclaration${withAnnotation('RequestMapping')}`)
            .map(md => {
                const m = new java.Method(eng, md);
                //console.log(JSON.stringify(m, nodeReplacer()));
                //console.log(stringify(m.methodDeclaration));
                return m;
            });
}

export class CompareMethods implements ProjectEditor {

    name = "CompareMethods"

    description = "Compare methods"

    edit(project: Project) {
      const eng = new DecoratingPathExpressionEngine(project.context.pathExpressionEngine);

      eng.with<File>(project, "//File()[/JavaFile()]", f => {
         const methods = springExportedMethodsIn(eng, f);
         //console.log(JSON.stringify(methods));
      });

      //for (let m of methodsA) {
       // console.log("Equivalent=" + structurallyEquivalent(m.methodDeclaration, m.methodDeclaration));
      //}

    }
}

export const editor = new CompareMethods()
