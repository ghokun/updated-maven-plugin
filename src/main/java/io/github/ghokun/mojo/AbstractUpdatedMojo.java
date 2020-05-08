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

package io.github.ghokun.mojo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

/**
 * Abstract class for common parts in mojos.
 *
 * @author ghokun
 * @since 1.0.0
 */
public abstract class AbstractUpdatedMojo extends AbstractMojo {
	
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;
	
	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	protected MavenSession mavenSession;
	
	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
	protected List<RemoteRepository> repositories;
	
	private static RepositorySystem repositorySystemSingleton;
	
	protected static RepositorySystem repositorySystem() {
		if (repositorySystemSingleton == null) {
			final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
			locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
			locator.addService(TransporterFactory.class, FileTransporterFactory.class);
			locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
			repositorySystemSingleton = locator.getService(RepositorySystem.class);
		}
		return repositorySystemSingleton;
	}
	
	private static RepositorySystemSession repositorySystemSessionSingleton;
	
	protected static RepositorySystemSession repositorySystemSession() {
		if (repositorySystemSessionSingleton == null) {
			final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
			final LocalRepository localRepo = new LocalRepository("/tmp/.m2");
			session.setLocalRepositoryManager(repositorySystem().newLocalRepositoryManager(session, localRepo));
			repositorySystemSessionSingleton = session;
		}
		return repositorySystemSessionSingleton;
	}
	
	/**
	 * Get all maven projects
	 * 
	 * @return {@link HashSet} of MavenProjects
	 */
	protected Set<MavenProject> getProjects() {
		final Set<MavenProject> projects = new HashSet<>();
		projects.addAll(this.mavenSession.getProjects());
		return projects;
	}
	
	/**
	 * Finds latest version of artifact from remote repositories.
	 * 
	 * @param groupId Group ID
	 * @param artifactId Artifact ID
	 * @param version Version
	 * @return Latest version of artifact
	 * @throws MojoExecutionException Better safe than sorry
	 */
	protected VersionRangeResult findLatestVersionOfArtifact(
			String groupId,
			String artifactId,
			String version) throws MojoExecutionException {
		final VersionRangeRequest request = new VersionRangeRequest(
			new DefaultArtifact(groupId + ":" + artifactId + ":" + version),
			this.repositories,
			null);
		try {
			return repositorySystem().resolveVersionRange(repositorySystemSession(), request);
		} catch (final VersionRangeResolutionException e) {
			throw new MojoExecutionException("An error occurred while resolving versions from remote repository.", e);
		}
	}
}