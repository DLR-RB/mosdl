// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl;

import de.dlr.gsoc.mcds.mosdl.generators.GeneratorException;
import de.dlr.gsoc.mcds.mosdl.generators.MosdlGenerator;
import de.dlr.gsoc.mcds.mosdl.loaders.LoaderException;
import java.io.File;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class that is responsible for exposing the MOSDL compiler functionality as Maven plugin.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MosdlMojo extends AbstractMojo {

	private static final Logger logger = LoggerFactory.getLogger(MosdlMojo.class);
	private static final String GENERATED_SOURCES_SUBDIR = "generated-sources/mosdl/";

	@Parameter(property = "generate.serviceSpecs", required = true)
	private List<File> serviceSpecs;

	@Parameter(property = "generate.xml")
	private Boolean xml;

	@Parameter(property = "generate.mosdl")
	private Boolean mosdl;

	@Parameter(property = "generate.skipValidation")
	private Boolean skipValidation;

	@Parameter(property = "generate.mosdl-doc-type", defaultValue = "BULK")
	private MosdlGenerator.DocType mosdlDocType;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${project.build.directory}")
	private String projectBuildDir;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File targetDirectory = new File(projectBuildDir, GENERATED_SOURCES_SUBDIR);
		targetDirectory.mkdirs();
		project.addCompileSourceRoot(targetDirectory.toString());
		logger.debug("MOSDL compilation target directory set to '{}'.", targetDirectory);

		boolean createXml = null != xml && xml;
		boolean createMosdl = null != mosdl && mosdl;
		boolean isSkipValidation = null != skipValidation && skipValidation;

		Runner runner = new MosdlRunner(isSkipValidation, createXml, createMosdl, mosdlDocType);
		for (File serviceFileOrDir : serviceSpecs) {
			logger.info("Compiling service specification in '{}'.", serviceFileOrDir);
			try {
				runner.execute(targetDirectory, serviceFileOrDir);
				logger.debug("Compilation of '{}' successful.", serviceFileOrDir);
			} catch (LoaderException lEx) {
				throw new MojoExecutionException("Specification load error.", lEx);
			} catch (GeneratorException gEx) {
				throw new MojoExecutionException("Generator error.", gEx);
			} catch (Exception ex) {
				throw new MojoExecutionException("Unexpected failure.", ex);
			}
		}
	}

}
