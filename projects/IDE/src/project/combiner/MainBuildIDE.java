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
import utils.lists.PosixFilePermissions;
import utils.streams.functions.ExDoubleConsumer;
import utils.streams2.IOStream;

public class MainBuildIDE {
	private static final String DATE_PATTERN_LONG = "EEE MMM d HH':'mm':'ss zzz uuuu";
	private static final String DATE_PATTERN_SHORT = "yyyyMMddHHmm";
	private static final String ECLIPSE_INI = "eclipse.ini";
	private static final String MACOS_ECLIPSE_INI = "Eclipse.app/Contents/MacOS/eclipse.ini";
	private static final String CONFIG_INI = "configuration/config.ini";
	private static final String BUNDLES_INFO = "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
	private static final String PLATFORM_XML = "configuration/org.eclipse.update/platform.xml";
	private static final String SOURCE_INFO = "configuration/org.eclipse.equinox.source/source.info";
	private static final String ORG_ECLIPSE_UI_IDE_PREFS = "configuration/.settings/org.eclipse.ui.ide.prefs";
	private static final String ARTIFACTS_XML = "artifacts.xml";
	private static final String DOT_ECLIPSEPRODUCT = ".eclipseproduct";
	private static final String JVMARGS =
		"p2/org.eclipse.equinox.p2.engine/profileRegistry/ECLIPSE_PROFILE_ID.profile/.data/org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions/jvmargs";
	private static final String PROFILE_GZ =
		"p2/org.eclipse.equinox.p2.engine/profileRegistry/ECLIPSE_PROFILE_ID.profile/PROFILE_TIME.profile.gz/PROFILE_TIME.profile";
	private static final String ECLIPSE_PRODUCT_ID = "org.eclipse.sdk.ide";
	private static final String ECLIPSE_PROFILE_ID = "SDKProfile";
	private static final Locale ENGLISH = Locale.ENGLISH;
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern(DATE_PATTERN_SHORT, ENGLISH);
	private static final DateTimeFormatter DATE_LONG = DateTimeFormatter.ofPattern(DATE_PATTERN_LONG, ENGLISH);
	private static final ZonedDateTime NOW = ZonedDateTime.now(ZoneId.of("UTC"));
	private static final Map<String, String> ENV = environment();
	private static final String FILES = ENV.getOrDefault("files", "");
	private static final String BUILD_TYPE = ENV.getOrDefault("build", "test");
	private static final String RUNNING = ENV.getOrDefault("running", "");
	private static final String WORKSPACE = ENV.getOrDefault("workspace", "");
	private static final String OSGI_OS = ENV.getOrDefault("osgios", "win32");
	private static final String OSGI_WS = ENV.getOrDefault("osgiws", "win32");
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
		fullBuild = "test".equals(BUILD_TYPE) || "full".equals(BUILD_TYPE);
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
		try {
			snapshot.copyFiles(
				"plugins/org.eclipse.m2e.maven.indexer/indexer-core-3.1.0.jar/org/apache/maven/index",
				"plugins/org.eclipse.m2e.maven.indexer/org/apache/maven/index");
			replacePlaceholders();
			pluginToManifest = getPluginToManifest();
			if(workspace != null && isRunningSelfHosted()) {
				registerFile(ORG_ECLIPSE_UI_IDE_PREFS, orgEclipseUiIdePrefs(workspace), p2Modified);
			} else {
				snapshot.removeFile(Paths.get(ORG_ECLIPSE_UI_IDE_PREFS));
			}
			registerFile(JVMARGS, jvmargs(), p2Modified);
			registerFile(BUNDLES_INFO, bundlesInfo(), pluginsModified);
			registerFile(CONFIG_INI, configIni(), platformModified);
			registerFile(ECLIPSE_INI, eclipseIni(false), launcherModified);
			registerFile(MACOS_ECLIPSE_INI, eclipseIni(true), launcherModified);
			registerFile(PLATFORM_XML, platformXml(), featuresModified);
			registerFile(SOURCE_INFO, sourceInfo(), sourceModified);
			registerFile(ARTIFACTS_XML, artifactsXml(), p2Modified);
			registerFile(DOT_ECLIPSEPRODUCT, dotEclipseproduct(), platformModified);
			registerFile(
				PROFILE_GZ,
				P2ProfileGenerator.profileGz(snapshot, PROFILE_TIME, ECLIPSE_PROFILE_ID, ECLIPSE_PRODUCT_ID),
				platformModified);
		} catch(RuntimeException e) {
			System.out.println("\nProbably transient error generating dynamic files: " + e.getMessage());
			e.printStackTrace(System.out);
		}
	}
	private static byte[] orgEclipseUiIdePrefs(String workspace) {
		ArrayList<String> lines = new ArrayList<>();
		String workspacePath = Paths.get(workspace).toString().replace("\\", "\\\\").replace(":", "\\:");
		lines.add("MAX_RECENT_WORKSPACES=5");
		lines.add("RECENT_WORKSPACES=" + workspacePath);
		lines.add("RECENT_WORKSPACES_PROTOCOL=3");
		lines.add("SHOW_WORKSPACE_SELECTION_DIALOG=false");
		lines.add("eclipse.preferences.version=1");
		lines.add("");
		String orgEclipseUiIdePrefs = String.join("\n", lines);
		return orgEclipseUiIdePrefs.getBytes(UTF8);
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
	private static byte[] eclipseIni(boolean insideEclipseApp) {
		String launcher = getBundleFilename("org.eclipse.equinox.launcher");
		String launcherName = "org.eclipse.equinox.launcher." + OSGI_OS + "." + OSGI_WS + ".x86_64";
		String launcherLibrary = getBundleFilename(launcherName);
		ArrayList<String> lines = new ArrayList<>();
		Path jvmDLLPath = Paths.get(System.getProperty("java.home", "")).resolve("bin/server/jvm.dll");
		String pluginsPath = insideEclipseApp ? "../../../plugins/" : "plugins/";
		if(Files.isRegularFile(jvmDLLPath)) {
			String jvmDLL = jvmDLLPath.toString();
			lines.addAll("-vm", jvmDLL);
		}
		lines.add("-startup");
		lines.add(pluginsPath + launcher);
		lines.add("--launcher.library");
		lines.add(pluginsPath + launcherLibrary);
		lines.add("-product");
		lines.add(ECLIPSE_PRODUCT_ID);
		lines.add("--launcher.defaultAction");
		lines.add("openFile");
		lines.add("-showsplash");
		lines.add("org.eclipse.platform");
		lines.add("--launcher.defaultAction");
		lines.add("openFile");
		lines.add("--launcher.appendVmargs");
		lines.add("-vmargs");
		lines.add("-Dosgi.requiredJavaVersion=1.7");
		lines.add("-Xms40m");
		lines.add("-Xmx3500m");
		if("macosx".equals(OSGI_OS)) {
			if(insideEclipseApp) {
				lines.add("-Xdock:icon=../Resources/Eclipse.icns");
			} else {
				lines.add("-Xdock:icon=Eclipse.app/Contents/Resources/Eclipse.icns");
			}
			lines.add("-XstartOnFirstThread");
			lines.add("-Dorg.eclipse.swt.internal.carbon.smallFonts");
		}
		lines.add("");
		String eclipseIni = String.join(System.lineSeparator(), lines);
		return eclipseIni.getBytes(UTF8);
	}
	private static byte[] configIni() {
		ZonedDateTime time = ZonedDateTime.ofInstant(platformModified, ZoneId.of("UTC"));
		String date = DATE_LONG.format(time);
		String osgi = getBundleFilename("org.eclipse.osgi");
		String timestamp = DATE_SHORT.format(time);
		String buildID = getBundleVersion("org.eclipse.platform").replace("qualifier", timestamp);
		String simpleConfigurator = getBundleFilename("org.eclipse.equinox.simpleconfigurator");
		String compatibilityState = getBundleFilename("org.eclipse.osgi.compatibility.state");
		String documentsPath = "linux".equals(OSGI_OS) ? "" : "/Documents";
		ArrayList<String> lines = new ArrayList<>();
		lines.add("#This configuration file was written by: org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxFwConfigFileParser");
		lines.add("#" + date);
		lines.add("org.eclipse.update.reconcile=false");
		lines.add("eclipse.p2.profile=" + ECLIPSE_PROFILE_ID);
		lines.add("osgi.instance.area.default=@user.home" + documentsPath + "/workspace");
		lines.add("osgi.framework=file\\:plugins/" + osgi);
		lines.add("equinox.use.ds=true");
		lines.add("eclipse.buildId=" + buildID);
		lines.add("osgi.bundles=reference\\:file\\:" + simpleConfigurator + "@1\\:start");
		lines.add("org.eclipse.equinox.simpleconfigurator.configUrl=file\\:org.eclipse.equinox.simpleconfigurator/bundles.info");
		lines.add("eclipse.product=" + ECLIPSE_PRODUCT_ID);
		lines.add("osgi.splashPath=platform\\:/base/plugins/org.eclipse.platform");
		lines.add("osgi.framework.extensions=reference\\:file\\:" + compatibilityState);
		lines.add("eclipse.application=org.eclipse.ui.ide.workbench");
		lines.add("eclipse.p2.data.area=@config.dir/../p2");
		lines.add("osgi.bundles.defaultStartLevel=4");
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
	private static byte[] jvmargs() {
		ArrayList<String> lines = new ArrayList<>();
		String date = DATE_LONG.format(ZonedDateTime.ofInstant(p2Modified, ZoneId.of("UTC")));
		lines.add("#" + date);
		if("macosx".equals(OSGI_OS)) {
			lines.add("-Xms=40m,40m");
			lines.add("-Xmx=512m,512m");
		} else {
			lines.add("-Xms=40m");
			lines.add("-Xmx=512m");
		}
		lines.add("");
		String bundlesInfo = String.join("\n", lines);
		return bundlesInfo.getBytes(UTF8);
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
		list2.filter(p -> p.startsWith(ROOT));
		if(list2.size() == 1) {
			Path path = list2.get(0);
			if(path.startsWith(projects)) {
				Path relativePath = ROOT.relativize(path);
				if(relativePath.getNameCount() > 1 && isWatchedFile(relativePath) == false) {
					Path folder = projects.resolve(relativePath.getName(1));
					ArrayList<Path> list3 = Files.walk(folder).toList();
					list3.filter(p -> Files.isRegularFile(p) && isWatchedFile(ROOT.relativize(p)));
					return list3;
				}
			}
		}
		return list2;
	}
	private static Path replaceTrackingPathsWithReal(Path links, Path projects, Path p) {
		if(p.startsWith(links)) {
			return projects.resolve(links.relativize(p));
		}
		return p;
	}
	private static void replacePlaceholders() throws IOException {
		updateJetty();
		updateM2e();
		updatePomVersionToFile(
			"libraries/apache.httpcore/pom.xml",
			"plugins/org.apache.httpcomponents.httpcore.jar/META-INF/MANIFEST.MF");
		updatePomVersionToFile(
			"libraries/apache.httpclient/pom.xml",
			"plugins/org.apache.httpcomponents.httpclient.jar/META-INF/MANIFEST.MF");
		updatePomVersionToManifest(
			"libraries/ow2.sat4j/org.sat4j.core/pom.xml",
			"plugins/org.sat4j.core.jar/META-INF/MANIFEST.MF",
			"9.9.9.token");
		updatePomVersionToManifest(
			"libraries/ow2.sat4j/org.sat4j.pb/pom.xml",
			"plugins/org.sat4j.pb.jar/META-INF/MANIFEST.MF",
			"9.9.9.token");
		updateAntVersionToFile(
			"libraries/apache.ant/STATUS",
			"plugins/org.apache.ant/ant-antlr.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-apache-bcel.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-apache-bsf.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-apache-log4j.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-apache-oro.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-apache-regexp.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-apache-resolver.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-apache-xalan2.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-commons-logging.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-commons-net.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-jai.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-javamail.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-jdepend.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-jmf.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-jsch.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-junit.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-junit4.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-launcher.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-netrexx.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-swing.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant-testutil.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/ant.jar/META-INF/MANIFEST.MF",
			"plugins/org.apache.ant/META-INF/MANIFEST.MF");
		updatePomVersionToFile("libraries/ow2.sat4j/org.sat4j.core/pom.xml", "plugins/org.sat4j.core.jar/sat4j.version");
		updatePomVersionToMaven(
			"eclipse.webtools-common/plugins/org.eclipse.wst.common.frameworks/pom.xml",
			"org.eclipse.wst.common.frameworks.jar",
			"org.eclipse.webtools.common",
			"org.eclipse.wst.common.frameworks");
		updatePomVersionToMaven(
			"eclipse.webtools-sourceediting/bundles/org.eclipse.wst.sse.ui/pom.xml",
			"org.eclipse.wst.sse.ui.jar",
			"org.eclipse.webtools.sourceediting",
			"org.eclipse.wst.sse.ui");
		updatePomVersionToMaven(
			"eclipse.webtools-sourceediting/bundles/org.eclipse.wst.xml.core/pom.xml",
			"org.eclipse.wst.xml.core.jar",
			"org.eclipse.webtools.sourceediting",
			"org.eclipse.wst.xml.core");
		updatePomVersionToMaven(
			"nodeclipse.editbox/pm.eclipse.editbox/pom.xml",
			"pm.eclipse.editbox.jar",
			"pm.eclipse.editbox",
			"pm.eclipse.editbox");
		updatePomVersionToMaven(
			"eclipse.jgit/org.eclipse.jgit.ui/pom.xml",
			"org.eclipse.jgit.ui.jar",
			"org.eclipse.jgit",
			"org.eclipse.jgit.ui");
		updatePomVersionToMaven(
			"nodeclipse.pluginslist/org.nodeclipse.pluginslist.core/pom.xml",
			"org.nodeclipse.pluginslist.core.jar",
			"org.nodeclipse.pluginslist",
			"org.nodeclipse.pluginslist.core");
		updatePomVersionToMaven(
			"eclipse.e4.tools/bundles/org.eclipse.e4.tools.spy/pom.xml",
			"org.eclipse.e4.tools.spy.jar",
			"org.eclipse.e4",
			"org.eclipse.e4.tools.spy");
		updatePomVersionToMaven(
			"eclipse.e4.tools/bundles/org.eclipse.e4.tools.css.spy/pom.xml",
			"org.eclipse.e4.tools.css.spy.jar",
			"org.eclipse.e4",
			"org.eclipse.e4.tools.css.spy");
		updatePomVersionToMaven(
			"eclipse.egit.github/org.eclipse.mylyn.github.doc/pom.xml",
			"org.eclipse.mylyn.github.doc.jar",
			"org.eclipse.mylyn.github",
			"org.eclipse.mylyn.github.doc");
		updatePomVersionToMaven(
			"eclipse.jgit/org.eclipse.jgit.pgm/pom.xml",
			"org.eclipse.jgit.pgm.jar",
			"org.eclipse.jgit",
			"org.eclipse.jgit.pgm");
		updatePomVersionToMaven(
			"eclipse.egit/org.eclipse.egit.mylyn.ui/pom.xml",
			"org.eclipse.egit.mylyn.ui.jar",
			"org.eclipse.egit",
			"org.eclipse.egit.mylyn.ui");
		updatePomVersionToMaven(
			"eclipse.egit.github/org.eclipse.egit.github.core/pom.xml",
			"org.eclipse.egit.github.core.jar",
			"org.eclipse.egit.github",
			"org.eclipse.egit.github.core");
		updatePomVersionToMaven(
			"eclipse.egit.github/org.eclipse.mylyn.github.core/pom.xml",
			"org.eclipse.mylyn.github.core.jar",
			"org.eclipse.mylyn.github",
			"org.eclipse.mylyn.github.core");
		updatePomVersionToMaven(
			"eclipse.gef/org.eclipse.draw2d/pom.xml",
			"org.eclipse.draw2d.jar",
			"org.eclipse.draw2d.plugins",
			"org.eclipse.draw2d");
		updatePomVersionToMaven(
			"eclipse.egit.github/org.eclipse.mylyn.github.ui/pom.xml",
			"org.eclipse.mylyn.github.ui.jar",
			"org.eclipse.mylyn.github",
			"org.eclipse.mylyn.github.ui");
	}
	private static void updateJetty() throws IOException {
		String group = "org.eclipse.jetty";
		String dir = "eclipse.jetty.project";
		updatePomVersionToJetty(group, dir, "continuation");
		updatePomVersionToJetty(group, dir, "security");
		updatePomVersionToJetty(group, dir, "servlet");
		updatePomVersionToJetty(group, dir, "server");
		updatePomVersionToJetty(group, dir, "http");
		updatePomVersionToJetty(group, dir, "util");
		updatePomVersionToJetty(group, dir, "io");
	}
	private static void updatePomVersionToJetty(String group, String dir, String project) throws IOException {
		String plugin = "jetty-" + project;
		String pomPath = dir + "/" + plugin + "/pom.xml";
		String pluginDir = "org.eclipse.jetty." + project + ".jar";
		updatePomVersionToMaven(pomPath, pluginDir, group, plugin);
	}
	private static void updateM2e() throws IOException {
		String dir1 = "eclipse.m2e.core";
		String group1 = "org.eclipse.m2e";
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.core.ui");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.core");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.discovery");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.editor.xml");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.editor");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.jdt.ui");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.jdt");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.launching");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.lifecyclemapping.defaults");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.logback.appender");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.logback.configuration");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.model.edit");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.profiles.core");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.profiles.ui");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.refactoring");
		undatePomVersionToM2e(dir1, group1, "org.eclipse.m2e.scm");
		String dir2 = "eclipse.m2e.core/m2e-maven-runtime";
		undatePomVersionToM2e(dir2, group1, "org.eclipse.m2e.maven.runtime");
		undatePomVersionToM2e(dir2, group1, "org.eclipse.m2e.maven.runtime.slf4j.simple");
		undatePomVersionToM2e(dir2, group1, "org.eclipse.m2e.archetype.common");
		undatePomVersionToM2e(dir2, group1, "org.eclipse.m2e.maven.indexer");
		String dir3 = "eclipse.m2e.workspace";
		String group2 = "io.takari.m2e.workspace";
		undatePomVersionToM2e(dir3, group2, "org.eclipse.m2e.workspace.cli");
	}
	private static void undatePomVersionToM2e(String dirName, String group, String plugin) throws IOException {
		String pomPath = dirName + "/" + plugin + "/pom.xml";
		updatePomVersionToMaven(pomPath, plugin, group, plugin);
	}
	private static void updatePomVersionToMaven(String pomPath, String pluginDir, String group, String plugin)
		throws IOException {
		String pom = "libraries/" + pomPath;
		String manifest = "plugins/" + pluginDir + "/META-INF/MANIFEST.MF";
		String pomProps = "plugins/" + pluginDir + "/META-INF/maven/" + group + "/" + plugin + "/pom.properties";
		String pomXml = "plugins/" + pluginDir + "/META-INF/maven/" + group + "/" + plugin + "/pom.xml";
		updatePomVersionToFile(pom, manifest, pomProps, pomXml);
	}
	private static void updateAntVersionToFile(String ant, String... files) throws IOException {
		String fullVersion = readVersionFromFile(ant, "Development:", "(in GIT Branch: master)", "") + ".qualifier";
		updateVersionToFile(fullVersion, files);
	}
	private static void updatePomVersionToFile(String pom, String... files) throws IOException {
		String fullVersion = readVersionFromPom(pom).replace("SNAPSHOT", "qualifier");
		updateVersionToFile(fullVersion, files);
	}
	private static void updateVersionToFile(String fullVersion, String... files) throws IOException {
		int indexOf = fullVersion.indexOf('.');
		String majorVersion = indexOf == -1 ? fullVersion : fullVersion.substring(0, indexOf);
		indexOf = indexOf == -1 ? -1 : fullVersion.indexOf('.', indexOf + 1);
		String minorVersion = indexOf == -1 ? fullVersion : fullVersion.substring(0, indexOf);
		indexOf = indexOf == -1 ? -1 : fullVersion.indexOf('.', indexOf + 1);
		String serviceVersion = indexOf == -1 ? fullVersion : fullVersion.substring(0, indexOf);
		for(String file : files) {
			Path filePath = fromZipPath(file);
			findAndReplaceInTextFile(filePath, "99.99.99.99", fullVersion);
			findAndReplaceInTextFile(filePath, "99.99.99", serviceVersion);
			findAndReplaceInTextFile(filePath, "99.99", minorVersion);
			findAndReplaceInTextFile(filePath, "99", majorVersion);
		}
	}
	private static void updatePomVersionToManifest(String pom, String manifest, String token) throws IOException {
		Path manifestPath = fromZipPath(manifest);
		String versionFromPom = readVersionFromPom(pom);
		findAndReplaceInTextFile(manifestPath, token, versionFromPom);
	}
	private static String readVersionFromPom(String pom) throws IOException {
		return readVersionFromFile(pom, "<version>", "</version>", "SNAPSHOT");
	}
	private static String readVersionFromFile(String file, String openingTag, String closingTag, String marker)
		throws IOException {
		ArrayList<String> list =
			Files.readAllLines(ROOT.resolve(file)).filter(s -> s.contains(openingTag) && s.contains(marker));
		if(list.isEmpty()) {
			return "9.9.9.token";
		}
		String version = list.get(-1).replace(openingTag, "").replace(closingTag, "").trim();
		return version.replace('-', '.');
	}
	private static void findAndReplaceInTextFile(Path key, String target, String replacement) throws IOException {
		String fileAsString = snapshot.getFileAsString(toZipPath(key));
		if(fileAsString == null) {
			return;
		}
		String replace = fileAsString.replace(target, replacement);
		snapshot.replaceFile(toZipPath(key), replace);
	}
	private static void writeIDE() throws IOException, InterruptedException {
		if(fullBuild && Files.isDirectory(TARGET_IDE)) {
			FileTime epoch = FileTime.from(Instant.EPOCH);
			Files.walk(TARGET_IDE).forEach(p -> Files.setLastModifiedTime(p, epoch));
		}
		snapshot.write(TARGET_IDE, monitor("target"));
		changePermissions("Eclipse.app/Contents/MacOS/eclipse", "rwxr-x---");
		changePermissions("eclipse", "rwxr-xr-x");
		changePermissions("icon.xpm", "rwxr-xr-x");
	}
	private static void changePermissions(String other, String permissions) {
		Path path = TARGET_IDE.resolve(other);
		if(Files.isRegularFile(path)) {
			try {
				Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
			} catch(@SuppressWarnings("unused") IOException | UnsupportedOperationException ignored) {}
		}
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
	static HashMap<Path, HashMap<String, String>> getPluginToManifest() throws IOException {
		HashMap<Path, HashMap<String, String>> map = new HashMap<>();
		for(Path pluginPath : snapshot.listAll(Paths.get("plugins"))) {
			Plugin plugin = snapshot.getFileAsPlugin(pluginPath);
			if(plugin != null &&
			plugin.manifest.containsKey("Bundle-SymbolicName") &&
			plugin.manifest.containsKey("Bundle-Version")) {
				map.put(fromZipPath(pluginPath), plugin.manifest);
			}
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
	private static void readSnapshot() {
		if(Files.isRegularFile(IDE_ZIP)) {
			try {
				snapshot.read(IDE_ZIP);
			} catch(IOException e) {
				snapshot.clear();
				System.out.println("Error reading snapshot:");
				e.printStackTrace(System.out);
			}
		}
	}
	private static Path toCopyOperationTarget(Path path) {
		Path rhs;
		if(path.startsWith("projects/IDE/extras")) {
			Path subpath = path.subpath(3, path.getNameCount());
			rhs = fromZipPath(subpath);
		} else if(path.startsWith("libraries/orbit-sources/com.google.gwt.servlet")) {
			Path subpath = path.subpath(3, path.getNameCount());
			rhs = fromZipPath("plugins/com.google.gwt.servlet.jar").resolve(subpath);
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
		folders.add(root.resolve("libraries/orbit-sources/com.google.gwt.servlet/com"));
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
		if(path.startsWith("libraries/orbit-sources/com.google.gwt.servlet")) {
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
	public static Path determineUnusedTarget() {
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
			if(isSameAsRunning(target)) {
				return false;
			}
			return Files.list(folder).noneMatch(p -> p.getFileName().toString().matches("\\.tmp\\d+\\.instance"));
		} catch(IOException e) {
			System.out.print("Could not examine folder contents in " + folder);
			e.printStackTrace(System.out);
			return false;
		}
	}
	private static boolean isRunningSelfHosted() {
		return isSameAsRunning(TARGET_IDE1) || isSameAsRunning(TARGET_IDE2) || isSameAsRunning(TARGET_IDE3);
	}
	private static boolean isSameAsRunning(Path target) {
		return Paths.get(RUNNING).toAbsolutePath().equals(target.toAbsolutePath());
	}
	private static Map<String, String> environment() {
		Map<String, String> map = Map.from(System.getenv());
		Path target = Paths.get("target/logs").toAbsolutePath();
		String prefix = "build_";
		if(map.containsKey("build")) {
			ArrayList<String> lines = map.keySet().toArrayList().sort().replaceAll(s -> s + "=" + map.get(s)).add("");
			try {
				Files.createDirectories(target);
				String timestamp2 = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss", ENGLISH).format(NOW);
				Files.write(target.resolve(prefix + timestamp2 + ".properties"), lines, UTF8);
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
