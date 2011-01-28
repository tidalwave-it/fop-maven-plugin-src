package ch.becompany;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Goal which invokes Apache FOP.
 * 
 * @goal fop
 * @phase process-sources
 */
public class FopMojo extends AbstractMojo {
    private FopFactory fopFactory = FopFactory.newInstance();

    /**
     * Output directory.
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Location of the XSLT file.
     * @parameter
     * @required
     */
    private File xslFile;

    /**
     * Fileset of input files
     * @parameter
     * @required
     */
    private DirectoryScanner inputFiles;

    /**
     * FOP target resolution.
     * @parameter expression="${fop.targetResolution}" default-value="300"
     */
    private Float targetResolution;

    /**
     * FOP base URL.
     * @parameter expression="${project.src.directory}"
     */
    private String baseUrl;

    /**
     * FOP user configuration file.
     * @parameter expression="${fop.userConfigFile}"
     */
    private File userConfigFile;

    public void execute() throws MojoExecutionException {
        /* log some parameters */
        getLog().debug("xslFile=" + xslFile);
        getLog().debug("outputDirectory=" + outputDirectory);

        if (this.userConfigFile != null) {
            try {
                fopFactory.setUserConfig(this.userConfigFile);
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to configure FOP factory: ", e);
            }
        }

        /* create output directory */
        if (!outputDirectory.exists()) {
            outputDirectory.mkdir();
        }

        /* iterate over input files */
        inputFiles.scan();
        for (String filename : inputFiles.getIncludedFiles()) {
            getLog().debug(
                    "processing " + new File(inputFiles.getBasedir(), filename).getAbsolutePath());
            try {
                processInputFile(filename);
            } catch (Exception e) {
                getLog().error(e);
            }
        }
    }

    /**
     * Processes a single input file.
     * 
     */
    private void processInputFile(String filename) throws Exception {
        final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        if (this.baseUrl != null) {
            foUserAgent.setBaseURL(this.baseUrl);
        }
        if (this.targetResolution != null) {
            foUserAgent.setTargetResolution(this.targetResolution);
        }

        final File outputFile = new File(outputDirectory, getRelativeOutputFilename(filename));
        if (outputFile.createNewFile()) {
            getLog().debug("output file created: " + outputFile);
        }
        final OutputStream out = new BufferedOutputStream(new java.io.FileOutputStream(outputFile));

        try {
            final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            final TransformerFactory factory = TransformerFactory.newInstance();
            final Transformer transformer = factory.newTransformer(new StreamSource(xslFile));

            final File inputFile = new File(inputFiles.getBasedir(), filename);
            final Source src = new StreamSource(inputFile);
            final Result res = new SAXResult(fop.getDefaultHandler());

            transformer.transform(src, res);
        } finally {
            out.close();
        }
    }

    private String getRelativeOutputFilename(final String filename) {
        return (filename.endsWith(".xml") ? filename.substring(0, filename.length() - 4) : filename)
                + ".pdf";
    }
}
