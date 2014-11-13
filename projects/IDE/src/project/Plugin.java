package project;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import utils.lists.HashMap;
import utils.lists.Map;
import utils.lists.Paths;
import utils.streams2.Stream;

public class Plugin implements Comparable<Plugin> {
	public final Path root;
	public final HashMap<Path, byte[]> contents;
	public final HashMap<String, String> manifest;
	public final String id;
	public final String shape;
	public final String version;
	private final int version1;
	private final int version2;
	private final int version3;
	private final String version4;

	private Plugin(Path root, HashMap<Path, byte[]> contents, HashMap<String, String> manifest, String id,
		String shape, String version, int version1, int version2, int version3, String version4) {
		this.root = root;
		this.contents = contents.toHashMap();
		this.manifest = manifest.toHashMap();
		this.id = id;
		this.shape = shape;
		this.version = version;
		this.version1 = version1;
		this.version2 = version2;
		this.version3 = version3;
		this.version4 = version4;
	}
	private Plugin(Path path, HashMap<Path, byte[]> contents, HashMap<String, String> manifest) {
		this.shape = path.getFileName().toString().endsWith(".jar") ? ".jar" : "";
		this.root = path;
		this.contents = contents;
		this.manifest = manifest;
		String[] split;
		if(manifest.containsKey("Bundle-Version")) {
			this.version = manifest.get("Bundle-Version");
			split = Arrays.copyOfRange(version.split("\\.", 4), 0, 4);
		} else {
			this.version = "0.0.0";
			split = new String[4];
		}
		this.version1 = split[0] == null ? 0 : Integer.parseInt(split[0]);
		this.version2 = split[1] == null ? 0 : Integer.parseInt(split[1]);
		this.version3 = split[2] == null ? 0 : Integer.parseInt(split[2]);
		this.version4 = split[3] == null ? "" : split[3];
		this.id = parseName();
	}
	public Plugin(Path path, byte[] manifestBytes) throws IOException {
		this(path, new HashMap<>(), parseManifest(manifestBytes));
	}
	public Plugin(Path path, HashMap<Path, byte[]> contents) throws IOException {
		this(path, contents, readManifest(contents));
	}
	public String name() {
		return id;
	}
	public Plugin name(String name) {
		return new Plugin(root, contents, manifest, name, shape, version, version1, version2, version3, version4);
	}
	public @Override int compareTo(Plugin o) {
		int n = id.compareTo(o.id);
		if(n != 0) return n;
		int v1 = version1 - o.version1;
		if(v1 != 0) return v1;
		int v2 = version2 - o.version2;
		if(v2 != 0) return v2;
		int v3 = version3 - o.version3;
		if(v3 != 0) return v3;
		return version4.compareTo(o.version4);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id.hashCode();
		result = prime * result + version1;
		result = prime * result + version2;
		result = prime * result + version3;
		result = prime * result + version4.hashCode();
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		Plugin other = (Plugin) obj;
		if(!id.equals(other.id)) return false;
		if(version1 != other.version1) return false;
		if(version2 != other.version2) return false;
		if(version3 != other.version3) return false;
		if(!version4.equals(other.version4)) return false;
		return true;
	}
	@Override
	public String toString() {
		return "[" + id + "_" + version + shape + ", " + contents.size() + " files @ " + root + "]";
	}
	public String fileName() {
		return bundleName() + "_" + version + shape;
	}
	public static HashMap<String, String> parseManifest(byte[] bytes) throws IOException {
		Attributes attr = new Manifest(new ByteArrayInputStream(bytes)).getMainAttributes();
		Stream<Object> stream = Stream.from(attr.keySet());
		return stream.toMap(o -> o.toString(), o -> attr.getValue((Attributes.Name) o));
	}
	private String parseName() {
		String bundleName = bundleName();
		if(version1 == 1 && "org.eclipse.jdt.annotation".equals(bundleName)) {
			return "org.eclipse.jdt.annotation_v1";
		}
		return bundleName;
	}
	private String bundleName() {
		if(manifest.containsKey("Bundle-SymbolicName")) {
			return manifest.get("Bundle-SymbolicName").split(";")[0].trim();
		}
		String fileName = root.getFileName().toString();
		if(shape == "") {
			return fileName;
		}
		return fileName.substring(0, fileName.length() - shape.length());
	}
	public Map<String, String> getLocalization(Snapshot snapshot) throws IOException {
		if(manifest.containsKey("Bundle-Localization")) {
			Path fileName = root.resolve(manifest.get("Bundle-Localization") + ".properties");
			if(snapshot.isFound(fileName)) {
				return snapshot.getFileAsProperties(fileName);
			}
		}
		Path fileName = root.resolve("OSGI-INF/l10n/bundle.properties");
		if(snapshot.isFound(fileName)) {
			return snapshot.getFileAsProperties(fileName);
		}
		return Map.of();
	}
	private static HashMap<String, String> readManifest(HashMap<Path, byte[]> contents) throws IOException {
		return parseManifest(contents.get(Paths.get(JarFile.MANIFEST_NAME)));
	}
}