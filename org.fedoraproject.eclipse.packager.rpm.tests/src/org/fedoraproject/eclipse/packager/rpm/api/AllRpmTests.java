package org.fedoraproject.eclipse.packager.rpm.api;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ FedoraPackagerTest.class, MockBuildCommandTest.class,
		RpmBuildCommandTest.class, RpmEvalCommandTest.class,
		SCMMockBuildCommandTest.class, SRPMImportCommandTest.class })
public class AllRpmTests {
	// empty
}
