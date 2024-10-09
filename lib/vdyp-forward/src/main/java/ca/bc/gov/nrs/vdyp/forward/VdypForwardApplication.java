package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_1;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_2;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_3;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_4;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;

public class VdypForwardApplication extends VdypApplication {

	static {
		try {
			LogManager.getLogManager().readConfiguration(
					ForwardProcessor.class.getClassLoader().getResourceAsStream("logging.properties")
			);
		} catch (SecurityException | IOException e) {
			System.err.println("Unable to configure logging system");
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(VdypForwardApplication.class);

	public static final int CONFIG_LOAD_ERROR = 1; // TODO check what Fortran VDYP Forward would exit with.
	public static final int PROCESSING_ERROR = 2; // TODO check what Fortran VDYP Forward would exit with.

	public static final String DEFAULT_VDYP_CONTROL_FILE_NAME = "vdyp.ctr";

	private static Set<ForwardPass> vdypPassSet = new HashSet<>(Arrays.asList(PASS_1, PASS_2, PASS_3, PASS_4, PASS_5));

	public static void main(final String... args) {

		var app = new VdypForwardApplication();

		app.logVersionInformation();

		List<String> controlFileNames = null;

		try {
			if (args.length == 0) {
				System.out.print("Enter name of VDYP control file (or RETURN for vdyp.ctr) or *name for both): ");

				controlFileNames = new ArrayList<>();
				try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
					String userResponse = br.readLine();
					if (userResponse.length() == 0) {
						controlFileNames.add(DEFAULT_VDYP_CONTROL_FILE_NAME);
					} else if (userResponse.startsWith("*")) {
						controlFileNames.add(DEFAULT_VDYP_CONTROL_FILE_NAME);

						userResponse = userResponse.substring(1);
						controlFileNames.addAll(Arrays.asList(userResponse.split("[[:space:]]+")));
					}
				}
			} else {
				controlFileNames = Arrays.asList(args);
			}
		} catch (Exception ex) {
			logger.error("Error during initialization", ex);
			System.exit(CONFIG_LOAD_ERROR);
		}

		try {
			ForwardProcessor processor = new ForwardProcessor();

			processor.run(new FileSystemFileResolver(), new FileSystemFileResolver(), controlFileNames, vdypPassSet);

		} catch (Exception ex) {
			logger.error("Error during processing", ex);
			System.exit(PROCESSING_ERROR);
		}
	}

	private void logVersionInformation() {
		logger.info("{} {}", RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("{} Ver:{} {}", RESOURCE_BINARY_NAME, RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("VDYP7 Support Ver: {}", AVERSION);
	}

	@Override
	public VdypApplicationIdentifier getId() {
		return VdypApplicationIdentifier.VDYP_FORWARD;
	}
}
