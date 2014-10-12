package project.generator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.stream.Collector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.Opcodes;
import project.Plugin;
import utils.lists.ArrayList;
import utils.lists.Files;
import utils.lists.HashMap;
import utils.lists.HashMap.Entry;
import utils.lists.HashSet;
import utils.lists.List;
import utils.lists.Map;
import utils.lists.Pair;
import utils.lists.Paths;
import utils.lists.Set;
import utils.streams2.Collectors;
import utils.streams2.IOStream;
import utils.streams2.Stream;
import utils.streams2.Streams;

public class Main {
	private static final Path MANIFEST_PATH = Paths.get(JarFile.MANIFEST_NAME);
	private static final Path PLUGINS_FOLDER = Paths.get(System.getProperty("user.home", ""), "evening/plugins");

	public static void main(String[] args) throws IOException {
		List<Plugin> plugins = readPluginContents(PLUGINS_FOLDER);
		Map<String, List<String>> depending = combineDependencies(plugins);
		Map<String, List<String>> exported = reexportedDependencies(plugins);
		for(Plugin plugin : plugins) {
			printManifestInfo(PLUGINS_FOLDER, depending, plugin);
		}
		Map<String, List<String>> dependents = invert(depending).remove("system.bundle");
		Map<String, List<String>> depended = invert(dependents);
		printDependencies(depended, "->");
		printDependencies(dependents, "<-");
		expandPluginContents(plugins);
		Path root = Paths.get("").toAbsolutePath().getParent().getParent();
		generateProjects(root, depended, exported, plugins);
	}
	private static void expandPluginContents(List<Plugin> plugins) throws IOException {
		for(Plugin plugin : plugins) {
			Stream<Pair<Path, byte[]>> stream = plugin.contents.entrySet().map(Entry::toPair).stream();
			IOStream<Pair<Path, byte[]>> expanded = stream.toIO().flatMap(Main::deepExpand);
			Path bundle = Paths.get(plugin.id + plugin.shape);
			HashMap<Path, byte[]> hashMap = expanded.toMap(p -> bundle.resolve(p.lhs), Pair::rhs);
			plugin.contents.clear().putAll(hashMap);
		}
	}
	private static IOStream<Pair<Path, byte[]>> deepExpand(Pair<Path, byte[]> content) throws IOException {
		String name = content.lhs.getFileName().toString();
		if(name.endsWith(".jar")) {
			IOStream<Pair<Path, byte[]>> io = readZip(content.rhs).entrySet().stream().toIO();
			IOStream<Pair<Path, byte[]>> expanded = io.flatMap(Main::deepExpand);
			return expanded.map(p -> p.keepingRhs(content.lhs.resolve(p.lhs)));
		}
		return Stream.of(content).toIO();
	}
	private static void generateProjects(
		Path root,
		Map<String, List<String>> depending,
		Map<String, List<String>> exported,
		List<Plugin> plugins) throws IOException {
		List<String> pluginNames = plugins.map(Plugin::name).sort();
		Map<String, List<Path>> sourceFolders = findSourceFolders(root, pluginNames);
		printRoots(sourceFolders);
		HashSet<Path> projectFolders = HashSet.of();
		Path projects = root.resolve("projects");
		for(Plugin plugin : plugins) {
			String pluginName = plugin.id;
			String projectName = projectName(sourceFolders, pluginName);
			if(projectName == null) {
				continue;
			}
			Path projectFolder = projects.resolve(projectName);
			Files.createDirectories(projectFolder);
			projectFolders.add(projectFolder);
			List<Path> sources = sourceFolders.get(pluginName);
			writeDotProject(projectFolder, projectName, sources);
			List<Path> libs = sources.replaceAll(p -> root.resolve(p));
			List<String> required = depending.getOrDefault(pluginName, List.of());
			Set<String> exports = exported.get(pluginName).toSet();
			writeDotClasspath(projectFolder, pluginName, libs, sourceFolders, required, exports, plugin);
		}
		printUnusedProjects(projectFolders, projects);
		printDependencyLoops(projects);
	}
	private static void printDependencyLoops(Path projects) throws IOException {
		Map<String, List<String>> deps =
			Files.list(projects).map(p -> p.resolve(".classpath")).filter(Files::isRegularFile).map(
				Main::readRequiredProjects).toMap(Pair::lhs, Pair::rhs).toMap();
		HashSet<Pair<String, String>> checked = HashSet.of();
		List<List<String>> chains = deps.keySet().stream().map(List::of).toList().toList();
		while(chains.notEmpty()) {
			ArrayList<List<String>> newChains = new ArrayList<>(chains.size() * 10);
			for(List<String> chain : chains) {
				if(chain.size() > 1) {
					String start = chain.get(0);
					String end = chain.get(-1);
					if(start.equals(end)) {
						System.out.println("Chain " + chain.size() + ":\t" + chain);
					} else {
						Pair<String, String> pair = Pair.of(start, end);
						if(checked.contains(pair)) {
							continue;
						}
						checked.add(pair);
					}
				}
				List<String> dep = deps.getOrDefault(chain.get(-1), List.of());
				List<List<String>> newChain = dep.map(s -> chain.add(s)).toList();
				newChains.addAll(newChain);
			}
			chains = newChains.toList();
		}
	}
	private static Pair<String, List<String>> readRequiredProjects(Path classpath) throws IOException {
		ArrayList<String> list = Files.readAllLines(classpath);
		list.replaceAll(String::trim);
		list.filter(s -> s.contains("kind=\"src\" path=\"/IDE") && s.endsWith("\"/>"));
		list.replaceAll(s -> s.substring(s.lastIndexOf("kind=\"src\" path=\"/IDE") + 18, s.length() - 3));
		String project = classpath.getParent().getFileName().toString();
		return Pair.of(project, list.toList());
	}
	private static void printUnusedProjects(HashSet<Path> projectFolders, Path projects) throws IOException {
		ArrayList<Path> list =
			Files.list(projects).toSet().removeAll(projectFolders).removeIf(
				p -> (p.getFileName() + "-other-").replace("-compile-", "-other-").replace("-all-", "-other-").startsWith(
					"IDE-other-")).toArrayList().sort();
		if(list.notEmpty()) {
			System.out.println("\nUnused folders inside projects folder:");
			for(Path path : list) {
				System.out.println(path);
			}
		}
	}
	private static void writeDotClasspath(
		Path project,
		String pluginName,
		List<Path> libs,
		Map<String, List<Path>> sourceFolders,
		List<String> depending,
		Set<String> exported,
		Plugin plugin) throws IOException {
		if(libs.size() > 1) {
			System.out.println(pluginName + " uses multiple repositories: " + libs);
		}
		List<Path> pluginFiles = sourceFilesForPlugin(plugin);
		List<Path> locallyAvailableFiles = listLocallyAvailableFiles(project);
		List<Path> availableFiles =
			deepListAvailableFiles(locallyAvailableFiles, libs, pluginFiles, pluginName, project);
		Map<Pair<Path, Path>, ArrayList<Path>> srcMappings = mapAvailableToFiles(availableFiles, pluginFiles).toMap();
		ArrayList<String> classpath = ArrayList.of("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<classpath>");
		List<Pair<Path, Path>> srcs =
			srcMappings.keySet().toList().sort(
				Comparator.<Pair<Path, Path>, Path> comparing(Pair::lhs).thenComparing(Pair::rhs));
		ArrayList<String> outputs = new ArrayList<>();
		for(int i = 0, n = srcs.size(); i < n; i++) {
			Pair<Path, Path> mapping = srcs.get(i);
			ArrayList<Path> files = srcMappings.get(mapping);
			classpath.add(files.stream().map(Path::toString).collect(Collectors.joining("\n\t     ", "\t<!-- ", " -->")));
			boolean commented = mapping.lhs.toString().contains("x-miss-x");
			String quoteBeg = commented ? "<!-- " : "";
			String quoteEnd = commented ? " -->" : "";
			String outputPath = combinableUnixPath(mapping.rhs);
			if(commented == false && outputs.contains(outputPath) == false) {
				outputs.add(outputPath);
			}
			int sequenceNum = commented ? 0 : outputs.indexOf(outputPath) + 1;
			String output = "bin/" + sequenceNum + "/plugins" + outputPath;
			String path = toUnixPath(mapping.lhs);
			String including = commented ? "" : collectInclusions(availableFiles, files, mapping.lhs);
			String excluding = commented ? "" : collectExclusions(srcs, mapping.lhs, locallyAvailableFiles, libs);
			String newItem =
				String.format(
					"	%s<classpathentry%s%s kind=\"src\" output=\"%s\" path=\"%s\"/>%s",
					quoteBeg,
					excluding,
					including,
					output,
					path,
					quoteEnd);
			classpath.add(newItem);
		}
		classpath.add("	<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>");
		for(String name : requiredProjects(pluginName, depending).sort()) {
			String export = exported.contains(name) ? " exported=\"true\"" : "";
			String path = projectName(sourceFolders, name);
			if(path != null) {
				classpath.add("	<classpathentry" + export + " kind=\"src\" path=\"/" + path + "\"/>");
			}
		}
		classpath.add("	<classpathentry kind=\"output\" path=\"classes\"/>");
		classpath.add("</classpath>");
		classpath.add("");
		String dotClasspath = classpath.stream().collect(Collectors.joining("\n"));
		String fileName = ".classpath";
		writeFile(project, dotClasspath, fileName);
		cleanOldOutputs(project, outputs);
	}
	private static String projectName(Map<String, List<Path>> sourceFolders, String plugin) {
		if(plugin.startsWith("IDE-compile")) {
			return plugin;
		}
		List<Path> list = sourceFolders.get(plugin);
		if(list == null || list.isEmpty()) {
			return null;
		}
		Path source = list.get(0);
		return projectName(plugin, source);
	}
	private static void cleanOldOutputs(Path projectFolder, ArrayList<String> outputs) throws IOException {
		Path binPath = projectFolder.resolve("bin");
		if(Files.isDirectory(binPath) == false) {
			return;
		}
		HashSet<Path> all = Files.walk(binPath).map(projectFolder::relativize).toSet();
		for(int i = 0, n = outputs.size(); i < n; i++) {
			Path other = Paths.get("bin", String.valueOf(i + 1), "plugins" + outputs.get(i));
			all.removeIf(p -> p.startsWith(other));
			for(int j = 1, m = other.getNameCount(); j < m; j++) {
				all.remove(other.subpath(0, j));
			}
		}
		if(all.isEmpty()) {
			return;
		}
		ArrayList<Path> list = all.toArrayList().sort(Comparator.comparingInt(Path::getNameCount).reversed());
		for(Path path : list) {
			Path path2 = projectFolder.resolve(path);
			if(path2.normalize().startsWith(projectFolder) == false) {
				throw new IllegalStateException("Invalid bin " + path2);
			}
			Files.delete(path2);
		}
	}
	private static List<String> requiredProjects(String plugin, List<String> required) {
		required = addCustomRequiredProjects(plugin, required);
		switch(plugin) {
			case "org.apache.batik.util":
				return required.remove("org.apache.batik.util.gui");
			case "org.eclipse.core.runtime":
				return required.remove("org.eclipse.core.runtime.compatibility.auth");
			case "org.eclipse.egit.doc":
				return required.remove("org.eclipse.mylyn.wikitext.mediawiki.core").remove(
					"org.eclipse.mylyn.wikitext.core").remove("org.eclipse.mylyn.wikitext.core.ant");
			case "org.eclipse.swt.win32.win32.x86_64":
				return required.remove("org.eclipse.swt");
			case "org.eclipse.team.cvs.ssh2":
				return required.remove("org.eclipse.team.cvs.ssh");
			default:
				return required;
		}
	}
	private static List<String> addCustomRequiredProjects(String plugin, List<String> required) {
		switch(plugin) {
			case "org.apache.ant":
				return required.addAll("IDE-compile-various", "IDE-compile-netrexx", "org.junit");
			case "org.apache.batik.util.gui":
				return required.add("org.apache.batik.util");
			case "org.apache.commons.compress":
				return required.add("IDE-compile-org.tukaani.xz");
			case "org.apache.commons.logging":
				return required.addAll("javax.servlet", "IDE-compile-various");
			case "org.apache.felix.gogo.command":
				return required.add("IDE-compile-apache.felix");
			case "org.apache.httpcomponents.httpclient":
				return required.add("IDE-compile-various.caches");
			case "org.eclipse.ant.core":
				return required.add("org.apache.ant");
			case "org.eclipse.core.resources":
				return required.add("org.apache.ant");
			case "org.eclipse.core.runtime.compatibility.registry":
				return required.addAll("org.eclipse.core.runtime", "org.eclipse.osgi");
			case "org.eclipse.e4.ui.model.workbench":
				return required.add("org.eclipse.emf.common");
			case "org.eclipse.e4.ui.workbench":
				return required.addAll("org.eclipse.emf.ecore", "org.eclipse.emf.common");
			case "org.eclipse.e4.ui.workbench.addons.swt":
				return required.addAll("org.eclipse.emf.ecore", "org.eclipse.emf.common");
			case "org.eclipse.e4.ui.workbench.renderers.swt":
				return required.add("org.eclipse.emf.common");
			case "org.eclipse.e4.ui.workbench.renderers.swt.cocoa":
				return required.addAll(
					"javax.inject",
					"org.eclipse.core.commands",
					"org.eclipse.e4.core.commands",
					"org.eclipse.e4.core.contexts",
					"org.eclipse.e4.core.di",
					"org.eclipse.e4.core.services",
					"org.eclipse.e4.ui.bindings",
					"org.eclipse.e4.ui.model.workbench",
					"org.eclipse.e4.ui.services",
					"org.eclipse.e4.ui.workbench",
					"org.eclipse.equinox.common",
					"org.eclipse.osgi",
					"org.eclipse.osgi.services",
					"org.eclipse.swt.cocoa.macosx.x86_64");
			case "org.eclipse.e4.ui.workbench.swt":
				return required.addAll("org.eclipse.emf.ecore", "org.eclipse.emf.common");
			case "org.eclipse.ecf.provider.filetransfer.httpclient4.ssl":
				return required.addAll("org.eclipse.ecf.filetransfer", "org.eclipse.osgi");
			case "org.eclipse.ecf.provider.filetransfer.ssl":
				return required.add("org.eclipse.osgi");
			case "org.eclipse.egit.ui":
				return required.add("org.eclipse.ui.views");
			case "org.eclipse.emf.ecore.change":
			case "org.eclipse.emf.ecore.xmi":
				return required.add("org.eclipse.emf.common");
			case "org.eclipse.equinox.p2.director.app":
				return required.add("org.apache.ant");
			case "org.eclipse.equinox.p2.garbagecollector":
				return required.add("org.eclipse.equinox.common");
			case "org.eclipse.equinox.p2.jarprocessor":
				return required.add("org.apache.ant");
			case "org.eclipse.equinox.p2.metadata.repository":
				return required.add("org.apache.ant");
			case "org.eclipse.equinox.p2.publisher.eclipse":
				return required.add("org.apache.ant");
			case "org.eclipse.equinox.p2.repository.tools":
				return required.addAll("org.apache.ant", "org.eclipse.equinox.p2.jarprocessor");
			case "org.eclipse.equinox.security.macosx":
			case "org.eclipse.equinox.security.win32.x86_64":
				return required.add("org.eclipse.osgi");
			case "org.eclipse.help.base":
				return required.add("org.apache.ant");
			case "org.eclipse.jetty.continuation":
				return required.add("IDE-compile-org.mortbay.jetty.util");
			case "org.eclipse.jetty.security":
				return required.add("org.eclipse.jetty.io");
			case "org.eclipse.jetty.server":
			case "org.eclipse.jetty.servlet":
				return required.add("IDE-compile-org.eclipse.jetty.jmx");
			case "org.eclipse.jetty.util":
				return required.add("IDE-compile-org.slf4j");
			case "org.eclipse.jdt.core":
				return required.add("org.apache.ant");
			case "org.eclipse.pde.api.tools":
				return required.add("org.apache.ant");
			case "org.eclipse.pde.api.tools.ui":
				return required.add("org.eclipse.ui.views");
			case "org.eclipse.pde.build":
				return required.add("org.apache.ant");
			case "org.eclipse.pde.core":
				return required.add("org.apache.ant");
			case "org.eclipse.osgi":
			case "org.eclipse.osgi.services":
				return required.add("IDE-compile-org.osgi.annotation.versioning");
			case "org.eclipse.pde.ds.ui":
				return required.addAll("org.eclipse.ui.workbench.texteditor", "org.eclipse.ui.views");
			case "org.eclipse.pde.ua.ui":
				return required.add("org.eclipse.ui.views");
			case "org.eclipse.swt":
				return required.add("org.eclipse.swt.win32.win32.x86_64");
			case "org.eclipse.team.ui":
				return required.add("org.eclipse.ui.workbench.texteditor");
			case "org.eclipse.ui.win32":
				return required.addAll(
					"com.ibm.icu",
					"org.eclipse.core.resources",
					"org.eclipse.equinox.common",
					"org.eclipse.equinox.registry",
					"org.eclipse.jface",
					"org.eclipse.swt",
					"org.eclipse.ui.workbench");
			case "org.eclipse.ui.cocoa":
				return required.addAll("org.eclipse.swt.cocoa.macosx.x86_64");
			case "org.apache.jasper.glassfish":
				return required.addAll(
					"org.apache.ant",
					"org.eclipse.jdt.core",
					"org.eclipse.equinox.common",
					"org.eclipse.core.contenttype",
					"org.eclipse.core.filesystem",
					"org.eclipse.core.jobs",
					"org.eclipse.core.resources",
					"org.eclipse.core.runtime",
					"org.eclipse.text");
			case "com.jcraft.jsch":
				return required.add("IDE-compile-com.jcraft.jzlib");
			default:
				return required;
		}
	}
	private static String collectInclusions(List<Path> files, ArrayList<Path> usedFiles, Path src) {
		int count = src.getNameCount();
		List<Path> allFiles = files.filter(p -> p.startsWith(src) && p.getNameCount() > count).map(src::relativize);
		Set<String> all = gatherInclusionsFromFileList(allFiles);
		Set<String> used = gatherInclusionsFromFileList(usedFiles.toList());
		if(all.removeAll(used).isEmpty() || used.isEmpty()) {
			return "";
		}
		ArrayList<String> included = used.toArrayList().replaceAll(s -> s.replace('#', '?')).sort();
		if(included.contains("org/apache/batik/")) {
			included.remove("org/apache/batik/");
			included.add("org/apache/batik/Version.java");
		}
		return " including=\"" + String.join("|", included) + "\"";
	}
	private static Set<String> gatherInclusionsFromFileList(List<Path> coveredFiles) {
		HashSet<String> included =
			coveredFiles.filter(p -> p.getNameCount() == 1).map(p -> p.getFileName().toString()).toHashSet();
		included.addAll(coveredFiles.filter(p -> p.getNameCount() > 1).map(
			p -> toUnixPath(p.subpath(0, p.getNameCount() - 1)) + "/*").toSet());
		return included.toSet();
	}
	private static String collectExclusions(
		List<Pair<Path, Path>> srcMappings,
		Path src,
		List<Path> locallyAvailableFiles,
		List<Path> libs) throws IOException {
		locallyAvailableFiles = locallyAvailableFiles.replaceAll(Main::flattenOne);
		libs = libs.filter(p -> p.endsWith(src.getName(0)));
		Path lib = libs.size() == 1 ? libs.get(0) : null;
		if(libs.isEmpty() && src.startsWith("src") == false && src.startsWith("res") == false) {
			throw new IllegalStateException("libs became empty");
		} else if(libs.notEmpty() && lib == null) {
			throw new IllegalStateException("libs contains " + libs);
		}
		List<Path> srcs = srcMappings.map(Pair::lhs);
		int srcNameCount = src.getNameCount();
		List<Path> list = srcs.removeIf(p -> p.getNameCount() <= srcNameCount);
		List<Path> filter = list.filter(p -> p.startsWith(src));
		List<String> excluded = filter.map(p -> toUnixPath(p.subpath(srcNameCount, p.getNameCount())) + "/");
		excluded = addCustomExclusions(src, excluded).removeIf(s -> s.contains("x-miss-x"));
		ArrayList<String> excluded2 = new ArrayList<>();
		if(lib != null) {
			for(Path local : locallyAvailableFiles) {
				if(Files.isRegularFile(lib.getParent().resolve(src).resolve(local))) {
					excluded2.add(toUnixPath(local));
					continue;
				}
			}
			List<String> commonExclusions = excluded.filter(s -> testExclusion(src, lib, excluded2, s));
			if(commonExclusions.notEmpty()) {
				System.out.println("Unnecessary exclusion " + src + ": " + commonExclusions);
			}
		}
		Stream<String> stream = excluded.stream();
		List<String> overlappingExclusions = excluded2.toList().filter(s -> stream.anyMatch(s2 -> s.startsWith(s2)));
		if(overlappingExclusions.notEmpty()) {
			System.out.println("Overlapping exclusion " + src + ": " + overlappingExclusions);
		}
		excluded = excluded.addAll(excluded2);
		if(excluded.isEmpty()) {
			return "";
		}
		return " excluding=\"" + String.join("|", excluded) + "\"";
	}
	private static boolean testExclusion(Path src, Path lib, ArrayList<String> excluded2, String exclusion)
		throws IOException {
		if(excluded2.contains(exclusion)) {
			return true;
		}
		if(exclusion.contains("*")) {
			if(exclusion.equals(".*")) {
				Path path = lib.getParent().resolve(src);
				return Files.list(path).noneMatch(
					p -> Files.isRegularFile(p) && p.getFileName().toString().startsWith("."));
			} else if(exclusion.equals(".*/")) {
				Path path = lib.getParent().resolve(src);
				return Files.list(path).noneMatch(
					p -> Files.isDirectory(p) && p.getFileName().toString().startsWith("."));
			}
			return false;
		}
		Path path = lib.getParent().resolve(src).resolve(exclusion);
		return exclusion.endsWith("/") ? Files.isDirectory(path) == false : Files.isRegularFile(path) == false;
	}
	private static Path flattenOne(Path p) {
		return p.subpath(1, p.getNameCount());
	}
	private static List<String> addCustomExclusions(Path src, List<String> excluded) {
		String unixPath = toUnixPath(src);
		switch(unixPath) {
			case "com.ibm.icu":
			case "com.jcraft.jsch":
			case "com.sun.el":
			case "javax.annotation":
			case "javax.el":
			case "javax.servlet":
			case "javax.servlet.jsp":
			case "org.hamcrest.core":
				return excluded.addAll("META-INF/ECLIPSE_.RSA", "META-INF/ECLIPSE_.SF", "META-INF/eclipse.inf");
			case "javax.inject":
			case "org.w3c.css.sac":
			case "org.w3c.dom.events":
			case "org.w3c.dom.smil":
			case "org.w3c.dom.svg":
				return excluded.addAll("META-INF/ECLIPSEF.RSA", "META-INF/ECLIPSEF.SF", "META-INF/eclipse.inf");
			case "external/src":
				return excluded.addAll("org/apache/xmlcommons/");
			case "org.apache.jasper.glassfish":
				return excluded.addAll(
					"META-INF/ECLIPSE_.RSA",
					"META-INF/ECLIPSE_.SF",
					"META-INF/eclipse.inf",
					"org/apache/jasper/runtime/PerThreadTagHandlerPool.java");
			case "org.eclipse.ant.ui":
				return excluded.addAll(".*", ".*/", "Ant Editor Content Assist Dev/");
			case "org.eclipse.core.net":
				return excluded.addAll(".*", ".*/", "fragments/");
			case "org.eclipse.jdt.core":
				return excluded.addAll(".*", ".*/", "scripts/");
			case "org.eclipse.jdt.launching":
				return excluded.addAll(".*", ".*/", "lib/");
			case "org.eclipse.jdt.ui":
				return excluded.addAll(".*", ".*/", "jar in jar loader/");
			case "org.eclipse.swt.win32.win32.x86_64":
			case "org.eclipse.swt.cocoa.macosx.x86_64":
			case "org.eclipse.swt.gtk.linux.x86_64":
				return excluded.addAll(".*");
			case "org.eclipse.pde.ui.templates":
				return excluded.addAll(
					".*",
					".*/",
					"templates_3.0/",
					"templates_3.1/",
					"templates_3.3/",
					"templates_3.5/");
			case "org.sat4j.core":
			case "org.sat4j.pb":
				return excluded.addAll(".*", ".*/", "src/test/");
			case "src":
			case "res":
				return excluded;
			default:
				return unixPath.contains("/") ? excluded : excluded.addAll(".*", ".*/");
		}
	}
	private static String combinableUnixPath(Path path) {
		String relative = toUnixPath(path);
		if(relative.length() > 0) {
			return "/" + relative;
		}
		return relative;
	}
	private static HashMap<Pair<Path, Path>, ArrayList<Path>> mapAvailableToFiles(
		List<Path> available,
		List<Path> target) {
		HashMap<Path, ArrayList<Path>> map = available.stream().toMap(Path::getFileName);
		Stream<Pair<Pair<Path, Path>, Path>> stream =
			target.stream().map(p -> splitLongest(p, map.get(p.getFileName()))).filter(p -> p != null);
		HashMap<Pair<Path, Path>, ArrayList<Path>> multiMap = stream.toMultiMap(Pair::lhs, Pair::rhs);
		//		Map<Path, Pair<Path, List<Path>>> map2 =
		//			multiMap.entrySet().stream().toMap(p -> p.lhs.lhs, p -> new Pair<>(p.lhs.rhs, p.rhs.toList())).toMap();
		return multiMap;
	}
	private static Pair<Pair<Path, Path>, Path> splitLongest(Path target, ArrayList<Path> available) {
		switch(target.getFileName().toString()) {
			case "ECLIPSE_.RSA":
			case "ECLIPSE_.SF":
			case "ECLIPSEF.RSA":
			case "ECLIPSEF.SF":
			case "eclipse.inf":
			case ".api_description":
				return null;
		}
		if(available != null) {
			Comparator<Path> comparator =
				Comparator.comparing(p -> p.toString().contains("x-miss-x") ? Integer.MAX_VALUE : p.getNameCount());
			for(int i = 1, n = target.getNameCount(); i < n; i++) {
				Path subpath = target.subpath(i, n);
				Stream<Path> stream = available.stream().filter(p -> p.endsWith(subpath)).sorted(comparator);
				ArrayList<Path> sorted = stream.toList();
				if(sorted.notEmpty()) {
					Path found = sorted.get(0);
					int endIndex = found.getNameCount() - subpath.getNameCount();
					Path src = endIndex == 0 ? Paths.get("") : found.subpath(0, endIndex);
					Path dst = target.subpath(0, i);
					return new Pair<>(new Pair<>(src, dst), subpath);
				}
			}
		}
		return null;
	}
	private static List<Path> listLocallyAvailableFiles(Path root) throws IOException {
		ArrayList<Path> list = listSourceCode(root.resolve("src"));
		list.addAll(listSourceCode(root.resolve("res")));
		list.removeIf(p -> p.getNameCount() == 1);
		return list.toList();
	}
	private static List<Path> deepListAvailableFiles(
		List<Path> localFiles,
		List<Path> libs,
		List<Path> pluginFiles,
		String plugin,
		Path project) throws IOException {
		ArrayList<Path> list = localFiles.toArrayList();
		if(Files.isRegularFile(project.resolve(".project.override"))) {
			list.addAll(Files.readAllLines(project.resolve(".project.override")).map(Paths::get));
		}
		for(Path lib : libs) {
			list.addAll(filterLibrary(listSourceCode(lib), plugin));
		}
		List<Path> missed = pluginFiles.map(p -> missedFileCatcher(p));
		list.addAll(missed);
		return list.toList();
	}
	private static ArrayList<Path> listSourceCode(Path src) throws IOException {
		if(Files.isDirectory(src) == false) {
			return new ArrayList<>();
		}
		Path srcParent = src.getParent();
		return Files.walk(src).filter(Files::isRegularFile).map(Main::canonicalizeFileNameToMatch).map(
			srcParent::relativize).toList();
	}
	private static Path missedFileCatcher(Path p) {
		int len = p.getNameCount();
		int off = Math.max(1, len - 3);
		Path subpath1 = p.subpath(0, off);
		Path subpath2 = p.subpath(off, len);
		return subpath1.resolve("x-miss-x").resolve(subpath2);
	}
	private static List<Path> sourceFilesForPlugin(Plugin plugin) {
		HashMap<Path, byte[]> pluginContent = plugin.contents;
		Stream<Path> stream = pluginContent.keySet().stream().sorted();
		ArrayList<Path> list =
			stream.filter(p -> removesInnerClasses(p, pluginContent.get(p))).map(Main::canonicalizeFileNameToMatch).toList();
		switch(plugin.id) {
			case "com.sun.el":
				return list.replaceAll(p -> convertComSunToOrgApache(p)).remove(
					Paths.get("com.sun.el.jar/org/apache/el/parser/AstMethodArguments.java")).add(
					Paths.get("com.sun.el.jar/org/apache/el/stream/Optional.java")).toList();
			case "org.apache.ant":
				return list.removeIf(p -> p.startsWith("org.apache.ant/etc")).toList();
			case "org.apache.batik.css":
				return list.add(
					Paths.get("org.apache.batik.css.jar/org/apache/batik/css/engine/value/svg12/AbstractCIEColor.java")).toList();
			case "org.apache.httpcomponents.httpclient":
				return list.addAll(
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/client/config/CookieSpecs.java"),
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/conn/socket/ConnectionSocketFactory.java"),
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/impl/execchain/BackoffStrategyExec.java"),
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/osgi/impl/OSGiClientBuilderFactory.java"),
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/osgi/services/HttpClientBuilderFactory.java")).toList();
			case "org.apache.httpcomponents.httpcore":
				return list.addAll(
					Paths.get("org.apache.httpcomponents.httpcore.jar/org/apache/http/config/Registry.java")).toList();
			case "org.apache.jasper.glassfish":
				return list.remove(
					Paths.get("org.apache.jasper.glassfish.jar/org/eclipse/jdt/internal/compiler/flow/NullInfoRegistry.java")).remove(
					Paths.get("org.apache.jasper.glassfish.jar/org/eclipse/jdt/internal/compiler/parser/readableNames.properties")).addAll(
					Paths.get("org.apache.jasper.glassfish.jar/org/apache/jasper/util/SystemLogHandler.java"),
					Paths.get("org.apache.jasper.glassfish.jar/org/eclipse/jdt/internal/compiler/parser/readableNames.props")).toList();
			case "org.eclipse.jetty.io":
				return list.add(Paths.get("org.eclipse.jetty.io.jar/org/eclipse/jetty/io/ssl/SslConnection.java")).toList();
			case "org.eclipse.jetty.util":
				return list.add(
					Paths.get("org.eclipse.jetty.util.jar/org/eclipse/jetty/util/annotation/ManagedAttribute.java")).toList();
			case "org.eclipse.jdt.doc.isv":
				return list.removeIf(
					p -> p.startsWith("org.eclipse.jdt.doc.isv.jar/index") ||
					p.startsWith("org.eclipse.jdt.doc.isv.jar/reference")).toList();
			case "org.eclipse.jdt.doc.user":
				return list.removeIf(p -> p.startsWith("org.eclipse.jdt.doc.user.jar/index")).toList();
			case "org.eclipse.help.webapp":
				return list.removeIf(p -> p.getFileName().toString().endsWith("_jsp.java")).toList();
			case "org.eclipse.pde.doc.user":
				return list.removeIf(
					p -> p.startsWith("org.eclipse.pde.doc.user.jar/index") ||
					p.startsWith("org.eclipse.pde.doc.user.jar/reference") ||
					p.startsWith("org.eclipse.pde.doc.user.jar/whatsNew")).toList();
			case "org.eclipse.platform.doc.isv":
				return list.removeIf(
					p -> p.startsWith("org.eclipse.platform.doc.isv.jar/index") ||
					p.startsWith("org.eclipse.platform.doc.isv.jar/reference") ||
					p.startsWith("org.eclipse.platform.doc.isv.jar/samples")).toList();
			case "org.eclipse.platform.doc.user":
				return list.removeIf(
					p -> p.startsWith("org.eclipse.platform.doc.user.jar/index") ||
					p.startsWith("org.eclipse.platform.doc.user.jar/whatsNew")).toList();
			case "org.eclipse.swt.win32.win32.x86_64":
				return list.addAll(
					Paths.get("org.eclipse.swt.win32.win32.x86_64.jar/org/eclipse/swt/browser/Mozilla.java"),
					Paths.get("org.eclipse.swt.win32.win32.x86_64.jar/org/eclipse/swt/browser/WebKit.java"),
					Paths.get("org.eclipse.swt.win32.win32.x86_64.jar/org/eclipse/swt/browser/Website.java"),
					Paths.get("org.eclipse.swt.win32.win32.x86_64.jar/org/eclipse/swt/browser/MozillaDelegate.java")).toList();
			case "org.sat4j.core":
				return list.remove(Paths.get("org.sat4j.core.jar/org/sat4j/minisat/core/Constr.java")).remove(
					Paths.get("org.sat4j.core.jar/org/sat4j/minisat/core/Propagatable.java")).remove(
					Paths.get("org.sat4j.core.jar/org/sat4j/MoreThanSAT.java")).toList();
			case "org.sat4j.pb":
				return list.remove(Paths.get("org.sat4j.pb.jar/org/sat4j/pb/PseudoBitsAdderDecorator.java")).addAll(
					Paths.get("org.sat4j.pb.jar/org/sat4j/pb/multiobjective/IMultiObjOptimizationProblem.java")).toList();
			default:
				return list.toList();
		}
	}
	private static Path convertComSunToOrgApache(Path p) {
		return Paths.get(toUnixPath(p).replace("com/sun/", "org/apache/"));
	}
	private static ArrayList<Path> filterLibrary(ArrayList<Path> list, String plugin) {
		switch(plugin) {
			case "com.ibm.icu":
				return list.filter(p -> p.startsWith("com.ibm.icu/com"));
			case "org.apache.batik.css":
				return list.remove(Paths.get("apache.batik/sources/org/apache/batik/css/engine/value/svg12/AbstractCIEColor.java"));
			case "org.apache.batik.util.gui":
				return list.remove(Paths.get("apache.batik/sources/org/apache/batik/util/gui/xmleditor/XMLScanner.java"));
			case "org.eclipse.ecf.identity":
				return list.remove(Paths.get("org.eclipse.ecf.identity/src/org/eclipse/ecf/core/identity/URIID.java"));
			case "org.eclipse.equinox.launcher.carbon.macosx":
			case "org.eclipse.equinox.launcher.cocoa.macosx":
			case "org.eclipse.equinox.launcher.cocoa.macosx.x86_64":
			case "org.eclipse.equinox.launcher.gtk.aix.ppc":
			case "org.eclipse.equinox.launcher.gtk.aix.ppc64":
			case "org.eclipse.equinox.launcher.gtk.hpux.ia64":
			case "org.eclipse.equinox.launcher.gtk.hpux.ia64_32":
			case "org.eclipse.equinox.launcher.gtk.linux.ppc":
			case "org.eclipse.equinox.launcher.gtk.linux.ppc64":
			case "org.eclipse.equinox.launcher.gtk.linux.s390":
			case "org.eclipse.equinox.launcher.gtk.linux.s390x":
			case "org.eclipse.equinox.launcher.gtk.linux.x86":
			case "org.eclipse.equinox.launcher.gtk.linux.x86_64":
			case "org.eclipse.equinox.launcher.gtk.solaris.sparc":
			case "org.eclipse.equinox.launcher.gtk.solaris.x86":
			case "org.eclipse.equinox.launcher.motif.aix.ppc":
			case "org.eclipse.equinox.launcher.motif.hpux.ia64_32":
			case "org.eclipse.equinox.launcher.motif.hpux.PA_RISC":
			case "org.eclipse.equinox.launcher.motif.linux.x86":
			case "org.eclipse.equinox.launcher.motif.solaris.sparc":
			case "org.eclipse.equinox.launcher.win32.win32.ia64":
			case "org.eclipse.equinox.launcher.win32.win32.x86":
			case "org.eclipse.equinox.launcher.win32.win32.x86_64":
			case "org.eclipse.equinox.launcher.wpf.win32.x86":
				return list.filter(p -> pathContains(p, plugin));
			case "org.eclipse.jetty.server":
				return list.remove(Paths.get("jetty-server/src/main/java/org/eclipse/jetty/server/handler/ContextHandler.java"));
			case "org.eclipse.jgit":
				return list.remove(Paths.get("org.eclipse.jgit/src/org/eclipse/jgit/internal/storage/file/BitmapIndexImpl.java"));
			case "org.eclipse.swt.win32.win32.x86_64":
				return list.removeIf(Main::notForWindows);
			case "org.eclipse.swt.cocoa.macosx.x86_64":
				return list.removeIf(Main::notForMacOSX);
			case "org.eclipse.swt.gtk.linux.x86_64":
				return list.removeIf(Main::notForLinux);
			case "org.junit":
				return list.filter(p -> p.startsWith("org.junit/junitsrc"));
			case "com.jcraft.jsch":
			case "com.sun.el":
			case "javax.annotation":
			case "javax.el":
			case "javax.inject":
			case "javax.servlet":
			case "javax.servlet.jsp":
			case "org.apache.jasper.glassfish":
			case "org.hamcrest.core":
			case "org.w3c.css.sac":
			case "org.w3c.dom.events":
			case "org.w3c.dom.smil":
			case "org.w3c.dom.svg":
				return list.removeIf(Main::orbitSourceIdentifier);
			default:
				return list;
		}
	}
	private static boolean orbitSourceIdentifier(Path p) {
		if(p.getNameCount() > 1) {
			switch(p.getName(1).toString()) {
				case "about_files":
				case "META-INF":
				case "about.html":
				case "plugin.properties":
					return true;
			}
		}
		return false;
	}
	private static boolean pathContains(Path path, String part) {
		for(Path p : path) {
			if(p.startsWith(part)) {
				return true;
			}
		}
		return false;
	}
	private static boolean notForWindows(Path p) {
		for(Path path : p) {
			switch(path.toString()) {
				case "cairo":
				case "carbon":
				case "cocoa":
				case "common_j2me":
				case "emulated":
				case "gtk":
				case "motif":
				case "photon":
				case "wpf":
					return true;
			}
		}
		return false;
	}
	private static boolean notForMacOSX(Path p) {
		for(Path path : p) {
			switch(path.toString()) {
				case "cairo":
				case "carbon":
				case "common_j2me":
				case "gtk":
				case "motif":
				case "photon":
				case "win32":
				case "wpf":
					return true;
			}
		}
		return false;
	}
	private static boolean notForLinux(Path p) {
		boolean afterEmulated = false;
		for(Path path : p) {
			switch(path.toString()) {
				case "carbon":
				case "cocoa":
				case "common_j2me":
				case "motif":
				case "photon":
				case "win32":
				case "wpf":
					return true;
				case "emulated":
					afterEmulated = true;
					break;
				case "org":
				case "bidi":
				case "coolbar":
				case "taskbar":
					if(afterEmulated) {
						return path.toString().equals("org");
					}
				default:
					afterEmulated = false;
			}
		}
		return false;
	}
	private static Path canonicalizeFileNameToMatch(Path p) {
		String file = p.getFileName().toString();
		if(file.endsWith(".class")) {
			return p.resolveSibling(file.substring(0, file.length() - 6) + ".java");
		} else if(file.endsWith(".dll") || file.endsWith(".so") || file.endsWith(".jnilib")) {
			return p.resolveSibling(file.replaceAll("[0-9]", "#"));
		}
		return p;
	}
	private static boolean removesInnerClasses(Path p, byte[] bytes) {
		String file = p.getFileName().toString();
		if((file.endsWith(".class") || file.endsWith(".java")) && file.contains("$")) {
			return false;
		}
		if(file.endsWith(".class")) {
			try {
				return (new ClassReader(bytes).getAccess() & Opcodes.ACC_PUBLIC) != 0;
			} catch(Exception e) {
				System.out.println("Invalid class " + file + ": " + e);
				return false;
			}
		}
		return true;
	}
	private static void writeDotProject(Path projectFolder, String projectName, List<Path> libs) throws IOException {
		if(Files.isRegularFile(projectFolder.resolve(".project.override"))) {
			return;
		}
		ArrayList<String> lines = new ArrayList<>();
		lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		lines.add("<projectDescription>");
		lines.add("	<name>" + projectName + "</name>");
		lines.add("	<comment></comment>");
		lines.add("	<projects>");
		lines.add("	</projects>");
		lines.add("	<buildSpec>");
		lines.add("		<buildCommand>");
		lines.add("			<name>org.eclipse.jdt.core.javabuilder</name>");
		lines.add("			<arguments>");
		lines.add("			</arguments>");
		lines.add("		</buildCommand>");
		lines.add("	</buildSpec>");
		lines.add("	<natures>");
		lines.add("		<nature>org.eclipse.jdt.core.javanature</nature>");
		lines.add("	</natures>");
		lines.add("	<linkedResources>");
		for(Path lib : libs) {
			lines.add("		<link>");
			lines.add("			<name>" + lib.getFileName() + "</name>");
			lines.add("			<type>2</type>");
			lines.add("			<locationURI>PARENT-2-PROJECT_LOC/" + toUnixPath(lib) + "</locationURI>");
			lines.add("		</link>");
		}
		lines.add("	</linkedResources>");
		lines.add("</projectDescription>");
		lines.add("");
		String dotProject = String.join("\n", lines);
		writeFile(projectFolder, dotProject, ".project");
	}
	private static void writeFile(Path projectFolder, String fileData, String fileName) throws IOException {
		byte[] bytes = fileData.getBytes(StandardCharsets.UTF_8);
		Path file = projectFolder.resolve(fileName);
		if(Files.notExists(file) || Arrays.equals(bytes, Files.readAllBytes(file)) == false) {
			Files.createDirectories(file.getParent());
			Files.write(file, bytes);
		}
	}
	private static String toUnixPath(Path path) {
		return String.join("/", Stream.from(path).map(Path::toString).iterable());
	}
	private static String projectName(String plugin, Path source) {
		return "IDE-" + moduleNameFromSourceLibrary(source) + "-" + plugin;
	}
	private static String moduleNameFromSourceLibrary(Path pluginRoot) {
		String string = pluginRoot.getName(1).toString();
		if(string.contains("eclipse")) {
			return string;
		}
		return "other";
	}
	private static Map<String, List<Path>> findSourceFolders(Path root, List<String> plugins) throws IOException {
		Path libraries = root.resolve("libraries");
		List<Path> folders = Files.walk(libraries, 5).filter(Files::isDirectory).toList().toList();
		return plugins.stream().toMap(p -> p, p -> libraryFolders(root, folders, p)).toMap();
	}
	private static void printRoots(Map<String, List<Path>> pluginRoots) {
		for(String name : sortedKeys(pluginRoots)) {
			List<Path> list = pluginRoots.get(name);
			System.out.printf("%-60s : %s%n", name, list);
		}
	}
	private static List<Path> libraryFolders(Path root, List<Path> folders, String pluginName) {
		switch(pluginName) {
			case "org.eclipse.equinox.launcher.carbon.macosx":
			case "org.eclipse.equinox.launcher.cocoa.macosx":
			case "org.eclipse.equinox.launcher.cocoa.macosx.x86_64":
			case "org.eclipse.equinox.launcher.gtk.aix.ppc":
			case "org.eclipse.equinox.launcher.gtk.aix.ppc64":
			case "org.eclipse.equinox.launcher.gtk.hpux.ia64":
			case "org.eclipse.equinox.launcher.gtk.hpux.ia64_32":
			case "org.eclipse.equinox.launcher.gtk.linux.ppc":
			case "org.eclipse.equinox.launcher.gtk.linux.ppc64":
			case "org.eclipse.equinox.launcher.gtk.linux.s390":
			case "org.eclipse.equinox.launcher.gtk.linux.s390x":
			case "org.eclipse.equinox.launcher.gtk.linux.x86":
			case "org.eclipse.equinox.launcher.gtk.linux.x86_64":
			case "org.eclipse.equinox.launcher.gtk.solaris.sparc":
			case "org.eclipse.equinox.launcher.gtk.solaris.x86":
			case "org.eclipse.equinox.launcher.motif.aix.ppc":
			case "org.eclipse.equinox.launcher.motif.hpux.ia64_32":
			case "org.eclipse.equinox.launcher.motif.hpux.PA_RISC":
			case "org.eclipse.equinox.launcher.motif.linux.x86":
			case "org.eclipse.equinox.launcher.motif.solaris.sparc":
			case "org.eclipse.equinox.launcher.win32.win32.ia64":
			case "org.eclipse.equinox.launcher.win32.win32.x86":
			case "org.eclipse.equinox.launcher.win32.win32.x86_64":
			case "org.eclipse.equinox.launcher.wpf.win32.x86":
				return List.of(
					Paths.get("libraries/eclipse.rt.equinox.binaries"),
					Paths.get("libraries/eclipse.rt.equinox.framework/bundles"));
		}
		Path pluginPath = pluginNameToPath(pluginName);
		List<Path> libraries = sourceInLibraries(root, folders, pluginPath);
		switch(pluginName) {
			case "org.apache.jasper.glassfish":
				return libraries.add(Paths.get("libraries/eclipse.jdt.core"));
			case "org.eclipse.swt.win32.win32.x86_64":
			case "org.eclipse.swt.cocoa.macosx.x86_64":
			case "org.eclipse.swt.gtk.linux.x86_64":
				return libraries.add(Paths.get("libraries/eclipse.platform.swt/bundles/org.eclipse.swt"));
			case "org.apache.felix.gogo.command":
				return libraries.add(Paths.get("libraries/eclipse.rt.equinox.framework/bundles/org.eclipse.osgi.services"));
			default:
				return libraries;
		}
	}
	private static Path pluginNameToPath(String pluginName) {
		switch(pluginName) {
			case "com.sun.el":
			case "javax.annotation":
			case "javax.el":
			case "javax.servlet":
			case "javax.servlet.jsp":
				return Paths.get("apache.tomcat");
			case "org.apache.ant":
				return Paths.get("apache.ant");
			case "org.apache.batik.css":
			case "org.apache.batik.util":
			case "org.apache.batik.util.gui":
				return Paths.get("apache.batik");
			case "org.apache.commons.codec":
			case "org.apache.commons.compress":
			case "org.apache.commons.logging":
				return Paths.get(pluginName.replace("org.apache.commons.", "apache.commons-"));
			case "org.apache.felix.gogo.command":
			case "org.apache.felix.gogo.runtime":
			case "org.apache.felix.gogo.shell":
				return Paths.get("gogo", pluginName.replace("org.apache.felix.gogo.", ""));
			case "org.apache.httpcomponents.httpclient":
			case "org.apache.httpcomponents.httpcore":
				return Paths.get(pluginName.replace("org.apache.httpcomponents.", "apache."));
			case "org.apache.lucene.analysis":
			case "org.apache.lucene.core":
				return Paths.get("lucene");
			case "javax.xml":
				return Paths.get("apache.xerces.xml-commons", "java", "external");
			case "org.eclipse.equinox.http.jetty":
				return Paths.get("org.eclipse.equinox.http.jetty9");
			case "org.eclipse.jetty.continuation":
			case "org.eclipse.jetty.http":
			case "org.eclipse.jetty.io":
			case "org.eclipse.jetty.security":
			case "org.eclipse.jetty.server":
			case "org.eclipse.jetty.servlet":
			case "org.eclipse.jetty.util":
				return Paths.get(pluginName.replace("org.eclipse.jetty.", "jetty-"));
			case "org.objectweb.asm":
			case "org.objectweb.asm.tree":
				return Paths.get("ow2.asm", "trunk", "asm");
			default:
				return Paths.get(pluginName);
		}
	}
	private static List<Path> sourceInLibraries(Path root, List<Path> folders, Path pluginName) {
		Stream<Path> stream = folders.stream().filter(path -> path.endsWith(pluginName));
		Stream<Path> sorted = stream.sorted(Comparator.comparing(path -> path.getNameCount()));
		return sorted.limit(1).toList().map(path -> root.relativize(path)).toList();
	}
	private static Map<String, List<String>> combineDependencies(List<Plugin> plugins) {
		HashMap<String, List<String>> depending = gatherNames(plugins, "Require-Bundle");
		addFromFragmentHosts(plugins, depending);
		HashMap<String, List<String>> importers = gatherNames(plugins, "Import-Package");
		importers.put(
			"org.apache.httpcomponents.httpclient",
			importers.get("org.apache.httpcomponents.httpclient").addAll("org.osgi.framework", "org.osgi.service.cm"));
		HashMap<String, List<String>> exported = gatherNames(plugins, "Export-Package");
		exported.keySet().remove("org.apache.felix.gogo.command");
		HashMap<String, List<String>> exporters = invert(exported);
		for(String bundle : importers.keySet().toArrayList().sort()) {
			HashSet<String> bundleDependencies = new HashSet<>();
			if(depending.containsKey(bundle)) {
				bundleDependencies.addAll(depending.get(bundle));
			}
			for(String module : importers.get(bundle)) {
				List<String> moduleExporters = exporters.computeIfAbsent(module, k -> List.of()).get(module);
				bundleDependencies.addAll(moduleExporters);
			}
			bundleDependencies.remove(bundle);
			depending.put(bundle, bundleDependencies.toArrayList().sort().toList());
		}
		return depending.toMap();
	}
	private static void addFromFragmentHosts(List<Plugin> plugins, HashMap<String, List<String>> depending) {
		HashMap<String, List<String>> hosts = gatherNames(plugins, "Fragment-Host");
		for(Pair<String, List<String>> pair : hosts.entrySet()) {
			depending.compute(pair.lhs, (k, v) -> v == null ? pair.rhs : v.addAll(pair.rhs));
		}
	}
	private static Map<String, List<String>> reexportedDependencies(List<Plugin> plugins) {
		HashMap<String, List<String>> map = gatherNamesWith(plugins, "Require-Bundle", "visibility:=reexport");
		map.put("org.eclipse.swt", List.of("org.eclipse.swt.win32.win32.x86_64"));
		return map.toMap();
	}
	private static void printDependencies(Map<String, List<String>> deps, String arrow) {
		for(String name : sortedKeys(deps)) {
			List<String> list = deps.get(name);
			System.out.printf("%-60s %s %s%n", name, arrow, list);
		}
	}
	private static <T extends Comparable<T>> ArrayList<T> sortedKeys(Map<T, ?> deps) {
		return deps.keySet().stream().sorted().toList();
	}
	private static HashMap<String, List<String>> gatherNames(List<Plugin> plugins, String key) {
		return gatherNamesWith(plugins, key, "");
	}
	private static HashMap<String, List<String>> gatherNamesWith(List<Plugin> plugins, String key, String tag) {
		return plugins.stream().toMultiMap(p -> p.id, p -> nameList(key, tag, p), m -> m);
	}
	private static List<String> nameList(String key, String tag, ArrayList<Plugin> p) {
		String required = p.sort().get(-1).manifest.get(key);
		if(required == null) {
			return List.of();
		}
		String[] split = required.replaceAll("\"[^\"]+\"", "\"\"").split(",");
		return Stream.of(split).filter(s -> s.contains(tag)).map(Main::toBundleName).toList().toList();
	}
	private static <A, B> HashMap<B, List<A>> invert(HashMap<A, List<B>> dag) {
		Collector<A, ArrayList<A>, ArrayList<A>> toArrayList = Collectors.toList();
		Collector<A, ArrayList<A>, List<A>> toList = Collectors.collectingAndThen(toArrayList, ArrayList::toList);
		Collector<Pair<A, B>, ?, List<A>> mapping = Collectors.mapping(Pair<A, B>::lhs, toList);
		Collector<Pair<A, B>, ?, HashMap<B, List<A>>> groupingBy = Collectors.groupingBy(Pair<A, B>::rhs, mapping);
		return graphToEdges(dag).collect(groupingBy);
	}
	private static <A, B> Map<B, List<A>> invert(Map<A, List<B>> dag) {
		Collector<A, ArrayList<A>, ArrayList<A>> toArrayList = Collectors.toList();
		Collector<A, ArrayList<A>, List<A>> toList = Collectors.collectingAndThen(toArrayList, ArrayList::toList);
		Collector<Pair<A, B>, ?, List<A>> mapping = Collectors.mapping(Pair<A, B>::lhs, toList);
		Collector<Pair<A, B>, ?, HashMap<B, List<A>>> groupingBy = Collectors.groupingBy(Pair<A, B>::rhs, mapping);
		return graphToEdges(dag).collect(groupingBy).toMap();
	}
	private static <A, B> Stream<Pair<A, B>> graphToEdges(Map<A, List<B>> dag) {
		Stream<Pair<A, List<B>>> stream = dag.entrySet().stream();
		return stream.flatMap(p -> p.rhs.stream().map(p::keepingLhs));
	}
	private static <A, B> Stream<Pair<A, B>> graphToEdges(HashMap<A, List<B>> dag) {
		Stream<Entry<A, List<B>>> stream = dag.entrySet().stream();
		return stream.flatMap(p -> p.rhs.stream().map(p::keepingLhs));
	}
	private static List<Plugin> readPluginContents(Path pluginsFolder) throws IOException {
		Path binaryInclusions = Paths.get("").toAbsolutePath().resolveSibling("IDE-all-extras/extras/plugins");
		HashSet<Path> skipped = HashSet.of();
		if(Files.isDirectory(binaryInclusions)) {
			ArrayList<Path> list =
				Files.list(binaryInclusions).map(p -> pluginsFolder.resolve(binaryInclusions.relativize(p))).toList();
			skipped.addAll(list);
		}
		IOStream<Path> stream = Files.list(pluginsFolder).filter(path -> filterExtraPluginsEntries(path, skipped));
		IOStream<Pair<Path, Map<Path, byte[]>>> stream3 = stream.map(Main::readContents).filter(Main::hasManifest);
		IOStream<Plugin> stream4 = stream3.map(p -> new Plugin(p.lhs, p.rhs.toHashMap()));
		List<Plugin> list =
			stream4.toMultiMap(Plugin::name, l -> l.sort().get(-1), m -> m.values().toArrayList().sort().toList());
		return list;
	}
	private static boolean filterExtraPluginsEntries(Path path, HashSet<Path> skipped) {
		return !skipped.contains(path) && !path.toString().contains(".source_");
	}
	private static void printManifestInfo(Path pluginsFolder, Map<String, List<String>> depending, Plugin plugin) {
		Path relativePath = pluginsFolder.relativize(plugin.root);
		List<String> dependencies = depending.get(plugin.id);
		System.out.printf("%80s: %60s -> %s%n", relativePath, plugin.id, dependencies);
	}
	private static String toBundleName(String symbolicName) {
		return symbolicName.split(";")[0].trim();
	}
	private static boolean hasManifest(Pair<Path, Map<Path, byte[]>> pathAndContents) {
		return pathAndContents.rhs.containsKey(MANIFEST_PATH);
	}
	private static Pair<Path, Map<Path, byte[]>> readContents(Path fileOrFolder) throws IOException {
		boolean isFolder = Files.isDirectory(fileOrFolder);
		Map<Path, byte[]> contents = isFolder ? readFolder(fileOrFolder) : readFile(fileOrFolder);
		return new Pair<>(fileOrFolder, contents);
	}
	private static Map<Path, byte[]> readFolder(Path folder) throws IOException {
		IOStream<Path> stream = Files.walk(folder).filter(path -> !Files.isDirectory(path));
		return stream.toMap(folder::relativize, Files::readAllBytes).toMap();
	}
	private static Map<Path, byte[]> readFile(Path file) throws IOException {
		if(!file.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
			return Map.of();
		}
		byte[] readAllBytes = Files.readAllBytes(file);
		return readZip(readAllBytes);
	}
	private static Map<Path, byte[]> readZip(byte[] bytes) throws IOException {
		HashMap<Path, byte[]> contents = new HashMap<>();
		try(ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));) {
			ZipEntry entry;
			while((entry = zip.getNextEntry()) != null) {
				if(!entry.isDirectory()) {
					Path path = Paths.get(entry.getName());
					bytes = Streams.readAllBytes(zip);
					contents.put(path, bytes);
				}
			}
		}
		return contents.toMap();
	}
}
