package project.combiner;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import project.Feature;
import project.Plugin;
import project.Snapshot;
import utils.lists.ArrayList;
import utils.lists.Arrays;
import utils.lists.Files;
import utils.lists.HashMap;
import utils.lists.List;
import utils.lists.Map;
import utils.lists.Pair;
import utils.lists.Paths;
import utils.streams.functions.ExDoubleConsumer;
import utils.streams2.IOStream;

public class MainBuildIDE {
	private static final String ECLIPSE_INI = "eclipse.ini";
	private static final String CONFIG_INI = "configuration/config.ini";
	private static final String BUNDLES_INFO = "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
	private static final String PLATFORM_XML = "configuration/org.eclipse.update/platform.xml";
	private static final String SOURCE_INFO = "configuration/org.eclipse.equinox.source/source.info";
	private static final String ARTIFACTS_XML = "artifacts.xml";
	private static final String DOT_ECLIPSEPRODUCT = ".eclipseproduct";
	private static final String PROFILE_GZ =
		"p2/org.eclipse.equinox.p2.engine/profileRegistry/ECLIPSE_PROFILE_ID.profile/PROFILE_TIME.profile.gz/PROFILE_TIME.profile";
	private static final String ECLIPSE_PRODUCT_ID = "org.eclipse.platform.ide";
	private static final String ECLIPSE_PROFILE_ID = "SDKProfile";
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final ZonedDateTime NOW = ZonedDateTime.now(ZoneId.of("UTC"));
	private static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm", Locale.ENGLISH).format(NOW);
	private static final Map<String, String> ENV = environment();
	private static final String FILES = ENV.getOrDefault("files", "");
	private static final String BUILD_TYPE = ENV.getOrDefault("build", "test");
	private static final String RUNNING = ENV.getOrDefault("running", "");
	private static final String WORKSPACE = ENV.getOrDefault("workspace", "");
	private static final Path CURRENT_FOLDER = Paths.get("").toAbsolutePath();
	private static final Path ROOT = CURRENT_FOLDER.getParent().getParent();
	private static final Path IDE_ZIP = CURRENT_FOLDER.resolve("target/ide.zip");
	private static final Path TARGET_IDE1 = CURRENT_FOLDER.resolve("target/ide-1");
	private static final Path TARGET_IDE2 = CURRENT_FOLDER.resolve("target/ide-2");
	private static final Path TARGET_IDE3 = CURRENT_FOLDER.resolve("target/ide-99");
	private static final Path TARGET_IDE = determineUnusedTarget();
	private static Snapshot snapshot = new Snapshot(NOW.toInstant()).addPathRemapper(MainBuildIDE::remapP2);
	private static long timeMillis;
	private static String oldPhase;
	private static int phaseCounter;
	private static boolean fullBuild;
	private static Instant pluginsModified = NOW.toInstant();
	private static Instant featuresModified = NOW.toInstant();
	private static Instant p2Modified = NOW.toInstant();
	private static Instant platformModified = NOW.toInstant();
	private static Instant sourceModified = NOW.toInstant();
	private static Instant launcherModified = NOW.toInstant();
	private static String PROFILE_TIME = String.valueOf(platformModified.toEpochMilli());
	private static HashMap<Path, HashMap<String, String>> pluginToManifest;

	public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
		fullBuild = "test".equals(BUILD_TYPE) || "full".equals(BUILD_TYPE) || isTargetOutdated();
		System.out.print("IDE " + BUILD_TYPE + " build, ");
		ArrayList<Path> changes = reportedChanges();
		System.out.println(changes.size() + " file(s) to go, target is " + TARGET_IDE);
		if(changes.isEmpty()) {
			return;
		}
		String workspace = "".equals(WORKSPACE) ? null : new File(new URI(WORKSPACE)).toPath().toString();
		updateTime();
		mergeChanges(changes);
		updateFiles(workspace);
		writeSnapshot();
		writeIDE();
	}
	static void initializeFromSnapshot() throws IOException {
		readSnapshot();
		updateFiles(null);
	}
	private static void updateFiles(String workspace) throws IOException {
		replacePlaceholders();
		pluginToManifest = getPluginToManifest();
		registerFile(BUNDLES_INFO, bundlesInfo(), pluginsModified);
		registerFile(CONFIG_INI, configIni(), platformModified);
		registerFile(ECLIPSE_INI, eclipseIni(workspace), launcherModified);
		registerFile(PLATFORM_XML, platformXml(), featuresModified);
		registerFile(SOURCE_INFO, sourceInfo(), sourceModified);
		registerFile(ARTIFACTS_XML, artifactsXml(), p2Modified);
		registerFile(DOT_ECLIPSEPRODUCT, dotEclipseproduct(), platformModified);
		registerFile(
			PROFILE_GZ,
			P2ProfileGenerator.profileGz(snapshot, PROFILE_TIME, ECLIPSE_PROFILE_ID, ECLIPSE_PRODUCT_ID),
			platformModified);
	}
	private static byte[] dotEclipseproduct() {
		String buildID = getBundleVersion("org.eclipse.platform").replace(".qualifier", "");
		String dotEclipseproduct =
			"name=Eclipse Platform\n" + "id=org.eclipse.platform\n" + "version=" + buildID + "\n" + "";
		return dotEclipseproduct.getBytes(UTF8);
	}
	private static byte[] artifactsXml() {
		String artifactsXml =
			"<?xml version='1.0' encoding='UTF-8'?>\n" +
			"<?artifactRepository version='1.1.0'?>\n" +
			"<repository name='Bundle pool' type='org.eclipse.equinox.p2.artifact.repository.simpleRepository' version='1'>\n" +
			"  <properties size='2'>\n" +
			"    <property name='p2.system' value='true'/>\n" +
			"    <property name='p2.timestamp' value='" +
			p2Modified.toEpochMilli() +
			"'/>\n" +
			"  </properties>\n" +
			"  <mappings size='3'>\n" +
			"    <rule filter='(&amp; (classifier=osgi.bundle))' output='${repoUrl}/plugins/${id}_${version}.jar'/>\n" +
			"    <rule filter='(&amp; (classifier=binary))' output='${repoUrl}/binary/${id}_${version}'/>\n" +
			"    <rule filter='(&amp; (classifier=org.eclipse.update.feature))' output='${repoUrl}/features/${id}_${version}.jar'/>\n" +
			"  </mappings>\n" +
			"  <artifacts size='0'>\n" +
			"  </artifacts>\n" +
			"</repository>\n" +
			"";
		return artifactsXml.getBytes(UTF8);
	}
	private static byte[] sourceInfo() {
		ArrayList<String> lines = new ArrayList<>();
		lines.addAll("#encoding=UTF-8", "#version=1");
		List<Path> all = snapshot.listAll(Paths.get("plugins")).sort();
		for(Path path : all) {
			String sourceBundle = path.getFileName().toString();
			if(sourceBundle.contains(".source_")) {
				int index = sourceBundle.indexOf(".source_") + 7;
				String name = sourceBundle.substring(0, index);
				String suffix = sourceBundle.endsWith(".jar") ? ".jar" : "/";
				int endIndex = sourceBundle.length() - (suffix == "/" ? 0 : 4);
				String version = sourceBundle.substring(index + 1, endIndex);
				String line = name + "," + version + ",plugins/" + name + "_" + version + suffix + ",-1,false";
				lines.add(line);
			}
		}
		lines.add("");
		String sourceInfo = String.join("\n", lines);
		return sourceInfo.getBytes(UTF8);
	}
	private static byte[] platformXml() throws IOException {
		ArrayList<String> lines = new ArrayList<>();
		lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		lines.add("<config transient=\"false\" date=\"" + featuresModified.toEpochMilli() + "\">");
		lines.add("\t<site enabled=\"true\" updateable=\"true\" policy=\"USER-EXCLUDE\" url=\"platform:/base/\">");
		for(Path path : snapshot.listFolders(Paths.get("features"))) {
			Feature feature = snapshot.getFileAsFeature(path);
			if(feature != null) {
				addFeatureLine(lines, feature.id, feature.version, "/");
			}
		}
		lines.addAll("\t</site>", "</config>", "");
		String platformXml = String.join("\n", lines);
		return platformXml.getBytes(UTF8);
	}
	private static void addFeatureLine(ArrayList<String> lines, String id, String version, String suffix) {
		lines.add(String.format(
			"\t\t<feature id=\"%1$s\" url=\"features/%1$s_%2$s%3$s\" version=\"%2$s\">",
			id,
			version,
			suffix));
		lines.add("\t\t</feature>");
	}
	private static byte[] eclipseIni(String workspace) {
		String launcher = getBundleFilename("org.eclipse.equinox.launcher");
		String launcherLibrary = getBundleFilename("org.eclipse.equinox.launcher.win32.win32.x86_64");
		ArrayList<String> lines = new ArrayList<>();
		Path jvmDLLPath = Paths.get(System.getProperty("java.home", "")).resolve("bin/server/jvm.dll");
		if(Files.isRegularFile(jvmDLLPath)) {
			String jvmDLL = jvmDLLPath.toString();
			lines.addAll("-vm", jvmDLL);
		}
//		if(workspace != null) {
//			lines.addAll("-data", workspace);
//		}
		lines.addAll(
			"-startup",
			"plugins/" + launcher,
			"--launcher.library",
			"plugins/" + launcherLibrary,
			"-product",
			ECLIPSE_PRODUCT_ID,
			"--launcher.defaultAction",
			"openFile",
			"-showsplash",
			"org.eclipse.platform",
			"--launcher.defaultAction",
			"openFile",
			"--launcher.appendVmargs",
			"-vmargs",
			"-Dosgi.requiredJavaVersion=1.7",
			"-Xms40m",
			"-Xmx3500m",
			"");
		String eclipseIni = String.join(System.lineSeparator(), lines);
		return eclipseIni.getBytes(UTF8);
	}
	private static byte[] configIni() {
		ZonedDateTime time = ZonedDateTime.ofInstant(platformModified, ZoneId.of("UTC"));
		String date = DateTimeFormatter.ofPattern("EEE MMM d HH':'mm':'ss zzz uuuu", Locale.ENGLISH).format(time);
		String osgi = getBundleFilename("org.eclipse.osgi");
		String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmm", Locale.ENGLISH).format(time);
		String buildID = getBundleVersion("org.eclipse.platform").replace("qualifier", timestamp);
		String simpleConfigurator = getBundleFilename("org.eclipse.equinox.simpleconfigurator");
		String compatibilityState = getBundleFilename("org.eclipse.osgi.compatibility.state");
		ArrayList<String> lines = new ArrayList<>();
		lines.addAll(
			"#This configuration file was written by: org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxFwConfigFileParser",
			"#" + date,
			"org.eclipse.update.reconcile=false",
			"eclipse.p2.profile=" + ECLIPSE_PROFILE_ID,
			"osgi.instance.area.default=@user.home/workspace",
			"osgi.framework=file\\:plugins/" + osgi,
			"equinox.use.ds=true",
			"eclipse.buildId=" + buildID,
			"osgi.bundles=reference\\:file\\:" + simpleConfigurator + "@1\\:start",
			"org.eclipse.equinox.simpleconfigurator.configUrl=file\\:org.eclipse.equinox.simpleconfigurator/bundles.info",
			"eclipse.product=" + ECLIPSE_PRODUCT_ID,
			"osgi.splashPath=platform\\:/base/plugins/org.eclipse.platform",
			"osgi.framework.extensions=reference\\:file\\:" + compatibilityState,
			"eclipse.application=org.eclipse.ui.ide.workbench",
			"eclipse.p2.data.area=@config.dir/../p2",
			"osgi.bundles.defaultStartLevel=4");
		String configIni = String.join(System.lineSeparator(), lines);
		return configIni.getBytes(UTF8);
	}
	private static String getBundleVersion(String name) {
		Path fromZipPath1 = fromZipPath("plugins/" + name + ".jar");
		HashMap<String, String> manifest = pluginToManifest.get(fromZipPath1);
		if(manifest == null) {
			Path fromZipPath2 = fromZipPath("plugins/" + name);
			manifest = pluginToManifest.get(fromZipPath2);
			if(manifest == null) {
				throw new IllegalArgumentException("No manifest found for " + name);
			}
		}
		String bundleVersion = "Bundle-Version";
		String value = manifest.get(bundleVersion);
		if(value == null) {
			throw new IllegalArgumentException("Version not found from " + name);
		}
		return value;
	}
	private static String getBundleFilename(String name) {
		Path path = fromZipPath("plugins/" + name + ".jar");
		HashMap<String, String> manifest = pluginToManifest.get(path);
		if(manifest != null) {
			return generateBundleIDFromManifest(".jar", manifest);
		}
		path = fromZipPath("plugins/" + name);
		manifest = pluginToManifest.get(path);
		if(manifest == null) {
			throw new IllegalArgumentException("No manifest found for " + name);
		}
		return generateBundleIDFromManifest("", manifest);
	}
	private static void registerFile(String name, byte[] bytes, Instant modified) throws IOException {
		snapshot.addFile(Paths.get(name), bytes, modified);
	}
	private static byte[] bundlesInfo() {
		ArrayList<String> lines = new ArrayList<>();
		lines.add("#version=1");
		for(Path pluginPath : pluginToManifest.keySet()) {
			String plugin = pluginPath.getFileName().toString();
			String suffix = plugin.endsWith(".jar") ? ".jar" : "/";
			HashMap<String, String> manifest = pluginToManifest.get(pluginPath);
			addParsedBundleInfoFromManifest(suffix, lines, manifest);
		}
		lines.sort().add("");
		String bundlesInfo = String.join("\n", lines);
		return bundlesInfo.getBytes(UTF8);
	}
	private static void addParsedBundleInfoFromManifest(
		String suffix,
		ArrayList<String> lines,
		HashMap<String, String> manifest) {
		String bundleVersion = "Bundle-Version";
		String version = manifest.get(bundleVersion);
		String bundleSymbolicName = "Bundle-SymbolicName";
		String symbolicName = manifest.get(bundleSymbolicName);
		if(version != null && symbolicName != null) {
			String name = symbolicName.split(";")[0].trim();
			if(name.endsWith(".source") == false) {
				String line = bundleInfoLine(name, version, suffix);
				lines.add(line);
			}
		}
	}
	private static String bundleInfoLine(String name, String version, String ext) {
		return name + "," + version + ",plugins/" + name + "_" + version + ext + "," + bundleStartSettings(name);
	}
	private static String bundleStartSettings(String plugin) {
		switch(plugin) {/*Q*/
			case "org.eclipse.core.runtime"                 : return "4,true" ;
			case "org.eclipse.equinox.common"               : return "2,true" ;
			case "org.eclipse.equinox.ds"                   : return "2,true" ;
			case "org.eclipse.equinox.event"                : return "2,true" ;
			case "org.eclipse.equinox.p2.reconciler.dropins": return "4,true" ;
			case "org.eclipse.equinox.simpleconfigurator"   : return "1,true" ;
			case "org.eclipse.osgi"                         : return "-1,true";
			default                                         : return "4,false";
		/*E*/}
	}
	private static ArrayList<Path> reportedChanges() throws IOException {
		if(fullBuild) {
			return streamAllFiles(ROOT);
		}
		if(FILES.isEmpty()) {
			return ArrayList.of();
		}
		readSnapshot();
		Path links = CURRENT_FOLDER.resolve("links");
		Path projects = ROOT.resolve("projects");
		ArrayList<String> list = ArrayList.of(FILES.replaceAll("^\"|\"$", "").split("\" \"")).removeIf(String::isEmpty);
		ArrayList<Path> list2 = list.map(Paths::get);
		list2.replaceAll(p -> replaceTrackingPathsWithReal(links, projects, p));
		list2.filter(p -> isTrackedFile(projects, p));
		return list2;
	}
	private static boolean isTrackedFile(Path projects, Path changed) {
		int minNameCount = projects.getNameCount() + 2;
		return changed.getNameCount() > minNameCount &&
		changed.startsWith(projects) &&
		changed.subpath(0, minNameCount).endsWith("bin") ||
		changed.startsWith(ROOT);
	}
	private static Path replaceTrackingPathsWithReal(Path links, Path projects, Path p) {
		if(p.startsWith(links)) {
			return projects.resolve(links.relativize(p));
		}
		return p;
	}
	private static void replacePlaceholders() throws IOException {
		updatePomVersionToManifest(
			"libraries/ow2.sat4j/org.sat4j.core/pom.xml",
			"plugins/org.sat4j.core.jar/META-INF/MANIFEST.MF",
			"9.9.9.token");
		updatePomVersionToManifest(
			"libraries/ow2.sat4j/org.sat4j.pb/pom.xml",
			"plugins/org.sat4j.pb.jar/META-INF/MANIFEST.MF",
			"9.9.9.token");
		updatePomVersionToFile("libraries/ow2.sat4j/org.sat4j.core/pom.xml", "plugins/org.sat4j.core.jar/sat4j.version");
	}
	private static void updatePomVersionToFile(String pom, String file) throws IOException {
		Path filePath = fromZipPath(file);
		String text = readVersionFromPom(pom) + "\n";
		overwriteTextFile(filePath, text);
	}
	private static void updatePomVersionToManifest(String pom, String manifest, String token) throws IOException {
		Path manifestPath = fromZipPath(manifest);
		String versionFromPom = readVersionFromPom(pom);
		findAndReplaceInTextFile1(manifestPath, token, versionFromPom);
	}
	private static String readVersionFromPom(String pom) throws IOException {
		ArrayList<String> list = Files.readAllLines(ROOT.resolve(pom)).filter(s -> s.contains("<version>"));
		if(list.isEmpty()) {
			return "9.9.9.token";
		}
		String version = list.get(0).trim().replace("<version>", "").replace("</version>", "");
		return version.replace('-', '.');
	}
	private static void findAndReplaceInTextFile1(Path key, String target, String replacement) throws IOException {
		findAndReplaceInTextFile(key, target, replacement);
	}
	private static void findAndReplaceInTextFile(Path key, String target, String replacement) throws IOException {
		String replace = snapshot.getFileAsString(toZipPath(key)).replace(target, replacement);
		snapshot.replaceFile(toZipPath(key), replace);
	}
	private static void overwriteTextFile(Path key, String text) throws IOException {
		snapshot.replaceFile(toZipPath(key), text);
	}
	private static boolean isTargetOutdated() throws IOException {
		Path otherIde = TARGET_IDE == TARGET_IDE1 ? TARGET_IDE2 : TARGET_IDE1;
		Path targetInfo = TARGET_IDE.resolve(BUNDLES_INFO);
		if(Files.isRegularFile(targetInfo) == false) {
			return true;
		}
		Path otherInfo = otherIde.resolve(BUNDLES_INFO);
		if(Files.isRegularFile(otherInfo) == false) {
			return false;
		}
		Instant otherInstant = Files.getLastModifiedTime(otherInfo).toInstant();
		Instant targetInstant = Files.getLastModifiedTime(targetInfo).toInstant();
		return otherInstant.isAfter(targetInstant);
	}
	private static void writeIDE() throws IOException, InterruptedException {
		if(fullBuild && Files.isDirectory(TARGET_IDE)) {
			FileTime epoch = FileTime.from(Instant.EPOCH);
			Files.walk(TARGET_IDE).forEach(p -> Files.setLastModifiedTime(p, epoch));
		}
		snapshot.write(TARGET_IDE, monitor("target"));
	}
	private static Path remapP2(Path file) {
		if(file.startsWith("p2/org.eclipse.equinox.p2.engine/profileRegistry") == false) {
			return file;
		}
		ArrayList<Path> list = ArrayList.fromIterable(file);
		if(list.size() > 3 && list.get(3).startsWith("ECLIPSE_PROFILE_ID.profile")) {
			list.set(3, Paths.get(ECLIPSE_PROFILE_ID + ".profile"));
		}
		if(list.size() > 4 && list.get(4).startsWith("PROFILE_TIME.profile.gz")) {
			list.set(4, Paths.get(PROFILE_TIME + ".profile.gz"));
		}
		if(list.size() > 5 && list.get(5).startsWith("PROFILE_TIME.profile")) {
			list.set(5, Paths.get(PROFILE_TIME + ".profile"));
		}
		Path resolved = Paths.get("");
		for(Path path : list) {
			resolved = resolved.resolve(path);
		}
		return resolved;
	}
	private static String generateBundleIDFromManifest(String suffix, HashMap<String, String> manifest) {
		String bundleVersion = "Bundle-Version";
		String version = manifest.get(bundleVersion);
		if(version == null) {
			throw new IllegalArgumentException("Manifest does not have Bundle-Version");
		}
		String bundleSymbolicName = "Bundle-SymbolicName";
		String symbolicName = manifest.get(bundleSymbolicName);
		if(symbolicName == null) {
			throw new IllegalArgumentException("Manifest does not have Bundle-SymbolicName");
		}
		String name = symbolicName.split(";")[0].trim();
		return name + "_" + version + suffix;
	}
	private static HashMap<Path, HashMap<String, String>> getPluginToManifest() throws IOException {
		HashMap<Path, HashMap<String, String>> map = new HashMap<>();
		for(Path pluginPath : snapshot.listAll(Paths.get("plugins"))) {
			Plugin plugin = snapshot.getFileAsPlugin(pluginPath);
			map.put(fromZipPath(pluginPath), plugin.manifest);
		}
		return map;
	}
	private static void writeSnapshot() throws IOException, InterruptedException {
		if(snapshot.notFound(Paths.get("dropins"))) {
			snapshot.addFolder(Paths.get("dropins"), NOW.toInstant());
		}
		snapshot.write(IDE_ZIP, monitor("cache"));
	}
	private static ExDoubleConsumer<InterruptedException> monitor(String phase) {
		return d -> reportTime(phase, (int) (1000 * d), 1000);
	}
	private static void mergeChanges(ArrayList<Path> changes) throws IOException, InterruptedException {
		long bytesRead = 0;
		int total = changes.size();
		int count = 0;
		for(Path next : changes) {
			reportTime("changes", ++count, total);
			if(next.startsWith(ROOT) && Files.isDirectory(next) == false) {
				Path path = ROOT.relativize(next);
				if(isWatchedFile(path)) {
					Path target = toCopyOperationTarget(path);
					if(Files.isRegularFile(next)) {
						Path updatedFile = ROOT.resolve(path);
						Instant modified = Files.getLastModifiedTime(updatedFile).toInstant();
						byte[] bytes = Files.readAllBytes(updatedFile);
						bytesRead += bytes.length;
						snapshot.addFile(toZipPath(target), bytes, modified);
					} else {
						snapshot.removeFile(toZipPath(target));
					}
				}
			}
		}
		if(bytesRead > 10 * 1024 * 1024) {
			System.out.printf("%n(done reading %,d bytes)", bytesRead);
		}
	}
	private static void readSnapshot() throws IOException {
		if(Files.isRegularFile(IDE_ZIP)) {
			snapshot.read(IDE_ZIP);
		}
	}
	private static Path toCopyOperationTarget(Path path) {
		Path rhs;
		if(path.startsWith("projects/IDE/extras")) {
			Path subpath = path.subpath(3, path.getNameCount());
			rhs = fromZipPath(subpath);
		} else {
			Path subpath = path.subpath(4, path.getNameCount());
			if(path.startsWith("libraries/eclipse.pde.ui/ui/org.eclipse.pde.ui.templates")) {
				rhs = fromZipPath("plugins/org.eclipse.pde.ui.templates.jar").resolve(subpath);
			} else if(path.startsWith("libraries/eclipse.jdt.core/org.eclipse.jdt.annotation_v1/src")) {
				rhs = fromZipPath("plugins/org.eclipse.jdt.annotation_v1.jar/src").resolve(subpath);
			} else if(path.startsWith("libraries/eclipse.jdt.core/org.eclipse.jdt.annotation/src")) {
				rhs = fromZipPath("plugins/org.eclipse.jdt.annotation.jar/src").resolve(subpath);
			} else {
				rhs = fromZipPath(subpath);
			}
		}
		return rhs;
	}
	static Path fromZipPath(String path) {
		return fromZipPath(Paths.get(path));
	}
	static Path fromZipPath(Path path) {
		return path;
	}
	private static Path toZipPath(Path path) {
		return path;
	}
	private static ArrayList<Path> streamAllFiles(Path root) throws IOException {
		ArrayList<Path> folders = new ArrayList<>();
		for(Path project : Files.list(root.resolve("projects")).filter(Files::isDirectory).toList()) {
			Path projectBin = project.resolve("bin");
			if(Files.isDirectory(projectBin)) {
				ArrayList<Path> projectBins = Files.list(projectBin).filter(Files::isDirectory).toList();
				folders.addAll(projectBins);
			}
		}
		for(String template : Arrays.asList("templates_3.0", "templates_3.1", "templates_3.3", "templates_3.5")) {
			Path folder = root.resolve("libraries/eclipse.pde.ui/ui/org.eclipse.pde.ui.templates").resolve(template);
			if(Files.isDirectory(folder)) {
				folders.add(folder);
			}
		}
		folders.add(root.resolve("libraries/eclipse.jdt.core/org.eclipse.jdt.annotation_v1/src"));
		folders.add(root.resolve("libraries/eclipse.jdt.core/org.eclipse.jdt.annotation/src"));
		ArrayList<Path> all = new ArrayList<>(folders.size() * 100);
		for(Path folder : folders) {
			all.addAll(Files.walk(folder).filter(Files::isRegularFile).toList());
		}
		return all;
	}
	private static boolean isWatchedFile(Path path) {
		if(path.getNameCount() < 5) {
			if(path.getNameCount() == 4 && path.startsWith("projects/IDE/extras")) {
				return path.getFileName().toString().equals(".instructions.txt") == false;
			}
			return false;
		}
		if(path.startsWith("libraries/eclipse.jdt.core/org.eclipse.jdt.annotation_v1/src")) {
			return true;
		}
		if(path.startsWith("libraries/eclipse.jdt.core/org.eclipse.jdt.annotation/src")) {
			return true;
		}
		if(path.startsWith("libraries/eclipse.pde.ui/ui/org.eclipse.pde.ui.templates")) {
			switch(path.getName(4).toString()) {
				case "templates_3.0":
				case "templates_3.1":
				case "templates_3.3":
				case "templates_3.5":
					return true;
				default:
					return false;
			}
		}
		if(path.startsWith("projects")) {
			if(path.startsWith("projects/IDE/extras")) {
				return path.getName(3).toString().startsWith(".") == false;
			}
			return path.getName(2).toString().equals("bin");
		}
		return false;
	}
	private static void reportTime(String phase, int progress, int total) throws InterruptedException {
		boolean done = progress == total && oldPhase == phase;
		if(done || System.currentTimeMillis() > timeMillis) {
			updateTime();
			if(phase != oldPhase) {
				phaseCounter = 0;
			}
			if(phaseCounter-- <= 0) {
				phaseCounter = 9;
				System.out.print("\n");
				if(phase == oldPhase) {
					System.out.print(oldPhase.replaceAll(".", " "));
				}
			}
			if(phase != oldPhase) {
				System.out.print(phase);
				oldPhase = phase;
			}
			System.out.printf(" %.1f%%", 100.0 * progress / total);
			if(Thread.interrupted()) {
				Thread.currentThread().interrupt();
				throw new InterruptedException();
			}
		}
	}
	private static void updateTime() {
		timeMillis = System.currentTimeMillis() + 1000;
	}
	private static Path determineUnusedTarget() {
		if(examineIdeTarget(TARGET_IDE1)) {
			return TARGET_IDE1;
		}
		if(examineIdeTarget(TARGET_IDE2)) {
			return TARGET_IDE2;
		}
		return TARGET_IDE3;
	}
	private static boolean examineIdeTarget(Path target) {
		Path folder = target.resolve("configuration/org.eclipse.equinox.app/.manager");
		if(Files.isDirectory(folder) == false) {
			return true;
		}
		try {
			if(Paths.get(RUNNING).toAbsolutePath().equals(target.toAbsolutePath())) {
				return false;
			}
			return Files.list(folder).noneMatch(p -> p.getFileName().toString().matches("\\.tmp\\d+\\.instance"));
		} catch(IOException e) {
			System.out.print("Could not examine folder contents in " + folder);
			e.printStackTrace(System.out);
			return false;
		}
	}
	private static Map<String, String> environment() {
		Map<String, String> map = Map.from(System.getenv());
		Path target = Paths.get("target/logs").toAbsolutePath();
		String prefix = "build_";
		if(map.containsKey("build")) {
			ArrayList<String> lines = map.keySet().toArrayList().sort().replaceAll(s -> s + "=" + map.get(s)).add("");
			try {
				Files.createDirectories(target);
				Files.write(target.resolve(prefix + TIMESTAMP + ".properties"), lines, UTF8);
			} catch(IOException e) {
				e.printStackTrace(System.out);
				System.out.println("Could not create log of build environment, proceeding anyway...");
			}
		} else if(Files.isDirectory(target)) {
			IOStream<Path> stream =
				Files.list(target).filter(p -> p.getFileName().toString().startsWith(prefix)).filter(
					Files::isRegularFile).sorted().limit(1);
			ArrayList<ArrayList<String>> envs;
			try {
				envs = stream.toList().map(Files::readAllLines);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
			if(envs.notEmpty()) {
				ArrayList<String> lines = envs.get(0).filter(s -> s.contains("="));
				HashMap<String, ArrayList<String>> multiMap =
					lines.stream().toMultiMap(s -> s.substring(0, s.indexOf('=')), s -> s.substring(s.indexOf('=') + 1));
				HashMap<String, String> hashMap = multiMap.entrySet().stream().toMap(Pair::lhs, p -> p.rhs.get(0));
				return hashMap.toMap();
			}
		}
		return map;
	}
}
