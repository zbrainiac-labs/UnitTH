/*
 * MIT License - see LICENSE.txt
 */
package unitth.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import unitth.core.ReportParser;
import unitth.core.UnitTHConfig;
import unitth.junit.TestCase;
import unitth.junit.TestCaseVerdict;
import unitth.junit.TestModule;
import unitth.junit.TestRun;

class JsonRunStoreTest {

	private static final UnitTHConfig CONFIG = UnitTHConfig.defaults();

	private static TestRun run(String date, String... caseNames) {
		TestModule tm = new TestModule();
		tm.setName("org_repo.Suite");
		tm.setDate(date);
		tm.setExecutionTime("1.5");
		tm.setNoTestCases(String.valueOf(caseNames.length));
		tm.setNoFailures("1");
		tm.setNoErrors("0");
		tm.setNoIgnored("0");
		for (String n : caseNames) {
			TestCase tc = new TestCase();
			tc.setName(n);
			tc.setClassName("org_repo.Suite");
			tc.setModuleName("org_repo.Suite");
			tc.setVerdict(n.startsWith("f") ? TestCaseVerdict.e_FAIL : TestCaseVerdict.e_PASS);
			tc.setExecutionTime("0.25");
			tm.addTestCase(tc);
		}
		TestRun run = new TestRun(CONFIG);
		java.util.HashMap<String, unitth.junit.TestPackage> packages = new java.util.HashMap<>();
		ReportParser.addToPackage(packages, tm);
		run.addTestPackages(packages);
		return run;
	}

	@Test
	void lockTimesOutWhileAnotherThreadHoldsIt(@TempDir Path tmp) throws Exception {
		Path file = tmp.resolve("s.json");
		CountDownLatch held = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ExecutorService ex = Executors.newSingleThreadExecutor();
		try {
			Future<Object> holder = ex.submit(() -> JsonRunStore.locked(file, Duration.ofSeconds(5), () -> {
				held.countDown();
				await(release);
				return null;
			}));
			assertThat(held.await(5, TimeUnit.SECONDS)).isTrue();
			assertThatThrownBy(() -> JsonRunStore.locked(file, Duration.ofMillis(200), () -> "x"))
					.isInstanceOf(JsonRunStore.LockTimeoutException.class).hasMessageContaining("locked");
			release.countDown();
			holder.get(5, TimeUnit.SECONDS);
			assertThat(JsonRunStore.locked(file, Duration.ofMillis(200), () -> "x")).isEqualTo("x");
		} finally {
			ex.shutdownNow();
		}
	}

	private static void await(CountDownLatch latch) throws IOException {
		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	@Test
	void concurrentLockedUpdatesKeepAllRuns(@TempDir Path tmp) throws Exception {
		Path file = tmp.resolve("s.json");
		int n = 8;
		ExecutorService ex = Executors.newFixedThreadPool(n);
		try {
			List<Future<Object>> all = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				String id = "sha256:" + i;
				String date = "2026-01-0" + (i + 1) + " 00:00:00";
				all.add(ex.submit(() -> JsonRunStore.locked(file, Duration.ofSeconds(30), () -> {
					JsonRunStore s = JsonRunStore.load(file);
					s.add(id, "r" + id, run(date, "t"));
					s.save();
					return null;
				})));
			}
			for (Future<Object> f : all) {
				f.get(30, TimeUnit.SECONDS);
			}
		} finally {
			ex.shutdownNow();
		}
		assertThat(JsonRunStore.load(file).size()).isEqualTo(n);
	}

	@Test
	void lockFileIsRemovedAfterUse(@TempDir Path tmp) throws IOException {
		Path file = tmp.resolve("s.json");
		assertThat(JsonRunStore.locked(file, Duration.ofSeconds(1), () -> Files.exists(tmp.resolve("s.json.lock"))))
				.isTrue();
		assertThat(tmp.resolve("s.json.lock")).doesNotExist();
	}

	@Test
	void freshForeignLockBlocksAndStaleLockIsTakenOver(@TempDir Path tmp) throws IOException {
		Path file = tmp.resolve("s.json");
		Path lock = Files.writeString(tmp.resolve("s.json.lock"), "other-host");
		assertThatThrownBy(() -> JsonRunStore.locked(file, Duration.ofMillis(300), () -> "x"))
				.isInstanceOf(JsonRunStore.LockTimeoutException.class);
		assertThat(lock).exists();
		Files.setLastModifiedTime(lock, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - 11 * 60_000L));
		assertThat(JsonRunStore.locked(file, Duration.ofSeconds(2), () -> "x")).isEqualTo("x");
		assertThat(lock).doesNotExist();
		try (Stream<Path> left = Files.list(tmp)) {
			assertThat(left).noneMatch(p -> p.getFileName().toString().contains(".lock"));
		}
	}

	@Test
	void lockIsExclusiveAcrossProcesses(@TempDir Path tmp) throws Exception {
		Path file = tmp.resolve("s.json");
		String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
		Process child = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
				LockHolder.class.getName(), file.toString()).redirectErrorStream(true).start();
		try (BufferedReader out = new BufferedReader(
				new InputStreamReader(child.getInputStream(), StandardCharsets.UTF_8))) {
			assertThat(out.readLine()).isEqualTo("locked");
			assertThatThrownBy(() -> JsonRunStore.locked(file, Duration.ofMillis(300), () -> "x"))
					.isInstanceOf(JsonRunStore.LockTimeoutException.class);
			child.getOutputStream().close(); // lets the child release the lock
			assertThat(JsonRunStore.locked(file, Duration.ofSeconds(10), () -> "x")).isEqualTo("x");
		} finally {
			assertThat(child.waitFor(10, TimeUnit.SECONDS)).isTrue();
		}
	}

	@Test
	void loadAllReadsOnlyStoreFiles(@TempDir Path tmp) throws IOException {
		JsonRunStore a = JsonRunStore.load(tmp.resolve("b.json"));
		a.add("sha256:1", "r1", run("2026-01-01 00:00:00", "t"));
		a.save();
		JsonRunStore b = JsonRunStore.load(tmp.resolve("a.json"));
		b.add("sha256:2", "r2", run("2026-01-02 00:00:00", "t"));
		b.save();
		Files.writeString(tmp.resolve("TEST_x.json"), "{\"results\": {}}");
		Files.writeString(tmp.resolve("other.txt"), "x");
		Files.createDirectories(tmp.resolve("sub.json"));
		assertThat(JsonRunStore.loadAll(tmp)).extracting(s -> s.getFile().getFileName().toString())
				.containsExactly("a.json", "b.json");
	}

	@Test
	void savedStoreIsReadableByOtherUsers(@TempDir Path tmp) throws IOException {
		JsonRunStore s = JsonRunStore.load(tmp.resolve("s.json"));
		s.add("sha256:1", "r1", run("2026-01-01 00:00:00", "t"));
		s.save();
		org.junit.jupiter.api.Assumptions.assumeTrue(tmp.getFileSystem().supportedFileAttributeViews().contains("posix"));
		// Created like any file (umask 022 here), not owner-only like temp files.
		assertThat(Files.getPosixFilePermissions(s.getFile()))
				.contains(java.nio.file.attribute.PosixFilePermission.OTHERS_READ);
	}

	@Test
	void missingFileIsEmptyStore(@TempDir Path tmp) throws IOException {
		JsonRunStore store = JsonRunStore.load(tmp.resolve("none.json"));
		assertThat(store.size()).isZero();
		assertThat(store.isKnown("x")).isFalse();
	}

	@Test
	void roundTripsRuns(@TempDir Path tmp) throws IOException {
		Path file = tmp.resolve("sub/store.json");
		JsonRunStore store = JsonRunStore.load(file);
		store.add("id1", "runs/r1", run("2026-01-02 03:04:05", "ok", "fail"));
		store.save();

		assertThat(Files.readString(file)).startsWith(JsonRunStore.HEADER);
		JsonRunStore loaded = JsonRunStore.load(file);
		assertThat(loaded.isKnown("id1")).isTrue();
		List<TestRun> runs = loaded.toRuns(CONFIG);
		assertThat(runs).hasSize(1);
		TestRun r = runs.get(0);
		r.calcStats();
		assertThat(r.getExecutionDate()).isEqualTo("2026-01-02&nbsp;03:04:05");
		assertThat(r.getNoTestCases()).isEqualTo(2);
		assertThat(r.getNoFailures()).isEqualTo(1);
		TestModule tm = r.getTestModule("org_repo", "org_repo.Suite");
		assertThat(tm.getTestCaseByName("fail").getVerdict()).isEqualTo(TestCaseVerdict.e_FAIL);
		assertThat(tm.getTestCaseByName("ok").getExecutionTimeDouble()).isEqualTo(0.25);
		assertThat(tm.getExecutionTimeDouble()).isEqualTo(1.5);
	}

	@Test
	void commentLineCasesAreDroppedOnLoadUnlessDisabled(@TempDir Path tmp) throws IOException {
		Path file = tmp.resolve("store.json");
		JsonRunStore store = JsonRunStore.load(file);
		store.add("id", "r", run("2026-01-02 03:04:05", "ok", "fail", "# section", " "));
		store.save();

		TestModule tm = JsonRunStore.load(file).toRuns(CONFIG).get(0).getTestModule("org_repo", "org_repo.Suite");
		assertThat(tm.getTestCases()).containsOnlyKeys("ok", "fail");
		assertThat(tm.getNoTestCases()).isEqualTo(2);
		assertThat(tm.getNoFailures()).isEqualTo(1);

		java.util.Properties sys = new java.util.Properties();
		sys.setProperty(UnitTHConfig.PROP_SKIP_COMMENTS, "false");
		TestModule raw = JsonRunStore.load(file).toRuns(UnitTHConfig.load(null, null, sys)).get(0)
				.getTestModule("org_repo", "org_repo.Suite");
		assertThat(raw.getNoTestCases()).isEqualTo(4);
	}

	@Test
	void restoresClassIgnoredModules(@TempDir Path tmp) throws IOException {
		Path file = tmp.resolve("store.json");
		TestRun run = run("2026-01-02 03:04:05", "Suite");
		run.getTestModules().values().forEach(TestModule::setAsIgnored);
		JsonRunStore store = JsonRunStore.load(file);
		store.add("id", "r", run);
		store.save();
		TestModule tm = JsonRunStore.load(file).toRuns(CONFIG).get(0).getTestModule("org_repo", "org_repo.Suite");
		assertThat(tm.getIsIgnored()).isTrue();
	}

	@Test
	void trimKeepsNewestAndRemembersDroppedIds(@TempDir Path tmp) throws IOException {
		Path file = tmp.resolve("store.json");
		JsonRunStore store = JsonRunStore.load(file);
		for (int i = 1; i <= 101; i++) {
			store.add("id" + i, "r" + i, run(String.format("2026-01-01 00:%02d:%02d", i / 60, i % 60), "ok"));
		}
		assertThat(store.trim(100)).isEqualTo(1);
		assertThat(store.size()).isEqualTo(100);
		assertThat(store.trim(0)).isZero();
		store.save();

		JsonRunStore loaded = JsonRunStore.load(file);
		assertThat(loaded.size()).isEqualTo(100);
		assertThat(loaded.isKnown("id1")).as("dropped run stays known").isTrue();
		assertThat(loaded.toRuns(CONFIG)).extracting(TestRun::getRunPath).doesNotContain("r1");
	}

	@Test
	void saveLeavesNoTemporaryFiles(@TempDir Path tmp) throws IOException {
		Path file = tmp.resolve("store.json");
		JsonRunStore store = JsonRunStore.load(file);
		store.add("id", "r", run("2026-01-02 03:04:05", "ok"));
		store.save();
		store.save();
		try (Stream<Path> s = Files.list(tmp)) {
			assertThat(s.map(p -> p.getFileName().toString())).containsExactly("store.json");
		}
	}

	@Test
	void rejectsForeignOrNewerFiles(@TempDir Path tmp) throws IOException {
		Path ctrf = Files.writeString(tmp.resolve("ctrf.json"), "{\"results\":{}}");
		assertThatThrownBy(() -> JsonRunStore.load(ctrf)).isInstanceOf(IOException.class).hasMessageContaining("not a UnitTH run store");

		Path newer = Files.writeString(tmp.resolve("newer.json"),
				"{\"format\":\"unitth-store\",\"schemaVersion\":2,\"seen\":[],\"runs\":[]}");
		assertThatThrownBy(() -> JsonRunStore.load(newer)).isInstanceOf(IOException.class).hasMessageContaining("schema version 2");

		Path broken = Files.writeString(tmp.resolve("broken.json"), "{\"format\":");
		assertThatThrownBy(() -> JsonRunStore.load(broken)).isInstanceOf(IOException.class).hasMessageContaining("not valid JSON");
	}
}
