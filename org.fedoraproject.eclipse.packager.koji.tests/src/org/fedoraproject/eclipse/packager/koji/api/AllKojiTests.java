package org.fedoraproject.eclipse.packager.koji.api;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ KojiBuildCommandTest.class, KojiBuildInfoTest.class,
		KojiClientTest.class, KojiScratchWithSRPMTest.class,
		KojiSSLHubClientTest.class, TestAbstractKojiHubBaseClientTest.class })
public class AllKojiTests {
	// none
}
