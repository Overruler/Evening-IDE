package project.generator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import utils.lists.ArrayList;
import utils.lists.Files;
import utils.lists.HashMap;
import utils.lists.Map;
import utils.lists.Pair;
import utils.lists.Paths;
import utils.lists.Set;
import utils.streams2.Stream;
import utils.streams2.Streams;

class ComparisonSummaryGenerator {
	private static final Path USER_HOME_FOLDER = Paths.get(System.getProperty("user.home", ""));
	private static final Path WORKING_FOLDER = Paths.get("").toAbsolutePath();

	public static void main(String[] args) throws IOException {
		Path version1 = chooseNewer(USER_HOME_FOLDER, "eclipse", "evening", 1);
		Path version2 = chooseNewer(WORKING_FOLDER, "target/ide-1", "target/ide-2", 2);
		Map<Path, Pair<byte[], Instant>> official =
			readContents("--- Summary report of different files between two versions, by type of file", version1);
		Map<Path, Pair<byte[], Instant>> current =
			readContents("--- (from " + version1 + " and " + version2 + ")", version2);
		int diffs;
		diffs = printReport("Short Report (ignoring all but major version differences)", official, current, 1);
		if(diffs > 0) return;
		diffs = printReport("Full Report (ignoring only snapshot version differences)", official, current, 3);
		if(diffs > 0) return;
		printFullReport("Exhaustive Report (not ignoring any differences)", official, current);
	}
	private static Path chooseNewer(Path folder, String path1, String path2, int version) throws IOException {
		Path version1 = folder.resolve(path1);
		Path version2 = folder.resolve(path2);
		boolean found1 = Files.isDirectory(version1);
		boolean found2 = Files.isDirectory(version2);
		if(found1 || found2) {
			Instant modified1 = Files.getLastModifiedTime(version1).toInstant();
			Instant modified2 = Files.getLastModifiedTime(version2).toInstant();
			return modified1.isAfter(modified2) ? version1 : version2;
		} else if(found1) {
			return version1;
		} else if(found2) {
			return version2;
		} else {
			System.out.printf(
				"Version %d for comparison doesn't exist.%nLooked in: %s%nAnd in: %s%n",
				version,
				version1,
				version2);
			System.exit(-1);
			throw new AssertionError();
		}
	}
	private static int printFullReport(
		String info,
		Map<Path, Pair<byte[], Instant>> official,
		Map<Path, Pair<byte[], Instant>> current) {
		int diffs = 0;
		System.out.println();
		int i = 0;
		int n = 9;
		info(info, i++, n);
		Map<Path, Pair<byte[], Instant>> officialContents = official;
		info(info, i++, n);
		Map<Path, Pair<byte[], Instant>> currentContents = current;
		info(info, i++, n);
		Set<Pair<String, Path>> officialFiles = groupedByFileExt(officialContents);
		info(info, i++, n);
		Set<Pair<String, Path>> currentFiles = groupedByFileExt(currentContents);
		info(info, i++, n);
		HashMap<String, ArrayList<Path>> missing = missingFiles(officialFiles, currentFiles);
		info(info, i++, n);
		HashMap<String, ArrayList<Path>> extras = missingFiles(currentFiles, officialFiles);
		info(info, i++, n);
		HashMap<String, ArrayList<Path>> different =
			differentFiles(officialContents, currentContents, officialFiles, currentFiles);
		info(info, i++, n);
		ArrayList<String> exts =
			missing.keySet().toHashSet().addAll(extras.keySet()).addAll(different.keySet()).toArrayList().sort();
		info(info, i++, n);
		for(String ext : exts) {
			System.out.printf("File type '%s':%n", ext);
			diffs += report(missing, ext, "\tMissing files:\t-\t-\t-\t-\t-\t-\t-\t-\t-");
			diffs += report(extras, ext, "\tExtra files:\t+\t\t+\t\t+\t\t+\t\t+");
			diffs += report(different, ext, "\tDifferent files:");
		}
		return diffs;
	}
	private static HashMap<String, ArrayList<Path>> differentFiles(
		Map<Path, Pair<byte[], Instant>> officialContents,
		Map<Path, Pair<byte[], Instant>> currentContents,
		Set<Pair<String, Path>> officialFiles,
		Set<Pair<String, Path>> currentFiles) {
		Set<Pair<String, Path>> shared = officialFiles.retainAll(currentFiles);
		Stream<Pair<String, Path>> stream = shared.stream().filter(p -> differ(officialContents, currentContents, p));
		HashMap<String, ArrayList<Path>> different = stream.toMultiMap(Pair::lhs, Pair::rhs);
		return different;
	}
	private static boolean differ(
		Map<Path, Pair<byte[], Instant>> officialContents,
		Map<Path, Pair<byte[], Instant>> currentContents,
		Pair<String, Path> p) {
		return Arrays.equals(officialContents.get(p.rhs).lhs, currentContents.get(p.rhs).lhs) == false;
	}
	private static int printReport(
		String info,
		Map<Path, Pair<byte[], Instant>> official,
		Map<Path, Pair<byte[], Instant>> current,
		int versionIgnoreIndex) throws IOException {
		int diffs = 0;
		System.out.println();
		int i = 0;
		int n = 11;
		info(info, i++, n);
		Map<Path, Pair<byte[], Instant>> officialContents = adjustVersions(official, versionIgnoreIndex);
		info(info, i++, n);
		Map<Path, Pair<byte[], Instant>> currentContents = adjustVersions(current, versionIgnoreIndex);
		info(info, i++, n);
		officialContents =
			officialContents.entrySet().removeIf(e -> e.lhs.toString().contains(".source_")).stream().toMap(
				Pair::lhs,
				Pair::rhs).toMap();
		info(info, i++, n);
		officialContents =
			officialContents.entrySet().removeIf(e -> e.lhs.startsWith("features")).stream().toMap(Pair::lhs, Pair::rhs).toMap();
		info(info, i++, n);
		currentContents =
			currentContents.entrySet().removeIf(e -> e.lhs.startsWith("features")).stream().toMap(Pair::lhs, Pair::rhs).toMap();
		info(info, i++, n);
		Set<Pair<String, Path>> officialFiles = groupedByFileExt(officialContents);
		info(info, i++, n);
		Set<Pair<String, Path>> currentFiles = groupedByFileExt(currentContents);
		info(info, i++, n);
		HashMap<String, ArrayList<Path>> missing = missingFiles(officialFiles, currentFiles);
		info(info, i++, n);
		HashMap<String, ArrayList<Path>> extras = missingFiles(currentFiles, officialFiles);
		info(info, i++, n);
		ArrayList<String> exts = missing.keySet().toHashSet().addAll(extras.keySet()).toArrayList().sort();
		info(info, i++, n);
		for(String ext : exts) {
			System.out.printf("File type '%s':%n", ext);
			diffs += report(missing, ext, "\tMissing files:\t-\t-\t-\t-\t-\t-\t-\t-\t-");
			diffs += report(extras, ext, "\tExtra files:\t+\t\t+\t\t+\t\t+\t\t+");
		}
		return diffs;
	}
	private static int report(HashMap<String, ArrayList<Path>> group, String ext, String info) {
		int diffs = 0;
		if(group.containsKey(ext)) {
			System.out.println(info);
			diffs += reportFiles(group.get(ext).sort());
		}
		return diffs;
	}
	private static int reportFiles(ArrayList<Path> files) {
		int diffs = files.size();
		ArrayList<Path> list = files;
		for(int i = 0, n = Math.min(list.size(), 10); i < n; i++) {
			System.out.printf("\t\t%s%n", list.get(i));
		}
		if(list.size() > 10) {
			System.out.printf("\t\t(and %d more..)%n", list.size() - 10);
		}
		return diffs;
	}
	private static HashMap<String, ArrayList<Path>> missingFiles(
		Set<Pair<String, Path>> officialFiles,
		Set<Pair<String, Path>> currentFiles) {
		HashMap<String, ArrayList<Path>> missing =
			officialFiles.removeAll(currentFiles).stream().toMultiMap(Pair::lhs, Pair::rhs);
		return missing;
	}
	private static Set<Pair<String, Path>> groupedByFileExt(Map<Path, Pair<byte[], Instant>> currentContents) {
		Stream<Pair<Path, Pair<byte[], Instant>>> stream = currentContents.stream();
		Stream<Pair<String, Path>> map = stream.map(e -> Pair.of(fileExt(e), e.lhs));
		return map.toSet().toSet();
	}
	private static String fileExt(Pair<Path, Pair<byte[], Instant>> e) {
		String name = e.lhs.getFileName().toString();
		return name.substring(name.lastIndexOf('.') + 1);
	}
	private static Map<Path, Pair<byte[], Instant>> readContents(String info, Path folder) throws IOException {
		HashMap<Path, Pair<byte[], Instant>> target = new HashMap<>();
		ArrayList<Path> list = Files.walk(folder).toList();
		for(int i = 0, n = list.size(); i < n; i++) {
			info(info, i, n);
			Path entry = list.get(i);
			if(Files.isRegularFile(entry)) {
				byte[] bytes = Files.readAllBytes(entry);
				Instant instant = Files.getLastModifiedTime(entry).toInstant();
				Path path = folder.relativize(entry);
				readFile(target, bytes, path, instant);
			}
		}
		return target.toMap();
	}
	private static Map<Path, Pair<byte[], Instant>> adjustVersions(
		Map<Path, Pair<byte[], Instant>> target,
		int versionIgnoreIndex) throws IOException {
		ArrayList<Pair<Path, Pair<byte[], Instant>>> list =
			target.stream().filter(e -> e.lhs.endsWith(JarFile.MANIFEST_NAME)).toList();
		HashMap<Path, Path> adjustments = new HashMap<>();
		for(Pair<Path, Pair<byte[], Instant>> entry : list) {
			Path path = entry.lhs.subpath(0, entry.lhs.getNameCount() - 2);
			String name = path.getFileName().toString();
			Attributes attributes = parseManifest(entry.rhs.lhs);
			String bundleID =
				generateBundleIDFromManifest(name.endsWith(".jar") ? ".jar" : "", attributes, versionIgnoreIndex);
			if(bundleID != null) {
				Path changed = path.getParent().resolve(bundleID);
				adjustments.put(path, changed);
			}
		}
		Map<Path, Path> adjusted = adjustments.toMap();
		return target.entrySet().replaceAll(e -> adjust(adjusted, e)).stream().toMap(Pair::lhs, Pair::rhs).toMap();
	}
	private static <V> Pair<Path, V> adjust(Map<Path, Path> adjustments, Pair<Path, V> e) {
		for(int i = e.lhs.getNameCount() - 1; i > 0; i--) {
			Path original = e.lhs.subpath(0, i);
			Path target = adjustments.get(original);
			if(target != null) {
				Path adjustedPath = target.resolve(original.relativize(e.lhs));
				e = e.keepingRhs(adjustedPath);
			}
		}
		return e;
	}
	private static String generateBundleIDFromManifest(String suffix, Attributes attributes, int versionIgnoreIndex) {
		String version = attributes.getValue("Bundle-Version");
		if(version == null) {
			return null;
		}
		String[] split = version.split("\\.");
		for(int i = versionIgnoreIndex, n = split.length; i < n; i++) {
			split[i] = "";
		}
		version = String.join(" ", split).trim().replace(' ', '.');
		String symbolicName = attributes.getValue("Bundle-SymbolicName");
		if(symbolicName == null) {
			return null;
		}
		String name = symbolicName.split(";")[0].trim();
		return name + "_" + version + suffix;
	}
	private static Attributes parseManifest(byte[] buf) throws IOException {
		return new Manifest(new ByteArrayInputStream(buf)).getMainAttributes();
	}
	private static void
		readFile(HashMap<Path, Pair<byte[], Instant>> target, byte[] bytes, Path path, Instant modified)
			throws IOException {
		String name = path.getFileName().toString();
		if(name.endsWith(".jar") || name.endsWith(".zip")) {
			readZip(bytes, target, path);
			return;
		}
		if(name.endsWith(".gz")) {
			path = path.resolve(name.substring(0, name.length() - 3));
			bytes = Streams.readFully(new GZIPInputStream(new ByteArrayInputStream(bytes)));
		}
		Pair<byte[], Instant> value = new Pair<>(bytes, modified);
		target.put(path, value);
	}
	private static void readZip(byte[] bytes, HashMap<Path, Pair<byte[], Instant>> target, Path root)
		throws IOException {
		try(ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));) {
			ZipEntry entry;
			while((entry = zip.getNextEntry()) != null) {
				if(entry.isDirectory()) {
					continue;
				}
				String name = entry.getName();
				if(name.contains("..")) {
					continue;
				}
				bytes = Streams.readAllBytes(zip);
				Path path = root.resolve(name);
				Instant instant = entry.getLastModifiedTime().toInstant();
				readFile(target, bytes, path, instant);
			}
		}
	}
	private static void info(String info, int i, int n) {
		int beginIndex = info.length() * i / n;
		int endIndex = info.length() * ++i / n;
		if(beginIndex != endIndex) {
			System.out.print(info.substring(beginIndex, endIndex));
		}
		if(i == n) {
			System.out.println();
		}
	}
}
