package de.dlr.gsoc.mcds.mosdl.integrationtests;

import de.dlr.gsoc.mcds.mosdl.CliMain;
import de.dlr.gsoc.mcds.mosdl.TestUtils;

import java.io.File;
import java.security.Permission;

import org.junit.jupiter.api.AfterAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CliMainIT {

	private static CliMain.SystemExiter oldSystemExiter;

	@BeforeAll
	static void init() {
		oldSystemExiter = CliMain.getSystemExiter();
		CliMain.setSystemExiter(status -> {
			throw new SystemExitError(status);
		});
	}

	@AfterAll
	static void tearDown() {
		CliMain.setSystemExiter(oldSystemExiter);
	}

	@Test
	void testHelp() throws Exception {
		String[] args = {"--help"};
		SystemExitError see = assertThrows(SystemExitError.class, () -> CliMain.main(args));
		assertEquals(0, see.getStatus());
	}

	@Test
	void testMosdlToXml(@TempDir File tempDir) throws Exception {
		File srcFile = TestUtils.getResource("/mosdl/VerySimpleService.mosdl");
		String[] args = {srcFile.getAbsolutePath(), tempDir.getAbsolutePath(), "--xml"};
		CliMain.main(args);
		assertEquals(1, tempDir.listFiles().length);
	}

	@Test
	void testXmlToMosdl(@TempDir File tempDir) throws Exception {
		File srcFile = TestUtils.getResource("/xml/MosdlDocExample.xml");
		String[] args = {srcFile.getAbsolutePath(), tempDir.getAbsolutePath(), "-m", "--doc-type", "BULK"};
		CliMain.main(args);
		assertEquals(1, tempDir.listFiles().length);
	}

	private static class SystemExitError extends Error {

		private final int status;

		private static final long serialVersionUID = 1L;

		public SystemExitError(int status) {
			this.status = status;
		}

		public int getStatus() {
			return status;
		}
	}

}
