/*
 * MIT License - see LICENSE.txt
 */
package unitth.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import unitth.core.UnitTH;
import unitth.core.UnitTHConfig;

/**
 * Two reports generated in parallel with different configurations must not
 * influence each other (OP 8: no shared mutable static state).
 */
class ConcurrencyTest {

	@Test
	void parallelRunsWithDifferentConfigAreIsolated(@TempDir Path tmp) throws Exception {
		String[] formats = { "xml", "json", "both", "xml", "json", "both" };
		ExecutorService pool = Executors.newFixedThreadPool(formats.length);
		try {
			Future<?>[] futures = new Future<?>[formats.length];
			for (int i = 0; i < formats.length; i++) {
				Path out = tmp.resolve("run" + i);
				String format = formats[i];
				futures[i] = pool.submit((Callable<Integer>) () ->
						ReportE2EIT.generate(out, format, false, UnitTHConfig.DEFAULT_MAX_RUNS));
			}
			for (Future<?> f : futures) {
				assertThat(f.get()).isEqualTo(UnitTH.OK);
			}
		} finally {
			pool.shutdown();
		}

		for (int i = 0; i < formats.length; i++) {
			Path out = tmp.resolve("run" + i);
			assertThat(ReportE2EIT.metrics(out)).as("run %d (%s)", i, formats[i])
					.isEqualTo(Files.readString(ReportE2EIT.GOLDEN.resolve(formats[i] + ".txt")));
		}
	}

	@Test
	void jenkinsWithoutFolderArgumentFails() {
		assertThat(new UnitTH(UnitTHConfig.defaults()).run("-j")).isEqualTo(UnitTH.FAILED);
	}

	@Test
	void helpAndVersionSucceed() {
		UnitTH th = new UnitTH(UnitTHConfig.defaults());
		assertThat(th.run("--help")).isEqualTo(UnitTH.OK);
		assertThat(th.run("-v")).isEqualTo(UnitTH.OK);
		assertThat(th.run()).isEqualTo(UnitTH.OK);
	}
}
