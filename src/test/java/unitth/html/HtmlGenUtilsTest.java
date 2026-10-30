/*
 * MIT License - see LICENSE.txt
 */
package unitth.html;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlGenUtilsTest {

	@Test
	void escapesHtmlSpecialCharacters() {
		assertThat(HtmlGenUtils.escapeHtml("<script>alert('x')</script> & \"q\""))
				.isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;q&quot;");
		assertThat(HtmlGenUtils.escapeHtml("AI: Avg DQ score >= 90")).isEqualTo("AI: Avg DQ score &gt;= 90");
		assertThat(HtmlGenUtils.escapeHtml(null)).isEmpty();
	}

	@Test
	void fileSafeKeepsExistingNamesAndNeutralisesTheRest() {
		assertThat(HtmlGenUtils.fileSafe("zbrainiac-labs_project-one.SQLValidation"))
				.isEqualTo("zbrainiac-labs_project-one.SQLValidation");
		assertThat(HtmlGenUtils.fileSafe("../../etc/passwd")).isEqualTo("_._.._etc_passwd");
		assertThat(HtmlGenUtils.fileSafe("a b<c>\"d")).isEqualTo("a_b_c__d");
		assertThat(HtmlGenUtils.fileSafe("")).isEqualTo("_");
	}
}
