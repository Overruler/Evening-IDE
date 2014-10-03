package project.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import utils.lists.ArrayList;
import utils.lists.Files;
import utils.lists.HashMap;
import utils.lists.List;
import utils.lists.Pair;
import utils.lists.Paths;
import utils.lists.Set;
import utils.streams2.IOStream;
import utils.streams2.Streams;

public class P2CommandGenerator {
	private static final List<Pair<String, String>> repos =	/*Q*/ List.of(
		Pair.of("D:\\p2_eclipse.epp.mars.txt",						"http://download.eclipse.org/technology/epp/packages/mars/"), 
		Pair.of("D:\\p2_eclipse.epp.mars.M1.txt",					"http://download.eclipse.org/technology/epp/packages/mars/M1"), 
		Pair.of("D:\\p2_eclipse.e4.tools.0.16.txt",					"http://download.eclipse.org/e4/updates/0.16"), 
		Pair.of("D:\\p2_satyagraha.generic.1.9.2.txt", 				"http://dl.bintray.com/satyagraha/generic/1.9.2/"), 
		Pair.of("D:\\p2_satyagraha.gfm_viewer.txt",					"https://raw.github.com/satyagraha/gfm_viewer/master/p2-composite/"), 
		Pair.of("D:\\p2_psnet.image_viewer.txt",					"http://psnet.nu/eclipse/updates/"), 
		Pair.of("D:\\p2_nodeclipse.0.10.0.txt",						"http://dl.bintray.com/nodeclipse/nodeclipse/0.10.0/"), 
		Pair.of("D:\\p2_nodeclipse.0.16.txt",						"http://dl.bintray.com/nodeclipse/nodeclipse/0.16/"), 
		Pair.of("D:\\p2_springsource.tools.m2e-sts310.txt",			"http://download.springsource.com/release/TOOLS/third-party/m2e-sts310/"), 
		Pair.of("D:\\p2_springsource.tools.gradle.3.6.1.txt",		"http://download.springsource.com/release/TOOLS/gradle/3.6.1.RELEASE/"), 
		Pair.of("D:\\p2_springsource.tools.update.3.6.1.e4.3.txt",	"http://dist.springsource.com/release/TOOLS/update/3.6.1.RELEASE/e4.3/"), 
		Pair.of("D:\\p2_springsource.tools.gradle.txt",				"http://dist.springsource.com/release/TOOLS/gradle/"), 
		Pair.of("D:\\p2_springsource.tools.update.e4.3.txt",		"http://dist.springsource.com/release/TOOLS/update/e4.3/"), 
		Pair.of("D:\\p2_nodeclipse.txt",							"http://www.nodeclipse.org/updates/"), 
		Pair.of("D:\\p2_jeeeyul.themes.txt",						"http://eclipse.jeeeyul.net/update/"), 
		Pair.of("D:\\p2_eclipse.m2e.1.4.0.20130601.txt",			"http://download.eclipse.org/technology/m2e/releases/1.4/1.4.0.20130601-0317"), 
		Pair.of("D:\\p2_eclipse.m2e.1.4.1.20140328.txt",			"http://download.eclipse.org/technology/m2e/releases/1.4/1.4.1.20140328-1905"), 
		Pair.of("D:\\p2_eclipse.m2e.1.4.txt",						"http://download.eclipse.org/technology/m2e/releases/1.4"), 
		Pair.of("D:\\p2_eclipse.m2e.1.5.0.20140606.txt",			"http://download.eclipse.org/technology/m2e/releases/1.5/1.5.0.20140606-0033"), 
		Pair.of("D:\\p2_eclipse.mylyn.3.12.0.20140609.txt",			"http://download.eclipse.org/mylyn/drops/3.12.0/v20140609-1648/"), 
		Pair.of("D:\\p2_eclipse.4.5milestones.txt",					"http://download.eclipse.org/eclipse/updates/4.5milestones"), 
		Pair.of("D:\\p2_eclipse.4.5.txt",							"http://download.eclipse.org/eclipse/updates/4.5"), 
		Pair.of("D:\\p2_eclipse.m2e.1.5.txt",						"http://download.eclipse.org/technology/m2e/releases/1.5"),
		Pair.of("D:\\p2_eclipse.mylyn.3.12.txt",					"http://download.eclipse.org/mylyn/releases/3.12"), 
		Pair.of("D:\\p2_eclipse.egit.txt",							"http://download.eclipse.org/egit/updates"), 
		Pair.of("D:\\p2_eclipse.m2e.txt",							"http://download.eclipse.org/technology/m2e/releases/"), 
		Pair.of("D:\\p2_eclipse.mylyn.latest.txt",					"http://download.eclipse.org/mylyn/releases/latest"), 
		Pair.of("D:\\p2_eclipse.mars.txt",							"http://download.eclipse.org/releases/mars")
	); /*E*/
	// -destination d:/evening/ -profile EveningIDE
	private static final String INSTALL_COMMAND =
		"d:\\evening\\eclipsec.exe -nosplash -application org.eclipse.equinox.p2.director -repository %s -installIU %s%n";
	private static final String LIST_COMMAND =
		"d:\\builder\\eclipsec.exe -nosplash -application org.eclipse.equinox.p2.director -repository %s -list > %s%n";
	private static final Path SRC_PLUGINS = Paths.get("D:\\evening\\plugins");
	private static final Path SRC_PROFILE =
		SRC_PLUGINS.resolveSibling("p2/org.eclipse.equinox.p2.engine/profileRegistry");
	private static final Path DST_PLUGINS = Paths.get("target/ide-1/plugins");
	private static final Path DST_PROFILE = Paths.get(
		System.getProperty("user.home"),
		"eclipse-SDK-I20140909-1315-win32-x86_64/eclipse/p2/org.eclipse.equinox.p2.engine/profileRegistry");

	public static void main(String[] args) throws IOException {
		Set<String> set1 = Files.list(DST_PLUGINS).map(p -> toName(p)).toSet().addAll(readProfile(DST_PROFILE)).toSet();
		Set<String> set2 = Files.list(SRC_PLUGINS).map(p -> toName(p)).toSet().addAll(readProfile(SRC_PROFILE)).toSet();
		Set<String> all = set1.removeAll(set2);
		System.out.println("Still missing:");
		all.toArrayList().sort().forEach(System.out::println);
		System.out.println("\nNew list commands:");
		for(Pair<String, String> repo : repos) {
			if(Files.isRegularFile(Paths.get(repo.lhs)) == false) {
				System.out.printf(LIST_COMMAND, repo.rhs, repo.lhs);
			}
		}
		ArrayList<Pair<String, Pair<Version, String>>> combinedIUs = new ArrayList<>();
		for(Pair<String, String> repo : repos) {
			Path file = Paths.get(repo.lhs);
			if(Files.isRegularFile(file)) {
				ArrayList<String> lines = Files.readAllLines(file);
				Set<Pair<String, Pair<Version, String>>> set =
					lines.filter(s -> s.contains("=")).map(s -> Pair.of(toID(s), Pair.of(toVersion(s), repo.rhs))).toSet();
				combinedIUs.addAll(set);
			}
		}
		HashMap<String, ArrayList<Pair<Version, String>>> iuVersions =
			combinedIUs.stream().toMultiMap(Pair::lhs, Pair::rhs).stream().toMap(
				Pair::lhs,
				e -> e.rhs.toArrayList().sort(Pair::lhs));
		ArrayList<String> ius = iuVersions.keySet().retainAll(all).toArrayList().sort();
		for(String iu : ius) {
			ArrayList<Pair<Version, String>> versions = iuVersions.get(iu);
			Version latest = versions.get(-1).lhs;
			versions.filter(p -> p.lhs.equals(latest));
			for(Pair<String, String> repo : repos.subList(0, -2)) {
				if(versions.size() == 1) {
					break;
				}
				versions.removeIf(p -> p.rhs.equals(repo.rhs));
			}
		}
		System.out.println("\nNew install commands:");
		for(String iu : ius) {
			ArrayList<Pair<Version, String>> versions = iuVersions.get(iu);
			if(versions.size() == 1) {
				System.out.printf(INSTALL_COMMAND, versions.get(0).rhs, iu);
			}
		}
		for(String iu : ius) {
			ArrayList<Pair<Version, String>> versions = iuVersions.get(iu);
			if(versions.size() == 2) {
				System.out.println("\nNew install commands with just 2 options for " + iu + ":");
				for(Pair<Version, String> version : versions) {
					System.out.printf(INSTALL_COMMAND, version.rhs, iu);
				}
			}
		}
		for(String iu : ius) {
			ArrayList<Pair<Version, String>> versions = iuVersions.get(iu);
			if(versions.size() > 2) {
				System.out.println("\nNew install commands with multiple options for " + iu + ":");
				for(Pair<Version, String> version : versions) {
					System.out.printf(INSTALL_COMMAND, version.rhs, iu);
				}
			}
		}
	}
	private static ArrayList<String> readProfile(Path registry) throws IOException {
		IOStream<Path> stream = Files.walk(registry).filter(p -> p.getFileName().toString().endsWith("profile.gz"));
		ArrayList<Path> files = stream.toList();
		files.sort(Path::getFileName);
		Path path = files.get(-1);
		byte[] bytes = Streams.readFully(new GZIPInputStream(Files.newInputStream(path)));
		String data = new String(bytes, StandardCharsets.UTF_8);
		ArrayList<String> lines = ArrayList.of(data.split("[\\r\\n]{1,2}"));
		lines.filter(s -> s.contains("<unit id='"));
		lines.replaceAll(s -> s.substring(s.indexOf('\'') + 1));
		lines.replaceAll(s -> s.substring(0, s.indexOf('\'')));
		lines.removeIf(s -> s.endsWith(".feature.group"));
		lines.removeIf(s -> s.endsWith(".feature.jar"));
		lines.removeIf(s -> s.endsWith(".source"));
		lines.removeIf(s -> s.contains("org.eclipse.sdk"));
		return lines;
	}
	private static String toID(String s) {
		return s.substring(0, s.indexOf('='));
	}
	private static Version toVersion(String s) {
		return new Version(s.substring(s.indexOf('=') + 1));
	}
	private static String toName(Path p) {
		String s = p.getFileName().toString();
		return s.substring(0, s.lastIndexOf('_'));
	}

	private static class Version implements Comparable<Version> {
		final int major;
		final int medium;
		final int minor;
		final long qualifier;

		Version(String from) {
			String[] split = from.split("\\.");
			split = Arrays.copyOfRange(split, 0, 4);
			major = (int) parse(split[0]);
			medium = (int) parse(split[1]);
			minor = (int) parse(split[2]);
			qualifier = parse(split[3]);
		}
		private static long parse(String string) {
			if(string == null) {
				return 0;
			}
			try {
				return Integer.parseInt(string);
			} catch(NumberFormatException e) {
				String s = string.replaceAll("\\D", "");
				if(s.length() == 8) {
					s += "0000";
				} else if(s.length() > 12 || s.length() == 0) {
					return 0;
				}
				return Long.parseLong(s);
			}
		}
		@Override
		public String toString() {
			return "v" + major + "." + medium + "." + minor + "." + qualifier;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + major;
			result = prime * result + medium;
			result = prime * result + minor;
			result = prime * result + (int) qualifier;
			result = prime * result + (int) (qualifier >> 32);
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if(this == obj) return true;
			if(obj == null) return false;
			if(getClass() != obj.getClass()) return false;
			Version other = (Version) obj;
			if(major != other.major) return false;
			if(medium != other.medium) return false;
			if(minor != other.minor) return false;
			if(qualifier != other.qualifier) return false;
			return true;
		}
		@Override
		public int compareTo(Version o) {
			if(major != o.major) {
				return major - o.major;
			}
			if(medium != o.medium) {
				return medium - o.medium;
			}
			if(minor != o.minor) {
				return minor - o.minor;
			}
			if(qualifier == o.qualifier) {
				return 0;
			}
			return qualifier < o.qualifier ? -1 : 1;
		}
	}
}
