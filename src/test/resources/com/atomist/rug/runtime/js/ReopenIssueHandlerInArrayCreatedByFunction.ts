import {HandleEvent, EventPlan, LifecycleMessage, DirectedMessage, MessageMimeTypes, ChannelAddress} from '@atomist/rug/operations/Handlers'
import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'
import {createEventHandler} from "@atomist/rug/operations/HandlerUtils"

function handleKitties(event: Match<TreeNode, TreeNode>) {
    let issue = event.root
    let plan = new EventPlan()

    const message = new DirectedMessage("message1", new ChannelAddress("w00t"))

    plan.add(message);

    plan.add({ instruction: {
                 kind: "execute",
                 name: "HTTP",
                 parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"}
               },
               onSuccess: {kind: "respond", name: "Kitties"},
               onError: {body: "No kitties for you today!", kind: "directed", contentType: MessageMimeTypes.PLAIN_TEXT, usernames: ["w00t"]}
             });

    const anEmptyPlan = new EventPlan()
    const aPlansPlan = new EventPlan()
    aPlansPlan.add(new DirectedMessage("this is a plan that is in another plan", new ChannelAddress("w00t")))

    aPlansPlan.add({ instruction: {
                       kind: "generate",
                       name: "createSomething"
                     },
                     onSuccess: anEmptyPlan
                   })

    plan.add({ instruction: {
                 kind: "edit",
                 name: "modifySomething",
                 parameters: {message: "planception"}
               },
               onSuccess: aPlansPlan,
               onError: {body: "error", kind: "directed", contentType: MessageMimeTypes.PLAIN_TEXT, usernames: ["w00t"]}
             });
    return plan;
 }


export const handlers = [
    createEventHandler("ClosedIssueReopener", "Reopens closed issues",
    new PathExpression<TreeNode,TreeNode>("/issue"),
    handleKitties,
    [ "cats", "kittens"] )
];