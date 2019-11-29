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

	private static SecurityManager oldSecurityMgr;

	@BeforeAll
	static void init() {
		oldSecurityMgr = System.getSecurityManager();
		System.setSecurityManager(new CatchExitSecurityManager());
	}

	@AfterAll
	static void tearDown() {
		System.setSecurityManager(oldSecurityMgr);
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

	private static class CatchExitSecurityManager extends SecurityManager {

		@Override
		public void checkExit(int status) {
			throw new SystemExitError(status);
		}

		@Override
		public void checkPermission(Permission perm) {
			if (null != oldSecurityMgr) {
				oldSecurityMgr.checkPermission(perm);
			}
		}

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
