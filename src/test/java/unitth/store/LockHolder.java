/*
 * MIT License - see LICENSE.txt
 */
package unitth.store;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Test helper: holds a store lock in a separate JVM until its stdin is
 * closed. Args: store file.
 */
public final class LockHolder {

	private LockHolder() {
	}

	public static void main(String[] args) throws IOException {
		JsonRunStore.locked(Path.of(args[0]), Duration.ofSeconds(10), () -> {
			System.out.println("locked"); // NOSONAR test helper protocol
			System.out.flush();
			System.in.transferTo(java.io.OutputStream.nullOutputStream());
			return null;
		});
	}
}
