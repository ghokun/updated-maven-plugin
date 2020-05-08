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

package io.github.ghokun.updated.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeResult;

import io.github.ghokun.updated.enumeration.ValidationPolicy;
import io.github.ghokun.updated.scm.SourceCodeChangeDetectorFactory;
import io.github.ghokun.updated.scm.SourceCodeChanges;
import io.github.ghokun.updated.scm.SourceCodeManagement;

/**
 * Updated Maven Plugin Validate Mojo. Purpose of this mojo is to determine source code changes in modules and check
 * whether local version of those modules are changed. This can be bound to lifecycles or can be used as pre-commit hook
 * to ensure version changes are according to policy.
 *
 * <pre>
 * // Default
 * mvn io.github.ghokun:updated-maven-plugin:validate
 *
 * // Enforcing policy
 * mvn io.github.ghokun:updated-maven-plugin:validate -Dpolicy=ENFORCING
 * </pre>
 *
 * @author ghokun
 * @since 1.0.0
 */
@Mojo(name = "validate", inheritByDefault = false, aggregator = true)
public class ValidateMojo extends AbstractUpdatedMojo {
	
	/**
	 * Source code management type. Defaults to GIT which uses Eclipse JGit. This is here for future implementations of
	 * different SCMs.
	 */
	@Parameter(defaultValue = "GIT", property = "scm", required = false)
	private SourceCodeManagement scm;
	
	/**
	 * Validation policy.
	 *
	 * <pre>
	 * PERMISSIVE : Validation errors are shown as warnings.
	 * ENFORCING  : Validation errors throw exception.
	 * </pre>
	 */
	@Parameter(defaultValue = "PERMISSIVE", property = "policy", required = false)
	private ValidationPolicy policy;
	
	/**
	 * Remote branch name to validate against.
	 */
	@Parameter(defaultValue = "HEAD", property = "remote", required = false)
	private String remoteBranch;
	
	/**
	 * If true, prints detailed diff.
	 */
	@Parameter(defaultValue = "true", property = "showChangeDetails", required = false)
	private boolean showChangeDetails;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final SourceCodeChanges sourceCodeChanges = SourceCodeChangeDetectorFactory
			.getDetector(this.scm)
			.detectChanges(this.project, this.getProjects(), this.getLog(), this.remoteBranch);
		
		if (this.showChangeDetails) {
			this.getLog().info("Change Details:" + System.lineSeparator() + sourceCodeChanges.toString());
		}
		
		boolean shouldThrowException = false;
		int progress = 0;
		if (this.showProgress) {
			this.getLog().info("");
			this.getLog().info("Progress:");
		}
		for (final SourceCodeChanges module : sourceCodeChanges) {
			if (this.showProgress) {
				this.getLog().info(++progress + " / " + this.getProjects().size() + " [" + module.getCoords() + "]");
			}
			if (module.hasDiff()) {
				
				final VersionRangeResult versionRangeResult = this
					.findLatestVersionOfArtifact(module.getGroupId(), module.getArtifactId(), ":[0,)");
				
				if (versionRangeResult != null && versionRangeResult.getHighestVersion() != null
						&& module.getVersion().equals(versionRangeResult.getHighestVersion().toString())) {
					
					final RemoteRepository remoteRepository = (RemoteRepository) versionRangeResult
						.getRepository(versionRangeResult.getHighestVersion());
					
					final String validation = new StringBuilder()
						.append("Module ")
						.append(module.getCoords())
						.append(" has ")
						.append(module.diffCount())
						.append(" changes. However local version is the same with the remote version. Version: ")
						.append(module.getVersion())
						.append(", Repository ID: ")
						.append(remoteRepository.getId())
						.append(", Repository URL: ")
						.append(remoteRepository.getUrl())
						.toString();
					
					if (this.policy.equals(ValidationPolicy.ENFORCING)) {
						this.getLog().error(validation);
						shouldThrowException = true;
					}
					if (this.policy.equals(ValidationPolicy.PERMISSIVE)) {
						this.getLog().warn(validation);
					}
				}
			}
		}
		if (shouldThrowException) {
			throw new MojoExecutionException("You have validation errors. Please fix them before continuing.");
		}
	}
}
