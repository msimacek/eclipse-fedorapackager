package org.fedoraproject.eclipse.packager.koji.api;

import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginExceptionTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ KojiBuildCommandTest.class, KojiBuildInfoTest.class,
		KojiHubClientLoginExceptionTest.class, KojiScratchWithSRPMTest.class,
		KojiSSLHubClientTest.class, TestAbstractKojiHubBaseClientTest.class })
public class AllKojiTests {
	// none
}
