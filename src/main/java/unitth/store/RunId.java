/*
 * MIT License - see LICENSE.txt
 */
package unitth.store;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Identity of a run: SHA-256 over its report files, sorted by name, hashing
 * name, a NUL byte and the content of each file. Independent of folder name,
 * location and dates, so the same reports in two folders are one run and a
 * reused folder with new reports is a new run.
 */
public final class RunId {

	private RunId() {
	}

	public static String of(List<File> files) throws IOException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
		List<File> sorted = new ArrayList<>(files);
		sorted.sort(Comparator.comparing(File::getName));
		byte[] buf = new byte[8192];
		for (File f : sorted) {
			md.update(f.getName().getBytes(StandardCharsets.UTF_8));
			md.update((byte) 0);
			try (InputStream in = Files.newInputStream(f.toPath())) {
				for (int n; (n = in.read(buf)) > 0;) {
					md.update(buf, 0, n);
				}
			}
		}
		return "sha256:" + HexFormat.of().formatHex(md.digest());
	}
}
