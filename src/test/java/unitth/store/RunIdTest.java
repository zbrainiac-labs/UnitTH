/*
 * MIT License - see LICENSE.txt
 */
package unitth.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunIdTest {

	private static File write(Path dir, String name, String content) throws Exception {
		Files.createDirectories(dir);
		return Files.writeString(dir.resolve(name), content).toFile();
	}

	@Test
	void isStableAcrossFileOrderAndFolder(@TempDir Path tmp) throws Exception {
		File a1 = write(tmp.resolve("r1"), "TEST_a.json", "A");
		File b1 = write(tmp.resolve("r1"), "TEST_b.json", "B");
		File a2 = write(tmp.resolve("copy"), "TEST_a.json", "A");
		File b2 = write(tmp.resolve("copy"), "TEST_b.json", "B");

		String id = RunId.of(List.of(a1, b1));
		assertThat(id).startsWith("sha256:").hasSize(7 + 64);
		assertThat(RunId.of(List.of(b1, a1))).isEqualTo(id);
		assertThat(RunId.of(List.of(a2, b2))).isEqualTo(id);
	}

	@Test
	void changesWithContentOrFileName(@TempDir Path tmp) throws Exception {
		File a = write(tmp.resolve("r"), "TEST_a.json", "A");
		String id = RunId.of(List.of(a));
		write(tmp.resolve("r"), "TEST_a.json", "A2");
		assertThat(RunId.of(List.of(a))).isNotEqualTo(id);
		File renamed = write(tmp.resolve("s"), "TEST_x.json", "A");
		assertThat(RunId.of(List.of(renamed))).isNotEqualTo(id);
	}

	@Test
	void fileBoundariesMatter(@TempDir Path tmp) throws Exception {
		String one = RunId.of(List.of(write(tmp.resolve("1"), "a", "bc")));
		String two = RunId.of(List.of(write(tmp.resolve("2"), "ab", "c")));
		assertThat(one).isNotEqualTo(two);
	}
}
