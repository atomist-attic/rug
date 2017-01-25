import {Project, File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'


class FruitererType implements TypeProvider {

 typeName = "fruiterer"

 private isFile(f: TreeNode): f is File {
   return true
 }

 find(context: TreeNode): TreeNode[] {
   if (this.isFile(context)) {
     let f = context as File
     console.log(f.name())
     if (f.isJava())
      return [ new Fruiterer() ]
      else return []
   }
   else 
    return []
 }

}

class Fruiterer implements TreeNode {

  nodeName(): string { return "fruiterer" }

  nodeType(): string[] { return [ this.nodeName()] }

  value(): string { return "" }

  update(newValue: string) {}

  children() { return [ new Banana(), new Pear() ] }

}

class Banana implements TreeNode {

  nodeName(): string { return "banana" }

  nodeType(): string[] { return [ this.nodeName()] }

  value(): string { return "yellow" }

  update(newValue: string) {}

  children() { return [] }

}

class Pear implements TreeNode {

  nodeName(): string { return "pear" }

  nodeType(): string[] { return [ this.nodeName()] }

  value(): string { return "green" }

  update(newValue: string) {}

  children() { return [] }

}

class TwoLevel implements ProjectEditor {

    name: string = "Constructed"
    description: string = "Uses single microgrammar"

    edit(project: Project) {
      //console.log("Editing")
      let mg = new FruitererType()
      let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg)

      let i = 0
      eng.with<Banana>(project, "//File()/fruiterer()/banana()", n => {
        //console.log("Checking color of banana")
        if (n.value() != "yellow")
         throw new Error(`Banana is not yellow but [${n.value()}]. Sad.`)
        i++
      })
      if (i != 2)
       throw new Error(`Two bananas should have been tested, not ${i}. Sad.`)
    }
  }
export let editor = new TwoLevel();