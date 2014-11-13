package project.combiner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import project.Feature;
import project.Feature.Import;
import project.Plugin;
import project.Snapshot;
import utils.lists.ArrayList;
import utils.lists.Arrays;
import utils.lists.Files;
import utils.lists.HashMap;
import utils.lists.List;
import utils.lists.Map;
import utils.lists.Paths;
import utils.lists.Set;
import utils.streams2.IOStream;
import utils.streams2.IntStream;
import utils.streams2.Streams;

class P2ProfileGenerator {
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static Snapshot snapshot;

	public static void main(String[] args) throws IOException {
		Path officialVersion = Paths.get(System.getProperty("user.home", ""), "evening");
		Path currentVersion = Paths.get("").toAbsolutePath().resolve("target/ide-1");
		List<String> version1 = readLastProfile(officialVersion);
		version1 = Files.readAllLines(Paths.get("").toAbsolutePath().resolve("1411396396819-eve.profile")).toList();
		List<String> version2 = readLastProfile(currentVersion);
		MainBuildIDE.initializeFromSnapshot();
		version2 =
			readNewProfile("p2/org.eclipse.equinox.p2.engine/profileRegistry/ECLIPSE_PROFILE_ID.profile/PROFILE_TIME.profile.gz/PROFILE_TIME.profile");
		Files.write(Paths.get("1411396396819-gen.profile"), version2, UTF8);
		compare(version1, version2);
	}
	static byte[] profileGz(Snapshot currentSnapshot, String timestamp, String profileID, String productID)
		throws IOException {
		snapshot = currentSnapshot;
		String ECLIPSE_PROFILE_ID = profileID;
		String ECLIPSE_PRODUCT_ID = productID;
		String INSTALL_FOLDER =
			"/jobs/genie.technology.packaging/mars.epp-tycho-build/workspace/org.eclipse.epp.packages/packages/" +
			ECLIPSE_PRODUCT_ID +
			"/target/products/" +
			ECLIPSE_PROFILE_ID +
			"/win32/win32/x86_64/eclipse";
		INSTALL_FOLDER = "D:\\evening";
		String cache =
			"/jobs/genie.technology.packaging/mars.epp-tycho-build/workspace/org.eclipse.epp.packages/packages/" +
			ECLIPSE_PRODUCT_ID +
			"/target/products/" +
			ECLIPSE_PROFILE_ID +
			"/win32/win32/x86_64/eclipse";
		cache = "D:\\evening";
		String cacheRoot = cache.replace('\\', '/');
		String cacheExtensions =
			"file:/" +
			cacheRoot +
			"/.eclipseextension|file:/" +
			cacheRoot +
			"/configuration/org.eclipse.osgi/118/data/listener_1925729951/";
		ArrayList<String> lines =
			ArrayList.of(
				"<?xml version='1.0' encoding='UTF-8'?>",
				"<?profile version='1.0.0'?>",
				"<profile id='" + ECLIPSE_PROFILE_ID + "' timestamp='" + timestamp + "'>",
				"  <properties size='7'>",
				"    <property name='org.eclipse.equinox.p2.installFolder' value='" + INSTALL_FOLDER + "'/>",
				"    <property name='org.eclipse.equinox.p2.cache' value='" + cache + "'/>",
				"    <property name='org.eclipse.update.install.features' value='true'/>",
				"    <property name='org.eclipse.equinox.p2.roaming' value='true'/>",
				"    <property name='org.eclipse.equinox.p2.environments' value='osgi.nl=en_US,osgi.ws=win32,osgi.arch=x86_64,osgi.os=win32'/>",
				"    <property name='eclipse.touchpoint.launcherName' value='eclipse'/>",
				"    <property name='org.eclipse.equinox.p2.cache.extensions' value='" + cacheExtensions + "'/>",
				"  </properties>");
		HashMap<Path, HashMap<String, String>> pluginToManifest = MainBuildIDE.getPluginToManifest();
		HashMap<Path, Map<String, String>> pluginToProperties = getPluginToProperties();
		HashMap<Path, Map<String, String>> pluginToP2Inf = getPluginToP2Inf();
		Path configJrePluginPath = fromZipPath("plugins/config.a.jre.javase");
		Path jrePluginPath = fromZipPath("plugins/a.jre.javase");
		Path platform_rootPluginPath = fromZipPath("plugins/org.eclipse.platform_root");
		Path platformIDEPath = fromZipPath("features/org.eclipse.platform.ide");
		Path platformIDEExecutablePath = fromZipPath("features/org.eclipse.platform.ide.executable.win32.win32.x86_64");
		Path platformIDEExecutableEclipsePath =
			fromZipPath("features/org.eclipse.platform.ide.executable.win32.win32.x86_64.eclipse");
		Path rcpConfiguration_rootPluginPath =
			fromZipPath("plugins/org.eclipse.rcp.configuration_root.win32.win32.x86_64");
		Path rcp_rootPluginPath = fromZipPath("plugins/org.eclipse.rcp_root");
		Path platformPath = fromZipPath("features/org.eclipse.platform.feature.group");
		HashMap<Path, Feature> originalFeatures = getFeatures();
		HashMap<Path, Plugin> plugins = getPlugins();
		ArrayList<Path> pluginPaths = pluginToManifest.keySet().toArrayList();
		HashMap<String, String> versionsStore = new HashMap<>();
		for(Path pluginPath : pluginPaths) {
			versionsStore.put(
				pluginPath.getFileName().toString().replace(".jar", ""),
				extractVersion(pluginPath, pluginToManifest));
		}
		pluginPaths.addAll(
			configJrePluginPath,
			jrePluginPath,
			platform_rootPluginPath,
			platformIDEPath,
			platformIDEExecutablePath,
			platformIDEExecutableEclipsePath,
			rcp_rootPluginPath,
			rcpConfiguration_rootPluginPath);
		pluginPaths.addAll(tooling.keySet());
		for(Tooling tool : Tooling.values()) {
			Path path = fromZipPath("features/" + tool.name);
			pluginPaths.add(path);
			Path versionSource = fromZipPath("plugins/" + tool.versionSource);
			String version = extractVersion(versionSource, pluginToManifest);
			versionsStore.put(tool.name, version);
		}
		HashMap<Path, Feature> features = new HashMap<>(originalFeatures.size() * 2);
		for(HashMap.Entry<Path, Feature> entry : originalFeatures) {
			Feature feature = entry.rhs;
			Path group = entry.lhs.resolveSibling(feature.id + ".feature.group");
			pluginPaths.add(group);
			features.put(group, feature);
			Path jar = entry.lhs.resolveSibling(feature.id + ".feature.jar");
			pluginPaths.add(jar);
			features.put(jar, feature);
			features.put(entry.lhs, feature);
		}
		pluginPaths.sort(Comparator.comparing((Path p) -> extractName(p, pluginToManifest)).thenComparing(
			p -> extractVersion(p, pluginToManifest)));
		Feature p2UserUIFeature = features.get(fromZipPath("features/org.eclipse.equinox.p2.user.ui"));
		Feature rcpConfigurationFeature = features.get(fromZipPath("features/org.eclipse.rcp.configuration"));
		Feature platformFeature = features.get(platformPath);
		Plugin platformPlugin = plugins.get(fromZipPath("plugins/org.eclipse.platform"));
		Plugin rcpPlugin = plugins.get(fromZipPath("plugins/org.eclipse.rcp.jar"));
		Plugin equinoxLauncherPlugin = plugins.get(fromZipPath("plugins/org.eclipse.equinox.launcher.jar"));
		Plugin equinoxCommonPlugin = plugins.get(fromZipPath("plugins/org.eclipse.equinox.common.jar"));
		Plugin equinoxDSPlugin = plugins.get(fromZipPath("plugins/org.eclipse.equinox.ds.jar"));
		Plugin equinoxEventPlugin = plugins.get(fromZipPath("plugins/org.eclipse.equinox.event.jar"));
		Plugin equinoxSimpleconfiguratorPlugin =
			plugins.get(fromZipPath("plugins/org.eclipse.equinox.simpleconfigurator.jar"));
		Plugin updateConfiguratorPlugin = plugins.get(fromZipPath("plugins/org.eclipse.update.configurator.jar"));
		Plugin equinoxP2ReconcilerDropinsPlugin =
			plugins.get(fromZipPath("plugins/org.eclipse.equinox.p2.reconciler.dropins.jar"));
		unitsOpen(lines, pluginPaths.size());
		for(Path pluginPath : pluginPaths) {
			if(pluginPath == configJrePluginPath) {
				addConfigJreUnit(lines);
			} else if(pluginPath == jrePluginPath) {
				addJreUnit(lines);
			} else if(pluginPath == platformIDEPath) {
				addPlatformIDE(lines, platformFeature, p2UserUIFeature, rcpConfigurationFeature);
			} else if(pluginPath == platformIDEExecutablePath) {
				addPlatformIDEExecutable(lines, platformFeature);
			} else if(pluginPath == platformIDEExecutableEclipsePath) {
				addPlatformIDEExecutableEclipse(lines, platformFeature);
			} else if(pluginPath == rcpConfiguration_rootPluginPath) {
				addRcpConfigurationUnit(lines, rcpConfigurationFeature);
			} else if(pluginPath == platform_rootPluginPath) {
				addBinary_RootUnit(lines, platformPlugin);
			} else if(pluginPath == rcp_rootPluginPath) {
				addBinary_RootUnit(lines, rcpPlugin);
			} else if(Tooling.has(pluginPath)) {
				performTooling(lines, Tooling.get(pluginPath), features, versionsStore, equinoxLauncherPlugin);
			} else if(tooling.containsKey(pluginPath)) {
				lines.addAll(replaceVersions(
					pluginPath,
					equinoxLauncherPlugin,
					equinoxCommonPlugin,
					equinoxDSPlugin,
					equinoxEventPlugin,
					equinoxP2ReconcilerDropinsPlugin,
					equinoxSimpleconfiguratorPlugin,
					updateConfiguratorPlugin));
			} else if(features.containsKey(pluginPath)) {
				addFeatureUnit(lines, features.get(pluginPath), pluginPath.getFileName().toString());
			} else {
				HashMap<String, String> manifest = pluginToManifest.get(pluginPath);
				Map<String, String> props = pluginToProperties.get(pluginPath);
				Map<String, String> p2Inf = pluginToP2Inf.get(pluginPath);
				addPluginUnit(pluginPath, lines, props, p2Inf, manifest);
			}
		}
		unitsClose(lines);
		iusPropertiesOpen(lines, pluginPaths.size());
		for(Path plugin : pluginPaths) {
			String s1 = plugin.getFileName().toString();
			String s2 = s1.replace(".feature.group", "");
			String s3 = s2.replace(".feature.jar", "");
			String replaceAll = s3.replaceFirst("(_[0-9&&[^6]][^_4][^_]+\\.?j?a?r?$)|(\\.jar$)", "");
			switch(replaceAll) {
				case "com.ibm.icu":
				case "com.jcraft.jsch":
				case "com.sun.el":
				case "javax.annotation":
				case "javax.el":
				case "javax.inject":
				case "javax.servlet.jsp":
				case "javax.servlet":
				case "javax.xml":
				case "org.apache.ant":
				case "org.apache.batik.css":
				case "org.apache.batik.util.gui":
				case "org.apache.batik.util":
				case "org.apache.commons.codec":
				case "org.apache.commons.logging":
				case "org.apache.felix.gogo.command":
				case "org.apache.felix.gogo.runtime":
				case "org.apache.felix.gogo.shell":
				case "org.apache.httpcomponents.httpclient":
				case "org.apache.httpcomponents.httpcore":
				case "org.apache.jasper.glassfish":
					//				case "org.apache.log4j":
				case "org.apache.lucene.analysis":
				case "org.apache.lucene.core":
					//				case "org.apache.ws.commons.util":
					//				case "org.apache.xerces":
					//				case "org.apache.xml.resolver":
					//				case "org.apache.xml.serializer":
					//				case "org.apache.xmlrpc":
				case "org.eclipse.ant.core":
					//				case "org.eclipse.ant.launching":
					//				case "org.eclipse.ant.ui":
				case "org.eclipse.compare.core":
				case "org.eclipse.compare.win32":
				case "org.eclipse.compare":
				case "org.eclipse.core.commands":
				case "org.eclipse.core.contenttype":
				case "org.eclipse.core.databinding.beans":
				case "org.eclipse.core.databinding.observable":
				case "org.eclipse.core.databinding.property":
				case "org.eclipse.core.databinding":
				case "org.eclipse.core.expressions":
				case "org.eclipse.core.externaltools":
				case "org.eclipse.core.filebuffers":
				case "org.eclipse.core.filesystem.java7":
				case "org.eclipse.core.filesystem.win32.x86_64":
				case "org.eclipse.core.filesystem":
				case "org.eclipse.core.jobs":
				case "org.eclipse.core.net.win32.x86_64":
				case "org.eclipse.core.net":
				case "org.eclipse.core.resources.win32.x86_64":
				case "org.eclipse.core.resources":
				case "org.eclipse.core.runtime.compatibility.registry":
				case "org.eclipse.core.runtime.compatibility":
				case "org.eclipse.core.runtime":
				case "org.eclipse.core.variables":
					//				case "org.eclipse.cvs":
				case "org.eclipse.debug.core":
				case "org.eclipse.debug.ui":
				case "org.eclipse.e4.core.commands":
				case "org.eclipse.e4.core.contexts":
				case "org.eclipse.e4.core.di.extensions":
				case "org.eclipse.e4.core.di":
				case "org.eclipse.e4.core.services":
				case "org.eclipse.e4.rcp":
					//				case "org.eclipse.e4.tools.css.spy":
					//				case "org.eclipse.e4.tools.spy":
				case "org.eclipse.e4.ui.bindings":
				case "org.eclipse.e4.ui.css.core":
				case "org.eclipse.e4.ui.css.swt.theme":
				case "org.eclipse.e4.ui.css.swt":
				case "org.eclipse.e4.ui.di":
				case "org.eclipse.e4.ui.model.workbench":
				case "org.eclipse.e4.ui.services":
				case "org.eclipse.e4.ui.widgets":
				case "org.eclipse.e4.ui.workbench.addons.swt":
				case "org.eclipse.e4.ui.workbench.renderers.swt":
				case "org.eclipse.e4.ui.workbench.swt":
				case "org.eclipse.e4.ui.workbench3":
				case "org.eclipse.e4.ui.workbench":
				case "org.eclipse.ecf":
				case "org.eclipse.ecf.core.feature":
				case "org.eclipse.ecf.core.ssl.feature":
				case "org.eclipse.ecf.filetransfer":
				case "org.eclipse.ecf.filetransfer.feature":
				case "org.eclipse.ecf.filetransfer.httpclient4.feature":
				case "org.eclipse.ecf.filetransfer.httpclient4.ssl.feature":
				case "org.eclipse.ecf.filetransfer.ssl.feature":
				case "org.eclipse.ecf.identity":
				case "org.eclipse.ecf.provider.filetransfer":
				case "org.eclipse.ecf.provider.filetransfer.httpclient4":
				case "org.eclipse.ecf.provider.filetransfer.httpclient4.ssl":
				case "org.eclipse.ecf.provider.filetransfer.ssl":
				case "org.eclipse.ecf.ssl":
					//				case "org.eclipse.egit":
					//				case "org.eclipse.egit.core":
					//				case "org.eclipse.egit.doc":
					//				case "org.eclipse.egit.ui":
				case "org.eclipse.emf.common":
				case "org.eclipse.emf.ecore.change":
					//				case "org.eclipse.emf.ecore.edit":
				case "org.eclipse.emf.ecore.xmi":
				case "org.eclipse.emf.ecore":
					//				case "org.eclipse.emf.edit":
					//				case "org.eclipse.epp.mpc.core":
					//				case "org.eclipse.epp.mpc.help.ui":
					//				case "org.eclipse.epp.mpc.ui":
				case "org.eclipse.equinox.app":
				case "org.eclipse.equinox.bidi":
				case "org.eclipse.equinox.common":
				case "org.eclipse.equinox.concurrent":
				case "org.eclipse.equinox.console":
				case "org.eclipse.equinox.ds":
				case "org.eclipse.equinox.event":
				case "org.eclipse.equinox.frameworkadmin.equinox":
				case "org.eclipse.equinox.frameworkadmin":
				case "org.eclipse.equinox.http.jetty":
				case "org.eclipse.equinox.http.registry":
				case "org.eclipse.equinox.http.servlet":
				case "org.eclipse.equinox.jsp.jasper.registry":
				case "org.eclipse.equinox.jsp.jasper":
				case "org.eclipse.equinox.launcher.win32.win32.x86_64":
				case "org.eclipse.equinox.launcher":
				case "org.eclipse.equinox.p2.artifact.repository":
				case "org.eclipse.equinox.p2.console":
				case "org.eclipse.equinox.p2.core":
				case "org.eclipse.equinox.p2.core.feature":
				case "org.eclipse.equinox.p2.director.app":
				case "org.eclipse.equinox.p2.directorywatcher":
				case "org.eclipse.equinox.p2.director":
					//				case "org.eclipse.equinox.p2.discovery":
					//				case "org.eclipse.equinox.p2.discovery.compatibility":
				case "org.eclipse.equinox.p2.engine":
				case "org.eclipse.equinox.p2.extensionlocation":
				case "org.eclipse.equinox.p2.extras.feature":
				case "org.eclipse.equinox.p2.garbagecollector":
				case "org.eclipse.equinox.p2.jarprocessor":
				case "org.eclipse.equinox.p2.metadata.repository":
				case "org.eclipse.equinox.p2.metadata":
				case "org.eclipse.equinox.p2.operations":
				case "org.eclipse.equinox.p2.publisher.eclipse":
				case "org.eclipse.equinox.p2.publisher":
				case "org.eclipse.equinox.p2.ql":
				case "org.eclipse.equinox.p2.reconciler.dropins":
				case "org.eclipse.equinox.p2.repository.tools":
				case "org.eclipse.equinox.p2.repository":
				case "org.eclipse.equinox.p2.rcp.feature":
				case "org.eclipse.equinox.p2.touchpoint.eclipse":
				case "org.eclipse.equinox.p2.touchpoint.natives":
				case "org.eclipse.equinox.p2.transport.ecf":
				case "org.eclipse.equinox.p2.ui":
					//				case "org.eclipse.equinox.p2.ui.discovery":
				case "org.eclipse.equinox.p2.ui.importexport":
				case "org.eclipse.equinox.p2.ui.sdk":
				case "org.eclipse.equinox.p2.ui.sdk.scheduler":
				case "org.eclipse.equinox.p2.updatechecker":
				case "org.eclipse.equinox.p2.updatesite":
				case "org.eclipse.equinox.p2.user.ui":
				case "org.eclipse.equinox.preferences":
				case "org.eclipse.equinox.registry":
				case "org.eclipse.equinox.security.ui":
				case "org.eclipse.equinox.security.win32.x86_64":
				case "org.eclipse.equinox.security":
				case "org.eclipse.equinox.simpleconfigurator.manipulator":
				case "org.eclipse.equinox.simpleconfigurator":
				case "org.eclipse.equinox.util":
				case "org.eclipse.help.base":
				case "org.eclipse.help.ui":
				case "org.eclipse.help.webapp":
				case "org.eclipse.help":
					//				case "org.eclipse.jdt":
					//				case "org.eclipse.jdt.annotation":
					//				case "org.eclipse.jdt.apt.core":
					//				case "org.eclipse.jdt.apt.pluggable.core":
					//				case "org.eclipse.jdt.apt.ui":
					//				case "org.eclipse.jdt.compiler.apt":
					//				case "org.eclipse.jdt.compiler.tool":
					//				case "org.eclipse.jdt.core.manipulation":
					//				case "org.eclipse.jdt.core":
					//				case "org.eclipse.jdt.debug.ui":
					//				case "org.eclipse.jdt.debug":
					//				case "org.eclipse.jdt.doc.isv":
					//				case "org.eclipse.jdt.doc.user":
					//				case "org.eclipse.jdt.junit.core":
					//				case "org.eclipse.jdt.junit.runtime":
					//				case "org.eclipse.jdt.junit4.runtime":
					//				case "org.eclipse.jdt.junit":
					//				case "org.eclipse.jdt.launching":
					//				case "org.eclipse.jdt.ui":
					//				case "org.eclipse.jem.util":
				case "org.eclipse.jetty.continuation":
				case "org.eclipse.jetty.http":
				case "org.eclipse.jetty.io":
				case "org.eclipse.jetty.security":
				case "org.eclipse.jetty.server":
				case "org.eclipse.jetty.servlet":
				case "org.eclipse.jetty.util":
				case "org.eclipse.jface.databinding":
				case "org.eclipse.jface.text":
				case "org.eclipse.jface":
					//				case "org.eclipse.jgit.archive":
					//				case "org.eclipse.jgit":
				case "org.eclipse.jsch.core":
				case "org.eclipse.jsch.ui":
				case "org.eclipse.ltk.core.refactoring":
				case "org.eclipse.ltk.ui.refactoring":
					//				case "org.eclipse.m2e.archetype.common":
					//				case "org.eclipse.m2e.core.ui":
					//				case "org.eclipse.m2e.core":
					//				case "org.eclipse.m2e.discovery":
					//				case "org.eclipse.m2e.editor.xml":
					//				case "org.eclipse.m2e.editor":
					//				case "org.eclipse.m2e.jdt.ui":
					//				case "org.eclipse.m2e.jdt":
					//				case "org.eclipse.m2e.launching":
					//				case "org.eclipse.m2e.lifecyclemapping.defaults":
					//				case "org.eclipse.m2e.logback.appender":
					//				case "org.eclipse.m2e.logback.configuration":
					//				case "org.eclipse.m2e.maven.indexer":
					//				case "org.eclipse.m2e.maven.runtime.slf4j.simple":
					//				case "org.eclipse.m2e.maven.runtime":
					//				case "org.eclipse.m2e.model.edit":
					//				case "org.eclipse.m2e.profiles.core":
					//				case "org.eclipse.m2e.profiles.ui":
					//				case "org.eclipse.m2e.refactoring":
					//				case "org.eclipse.m2e.scm":
					//				case "org.eclipse.mylyn.bugzilla.core":
					//				case "org.eclipse.mylyn.bugzilla.ide":
					//				case "org.eclipse.mylyn.bugzilla.ui":
					//				case "org.eclipse.mylyn.commons.core":
					//				case "org.eclipse.mylyn.commons.identity.core":
					//				case "org.eclipse.mylyn.commons.net":
					//				case "org.eclipse.mylyn.commons.notifications.core":
					//				case "org.eclipse.mylyn.commons.notifications.feed":
					//				case "org.eclipse.mylyn.commons.notifications.ui":
					//				case "org.eclipse.mylyn.commons.repositories.core":
					//				case "org.eclipse.mylyn.commons.repositories.ui":
					//				case "org.eclipse.mylyn.commons.screenshots":
					//				case "org.eclipse.mylyn.commons.ui":
					//				case "org.eclipse.mylyn.commons.workbench":
					//				case "org.eclipse.mylyn.commons.xmlrpc":
					//				case "org.eclipse.mylyn.context.core":
					//				case "org.eclipse.mylyn.context.tasks.ui":
					//				case "org.eclipse.mylyn.context.ui":
					//				case "org.eclipse.mylyn.discovery.core":
					//				case "org.eclipse.mylyn.discovery.ui":
					//				case "org.eclipse.mylyn.help.ui":
					//				case "org.eclipse.mylyn.ide.ui":
					//				case "org.eclipse.mylyn.monitor.core":
					//				case "org.eclipse.mylyn.monitor.ui":
					//				case "org.eclipse.mylyn.resources.ui":
					//				case "org.eclipse.mylyn.tasks.bugs":
					//				case "org.eclipse.mylyn.tasks.core":
					//				case "org.eclipse.mylyn.tasks.index.core":
					//				case "org.eclipse.mylyn.tasks.index.ui":
					//				case "org.eclipse.mylyn.tasks.search":
					//				case "org.eclipse.mylyn.tasks.ui":
					//				case "org.eclipse.mylyn.team.cvs":
					//				case "org.eclipse.mylyn.team.ui":
				case "org.eclipse.osgi.compatibility.state":
				case "org.eclipse.osgi.services":
				case "org.eclipse.osgi.util":
				case "org.eclipse.osgi":
					//				case "org.eclipse.pde":
					//				case "org.eclipse.pde.api.tools.annotations":
					//				case "org.eclipse.pde.api.tools.ui":
					//				case "org.eclipse.pde.api.tools":
					//				case "org.eclipse.pde.build":
					//				case "org.eclipse.pde.core":
					//				case "org.eclipse.pde.doc.user":
					//				case "org.eclipse.pde.ds.core":
					//				case "org.eclipse.pde.ds.ui":
					//				case "org.eclipse.pde.junit.runtime":
					//				case "org.eclipse.pde.launching":
					//				case "org.eclipse.pde.runtime":
					//				case "org.eclipse.pde.ua.core":
					//				case "org.eclipse.pde.ua.ui":
					//				case "org.eclipse.pde.ui":
					//				case "org.eclipse.pde.ui.templates":
					//				case "org.eclipse.platform.doc.isv":
				case "org.eclipse.platform.doc.user":
				case "org.eclipse.platform":
				case "org.eclipse.rcp":
				case "org.eclipse.rcp.configuration":
				case "org.eclipse.search":
				case "org.eclipse.swt.win32.win32.x86_64":
				case "org.eclipse.swt":
				case "org.eclipse.team.core":
					//				case "org.eclipse.team.cvs.core":
					//				case "org.eclipse.team.cvs.ssh2":
					//				case "org.eclipse.team.cvs.ui":
				case "org.eclipse.team.ui":
				case "org.eclipse.text":
				case "org.eclipse.ui.browser":
				case "org.eclipse.ui.cheatsheets":
				case "org.eclipse.ui.console":
				case "org.eclipse.ui.editors":
				case "org.eclipse.ui.externaltools":
				case "org.eclipse.ui.forms":
				case "org.eclipse.ui.ide.application":
				case "org.eclipse.ui.ide":
				case "org.eclipse.ui.intro.universal":
				case "org.eclipse.ui.intro":
				case "org.eclipse.ui.navigator.resources":
				case "org.eclipse.ui.navigator":
				case "org.eclipse.ui.net":
				case "org.eclipse.ui.themes":
					//				case "org.eclipse.ui.trace":
					//				case "org.eclipse.ui.views.log":
				case "org.eclipse.ui.views.properties.tabbed":
				case "org.eclipse.ui.views":
				case "org.eclipse.ui.win32":
				case "org.eclipse.ui.workbench.texteditor":
				case "org.eclipse.ui.workbench":
				case "org.eclipse.ui":
				case "org.eclipse.update.configurator":
					//				case "org.eclipse.wst.common.core":
					//				case "org.eclipse.wst.common.emf":
					//				case "org.eclipse.wst.common.environment":
					//				case "org.eclipse.wst.common.frameworks.ui":
					//				case "org.eclipse.wst.common.frameworks":
					//				case "org.eclipse.wst.common.project.facet.core":
					//				case "org.eclipse.wst.common.ui":
					//				case "org.eclipse.wst.common.uriresolver":
					//				case "org.eclipse.wst.sse.core":
					//				case "org.eclipse.wst.sse.ui":
					//				case "org.eclipse.wst.validation.ui":
					//				case "org.eclipse.wst.validation":
					//				case "org.eclipse.wst.xml.core":
					//				case "org.eclipse.wst.xml.ui":
					//				case "org.eclipse.wst.xsd.core":
					//				case "org.eclipse.xsd":
					//				case "org.hamcrest.core":
					//				case "org.junit":
					//				case "org.nodeclipse.pluginslist.core":
					//				case "org.objectweb.asm.tree":
					//				case "org.objectweb.asm":
				case "org.sat4j.core":
				case "org.sat4j.pb":
					//				case "org.slf4j.api":
					//				case "org.springsource.ide.eclipse.commons.core":
					//				case "org.springsource.ide.eclipse.commons.frameworks.core":
					//				case "org.springsource.ide.eclipse.commons.livexp":
					//				case "org.springsource.ide.eclipse.commons.quicksearch":
				case "org.w3c.css.sac":
				case "org.w3c.dom.events":
				case "org.w3c.dom.smil":
				case "org.w3c.dom.svg":
					//				case "winterwell.markdown":
					continue;
				default:
			}
			if(plugin == configJrePluginPath) {
				//addConfigJreUnit(lines);
			} else if(plugin == jrePluginPath) {
				//addJreUnit(lines);
			} else if(plugin == platformIDEPath) {
				iuPropertiesOpen(lines, replaceAll, platformFeature.version);
				propertiesOpen(lines, 2);
				property(lines, "org.eclipse.equinox.p2.internal.inclusion.rules", "STRICT");
				property(lines, "org.eclipse.equinox.p2.type.root", "true");
				propertiesClose(lines);
				iuPropertiesClose(lines);
			} else if(plugin == platformIDEExecutablePath) {
				Feature feature2 = platformFeature;
				iuPropertiesOpen(lines, replaceAll, feature2.version);
				propertiesOpen(lines, 1);
				String key =
					"unzipped|"
					+ "@artifact|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse";
				String value =
					"/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/eclipse.exe|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/eclipsec.exe|";
				property(lines, key, value);
				propertiesClose(lines);
				iuPropertiesClose(lines);
			} else if(plugin == platformIDEExecutableEclipsePath) {
				//				Feature feature = features.get(plugin.resolveSibling("org.eclipse.platform.feature.group"));
				//				addPlatformIDEExecutableEclipse(lines, feature);
			} else if(plugin == rcpConfiguration_rootPluginPath) {
				iuPropertiesOpen(lines, replaceAll, platformFeature.version);
				propertiesOpen(lines, 1);
				String key =
					"unzipped|"
					+ "@artifact|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse";
				String value =
					"/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/eclipse.exe|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/eclipsec.exe|";
				property(lines, key, value);
				propertiesClose(lines);
				iuPropertiesClose(lines);
			} else if(plugin == platform_rootPluginPath) {
				iuPropertiesOpen(lines, replaceAll, platformFeature.version);
				propertiesOpen(lines, 1);
				String key =
					"unzipped|"
					+ "@artifact|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse";
				String value =
					"/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/notice.html|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/.eclipseproduct|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/epl-v10.html|";
				property(lines, key, value);
				propertiesClose(lines);
				iuPropertiesClose(lines);
			} else if(plugin == rcp_rootPluginPath) {
				iuPropertiesOpen(lines, replaceAll, platformFeature.version);
				propertiesOpen(lines, 1);
				String key =
					"unzipped|"
					+ "@artifact|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse";
				String value =
					"/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/readme|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/readme/readme_eclipse.html|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/epl-v10.html|"
					+ "/opt/public/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/"
					+ "eclipse.platform.releng.tychoeclipsebuilder/platform/target/products/"
					+ "org.eclipse.platform.ide/win32/win32/x86_64/eclipse"
					+ "/notice.html|";
				property(lines, key, value);
				propertiesClose(lines);
				iuPropertiesClose(lines);
			} else if(Tooling.has(plugin)) {
				//performTooling(lines, Tooling.get(plugin), features, versionsStore);
			} else if(tooling.containsKey(plugin)) {
				//lines.addAll(tooling.get(plugin));
			} else if(features.containsKey(plugin)) {
				Feature someFeature = features.get(plugin);
				iuPropertiesOpen(lines, someFeature.id, someFeature.version);
				propertiesOpen(lines, 2);
				property(lines, "org.eclipse.equinox.p2.internal.inclusion.rules", "STRICT");
				property(lines, "org.eclipse.equinox.p2.type.root", "true");
				propertiesClose(lines);
				iuPropertiesClose(lines);
			} else {
				HashMap<String, String> manifest = pluginToManifest.get(plugin);
				String bsn = manifest.get("Bundle-SymbolicName");
				String id = bsn.split(";")[0].trim();
				String version = manifest.get("Bundle-Version");
				iuPropertiesOpen(lines, id, version);
				propertiesOpen(lines, 2);
				property(lines, "org.eclipse.equinox.p2.internal.inclusion.rules", "STRICT");
				property(lines, "org.eclipse.equinox.p2.type.root", "true");
				propertiesClose(lines);
				iuPropertiesClose(lines);
			}
		}
		iusPropertiesClose(lines);
		lines.addAll("</profile>", "");
		String profileGz = String.join("\n", lines);
		return profileGz.getBytes(UTF8);
	}
	private static List<String> replaceVersions(
		Path pluginPath,
		Plugin equinoxLauncherPlugin,
		Plugin equinoxCommonPlugin,
		Plugin equinoxDSPlugin,
		Plugin equinoxEventPlugin,
		Plugin equinoxP2ReconcilerDropinsPlugin,
		Plugin equinoxSimpleconfiguratorPlugin,
		Plugin updateConfiguratorPlugin) {
		List<String> list =
			tooling.get(pluginPath).replaceAll(
				s -> s.replace("1.3.0.v20140415-2008", equinoxLauncherPlugin.version).replace(
					"1.3.100.v20140115-1647",
					equinoxEventPlugin.version).replace("1.1.200.v20140603-1326", "1.1.200.qualifier").replace(
					"1.1.200.v20131119-0908",
					equinoxP2ReconcilerDropinsPlugin.version).replace(
					"1.1.0.v20131217-1203",
					equinoxSimpleconfiguratorPlugin.version).replace(
					"3.3.300.v20140518-1928",
					updateConfiguratorPlugin.version).replace("3.6.200.v20140819-0835", equinoxCommonPlugin.version).replace(
					"1.4.200.v20131126-2331",
					equinoxDSPlugin.version));
		for(String string : list) {
			if(string.contains(".v20")) {
				throw new IllegalStateException("Placeholder " + string + " still in " + pluginPath);
			}
		}
		return list;
	}
	private static void iuPropertiesClose(ArrayList<String> lines) {
		lines.add("    </iuProperties>");
	}
	private static void iuPropertiesOpen(ArrayList<String> lines, String id, String version) {
		lines.add("    <iuProperties id='" + id + "' version='" + version + "'>");
	}
	private static void iusPropertiesClose(ArrayList<String> lines) {
		lines.add("  </iusProperties>");
	}
	private static void iusPropertiesOpen(ArrayList<String> lines, int size) {
		lines.add("  <iusProperties size='" + size + "'>");
	}
	private static void unitsClose(ArrayList<String> lines) {
		lines.add("  </units>");
	}
	private static void unitsOpen(ArrayList<String> lines, int size) {
		lines.add("  <units size='" + size + "'>");
	}

	private static enum Tooling {
		TOOLING_ORG_ECLIPSE_UPDATE("tooling.org.eclipse.update", "org.eclipse.update"),
		TOOLINGORG_ECLIPSE_PLATFORM_IDE_APPLICATION("toolingorg.eclipse.platform.ide.application", "org.eclipse.platform"),
		TOOLINGORG_ECLIPSE_PLATFORM_IDE_CONFIG_WIN32_WIN32_X86_64("toolingorg.eclipse.platform.ide.config.win32.win32.x86_64", "org.eclipse.platform"),
		TOOLINGORG_ECLIPSE_PLATFORM_IDE_CONFIGURATION("toolingorg.eclipse.platform.ide.configuration", "org.eclipse.platform"),
		TOOLINGORG_ECLIPSE_PLATFORM_IDE_EXECUTABLE_WIN32_WIN32_X86_64("toolingorg.eclipse.platform.ide.executable.win32.win32.x86_64", "org.eclipse.platform"),
		TOOLINGORG_ECLIPSE_PLATFORM_IDE_INI_WIN32_WIN32_X86_64("toolingorg.eclipse.platform.ide.ini.win32.win32.x86_64", "org.eclipse.platform"),
		TOOLINGWIN32_WIN32_X86_64ORG_ECLIPSE_CORE_RUNTIME("toolingwin32.win32.x86_64org.eclipse.core.runtime", "org.eclipse.platform"),
		TOOLINGORG_ECLIPSE_PLATFORM_CONFIGURATION("toolingorg.eclipse.platform.configuration", "org.eclipse.platform");
		Tooling(String name, String source) {
			this.name = name;
			versionSource = source;
		}
		public static Tooling get(Path plugin) {
			for(Tooling tool : values()) {
				if(tool.name.equals(plugin.getFileName().toString())) {
					return tool;
				}
			}
			return null;
		}
		public static boolean has(Path plugin) {
			return get(plugin) != null;
		}

		String name;
		String versionSource;
	}

	private static void performTooling(
		ArrayList<String> lines,
		Tooling tool,
		HashMap<Path, Feature> features,
		HashMap<String, String> versionsStore,
		Plugin equinoxLauncherPlugin) {
		Path path = fromZipPath("features/org.eclipse.platform.feature.group");
		Feature feature = features.get(path);
		tooling(lines, tool, feature, versionsStore, equinoxLauncherPlugin);
	}
	private static void tooling(
		ArrayList<String> lines,
		Tooling tool,
		Feature platformFeature,
		HashMap<String, String> versions,
		Plugin equinoxLauncherPlugin) {
		String version = platformFeature.version;
		String range = inclusive(version);
		switch(tool) {
			case TOOLING_ORG_ECLIPSE_UPDATE:
				unitOpen(lines, "tooling.org.eclipse.update.feature.default", "1.0.0", false);
				hostRequirementsOpen(lines, 1);
				required(lines, "org.eclipse.equinox.p2.eclipse.type", "feature", "0.0.0", true, false, true);
				hostRequirementsClose(lines);
				propertiesOpen(lines, 1);
				property(lines, "org.eclipse.equinox.p2.type.fragment", "true");
				propertiesClose(lines);
				providesOpen(lines, 2);
				provided(lines, "org.eclipse.equinox.p2.iu", "tooling.org.eclipse.update.feature.default", "1.0.0");
				provided(lines, "org.eclipse.equinox.p2.flavor", "tooling", "1.0.0");
				providesClose(lines);
				requiresOpen(lines, 1);
				required(lines, "org.eclipse.equinox.p2.eclipse.type", "feature", "0.0.0", true, false, true);
				requiresClose(lines);
				filter(lines, "(org.eclipse.update.install.features=true)");
				touchpoint(lines, "null", "0.0.0");
				touchpointDataOpen(lines, 1);
				instructionsOpen(lines, 2);
				instruction(
					lines,
					"install",
					"installFeature(feature:${artifact},featureId:default,featureVersion:default)");
				instruction(
					lines,
					"uninstall",
					"uninstallFeature(feature:${artifact},featureId:default,featureVersion:default)");
				instructionsClose(lines);
				touchpointDataClose(lines);
				unitClose(lines);
				break;
			case TOOLINGORG_ECLIPSE_PLATFORM_CONFIGURATION:
				unitOpen(lines, "toolingorg.eclipse.platform.configuration", "1.0.0", false);
				providesOpen(lines, 1);
				provided(lines, "toolingorg.eclipse.platform.configuration", "1.0.0");
				providesClose(lines);
				filter(lines, "(!(osgi.os=macosx))");
				touchpoint(lines, "org.eclipse.equinox.p2.osgi", "1.0.0");
				touchpointDataOpen(lines, 1);
				instructionsOpen(lines, 2);
				instruction(
					lines,
					"unconfigure",
					"setProgramProperty(propName:osgi.instance.area.default,propValue:);remove(targetDir:${installFolder},linkName:eclipse);");
				instruction(
					lines,
					"configure",
					"setProgramProperty(propName:osgi.instance.area.default,propValue:@user.home/workspace);");
				instructionsClose(lines);
				touchpointDataClose(lines);
				unitClose(lines);
				break;
			case TOOLINGORG_ECLIPSE_PLATFORM_IDE_APPLICATION:
				unitOpen(lines, "toolingorg.eclipse.platform.ide.application", version);
				providesOpen(lines, 1);
				provided(lines, "toolingorg.eclipse.platform.ide.application", version);
				providesClose(lines);
				requiresOpen(lines, 62);
				writeRequires(lines, platformFeature, equinoxLauncherPlugin);
				requiresClose(lines);
				touchpoint(lines, "null", "0.0.0");
				unitClose(lines);
				break;
			case TOOLINGORG_ECLIPSE_PLATFORM_IDE_CONFIG_WIN32_WIN32_X86_64:
				unitOpen(lines, "toolingorg.eclipse.platform.ide.config.win32.win32.x86_64", version, false);
				providesOpen(lines, 2);
				provided(
					lines,
					"org.eclipse.equinox.p2.iu",
					"toolingorg.eclipse.platform.ide.config.win32.win32.x86_64",
					version);
				provided(lines, "toolingorg.eclipse.platform.ide", "org.eclipse.platform.ide.config", version);
				providesClose(lines);
				filter(lines, "(&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))");
				touchpoint(lines, "org.eclipse.equinox.p2.osgi", "1.0.0");
				touchpointDataOpen(lines, 1);
				instructionsOpen(lines, 2);
				instruction(
					lines,
					"unconfigure",
					"setProgramProperty(propName:eclipse.buildId,propValue:);setProgramProperty(propName:osgi.splashPath,propValue:);setProgramProperty(propName:eclipse.application,propValue:);setProgramProperty(propName:eclipse.product,propValue:);setProgramProperty(propName:osgi.bundles.defaultStartLevel,propValue:);");
				instruction(
					lines,
					"configure",
					"setProgramProperty(propName:eclipse.buildId,propValue:" +
					version +
					");setProgramProperty(propName:osgi.splashPath,propValue:platform${#58}/base/plugins/org.eclipse.platform);setProgramProperty(propName:eclipse.application,propValue:org.eclipse.ui.ide.workbench);setProgramProperty(propName:eclipse.product,propValue:org.eclipse.platform.ide);setProgramProperty(propName:osgi.bundles.defaultStartLevel,propValue:4);");
				instructionsClose(lines);
				touchpointDataClose(lines);
				unitClose(lines);
				break;
			case TOOLINGORG_ECLIPSE_PLATFORM_IDE_CONFIGURATION:
				unitOpen(lines, "toolingorg.eclipse.platform.ide.configuration", version);
				providesOpen(lines, 1);
				provided(lines, "toolingorg.eclipse.platform.ide.configuration", version);
				providesClose(lines);
				requiresOpen(lines, 135);
				toolingRequires(lines, range, "x86_64", "linux", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "ia64", "hpux", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "s390", "linux", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "x86", "linux", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "ia64", "hpux", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "ia64", "hpux", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "s390x", "linux", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "s390x", "linux", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "sparc", "solaris", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "x86_64", "macosx", "cocoa", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "x86", "solaris", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "ppc", "aix", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "ppc64", "aix", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "ppc", "aix", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "x86", "linux", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "ppc", "aix", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "x86", "win32", "win32", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "x86_64", "linux", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "ia64", "hpux", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "ppc", "linux", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "x86", "solaris", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "x86_64", "macosx", "cocoa", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "ppc", "aix", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "ppc", "linux", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "x86", "solaris", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "x86_64", "win32", "win32", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "sparc", "solaris", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "x86", "linux", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "s390", "linux", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "s390x", "linux", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "x86_64", "linux", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "ppc64", "linux", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "ppc", "linux", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "ppc", "linux", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "s390", "linux", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "x86_64", "win32", "win32", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "x86", "linux", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "x86", "linux", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "ppc64le", "linux", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "x86_64", "win32", "win32", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "ppc64", "aix", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "x86_64", "linux", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "ppc64", "aix", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "s390x", "linux", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "x86_64", "win32", "win32", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "ppc64le", "linux", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "ia64", "hpux", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "ppc64", "linux", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "x86_64", "macosx", "cocoa", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "x86_64", "linux", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "x86_64", "linux", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "ppc64le", "linux", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "ppc64le", "linux", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "x86", "win32", "win32", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "ia64", "hpux", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "ppc64", "linux", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "x86", "win32", "win32", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "s390", "linux", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "sparc", "solaris", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "s390x", "linux", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "x86_64", "win32", "win32", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "s390", "linux", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "s390", "linux", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "x86", "linux", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "x86_64", "linux", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "sparc", "solaris", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "sparc", "solaris", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "x86_64", "win32", "win32", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "x86_64", "macosx", "cocoa", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "ppc", "aix", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "ppc", "linux", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "sparc", "solaris", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "ppc64", "aix", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "ppc64le", "linux", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "x86", "solaris", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "ppc", "aix", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "ia64", "hpux", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "x86", "win32", "win32", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "ppc", "linux", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "ppc64", "linux", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "ppc64le", "linux", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "ia64", "hpux", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "x86", "win32", "win32", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "x86_64", "macosx", "cocoa", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "ppc64", "aix", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "sparc", "solaris", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "x86", "win32", "win32", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "ppc64", "linux", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "x86_64", "macosx", "cocoa", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "ppc", "linux", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "ppc64", "aix", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "x86_64", "win32", "win32", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "x86", "solaris", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "x86", "solaris", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "ppc64", "linux", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "x86", "win32", "win32", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "sparc", "solaris", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "x86", "linux", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "x86", "solaris", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "x86_64", "linux", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "ppc", "aix", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "s390x", "linux", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "s390", "linux", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "ppc64", "linux", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "x86", "win32", "win32", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "s390x", "linux", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "ppc", "linux", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "ppc", "aix", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "x86_64", "linux", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "x86_64", "macosx", "cocoa", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "ppc64le", "linux", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "ppc64le", "linux", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "ppc64le", "linux", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "ppc", "aix", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "s390", "linux", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "x86", "win32", "win32", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "x86_64", "win32", "win32", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "x86_64", "macosx", "cocoa", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "ppc64", "aix", "gtk", "org.eclipse.equinox.simpleconfigurator");
				toolingRequires(lines, range, "ppc64", "aix", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "ppc", "linux", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "s390x", "linux", "gtk", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "ppc64", "aix", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "s390", "linux", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "sparc", "solaris", "gtk", "org.eclipse.equinox.common");
				toolingRequires(lines, range, "s390x", "linux", "gtk", "org.eclipse.equinox.p2.reconciler.dropins");
				toolingRequires(lines, range, "ia64", "hpux", "gtk", "org.eclipse.equinox.ds");
				toolingRequires(lines, range, "x86", "solaris", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "x86", "solaris", "gtk", "org.eclipse.update.configurator");
				toolingRequires(lines, range, "ppc64", "linux", "gtk", "org.eclipse.platform.ide.config.");
				toolingRequires(lines, range, "x86", "linux", "gtk", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "x86_64", "macosx", "cocoa", "org.eclipse.platform.ide.ini.");
				toolingRequires(lines, range, "x86_64", "win32", "win32", "org.eclipse.core.runtime");
				toolingRequires(lines, range, "x86", "linux", "gtk", "org.eclipse.equinox.event");
				toolingRequires(lines, range, "ppc64", "linux", "gtk", "org.eclipse.equinox.ds");
				requiresClose(lines);
				touchpoint(lines, "null", "0.0.0");
				unitClose(lines);
				break;
			case TOOLINGORG_ECLIPSE_PLATFORM_IDE_EXECUTABLE_WIN32_WIN32_X86_64:
				unitOpen(lines, "toolingorg.eclipse.platform.ide.executable.win32.win32.x86_64", version, false);
				hostRequirementsOpen(lines, 1);
				required(lines, "org.eclipse.platform.ide.executable.win32.win32.x86_64", range);
				hostRequirementsClose(lines);
				propertiesOpen(lines, 1);
				property(lines, "org.eclipse.equinox.p2.type.fragment", "true");
				propertiesClose(lines);
				providesOpen(lines, 1);
				provided(lines, "toolingorg.eclipse.platform.ide.executable.win32.win32.x86_64", version);
				providesClose(lines);
				requiresOpen(lines, 1);
				required(lines, "org.eclipse.platform.ide.executable.win32.win32.x86_64", range);
				requiresClose(lines);
				filter(lines, "(&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))");
				touchpoint(lines, "org.eclipse.equinox.p2.native", "1.0.0");
				touchpointDataOpen(lines, 1);
				instructionsOpen(lines, 2);
				instruction(lines, "install", "unzip(source:@artifact, target:${installFolder});");
				instruction(lines, "uninstall", "cleanupzip(source:@artifact, target:${installFolder});");
				instructionsClose(lines);
				touchpointDataClose(lines);
				unitClose(lines);
				break;
			case TOOLINGORG_ECLIPSE_PLATFORM_IDE_INI_WIN32_WIN32_X86_64:
				unitOpen(lines, "toolingorg.eclipse.platform.ide.ini.win32.win32.x86_64", version, false);
				providesOpen(lines, 2);
				provided(lines, "toolingorg.eclipse.platform.ide.ini.win32.win32.x86_64", version);
				provided(lines, "toolingorg.eclipse.platform.ide", "org.eclipse.platform.ide.ini", version);
				providesClose(lines);
				filter(lines, "(&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))");
				touchpoint(lines, "org.eclipse.equinox.p2.osgi", "1.0.0");
				touchpointDataOpen(lines, 1);
				instructionsOpen(lines, 2);
				instruction(
					lines,
					"unconfigure",
					"removeJvmArg(jvmArg:-Xms40m);removeJvmArg(jvmArg:-Xmx512m);removeProgramArg(programArg:-showsplash);removeProgramArg(programArg:org.eclipse.platform);removeProgramArg(programArg:--launcher.XXMaxPermSize);removeProgramArg(programArg:256m);removeProgramArg(programArg:--launcher.defaultAction);removeProgramArg(programArg:openFile);removeProgramArg(programArg:--launcher.appendVmargs);");
				instruction(
					lines,
					"configure",
					"addJvmArg(jvmArg:-Xms40m);addJvmArg(jvmArg:-Xmx512m);addProgramArg(programArg:-showsplash);addProgramArg(programArg:org.eclipse.platform);addProgramArg(programArg:--launcher.XXMaxPermSize);addProgramArg(programArg:256m);addProgramArg(programArg:--launcher.defaultAction);addProgramArg(programArg:openFile);addProgramArg(programArg:--launcher.appendVmargs);");
				instructionsClose(lines);
				touchpointDataClose(lines);
				unitClose(lines);
				break;
			case TOOLINGWIN32_WIN32_X86_64ORG_ECLIPSE_CORE_RUNTIME:
				unitOpen(lines, "toolingwin32.win32.x86_64org.eclipse.core.runtime", version, false);
				hostRequirementsOpen(lines, 2);
				required(
					lines,
					"osgi.bundle",
					"org.eclipse.core.runtime",
					versions.get("org.eclipse.core.runtime"),
					false,
					true);
				required(lines, "org.eclipse.equinox.p2.eclipse.type", "bundle", "[1.0.0,2.0.0)", false, false);
				hostRequirementsClose(lines);
				propertiesOpen(lines, 1);
				property(lines, "org.eclipse.equinox.p2.type.fragment", "true");
				propertiesClose(lines);
				providesOpen(lines, 2);
				provided(
					lines,
					"org.eclipse.equinox.p2.iu",
					"toolingwin32.win32.x86_64org.eclipse.core.runtime",
					version);
				provided(lines, "org.eclipse.equinox.p2.flavor", "toolingwin32.win32.x86_64", "1.0.0");
				providesClose(lines);
				requiresOpen(lines, 2);
				required(
					lines,
					"osgi.bundle",
					"org.eclipse.core.runtime",
					versions.get("org.eclipse.core.runtime"),
					false,
					true);
				required(lines, "org.eclipse.equinox.p2.eclipse.type", "bundle", "[1.0.0,2.0.0)", false, false);
				requiresClose(lines);
				filter(lines, "(&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))");
				touchpoint(lines, "null", "0.0.0");
				touchpointDataOpen(lines, 1);
				instructionsOpen(lines, 4);
				instruction(lines, "install", "installBundle(bundle:${artifact})");
				instruction(lines, "uninstall", "uninstallBundle(bundle:${artifact})");
				instruction(lines, "unconfigure", "markStarted(started: false);");
				instruction(lines, "configure", "markStarted(started: true);");
				instructionsClose(lines);
				touchpointDataClose(lines);
				unitClose(lines);
				break;
			default:
				lines.add("");
				lines.add("");
				break;
		}
	}
	private static void toolingRequires(
		ArrayList<String> lines,
		String range,
		String arch,
		String os,
		String ws,
		String id) {
		String filter = "(&amp;(osgi.arch=" + arch + ")(osgi.os=" + os + ")(osgi.ws=" + ws + "))";
		if(id.endsWith("ini.") || id.endsWith("config.")) {
			required(lines, "tooling" + id + ws + "." + os + "." + arch, range, filter);
		} else {
			required(lines, "tooling" + ws + "." + os + "." + arch + id, range, filter);
		}
	}
	private static void writeRequires(ArrayList<String> lines, Feature platform, Plugin launcher) {
		String platformID = platform.id;
		String platformV = platform.version;
		String launcherV102 = "1.0.200.qualifier";
		String launcherID = launcher.id;
		String launcherV112 = "1.1.200.qualifier";
		String launcherV111 = "1.1.100.qualifier";
		String launcherV101 = "1.0.100.qualifier";
		String toolingPlatformExecutable = "tooling" + platformID + ".ide.executable";
		String toolingLaucher = "tooling" + launcherID;
		String platformExecutable = platformID + ".ide.executable";
		require2(lines, "gtk", "linux", "ppc64le", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "ppc64le", toolingLaucher, "", launcherV102);
		require2(lines, "gtk", "hpux", "ia64", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "linux", "s390", toolingLaucher, "", launcherV112);
		require2(lines, "gtk", "aix", "ppc", toolingLaucher, "", launcherV111);
		require2(lines, "gtk", "linux", "x86_64", platformExecutable, "", platformV);
		require2(lines, "gtk", "aix", "ppc", toolingPlatformExecutable, "", platformV);
		required(lines, MAC_BUNDLED_EXE, inclusive(platformV), MAC_BUNDLED_FILTER);
		require2(lines, "gtk", "linux", "ppc", platformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "ppc64le", platformExecutable, "", platformV);
		require2(lines, "cocoa", "macosx", "x86_64", toolingLaucher, "", launcherV112);
		require2(lines, "gtk", "linux", "s390x", platformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "s390x", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "x86_64", toolingLaucher, "", launcherV112);
		require2(lines, "gtk", "linux", "ppc", toolingLaucher, "", launcherV112);
		require2(lines, "win32", "win32", "x86_64", platformExecutable, "", platformV);
		require2(lines, "win32", "win32", "x86", toolingLaucher, "", launcherV112);
		require2(lines, "gtk", "linux", "s390x", platformExecutable, ".eclipse", platformV);
		required(lines, "tooling" + launcherID, inclusive(launcher.version));
		require2(lines, "win32", "win32", "x86", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "aix", "ppc", platformExecutable, "", platformV);
		require2(lines, "gtk", "aix", "ppc", platformExecutable, ".eclipse", platformV);
		require2(lines, "win32", "win32", "x86", platformExecutable, "", platformV);
		require2(lines, "gtk", "hpux", "ia64", toolingLaucher, "", launcherV101);
		require2(lines, "win32", "win32", "x86_64", toolingLaucher, "", launcherV112);
		require2(lines, "gtk", "linux", "ppc64", toolingLaucher, "", launcherV102);
		require2(lines, "gtk", "solaris", "sparc", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "aix", "ppc64", toolingLaucher, "", launcherV111);
		require2(lines, "gtk", "linux", "x86_64", toolingPlatformExecutable, "", platformV);
		require2(lines, "cocoa", "macosx", "x86_64", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "linux", "ppc", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "hpux", "ia64", platformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "s390", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "solaris", "sparc", toolingLaucher, "", launcherV112);
		require2(lines, "win32", "win32", "x86_64", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "s390", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "aix", "ppc64", platformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "x86", platformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "ppc", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "solaris", "x86", toolingLaucher, "", launcherV112);
		require2(lines, "gtk", "linux", "x86_64", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "solaris", "sparc", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "ppc64", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "aix", "ppc64", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "linux", "ppc64le", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "solaris", "x86", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "ppc64", toolingPlatformExecutable, "", platformV);
		require2(lines, "win32", "win32", "x86_64", platformExecutable, ".eclipse", platformV);
		require2(lines, "gtk", "linux", "x86", toolingLaucher, "", launcherV112);
		require2(lines, "gtk", "linux", "s390", platformExecutable, "", platformV);
		require2(lines, "gtk", "aix", "ppc64", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "x86", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "linux", "x86", platformExecutable, ".eclipse", platformV);
		require2(lines, "win32", "win32", "x86", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "hpux", "ia64", toolingPlatformExecutable, "", platformV);
		require2(lines, "gtk", "solaris", "x86", platformExecutable, ".eclipse", platformV);
		required(lines, MAC_UNBUNDLED_EXE, inclusive(platformV), MAC_UNBUNDLED_FILTER);
		require2(lines, "gtk", "linux", "s390x", toolingLaucher, "", launcherV112);
		require2(lines, "gtk", "linux", "ppc64", platformExecutable, "", platformV);
		require2(lines, "cocoa", "macosx", "x86_64", platformExecutable, "", platformV);
		require2(lines, "gtk", "solaris", "x86", platformExecutable, "", platformV);
		require2(lines, "gtk", "solaris", "sparc", platformExecutable, "", platformV);
	}
	private static void require2(
		ArrayList<String> lines,
		String ws,
		String os,
		String arch,
		String id,
		String suffix,
		String ver) {
		required(lines, id + "." + ws + "." + os + "." + arch + suffix, inclusive(ver), "(&amp;(osgi.arch=" +
		arch +
		")(osgi.os=" +
		os +
		")(osgi.ws=" +
		ws +
		"))");
	}
	private static void hostRequirementsClose(ArrayList<String> lines) {
		lines.add("      </hostRequirements>");
	}
	private static void hostRequirementsOpen(ArrayList<String> lines, int size) {
		lines.add("      <hostRequirements size='" + size + "'>");
	}
	private static void addPlatformIDEExecutableEclipse(ArrayList<String> lines, Feature feature) {
		String ver = feature.version;
		String id = "org.eclipse.platform.ide.executable.win32.win32.x86_64.eclipse";
		unitOpen(lines, id, ver, false);
		providesOpen(lines, 1);
		provided(lines, id, ver);
		providesClose(lines);
		filter(lines, "(&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))");
		touchpoint(lines, "org.eclipse.equinox.p2.osgi", "1.0.0");
		touchpointDataOpen(lines, 1);
		instructionsOpen(lines, 2);
		instruction(lines, "unconfigure", "setLauncherName()");
		instruction(lines, "configure", "setLauncherName(name:eclipse)");
		instructionsClose(lines);
		touchpointDataClose(lines);
		unitClose(lines);
	}
	private static void unitOpen(ArrayList<String> lines, String id, String version, boolean singleton) {
		lines.add("    <unit id='" +
		id +
		"' version='" +
		version +
		"'" +
		(!singleton ? " singleton='false'" : "") +
		">");
	}
	private static void addPlatformIDEExecutable(ArrayList<String> lines, Feature feature) {
		String ver = feature.version;
		String id = "org.eclipse.platform.ide.executable.win32.win32.x86_64";
		String filter = "(&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))";
		unitOpen(lines, id, ver);
		providesOpen(lines, 2);
		provided(lines, "org.eclipse.equinox.p2.iu", id, ver);
		provided(lines, "toolingorg.eclipse.platform.ide", "org.eclipse.platform.ide.executable", ver);
		providesClose(lines);
		requiresOpen(lines, 1);
		required(lines, "org.eclipse.equinox.launcher.win32.win32.x86_64", "0.0.0", filter);
		requiresClose(lines);
		filter(lines, filter);
		artifactsOpen(lines, 1);
		artifact(lines, "binary", id, ver);
		artifactsClose(lines);
		touchpoint(lines, "org.eclipse.equinox.p2.native", "1.0.0");
		unitClose(lines);
	}
	private static void filter(ArrayList<String> lines, String filter) {
		lines.add("      <filter>");
		lines.add("        " + filter);
		lines.add("      </filter>");
	}
	private static void provided(ArrayList<String> lines, String namespace, String name, String version) {
		lines.add("        <provided namespace='" + namespace + "' name='" + name + "' version='" + version + "'/>");
	}
	private static void artifact(ArrayList<String> lines, String classifier, String id, String version) {
		lines.add("        <artifact classifier='" + classifier + "' id='" + id + "' version='" + version + "'/>");
	}
	private static void artifactsClose(ArrayList<String> lines) {
		lines.add("      </artifacts>");
	}
	private static void artifactsOpen(ArrayList<String> lines, int size) {
		lines.add("      <artifacts size='" + size + "'>");
	}
	private static void addPlatformIDE(
		ArrayList<String> lines,
		Feature platformIDE,
		Feature p2UserUI,
		Feature rcpConfiguration) {
		String ver = platformIDE.version;
		String license = platformIDE.properties.getOrDefault(platformIDE.license.replace("%", ""), platformIDE.license);
		String id = "org.eclipse.platform.ide";
		unitOpen(lines, id, ver);
		update(lines, id, exclusive("4.0.0", ver), "An update for 4.x generation Eclipse SDK.");
		propertiesOpen(lines, 3);
		property(lines, "org.eclipse.equinox.p2.name", "Eclipse Platform");
		property(lines, "org.eclipse.equinox.p2.type.product", "true");
		property(lines, "org.eclipse.equinox.p2.type.group", "true");
		propertiesClose(lines);
		providesOpen(lines, 1);
		provided(lines, id, ver);
		providesClose(lines);
		requiresOpen(lines, 13);
		required(lines, "tooling" + id + ".application", inclusive(ver));
		required(lines, "config.a.jre.javase", inclusive("1.6.0"));
		required(lines, "toolingorg.eclipse.platform.configuration", inclusive("1.0.0"), "(!(osgi.os=macosx))");
		required(
			lines,
			"toolingorg.eclipse.platform.configuration.gtk.linux.x86_64",
			inclusive("1.0.0"),
			"(&amp;(osgi.arch=x86_64)(osgi.os=linux)(osgi.ws=gtk))");
		required(
			lines,
			"tooling.org.eclipse.update.feature.default",
			inclusive("1.0.0"),
			"(org.eclipse.update.install.features=true)");
		required(lines, "org.eclipse.rcp.configuration.feature.group", inclusive(rcpConfiguration.version));
		required(lines, "tooling" + id + ".configuration", inclusive(ver));
		required(lines, "org.eclipse.platform.feature.group", inclusive(ver));
		required(lines, "toolingorg.eclipse.platform.configuration.macosx", inclusive("1.0.0"), "(osgi.os=macosx)");
		required(lines, "a.jre.javase", inclusive("1.6.0"));
		required(lines, "tooling.source.default", inclusive("1.0.0"));
		required(lines, "org.eclipse.equinox.p2.user.ui.feature.group", inclusive(p2UserUI.version));
		required(lines, "tooling.osgi.bundle.default", inclusive("1.0.0"));
		requiresClose(lines);
		touchpoint(lines, "org.eclipse.equinox.p2.osgi", "1.0.0");
		touchpointDataOpen(lines, 1);
		instructionsOpen(lines, 1);
		instruction(
			lines,
			"configure",
			"addRepository(type:0,location:http${#58}//download.eclipse.org/eclipse/updates/4.5,name:The Eclipse Project Updates);addRepository(type:1,location:http${#58}//download.eclipse.org/eclipse/updates/4.5,name:The Eclipse Project Updates);addRepository(type:0,location:http${#58}//download.eclipse.org/releases/mars,name:Mars);addRepository(type:1,location:http${#58}//download.eclipse.org/releases/mars,name:Mars);mkdir(path:${installFolder}/dropins);");
		instructionsClose(lines);
		touchpointDataClose(lines);
		licensesOpen(lines, 1);
		license(lines, "http://eclipse.org/legal/epl/notice.php", license);
		licensesClose(lines);
		unitClose(lines);
	}
	private static String inclusive(String s2) {
		return "[" + s2 + "," + s2 + "]";
	}
	private static void licensesClose(ArrayList<String> lines) {
		lines.add("      </licenses>");
	}
	private static void licensesOpen(ArrayList<String> lines, int size) {
		lines.add("      <licenses size='" + size + "'>");
	}
	private static void instruction(ArrayList<String> lines, String key, String action) {
		lines.add("          <instruction key='" + key + "'>");
		lines.add("            " + action);
		lines.add("          </instruction>");
	}
	private static void instructionsClose(ArrayList<String> lines) {
		lines.add("        </instructions>");
	}
	private static void touchpointDataClose(ArrayList<String> lines) {
		lines.add("      </touchpointData>");
	}
	private static void instructionsOpen(ArrayList<String> lines, int size) {
		lines.add("        <instructions size='" + size + "'>");
	}
	private static void touchpointDataOpen(ArrayList<String> lines, int size) {
		lines.add("      <touchpointData size='" + size + "'>");
	}
	private static void touchpoint(ArrayList<String> lines, String id, String version) {
		lines.add("      <touchpoint id='" + id + "' version='" + version + "'/>");
	}
	private static void required(ArrayList<String> lines, String name, String range) {
		lines.add("        <required namespace='org.eclipse.equinox.p2.iu' name='" + name + "' range='" + range + "'/>");
	}
	private static void required(ArrayList<String> lines, String name, String range, String filter) {
		lines.add("        <required namespace='org.eclipse.equinox.p2.iu' name='" + name + "' range='" + range + "'>");
		lines.add("          <filter>");
		lines.add("            " + filter);
		lines.add("          </filter>");
		lines.add("        </required>");
	}
	private static void requiresClose(ArrayList<String> lines) {
		lines.add("      </requires>");
	}
	private static void requiresOpen(ArrayList<String> lines, int size) {
		lines.add("      <requires size='" + size + "'>");
	}
	private static void provided(ArrayList<String> lines, String id, String version) {
		lines.add("        <provided namespace='org.eclipse.equinox.p2.iu' name='" +
		id +
		"' version='" +
		version +
		"'/>");
	}
	private static void providesClose(ArrayList<String> lines) {
		lines.add("      </provides>");
	}
	private static void providesOpen(ArrayList<String> lines, int size) {
		lines.add("      <provides size='" + size + "'>");
	}
	private static void property(ArrayList<String> lines, String key, String value) {
		lines.add(featureProperty(key, value));
	}
	private static void propertiesClose(ArrayList<String> lines) {
		lines.add("      </properties>");
	}
	private static void propertiesOpen(ArrayList<String> lines, int size) {
		lines.add("      <properties size='" + size + "'>");
	}
	private static void update(ArrayList<String> lines, String id, String range, String description) {
		lines.add("      <update id='" +
		id +
		"' range='" +
		range +
		"' severity='0' description='" +
		description +
		"'/>");
	}
	private static void update(ArrayList<String> lines, String id, String range) {
		lines.add("      <update id='" + id + "' range='" + range + "' severity='0'/>");
	}
	private static String exclusive(String v1, String v2) {
		return "[" + v1 + "," + v2 + ")";
	}
	private static void unitClose(ArrayList<String> lines) {
		lines.add("    </unit>");
	}
	private static void unitOpen(ArrayList<String> lines, String id, String version) {
		lines.add("    <unit id='" + id + "' version='" + version + "'>");
	}
	private static void addFeatureUnit(ArrayList<String> lines, Feature feature, String fileName) {
		boolean isGroup = fileName.endsWith(".feature.group");
		String suffix = isGroup ? ".feature.group" : ".feature.jar";
		String version = feature.version;
		String id = feature.id;
		if(isGroup) {
			unitOpen(lines, id + suffix, version, false);
			update(lines, id + suffix, "[0.0.0," + version + ")");
		} else {
			unitOpen(lines, id + suffix, version);
		}
		addFeatureUnitProperties(lines, feature, isGroup);
		providesOpen(lines, 2);
		provided(lines, id + suffix, version);
		if(isGroup) {
			provided(lines, "org.eclipse.equinox.p2.localization", "df_LT", "1.0.0");
		} else {
			provided(lines, "org.eclipse.equinox.p2.eclipse.type", "feature", "1.0.0");
			provided(lines, "org.eclipse.update.feature", id, version);
		}
		providesClose(lines);
		if(isGroup) {
			addFeatureUnitRequires(lines, feature);
			touchpoint(lines, "null", "0.0.0");
		} else {
			filter(lines, "(org.eclipse.update.install.features=true)");
			artifactsOpen(lines, 1);
			artifact(lines, "org.eclipse.update.feature", id, version);
			artifactsClose(lines);
			touchpoint(lines, "org.eclipse.equinox.p2.osgi", "1.0.0");
			touchpointDataOpen(lines, 1);
			instructionsOpen(lines, 1);
			instruction(lines, "zipped", "true");
			instructionsClose(lines);
			touchpointDataClose(lines);
		}
		licensesOpen(lines, 1);
		license(lines, feature.licenseURL, feature.license);
		licensesClose(lines);
		copyright(lines, feature.copyright, feature.copyrightURL);
		unitClose(lines);
	}
	private static void license(ArrayList<String> lines, String url, String license) {
		if(url.startsWith("%")) {
			url = "%25" + url.substring(1);
		}
		lines.add("        <license uri='" + url + "' url='" + url + "'>");
		lines.add("          " + wrap(license));
		lines.add("        </license>");
	}
	private static void copyright(ArrayList<String> lines, String copyright, String url) {
		if(url != null) {
			lines.add("      <copyright uri='" + wrap(url) + "' url='" + wrap(url) + "'>");
			lines.add("        " + wrap(copyright));
			lines.add("      </copyright>");
		} else {
			copyright(lines, copyright);
		}
	}
	private static void copyright(ArrayList<String> lines, String copyright) {
		lines.add("      <copyright>");
		lines.add("        " + wrap(copyright));
		lines.add("      </copyright>");
	}
	private static void addFeatureUnitRequires(ArrayList<String> lines, Feature feature) {
		boolean needsPlatformRoot = "org.eclipse.platform".equals(feature.id);
		boolean needsRCPRoot = "org.eclipse.rcp".equals(feature.id);
		boolean needsConfigurationRoots = "org.eclipse.rcp.configuration".equals(feature.id);
		int size = feature.plugins.size() + feature.includes.size() + feature.requiresImports.size() + 1;
		if(needsRCPRoot) {
			size++;
		}
		if(needsPlatformRoot) {
			size++;
		}
		if(needsConfigurationRoots) {
			size += 14;
		}
		requiresOpen(lines, size);
		for(Import required : feature.requiresImports) {
			String name = required.feature != null ? required.feature + ".feature.group" : required.plugin;
			String version = required.version;
			int nextMajor = Integer.parseInt(version.split("\\.")[0]) + 1;
			required(lines, name, "[" + version + "," + nextMajor + ".0.0)");
		}
		for(Feature.Plugin include : feature.includes) {
			addFeatureUnitRequired(lines, include);
		}
		for(Feature.Plugin plugin : feature.plugins) {
			addFeatureUnitRequired(lines, plugin);
		}
		String featureJar = feature.id + ".feature.jar";
		String range = inclusive(feature.version);
		required(lines, featureJar, range, "(org.eclipse.update.install.features=true)");
		if(needsConfigurationRoots) {
			required_root(lines, "cocoa", "macosx", "x86_64", range);
			required_root(lines, "gtk", "aix", "ppc64", range);
			required_root(lines, "gtk", "linux", "s390", range);
			required_root(lines, "gtk", "linux", "x86", range);
			required_root(lines, "gtk", "linux", "ppc64", range);
			required_root(lines, "gtk", "solaris", "x86", range);
			required_root(lines, "win32", "win32", "x86_64", range);
			required_root(lines, "win32", "win32", "x86", range);
			required_root(lines, "gtk", "linux", "x86_64", range);
			required_root(lines, "gtk", "aix", "ppc", range);
			required_root(lines, "gtk", "linux", "s390x", range);
			required_root(lines, "gtk", "solaris", "sparc", range);
			required_root(lines, "gtk", "linux", "ppc", range);
			required_root(lines, "gtk", "hpux", "ia64", range);
		}
		if(needsPlatformRoot || needsRCPRoot) {
			String rootID = feature.id + "_root";
			required(lines, rootID, range);
		}
		requiresClose(lines);
	}
	private static void required_root(ArrayList<String> lines, String ws, String os, String arch, String range) {
		String id = "org.eclipse.rcp.configuration_root." + ws + "." + os + "." + arch;
		String filter = "(&amp;(osgi.arch=" + arch + ")(osgi.os=" + os + ")(osgi.ws=" + ws + "))";
		required(lines, id, range, filter);
	}
	private static void addFeatureUnitRequired(ArrayList<String> lines, Feature.Plugin plugin) {
		String filter = "";
		int count = 0;
		if(plugin.arch != null) {
			filter += "(osgi.arch=" + plugin.arch + ")";
			count++;
		}
		if(plugin.os != null) {
			filter += "(osgi.os=" + plugin.os + ")";
			count++;
		}
		if(plugin.ws != null) {
			filter += "(osgi.ws=" + plugin.ws + ")";
			count++;
		}
		if(count > 1) {
			filter = "(&" + filter + ")";
		}
		String required =
			String.format(
				"        <required namespace='org.eclipse.equinox.p2.iu' name='%1$s' range='[%2$s,%2$s]'",
				plugin.id + (plugin.fragment ? ".feature.group" : ""),
				plugin.version);
		if(count == 0) {
			lines.add(required + "/>");
		} else {
			lines.add(required + ">");
			lines.add("          <filter>");
			lines.add("            " + wrap(filter));
			lines.add("          </filter>");
			lines.add("        </required>");
		}
	}
	private static void addFeatureUnitProperties(ArrayList<String> lines, Feature feature, boolean isGroup) {
		ArrayList<String> added = new ArrayList<>();
		added.add(featurePropertyP2("name", feature.label));
		added.add(featurePropertyP2("description", feature.description));
		added.add(featurePropertyP2("description.url", feature.descriptionURL));
		added.add(featurePropertyP2("provider", feature.providerName));
		if(isGroup) {
			added.add(featurePropertyP2("type.group", "true"));
		} else {
			added.add(featureProperty("org.eclipse.update.feature.plugin", feature.plugin));
		}
		if(feature.id.equals("org.eclipse.e4.rcp") ||
		feature.id.equals("org.eclipse.equinox.p2.core.feature") ||
		feature.id.equals("org.eclipse.equinox.p2.extras.feature") ||
		feature.id.equals("org.eclipse.equinox.p2.rcp.feature") ||
		feature.id.equals("org.eclipse.equinox.p2.user.ui") ||
		feature.id.equals("org.eclipse.help") ||
		feature.id.equals("org.eclipse.platform") ||
		feature.id.equals("org.eclipse.help") ||
		feature.id.equals("org.eclipse.rcp") ||
		feature.id.equals("org.eclipse.rcp.configuration")) {
			added.add(featurePropertyMaven("groupId", feature.mavenGroupID()));
			added.add(featurePropertyMaven("artifactId", feature.id));
			added.add(featurePropertyMaven("version", feature.mavenVersion()));
		}
		added.add(featurePropertyLT(feature, feature.label));
		added.add(featurePropertyLT(feature, feature.providerName));
		added.add(featurePropertyLT(feature, feature.description));
		added.add(featurePropertyLT(feature, feature.copyright));
		added.add(featurePropertyLT(feature, feature.license));
		added.removeIf(s -> s == null);
		int size = added.size();
		propertiesOpen(lines, size);
		lines.addAll(added);
		propertiesClose(lines);
	}
	private static String featurePropertyLT(Feature feature, String label) {
		if(label == null || label.startsWith("%") == false) {
			return null;
		}
		String key = label.substring(1);
		String value = feature.properties.get(key);
		if(value == null) {
			return null;
		}
		return String.format("        <property name='df_LT.%s' value='%s'/>", wrap(key), wrap(value.trim()));
	}
	private static String featureProperty(String key, String value) {
		if(value == null) {
			return null;
		}
		return String.format("        <property name='%s' value='%s'/>", wrap(key), wrap(value));
	}
	private static String featurePropertyP2(String key, String value) {
		if(value == null) {
			return null;
		}
		return String.format("        <property name='org.eclipse.equinox.p2.%s' value='%s'/>", wrap(key), wrap(value));
	}
	private static String featurePropertyMaven(String key, String value) {
		if(value == null) {
			return null;
		}
		return String.format("        <property name='maven-%s' value='%s'/>", wrap(key), wrap(value));
	}
	private static void addRcpConfigurationUnit(ArrayList<String> lines, Feature rcpConfiguration) {
		unitOpen(lines, "org.eclipse.rcp.configuration_root.win32.win32.x86_64", rcpConfiguration.version);
		providesOpen(lines, 1);
		provided(lines, "org.eclipse.rcp.configuration_root.win32.win32.x86_64", rcpConfiguration.version);
		providesClose(lines);
		filter(lines, "(&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))");
		artifactsOpen(lines, 1);
		artifact(lines, "binary", "org.eclipse.rcp.configuration_root.win32.win32.x86_64", rcpConfiguration.version);
		artifactsClose(lines);
		touchpoint(lines, "org.eclipse.equinox.p2.native", "1.0.0");
		touchpointDataOpen(lines, 2);
		instructionsOpen(lines, 2);
		instruction(lines, "install", "unzip(source:@artifact, target:${installFolder});");
		instruction(lines, "uninstall", "cleanupzip(source:@artifact, target:${installFolder});");
		instructionsClose(lines);
		instructionsOpen(lines, 1);
		instruction(lines, "install", "chmod(targetDir:${installFolder}, targetFile:eclipse.exe, permissions:755);");
		instructionsClose(lines);
		touchpointDataClose(lines);
		unitClose(lines);
	}
	private static void addBinary_RootUnit(ArrayList<String> lines, Plugin plugin) {
		String id = plugin.id + "_root";
		unitOpen(lines, id, plugin.version);
		providesOpen(lines, 1);
		provided(lines, id, plugin.version);
		providesClose(lines);
		artifactsOpen(lines, 1);
		artifact(lines, "binary", id, plugin.version);
		artifactsClose(lines);
		touchpoint(lines, "org.eclipse.equinox.p2.native", "1.0.0");
		touchpointDataOpen(lines, 1);
		instructionsOpen(lines, 2);
		instruction(lines, "install", "(source:@artifact, target:${installFolder});");
		instruction(lines, "uninstall", "cleanupzip(source:@artifact, target:${installFolder});");
		instructionsClose(lines);
		touchpointDataClose(lines);
		unitClose(lines);
	}
	private static String extractName(Path p, HashMap<Path, HashMap<String, String>> pluginToManifest) {
		HashMap<String, String> manifest = pluginToManifest.get(p);
		if(manifest == null) {
			return p.getFileName().toString();
		}
		String bsn = manifest.get("Bundle-SymbolicName");
		String id = bsn.split(";")[0].trim();
		return id;
	}
	private static String extractVersion(Path p, HashMap<Path, HashMap<String, String>> pluginToManifest) {
		HashMap<String, String> manifest = pluginToManifest.get(p);
		if(manifest == null) {
			return "0.0.0";
		}
		return manifest.get("Bundle-Version");
	}
	private static void addPluginUnit(
		Path plugin,
		ArrayList<String> lines,
		Map<String, String> props,
		Map<String, String> p2Inf,
		HashMap<String, String> manifest) {
		String bsn = manifest.get("Bundle-SymbolicName");
		String id = bsn.split(";")[0].trim();
		String version = manifest.get("Bundle-Version");
		boolean singleton = bsn.contains("singleton:=true");
		unitOpen(lines, id, version, singleton);
		update(lines, id, "[0.0.0," + version + ")");
		addUnitProperties(lines, props, manifest);
		addUnitProvides(lines, id, version, props, manifest, p2Inf);
		addUnitRequires(lines, manifest, p2Inf);
		addUnitFilter(lines, manifest);
		addUnitArtifacts(lines, id, version);
		touchpoint(lines, "org.eclipse.equinox.p2.osgi", "1.0.0");
		addUnitTouchpointData(plugin, manifest, lines, id, p2Inf);
		unitClose(lines);
	}
	private static void addUnitFilter(ArrayList<String> lines, HashMap<String, String> manifest) {
		String filter = manifest.get("Eclipse-PlatformFilter");
		if(filter != null) {
			String s = filter.trim().replace(" ", "");
			switch(s) {
				case "(&(osgi.os=win32)(osgi.arch=x86_64))":
					s = "(&(osgi.arch=x86_64)(osgi.os=win32))";
					break;
				case "(&(osgi.ws=win32)(osgi.os=win32)(osgi.arch=x86_64))":
				case "(&(osgi.os=win32)(osgi.ws=win32)(osgi.arch=x86_64))":
				case "(&(osgi.ws=win32)(osgi.arch=x86_64)(osgi.os=win32))":
				case "(&(osgi.os=win32)(osgi.arch=x86_64)(osgi.ws=win32))":
				case "(&(osgi.arch=x86_64)(osgi.ws=win32)(osgi.os=win32))":
					s = "(&(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))";
					break;
			}
			lines.add("      <filter>");
			lines.add("        " + wrap(s));
			lines.add("      </filter>");
		}
	}
	private static void addUnitTouchpointData(
		Path plugin,
		HashMap<String, String> manifest,
		ArrayList<String> lines,
		String id,
		Map<String, String> p2Inf) {
		boolean dirShape = plugin.getFileName().toString().endsWith(".jar") == false;
		List<String> instructions;
		if(dirShape && id.startsWith("nu.")) {
			instructions =
				List.of(
					"Bundle-RequiredExecutionEnvironment",
					"Bundle-Name",
					"Bundle-Activator",
					"Manifest-Version",
					"Bundle-Vendor",
					"Bundle-ClassPath",
					"Bundle-SymbolicName",
					"Bundle-ManifestVersion",
					"Eclipse-LazyStart",
					"Bundle-Version",
					"Bundle-Localization",
					"Export-Package",
					"Require-Bundle");
		} else {
			instructions = List.of("Bundle-SymbolicName", "Bundle-Version", "Fragment-Host");
		}
		instructions =
			instructions.filter(s -> manifest.get(s) != null).replaceAll(s -> s + ": " + wrap(manifest.get(s)));
		String instruction = "            " + String.join("&#xA;", instructions);
		int size = 1;
		if(dirShape) {
			size++;
		}
		if(p2Inf != null && p2Inf.containsKey("instructions.unconfigure")) {
			size++;
		}
		if(p2Inf != null && p2Inf.containsKey("instructions.configure")) {
			size++;
		}
		lines.add("      <touchpointData size='1'>");
		instructionsOpen(lines, size);
		lines.add("          <instruction key='manifest'>");
		lines.add(instruction);
		lines.add("          </instruction>");
		if(dirShape) {
			lines.add("          <instruction key='zipped'>");
			lines.add("            true");
			lines.add("          </instruction>");
		}
		if(p2Inf != null) {
			if(p2Inf.containsKey("instructions.unconfigure")) {
				lines.add("          <instruction key='unconfigure'>");
				lines.add("            " + p2Inf.get("instructions.unconfigure"));
				lines.add("          </instruction>");
			}
			if(p2Inf.containsKey("instructions.configure")) {
				lines.add("          <instruction key='configure'>");
				lines.add("            " + p2Inf.get("instructions.configure"));
				lines.add("          </instruction>");
			}
		}
		instructionsClose(lines);
		touchpointDataClose(lines);
	}
	private static void addUnitArtifacts(ArrayList<String> lines, String id, String version) {
		lines.add("      <artifacts size='1'>");
		lines.add("        <artifact classifier='osgi.bundle' id='" + id + "' version='" + version + "'/>");
		artifactsClose(lines);
	}
	private static void addUnitRequires(
		ArrayList<String> lines,
		HashMap<String, String> manifest,
		Map<String, String> p2Inf) {
		int size = 0;
		Matcher fragmentMatcher = matcherForValue(manifest, "Fragment-Host");
		Matcher requireMatcher = matcherForValue(manifest, "Require-Bundle");
		Matcher importMatcher = matcherForValue(manifest, "Import-Package");
		ArrayList<String> fragmentLines =
			collectRequired(fragmentMatcher, "        <required namespace='osgi.bundle' name='%s' range='%s'%s/>");
		ArrayList<String> requireLines =
			collectRequired(requireMatcher, "        <required namespace='osgi.bundle' name='%s' range='%s'%s/>");
		ArrayList<String> importLines =
			collectRequired(importMatcher, "        <required namespace='java.package' name='%s' range='%s'%s/>");
		size = fragmentLines.size() + requireLines.size() + importLines.size();
		boolean addWin32 = "(osgi.os=win32)".equals(manifest.get("Eclipse-PlatformFilter"));
		if(addWin32) {
			size += 2;
		}
		int[] indexes =
			p2Inf.keySet().filter(s -> s.endsWith(".range") && s.startsWith("requires.")).mapToInt(
				s -> Integer.parseInt(s.replace("requires.", "").replace(".range", "")));
		Arrays.sort(indexes);
		size += indexes.length;
		if(size == 0) {
			return;
		}
		requiresOpen(lines, size);
		for(int index : indexes) {
			String namespace = p2Inf.getOrDefault("requires." + index + ".namespace", "java.package");
			String name = p2Inf.get("requires." + index + ".name");
			String optional = p2Inf.getOrDefault("requires." + index + ".optional", "false");
			String greedy = p2Inf.getOrDefault("requires." + index + ".greedy", "true");
			String range = p2Inf.getOrDefault("requires." + index + ".range", "0.0.0");
			required(lines, namespace, name, range, Boolean.parseBoolean(optional), Boolean.parseBoolean(greedy));
		}
		if(addWin32) {
			String x86 = "org.eclipse.swt.win32.win32.x86";
			String part1 = "(&amp;(osgi.arch=x86";
			String part2 = ")(osgi.os=win32)(osgi.ws=win32))";
			required(lines, x86, "0.0.0", part1 + part2);
			required(lines, x86 + "_64", "0.0.0", part1 + "_64" + part2);
		}
		lines.addAll(fragmentLines);
		lines.addAll(requireLines);
		lines.addAll(importLines);
		requiresClose(lines);
	}
	private static void required(
		ArrayList<String> lines,
		String namespace,
		String name,
		String range,
		boolean optional,
		boolean greedy) {
		//String opt = optional ? greedy ? " optional='true'" : " optional='true' greedy='false'" : "";
		lines.add("        <required namespace='" +
		namespace +
		"' name='" +
		name +
		"' range='" +
		range +
		"'" +
		(optional ? " optional='true'" : "") +
		(!greedy ? " greedy='false'" : "") +
		"/>");
	}
	private static void required(
		ArrayList<String> lines,
		String namespace,
		String name,
		String range,
		boolean optional,
		boolean greedy,
		boolean multiple) {
		lines.add("        <required namespace='" +
		namespace +
		"' name='" +
		name +
		"' range='" +
		range +
		"'" +
		(optional ? " optional='true'" : "") +
		(multiple ? " multiple='true'" : "") +
		(!greedy ? " greedy='false'" : "") +
		"/>");
	}
	private static Matcher matcherForValue(HashMap<String, String> manifest, String name) {
		String requireBundle = manifest.get(name);
		if(requireBundle == null) {
			requireBundle = "";
		}
		Matcher requireMatcher = EXPORTED_PACKAGE_PATTERN.matcher(requireBundle);
		return requireMatcher;
	}
	private static ArrayList<String> collectRequired(Matcher matcher, String format) {
		ArrayList<String> lines = new ArrayList<>();
		while(matcher.find()) {
			String group = matcher.group();
			String name = matcher.group(1);
			if(name.equals("system.bundle")) {
				name = "org.eclipse.osgi";
			}
			String ver = findVersion(group);
			String opt =
				group.contains("resolution:=optional") ? group.contains("x-installation:=greedy") ? " optional='true'"
					: " optional='true' greedy='false'" : "";
			String line = String.format(format, name, ver, opt);
			lines.add(line);
		}
		return lines;
	}
	private static void addUnitProvides(
		ArrayList<String> lines,
		String id,
		String version,
		Map<String, String> props,
		HashMap<String, String> manifest,
		Map<String, String> p2Inf) {
		String exportPackage = manifest.get("Export-Package");
		if(exportPackage == null) {
			exportPackage = "";
		}
		int size = 3;
		if(props.notEmpty()) {
			size++;
		}
		String fragment = null;
		if(manifest.get("Fragment-Host") != null) {
			Matcher matcher = EXPORTED_PACKAGE_PATTERN.matcher(manifest.get("Fragment-Host"));
			if(matcher.find()) {
				size++;
				String name = matcher.group(1);
				fragment =
					"        <provided namespace='osgi.fragment' name='" + name + "' version='" + version + "'/>";
			}
		}
		Matcher matcher = EXPORTED_PACKAGE_PATTERN.matcher(exportPackage);
		while(matcher.find()) {
			size++;
		}
		matcher.reset();
		for(int i = 0;; i++) {
			if(p2Inf.containsKey("provides." + i + ".name") == false) {
				break;
			}
			size++;
		}
		providesOpen(lines, size);
		lines.add(String.format(
			"        <provided namespace='org.eclipse.equinox.p2.iu' name='%s' version='%s'/>",
			id,
			version));
		for(int i = 0;; i++) {
			String p2InfName = p2Inf.get("provides." + i + ".name");
			if(p2InfName == null) {
				break;
			}
			String p2InfNamespace = p2Inf.getOrDefault("provides." + i + ".namespace", "org.eclipse.equinox.p2.iu");
			String p2InfVersion = p2Inf.getOrDefault("provides." + i + ".version", "0.0.0");
			lines.add(String.format(
				"        <provided namespace='%s' name='%s' version='%s'/>",
				p2InfNamespace,
				p2InfName,
				p2InfVersion.replace("$version$", version)));
		}
		lines.add(String.format("        <provided namespace='osgi.bundle' name='%s' version='%s'/>", id, version));
		while(matcher.find()) {
			String group = matcher.group();
			String name = matcher.group(1);
			String ver = findVersion(group);
			lines.add(String.format("        <provided namespace='java.package' name='%s' version='%s'/>", name, ver));
		}
		lines.add("        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>");
		if(fragment != null) {
			lines.add(fragment);
		}
		if(MAVEN_PROPERTIES.containsAll(props.keySet()) == false) {
			lines.add("        <provided namespace='org.eclipse.equinox.p2.localization' name='df_LT' version='1.0.0'/>");
		}
		providesClose(lines);
	}
	private static String findVersion(String group) {
		Matcher versionMatcher = EXPORTED_PACKAGE_VERSION_PATTERN.matcher(group);
		String ver = versionMatcher.find() ? versionMatcher.group(1) : "0.0.0";
		if(ver.contains(",")) {
			String leftLimit = ver.substring(0, 1);
			String rightLimit = ver.substring(ver.length() - 1, ver.length());
			String lower = ver.substring(1, ver.indexOf(','));
			String upper = ver.substring(ver.indexOf(',') + 1, ver.length() - 1);
			return leftLimit + pad(lower) + "," + pad(upper) + rightLimit;
		}
		return pad(ver);
	}
	private static String pad(String version) {
		int count = (int) IntStream.from(version).filter(c -> c == '.').count();
		switch(count) {
			case 0:
				return version + ".0.0";
			case 1:
				return version + ".0";
			default:
				return version;
		}
	}

	private static final boolean OK = true;
	private static final boolean NO = false;
	private static final Pattern EXPORTED_PACKAGE_VERSION_PATTERN =
		Pattern.compile(";\\s*b?u?n?d?l?e?-?version=\"([a-zA-Z0-9.,\\s\\[\\)]+)\"");
	private static final Pattern EXPORTED_PACKAGE_PATTERN =
		Pattern.compile("([-a-zA-Z0-9.]+)(?:;\\s*[-.a-z]+:?=(?:(?:[a-zA-Z0-9.]+)|(?:\"[a-zA-Z0-9.,\\s\\[\\]\\(\\)]+\")))*");

	private static enum UnitProperty {
		LT_VENDOR("            Bundle-Vendor", OK, "        <property name='df_LT.%2$s' value='%3$s'/>"),
		LT_NAME("                Bundle-Name", OK, "        <property name='df_LT.%2$s' value='%3$s'/>"),
		LT_DESCRIPTION("  Bundle-Description", OK, "        <property name='df_LT.%2$s' value='%3$s'/>"),
		P2_NAME("                Bundle-Name", NO, "        <property name='org.eclipse.equinox.p2.name' value='%1$s'/>"),
		P2_DESCRIPTION("  Bundle-Description", NO, "        <property name='org.eclipse.equinox.p2.description' value='%1$s'/>"),
		P2_PROVIDER("          Bundle-Vendor", NO, "        <property name='org.eclipse.equinox.p2.provider' value='%1$s'/>"),
		P2_CONTACT("   Bundle-ContactAddress", NO, "        <property name='org.eclipse.equinox.p2.contact' value='%1$s'/>"),
		P2_DOC_URL_1("         Bundle-DocURL", NO, "        <property name='org.eclipse.equinox.p2.doc.url' value='%1$s'/>"),
		P2_DOC_URL_2("         Bundle-DocUrl", NO, "        <property name='org.eclipse.equinox.p2.doc.url' value='%1$s'/>"),
		P2_LOCALIZATION("Bundle-Localization", NO, "        <property name='org.eclipse.equinox.p2.bundle.localization' value='%1$s'/>"),
		POM_GROUP_ID("               groupId", NO, "        <property name='maven-groupId' value='%1$s'/>"),
		POM_ARTIFACT_ID("         artifactId", NO, "        <property name='maven-artifactId' value='%1$s'/>"),
		POM_VERSION("                version", NO, "        <property name='maven-version' value='%1$s'/>"),
		MVN_GROUP_ID("   Bundle-SymbolicName", NO, "        <property name='maven-groupId' value='%1$s'/>"),
		MVN_ARTIFACT_ID("Bundle-SymbolicName", NO, "        <property name='maven-artifactId' value='%1$s'/>"),
		MVN_VERSION("         Bundle-Version", NO, "        <property name='maven-version' value='%1$s-SNAPSHOT'/>");
		boolean needsProp;
		String manifestKey;
		String format;

		UnitProperty(String manifestKey, boolean needsProp, String format) {
			this.needsProp = needsProp;
			this.manifestKey = manifestKey.trim();
			this.format = format;
		}
		boolean found(Map<String, String> props, HashMap<String, String> manifest) {
			switch(this) {
				case POM_GROUP_ID:
				case POM_ARTIFACT_ID:
				case POM_VERSION: {
					String group = props.get(POM_GROUP_ID.manifestKey);
					return props.containsKey(POM_ARTIFACT_ID.manifestKey) &&
					props.containsKey(POM_VERSION.manifestKey) &&
					group != null /*&&
								  group.startsWith("org.eclipse.") &&
								  !group.startsWith("org.eclipse.ecf") &&
								  !group.startsWith("org.eclipse.emf") &&
								  !group.startsWith("org.eclipse.jem") &&
								  !group.startsWith("org.eclipse.jetty") &&
								  !group.startsWith("org.eclipse.mylyn") &&
								  !group.startsWith("org.eclipse.wst") &&
								  !group.startsWith("org.eclipse.xsd")*/
					;
				}
				default:
			}
			String value = manifest.get(manifestKey);
			if(value != null) {
				switch(this) {
					case MVN_GROUP_ID:
					case MVN_ARTIFACT_ID:
					case MVN_VERSION: {
						String group = manifest.get(MVN_GROUP_ID.manifestKey);
						return props.get(POM_GROUP_ID.manifestKey) == null &&
						group != null &&
						group.startsWith("org.eclipse.") &&
						!group.startsWith("org.eclipse.ecf") &&
						!group.startsWith("org.eclipse.emf") &&
						!group.startsWith("org.eclipse.jem") &&
						!group.startsWith("org.eclipse.jetty") &&
						!group.startsWith("org.eclipse.mylyn") &&
						!group.startsWith("org.eclipse.wst") &&
						!group.startsWith("org.eclipse.xsd");
					}
					case LT_DESCRIPTION:
						if(value.equals(manifest.get(LT_NAME.manifestKey))) {
							return false;
						}
					default:
				}
				String value2 = value.replace("%", "");
				if(needsProp == false || props.containsKey(value2)) {
					return true;
				}
			}
			return false;
		}
		String format(Map<String, String> props, HashMap<String, String> manifest) {
			switch(this) {
				case POM_GROUP_ID:
				case POM_ARTIFACT_ID:
				case POM_VERSION:
					return String.format(format, props.get(manifestKey));
				default:
			}
			String value = manifest.get(manifestKey);
			if(value != null) {
				switch(this) {
					case MVN_GROUP_ID: {
						String trim = value.split(";")[0].trim();
						if(trim.startsWith("org.eclipse.epp.mpc")) {
							return String.format(format, "org.eclipse.epp.mpc");
						}
						return String.format(format, String.join(".", List.of(trim.split("\\.")).subList(0, 3)));
					}
					case MVN_ARTIFACT_ID:
						return String.format(format, value.split(";")[0].trim());
					case MVN_VERSION:
						return String.format(format, String.join(".", List.of(value.split("\\.")).subList(0, 3)));
					default:
				}
				String value2 = value.replace("%", "");
				String prop = props.getOrDefault(value2, "MISS-" + value2);
				if(needsProp == false || props.containsKey(value2)) {
					return String.format(format, wrap(value), wrap(value2), wrap(prop));
				}
			}
			throw new IllegalStateException("value not found for " + manifestKey);
		}
	}

	private static void addUnitProperties(
		ArrayList<String> lines,
		Map<String, String> props,
		HashMap<String, String> manifest) {
		List<UnitProperty> found = List.of(UnitProperty.values()).filter(u -> u.found(props, manifest));
		int size = found.size();
		propertiesOpen(lines, size);
		for(UnitProperty unitProperty : found) {
			lines.add(unitProperty.format(props, manifest));
		}
		propertiesClose(lines);
	}
	private static void addConfigJreUnit(ArrayList<String> lines) {
		lines.addAll(
			"    <unit id='config.a.jre.javase' version='1.6.0' singleton='false'>",
			"      <hostRequirements size='1'>",
			"        <required namespace='org.eclipse.equinox.p2.iu' name='a.jre.javase' range='1.6.0'/>",
			"      </hostRequirements>",
			"      <properties size='1'>",
			"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
			"      </properties>",
			"      <provides size='1'>",
			"        <provided namespace='org.eclipse.equinox.p2.iu' name='config.a.jre.javase' version='1.6.0'/>",
			"      </provides>",
			"      <requires size='1'>",
			"        <required namespace='org.eclipse.equinox.p2.iu' name='a.jre.javase' range='1.6.0'/>",
			"      </requires>",
			"      <touchpoint id='org.eclipse.equinox.p2.native' version='1.0.0'/>",
			"      <touchpointData size='1'>",
			"        <instructions size='1'>",
			"          <instruction key='install'>",
			"",
			"          </instruction>",
			"        </instructions>",
			"      </touchpointData>",
			"    </unit>");
	}
	private static void addJreUnit(ArrayList<String> lines) {
		String[] javaPackages =
			{ "javax.accessibility", "javax.activation", "javax.activity", "javax.annotation",
				"javax.annotation.processing", "javax.crypto", "javax.crypto.interfaces", "javax.crypto.spec",
				"javax.imageio", "javax.imageio.event", "javax.imageio.metadata", "javax.imageio.plugins.bmp",
				"javax.imageio.plugins.jpeg", "javax.imageio.spi", "javax.imageio.stream", "javax.jws",
				"javax.jws.soap", "javax.lang.model", "javax.lang.model.element", "javax.lang.model.type",
				"javax.lang.model.util", "javax.management", "javax.management.loading", "javax.management.modelmbean",
				"javax.management.monitor", "javax.management.openmbean", "javax.management.relation",
				"javax.management.remote", "javax.management.remote.rmi", "javax.management.timer", "javax.naming",
				"javax.naming.directory", "javax.naming.event", "javax.naming.ldap", "javax.naming.spi", "javax.net",
				"javax.net.ssl", "javax.print", "javax.print.attribute", "javax.print.attribute.standard",
				"javax.print.event", "javax.rmi", "javax.rmi.CORBA", "javax.rmi.ssl", "javax.script",
				"javax.security.auth", "javax.security.auth.callback", "javax.security.auth.kerberos",
				"javax.security.auth.login", "javax.security.auth.spi", "javax.security.auth.x500",
				"javax.security.cert", "javax.security.sasl", "javax.sound.midi", "javax.sound.midi.spi",
				"javax.sound.sampled", "javax.sound.sampled.spi", "javax.sql", "javax.sql.rowset",
				"javax.sql.rowset.serial", "javax.sql.rowset.spi", "javax.swing", "javax.swing.border",
				"javax.swing.colorchooser", "javax.swing.event", "javax.swing.filechooser", "javax.swing.plaf",
				"javax.swing.plaf.basic", "javax.swing.plaf.metal", "javax.swing.plaf.multi", "javax.swing.plaf.synth",
				"javax.swing.table", "javax.swing.text", "javax.swing.text.html", "javax.swing.text.html.parser",
				"javax.swing.text.rtf", "javax.swing.tree", "javax.swing.undo", "javax.tools", "javax.transaction",
				"javax.transaction.xa", "javax.xml", "javax.xml.bind", "javax.xml.bind.annotation",
				"javax.xml.bind.annotation.adapters", "javax.xml.bind.attachment", "javax.xml.bind.helpers",
				"javax.xml.bind.util", "javax.xml.crypto", "javax.xml.crypto.dom", "javax.xml.crypto.dsig",
				"javax.xml.crypto.dsig.dom", "javax.xml.crypto.dsig.keyinfo", "javax.xml.crypto.dsig.spec",
				"javax.xml.datatype", "javax.xml.namespace", "javax.xml.parsers", "javax.xml.soap", "javax.xml.stream",
				"javax.xml.stream.events", "javax.xml.stream.util", "javax.xml.transform", "javax.xml.transform.dom",
				"javax.xml.transform.sax", "javax.xml.transform.stax", "javax.xml.transform.stream",
				"javax.xml.validation", "javax.xml.ws", "javax.xml.ws.handler", "javax.xml.ws.handler.soap",
				"javax.xml.ws.http", "javax.xml.ws.soap", "javax.xml.ws.spi", "javax.xml.ws.wsaddressing",
				"javax.xml.xpath", "org.ietf.jgss", "org.omg.CORBA", "org.omg.CORBA_2_3", "org.omg.CORBA_2_3.portable",
				"org.omg.CORBA.DynAnyPackage", "org.omg.CORBA.ORBPackage", "org.omg.CORBA.portable",
				"org.omg.CORBA.TypeCodePackage", "org.omg.CosNaming", "org.omg.CosNaming.NamingContextExtPackage",
				"org.omg.CosNaming.NamingContextPackage", "org.omg.Dynamic", "org.omg.DynamicAny",
				"org.omg.DynamicAny.DynAnyFactoryPackage", "org.omg.DynamicAny.DynAnyPackage", "org.omg.IOP",
				"org.omg.IOP.CodecFactoryPackage", "org.omg.IOP.CodecPackage", "org.omg.Messaging",
				"org.omg.PortableInterceptor", "org.omg.PortableInterceptor.ORBInitInfoPackage",
				"org.omg.PortableServer", "org.omg.PortableServer.CurrentPackage",
				"org.omg.PortableServer.POAManagerPackage", "org.omg.PortableServer.POAPackage",
				"org.omg.PortableServer.portable", "org.omg.PortableServer.ServantLocatorPackage",
				"org.omg.SendingContext", "org.omg.stub.java.rmi", "org.w3c.dom", "org.w3c.dom.bootstrap",
				"org.w3c.dom.css", "org.w3c.dom.events", "org.w3c.dom.html", "org.w3c.dom.ls", "org.w3c.dom.ranges",
				"org.w3c.dom.stylesheets", "org.w3c.dom.traversal", "org.w3c.dom.views", "org.w3c.dom.xpath",
				"org.xml.sax", "org.xml.sax.ext", "org.xml.sax.helpers", };
		String[] osgiMinimums = { "1.0.0", "1.1.0", "1.2.0", };
		String[] osgiJREs = { "1.0.0", "1.1.0", };
		String[] osgiJavaSEs = { "1.0.0", "1.1.0", "1.2.0", "1.3.0", "1.4.0", "1.5.0", "1.6.0", };
		int size = 1 + javaPackages.length + osgiMinimums.length + osgiJREs.length + osgiJavaSEs.length;
		lines.add("    <unit id='a.jre.javase' version='1.6.0' singleton='false'>");
		providesOpen(lines, size);
		lines.add("        <provided namespace='org.eclipse.equinox.p2.iu' name='a.jre.javase' version='1.6.0'/>");
		for(String s : javaPackages) {
			lines.add("        <provided namespace='java.package' name='" + s + "' version='0.0.0'/>");
		}
		for(String s : osgiMinimums) {
			lines.add("        <provided namespace='osgi.ee' name='OSGi/Minimum' version='" + s + "'/>");
		}
		for(String s : osgiJREs) {
			lines.add("        <provided namespace='osgi.ee' name='JRE' version='" + s + "'/>");
		}
		for(String s : osgiJavaSEs) {
			lines.add("        <provided namespace='osgi.ee' name='JavaSE' version='" + s + "'/>");
		}
		providesClose(lines);
		lines.add("      <touchpoint id='org.eclipse.equinox.p2.native' version='1.0.0'/>");
		unitClose(lines);
	}
	private static void compare(List<String> v1, List<String> v2) {
		int i1 = 0;
		int i2 = 0;
		int matchCount = 0;
		int errorCount = 0;
		int n1 = v1.size();
		int n2 = v2.size();
		while(i1 < n1 && i2 < n2) {
			String s1 = v1.get(i1);
			String s2 = v2.get(i2);
			String filterS1 = significantChanges(s1);
			String filterS2 = significantChanges(s2);
			if(filterS1.equals(filterS2)) {
				matchCount++;
				i1++;
				i2++;
			} else {
				List<String> subV1 =
					v1.subList(Math.max(i1 - 10, 0), Math.min(i1 + 10, n1)).replaceAll(s -> significantChanges(s));
				List<String> subV2 =
					v2.subList(Math.max(i2 - 10, 0), Math.min(i2 + 10, n2)).replaceAll(s -> significantChanges(s));
				boolean foundS1 = subV2.contains(filterS1);
				boolean foundS2 = subV1.contains(filterS2);
				if(foundS1 && foundS2) {
					matchCount++;
					i1++;
					i2++;
				} else if(filterS2.contains("name='maven-")) {
					matchCount++;
					i2++;
				} else if(filterS1.contains("name='maven-")) {
					i1++;
				} else {
					if(matchCount > 0) {
						if(matchCount > 11) {
							System.out.println("(skipping to line " + (i1 - 9) + " / " + (i2 - 9) + ")");
						}
						for(int i = -Math.min(matchCount, 10); i < 0; i++) {
							report("match  v1 v2 ", v2.get(i2 + i));
						}
						matchCount = 0;
					}
					if(foundS2) {
						i1++;
						report("NOT FOUND v1 ", s1);
					} else if(foundS1) {
						i2++;
						report("NOT FOUND v2 ", s2);
					} else {
						i1++;
						i2++;
						report("MISMATCH ", s1, s2);
					}
					errorCount++;
				}
			}
			if(errorCount > 10) {
				System.out.println("Too many errors, aborting...");
				break;
			}
		}
		if(matchCount > 0) {
			System.out.println("(" + matchCount + " matching lines)");
		}
	}
	private static String significantChanges(String s) {
		s = s.replaceAll("\\s|&#xA;|&#x9;|optional='true'|\\.SNAPSHOT|\\.qualifier|\\.[vI]?\\d{8}-?\\d{0,4}-?r?", "");
		s = s.replaceAll("\\d+", "9");
		s = s.replace("9.9.9", "9");
		s = s.replace("9.9", "9");
		s = s.replace("[9,9]", "9");
		s = s.replace("[9,9)", "9");
		return s;
	}
	private static void report(String error, String s) {
		if(s.length() > 200) {
			s = s.substring(0, 200) + "...";
		}
		System.out.println(error + s);
	}
	private static void report(String error, String s1, String s2) {
		if(s1.length() > 10000) {
			s1 = s1.substring(0, 10000) + "...";
		}
		System.out.println(error + " v1 " + s1);
		if(s2.length() > 10000) {
			s2 = s2.substring(0, 10000) + "...";
		}
		System.out.println(error + " v2 " + s2);
	}
	private static List<String> readLastProfile(Path root) throws IOException {
		Path registryFolder = root.resolve("p2/org.eclipse.equinox.p2.engine/profileRegistry");
		IOStream<Path> sorted =
			Files.walk(registryFolder).filter(p -> p.getFileName().toString().endsWith(".gz")).sorted(
				Comparator.comparing(Path::getFileName).reversed());
		Path path = sorted.findFirst().get();
		System.out.println("Reading " + path);
		byte[] bs = Streams.readFully(new GZIPInputStream(Files.newInputStream(path)));
		String string = new String(bs, StandardCharsets.UTF_8).replace("\r", "");
		return List.of(string.split("\n"));
	}
	private static String wrap(String s) {
		return s.replace("&", "&amp;").replace("'", "&apos;").replace("\"", "&quot;").replace("<", "&lt;").replace(
			"\n",
			"&#xA;").replace("\r", "&#xD;").replace("\t", "&#x9;");
	}
	private static List<String> readNewProfile(String profileName) {
		String string = snapshot.getFileAsString(Paths.get(profileName)).replace("\r", "");
		return List.of(string.split("\n"));
	}
	private static HashMap<Path, Feature> getFeatures() throws IOException {
		HashMap<Path, Feature> originalFeatures = new HashMap<>();
		List<Path> folders = snapshot.listFolders(Paths.get("features"));
		for(Path path : folders) {
			Feature feature = snapshot.getFileAsFeature(path);
			originalFeatures.put(fromZipPath(path), feature);
		}
		return originalFeatures;
	}
	private static HashMap<Path, Plugin> getPlugins() throws IOException {
		HashMap<Path, Plugin> originalPlugins = new HashMap<>();
		List<Path> folders = snapshot.listFolders(Paths.get("plugins"));
		for(Path path : folders) {
			Plugin feature = snapshot.getFileAsPlugin(path);
			originalPlugins.put(fromZipPath(path), feature);
		}
		return originalPlugins;
	}
	private static HashMap<Path, Map<String, String>> getPluginToP2Inf() throws IOException {
		HashMap<Path, Map<String, String>> pluginToP2Inf = new HashMap<>();
		for(Path plugin : snapshot.listAll(Paths.get("plugins"))) {
			Path p2Inf = plugin.resolve("META-INF/p2.inf");
			Map<String, String> properties = snapshot.getFileAsProperties(p2Inf);
			pluginToP2Inf.put(fromZipPath(plugin), properties);
		}
		return pluginToP2Inf;
	}
	private static HashMap<Path, Map<String, String>> getPluginToProperties() throws IOException {
		HashMap<Path, Map<String, String>> pluginToProperties = new HashMap<>();
		for(Path pluginPath : snapshot.listAll(Paths.get("plugins"))) {
			Plugin plugin = snapshot.getFileAsPlugin(pluginPath);
			if(plugin == null) {
				continue;
			}
			Map<String, String> localization = plugin.getLocalization(snapshot);
			List<Path> metaInfFiles = snapshot.listFilesRecursive(pluginPath.resolve("META-INF"));
			Optional<Path> optional = metaInfFiles.stream().filter(p -> p.endsWith("pom.properties")).findAny();
			if(optional.isPresent()) {
				Map<String, String> pomProperties = snapshot.getFileAsProperties(optional.get());
				localization = localization.putAll(pomProperties);
			}
			pluginToProperties.put(fromZipPath(pluginPath), localization);
		}
		return pluginToProperties;
	}
	private static Path fromZipPath(String path) {
		return MainBuildIDE.fromZipPath(path);
	}
	private static Path fromZipPath(Path path) {
		return MainBuildIDE.fromZipPath(path);
	}

	private static Map<Path, List<String>> tooling =
		HashMap.of(
			fromZipPath("tooling.osgi.bundle.default"),
			List.of(
				"    <unit id='tooling.osgi.bundle.default' version='1.0.0' singleton='false'>",
				"      <hostRequirements size='1'>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='0.0.0' multiple='true' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='tooling.osgi.bundle.default' version='1.0.0'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='tooling' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='1'>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='0.0.0' multiple='true' greedy='false'/>",
				"      </requires>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='4'>",
				"          <instruction key='install'>",
				"            installBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            uninstallBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='unconfigure'>",
				"",
				"          </instruction>",
				"          <instruction key='configure'>",
				"            setStartLevel(startLevel:4);",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).put(
			fromZipPath("tooling.source.default"),
			List.of(
				"    <unit id='tooling.source.default' version='1.0.0' singleton='false'>",
				"      <hostRequirements size='1'>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='source' range='0.0.0' optional='true' multiple='true' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='tooling.source.default' version='1.0.0'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='tooling' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='1'>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='source' range='0.0.0' optional='true' multiple='true' greedy='false'/>",
				"      </requires>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='2'>",
				"          <instruction key='install'>",
				"            addSourceBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            removeSourceBundle(bundle:${artifact})",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).put(
			fromZipPath("toolingorg.eclipse.equinox.launcher"),
			List.of(
				"    <unit id='toolingorg.eclipse.equinox.launcher' version='1.3.0.v20140415-2008' singleton='false'>",
				"      <hostRequirements size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.launcher' range='1.3.0.v20140415-2008'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='toolingorg.eclipse.equinox.launcher' version='1.3.0.v20140415-2008'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='tooling' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.launcher' range='1.3.0.v20140415-2008'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </requires>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='4'>",
				"          <instruction key='install'>",
				"            installBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            uninstallBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='unconfigure'>",
				"            removeProgramArg(programArg:-startup);removeProgramArg(programArg:@artifact);",
				"          </instruction>",
				"          <instruction key='configure'>",
				"            addProgramArg(programArg:-startup);addProgramArg(programArg:@artifact);",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).put(
			fromZipPath("toolingorg.eclipse.equinox.launcher.win32.win32.x86_64"),
			List.of(
				"    <unit id='toolingorg.eclipse.equinox.launcher.win32.win32.x86_64' version='1.1.200.v20140603-1326' singleton='false'>",
				"      <hostRequirements size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.launcher.win32.win32.x86_64' range='1.1.200.v20140603-1326'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='toolingorg.eclipse.equinox.launcher.win32.win32.x86_64' version='1.1.200.v20140603-1326'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='tooling' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.launcher.win32.win32.x86_64' range='1.1.200.v20140603-1326'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </requires>",
				"      <filter>",
				"        (&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))",
				"      </filter>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='4'>",
				"          <instruction key='install'>",
				"            installBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            uninstallBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='unconfigure'>",
				"            removeProgramArg(programArg:--launcher.library);removeProgramArg(programArg:@artifact);",
				"          </instruction>",
				"          <instruction key='configure'>",
				"            addProgramArg(programArg:--launcher.library);addProgramArg(programArg:@artifact);",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).put(
			fromZipPath("toolingwin32.win32.x86_64org.eclipse.equinox.common"),
			List.of(
				"    <unit id='toolingwin32.win32.x86_64org.eclipse.equinox.common' version='4.5.0.I20140909-1315' singleton='false'>",
				"      <hostRequirements size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.common' range='3.6.200.v20140819-0835'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='toolingwin32.win32.x86_64org.eclipse.equinox.common' version='4.5.0.I20140909-1315'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='toolingwin32.win32.x86_64' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.common' range='3.6.200.v20140819-0835'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </requires>",
				"      <filter>",
				"        (&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))",
				"      </filter>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='4'>",
				"          <instruction key='install'>",
				"            installBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            uninstallBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='unconfigure'>",
				"            setStartLevel(startLevel:-1);markStarted(started: false);",
				"          </instruction>",
				"          <instruction key='configure'>",
				"            setStartLevel(startLevel:2);markStarted(started: true);",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).put(
			fromZipPath("toolingwin32.win32.x86_64org.eclipse.equinox.ds"),
			List.of(
				"    <unit id='toolingwin32.win32.x86_64org.eclipse.equinox.ds' version='4.5.0.I20140909-1315' singleton='false'>",
				"      <hostRequirements size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.ds' range='1.4.200.v20131126-2331'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='toolingwin32.win32.x86_64org.eclipse.equinox.ds' version='4.5.0.I20140909-1315'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='toolingwin32.win32.x86_64' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.ds' range='1.4.200.v20131126-2331'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </requires>",
				"      <filter>",
				"        (&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))",
				"      </filter>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='4'>",
				"          <instruction key='install'>",
				"            installBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            uninstallBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='unconfigure'>",
				"            setStartLevel(startLevel:-1);markStarted(started: false);",
				"          </instruction>",
				"          <instruction key='configure'>",
				"            setStartLevel(startLevel:2);markStarted(started: true);",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).put(
			fromZipPath("toolingwin32.win32.x86_64org.eclipse.equinox.event"),
			List.of(
				"    <unit id='toolingwin32.win32.x86_64org.eclipse.equinox.event' version='4.5.0.I20140909-1315' singleton='false'>",
				"      <hostRequirements size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.event' range='1.3.100.v20140115-1647'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='toolingwin32.win32.x86_64org.eclipse.equinox.event' version='4.5.0.I20140909-1315'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='toolingwin32.win32.x86_64' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.event' range='1.3.100.v20140115-1647'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </requires>",
				"      <filter>",
				"        (&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))",
				"      </filter>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='4'>",
				"          <instruction key='install'>",
				"            installBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            uninstallBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='unconfigure'>",
				"            setStartLevel(startLevel:-1);markStarted(started: false);",
				"          </instruction>",
				"          <instruction key='configure'>",
				"            setStartLevel(startLevel:2);markStarted(started: true);",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).put(
			fromZipPath("toolingwin32.win32.x86_64org.eclipse.equinox.p2.reconciler.dropins"),
			List.of(
				"    <unit id='toolingwin32.win32.x86_64org.eclipse.equinox.p2.reconciler.dropins' version='4.5.0.I20140909-1315' singleton='false'>",
				"      <hostRequirements size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.p2.reconciler.dropins' range='1.1.200.v20131119-0908'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='toolingwin32.win32.x86_64org.eclipse.equinox.p2.reconciler.dropins' version='4.5.0.I20140909-1315'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='toolingwin32.win32.x86_64' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.p2.reconciler.dropins' range='1.1.200.v20131119-0908'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </requires>",
				"      <filter>",
				"        (&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))",
				"      </filter>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='4'>",
				"          <instruction key='install'>",
				"            installBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            uninstallBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='unconfigure'>",
				"            markStarted(started: false);",
				"          </instruction>",
				"          <instruction key='configure'>",
				"            markStarted(started: true);",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).put(
			fromZipPath("toolingwin32.win32.x86_64org.eclipse.equinox.simpleconfigurator"),
			List.of(
				"    <unit id='toolingwin32.win32.x86_64org.eclipse.equinox.simpleconfigurator' version='4.5.0.I20140909-1315' singleton='false'>",
				"      <hostRequirements size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.simpleconfigurator' range='1.1.0.v20131217-1203'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='toolingwin32.win32.x86_64org.eclipse.equinox.simpleconfigurator' version='4.5.0.I20140909-1315'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='toolingwin32.win32.x86_64' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.equinox.simpleconfigurator' range='1.1.0.v20131217-1203'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </requires>",
				"      <filter>",
				"        (&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))",
				"      </filter>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='4'>",
				"          <instruction key='install'>",
				"            installBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            uninstallBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='unconfigure'>",
				"            setStartLevel(startLevel:-1);markStarted(started: false);",
				"          </instruction>",
				"          <instruction key='configure'>",
				"            setStartLevel(startLevel:1);markStarted(started: true);",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).put(
			fromZipPath("toolingwin32.win32.x86_64org.eclipse.update.configurator"),
			List.of(
				"    <unit id='toolingwin32.win32.x86_64org.eclipse.update.configurator' version='4.5.0.I20140909-1315' singleton='false'>",
				"      <hostRequirements size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.update.configurator' range='3.3.300.v20140518-1928'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </hostRequirements>",
				"      <properties size='1'>",
				"        <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>",
				"      </properties>",
				"      <provides size='2'>",
				"        <provided namespace='org.eclipse.equinox.p2.iu' name='toolingwin32.win32.x86_64org.eclipse.update.configurator' version='4.5.0.I20140909-1315'/>",
				"        <provided namespace='org.eclipse.equinox.p2.flavor' name='toolingwin32.win32.x86_64' version='1.0.0'/>",
				"      </provides>",
				"      <requires size='2'>",
				"        <required namespace='osgi.bundle' name='org.eclipse.update.configurator' range='3.3.300.v20140518-1928'/>",
				"        <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='[1.0.0,2.0.0)' greedy='false'/>",
				"      </requires>",
				"      <filter>",
				"        (&amp;(osgi.arch=x86_64)(osgi.os=win32)(osgi.ws=win32))",
				"      </filter>",
				"      <touchpoint id='null' version='0.0.0'/>",
				"      <touchpointData size='1'>",
				"        <instructions size='4'>",
				"          <instruction key='install'>",
				"            installBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='uninstall'>",
				"            uninstallBundle(bundle:${artifact})",
				"          </instruction>",
				"          <instruction key='unconfigure'>",
				"            setProgramProperty(propName:org.eclipse.update.reconcile, propValue:);",
				"          </instruction>",
				"          <instruction key='configure'>",
				"            setProgramProperty(propName:org.eclipse.update.reconcile, propValue:false);",
				"          </instruction>",
				"        </instructions>",
				"      </touchpointData>",
				"    </unit>")).toMap();
	private static final Set<String> MAVEN_PROPERTIES = Set.of("groupId", "artifactId", "version");
	private static final String MAC_UNBUNDLED_EXE = "toolingorg.eclipse.platform.ide.executable.cocoa.macosx.x86_64";
	private static final String MAC_UNBUNDLED_FILTER =
		"(&amp;(!(macosx-bundled=*))(osgi.arch=x86_64)(osgi.os=macosx)(osgi.ws=cocoa))";
	private static final String MAC_BUNDLED_EXE =
		"toolingorg.eclipse.platform.ide.executable.cocoa.macosx.x86_64-bundled";
	private static final String MAC_BUNDLED_FILTER =
		"(&amp;(macosx-bundled=true)(osgi.arch=x86_64)(osgi.os=macosx)(osgi.ws=cocoa))";
}
