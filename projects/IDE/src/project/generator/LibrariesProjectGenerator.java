package project.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import utils.lists.ArrayList;
import utils.lists.Files;
import utils.lists.Paths;

public class LibrariesProjectGenerator {
	public static void main(String[] args) {
		Path libraries = Paths.get("").toAbsolutePath().getParent().getParent().resolve("libraries");
		try {
			generateProjects(libraries);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	private static void generateProjects(Path libraries) throws IOException {
		ArrayList<Path> repos = Files.list(libraries).filter(Files::isDirectory).toList();
		for(Path repo : repos) {
			if(repo.endsWith("SuperiorStreams")) {
				continue;
			}
			generateDotProject(repo);
			generateDotGitignore(repo);
		}
	}
	private static void generateDotGitignore(Path repo) throws IOException {
		Path dotGitignore = repo.resolve(".gitignore");
		ArrayList<String> lines =
			Files.isRegularFile(dotGitignore) ? Files.readAllLines(dotGitignore) : new ArrayList<>();
		if(lines.contains("/.project") || lines.contains(".project")) {
			return;
		}
		lines.add("/.project");
		byte[] bytes = String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
		Files.write(dotGitignore, bytes);
	}
	private static void generateDotProject(Path repo) throws IOException {
		ArrayList<String> lines = new ArrayList<>();
		lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		lines.add("<projectDescription>");
		lines.add("	<name>" + repo.getFileName() + "</name>");
		lines.add("	<comment></comment>");
		lines.add("	<projects>");
		lines.add("	</projects>");
		lines.add("	<buildSpec>");
		lines.add("	</buildSpec>");
		lines.add("	<natures>");
		lines.add("	</natures>");
		lines.add("</projectDescription>");
		lines.add("");
		Path dotProject = repo.resolve(".project");
		byte[] bytes = String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
		Files.write(dotProject, bytes);
	}
}
