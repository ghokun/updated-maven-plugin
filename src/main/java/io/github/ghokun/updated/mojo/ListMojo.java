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

import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeResult;

import io.github.ghokun.updated.enumeration.LineEnding;

/**
 * Updated Maven Plugin List Mojo. This mojo lists local and remote versions of all sub-modules (recursively). Uses
 * .m2/settings.xml mirrors.
 *
 * <pre>
 * // Default
 * mvn io.github.ghokun:updated-maven-plugin:list
 *
 * // Outputs to myFile.csv
 * mvn io.github.ghokun:updated-maven-plugin:list -DoutputFile=myFile
 *
 * // Displays specified fields in template
 * mvn io.github.ghokun:updated-maven-plugin:list -Dheader=artifact,local,remote -Dtemplate=artifactId,localVersion,remoteVersion
 * </pre>
 *
 * @author ghokun
 * @since 1.0.0
 */
@Mojo(name = "list", inheritByDefault = false, aggregator = true)
public class ListMojo extends AbstractUpdatedMojo {
	
	/**
	 * If true, includes header in result.
	 */
	@Parameter(defaultValue = "true", property = "printHeader", required = false)
	private boolean printHeader;
	
	/**
	 * If true, includes all modules in result. By default only includes modules that have their versions changed
	 * compared to a remote repository.
	 */
	@Parameter(defaultValue = "false", property = "printAll", required = false)
	private boolean printAll;
	
	/**
	 * Header information. Could be useful in csv to json/xml conversions.
	 */
	@Parameter(	defaultValue = "directory,group:artifact,localVersion,remoteVersion,remoteRepositoryId,remoteRepositoryUrl",
				property = "header", required = false)
	private String header;
	
	/**
	 * Template for result. Available parameters are [baseDir, pomPath, groupId, artifactId, localVersion,
	 * remoteVersion, remoteRepositoryId, remoteRepositoryUrl]. Any other string is regarded as static text. Parameters
	 * are evaluated per-module.
	 */
	@Parameter(	defaultValue = "baseDir,groupId:artifactId,localVersion,remoteVersion,remoteRepositoryId,remoteRepositoryUrl",
				property = "template", required = false)
	private String template;
	
	/**
	 * Output file name. Produces a csv file, regardless of given extension. Does not produce output if left blank.
	 */
	@Parameter(property = "outputFile", required = false)
	private String outputFile;
	
	private String getOutputFile() {
		return this.outputFile != null && this.outputFile.length() > 0 ? this.outputFile + ".csv" : "";
	}
	
	/**
	 * Line endings. If empty uses system default (Unix \n, Windows \r\n).
	 */
	@Parameter(property = "lineEnding", required = false)
	private LineEnding lineEnding;
	
	private String getLineEnding() {
		return this.lineEnding != null ? this.lineEnding.getValue() : System.lineSeparator();
	}
	
	private String getComputedLineEnding() {
		return LineEnding.fromValue(this.getLineEnding()).name();
	}
	
	@Override
	public void execute() throws MojoExecutionException {
		
		this.getLog().info("Running io.github.ghokun:updated-maven-plugin:list goal");
		this.getLog().info("Parameters:");
		this.getLog().info("  printHeader  : " + this.printHeader);
		this.getLog().info("  printAll     : " + this.printAll);
		this.getLog().info("  header       : " + this.header);
		this.getLog().info("  template     : " + this.template);
		this.getLog().info("  outputFile   : " + this.getOutputFile());
		this.getLog().info("  lineEnding   : " + this.getComputedLineEnding());
		
		final StringBuilder resultBuilder = new StringBuilder();
		if (this.printHeader) {
			resultBuilder.append(this.header);
		}
		
		int progress = 0;
		if (this.showProgress) {
			this.getLog().info("");
			this.getLog().info("Progress:");
		}
		for (final MavenProject p : this.getProjects()) {
			if (this.showProgress) {
				this
					.getLog()
					.info(++progress + " / " + this.getProjects().size() + " [" + p.getGroupId() + ":"
							+ p.getArtifactId() + "]");
			}
			final VersionRangeResult result = this
				.findLatestVersionOfArtifact(p.getArtifact().getGroupId(), p.getArtifact().getArtifactId(), "[0,)");
			
			if (this.printAll || !p.getVersion().equals(String.valueOf(result.getHighestVersion()))) {
				
				final RemoteRepository remoteRepository = (RemoteRepository) result
					.getRepository(result.getHighestVersion());
				String remoteRepositoryId = null;
				String remoteRepositoryUrl = null;
				if (remoteRepository != null) {
					remoteRepositoryId = remoteRepository.getId();
					remoteRepositoryUrl = remoteRepository.getUrl();
				}
				
				resultBuilder
					.append(resultBuilder.length() > 0 ? this.getLineEnding() : "")
					.append(this.template
						.replace("baseDir", p.getBasedir().getPath())
						.replace("pomPath", p.getFile().getPath())
						.replace("groupId", p.getGroupId())
						.replace("artifactId", p.getArtifactId())
						.replace("localVersion", p.getVersion())
						.replace("remoteVersion", String.valueOf(result.getHighestVersion()))
						.replace("remoteRepositoryId", String.valueOf(remoteRepositoryId))
						.replace("remoteRepositoryUrl", String.valueOf(remoteRepositoryUrl)));
			}
		}
		this.getLog().info("");
		this.getLog().info("Output:" + this.getLineEnding() + resultBuilder.toString());
		this.getLog().info("");
		
		if (this.getOutputFile().length() > 0) {
			try (FileWriter writer = new FileWriter(this.outputFile + ".csv")) {
				writer.write(resultBuilder.toString());
			} catch (final IOException e) {
				this.getLog().error(e);
			}
		}
	}
}