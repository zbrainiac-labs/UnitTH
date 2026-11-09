/*
 * MIT License - see LICENSE.txt
 */
package unitth.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import unitth.core.ReportParser;
import unitth.core.UnitTHConfig;
import unitth.junit.TestCase;
import unitth.junit.TestCaseVerdict;
import unitth.junit.TestModule;
import unitth.junit.TestPackage;
import unitth.junit.TestRun;

/**
 * JSON file holding parsed runs, so reports can be regenerated and new runs
 * ingested without re-parsing (or keeping) the raw report files.
 * <p>
 * Each run stores the parsed module and test case state; packages and all
 * statistics are rebuilt from it exactly as after parsing. Run ids are content
 * hashes ({@link RunId}); every id ever stored is kept in <code>seen</code> so
 * trimmed runs are not ingested again.
 */
public final class JsonRunStore {

	/** First member of every store file; also used to keep it out of the report input. */
	public static final String HEADER = "{\"format\":\"unitth-store\"";
	static final String FORMAT = "unitth-store";
	static final long SCHEMA_VERSION = 1;
	private static final long LOCK_RETRY_MILLIS = 100;
	/* Ingest takes seconds; a lock this old was left by a crashed run. */
	private static final long STALE_LOCK_MILLIS = 10L * 60 * 1000;
	private static final String INTERRUPTED = "Interrupted while waiting for the run store lock";
	/* The lock file is shared across processes; threads of one JVM are serialised here first. */
	private static final Map<Path, ReentrantLock> JVM_LOCKS = new ConcurrentHashMap<>();

	/** Work done while holding the store lock. */
	@FunctionalInterface
	public interface LockedBody<T> {
		T run() throws IOException;
	}

	/** Thrown when the store lock could not be taken in time. */
	public static final class LockTimeoutException extends IOException {
		private static final long serialVersionUID = 1L;

		LockTimeoutException(String message) {
			super(message);
		}
	}

	private final Path file;
	private final Set<String> seen = new LinkedHashSet<>();
	private final List<Map<String, Object>> runs = new ArrayList<>();

	private JsonRunStore(Path file) {
		this.file = file;
	}

	/** Loads the store, or returns an empty one if the file does not exist yet. */
	@SuppressWarnings("unchecked")
	public static JsonRunStore load(Path file) throws IOException {
		JsonRunStore store = new JsonRunStore(file);
		if (!Files.exists(file)) {
			return store;
		}
		Object doc;
		try {
			doc = Json.parse(Files.readString(file, StandardCharsets.UTF_8));
		} catch (IllegalArgumentException e) {
			throw new IOException("Run store '" + file + "' is not valid JSON: " + e.getMessage(), e);
		}
		if (!(doc instanceof Map<?, ?> m) || !FORMAT.equals(m.get("format"))) {
			throw new IOException("'" + file + "' is not a UnitTH run store");
		}
		Object version = m.get("schemaVersion");
		if (!(version instanceof Long v) || v != SCHEMA_VERSION) {
			throw new IOException("Run store '" + file + "' has schema version " + version
					+ ", this UnitTH supports " + SCHEMA_VERSION);
		}
		for (Object id : (List<Object>) m.get("seen")) {
			store.seen.add((String) id);
		}
		for (Object run : (List<Object>) m.get("runs")) {
			store.runs.add((Map<String, Object>) run);
		}
		return store;
	}

	/**
	 * Runs <code>body</code> while holding an exclusive lock: the file
	 * <code>&lt;file&gt;.lock</code>, created atomically and removed
	 * afterwards, so concurrent processes, containers and threads updating the
	 * same store do not lose each other's runs.
	 *
	 * @throws LockTimeoutException
	 *             if the lock was not acquired within <code>timeout</code>.
	 */
	public static <T> T locked(Path file, Duration timeout, LockedBody<T> body) throws IOException {
		Path abs = file.toAbsolutePath().normalize();
		long deadline = System.nanoTime() + timeout.toNanos();
		ReentrantLock jvmLock = JVM_LOCKS.computeIfAbsent(abs, p -> new ReentrantLock());
		try {
			if (!jvmLock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
				throw timeout(abs, timeout);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(INTERRUPTED, e);
		}
		try {
			return withFileLock(abs, deadline, timeout, body);
		} finally {
			jvmLock.unlock();
		}
	}

	private static <T> T withFileLock(Path abs, long deadline, Duration timeout, LockedBody<T> body)
			throws IOException {
		Files.createDirectories(abs.getParent());
		Path lockFile = abs.resolveSibling(abs.getFileName() + ".lock");
		String token = ProcessHandle.current().pid() + "@" + UUID.randomUUID();
		while (!tryCreate(lockFile, token)) {
			if (System.nanoTime() >= deadline) {
				throw timeout(abs, timeout);
			}
			breakIfStale(lockFile);
			sleep();
		}
		try {
			return body.run();
		} finally {
			release(lockFile, token);
		}
	}

	/*
	 * Exclusive creation is atomic on local and shared (NFS, Docker bind mount)
	 * file systems, unlike OS file locks, which other hosts or containers may
	 * not see.
	 */
	private static boolean tryCreate(Path lockFile, String token) throws IOException {
		try {
			Files.writeString(lockFile, token, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
					StandardOpenOption.WRITE);
			return true;
		} catch (FileAlreadyExistsException e) {
			return false;
		}
	}

	/* A lock left by a crashed run is taken over by exactly one waiter (atomic rename). */
	private static void breakIfStale(Path lockFile) {
		try {
			long age = System.currentTimeMillis() - Files.getLastModifiedTime(lockFile).toMillis();
			if (age > STALE_LOCK_MILLIS) {
				Path taken = lockFile.resolveSibling(lockFile.getFileName() + ".stale-" + UUID.randomUUID());
				Files.move(lockFile, taken, StandardCopyOption.ATOMIC_MOVE);
				Files.deleteIfExists(taken);
			}
		} catch (IOException e) {
			// released or taken over by another waiter meanwhile: retry normally
		}
	}

	private static void release(Path lockFile, String token) throws IOException {
		try {
			if (token.equals(Files.readString(lockFile, StandardCharsets.UTF_8))) {
				Files.delete(lockFile);
			}
		} catch (NoSuchFileException e) {
			// taken over as stale: nothing to release
		}
	}

	private static void sleep() throws IOException {
		try {
			Thread.sleep(LOCK_RETRY_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(INTERRUPTED, e);
		}
	}

	private static LockTimeoutException timeout(Path file, Duration timeout) {
		return new LockTimeoutException("Run store '" + file + "' is locked by another run, gave up after "
				+ timeout.toSeconds() + " s");
	}

	/** True if the file starts with the store header (cheap check, no parsing). */
	public static boolean isStoreFile(Path f) {
		byte[] head = new byte[HEADER.length()];
		try (InputStream in = Files.newInputStream(f)) {
			return in.readNBytes(head, 0, head.length) == head.length
					&& HEADER.equals(new String(head, StandardCharsets.UTF_8));
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Loads every run store directly in <code>dir</code> (other files are
	 * ignored), sorted by file name.
	 */
	public static List<JsonRunStore> loadAll(Path dir) throws IOException {
		List<Path> files;
		try (Stream<Path> s = Files.list(dir)) {
			files = s.filter(p -> p.getFileName().toString().endsWith(".json") && Files.isRegularFile(p) && isStoreFile(p))
					.sorted().toList();
		}
		try {
			return files.stream().map(p -> {
				try {
					return load(p);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).toList();
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public Path getFile() {
		return file;
	}

	/** True if the run was ever stored, even if it has been trimmed since. */
	public boolean isKnown(String id) {
		return seen.contains(id);
	}

	public int size() {
		return runs.size();
	}

	/**
	 * Adds a freshly parsed run (before <code>calcStats</code>).
	 *
	 * @param id
	 *            The content hash of the run's report files.
	 * @param sourcePath
	 *            The run folder as given on the command line.
	 */
	public void add(String id, String sourcePath, TestRun run) {
		List<Object> modules = new ArrayList<>();
		String date = "";
		for (TestPackage tp : run.getTestPackages().values()) {
			for (TestModule tm : tp.getTestModules().values()) {
				modules.add(module(tm));
				String d = tm.getExecutionDate();
				if (d != null && (date.isEmpty() || d.compareTo(date) < 0)) {
					date = d;
				}
			}
		}
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("id", id);
		r.put("sourcePath", sourcePath);
		r.put("date", date);
		r.put("modules", modules);
		runs.add(r);
		seen.add(id);
	}

	/**
	 * Keeps the newest <code>keep</code> runs by run date; 0 or less keeps
	 * all.
	 *
	 * @return The number of dropped runs.
	 */
	public int trim(int keep) {
		if (keep <= 0 || runs.size() <= keep) {
			return 0;
		}
		runs.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("date"))
				.thenComparing(r -> (String) r.get("id")));
		int dropped = runs.size() - keep;
		runs.subList(0, dropped).clear();
		return dropped;
	}

	/** Writes the store atomically (temporary file, then rename). */
	public void save() throws IOException {
		Map<String, Object> doc = new LinkedHashMap<>();
		doc.put("format", FORMAT);
		doc.put("schemaVersion", SCHEMA_VERSION);
		doc.put("seen", new ArrayList<>(seen));
		doc.put("runs", runs);
		Path dir = file.toAbsolutePath().getParent();
		Files.createDirectories(dir);
		// A unique sibling created like any other file (umask), not via createTempFile,
		// which is owner-only; the store is shared with other users (CI runners).
		Path tmp = dir.resolve(file.getFileName() + "." + UUID.randomUUID() + ".tmp");
		try {
			Files.writeString(tmp, Json.write(doc), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
					StandardOpenOption.WRITE);
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	/** Rebuilds all stored runs as freshly parsed <code>TestRun</code> objects. */
	@SuppressWarnings("unchecked")
	public List<TestRun> toRuns(UnitTHConfig config) {
		List<TestRun> result = new ArrayList<>();
		for (Map<String, Object> r : runs) {
			Map<String, TestPackage> packages = new HashMap<>();
			for (Object m : (List<Object>) r.get("modules")) {
				TestModule tm = module((Map<String, Object>) m);
				if (config.skipCommentTests()) {
					tm.removeCommentTests();
				}
				ReportParser.addToPackage(packages, tm);
			}
			TestRun run = new TestRun(config);
			run.setRunPath((String) r.get("sourcePath"));
			run.addTestPackages(new HashMap<>(packages));
			result.add(run);
		}
		return result;
	}

	private static Map<String, Object> module(TestModule tm) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", tm.getName());
		m.put("date", tm.getExecutionDate());
		m.put("time", tm.getExecutionTimeDouble());
		m.put("tests", (long) tm.getNoTestCases());
		m.put("failures", (long) tm.getNoFailures());
		m.put("errors", (long) tm.getNoErrors());
		m.put("ignored", (long) tm.getNoIgnored());
		m.put("classIgnored", tm.getIsIgnored());
		List<Object> cases = new ArrayList<>();
		for (TestCase tc : tm.getTestCases().values()) {
			Map<String, Object> c = new LinkedHashMap<>();
			c.put("name", tc.getName());
			c.put("class", tc.getClassName());
			c.put("module", tc.getModuleName());
			c.put("verdict", tc.getVerdict().name());
			c.put("time", tc.getExecutionTimeDouble());
			cases.add(c);
		}
		m.put("cases", cases);
		return m;
	}

	@SuppressWarnings("unchecked")
	private static TestModule module(Map<String, Object> m) {
		TestModule tm = new TestModule();
		tm.setName((String) m.get("name"));
		tm.setFormattedDate((String) m.get("date"));
		tm.setExecutionTime(String.valueOf(m.get("time")));
		tm.setNoTestCases(String.valueOf(m.get("tests")));
		tm.setNoFailures(String.valueOf(m.get("failures")));
		tm.setNoErrors(String.valueOf(m.get("errors")));
		tm.setNoIgnored(String.valueOf(m.get("ignored")));
		if (Boolean.TRUE.equals(m.get("classIgnored"))) {
			tm.setAsIgnored();
		}
		for (Object o : (List<Object>) m.get("cases")) {
			Map<String, Object> c = (Map<String, Object>) o;
			TestCase tc = new TestCase();
			tc.setName((String) c.get("name"));
			tc.setClassName((String) c.get("class"));
			tc.setModuleName((String) c.get("module"));
			tc.setVerdict(TestCaseVerdict.valueOf((String) c.get("verdict")));
			tc.setExecutionTime(String.valueOf(c.get("time")));
			tm.addTestCase(tc);
		}
		return tm;
	}
}
