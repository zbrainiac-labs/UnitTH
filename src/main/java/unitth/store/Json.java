/*
 * MIT License - see LICENSE.txt
 */
package unitth.store;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader/writer for the run store. Objects map to
 * <code>LinkedHashMap</code>, arrays to <code>List</code>, numbers to
 * <code>Double</code> or <code>Long</code>.
 */
public final class Json {

	private final String s;
	private int pos;

	private Json(String s) {
		this.s = s;
	}

	/** Parses a JSON document. */
	public static Object parse(String text) {
		Json p = new Json(text);
		Object v = p.value();
		p.ws();
		if (p.pos != text.length()) {
			throw p.error("Trailing characters");
		}
		return v;
	}

	/** Serialises maps, lists, strings, numbers, booleans and null. */
	public static String write(Object v) {
		StringBuilder sb = new StringBuilder();
		write(sb, v);
		return sb.toString();
	}

	private static void write(StringBuilder sb, Object v) {
		if (v == null) {
			sb.append("null");
		} else if (v instanceof String str) {
			writeString(sb, str);
		} else if (v instanceof Number || v instanceof Boolean) {
			sb.append(v);
		} else if (v instanceof Map<?, ?> m) {
			writeObject(sb, m);
		} else if (v instanceof List<?> l) {
			writeArray(sb, l);
		} else {
			throw new IllegalArgumentException("Not serialisable: " + v.getClass());
		}
	}

	private static void writeObject(StringBuilder sb, Map<?, ?> m) {
		sb.append('{');
		boolean first = true;
		for (Map.Entry<?, ?> e : m.entrySet()) {
			sb.append(first ? "" : ",");
			first = false;
			writeString(sb, String.valueOf(e.getKey()));
			sb.append(':');
			write(sb, e.getValue());
		}
		sb.append('}');
	}

	private static void writeArray(StringBuilder sb, List<?> l) {
		sb.append('[');
		for (int i = 0; i < l.size(); i++) {
			sb.append(i > 0 ? "," : "");
			write(sb, l.get(i));
		}
		sb.append(']');
	}

	private static void writeString(StringBuilder sb, String str) {
		sb.append('"');
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			switch (c) {
			case '"' -> sb.append("\\\"");
			case '\\' -> sb.append("\\\\");
			case '\n' -> sb.append("\\n");
			case '\r' -> sb.append("\\r");
			case '\t' -> sb.append("\\t");
			default -> {
				if (c < 0x20) {
					sb.append(String.format("\\u%04x", (int) c));
				} else {
					sb.append(c);
				}
			}
			}
		}
		sb.append('"');
	}

	private IllegalArgumentException error(String msg) {
		return new IllegalArgumentException(msg + " at position " + pos);
	}

	private void ws() {
		while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
			pos++;
		}
	}

	private Object value() {
		ws();
		if (pos >= s.length()) {
			throw error("Unexpected end");
		}
		char c = s.charAt(pos);
		switch (c) {
		case '{':
			return object();
		case '[':
			return array();
		case '"':
			return string();
		case 't':
			return literal("true", Boolean.TRUE);
		case 'f':
			return literal("false", Boolean.FALSE);
		case 'n':
			return literal("null", null);
		default:
			return number();
		}
	}

	private Object literal(String word, Object v) {
		if (!s.startsWith(word, pos)) {
			throw error("Unexpected token");
		}
		pos += word.length();
		return v;
	}

	private Map<String, Object> object() {
		Map<String, Object> m = new LinkedHashMap<>();
		pos++;
		ws();
		if (peek('}')) {
			return m;
		}
		do {
			ws();
			if (pos >= s.length() || s.charAt(pos) != '"') {
				throw error("Expected key");
			}
			String key = string();
			ws();
			expect(':');
			m.put(key, value());
			ws();
		} while (peek(','));
		expect('}');
		return m;
	}

	private List<Object> array() {
		List<Object> l = new ArrayList<>();
		pos++;
		ws();
		if (peek(']')) {
			return l;
		}
		do {
			l.add(value());
			ws();
		} while (peek(','));
		expect(']');
		return l;
	}

	private boolean peek(char c) {
		if (pos < s.length() && s.charAt(pos) == c) {
			pos++;
			return true;
		}
		return false;
	}

	private void expect(char c) {
		if (!peek(c)) {
			throw error("Expected '" + c + "'");
		}
	}

	private String string() {
		StringBuilder sb = new StringBuilder();
		pos++;
		while (pos < s.length()) {
			char c = s.charAt(pos++);
			if (c == '"') {
				return sb.toString();
			}
			if (c == '\\') {
				escape(sb);
			} else {
				sb.append(c);
			}
		}
		throw error("Unterminated string");
	}

	/* Appends the character of the escape sequence after a backslash. */
	private void escape(StringBuilder sb) {
		if (pos >= s.length()) {
			throw error("Unterminated string");
		}
		char e = s.charAt(pos++);
		switch (e) {
		case 'n' -> sb.append('\n');
		case 'r' -> sb.append('\r');
		case 't' -> sb.append('\t');
		case 'b' -> sb.append('\b');
		case 'f' -> sb.append('\f');
		case 'u' -> {
			if (pos + 4 > s.length()) {
				throw error("Bad unicode escape");
			}
			sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
			pos += 4;
		}
		default -> sb.append(e);
		}
	}

	private Number number() {
		int start = pos;
		while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
			pos++;
		}
		String n = s.substring(start, pos);
		if (n.isEmpty()) {
			throw error("Unexpected character");
		}
		try {
			if (n.indexOf('.') < 0 && n.indexOf('e') < 0 && n.indexOf('E') < 0) {
				return Long.parseLong(n);
			}
			return Double.parseDouble(n);
		} catch (NumberFormatException e) {
			throw error("Bad number '" + n + "'");
		}
	}
}
