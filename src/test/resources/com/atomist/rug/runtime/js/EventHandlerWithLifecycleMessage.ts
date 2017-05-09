import {HandleEvent, EventPlan, LifecycleMessage} from '@atomist/rug/operations/Handlers'
import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'

@EventHandler("OpenIssueLifecycle", "Open issue lifecycle message", new PathExpression<TreeNode,TreeNode>("/issue"))
@Tags("github", "issues")
class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
  handle(event: Match<TreeNode, TreeNode>){
    let issue = event.root

    const message = new LifecycleMessage(issue, "issue1")
    message.addAction({
        label: "Assign",
        instruction: {
            kind: "command",
            name: "AssignGitHubIssue",
            parameters: {
                issue: "123",
                owner: "atomist",
                repo: "rus",
            },
        },
    });

    return EventPlan.ofMessage(message);
  }
}
export const handler = new SimpleHandler();