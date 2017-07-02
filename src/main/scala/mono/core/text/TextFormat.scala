package mono.core.text

import play.twirl.api.Html

object TextFormat {
  def toHtml(markdown: String): Html = Html({
    import org.commonmark.parser.Parser
    import org.commonmark.renderer.html.HtmlRenderer
    val parser = Parser.builder.build
    val document = parser.parse(markdown)
    val renderer = HtmlRenderer.builder.build
    renderer.render(document)
  })
}
