import {HandleCommand, Instruction, Response, HandlerContext, Plan, DirectedMessage, ResponseMessage, LifecycleMessage, ChannelAddress} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
class KittieFetcher implements HandleCommand {

  @Parameter({description: "his dudeness", pattern: "^.*$"})
  name: string

  handle(ctx: HandlerContext) : Plan {
    let pxe = ctx.pathExpressionEngine

    if(this.name != "el duderino") {
      throw new Error("This will not stand");
    }
    let result = new Plan()
    result.add({ instruction: {
                 kind: "execute",
                 name: "HTTP",
                 parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"}
               },
               onSuccess: {kind: "respond", name: "Kitties"},
               onError: {body: "No kitties for you today!", kind: "response", contentType: "text/plain"}})


    result.add({ instruction: {
                                kind: "execute",
                                name: "HTTP",
                                parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"}
                              },
                              onSuccess: {kind: "respond", name: "Kitties"},
                              onError: new DirectedMessage("directed", new ChannelAddress("woot"))})

    result.add(new DirectedMessage("directed", new ChannelAddress("engineering")));
    result.add(new ResponseMessage("response"));
    return result;
  }
}

export const command = new KittieFetcher()
