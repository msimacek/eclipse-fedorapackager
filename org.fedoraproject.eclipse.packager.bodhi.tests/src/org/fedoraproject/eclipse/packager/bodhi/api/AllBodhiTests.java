package org.fedoraproject.eclipse.packager.bodhi.api;

import org.fedoraproject.eclipse.packager.bodhi.fas.DateTimeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	BodhiClientTest.class,
	BodhiLoginResponseTest.class,
	BodhiUpdateResponseTest.class,
	PushUpdateCommandTest.class,
	DateTimeTest.class
})

public class AllBodhiTests {

}
