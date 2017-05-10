import {Project,File} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {ScalaPathExpressionEngine} from '@atomist/rug/ast/scala/ScalaPathExpressionEngine'
import * as scala from '@atomist/rug/ast/scala/Types'
import {ReviewComment,Severity} from "@atomist/rug/operations/RugOperation"

/**
 * Removes printlns
 */
@Editor("Edits")
class RemovePrintlns  {

    /*
      Our target looks like this:
     
      termApply:[ScalaMetaTreeBacked, -dynamic, termApply]
            termName:[println]
            termApplyInfix:[ScalaMetaTreeBacked, -dynamic, termApplyInfix]
              lit:[1]
              termName:[+]
              lit:[2]
     */
    edit(project: Project) {
      let eng: PathExpressionEngine =
        new ScalaPathExpressionEngine(project.context.pathExpressionEngine)

      let printlnStatement = 
        `/src/Directory()/scala//ScalaFile()//termApply
            [/termName[@value='println'] or contains(termSelect, 'System.out.println')]`   

      eng.with<scala.TermApply>(project, printlnStatement, termApply => {
        termApply.delete();
        if (termApply.containingFile() == null)
            throw new Error("Can't determine file");
        // Verify review comment behavior
        const rc = termApply.commentConcerning("Something's wrong", Severity.Major);
        if (rc.line <= 1) throw new Error(`line of ${rc.line} is wrong`);
        if (!rc.projectName) throw new Error("Expected project name");
      })
  }

}

export const editor = new RemovePrintlns();
