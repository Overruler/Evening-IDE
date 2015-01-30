package project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.stream.XMLStreamException;
import utils.lists.ArrayList;
import utils.lists.Arrays;
import utils.lists.Files;
import utils.lists.HashMap;
import utils.lists.HashSet;
import utils.lists.List;
import utils.lists.Map;
import utils.lists.Paths;
import utils.lists.Set;
import utils.streams.functions.ExDoubleConsumer;
import utils.streams.functions.IOFunction;
import utils.streams2.IntStream;
import utils.streams2.Stream;
import utils.streams2.Streams;

public class Snapshot {
	public static class Content {
		byte[] bytes;
		Instant modified;
		HashSet<Path> contained = new HashSet<>();

		Content(Instant modified) {
			this.modified = modified;
		}
		@Override
		public String toString() {
			return "[" +
			(bytes != null ? "bytes=" + bytes.length + ", " : "") +
			(modified != null ? "modified=" + modified + ", " : "") +
			(contained != null ? "contained=" + contained.size() : "") +
			"]";
		}
	}

	private static final Path ROOT = Paths.get("a");
	private HashMap<Path, Content> contents = new HashMap<>();
	private Instant now;
	private static final Comparator<Path> LONGEST_FIRST = Comparator.comparingInt(p -> -p.getNameCount());
	public final IOFunction<Path, Path> BUNDLE_REMAPPER = this::remapper;
	private List<IOFunction<Path, Path>> remappers = List.of(BUNDLE_REMAPPER);
	private int progress;
	private int progressTotal;

	public static void main(String[] args) throws IOException {
		Path ide = Paths.get("C:\\Evening-IDE\\projects\\IDE\\target\\ide.zip");
		Instant instant = Instant.now();
		Snapshot snapshot = new Snapshot(instant);
		snapshot.read(ide);
		Path touch = Paths.get("plugins", "touch");
		snapshot.addFile(touch, new byte[0], instant);
		snapshot.removeFile(touch);
		Path ide2 = ide.resolveSibling("ide2.zip");
		snapshot.write(ide2);
		Path ide3 = ide.resolveSibling("ide3");
		snapshot.write(ide3);
	}
	public Snapshot(Instant now) {
		this.now = now;
	}
	public void clearPathRemappers() {
		remappers = List.of();
	}
	public Snapshot addPathRemapper(IOFunction<Path, Path> mapper) {
		remappers = remappers.add(mapper);
		return this;
	}
	public Snapshot addPathRemapper(String path1, String path2) {
		Path src = Paths.get(path1);
		Path dst = Paths.get(path2);
		addPathRemapper(p -> p.equals(src) ? dst : p);
		return this;
	}
	public void addFile(Path path, byte[] bytes, Instant modified) throws IOException {
		addFileImplementation(path, bytes, modified);
	}
	public void addFile(Path path, String data, Instant modified) throws IOException {
		addFileImplementation(path, data.getBytes(StandardCharsets.UTF_8), modified);
	}
	public void replaceFile(Path path, byte[] bytes) throws IOException {
		replaceFileImplementation(path, bytes);
	}
	public void replaceFile(Path path, String data) throws IOException {
		replaceFileImplementation(path, data.getBytes(StandardCharsets.UTF_8));
	}
	public void copyFiles(String srcFolder, String dstFolder) throws IOException {
		copyFilesImplementation(srcFolder, dstFolder);
	}
	public void addFolder(Path path, Instant modified) throws IOException {
		addFileImplementation(path, null, modified);
	}
	public void removeFile(Path path) {
		removeFileImplementation(path);
	}
	public List<Path> listFiles(Path path) {
		return listFilesOrFoldersImplementation(path, true);
	}
	public List<Path> listFolders(Path path) {
		return listFilesOrFoldersImplementation(path, false);
	}
	public List<Path> listAll(Path path) {
		Content content = contents.get(ROOT.resolve(path));
		if(content == null) {
			return List.of();
		}
		return content.contained.toArrayList().sort().replaceAll(ROOT::relativize).toList();
	}
	public String getFileAsString(Path path) {
		byte[] bytes = getFileImplementation(path);
		if(bytes == null) {
			return null;
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}
	public Feature getFileAsFeature(Path path) throws IOException {
		byte[] bytesFeatureXML = getFileImplementation(path.resolve("feature.xml"));
		if(bytesFeatureXML == null) {
			return null;
		}
		byte[] bytesFeatureProperties = getFileImplementation(path.resolve("feature.properties"));
		try {
			return Feature.fromXML(path, bytesFeatureXML, bytesFeatureProperties);
		} catch(XMLStreamException e) {
			throw new IOException(e);
		}
	}
	public List<Path> listFilesRecursive(Path path) {
		ArrayList<Path> result = new ArrayList<>();
		for(Path folder : listFolders(path)) {
			result.addAll(listFilesRecursive(folder));
		}
		for(Path folder : listFiles(path)) {
			result.addAll(listFilesRecursive(folder));
			result.add(folder);
		}
		return result.toList();
	}
	public Plugin getFileAsPlugin(Path path) throws IOException {
		byte[] bytesManifestMf = getFileImplementation(path.resolve(JarFile.MANIFEST_NAME));
		if(bytesManifestMf == null) {
			return null;
		}
		return new Plugin(path, bytesManifestMf);
	}
	public Map<String, String> getFileAsProperties(Path path) throws IOException {
		byte[] bytesProperties = getFileImplementation(path);
		if(bytesProperties == null) {
			return Map.of();
		}
		Properties properties = new Properties();
		properties.load(new ByteArrayInputStream(bytesProperties));
		Set<String> names = Set.from(properties.stringPropertyNames());
		return names.stream().toMap(s -> s, s -> properties.getProperty(s)).toMap();
	}
	public boolean notFound(Path path) {
		return containsImplementation(path) == false;
	}
	public boolean isFound(Path path) {
		return containsImplementation(path);
	}
	public void write(Path path) throws IOException {
		write(path, d -> {});
	}
	public <E extends Exception> void write(Path path, ExDoubleConsumer<E> monitor) throws IOException, E {
		boolean isZIP = path.getFileName().toString().endsWith(".zip");
		int level = isZIP ? 0 : 9;
		Content content = contents.get(ROOT);
		writeSnapshot(path, content, level, monitor);
		monitor.accept(1);
	}
	public void read(Path start) throws IOException {
		if(Files.isDirectory(start)) {
			ArrayList<Path> list = Files.walk(start).toList();
			for(int i = 0, n = list.size(); i < n; i++) {
				Path entry = list.get(i);
				if(Files.isRegularFile(entry)) {
					byte[] bytes = Files.readAllBytes(entry);
					Instant instant = Files.getLastModifiedTime(entry).toInstant();
					String name = entry.getFileName().toString();
					Path path = ROOT.resolve(relative(start, entry));
					readFile(bytes, path, name, instant);
				}
			}
		} else if(Files.isRegularFile(start)) {
			byte[] bytes = Files.readAllBytes(start);
			Instant instant = Files.getLastModifiedTime(start).toInstant();
			String name = start.getFileName().toString();
			readFile(bytes, ROOT, name, instant);
			contents.get(ROOT).bytes = null;
		}
	}
	public void clear() {
		contents.clear();
	}
	private Path remapper(Path src) throws IOException {
		if(src.getNameCount() == 2) {
			if(src.startsWith(Paths.get("plugins"))) {
				Content content = contents.get(ROOT.resolve(src).resolve(JarFile.MANIFEST_NAME));
				if(content != null) {
					Plugin plugin = new Plugin(src, content.bytes);
					return src.resolveSibling(plugin.fileName());
				}
			} else if(src.startsWith(Paths.get("features"))) {
				Content content = contents.get(ROOT.resolve(src).resolve("feature.xml"));
				if(content != null) {
					Content properties = contents.get(ROOT.resolve(src).resolve("feature.properties"));
					try {
						Feature feature =
							Feature.fromXML(src, content.bytes, properties != null ? properties.bytes : null);
						return src.resolveSibling(feature.fileName());
					} catch(XMLStreamException e) {
						throw new IOException(e);
					}
				}
			}
		}
		return src;
	}
	private void copyFilesImplementation(String srcFolder, String dstFolder) throws IOException {
		Path dstPath = Paths.get(dstFolder);
		removeFileImplementation(dstPath);
		Path srcPath = Paths.get(srcFolder);
		List<Path> files = listFilesOrFoldersImplementation(srcPath, true);
		for(Path filePath : files) {
			Path targetPath = dstPath.resolve(srcPath.relativize(filePath));
			byte[] targetBytes = getFileImplementation(filePath);
			Instant targetInstant = getInstantImplementation(filePath);
			addFileImplementation(targetPath, targetBytes, targetInstant);
		}
	}
	private void addFileImplementation(Path path, byte[] bytes, Instant when) throws IOException {
		Path root = ROOT.resolve(path);
		String name = path.getFileName().toString();
		readFile(bytes, root, name, when);
	}
	private void replaceFileImplementation(Path path, byte[] bytes) throws IOException {
		Path root = ROOT.resolve(path);
		String name = path.getFileName().toString();
		Instant modified = contents.containsKey(root) ? contents.get(root).modified : now;
		readFile(bytes, root, name, modified);
	}
	private byte[] getFileImplementation(Path path) {
		Path root = ROOT.resolve(path);
		Content content = contents.get(root);
		if(content == null) {
			return null;
		}
		return content.bytes;
	}
	private Instant getInstantImplementation(Path path) {
		Path root = ROOT.resolve(path);
		Content content = contents.get(root);
		if(content == null) {
			return null;
		}
		return content.modified;
	}
	private boolean containsImplementation(Path path) {
		Path root = ROOT.resolve(path);
		return contents.containsKey(root);
	}
	private void readFile(byte[] bytes, Path root, String name, Instant modified) throws IOException {
		internalAddFile(root, bytes, modified);
		if(name.endsWith(".jar") || name.endsWith(".zip")) {
			readZip(bytes, root);
		} else if(name.endsWith(".gz")) {
			root = root.resolve(name.substring(0, name.length() - 3));
			bytes = Streams.readFully(new GZIPInputStream(new ByteArrayInputStream(bytes)));
			internalAddFile(root, bytes, modified);
		}
	}
	private void readZip(byte[] bytes, Path root) throws IOException {
		try(ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));) {
			ZipEntry entry;
			while((entry = zip.getNextEntry()) != null) {
				String name = entry.getName();
				if(name.contains("..")) {
					continue;
				}
				if(!IntStream.from(name).allMatch(
					i -> i >= 'a' &&
					i <= 'z' ||
					i >= 'A' &&
					i <= 'Z' ||
					i >= '0' &&
					i <= '9' ||
					i == '.' ||
					i == '/' ||
					i == '$' ||
					i == ' ' ||
					i == '_' ||
					i == '(' ||
					i == ')' ||
					i == '-')) {
					System.out.println("Skipped " + Arrays.toString(name.toCharArray()));
					continue;
				}
				Path path = root.resolve(name);
				Instant instant = entry.getLastModifiedTime().toInstant();
				if(entry.isDirectory()) {
					internalAddFile(path, null, instant);
				} else {
					bytes = Streams.readAllBytes(zip);
					readFile(bytes, path, name, instant);
				}
			}
		}
	}
	private void internalAddFile(Path path, byte[] bytes, Instant modified) {
		Content content = contents.get(path);
		if(content == null) {
			content = new Content(modified);
			contents.put(path, content);
		}
		if(content.modified.isAfter(modified) == false) {
			content.bytes = bytes;
			content.modified = modified;
			Path parent;
			while((parent = path.getParent()) != null) {
				content = contents.get(parent);
				if(content == null) {
					content = new Content(modified);
					contents.put(parent, content);
				}
				content.contained.add(path);
				if(content.modified.isAfter(modified) || content.bytes != null) {
					break;
				}
				content.modified = modified;
				path = parent;
			}
		}
	}
	private void removeFileImplementation(Path path) {
		Path root = ROOT.resolve(path);
		Content folder = contents.get(root);
		if(folder != null && folder.contained.notEmpty()) {
			for(Path contentPath : folder.contained.toList()) {
				removeRecursive(contentPath);
			}
		}
		internalRemoveFile(root, now);
	}
	private void removeRecursive(Path root) {
		Content folder = contents.get(root);
		if(folder != null && folder.contained.notEmpty()) {
			for(Path contentPath : folder.contained.toList()) {
				removeRecursive(contentPath);
			}
		}
		internalRemoveFile(root, now);
	}
	private void internalRemoveFile(Path path, Instant modified) {
		Content content = contents.get(path);
		if(content == null) {
			return;
		}
		if(content.contained.notEmpty()) {
			throw new IllegalArgumentException("Path " + path + " contains " + content.contained.size() + " items");
		}
		boolean remove = true;
		Path parent;
		while((parent = path.getParent()) != null) {
			content = contents.get(parent);
			if(content == null) {
				throw new IllegalStateException("Parent " + parent + " of " + path + " not found");
			}
			if(remove) {
				content.contained.remove(path);
				remove = content.contained.isEmpty();
			}
			if(content.modified.isAfter(modified) || content.bytes != null) {
				break;
			}
			content.modified = modified;
			path = parent;
		}
	}
	private List<Path> listFilesOrFoldersImplementation(Path path, boolean files) {
		Content content = contents.get(ROOT.resolve(path));
		if(content == null) {
			return List.of();
		}
		return internalListFilesOrFolders(content, files).replaceAll(ROOT::relativize);
	}
	private List<Path> internalListFilesOrFolders(Content content, boolean files) {
		ArrayList<Path> list = content.contained.toArrayList();
		list.filter(p -> contents.get(p).bytes != null == files);
		list.sort();
		return list.toList();
	}
	private <E extends Exception> void writeSnapshot(
		Path target,
		Content content,
		int level,
		ExDoubleConsumer<E> monitor) throws IOException, E {
		progress = 1;
		progressTotal = contents.size() + 1;
		String fileName = target.getFileName().toString();
		if(isZIP(content, fileName)) {
			Files.createDirectories(target.getParent());
			writeZIP(target, content, level, monitor);
		} else if(isGZIP(content, fileName)) {
			Files.createDirectories(target.getParent());
			writeGZIP(target, content, monitor);
		} else {
			writeFolder(target, ROOT, content, level, monitor);
		}
	}
	private <E extends Exception> void writeFolder(
		Path target,
		Path root,
		Content folder,
		int level,
		ExDoubleConsumer<E> monitor) throws IOException, E {
		progress(monitor);
		Files.createDirectories(target);
		HashSet<Path> existing = Files.list(target).toSet();
		for(Path path : folder.contained) {
			String fileName = remap(path, level).getFileName().toString();
			Path file = target.resolve(fileName);
			Content content = contents.get(path);
			boolean update = true;
			boolean check = true;
			if(Files.exists(file)) {
				Instant instant = Files.getLastModifiedTime(file).toInstant();
				update = instant.isBefore(content.modified);
				check = instant.isAfter(content.modified) == false;
			}
			if(update || check) {
				if(isZIP(content, fileName)) {
					if(update) {
						byte[] bytes = createZIP(path, content, level, monitor);
						writeFile(file, bytes, content.modified);
					}
				} else if(isGZIP(content, fileName)) {
					if(update) {
						byte[] bytes = createGZIP(content);
						writeFile(file, bytes, content.modified);
					}
					progressFolder(path, monitor);
				} else if(content.bytes != null) {
					if(update) {
						byte[] bytes = content.bytes;
						writeFile(file, bytes, content.modified);
					}
					progressFolder(path, monitor);
				} else {
					writeFolder(file, root, content, level, monitor);
				}
			}
			existing.remove(file);
		}
		for(Path path : existing) {
			if(Files.isDirectory(path)) {
				ArrayList<Path> toBeDeleted = Files.walk(path).sorted(LONGEST_FIRST).toList();
				for(Path toDelete : toBeDeleted) {
					clean(toDelete);
				}
			} else {
				clean(path);
			}
		}
		Files.setLastModifiedTime(target, FileTime.from(folder.modified));
	}
	private static void clean(Path path) throws IOException {
		try {
			Files.delete(path);
		} catch(DirectoryNotEmptyException e) {
			System.out.println("\nCould not delete " + path + " because " + e.getReason());
		}
	}
	private <E extends Exception> void writeGZIP(Path target, Content content, ExDoubleConsumer<E> monitor)
		throws IOException, E {
		progress(monitor);
		byte[] bytes = createGZIP(content);
		writeFile(target, bytes, content.modified);
		progressFolder(target, monitor);
	}
	private byte[] createGZIP(Content content) throws IOException {
		List<Path> gzFiles = internalListFilesOrFolders(content, true);
		ByteArrayOutputStream bout = new ByteArrayOutputStream(1024 * 1024);
		try(GZIPOutputStream gzip = new GZIPOutputStream(bout);) {
			Content data2 = contents.get(gzFiles.get(0));
			gzip.write(data2.bytes);
		}
		byte[] bytes = bout.toByteArray();
		return bytes;
	}
	private static boolean isZIP(Content content, String fileName) {
		return content.bytes == null && (fileName.endsWith(".jar") || fileName.endsWith(".zip"));
	}
	private boolean isGZIP(Content content, String fileName) {
		return fileName.endsWith(".gz") &&
		content.contained.size() == 1 &&
		internalListFilesOrFolders(content, true).notEmpty();
	}
	private <E extends Exception> void writeZIP(Path target, Content content, int level, ExDoubleConsumer<E> monitor)
		throws IOException, E {
		target = remap(target, level);
		byte[] bytes = createZIP(ROOT, content, level, monitor);
		Instant modified = content.modified;
		writeFile(target, bytes, modified);
	}
	private static void writeFile(Path target, byte[] bytes, Instant modified) throws IOException {
		Files.write(target, bytes);
		Files.setLastModifiedTime(target, FileTime.from(modified));
	}

	static int depth;

	private <E extends Exception> byte[]
		createZIP(Path target, Content content, int level, ExDoubleConsumer<E> monitor) throws IOException, E {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try(ZipOutputStream out = new ZipOutputStream(bout);) {
			out.setLevel(level);
			if(depth++ > 100) {
				System.out.println("BREAK");
			}
			writeFolderToZIP(out, target, content, level, monitor);
		} finally {
			depth--;
		}
		return bout.toByteArray();
	}
	private <E extends Exception> void writeFolderToZIP(
		ZipOutputStream zip,
		Path root,
		Content folder,
		int level,
		ExDoubleConsumer<E> monitor) throws IOException, E {
		progress(monitor);
		for(Path path : internalListFilesOrFolders(folder, true)) {
			Path file = relative(root, remap(path, level));
			Content data = contents.get(path);
			writeFileToZIP(zip, file, data.bytes, data.modified);
			progress(monitor);
		}
		for(Path path : internalListFilesOrFolders(folder, false)) {
			Path file = relative(root, remap(path, level));
			Content content = contents.get(path);
			String fileName = file.getFileName().toString();
			if(level > 0) {
				if(fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
					byte[] bytes = createZIP(path, content, level, monitor);
					writeFileToZIP(zip, file, bytes, content.modified);
					continue;
				} else if(fileName.endsWith(".gz")) {
					List<Path> gzFiles = internalListFilesOrFolders(content, true);
					if(gzFiles.size() == 1) {
						ByteArrayOutputStream bout = new ByteArrayOutputStream(1024 * 1024);
						try(GZIPOutputStream gzip = new GZIPOutputStream(bout);) {
							Content data2 = contents.get(gzFiles.get(0));
							gzip.write(data2.bytes);
						}
						byte[] bytes = bout.toByteArray();
						writeFileToZIP(zip, file, bytes, content.modified);
						progressFolder(path, monitor);
						continue;
					}
				}
			}
			ZipEntry entry = new ZipEntry(toUnixPath(file) + "/");
			entry.setLastModifiedTime(FileTime.from(content.modified));
			zip.putNextEntry(entry);
			zip.closeEntry();
			writeFolderToZIP(zip, root, content, level, monitor);
		}
	}
	private <E extends Exception> void progressFolder(Path path, ExDoubleConsumer<E> monitor) throws E {
		progress(monitor);
		for(Path folder : listFolders(path)) {
			progressFolder(folder, monitor);
		}
		for(Path folder : listFiles(path)) {
			progressFolder(folder, monitor);
		}
	}
	private <E extends Exception> void progress(ExDoubleConsumer<E> monitor) throws E {
		monitor.accept(++progress / (double) progressTotal);
	}
	private Path remap(Path path, int level) throws IOException {
		if(level == 0) {
			return path;
		}
		path = ROOT.relativize(path);
		for(IOFunction<Path, Path> remapper : remappers) {
			path = remapper.apply(path);
		}
		return ROOT.resolve(path);
	}
	private static Path relative(Path root, Path path) {
		Path relativize = root.relativize(path);
		if(relativize.toString().contains("..")) {
			throw new IllegalStateException("Path " + path + " not relative to " + root);
		}
		return relativize;
	}
	private static void writeFileToZIP(ZipOutputStream zip, Path file, byte[] bytes, Instant modified)
		throws IOException {
		ZipEntry entry = createEntry(file, modified);
		zip.putNextEntry(entry);
		zip.write(bytes);
		zip.closeEntry();
	}
	private static ZipEntry createEntry(Path file, Instant modified) {
		String name = toUnixPath(file);
		ZipEntry entry = new ZipEntry(name);
		FileTime lastModifiedTime = FileTime.from(modified);
		entry.setLastModifiedTime(lastModifiedTime);
		return entry;
	}
	private static String toUnixPath(Path path) {
		return String.join("/", Stream.from(path).map(Path::toString).iterable());
	}
}
