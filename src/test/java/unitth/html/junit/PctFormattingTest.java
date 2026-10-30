/*
 * MIT License - see LICENSE.txt
 */
package unitth.html.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import unitth.core.TestItemUtils;

class PctFormattingTest {

	@Test
	void pctTextShowsNotAvailableForNaN() {
		assertThat(HtmlGen.pctText("NaN")).isEqualTo("N/A");
		assertThat(HtmlGen.pctText(null)).isEqualTo("N/A");
		assertThat(HtmlGen.pctText("75.00")).isEqualTo("75.00%");
	}

	@Test
	void jsPctEmitsNullGapForNaN() {
		assertThat(HtmlGen.jsPct(Double.NaN)).isEqualTo("null");
		assertThat(HtmlGen.jsPct(42.0)).isEqualTo("42.0");
	}

	@Test
	void numbersUseDotRegardlessOfDefaultLocale() {
		Locale saved = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			assertThat(HtmlGen.jsPct(50.0)).isEqualTo("50.0");
			assertThat(TestItemUtils.passPctToString(50.0)).isEqualTo("50.00");
		} finally {
			Locale.setDefault(saved);
		}
	}
}
