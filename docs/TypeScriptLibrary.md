# Rug TypeScript Library

## Overview

In the case of AST-backed `TreeNode` implementations, there is little or no logic in their backing Scala objects, as they consist merely of `UpdatableTreeNode`
instances that offer only the ability to navigate the tree and update any point within a tree using a string. 

For example, it is possible to obtain a node for a Scala `match` statement using a path expression, but there is no method to add a `case` clause without modifying the existing string content of the entire `match`. 

However, it is important to offer the user such convenience methods, which hide the building the appropriate updated string. Their implementation will  often navigate descendant nodes, but work purely with the information and update methods exposed by the backing node structure.

The best place to add such methods is in TypeScript, rather than Scala, for several reasons:

- It simplifies the JVM implementation significantly, as we don't need to preserve operations through tree transforms
- TypeScript is a more accessible language for end users than Scala 
- It's possible to update or add logic via `node` without updating the entire Rug version

Thus Rug includes a TypeScript class library with the following purposes:

- TypeScript interfaces for core concepts such as `TreeNode` and `PathExpressionEngine`. These objects are normally backed at runtime by Nashorn objects.
- Decoration of `TreeNode` instances at runtime with additional functionality, written purely in TypeScript. _This is  how we add the operations described above._
- Other utility classes and functions, not backed by Nashorn.

The second of these topics is the most complex and important. This document will first describe the end user experience and then the steps involved in implementing this decoration for a new tree structure.

## Node Decorators

From the user perspective, TypeScript tree nodes returned as a result of evaluating path expressions contain both the underlying primitive functions, but convenient navigation functions for working with known children and functions hiding manipulation and evaluation logic.

This is achieved at runtime by mixins, using [TypeScript intersection types](https://www.typescriptlang.org/docs/handbook/advanced-types.html).

The following UML diagram illustrates how types can be combined to satisfy the `match` case scenario outlined above. When we query for the node matching a `match` we obtain a `TextTreeNode`. However, we want to expose functions such as `addClause` to user, and we want to add navigation functions with names corresponding to the child nodes, to give users easy code completion.

The result will look like this to the user:

```                                                                                                                                                                                                                                                                                                                                                                                                                                   
                                                                          
                        ┌─────────────────┐  common                       
                        │  TextTreeNode   │ functions                     
                        └─────────────────┘                               
                                 △                                        
                                 │                                        
                        ┌─────────────────┐  ┌─────────────────┐          
                        │    MatchNav     │  │   MatchNavOps   │          
                        └─────────────────┘  └─────────────────┘          
                  navigation     △                    △      mixed in     
                  functions      └───────────┐┌───────┘  functions adding 
              (return children)              ││             operations    
                                    ┌─────────────────┐                   
                                    │      Match      │                   
                                    └─────────────────┘                   
                                                returned by               
                                                 decorator                
                                                                                    
```
The types in teh diagram are as follows:

| Name  |  Type | Authored  | Contains  |  Notes |
|---|---|---|---|---|
|  `TextTreeNode` | interface  |  Infrastructure method |  Methods to navigate providing child names as strings. String `value` and `update` methods |   |
|  `MatchNav` |  interface |  Hand* | Navigation methods _correspondly exactly to child node names_.  | There's no need to implement this interface. The JVM proxy automatically dispatches such methods. If such an interface isn't supplied, those functions can still be used in JavaScript or via TypeScript detyping to `any`.   |
|  `MatchNavOps` | class   |  Hand |  Operations such as `addClause` which work with the wrapped node |   Extends library class `TextTreeNodeOps`. |
| `Match` | mixin | Hand | `MatchNav & MatchOps`: All operations on both types | Type the user declares, for example in a `with` call.

*Currently `Nav` interfaces are supplied by hand. However, they could be generated in cases where child node names are known. (This will depend on whether there's an underlying grammar.)

Usage will look like this:

```
eng.with<scala.TermApply>(project, "/src/main/scala/ScalaFileType()//match", match => {
        match.addCaseClause("case _ => ???") 
}
        
```


## Other Helpers
Notable other TypeScript helpers in the `rug` module include:

- `TreeHelper`: Useful for finding the ancestor of a node meeting certain criteria

## How To Implement Node Decoration for a New Tree

### Adding TypeScript decoration
There are three possible steps to enhance the experience around tree nodes. None is essential, but all play a role in achieving the greatest overall functionality:

1. Provide a `Nav` interface providing navigation methods for the class. The names must correspond *exactly* to the names of child nodes, including case and possible use of `_` instead of camel case. There is no need to provide implementations for these methods, as they are implemented by the underlying JVM proxy. The interface merely acts as a convenience for users, who gain tooling support.
- Provide an `Ops` class offering additional convenience methods. This should extend the `TextTreeNodeOps` class. This contains the logic of operations like `addClause`. Such functions will often navigate using the `Nav` interface.
- Write a decorator for the class that extends `DecoratingPathExpressionEngine`. If you do not do this, you can use `DecoratingPathExpressionEngine` directly to ensure that at least `TextTreeNodeOps` convenience methods are mixed in to all nodes.

Normally these types be in their own module, which can be packaged as a separate `node` module. Users can fall back to regular `TreeNode` methods if they don't have these interfaces and classes. They can access the nav methods by detyping to `any`.

You do not need to provide a nav interface or helper class for all tree nodes in your structure; only the ones that are particularly interesting.

#### Examples
Consider a Scala type. We want to provide a method to make it easy to add content to the beginning of it.

First, we perform step (1), defining a nav class. Each method corresponds to a possible child of the target node type. (Refer to the grammar or print out a tree structure at runtime to see what the possible types are.)

```
export interface DefnClassNav extends TextTreeNode {

    typeName(): string

    template(): TextTreeNode
    
    ...
     
}
```

Next, step (2): Implement an Ops class that adds operations to this type. In this case, we want to add a method that adds to the beginning of the node. We have access to `this.node` and the `PathExpressionEngine` inherited from the superclass. We can also call superclass convenience methods such as `append` and `prepend`.

```

export class DefnClassOps extends TextTreeNodeOps<DefnClassNav> {

    /**
     * Add this content to the beginning of the body
     */
    addToStartOfBody(what: string) {
        let firstStatement = this.node.template().children()[0] as TextTreeNode
        firstStatement.update(`${what}\n\n${firstStatement.value()}`)
    }
    
}

export type DefnClass = DefnClassNav & DefnClassOps
```

Now, Step (3): Extend `DecoratingPathExpressionEngine ` to provide a custom decorator, which instantiates decorator ops classes. Using `new <module name>[class name]` is a common and convenient pattern, but it's also possible to instantiate desired classes directly.

```
import {TreeNode} from "../../tree/PathExpression"
import {DecoratingPathExpressionEngine} from "../DecoratingPathExpressionEngine"

import * as scala from "./Types"

export class ScalaPathExpressionEngine extends DecoratingPathExpressionEngine {

    protected decoratorFor(n: TreeNode): any {
        let className = this.decoratorClassName(n)
        // Dynamically instantiate the class with the given name in the
        // target module
        let ops = new scala[className](n, this.delegate)
        return ops
    }
}
```

When writing Rug operations, this new `PathExpressionEngine` should be used to wrap the default one returned from the context, which doesn't know about our custom TypeScript library. We can then work with the decorated types for our target nodes as follows:

```
// Import the desired path expression engine
import {ScalaPathExpressionEngine} from '@atomist/rug/ast/scala/ScalaPathExpressionEngine'

// Import all the added types from our module
// The module can have any name.
import * as scala from '@atomist/rug/ast/scala/Types'

... 

edit(project: Project) {
	  // Create a decorated PathExpressionEngine
      let eng: PathExpressionEngine =
        new ScalaPathExpressionEngine(project.context().pathExpressionEngine())

      let printlnStatement = 
        `/src/Directory()/scala//ScalaFile()//termApply
            [/termName[@value='println'] or contains(termSelect, 'System.out.println')]`   

      eng.with<scala.TermApply>(project, printlnStatement, termApply => {
        let newContent = termApply.value()
            .replace("System.out.println", this.logStatement)
            .replace("println", this.logStatement)
        termApply.update(newContent)
      })

      // We now may have files that use but don't import the logger. Fix them.
      let nonImportingFiles = 
        `/src/Directory()/scala//ScalaFile()[//termApply
            [contains(termSelect, '${this.logStatement}')]]` 
      eng.with<scala.Source>(project, nonImportingFiles, source => {
        source.addImport(this.loggerImport)
      })

      // We may have classes using the logger that don't instantiate it. Fix them.
      let nonInstantiatingTypes = 
        `/src/Directory()/scala//ScalaFile()//defnClass[//termApply
            [contains(termSelect, '${this.logStatement}')]]` 
      eng.with<scala.DefnClass>(project, nonInstantiatingTypes, type => {
        type.addToStartOfBody(this.loggerInstantiation)
      })
  }
```


