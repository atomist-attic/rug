import {HandleCommand, Instruction, Response, GitHubPullRequest, HandlerContext, CommandPlan, DirectedMessage, ResponseMessage, LifecycleMessage, ChannelAddress} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("EdithWithTarget","Edith something with target info")
class EdithWithTarget implements HandleCommand {

  handle(ctx: HandlerContext) : CommandPlan {
    let result = new CommandPlan()
    result.add({ instruction: {
                 kind: "edit",
                 name: "blah",
                 project: "testme",
                 target: {
                    title: "PR title",
                    body: "PR body",
                    targetBranch: "target-branch",
                    sourceBranch: "source-branch",
                    kind: "github-pull-request"
                 }
               }})

    return result;
  }
}

export const command = new EdithWithTarget()

@CommandHandler("EdithWithTarget2","Edith something with target info")
class EdithWithTarget2 implements HandleCommand {

  handle(ctx: HandlerContext) : CommandPlan {
    let result = new CommandPlan()
    result.add({ instruction: {
                 kind: "edit",
                 name: "blah",
                 project: "testme",
                 target: new GitHubPullRequest("dev")
               }})

    return result;
  }
}

export const command2 = new EdithWithTarget2()

@CommandHandler("EdithWithTarget3","Edith something with target info")
class EdithWithTarget3 implements HandleCommand {

  handle(ctx: HandlerContext) : CommandPlan {
    let result = new CommandPlan()
    result.add({ instruction: {
                 kind: "edit",
                 name: "blah",
                 project: "testme",
                 target: new GitHubPullRequest()
               }})

    return result;
  }
}

export const command3 = new EdithWithTarget3()
