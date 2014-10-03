package project;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import utils.lists.ArrayList;
import utils.lists.HashMap;
import utils.lists.List;
import utils.lists.Map;
import utils.lists.Set;

public class Feature implements Comparable<Feature> {
	public static class Plugin {
		public final String id;
		public final String os;
		public final String ws;
		public final String arch;
		public final String downloadSize;
		public final String installSize;
		public final String version;
		public final boolean fragment;
		public final boolean unpack;

		Plugin(XMLStreamReader reader, boolean fragmentByDefault) {
			this(
				fragmentByDefault,
				reader.getAttributeValue(null, "id"),
				reader.getAttributeValue(null, "os"),
				reader.getAttributeValue(null, "ws"),
				reader.getAttributeValue(null, "arch"),
				reader.getAttributeValue(null, "download-size"),
				reader.getAttributeValue(null, "install-size"),
				reader.getAttributeValue(null, "version"),
				reader.getAttributeValue(null, "fragment"),
				reader.getAttributeValue(null, "unpack"));
		}
		Plugin(boolean fragmentByDefault, String id, String os, String ws, String arch, String downloadSize,
			String installSize, String version, String fragment, String unpack) {
			this.id = id;
			this.os = os;
			this.ws = ws;
			this.arch = arch;
			this.downloadSize = downloadSize == null ? "0" : downloadSize;
			this.installSize = installSize == null ? "0" : installSize;
			this.version = version == null ? "0.0.0" : version;
			this.fragment = fragment == null ? fragmentByDefault : Boolean.parseBoolean(unpack);
			this.unpack = unpack == null ? true : Boolean.parseBoolean(unpack);
		}
		@Override
		public String toString() {
			return "[" +
			(id != null ? "id=" + id + ", " : "") +
			(os != null ? "os=" + os + ", " : "") +
			(ws != null ? "ws=" + ws + ", " : "") +
			(arch != null ? "arch=" + arch + ", " : "") +
			(downloadSize != null ? "downloadSize=" + downloadSize + ", " : "") +
			(installSize != null ? "installSize=" + installSize + ", " : "") +
			(version != null ? "version=" + version + ", " : "") +
			"fragment=" +
			fragment +
			", unpack=" +
			unpack +
			"]";
		}
	}
	public static class Import {
		public final String plugin;
		public final String feature;
		public final String version;
		public final String match;

		Import(XMLStreamReader reader) {
			this(
				reader.getAttributeValue(null, "feature"),
				reader.getAttributeValue(null, "plugin"),
				reader.getAttributeValue(null, "version"),
				reader.getAttributeValue(null, "match"));
		}
		Import(String feature, String plugin, String version, String match) {
			this.feature = feature;
			this.plugin = plugin;
			this.version = version == null ? "0.0.0" : version;
			this.match = match;
		}
		@Override
		public String toString() {
			return "[" +
			(plugin != null ? "plugin=" + plugin + ", " : "") +
			(feature != null ? "feature=" + feature + ", " : "") +
			(version != null ? "version=" + version + ", " : "") +
			(match != null ? "match=" + match : "") +
			"]";
		}
	}

	public final Path path;
	public final String id;
	public final String plugin;
	public final String label;
	public final String version;
	public final String providerName;
	public final String licenseFeature;
	public final String licenseFeatureVersion;
	public final String descriptionURL;
	public final String description;
	public final String copyrightURL;
	public final String copyright;
	public final String licenseURL;
	public final String license;
	public final List<Import> requiresImports;
	public final List<Plugin> includes;
	public final List<Plugin> plugins;
	public final Map<String, String> properties;
	private static XMLInputFactory factory;

	public Feature(Path path, String id, String plugin, String label, String version, String providerName,
		String licenseFeature, String licenseFeatureVersion, String descriptionURL, String description,
		String copyrightURL, String copyright, String licenseURL, String license, ArrayList<Import> requiresImports,
		ArrayList<Plugin> includes, ArrayList<Plugin> plugins, HashMap<String, String> properties) {
		this.path = path;
		this.id = id;
		this.plugin = plugin;
		this.label = label;
		this.version = version;
		this.providerName = providerName;
		this.licenseFeature = licenseFeature;
		this.licenseFeatureVersion = licenseFeatureVersion;
		this.descriptionURL = descriptionURL;
		this.description = description;
		this.copyrightURL = copyrightURL;
		this.copyright = copyright;
		this.licenseURL = licenseURL;
		this.license = license;
		this.requiresImports = requiresImports.toList();
		this.includes = includes.toList();
		this.plugins = plugins.toList();
		this.properties = properties == null ? Map.of() : properties.toMap();
	}
	public static Feature fromXML(Path path, byte[] xmlBytes, byte[] propertiesBytes) throws XMLStreamException,
		IOException {
		if(factory == null) {
			factory = XMLInputFactory.newFactory();
		}
		XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xmlBytes));
		String featureID = null;
		String featurePlugin = null;
		String featureLabel = null;
		String featureVersion = null;
		String featureProviderName = null;
		String featureLicenseFeature = null;
		String featureLicenseFeatureVersion = null;
		String descriptionURL = null;
		String description = null;
		String copyrightURL = null;
		String copyright = null;
		String licenseURL = null;
		String license = null;
		ArrayList<Import> requiresImports = new ArrayList<>();
		ArrayList<Plugin> includes = new ArrayList<>();
		ArrayList<Plugin> plugins = new ArrayList<>();
		HashMap<String, String> properties = readProperties(propertiesBytes);
		try {
			while(reader.hasNext()) {
				if(reader.next() == XMLStreamConstants.START_ELEMENT) {
					switch(reader.getLocalName()) {
						case "feature":
							featureID = reader.getAttributeValue(null, "id");
							featurePlugin = reader.getAttributeValue(null, "plugin");
							featureLabel = reader.getAttributeValue(null, "label");
							featureVersion = reader.getAttributeValue(null, "version");
							featureProviderName = reader.getAttributeValue(null, "provider-name");
							featureLicenseFeature = reader.getAttributeValue(null, "license-feature");
							featureLicenseFeatureVersion = reader.getAttributeValue(null, "license-feature-version");
							break;
						case "description":
							descriptionURL = reader.getAttributeValue(null, "url");
							description = reader.getElementText().trim();
							break;
						case "copyright":
							copyrightURL = reader.getAttributeValue(null, "url");
							copyright = reader.getElementText().trim();
							break;
						case "license":
							licenseURL = reader.getAttributeValue(null, "url");
							license = reader.getElementText().trim();
							break;
						case "import":
							requiresImports.add(new Import(reader));
							break;
						case "plugin":
							plugins.add(new Plugin(reader, false));
							break;
						case "includes":
							includes.add(new Plugin(reader, true));
							break;
					}
				}
			}
		} finally {
			reader.close();
		}
		return new Feature(
			path,
			featureID,
			featurePlugin,
			featureLabel,
			featureVersion,
			featureProviderName,
			featureLicenseFeature,
			featureLicenseFeatureVersion,
			descriptionURL,
			description,
			copyrightURL,
			copyright,
			licenseURL,
			license,
			requiresImports,
			includes,
			plugins,
			properties);
	}
	public String mavenVersion() {
		return String.join(".", List.of(version.split("\\.")).subList(0, 3)) + "-SNAPSHOT";
	}
	public String mavenGroupID() {
		if(id.startsWith("org.eclipse.equinox")) {
			return "org.eclipse.equinox";
		}
		switch(id) {
			case "org.eclipse.rcp.configuration":
				return "org.eclipse.rcp.configuration";
		}
		//			case "org.eclipse.equinox.p2.extras.feature":
		//			case "org.eclipse.equinox.p2.core.feature":
		//				return "org.eclipse.equinox";
		//			case "org.eclipse.help":
		//				return "org.eclipse.help.feature";
		List<String> list = List.of(id.split("\\."));
		return String.join(".", list.subList(0, Math.min(3, list.size()))) + ".feature";
	}
	public String fileName() {
		return id + "_" + version;
	}
	private static HashMap<String, String> readProperties(byte[] bytes) throws IOException {
		if(bytes == null) {
			return HashMap.of();
		}
		Properties props = new Properties();
		props.load(new ByteArrayInputStream(bytes));
		Set<String> names = Set.from(props.stringPropertyNames());
		return names.stream().toMap(s -> s, s -> props.getProperty(s));
	}
	public @Override int compareTo(Feature o) {
		return id.compareTo(o.id);
	}
	@Override
	public String toString() {
		return "[" +
		(id != null ? "id=" + id + ", " : "") +
		(plugin != null ? "plugin=" + plugin + ", " : "") +
		(label != null ? "label=" + label + ", " : "") +
		(version != null ? "version=" + version + ", " : "") +
		(providerName != null ? "providerName=" + providerName + ", " : "") +
		(licenseFeature != null ? "licenseFeature=" + licenseFeature + ", " : "") +
		(licenseFeatureVersion != null ? "licenseFeatureVersion=" + licenseFeatureVersion + ", " : "") +
		(descriptionURL != null ? "descriptionURL=" + descriptionURL + ", " : "") +
		(description != null ? "description=" + description + ", " : "") +
		(copyrightURL != null ? "copyrightURL=" + copyrightURL + ", " : "") +
		(copyright != null ? "copyright=" + copyright + ", " : "") +
		(licenseURL != null ? "licenseURL=" + licenseURL + ", " : "") +
		(license != null ? "license=" + license + ", " : "") +
		(requiresImports != null ? "requiresImports=" + requiresImports + ", " : "") +
		(includes != null ? "includes=" + includes + ", " : "") +
		(plugins != null ? "plugins=" + plugins + ", " : "") +
		(properties != null ? "properties=" + properties : "") +
		"]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		Feature other = (Feature) obj;
		if(id == null) {
			if(other.id != null) return false;
		} else if(!id.equals(other.id)) return false;
		if(version == null) {
			if(other.version != null) return false;
		} else if(!version.equals(other.version)) return false;
		return true;
	}
}