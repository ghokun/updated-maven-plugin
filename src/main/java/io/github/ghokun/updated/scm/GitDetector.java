/*
 * MIT License
 *
 * Copyright (c) 2020 ghokun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.ghokun.updated.scm;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

/**
 * Source code change detector implementation for Git. Assumes a ${baseDir}/.git directory exists. Uses JGit.
 *
 * @author ghokun
 * @since 1.0.0
 */
public final class GitDetector implements SourceCodeChangeDetector {
	
	@Override
	public SourceCodeChanges detectChanges(
			MavenProject project,
			Set<MavenProject> projects,
			Log log,
			String remoteBranch) throws MojoExecutionException {
		try (final Git git = Git.open(new File(project.getBasedir().getPath() + "/.git"));
				final Repository repository = git.getRepository();
				final ObjectReader reader = repository.newObjectReader();) {
			
			// Fetch latest
			git.fetch();
			
			// Get local tree
			final String localBranch = repository.getBranch();
			final ObjectId localHead = repository.resolve("refs/heads/" + localBranch + "^{tree}");
			final CanonicalTreeParser localTree = new CanonicalTreeParser();
			localTree.reset(reader, localHead);
			log.info("Local Branch  : " + localBranch);
			log.info("Local Head    : " + localHead.toString());
			
			// Get remote tree
			final ObjectId remoteHead = repository.resolve("refs/remotes/origin/" + remoteBranch + "^{tree}");
			final CanonicalTreeParser remoteTree = new CanonicalTreeParser();
			remoteTree.reset(reader, remoteHead);
			log.info("Remote Branch : " + remoteBranch);
			log.info("Remote Head   : " + remoteHead.toString());
			
			// Diff
			final List<DiffEntry> diffs = git
				.diff()
				.setShowNameAndStatusOnly(true)
				.setOldTree(remoteTree)
				.setNewTree(localTree)
				.call();
			
			return this.parseDiffs(diffs, project, projects);
			
		} catch (final RevisionSyntaxException | IOException | GitAPIException e) {
			throw new MojoExecutionException("An error occurred while detecting source code changes", e);
		}
	}
	
	private SourceCodeChanges parseDiffs(
			Collection<DiffEntry> diffs,
			MavenProject project,
			Set<MavenProject> projects) {
		
		final Map<String, MavenProject> mapOfProjects = new HashMap<>();
		mapOfProjects.put(project.getArtifactId(), project);
		if (projects != null && !projects.isEmpty()) {
			projects.forEach(p -> mapOfProjects.put(p.getArtifactId(), p));
		}
		final SourceCodeChanges moduleTree = this.generateTree(project, mapOfProjects);
		
		for (final DiffEntry diff : diffs) {
			
			// new : ADD, COPY, RENAME
			if (diff.getChangeType().equals(ChangeType.ADD) || diff.getChangeType().equals(ChangeType.COPY)
					|| diff.getChangeType().equals(ChangeType.RENAME)) {
				final SourceCodeChanges module = this
					.findModuleOfDiff(moduleTree, project.getBasedir().getPath(), this.pathToDeque(diff.getNewPath()));
				module
					.getDiffs()
					.add(new SourceCodeDiff(DiffType.valueOf(diff.getChangeType().name()),
						diff.getOldPath(),
						diff.getNewPath()));
			}
			
			// old : COPY, DELETE, MODIFY, RENAME
			if (diff.getChangeType().equals(ChangeType.COPY) || diff.getChangeType().equals(ChangeType.DELETE)
					|| diff.getChangeType().equals(ChangeType.MODIFY)
					|| diff.getChangeType().equals(ChangeType.RENAME)) {
				final SourceCodeChanges module = this
					.findModuleOfDiff(moduleTree, project.getBasedir().getPath(), this.pathToDeque(diff.getOldPath()));
				module
					.getDiffs()
					.add(new SourceCodeDiff(DiffType.valueOf(diff.getChangeType().name()),
						diff.getOldPath(),
						diff.getNewPath()));
			}
		}
		
		return moduleTree; // for now
	}
	
	@SuppressWarnings("unchecked")
	private SourceCodeChanges generateTree(MavenProject project, Map<String, MavenProject> mapOfProjects) {
		final SourceCodeChanges tree = new SourceCodeChanges(project.getGroupId(),
			project.getArtifactId(),
			project.getVersion(),
			project.getBasedir().getPath());
		
		if (project.getModules() != null && !project.getModules().isEmpty()) {
			for (final String moduleArtifactId : (List<String>) project.getModules()) {
				final MavenProject module = mapOfProjects.get(moduleArtifactId);
				tree.getModules().add(this.generateTree(module, mapOfProjects));
			}
		}
		return tree;
	}
	
	private SourceCodeChanges findModuleOfDiff(SourceCodeChanges tree, String rootPath, Deque<String> path) {
		for (final SourceCodeChanges module : tree.getModules()) {
			final String relativePath = module.getRelativePath(rootPath);
			if (path.getFirst().equals(this.pathToDeque(relativePath).getFirst())) {
				path.removeFirst();
				return this.findModuleOfDiff(module, module.getPath(), path);
			}
		}
		return tree;
	}
	
	private Deque<String> pathToDeque(String path) {
		if (path.substring(0, 1).equals("/")) {
			path = path.replace("/", "");
		}
		final String[] splitPath = path.split("/");
		final Deque<String> pathQueue = new LinkedList<>();
		for (final String split : splitPath) {
			if (split != null && split.length() > 0) {
				pathQueue.addLast(split);
			}
		}
		return pathQueue;
	}
}