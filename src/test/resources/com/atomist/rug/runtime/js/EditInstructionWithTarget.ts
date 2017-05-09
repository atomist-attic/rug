import {HandleCommand, Instruction, Response, GitHubPullRequest, GitHubBranch, HandlerContext, CommandPlan, DirectedMessage, ResponseMessage, LifecycleMessage, ChannelAddress} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("Edit something with target info")
class EditWithTarget implements HandleCommand {

  handle(ctx: HandlerContext) : CommandPlan {
    let result = new CommandPlan()
    result.add({ instruction: {
                 kind: "edit",
                 name: "blah",
                 project: "testme",
                 target: {
                    title: "PR title",
                    body: "PR body",
                    baseBranch: "base-branch",
                    headBranch: "head-branch",
                    kind: "github-pull-request"
                 }
               }})

    return result;
  }
}

export const command = new EditWithTarget()

@CommandHandler("Edit something with target info")
class EditWithTarget2 implements HandleCommand {

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

export const command2 = new EditWithTarget2()

@CommandHandler("Edit something with target info")
class EditWithTarget3 implements HandleCommand {

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

export const command3 = new EditWithTarget3()

@CommandHandler("Edit something with target info")
class EditWithTarget4 implements HandleCommand {

  handle(ctx: HandlerContext) : CommandPlan {
    let result = new CommandPlan()
    result.add({ instruction: {
                 kind: "edit",
                 name: "blah",
                 project: "testme",
                 target: new GitHubBranch("development")
               }})

    return result;
  }
}

export const command4 = new EditWithTarget4()

@CommandHandler("Edit something with target info")
class EditWithTarget5 implements HandleCommand {

  handle(ctx: HandlerContext) : CommandPlan {
    let result = new CommandPlan()
    result.add({ instruction: {
                 kind: "edit",
                 name: "blah",
                 project: "testme",
                 target: new GitHubBranch("master", "feature")
               }})

    return result;
  }
}

export const command5 = new EditWithTarget5()
