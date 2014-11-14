package project.generator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import utils.lists.ReadOnlyList;
import utils.lists.Set;
import utils.streams2.Collectors;
import utils.streams2.IOStream;
import utils.streams2.Stream;
import utils.streams2.Streams;

public class RegenerateProjectsMain {
	private static final Path MANIFEST_PATH = Paths.get(JarFile.MANIFEST_NAME);
	private static final Path PLUGINS_FOLDER = Paths.get(System.getProperty("user.home", ""), "evening/plugins");
	private static final Set<Path> CLASS_FILE_INDICATORS = Set.of(
		Paths.get("target"),
		Paths.get("bin"),
		Paths.get("classes"));
	private static HashMap<Pair<String, String>, Integer> cachedDistances = new HashMap<>();

	public static void main(String[] args) throws IOException {
		Path root = Paths.get("").toAbsolutePath().getParent().getParent();
		ArrayList<Plugin> plugins = readModelFiles();
		ArrayList<String> pluginNames = plugins.map(Plugin::name).sort();
		Map<String, List<Path>> sourceFolders = findSourceFolders(root, pluginNames);
		printRoots(sourceFolders);
		generateDotProjects(root, plugins, sourceFolders);
		generateDotClasspaths(root, plugins, sourceFolders);
	}
	private static void generateDotClasspaths(
		Path root,
		ReadOnlyList<Plugin> plugins,
		Map<String, List<Path>> sourceFolders) throws IOException {
		Map<String, List<String>> depending = combineDependencies(plugins);
		Map<String, List<String>> exported = reexportedDependencies(plugins);
		for(Plugin plugin : plugins) {
			printManifestInfo(PLUGINS_FOLDER, depending, plugin);
		}
		Map<String, List<String>> dependents = invert(depending).remove("system.bundle");
		Map<String, List<String>> depended = invert(dependents);
		printDependencies(depended, "->");
		printDependencies(dependents, "<-");
		generateClasspaths(root, depended, exported, plugins, sourceFolders);
	}
	private static ArrayList<Plugin> readModelFiles() throws IOException {
		ArrayList<Plugin> plugins = readPluginContents(PLUGINS_FOLDER);
		for(Plugin plugin : plugins) {
			Stream<Pair<Path, byte[]>> stream = plugin.contents.entrySet().map(Entry::toPair).stream();
			IOStream<Pair<Path, byte[]>> expanded = stream.toIO().flatMap(RegenerateProjectsMain::deepExpand);
			Path bundle = Paths.get(plugin.id + plugin.shape);
			HashMap<Path, byte[]> hashMap = expanded.toMap(p -> bundle.resolve(p.lhs), Pair::rhs);
			plugin.contents.clear().putAll(hashMap);
		}
		return plugins;
	}
	private static IOStream<Pair<Path, byte[]>> deepExpand(Pair<Path, byte[]> content) throws IOException {
		String name = content.lhs.getFileName().toString();
		IOStream<Pair<Path, byte[]>> ioStream = Stream.of(content).toIO();
		if(name.endsWith(".jar")) {
			IOStream<Pair<Path, byte[]>> io = readZip(content.rhs).entrySet().stream().toIO();
			IOStream<Pair<Path, byte[]>> expanded = io.flatMap(RegenerateProjectsMain::deepExpand);
			IOStream<Pair<Path, byte[]>> ioStream2 = expanded.map(p -> p.keepingRhs(content.lhs.resolve(p.lhs)));
			return ioStream.concat(ioStream2);
		}
		return ioStream;
	}
	private static void generateClasspaths(
		Path root,
		Map<String, List<String>> depending,
		Map<String, List<String>> exported,
		ReadOnlyList<Plugin> plugins,
		Map<String, List<Path>> sourceFolders) throws IOException {
		HashSet<Path> projectFolders = HashSet.of();
		Path projects = root.resolve("projects");
		for(Plugin plugin : plugins) {
			String pluginName = plugin.id;
			String projectName = projectName(sourceFolders, pluginName);
			if(projectName == null) {
				System.out.println("--- Missing library --- " + pluginName);
				continue;
			}
			Path projectFolder = projects.resolve(projectName);
			Files.createDirectories(projectFolder);
			projectFolders.add(projectFolder);
			List<Path> sources = sourceFolders.get(pluginName);
			List<Path> libs = sources.replaceAll(p -> root.resolve(p));
			List<String> required = depending.getOrDefault(pluginName, List.of());
			Set<String> exports = exported.get(pluginName).toSet();
			writeDotClasspath(projectFolder, pluginName, libs, sourceFolders, required, exports, plugin);
		}
		printUnusedProjects(projectFolders, projects);
		printDependencyLoops(projects);
	}
	private static void
		generateDotProjects(Path root, ArrayList<Plugin> plugins, Map<String, List<Path>> sourceFolders)
			throws IOException {
		HashSet<Path> projectFolders = HashSet.of();
		Path projects = root.resolve("projects");
		for(int i = 0, n = plugins.size(); i < n; i++) {
			Plugin plugin = plugins.get(i);
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
			HashMap<String, String> manifest = findRealManifest(root, projectFolder, sources, plugin);
			plugins.set(i, plugin.manifest(manifest));
		}
	}
	private static HashMap<String, String> findRealManifest(
		Path root,
		Path projectFolder,
		List<Path> sources,
		Plugin plugin) throws IOException {
		Path manifestPath = projectFolder.resolve("res/META-INF/MANIFEST.MF");
		if(Files.isRegularFile(manifestPath)) {
			return Plugin.parseManifest(Files.readAllBytes(manifestPath));
		}
		for(Path source : sources) {
			manifestPath = root.resolve(source).resolve("META-INF/MANIFEST.MF");
			if(Files.isRegularFile(manifestPath)) {
				return Plugin.parseManifest(Files.readAllBytes(manifestPath));
			}
		}
		System.out.println("Real manifest not found for plugin: " + plugin.id);
		return plugin.manifest;
	}
	private static void printDependencyLoops(Path projects) throws IOException {
		Map<String, List<String>> deps;
		deps =
			Files.list(projects).map(p -> p.resolve(".classpath")).filter(Files::isRegularFile).map(
				RegenerateProjectsMain::readRequiredProjects).toMap(Pair::lhs, Pair::rhs).toMap();
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
		List<Path> pluginFiles = sourceFilesForPlugin(plugin).toList();
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
			classpath.add(files.stream().map(p -> p.toString().replace('\\', '/')).collect(
				Collectors.joining("\n\t     ", "\t<!-- ", " -->")));
			boolean commented = mapping.lhs.toString().contains("x-miss-x");
			String quoteBeg = commented ? "<!-- " : "";
			String quoteEnd = commented ? " -->" : "";
			String outputPath = combinableUnixPath(mapping.rhs);
			String outputPathParent = combinableUnixPath(mapping.rhs.getParent());
			if(commented == false && outputs.contains(outputPathParent) == false) {
				outputs.add(outputPathParent);
			}
			int sequenceNum = commented ? 0 : outputs.indexOf(outputPathParent) + 1;
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
		List<String> listReexportedProjects = listReexportedProjects(pluginName, exported);
		for(String name : listRequiredProjects(pluginName, depending)) {
			String projectName = customizeChangeProjectName(name);
			String path = projectPath(sourceFolders, projectName);
			if(path != null && !listReexportedProjects.contains(name)) {
				classpath.add("	<classpathentry kind=\"src\" path=\"" + path + "\"/>");
			}
		}
		for(String name : listReexportedProjects) {
			String projectName = customizeChangeProjectName(name);
			String path = projectPath(sourceFolders, projectName);
			if(path != null) {
				classpath.add("	<classpathentry exported=\"true\" kind=\"src\" path=\"" + path + "\"/>");
			}
		}
		for(String path : customizeAddRequiredClasspathLibs(pluginName)) {
			classpath.add("	<classpathentry kind=\"lib\" path=\"" + path + "\"/>");
		}
		for(String path : customizeAddReexportedLibsClasspath(pluginName)) {
			classpath.add("	<classpathentry exported=\"true\" kind=\"lib\" path=\"" + path + "\"/>");
		}
		classpath.add("	<classpathentry kind=\"output\" path=\"classes\"/>");
		classpath.add("</classpath>");
		classpath.add("");
		String dotClasspath = classpath.stream().collect(Collectors.joining("\n"));
		String fileName = ".classpath";
		writeFile(project, dotClasspath, fileName);
		cleanOldOutputs(project, outputs);
	}
	private static List<String> listReexportedProjects(String pluginName, Set<String> exported) {
		List<String> projects = customizeAddReexportedProjects(pluginName, exported.toList());
		return projects.sort();
	}
	private static List<String> listRequiredProjects(String pluginName, List<String> depending) {
		List<String> list = customizeAddRequiredProjects(pluginName, depending);
		List<String> projects = customizeRemoveRequiredProjects(pluginName, list);
		return projects.sort();
	}
	private static Map<String, List<String>> reexportedDependencies(ReadOnlyList<Plugin> plugins) {
		return gatherNamesWith(plugins, "Require-Bundle", "visibility:=reexport").toMap();
	}
	private static String projectPath(Map<String, List<Path>> sourceFolders, String plugin) {
		if(plugin.startsWith("lib")) {
			return plugin;
		}
		String projectName = projectName(sourceFolders, plugin);
		if(projectName == null) {
			return null;
		}
		return "/" + projectName;
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
		String name = source.getName(1).toString();
		return name.startsWith("eclipse.") ? "IDE-" + name + "-" + plugin : "IDE-other-" + plugin;
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
	private static String customizeChangeProjectName(String name) {
		switch(name) {
			case "org.eclipse.ecf.discovery":
				return "IDE-compile-eclipse.ecf";
			default:
				return name;
		}
	}
	private static List<String> customizeAddRequiredClasspathLibs(String plugin) {
		switch(plugin) {
			case "org.eclipse.jem.util":
				return List.of("org.eclipse.jem.util/org.eclipse.perfmsr.core.stub/perfmsr.jar");
			default:
				return List.of();
		}
	}
	private static List<String> customizeAddReexportedLibsClasspath(String plugin) {
		switch(plugin) {
			case "net.jeeeyul.eclipse.themes":
				return List.of(
					"lib/org.eclipse.xtend.lib.macro_2.7.1.v201409090713.jar",
					"lib/com.google.guava_15.0.0.v201403281430.jar",
					"lib/org.eclipse.xtend.lib_2.7.1.v201409090713.jar",
					"lib/org.eclipse.xtext.xbase.lib_2.7.1.v201409090713.jar");
			case "org.eclipse.m2e.maven.indexer":
				return List.of(
					"lib/indexer-artifact-3.1.0.jar",
					"lib/indexer-core-3.1.0.jar",
					"lib/lucene-core-2.4.1.jar",
					"lib/lucene-highlighter-2.4.1.jar");
			default:
				return List.of();
		}
	}
	private static List<String> customizeRemoveRequiredProjects(String plugin, List<String> required) {
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
	private static List<String> customizeAddReexportedProjects(String plugin, List<String> exported) {
		switch(plugin) {
			case "org.eclipse.swt":
				return exported.add("org.eclipse.swt.win32.win32.x86_64");
			case "org.eclipse.jdt.junit":
				return exported.add("org.eclipse.debug.ui");
			default:
				return exported;
		}
	}
	private static List<String> customizeAddRequiredProjects(String plugin, List<String> required) {
		switch(plugin) {
			case "com.google.gerrit.common":
				return required.addAll("com.google.guava", "IDE-compile-gwt");
			case "com.google.gerrit.prettify":
				return required.addAll("com.google.gerrit.reviewdb", "IDE-compile-gwt");
			case "com.google.gerrit.reviewdb":
				return required.addAll("com.google.guava");
			case "com.google.guava":
				return required.addAll("IDE-compile-jsr305");
			case "com.google.gwt.servlet":
				return required.addAll(
					"com.google.gson",
					"com.google.guava",
					"com.google.protobuf",
					"org.w3c.css.sac",
					"org.eclipse.jdt.core",
					"org.junit",
					"IDE-compile-gwt",
					"IDE-compile-jsr305");
			case "com.google.gwtjsonrpc":
				return required.addAll("org.apache.commons.codec", "IDE-compile-gwt");
			case "com.google.gwtorm":
				return required.addAll("com.google.guava", "com.google.protobuf", "org.antlr.runtime");
			case "it.unibz.instasearch":
				return required.addAll("IDE-compile-lucene-2.9.4");
			case "net.jeeeyul.eclipse.themes.ui":
				return required.addAll("org.eclipse.ui.workbench.texteditor");
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
			case "org.apache.lucene.core":
				return required.add("com.ibm.icu");
			case "org.apache.lucene":
			case "org.apache.lucene.highlighter":
			case "org.apache.lucene.memory":
			case "org.apache.lucene.misc":
			case "org.apache.lucene.queries":
			case "org.apache.lucene.snowball":
			case "org.apache.lucene.spellchecker":
				return required.add("IDE-compile-lucene-2.9.4");
			case "org.apache.xmlrpc":
				return required.add("javax.servlet");
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
			case "org.eclipse.emf.ecore.edit":
			case "org.eclipse.emf.ecore.xmi":
				return required.add("org.eclipse.emf.common");
			case "org.eclipse.equinox.http.servlet":
				return required.add("IDE-compile-org.osgi.annotation.versioning");
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
			case "org.eclipse.jem.util":
				return required.add("org.eclipse.emf.common");
			case "org.eclipse.jetty.continuation":
				return required.add("IDE-compile-org.mortbay.jetty.util");
			case "org.eclipse.jetty.security":
				return required.add("org.eclipse.jetty.io");
			case "org.eclipse.jetty.server":
			case "org.eclipse.jetty.servlet":
				return required.add("IDE-compile-org.eclipse.jetty.jmx");
			case "org.eclipse.jdt.core":
				return required.add("org.apache.ant");
			case "org.eclipse.m2e.core":
			case "org.eclipse.m2e.core.ui":
			case "org.eclipse.m2e.editor.xml":
			case "org.eclipse.m2e.jdt":
			case "org.eclipse.m2e.jdt.ui":
			case "org.eclipse.m2e.launching":
			case "org.eclipse.m2e.profiles.core":
			case "org.eclipse.m2e.scm":
				return required.addAll("IDE-compile-apache.maven");
			case "org.eclipse.m2e.discovery":
				return required.addAll("IDE-compile-apache.maven", "org.eclipse.e4.ui.workbench");
			case "org.eclipse.m2e.model.edit":
			case "org.eclipse.m2e.refactoring":
				return required.addAll("IDE-compile-apache.maven", "org.eclipse.emf.common");
			case "org.eclipse.m2e.editor":
				return required.addAll("IDE-compile-apache.maven", "org.eclipse.ui.workbench.texteditor");
			case "org.eclipse.m2e.maven.indexer":
				return required.addAll("javax.inject", "org.slf4j.api", "IDE-compile-apache.maven");
			case "org.eclipse.m2e.workspace.cli":
				return required.addAll("javax.inject", "IDE-compile-apache.maven");
			case "org.eclipse.mylyn.gerrit.core":
				return required.addAll("org.apache.commons.httpclient", "IDE-compile-gwt");
			case "org.eclipse.mylyn.reviews.ui":
				return required.add("org.eclipse.emf.common");
			case "org.eclipse.mylyn.tasks.core":
			case "org.eclipse.mylyn.tasks.ui":
				return required.addAll("IDE-compile-apache.maven", "org.eclipse.jdt.annotation");
			case "org.eclipse.mylyn.wikitext.mediawiki.core":
				return required.addAll("org.apache.ant");
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
				//			case "org.eclipse.swt":
				//				return required.add("org.eclipse.swt.win32.win32.x86_64");
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
			case "org.eclipse.wst.common.emf":
			case "org.eclipse.wst.common.emfworkbench.integration":
			case "org.eclipse.wst.xml.core":
			case "org.eclipse.wst.xsd.core":
				return required.addAll("org.eclipse.emf.common", "org.eclipse.emf.ecore");
			case "org.eclipse.xsd":
				return required.addAll("org.eclipse.emf.common");
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
			case "org.slf4j.api":
				return required.add("IDE-compile-org.slf4j");
			case "com.jcraft.jsch":
				return required.add("IDE-compile-com.jcraft.jzlib");
			default:
				return required;
		}
	}
	private static String collectInclusions(List<Path> files, ArrayList<Path> usedFiles, Path src) {
		int count = src.getNameCount();
		if(count == 1 && (src.startsWith("src") || src.startsWith("res"))) {
			return "";
		}
		List<Path> allFiles = files.filter(p -> p.startsWith(src) && p.getNameCount() > count).map(src::relativize);
		Set<String> all = gatherInclusionsFromFileList(allFiles);
		Set<String> used = gatherInclusionsFromFileList(usedFiles.toList());
		if(all.removeAll(used).isEmpty() || used.isEmpty()) {
			return "";
		}
		ArrayList<String> included = used.toArrayList().replaceAll(s -> s.replace('#', '?')).sort();
		replaceInclusion(included, "org.apache.batik.", "Version");
		replaceInclusion(included, "com.google.gwt.dev.util.*", "Name", "StringKey");
		replaceInclusion(
			included,
			"com.google.gwt.core.ext.*",
			"LinkerContext",
			"TreeLogger",
			"UnableToCompleteException");
		replaceInclusion(
			included,
			"com.google.gwt.core.ext.linker.*",
			"AbstractLinker",
			"Artifact",
			"ArtifactSet",
			"ConfigurationProperty",
			"CompilationResult",
			"EmittedArtifact",
			"LinkerOrder",
			"PropertyProviderGenerator",
			"SelectionProperty",
			"Shardable",
			"SoftPermutation",
			"SymbolData",
			"SyntheticArtifact");
		replaceInclusion(included, "com.google.gwt.core.linker.*", "SymbolMapsLinker");
		replaceInclusion(included, "com.google.gwt.user.rebind.*", "SourceWriter", "StringSourceWriter");
		replaceInclusion(included, "com.google.gwt.i18n.rebind.keygen.*", "KeyGenerator");
		replaceInclusion(included, "com.google.gerrit.extensions.api.projects.*", "ProjectState");
		replaceInclusion(included, "com.google.gerrit.extensions.common.*", "InheritableBoolean", "SubmitType");
		return " including=\"" + String.join("|", included) + "\"";
	}
	private static void replaceInclusion(ArrayList<String> included, String inclusion, String... newInclusions) {
		inclusion = inclusion.replace('.', '/');
		if(included.contains(inclusion)) {
			included.remove(inclusion);
			if(inclusion.endsWith("*")) {
				inclusion = inclusion.substring(0, inclusion.length() - 1);
			}
			for(String newInclusion : newInclusions) {
				included.add(inclusion + newInclusion + ".java");
			}
		}
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
		locallyAvailableFiles = locallyAvailableFiles.replaceAll(RegenerateProjectsMain::flattenOne);
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
		excluded = customizeAddClasspathEntryExclusions(src, excluded).removeIf(s -> s.contains("x-miss-x"));
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
	private static List<String> customizeAddClasspathEntryExclusions(Path src, List<String> excluded) {
		String unixPath = toUnixPath(src);
		switch(unixPath) {
			case "lucene-2.9.4/src/test":
				return excluded.add("org/apache/lucene/util/*Test*.java");
			case "apache.commons-lang/src/main/java":
				return excluded.add("org/apache/commons/lang/enum/");
			case "antlr.antlr3/runtime/Java/src/main/java":
				return excluded.addAll("org/antlr/runtime/tree/DOTTreeGenerator.java");
			case "apache.maven.indexer/indexer-core/src/main/java":
				return excluded.addAll("org/apache/maven/index/DefaultIndexerEngine.java");
			case "com.google.gerrit.common":
			case "com.google.gerrit.prettify":
			case "com.google.gerrit.reviewdb":
			case "com.google.gwt.servlet":
			case "com.google.gwtjsonrpc":
			case "com.google.gwtorm":
			case "org.apache.commons.httpclient":
			case "org.eclipse.m2e.lifecyclemapping.defaults":
			case "org.nodeclipse.pluginslist.core":
			case "org.slf4j.api":
				return excluded;
			case "org.eclipse.m2e.core.ui":
			case "org.eclipse.m2e.discovery":
			case "org.eclipse.m2e.editor":
			case "org.eclipse.m2e.editor.xml":
			case "org.eclipse.m2e.jdt":
			case "org.eclipse.m2e.jdt.ui":
			case "org.eclipse.m2e.launching":
			case "org.eclipse.m2e.model.edit":
			case "org.eclipse.m2e.profiles.core":
			case "org.eclipse.m2e.profiles.ui":
			case "org.eclipse.m2e.refactoring":
			case "org.eclipse.m2e.scm":
				return excluded.addAll(".*/");
			case "pm.eclipse.editbox":
				return excluded.addAll(".*");
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
			case "jetty-http/src/test/java":
				return excluded.addAll("org/eclipse/jetty/http/*Test.java");
			case "org.eclipse.swt.win32.win32.x86_64":
			case "org.eclipse.swt.cocoa.macosx.x86_64":
			case "org.eclipse.swt.gtk.linux.x86_64":
				return excluded.addAll(".*");
			case "org.eclipse.equinox.launcher.cocoa.macosx.x86_64":
			case "org.eclipse.equinox.launcher.win32.win32.x86_64":
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
		if(path == null) {
			return "";
		}
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
		HashMap<Pair<Path, Path>, ArrayList<Path>> multiMap = new HashMap<>();
		Path previousTargetFile = null;
		for(Path targetFile : target) {
			if(previousTargetFile == null || targetFile.startsWith(previousTargetFile) == false) {
				Pair<Pair<Path, Path>, Path> splitLongest = splitLongest(targetFile, map.get(targetFile.getFileName()));
				if(splitLongest != null) {
					Pair<Path, Path> key = splitLongest.lhs;
					Path value = splitLongest.rhs;
					boolean found = key.lhs.endsWith("x-miss-x") == false;
					if(found) {
						previousTargetFile = targetFile;
					}
					if(found || value.getFileName().toString().endsWith(".jar") == false) {
						multiMap.computeIfAbsent(key, p -> ArrayList.of());
						multiMap.get(key).add(value);
					}
				}
			}
		}
		return multiMap;
	}
	private static Pair<Pair<Path, Path>, Path> splitLongest(Path target, ArrayList<Path> available) {
		switch(target.getFileName().toString()) {
			case "ECLIPSE_.RSA":
			case "ECLIPSE_.SF":
			case "ECLIPSEF.RSA":
			case "ECLIPSEF.SF":
			case "JEEEYUL_.DSA":
			case "JEEEYUL_.SF":
			case "eclipse.inf":
			case ".api_description":
				return null;
		}
		if(available != null) {
			Comparator<Path> comparator =
				Comparator.comparing(p -> p.toString().contains("x-miss-x") ? Integer.MAX_VALUE : p.getNameCount());
			for(int i = 1, n = target.getNameCount(); i < n; i++) {
				Path subpath = target.subpath(i, n);
				Stream<Path> stream =
					available.stream().filter(p -> p.endsWith(subpath) && p.getNameCount() > subpath.getNameCount()).sorted(
						comparator);
				ArrayList<Path> sorted = stream.toList();
				if(sorted.notEmpty()) {
					Path found = sorted.get(0);
					int endIndex = found.getNameCount() - subpath.getNameCount();
					if(endIndex == 0) {
						throw new IllegalStateException("Ambiguous match from " + target + " and " + available);
					}
					Path src = found.subpath(0, endIndex);
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
			ArrayList<Path> listSourceCode = listSourceCode(lib);
			listSourceCode.removeIf(p -> p.getNameCount() > 2 && CLASS_FILE_INDICATORS.contains(p.getName(1)));
			list.addAll(customizeAvailableFilesList(listSourceCode, plugin));
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
		return Files.walk(src).filter(Files::isRegularFile).map(RegenerateProjectsMain::canonicalizeFileNameToMatch).map(
			srcParent::relativize).toList();
	}
	private static Path missedFileCatcher(Path p) {
		int len = p.getNameCount();
		int off = Math.max(1, len - 3);
		Path subpath1 = p.subpath(0, off);
		Path subpath2 = p.subpath(off, len);
		return subpath1.resolve("x-miss-x").resolve(subpath2);
	}
	private static ArrayList<Path> sourceFilesForPlugin(Plugin plugin) {
		HashMap<Path, byte[]> pluginContent = plugin.contents;
		Stream<Path> stream = pluginContent.keySet().stream().sorted();
		ArrayList<Path> list =
			stream.filter(p -> removesInnerClasses(p, pluginContent.get(p))).map(
				RegenerateProjectsMain::canonicalizeFileNameToMatch).toList();
		return customizeModelFilesList(plugin.id, list);
	}
	private static ArrayList<Path> customizeModelFilesList(String pluginName, ArrayList<Path> list) {
		switch(pluginName) {
			case "com.google.gerrit.prettify":
				return list.addAll(
					Paths.get("com.google.gerrit.prettify.jar/com/google/gerrit/prettify/common/PrettyFormatter.java"),
					Paths.get("com.google.gerrit.prettify.jar/com/google/gwtexpui/safehtml/client/SafeHtml.java"));
			case "com.google.gson":
				return list.remove(
					Paths.get("com.google.gson.jar/com/google/gson/internal/bind/BigDecimalTypeAdapter.java")).remove(
					Paths.get("com.google.gson.jar/com/google/gson/internal/bind/BigIntegerTypeAdapter.java")).remove(
					Paths.get("com.google.gson.jar/com/google/gson/internal/GsonInternalAccess.java")).remove(
					Paths.get("com.google.gson.jar/com/google/gson/internal/Pair.java"));
			case "com.google.guava":
				return list.add(
					Paths.get("com.google.guava.jar/com/google/thirdparty/publicsuffix/PublicSuffixPatterns.java")).remove(
					Paths.get("com.google.guava.jar/com/google/common/util/concurrent/ForwardingService.java")).remove(
					Paths.get("com.google.guava.jar/com/google/common/hash/HashCodes.java"));
			case "com.google.gwtjsonrpc":
				return list.add(Paths.get("com.google.gwtjsonrpc.jar/com/google/gwt/json/client/JSONValue.java"));
			case "com.sun.el":
				return list.replaceAll(p -> convertComSunToOrgApache(p)).remove(
					Paths.get("com.sun.el.jar/org/apache/el/parser/AstMethodArguments.java")).add(
					Paths.get("com.sun.el.jar/org/apache/el/stream/Optional.java"));
			case "javax.el":
				return list.remove(Paths.get("javax.el.jar/javax/el/PrivateMessages.properties"));
			case "javax.servlet":
				return list.remove(Paths.get("javax.servlet.jar/javax/servlet/annotation/package.html")).remove(
					Paths.get("javax.servlet.jar/javax/servlet/descriptor/package.html")).add(
					Paths.get("javax.servlet.jar/javax/servlet/resources/web-jsptaglibrary_2_0.xsd"));
			case "javax.servlet.jsp":
				return list.add(Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/jspxml.xsd")).remove(
					Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/jspxml_2_0.dtd")).remove(
					Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/jspxml_2_0.xsd")).remove(
					Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/jsp_2_0.xsd")).remove(
					Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/jsp_2_1.xsd")).remove(
					Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/jsp_2_2.xsd")).remove(
					Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/web-jsptaglibrary_1_1.dtd")).remove(
					Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/web-jsptaglibrary_1_2.dtd")).remove(
					Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/web-jsptaglibrary_2_0.xsd")).remove(
					Paths.get("javax.servlet.jsp.jar/javax/servlet/jsp/resources/web-jsptaglibrary_2_1.xsd"));
			case "org.antlr.runtime":
				return list.remove(Paths.get("antlr.antlr3/runtime/Java/src/main/java/org/antlr/runtime/tree/DOTTreeGenerator.java"));
			case "org.apache.batik.css":
				return list.add(Paths.get("org.apache.batik.css.jar/org/apache/batik/css/engine/value/svg12/AbstractCIEColor.java"));
			case "org.apache.commons.codec":
				return list.add(Paths.get("org.apache.commons.codec.jar/org/apache/commons/codec/language/dmrules.txt"));
			case "org.apache.commons.compress":
				return list.remove(
					Paths.get("org.apache.commons.compress.jar/org/apache/commons/compress/compressors/z/_internal_/InternalLZWInputStream.java")).remove(
					Paths.get("org.apache.commons.compress.jar/org/apache/commons/compress/compressors/z/_internal_/package.html")).add(
					Paths.get("org.apache.commons.compress.jar/org/apache/commons/compress/compressors/lzw/LZWInputStream.java"));
			case "org.apache.httpcomponents.httpclient":
				return list.addAll(
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/client/config/CookieSpecs.java"),
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/conn/socket/ConnectionSocketFactory.java"),
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/impl/execchain/BackoffStrategyExec.java"),
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/osgi/impl/OSGiClientBuilderFactory.java"),
					Paths.get("org.apache.httpcomponents.httpclient.jar/org/apache/http/osgi/services/HttpClientBuilderFactory.java"));
			case "org.apache.httpcomponents.httpcore":
				return list.addAll(Paths.get("org.apache.httpcomponents.httpcore.jar/org/apache/http/config/Registry.java"));
			case "org.apache.jasper.glassfish":
				return list.remove(
					Paths.get("org.apache.jasper.glassfish.jar/org/eclipse/jdt/internal/compiler/flow/NullInfoRegistry.java")).remove(
					Paths.get("org.apache.jasper.glassfish.jar/org/eclipse/jdt/internal/compiler/parser/readableNames.properties")).addAll(
					Paths.get("org.apache.jasper.glassfish.jar/org/apache/jasper/util/SystemLogHandler.java"),
					Paths.get("org.apache.jasper.glassfish.jar/org/eclipse/jdt/internal/compiler/parser/readableNames.props"));
			case "org.apache.xerces":
				return list.remove(Paths.get("org.apache.xerces.jar/org/apache/xerces/impl/xs/util/NSItemListImpl.java"));
			case "org.eclipse.e4.ui.workbench":
				return list.remove(
					Paths.get("org.eclipse.e4.ui.workbench.jar/org/eclipse/e4/ui/internal/workbench/ExitHandler.java")).remove(
					Paths.get("org.eclipse.e4.ui.workbench.jar/org/eclipse/e4/ui/internal/workbench/IHelpService.java"));
			case "org.eclipse.egit.ui":
				return list.remove(Paths.get("org.eclipse.egit.ui.jar/icons/ovr/symlink_ovr.gif")).addAll(
					Paths.get("org.eclipse.egit.ui.jar/org/eclipse/egit/ui/internal/revision/EditableRevision.java"),
					Paths.get("org.eclipse.egit.ui.jar/org/eclipse/egit/ui/internal/selection/SelectionUtils.java"));
			case "org.eclipse.equinox.http.servlet":
				return list.remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/DefaultHttpContext.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/FilterChainImpl.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/FilterConfigImpl.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/FilterRegistration.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/HttpServletRequestAdaptor.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/HttpSessionAdaptor.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/ProxyContext.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/ProxyServlet.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/Registration.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/RequestDispatcherAdaptor.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/ResourceServlet.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/ServletConfigImpl.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/ServletContextAdaptor.java")).remove(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/ServletRegistration.java")).addAll(
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/registration/FilterRegistration.java"),
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/context/DefaultServletContextHelper.java"),
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/error/NullContextNamesException.java"),
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/util/StringPlus.java"),
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/customizer/FilterTrackerCustomizer.java"),
					Paths.get("org.eclipse.equinox.http.servlet.jar/org/eclipse/equinox/http/servlet/internal/servlet/ProxyServlet.java"));
			case "org.eclipse.help.webapp":
				return list.removeIf(p -> p.getFileName().toString().endsWith("_jsp.java"));
			case "org.eclipse.jetty.io":
				return list.add(Paths.get("org.eclipse.jetty.io.jar/org/eclipse/jetty/io/ssl/SslConnection.java"));
			case "org.eclipse.jetty.util":
				return list.add(Paths.get("org.eclipse.jetty.util.jar/org/eclipse/jetty/util/annotation/ManagedAttribute.java"));
			case "org.eclipse.jdt.core":
				return list.remove(
					Paths.get("org.eclipse.jdt.core.jar/org/eclipse/jdt/internal/compiler/ast/InnerInferenceHelper.java")).remove(
					Paths.get("org.eclipse.jdt.core.jar/org/eclipse/jdt/internal/compiler/lookup/IntersectionCastTypeBinding.java")).remove(
					Paths.get("org.eclipse.jdt.core.jar/org/eclipse/jdt/internal/compiler/parser/CommitRollbackParser.java"));
			case "org.eclipse.jdt.doc.isv":
				return list.removeIf(p -> p.startsWith("org.eclipse.jdt.doc.isv.jar/index")).removeIf(
					p -> p.startsWith("org.eclipse.jdt.doc.isv.jar/reference"));
			case "org.eclipse.jdt.doc.user":
				return list.removeIf(p -> p.startsWith("org.eclipse.jdt.doc.user.jar/index"));
			case "org.eclipse.jgit":
				return list.add(Paths.get("org.eclipse.jgit.jar/org/eclipse/jgit/ignore/internal/IMatcher.java"));
			case "org.eclipse.m2e.core":
				return list.addAll(Paths.get("org.eclipse.m2e.core.jar/org/eclipse/m2e/core/internal/equinox/EquinoxLocker.java"));
			case "org.eclipse.m2e.maven.indexer":
				return list.removeIf(p -> p.startsWith("org.eclipse.m2e.maven.indexer/org"));
			case "org.eclipse.pde.ui.templates":
				return list.removeIf(p -> p.getName(1).toString().startsWith("templates_"));
			case "org.eclipse.osgi.services":
				return list.addAll(
					Paths.get("org.eclipse.osgi.services.jar/org/osgi/service/http/context/ServletContextHelper.java"),
					Paths.get("org.eclipse.osgi.services.jar/org/osgi/service/http/whiteboard/HttpWhiteboardConstants.java"),
					Paths.get("org.eclipse.osgi.services.jar/org/osgi/service/http/runtime/HttpServiceRuntime.java"),
					Paths.get("org.eclipse.osgi.services.jar/org/osgi/service/http/runtime/dto/ErrorPageDTO.java"));
			case "org.eclipse.pde.doc.user":
				return list.removeIf(p -> p.startsWith("org.eclipse.pde.doc.user.jar/index")).removeIf(
					p -> p.startsWith("org.eclipse.pde.doc.user.jar/reference")).removeIf(
					p -> p.startsWith("org.eclipse.pde.doc.user.jar/whatsNew"));
			case "org.eclipse.platform.doc.isv":
				return list.removeIf(p -> p.startsWith("org.eclipse.platform.doc.isv.jar/index")).removeIf(
					p -> p.startsWith("org.eclipse.platform.doc.isv.jar/reference")).removeIf(
					p -> p.startsWith("org.eclipse.platform.doc.isv.jar/samples"));
			case "org.eclipse.platform.doc.user":
				return list.removeIf(p -> p.startsWith("org.eclipse.platform.doc.user.jar/index")).removeIf(
					p -> p.startsWith("org.eclipse.platform.doc.user.jar/whatsNew"));
			case "org.eclipse.ui.ide":
				return list.remove(Paths.get("org.eclipse.ui.ide.jar/org/eclipse/ui/internal/ide/dialogs/SimpleListContentProvider.java"));
			case "org.eclipse.ui.ide.application":
				return list.add(Paths.get("org.eclipse.ui.ide.application.jar/org/eclipse/ui/internal/ide/application/addons/ModelCleanupAddon.java"));
			case "org.eclipse.swt.win32.win32.x86_64":
				return list.addAll(
					Paths.get("org.eclipse.swt.win32.win32.x86_64.jar/org/eclipse/swt/browser/Mozilla.java"),
					Paths.get("org.eclipse.swt.win32.win32.x86_64.jar/org/eclipse/swt/browser/WebKit.java"),
					Paths.get("org.eclipse.swt.win32.win32.x86_64.jar/org/eclipse/swt/browser/Website.java"),
					Paths.get("org.eclipse.swt.win32.win32.x86_64.jar/org/eclipse/swt/browser/MozillaDelegate.java"));
			case "org.kohsuke.args4j":
				return list.remove(Paths.get("org.kohsuke.args4j.jar/org/kohsuke/args4j/MapSetter.java")).remove(
					Paths.get("org.kohsuke.args4j.jar/org/kohsuke/args4j/Messages_de_DE.properties")).remove(
					Paths.get("org.kohsuke.args4j.jar/org/kohsuke/args4j/Messages_ru_RU.properties"));
			case "org.sat4j.core":
				return list.remove(Paths.get("org.sat4j.core.jar/org/sat4j/minisat/core/Constr.java")).remove(
					Paths.get("org.sat4j.core.jar/org/sat4j/minisat/core/Propagatable.java")).remove(
					Paths.get("org.sat4j.core.jar/org/sat4j/MoreThanSAT.java"));
			case "org.sat4j.pb":
				return list.remove(Paths.get("org.sat4j.pb.jar/org/sat4j/pb/PseudoBitsAdderDecorator.java")).addAll(
					Paths.get("org.sat4j.pb.jar/org/sat4j/pb/multiobjective/IMultiObjOptimizationProblem.java"));
			case "pm.eclipse.editbox":
				return list.remove(Paths.get("pm.eclipse.editbox.jar/icons/editbox.gif")).remove(
					Paths.get("pm.eclipse.editbox.jar/Java_PaleBlue.eb")).addAll(
					Paths.get("pm.eclipse.editbox.jar/icons/editbox.png"));
			default:
				return list;
		}
	}
	private static Path convertComSunToOrgApache(Path p) {
		return Paths.get(toUnixPath(p).replace("com/sun/", "org/apache/"));
	}
	private static ArrayList<Path> customizeAvailableFilesList(ArrayList<Path> list, String plugin) {
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
			case "org.eclipse.jgit":
				return list.remove(Paths.get("org.eclipse.jgit/src/org/eclipse/jgit/internal/storage/file/BitmapIndexImpl.java"));
			case "org.eclipse.swt.win32.win32.x86_64":
				return list.removeIf(RegenerateProjectsMain::notForWindows);
			case "org.eclipse.swt.cocoa.macosx.x86_64":
				return list.removeIf(RegenerateProjectsMain::notForMacOSX);
			case "org.eclipse.swt.gtk.linux.x86_64":
				return list.removeIf(RegenerateProjectsMain::notForLinux);
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
				return list.removeIf(RegenerateProjectsMain::orbitSourceIdentifier);
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
	private static Map<String, List<Path>> findSourceFolders(Path root, ArrayList<String> plugins) throws IOException {
		Path libraries = root.resolve("libraries");
		ArrayList<Path> unused = Files.walk(libraries, 5).filter(Files::isDirectory).toList();
		List<Path> folders = unused.toList();
		HashSet<Path> usedLibraries = HashSet.of();
		HashSet<Path> unusedLibraries =
			folders.stream().map(libraries::relativize).filter(p -> p.getNameCount() > 1).map(p -> p.subpath(0, 2)).toSet();
		filterCompileDependencies(unusedLibraries, root);
		HashMap<String, List<Path>> map = HashMap.of();
		for(String plugin : plugins) {
			List<Path> libraryFolders = libraryFolders(root, folders, plugin);
			map.put(plugin, libraryFolders);
			for(Path libraryFolder : libraryFolders) {
				if(libraryFolder.getNameCount() > 2) {
					Path prefix = libraryFolder.subpath(1, 3);
					unusedLibraries.remove(prefix);
					usedLibraries.add(prefix.getName(0));
				} else if(libraryFolder.getNameCount() > 1) {
					Path prefix = libraryFolder.getName(1);
					unusedLibraries.removeIf(p -> p.startsWith(prefix));
					usedLibraries.add(prefix);
				}
			}
		}
		if(unused.notEmpty()) {
			System.out.println("\nUnused libraries paths:");
			for(Path path : unusedLibraries.toArrayList().sort()) {
				if(usedLibraries.contains(path.getName(0))) {
					System.out.println("(partial) " + path);
				} else {
					System.out.println("(fully)   " + path);
				}
			}
			System.out.println();
		}
		return map.toMap();
	}
	private static void filterCompileDependencies(HashSet<Path> unusedLibraries, Path root) throws IOException {
		Path projects = root.resolve("projects");
		ArrayList<Path> list =
			Files.list(projects).filter(p -> p.endsWith("IDE") || p.getFileName().toString().contains("-compile-")).toList();
		Pattern pattern = Pattern.compile("<locationURI>PARENT-2-PROJECT_LOC/libraries/(.*)</locationURI>");
		for(Path project : list) {
			Path dotProject = project.resolve(".project");
			if(Files.isRegularFile(dotProject)) {
				ArrayList<String> l = Files.readAllLines(dotProject);
				Stream<Path> s = l.stream().map(pattern::matcher).filter(Matcher::find).map(m -> Paths.get(m.group(1)));
				ArrayList<Path> links = s.toList();
				for(Path link : links) {
					unusedLibraries.removeIf(p -> p.startsWith(link));
				}
			}
		}
	}
	private static void printRoots(Map<String, List<Path>> pluginRoots) {
		for(String name : sortedKeys(pluginRoots)) {
			List<Path> list = pluginRoots.get(name);
			System.out.printf("%-60s : %s%n", name, list);
		}
	}
	private static List<Path> libraryFolders(Path root, List<Path> folders, String pluginName) {
		if(pluginName.endsWith("-2") || pluginName.endsWith("-3")) {
			pluginName = pluginName.substring(0, pluginName.length() - 2);
		}
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
					Paths.get("libraries/eclipse.rt.equinox.framework/bundles/" + pluginName));
		}
		Path pluginPath = customizePluginNameToLibrariesPathMapping(pluginName);
		Path pluginPathGuess = pluginNameToLibrariesPathLevenshtein(pluginName, folders);
		if(pluginPath == null) {
			pluginPath = pluginPathGuess;
		} else if(pluginPathGuess.equals(pluginPath)) {
			System.out.println("Unnecessary librariesPath " + pluginName + " \t\t--- " + pluginPath);
		}
		List<Path> libraries = sourceInLibraries(root, folders, pluginPath);
		return customizeAddLibrariesPaths(pluginName, libraries);
	}
	private static List<Path> customizeAddLibrariesPaths(String pluginName, List<Path> libraries) {
		switch(pluginName) {
			case "com.google.gerrit.prettify":
				return libraries.add(Paths.get("libraries/google.gerrit/gerrit-gwtexpui"));
			case "com.google.gerrit.reviewdb":
				return libraries.add(Paths.get("libraries/google.gerrit/gerrit-extension-api"));
			case "net.jeeeyul.eclipse.themes":
				return libraries.add(Paths.get("libraries/jeeeyul.eclipse-themes/net.jeeeyul.eclipse.themes/lib"));
			case "org.apache.jasper.glassfish":
				return libraries.add(Paths.get("libraries/eclipse.jdt.core"));
			case "org.eclipse.m2e.maven.indexer":
				return libraries.addAll(
					Paths.get("libraries/apache.maven.indexer"),
					Paths.get("libraries/apache.lucene-solr/lucene"));
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
	private static Path customizePluginNameToLibrariesPathMapping(String pluginName) {
		switch(pluginName) {
			case "com.sun.el":
			case "javax.annotation":
			case "javax.el":
			case "javax.servlet":
			case "javax.servlet.jsp":
				return Paths.get("apache.tomcat");
			case "org.antlr.runtime":
				return Paths.get("antlr.antlr3");
			case "org.apache.batik.util.gui":
				return Paths.get("apache.batik");
			case "org.apache.ws.commons.util":
				return Paths.get("ws-commons-util-1.0.1");
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
			case "org.apache.lucene":
			case "org.apache.lucene.highlighter":
			case "org.apache.lucene.memory":
			case "org.apache.lucene.misc":
			case "org.apache.lucene.queries":
			case "org.apache.lucene.snowball":
			case "org.apache.lucene.spellchecker":
				return Paths.get("lucene-2.9.4");
			case "org.apache.xmlrpc":
				return Paths.get("apache-xmlrpc-3.1.3");
			case "org.apache.xml.resolver":
				return Paths.get("apache.xerces.xml-commons");
			case "org.apache.xml.serializer":
				return Paths.get("apache.xalan-j");
			case "javax.xml":
				return Paths.get("apache.xerces.xml-commons/java/external");
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
				return Paths.get("ow2.asm/asm");
			default:
				return null;
		}
	}
	private static Path pluginNameToLibrariesPathLevenshtein(String pluginName, List<Path> folders) {
		Comparator<Path> c = Comparator.comparingInt(p -> distanceLevenshtein(p.getFileName().toString(), pluginName));
		Optional<Path> min = folders.stream().min(c);
		return min.get().getFileName();
	}
	private static int distanceLevenshtein(String s1, String s2) {
		if(Objects.equals(s1, s2)) {
			return 0;
		}
		if(s1.compareTo(s2) > 0) {
			String tmp = s2;
			s2 = s1;
			s1 = tmp;
		}
		Pair<String, String> key = Pair.of(s1, s2);
		Integer result = cachedDistances.get(key);
		if(result != null) {
			return result;
		}
		int[][] distance1 = new int[s1.length() + 1][s2.length() + 1];
		for(int i = 0; i <= s1.length(); i++) {
			distance1[i][0] = i;
		}
		for(int j = 0; j <= s2.length(); j++) {
			distance1[0][j] = j;
		}
		for(int i = 1; i <= s1.length(); i++) {
			for(int j = 1; j <= s2.length(); j++) {
				int d1 = distance1[i - 1][j] + 1;
				int d2 = distance1[i][j - 1] + 1;
				int d3 = distance1[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1);
				distance1[i][j] = Math.min(Math.min(d1, d2), d3);
			}
		}
		result = distance1[s1.length()][s2.length()];
		cachedDistances.put(key, result);
		return result;
	}
	private static List<Path> sourceInLibraries(Path root, List<Path> folders, Path pluginName) {
		Stream<Path> stream = folders.stream().filter(path -> path.endsWith(pluginName));
		Stream<Path> sorted = stream.sorted(Comparator.comparing(path -> path.getNameCount()));
		return sorted.limit(1).toList().map(path -> root.relativize(path)).toList();
	}
	private static Map<String, List<String>> combineDependencies(ReadOnlyList<Plugin> plugins) {
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
	private static void addFromFragmentHosts(ReadOnlyList<Plugin> plugins, HashMap<String, List<String>> depending) {
		HashMap<String, List<String>> hosts = gatherNames(plugins, "Fragment-Host");
		for(Pair<String, List<String>> pair : hosts.entrySet()) {
			depending.compute(pair.lhs, (k, v) -> v == null ? pair.rhs : v.addAll(pair.rhs));
		}
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
	private static HashMap<String, List<String>> gatherNames(ReadOnlyList<Plugin> plugins, String key) {
		return gatherNamesWith(plugins, key, "");
	}
	private static HashMap<String, List<String>> gatherNamesWith(ReadOnlyList<Plugin> plugins, String key, String tag) {
		return plugins.stream().toMultiMap(p -> p.id, p -> nameList(key, tag, p), m -> m);
	}
	private static List<String> nameList(String key, String tag, ArrayList<Plugin> p) {
		Plugin plugin = p.sort().get(-1);
		String required = plugin.manifest.get(key);
		if(required == null) {
			return List.of();
		}
		String[] split = required.replaceAll("\"\\[?([^\",]+),?[^\"]*\"", "$1").split(",");
		return Stream.of(split).filter(s -> s.contains(tag)).map(RegenerateProjectsMain::toBundleName).toList().toList();
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
	private static ArrayList<Plugin> readPluginContents(Path pluginsFolder) throws IOException {
		Path binaryInclusions = Paths.get("").toAbsolutePath().resolveSibling("IDE-all-extras/extras/plugins");
		HashSet<Path> skipped = HashSet.of();
		if(Files.isDirectory(binaryInclusions)) {
			ArrayList<Path> list =
				Files.list(binaryInclusions).map(p -> pluginsFolder.resolve(binaryInclusions.relativize(p))).toList();
			skipped.addAll(list);
		}
		IOStream<Path> stream = Files.list(pluginsFolder).filter(path -> filterExtraPluginsEntries(path, skipped));
		IOStream<Pair<Path, Map<Path, byte[]>>> stream3 =
			stream.map(RegenerateProjectsMain::readContents).filter(RegenerateProjectsMain::hasManifest);
		IOStream<Plugin> stream4 = stream3.map(p -> new Plugin(p.lhs, p.rhs.toHashMap()));
		HashMap<String, Plugin> map = stream4.toMultiMap(Plugin::name, l -> l.sort().get(-1), m -> m);
		return map.values().toArrayList().sort();
	}
	static void duplicatePlugin(HashMap<String, Plugin> map, String name, String suffix) {
		if(map.containsKey(name)) {
			String newName = name + suffix;
			map.put(newName, map.get(name).name(newName));
		}
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
