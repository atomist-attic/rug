# Varieties of Tree Nodes

Rug deals in tree nodes. When it understands code, it understands it as a tree, a hierarchy
of groupings of textual elements. These are TextTreeNodes, for text. 

When Rug understands a Project, it has TreeNodes that it builds on top of the ArtifactSource.
It's a parallel hierarchy; Rugs do updates on the MutableView hierarchy, and it manages those
 while the ArtifactSource structure remains immutable.
They parallel hierarchies look like this:

```
                          ┌───────────────────┐                                 ┌──────────────┐                  
                          │                   │                                 │              │                  
                          │ProjectMutableView │─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ▶│ArtifactSource│                  
                          │                   │                                 │              │                  
                          └───────────────────┘                                 └──────────────┘                  
                                    │                                                   │                         
                                    ├─────────────────┐                             ┌───┤                         
                                    │                 ▼                             ▼   └─────────────────┐       
                  ┌─────────────────┘  ┌─────────────────────────────┐      ┌──────────────┐              │       
                  │                    │                             │      │              │              │       
                  │                    │FileArtifactBackedMutableView│─ ─ ─▶│ FileArtifact │              ▼       
                  │                    │                             │      │              │      ┌──────────────┐
                  ▼                    └─────────────────────────────┘      └──────────────┘      │              │
   ┌─────────────────────────────┐                                ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─▶│ FileArtifact │
   │                             │                               │                                │              │
   │FileArtifactBackedMutableView│─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─                                 └──────────────┘
   │                             │                                                                                
   └─────────────────────────────┘                                                                                
```

## Text Nodes

Once we get into text tree nodes, this is all in Rug, and the text nodes can be mutable.


```
               ┌───────────────────┐                    ┌────────────────────┐                       
               │                   │                    │                    │                       
───parsing────▶│PositionedTreeNode │─────makeReady─────▶│ UpdatableTreeNode  │───────▶  proceed with 
               │                   │                    │                    │            updates    
               └───────────────────┘                    └────────────────────┘                       
```


## PositionedTreeNode

Conceptually, when a file is parsed, the result is a hierarchy of PositionedTreeNodes.
These nodes know their offset in the file, their name, maybe some type information.

These PositionedTreeNodes are then combined with the full file contents, and restructured to
* eliminate structural noise (extra layers that were handy for parsing)
* correct any double-allocated characters (this happens in the Python ANTLR parsing with newlines)
* introduce padding nodes (unnamed text segments) so that every character in the file is accounted for.

This operation is called "pad" as the last of these operations is most important.

## UpdatableTreeNode

Only after padding can nodes provide their value. Only after padding can values be updated.

After padding, every character in the file belongs to exactly one node. Offsets
are discarded; the contents can be reconstructed by traversing the tree and concatenating
node values.

The output of padding is an UpdatableTreeNode structure.
The value() and update() methods are available here.
(Currently value() is on every tree node. But you get an exception when you call it on a PositionedTreeNode. Bummer.)

UpdatableTreeNode is new; its intention is to replace MutableTreeNode which
has more baggage. MutableTreeNodes want to append children and blah blah blah.
We have observed that it is sufficient to have update(), which overwrites the
entire value of a tree node with text.

## MutableView

When tree nodes are immutable, such as ArtifactSource and FileArtifact, then
Rug programs can't interact with them directly. We construct a parallel hierarchy
of MutableView TreeNodes on top of them. These are ProjectMutableView
and FileArtifactBackedMutableView, respectively.

In the case of TextTreeNodes, the nodes are mutable, so we can use the same object.
An UpdatableTreeNode can interface directly with Rug.

But old implementations of TreeNode (MutableContainerTreeNode, MutableTerminalTreeNode) 
still have separate MutableViews even though they are mutable. These are deprecated.

# Implementation

ImmutablePositionedTreeNode is the target implementation. It serves just fine as a
PositionedTreeNode. For historical reasons,
 old-style MutableContainerTreeNode and MutableTerminalTreeNode
are serving as PositionedTreeNodes.

## OverwritableTextTreeNode

After the pad operation, we get a hierarchy of OverwritableTextTreeNodes.
Each of these has meaning (it has a name that you can navigate to). They also
have children that are PaddingTreeNodes. All the text lives in PaddingTreeNodes.
PaddingTreeNodes aren't visible in the hierarchy (they aren't returned by childNodes),
but they impact the value of the node.

Any of the OverwritableTextTreeNodes can have its entire contents replaced by
a new string of text. This is the only value-mutating operation.

When a Rug updates the value of an OverwritableTextTreeNodes, all its children are invalidated
(they conceptually don't exist, so from then on they throw exceptions on access,
in case something in a TypeScript Rug is holding on to them).
Next, an update notification moves up the hierarchy; each parent node updates
its value to the concatenation of its children again.
The sequence of childNodes on an OTTN never changes,
but the value of each child can change.

## OverwritableTextInFile

When any node's value is changed, the update notification reaches the
FileArtifactBackedMutableView via the very top level UpdatableTreeNode,
which is an OverwritableTextInFile node - this acts as an adapter between the
OTTN hierarchy and the FileArtifactMutableView.

It also serves as the root node for OTTNs to find their formatInfo. The formatInfo
method on a Text TreeNode supplies its current offset and indentation information to Rugs.

It also contains the padding before, between, and after microgrammar matches.
Something has to hold the entire contents of the file when the parsed nodes don't.

(microgrammars don't exist in Scala anymore but maybe a custom language extension might only represent
part of the file as a tree? it could happen.)

The completed, ready-for-Rug-operations structure looks like this,
for a microgrammar that looks for simple Scala methods:

```
                   ┌───────────────────────────────────────────┐                      
                   │       FileArtifactBackedMutableView       │                      
                   │              Whatever.scala               │                      
                   └───────────────────────────────────────────┘                      
                                         ▲                                            
                                         │                                            
                                         │                                            
                           ┌──────────────────────────┐                               
                           │                          │                               
                           │  OverwritableTextInFile  │                               
                           │                          │                               
                           └──────────────────────────┘                               
                                         │                                            
                                         │                                            
               ┌─────────────────────────┼──────────────────────┐                     
               │                         │                      │                     
               ▼                         ▼                      ▼                     
          ┌─────────┐     ┌────────────────────────────┐   ┌─────────┐                
          │ Padding │     │  OverwritableTextTreeNode  │   │ Padding │                
          └─────────┘     │        scalaMethod         │   └─────────┘                
                          └────────────────────────────┘                              
                                         │                                            
                                         │                                            
     ┌──────────────────────┬────────────┴───────┬─────────────────────┐              
     │                      │                    │                     │              
     ▼                      ▼                    ▼                     ▼              
┌─────────┐  ┌────────────────────────────┐ ┌─────────┐ ┌────────────────────────────┐
│ Padding │  │  OverwritableTextTreeNode  │ │ Padding │ │  OverwritableTextTreeNode  │
│ "def "  │  │         methodName         │ │ "() = " │ │         methodBody         │
└─────────┘  └────────────────────────────┘ └─────────┘ └────────────────────────────┘
                            │                                          │              
                            │                                          │              
                            ▼                                          ▼              
                       ┌─────────┐                       ┌───────────────────────────┐
                       │ Padding │                       │          Padding          │
                       │"doStuff"│                       │`println("This is great")` │
                       └─────────┘                       └───────────────────────────┘
```

(graphic created in MonoDraw)

Note that the OTTNs always calculate the value by summing the values of their children, and the actual text is stored only in Padding nodes. (The exception is: if you call update on a node, then it stores that and ignores its children.)

## Lifecycle

This took some construction. Each TextTreeNode knows its parent and its root.
They have to be constructed bottom-up, and then informed about their parents
with a mutating method call.
Later, when a Rug updates a node, it becomes overwritten, and all its
children become invalidated. Its lifecycle looks like this:

```
                     │
               construction
                     │
                     ▼
             ┌───────────────┐
             │               │
             │    Unready    │
             │               │
             └───────────────┘
                     │
                 setParent
                     │
                     ▼
             ┌───────────────┐
             │               │
        ┌────│     Ready     │────┐
        │    │               │    │
        │    └───────────────┘    │
     update                  invalidate
        │                         │
        ▼                         ▼
┌───────────────┐         ┌───────────────┐
│               │         │               │
│  Overwritten  │         │  Invalidated  │
│               │         │               │
└───────────────┘         └───────────────┘
```

So, OverwritableTextTreeNode is mutable, but only in very specific state
transitions.

# State of the Code

In PR317, OverwritableTextTreeNodes replace MutableContainerMutableView,
MutableContainerTreeNode, MutableTerminalMutableView, and ScalarValueView
for Antlr file types, post-pad.

Sadly, all the more complicated types still exist. Fully extracting them is
another project. It is time to get comments on this structure, first.
Then, we could use ImmutablePositionedTreeNode as the output of parsing.
And then, we can look at extracting, converting, or replacing the older
Rug language extensions.

In the meantime, we can move forward with these general, simple TreeNodes.
Any functionality we would like to add that is specific to certain nodes, we
can add in TypeScript!
