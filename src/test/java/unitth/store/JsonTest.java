/*
 * MIT License - see LICENSE.txt
 */
package unitth.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JsonTest {

	@Test
	void roundTripsNestedValues() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("s", "a \"quoted\" \\ back\nslash\t\u0001 ü");
		m.put("l", 42L);
		m.put("d", 0.125);
		m.put("t", true);
		m.put("f", false);
		m.put("n", null);
		m.put("arr", Arrays.asList(1L, "x", List.of(), Map.of()));
		String json = Json.write(m);
		assertThat(Json.parse(json)).isEqualTo(m);
	}

	@Test
	void writesCompactJsonInInsertionOrder() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("b", 1L);
		m.put("a", List.of("x"));
		assertThat(Json.write(m)).isEqualTo("{\"b\":1,\"a\":[\"x\"]}");
	}

	@Test
	void parsesWhitespaceEscapesAndNumbers() {
		Object v = Json.parse(" { \"a\" : [ 1 , -2.5e1 , \"\\u0041\\/\\b\\f\" ] } ");
		assertThat(v).isEqualTo(Map.of("a", List.of(1L, -25.0, "A/\b\f")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "{", "[1,", "{\"a\" 1}", "{1:2}", "\"open", "tru", "1 2", "-", "\"\\u12\"", "[1}" })
	void rejectsMalformedInput(String bad) {
		assertThatThrownBy(() -> Json.parse(bad)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsUnsupportedTypes() {
		Object unsupported = new Object();
		assertThatThrownBy(() -> Json.write(unsupported)).isInstanceOf(IllegalArgumentException.class);
	}
}
