/*
 * MIT License - see LICENSE.txt
 */
package unitth.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import unitth.core.UnitTH;
import unitth.core.UnitTHConfig;
import unitth.junit.TestRun;
import unitth.store.JsonRunStore;

/**
 * Run store mode must produce the same report as parsing the raw folders,
 * whether runs are ingested at once, one by one, or the raw folders are gone.
 */
class StoreModeTest {

	private static List<String> sampleFolders() throws IOException {
		try (Stream<Path> s = Files.list(ReportE2EIT.SAMPLES)) {
			return s.filter(Files::isDirectory).map(Path::toString).sorted().toList();
		}
	}

	private static int run(Path out, String format, int maxRuns, List<String> args) {
		return new UnitTH(ReportE2EIT.config(out, format, maxRuns)).run(args.toArray(new String[0]));
	}

	private static List<String> args(String store, boolean ctrf, List<String> folders) {
		List<String> a = new ArrayList<>();
		if (store != null) {
			a.add("--store");
			a.add(store);
		}
		if (ctrf) {
			a.add("--ctrf");
		}
		a.addAll(folders);
		return a;
	}

	/** Page content without the generation time and the echoed input arguments. */
	private static String page(Path p) throws IOException {
		String html = ReportE2EIT.read(p);
		html = html.replaceAll("Report generated: [^<]*", "");
		int args = html.indexOf("<div class=\"hidden\">");
		return args < 0 ? html : html.substring(0, args);
	}

	private static void assertSameReport(Path expected, Path actual) throws IOException {
		List<Path> pages = ReportE2EIT.list(expected, "");
		assertThat(ReportE2EIT.list(actual, "")).extracting(Path::getFileName).containsExactlyElementsOf(
				pages.stream().map(Path::getFileName).toList());
		for (Path p : pages) {
			if (p.toString().endsWith(".html")) {
				assertThat(page(actual.resolve(p.getFileName()))).as(p.getFileName().toString()).isEqualTo(page(p));
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({ "xml,xml,false", "json,json,false", "both,both,false", "ctrf,json,true" })
	void storeReportEqualsPlainReport(String scenario, String format, boolean ctrf, @TempDir Path tmp) throws IOException {
		List<String> folders = sampleFolders();
		Path plain = tmp.resolve("plain");
		assertThat(run(plain, format, 100, args(null, ctrf, folders))).isEqualTo(UnitTH.OK);

		String store = tmp.resolve("store.json").toString();
		Path once = tmp.resolve("once");
		assertThat(run(once, format, 100, args(store, ctrf, folders))).isEqualTo(UnitTH.OK);
		assertSameReport(plain, once);

		Path incremental = tmp.resolve("incremental");
		String store2 = tmp.resolve("store2.json").toString();
		for (String f : folders) {
			// Folders without reports of this format fail while the store is still empty.
			run(incremental, format, 100, args(store2, ctrf, List.of(f)));
		}
		assertThat(run(incremental, format, 100, args(store2, ctrf, List.of()))).isEqualTo(UnitTH.OK);
		assertSameReport(plain, incremental);

		Path regenerated = tmp.resolve("regenerated");
		assertThat(run(regenerated, format, 100, args(store, ctrf, List.of()))).isEqualTo(UnitTH.OK);
		assertSameReport(plain, regenerated);
	}

	@Test
	void secondIngestAddsNothing(@TempDir Path tmp) throws IOException {
		Path store = tmp.resolve("store.json");
		List<String> a = args(store.toString(), true, sampleFolders());
		run(tmp.resolve("out"), "json", 100, a);
		String first = Files.readString(store);
		run(tmp.resolve("out"), "json", 100, a);
		assertThat(Files.readString(store)).isEqualTo(first);
	}

	@Test
	void retentionKeepsNewestRunsAndDoesNotReaddTrimmedOnes(@TempDir Path tmp) throws IOException {
		Path store = tmp.resolve("store.json");
		List<String> folders = sampleFolders();
		for (String f : folders) {
			run(tmp.resolve("out"), "json", 2, args(store.toString(), true, List.of(f)));
		}
		assertThat(JsonRunStore.load(store).size()).isEqualTo(2);
		run(tmp.resolve("out"), "json", 2, args(store.toString(), true, List.of(folders.get(0))));
		JsonRunStore loaded = JsonRunStore.load(store);
		assertThat(loaded.size()).isEqualTo(2);
		assertThat(loaded.toRuns(UnitTHConfig.defaults()))
				.extracting(TestRun::getRunPath).noneMatch(p -> p.contains(Path.of(folders.get(0)).getFileName().toString()));
	}

	@Test
	void storeInsideInputFolderIsNotReadAsReport(@TempDir Path tmp) throws IOException {
		Path in = Files.createDirectories(tmp.resolve("in/2026-01-01T000000Z"));
		Files.writeString(in.resolve("TEST_r.json"),
				"{\"results\": {\"environment\": {\"projectName\": \"o_r_S\"}, \"tests\": [{\"name\": \"a\", \"status\": \"passed\", \"duration\": 1}]}}");
		Path store = in.resolve("TEST_store.json");
		List<String> a = args(store.toString(), true, List.of(in.toString()));
		assertThat(run(tmp.resolve("out"), "json", 100, a)).isEqualTo(UnitTH.OK);
		assertThat(JsonRunStore.load(store).size()).isEqualTo(1);
		// Read as report input, the store would change the folder's content hash and add a second run.
		assertThat(run(tmp.resolve("out"), "json", 100, a)).isEqualTo(UnitTH.OK);
		assertThat(JsonRunStore.load(store).size()).isEqualTo(1);
	}

	@Test
	void ingestOnlyWritesStoreButNoReport(@TempDir Path tmp) throws IOException {
		Path store = tmp.resolve("store.json");
		List<String> a = new ArrayList<>(List.of("--ingest-only"));
		a.addAll(args(store.toString(), true, sampleFolders()));
		Path out = tmp.resolve("out");
		assertThat(run(out, "json", 100, a)).isEqualTo(UnitTH.OK);
		assertThat(store).exists();
		assertThat(out.resolve("index.html")).doesNotExist();
	}

	@Test
	void ingestOnlyNeedsAStore(@TempDir Path tmp) throws IOException {
		List<String> a = new ArrayList<>(List.of("--ingest-only", "--ctrf"));
		a.addAll(sampleFolders());
		assertThat(run(tmp.resolve("out"), "json", 100, a)).isEqualTo(UnitTH.FAILED);
	}

	@Test
	void storeOptionNeedsAFile(@TempDir Path tmp) {
		assertThat(run(tmp.resolve("out"), "json", 100, List.of("--store"))).isEqualTo(UnitTH.FAILED);
	}

	@Test
	void emptyStoreWithoutInputFails(@TempDir Path tmp) {
		assertThat(run(tmp.resolve("out"), "json", 100, args(tmp.resolve("s.json").toString(), true, List.of())))
				.isEqualTo(UnitTH.FAILED);
	}

	@Test
	void unusableStoreFails(@TempDir Path tmp) throws IOException {
		Path bad = Files.writeString(tmp.resolve("bad.json"), "not json");
		List<String> a = args(bad.toString(), true, sampleFolders());
		assertThat(run(tmp.resolve("out"), "json", 100, a)).isEqualTo(UnitTH.FAILED);
	}

	@Test
	void storeDirReportEqualsPlainReport(@TempDir Path tmp) throws IOException {
		List<String> folders = sampleFolders();
		Path plain = tmp.resolve("plain");
		assertThat(run(plain, "json", 100, args(null, true, folders))).isEqualTo(UnitTH.OK);

		// One store per project: split the runs over two stores in one directory.
		Path dir = Files.createDirectories(tmp.resolve("stores"));
		List<String> even = new ArrayList<>();
		List<String> odd = new ArrayList<>();
		for (int i = 0; i < folders.size(); i++) {
			(i % 2 == 0 ? even : odd).add(folders.get(i));
		}
		run(tmp.resolve("x"), "json", 100, args(dir.resolve("a.json").toString(), true, even));
		run(tmp.resolve("x"), "json", 100, args(dir.resolve("b.json").toString(), true, odd));
		Files.writeString(dir.resolve("TEST_foreign.json"),
				"{\"results\": {\"tests\": [{\"name\": \"z\", \"status\": \"passed\", \"duration\": 1}]}}");

		Path union = tmp.resolve("union");
		List<String> a = new ArrayList<>(List.of("--store-dir", dir.toString(), "--ctrf"));
		assertThat(run(union, "json", 100, a)).isEqualTo(UnitTH.OK);
		assertSameReport(plain, union);
	}

	@Test
	void storeDirWithFoldersNeedsAStore(@TempDir Path tmp) throws IOException {
		List<String> a = new ArrayList<>(List.of("--store-dir", tmp.toString(), "--ctrf"));
		a.addAll(sampleFolders());
		assertThat(run(tmp.resolve("out"), "json", 100, a)).isEqualTo(UnitTH.FAILED);
	}

	@Test
	void lockTimeoutSkipsIngestButStillReports(@TempDir Path tmp) throws Exception {
		List<String> folders = sampleFolders();
		Path store = tmp.resolve("store.json");
		run(tmp.resolve("x"), "json", 100, args(store.toString(), true, folders.subList(folders.size() - 1, folders.size())));
		int before = JsonRunStore.load(store).size();
		assertThat(before).isPositive();
		String old = System.setProperty(UnitTH.PROP_LOCK_TIMEOUT, "0");
		try {
			Path out = tmp.resolve("out");
			int rc = JsonRunStore.locked(store, Duration.ofSeconds(5), () -> {
				// Another thread is the competing pipeline run.
				ExecutorService ex = Executors.newSingleThreadExecutor();
				try {
					return ex.submit(() -> run(out, "json", 100, args(store.toString(), true, folders)))
							.get(30, TimeUnit.SECONDS);
				} catch (Exception e) {
					throw new IOException(e);
				} finally {
					ex.shutdownNow();
				}
			});
			assertThat(rc).isEqualTo(UnitTH.OK);
			assertThat(out.resolve("index.html")).exists();
			assertThat(JsonRunStore.load(store).size()).isEqualTo(before);
		} finally {
			if (old == null) {
				System.clearProperty(UnitTH.PROP_LOCK_TIMEOUT);
			} else {
				System.setProperty(UnitTH.PROP_LOCK_TIMEOUT, old);
			}
		}
	}

	@Test
	void storeFileAndMaxRunsComeFromProperties() {
		java.util.Properties p = new java.util.Properties();
		p.setProperty(UnitTHConfig.PROP_STORE_FILE, "h.json");
		p.setProperty(UnitTHConfig.PROP_MAX_RUNS, "7");
		UnitTHConfig c = UnitTHConfig.load(null, null, p);
		assertThat(c.storeFile()).isEqualTo("h.json");
		assertThat(c.maxRuns()).isEqualTo(7);
		p.setProperty(UnitTHConfig.PROP_MAX_RUNS, "seven");
		assertThat(UnitTHConfig.load(null, null, p).maxRuns()).isEqualTo(UnitTHConfig.DEFAULT_MAX_RUNS);
	}
}
