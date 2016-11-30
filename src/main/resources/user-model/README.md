# user-model
TypeScript model for user-authored code

## installation

To make our model classes and interfaces work with the compiler and with jvm-npm
for running it in Nashorn, read the following instructions.

Clone this repo into a folder called `node_modules`. `node_modules` needs to be
in one of the parent folders of your project hosting your TypeScript Editor implementations.

Eg. the following layout will work

```
+- workspace
   +- node_modules
      +- user-model
   +- my-first-editor-project
      +- .atomist
         +- editors
            +- MyEditor.ts
```
