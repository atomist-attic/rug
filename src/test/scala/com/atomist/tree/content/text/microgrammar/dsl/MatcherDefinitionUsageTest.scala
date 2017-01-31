package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.parse.java.ParsingTargets
import com.atomist.tree.content.text.microgrammar.{InputState, MatcherMicrogrammar}
import org.scalatest.{FlatSpec, Matchers}

class MatcherDefinitionUsageTest extends FlatSpec with Matchers {

  val mgp = new MatcherDefinitionParser

  it should "match literal" in {
    val matcher = mgp.parseMatcher("foo", "def foo")
    matcher.matchPrefix(InputState("def foo thing")) match {
      case Right(pm) =>
    }
  }

  it should "match literal using microgrammar" in {
    val matcher = mgp.parseMatcher("lit", "def foo")
    val mg = new MatcherMicrogrammar(matcher)
    mg.findMatches("def foo bar").size should be(1)
  }

  it should "match regex using microgrammar" in {
    val matcher = mgp.parseMatcher("l", "def $foo:§f.o§")
    val mg = new MatcherMicrogrammar(matcher)
    val input = "def foo bar"
    matcher.matchPrefix(InputState(input)) match {
      case Right(pm) =>
    }
    mg.findMatches(input).size should be(1)
  }

  it should "match regex in Maven POM" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val mg = new MatcherMicrogrammar(
      mgp.parseMatcher("m", "<modelVersion>$modelVersion:§[a-zA-Z0-9_\\.]+§</modelVersion>"))
    val pom = proj.findFile("pom.xml").get.content
    val matches = mg.findMatches(pom)
    matches.size should be(1)
  }

  it should "parse Slack emoji html" in {
    val html =
      """<tr class="emoji_row">
        |													<td class="align_middle"><span data-original="https://emoji.slack-edge.com/T024F4A92/666/5b9d8b4d571e51c5.jpg" class="lazy emoji-wrapper"></span></td>
        |												<td class="align_middle" style="white-space: normal; word-break: break-all; word-wrap: break-word; height: 100%; vertical-align:middle;">:666:
        |												</td>
        |													<td class="align_middle">Image</td>
        |												<td class="author_cell hide_on_mobile" style="white-space: normal;">
        |															<a href="/team/asf" class="display_flex align_items_center break_word bold">
        |									<span class="small_right_margin"><span class="member_image thumb_24" style="background-image: url('https://secure.gravatar.com/avatar/ea56481d511928f89ecbfba1578b675b.jpg?s=48&amp;d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0025-48.png')" data-thumb-size="24" data-member-id="U024F4XCH"></span></span>
        |									asf
        |								</a>
        |													</td>
        |						<td class="align_middle align_right bold">
        |							<ts-icon class="ts_icon_times_circle subtle_silver cursor_pointer candy_red_on_hover" data-emoji-type="emoji" data-emoji-name="666" data-action="emoji.remove"></ts-icon>
        |						</td>
        |					</tr>""".stripMargin

    val mg = new MatcherMicrogrammar(
      mgp.parseMatcher("emoji",
        """<tr class="emoji_row">¡<span data-original="¡$emojiUrl:§https://[^\"]+§" class="$name:§[\sa-zA-Z0-9_\-]*§"""
      ))

    val matches = mg.findMatches(html)
    matches.size should be(1)

    withClue(matches) {
      matches.head.fieldValues.head.value should be("https://emoji.slack-edge.com/T024F4A92/666/5b9d8b4d571e51c5.jpg")
      matches.head.fieldValues(1).value should be("lazy emoji-wrapper")
    }
  }

}
