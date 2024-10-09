package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_1;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_2;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_3;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_4;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_5;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.ZipOutputFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ForwardProcessorZipOutputStreamTest {

	private static final Logger logger = LoggerFactory.getLogger(ForwardProcessorZipOutputStreamTest.class);

	private static Set<ForwardPass> vdypPassSet = new HashSet<>(Arrays.asList(PASS_1, PASS_2, PASS_3, PASS_4, PASS_5));

	@TempDir
	Path outputFilesLocation;

	@Test
	void test() throws IOException, ResourceParseException, ProcessingException {

		ForwardProcessor fp = new ForwardProcessor();

		FileResolver inputFileResolver = TestUtils.fileResolver(TestUtils.class);

		var outputResolver = new ZipOutputFileResolver();

		fp.run(inputFileResolver, outputResolver, List.of("VDYP.CTR"), vdypPassSet);

		Path resourceDirectory = Paths.get("src", "test", "resources", "output");
		Files.createDirectories(resourceDirectory);

		Path zipFilePath = outputFilesLocation.resolve(this.getClass().getSimpleName() + ".zip");
		Path zipFileFromStreamPath = outputFilesLocation.resolve(this.getClass().getSimpleName() + "-fromtream.zip");

		System.out.println("Writing output to " + outputFilesLocation.toString());

		outputResolver.generate(zipFilePath);
		byte[] zipFileBytes = Files.readAllBytes(zipFilePath);

		InputStream is = outputResolver.generateStream();
		byte[] zipStreamBytes = is.readAllBytes();
		Files.write(zipFileFromStreamPath, zipStreamBytes);

		assertTrue(zipFileBytes.length == zipStreamBytes.length);

		try (ZipFile zipFileFromStream = new ZipFile(zipFileFromStreamPath.toFile())) {
			try (ZipFile zipFileFromFile = new ZipFile(zipFilePath.toFile())) {

				var streamEntries = zipFileFromStream.entries().asIterator();
				var fileEntries = zipFileFromFile.entries().asIterator();

				while (streamEntries.hasNext()) {
					assertTrue(fileEntries.hasNext());

					var streamEntry = streamEntries.next();
					var fileEntry = fileEntries.next();

					logger.info("Saw file entry {} and stream entry {}", fileEntry.getName(), streamEntry.getName());
					assertTrue(streamEntry.hashCode() == fileEntry.hashCode());
					assertTrue(streamEntry.getName().equals(fileEntry.getName()));
				}

				assertFalse(fileEntries.hasNext());
			}
		}
	}
}
