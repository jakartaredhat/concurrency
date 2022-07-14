/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package ee.jakarta.tck.concurrent.spec.ContextService.contextPropagate;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import ee.jakarta.tck.concurrent.framework.TestClient;
import ee.jakarta.tck.concurrent.framework.TestConstants;
import ee.jakarta.tck.concurrent.framework.URLBuilder;
import ee.jakarta.tck.concurrent.spi.context.IntContextProvider;
import ee.jakarta.tck.concurrent.spi.context.StringContextProvider;
import ee.jakarta.tck.concurrent.framework.FullProfileInterceptor;
import jakarta.enterprise.concurrent.spi.ThreadContextProvider;

import static ee.jakarta.tck.concurrent.common.TestGroups.JAKARTAEE_FULL;
import static ee.jakarta.tck.concurrent.common.TestGroups.JAKARTAEE_FULL_PROPERTY;

@Listeners(FullProfileInterceptor.class)
public class ContextPropagationTests extends TestClient {

	public static final String LimitedBeanAppJNDI = "java:app/ContextPropagationTests_ejb/LimitedBean";

	@Deployment(name="ContextPropagationTests", testable=false)
	public static Archive<?> createDeployment() {
		
		final boolean isFullProfile = Boolean.getBoolean(JAKARTAEE_FULL_PROPERTY);
		
		if(isFullProfile) {
			WebArchive war = ShrinkWrap.create(WebArchive.class, "ContextPropagationTests_web.war")
					.addPackages(true, getFrameworkPackage(), getContextPackage(), getContextProvidersPackage())
					.addClasses(
							ContextServiceDefinitionServlet.class,
							ClassloaderServlet.class,
							JNDIServlet.class,
							SecurityServlet.class,
							JSPSecurityServlet.class,
							ContextServiceDefinitionFromEJBServlet.class)
					.addAsServiceProvider(ThreadContextProvider.class.getName(), IntContextProvider.class.getName(), StringContextProvider.class.getName())
					.addAsWebInfResource(ContextPropagationTests.class.getPackage(), "web.xml", "web.xml")
					.addAsWebResource(ContextPropagationTests.class.getPackage(), "jspTests.jsp", "jspTests.jsp");
			
			JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ContextPropagationTests_ejb.jar")
					.addPackages(true, getFrameworkPackage(), ContextPropagationTests.class.getPackage())
					.deleteClasses(
							ContextServiceDefinitionServlet.class,
							JSPSecurityServlet.class,
							ContextServiceDefinitionFromEJBServlet.class)
					.addAsManifestResource(ContextPropagationTests.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
					//TODO document how users can dynamically inject vendor specific deployment descriptors into this archive
			
			EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ContextPropagationTests.ear").addAsModules(war, jar);
			
			return ear;
		} else {
			WebArchive war = ShrinkWrap.create(WebArchive.class, "ContextPropagationTests_web.war")
					.addPackages(true, getFrameworkPackage(), getContextPackage(), getContextProvidersPackage())
					.addClasses(
							ContextServiceDefinitionServlet.class,
							ClassloaderServlet.class,
							SecurityServlet.class,
							JSPSecurityServlet.class)
					.addAsServiceProvider(ThreadContextProvider.class.getName(), IntContextProvider.class.getName(), StringContextProvider.class.getName())
					.addAsWebInfResource(ContextPropagationTests.class.getPackage(), "web.xml", "web.xml")
					.addAsWebResource(ContextPropagationTests.class.getPackage(), "jspTests.jsp", "jspTests.jsp");
						
			return war;
		}

	}
	
	@ArquillianResource(JSPSecurityServlet.class)
	URL jspURL;
	
	@ArquillianResource(ContextServiceDefinitionServlet.class)
	URL contextURL;

	// HttpServletRequest.getUserPrincipal behavior is unclear when accessed from another thread or the current user is changed
	@Test(enabled = false)
	public void testSecurityClearedContext() {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(jspURL).withPaths("jspTests.jsp").withTestName(testName);
		runTest(requestURL);
	}

	// HttpServletRequest.getUserPrincipal behavior is unclear when accessed from another thread or the current user is changed
	@Test(enabled = false)
	public void testSecurityUnchangedContext() {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(jspURL).withPaths("jspTests.jsp").withTestName(testName);
		runTest(requestURL);
	}
	
	@Test
	public void testSecurityPropagatedContext() {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(jspURL).withPaths("jspTests.jsp").withTestName(testName);
		runTest(requestURL);
	}

	/*
	 * @testName: testJNDIContextAndCreateProxyInServlet
	 *
	 * @assertion_ids:
	 * CONCURRENCY:SPEC:85;CONCURRENCY:SPEC:76;CONCURRENCY:SPEC:76.1;
	 * CONCURRENCY:SPEC:76.2;CONCURRENCY:SPEC:76.3;CONCURRENCY:SPEC:77;
	 * CONCURRENCY:SPEC:84;CONCURRENCY:SPEC:2;CONCURRENCY:SPEC:4.1;
	 *
	 * @test_Strategy: create proxy in servlet and pass it into ejb container, then
	 * verify JNDI Context.
	 *
	 */
	@Test(groups = JAKARTAEE_FULL, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
	public void testJNDIContextAndCreateProxyInServlet(@ArquillianResource(JNDIServlet.class) URL jndiURL) {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(jndiURL).withPaths("JNDIServlet").withTestName(testName);
		String resp = runTestWithResponse(requestURL, null);
		this.assertStringInResponse(testName + "failed to get correct result.", "JNDIContextWeb", resp);
	}

	/*
	 * @testName: testJNDIContextAndCreateProxyInEJB
	 *
	 * @assertion_ids:
	 * CONCURRENCY:SPEC:85;CONCURRENCY:SPEC:76;CONCURRENCY:SPEC:76.1;
	 * CONCURRENCY:SPEC:76.2;CONCURRENCY:SPEC:76.3;CONCURRENCY:SPEC:77;
	 * CONCURRENCY:SPEC:84;CONCURRENCY:SPEC:3;CONCURRENCY:SPEC:3.1;
	 * CONCURRENCY:SPEC:3.2;CONCURRENCY:SPEC:3.3;CONCURRENCY:SPEC:3.4;
	 * CONCURRENCY:SPEC:4;
	 *
	 * @test_Strategy: create proxy in servlet and pass it into ejb container, then
	 * verify JNDI Context.
	 *
	 */
	@Test(groups = JAKARTAEE_FULL, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
	public void testJNDIContextAndCreateProxyInEJB(@ArquillianResource(JNDIServlet.class) URL jndiURL) {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(jndiURL).withPaths("JNDIServlet").withTestName(testName);
		String resp = runTestWithResponse(requestURL, null);
		this.assertStringInResponse(testName + "failed to get correct result.", "JNDIContextEJB", resp);
	}

	/*
	 * @testName: testClassloaderAndCreateProxyInServlet
	 *
	 * @assertion_ids:
	 * CONCURRENCY:SPEC:85;CONCURRENCY:SPEC:76;CONCURRENCY:SPEC:76.1;
	 * CONCURRENCY:SPEC:76.2;CONCURRENCY:SPEC:76.3;CONCURRENCY:SPEC:77;
	 * CONCURRENCY:SPEC:84;CONCURRENCY:SPEC:4.2;CONCURRENCY:SPEC:4.4;
	 *
	 * @test_Strategy: create proxy in servlet and pass it into ejb container, then
	 * verify classloader.
	 *
	 */
	@Test(groups = JAKARTAEE_FULL, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
	public void testClassloaderAndCreateProxyInServlet(@ArquillianResource(SecurityServlet.class) URL securityURL) {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(securityURL).withPaths("ClassloaderServlet").withTestName(testName);
		String resp = runTestWithResponse(requestURL, null);
		this.assertStringInResponse(testName + "failed to get correct result.", TestConstants.ComplexReturnValue, resp);
	}

	/*
	 * @testName: testSecurityAndCreateProxyInServlet
	 *
	 * @assertion_ids:
	 * CONCURRENCY:SPEC:85;CONCURRENCY:SPEC:76;CONCURRENCY:SPEC:76.1;
	 * CONCURRENCY:SPEC:76.2;CONCURRENCY:SPEC:76.3;CONCURRENCY:SPEC:77;
	 * CONCURRENCY:SPEC:84;CONCURRENCY:SPEC:4.3;CONCURRENCY:SPEC:4.4;
	 * CONCURRENCY:SPEC:4.4;
	 *
	 * @test_Strategy: create proxy in servlet and pass it into ejb container, then
	 * verify permission.
	 *
	 */
	@Test(groups = JAKARTAEE_FULL, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
	public void testSecurityAndCreateProxyInServlet(@ArquillianResource(ClassloaderServlet.class) URL classloaderURL) {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(classloaderURL).withPaths("SecurityServlet").withTestName(testName);
		String resp = runTestWithResponse(requestURL, null);
		this.assertStringInResponse(testName + "failed to get correct result.", TestConstants.ComplexReturnValue, resp);
	}
	
    /**
     * A ContextServiceDefinition with all attributes configured
     * propagates/clears/ignores context types as configured.
     * ContextA, which is tested here, propagates Application context and IntContext,
     * clears StringContext, and leaves Transaction context unchanged.
     */
	@Test
    public void testContextServiceDefinitionAllAttributes() throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(contextURL).withPaths("ContextServiceDefinitionServlet").withTestName(testName);
		runTest(requestURL);
    }
	
    /**
     * A ContextServiceDefinition defined in an EJB with all attributes configured
     * propagates/clears/ignores context types as configured.
     * ContextA, which is tested here, propagates Application context and IntContext,
     * clears StringContext, and leaves Transaction context unchanged.
     */
	@Test(groups = JAKARTAEE_FULL, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testContextServiceDefinitionFromEJBAllAttributes(@ArquillianResource(ContextServiceDefinitionFromEJBServlet.class)
	URL ejbContextURL) throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(ejbContextURL).withPaths("ContextServiceDefinitionFromEJBServlet").withTestName(testName);
		runTest(requestURL);
    }

    /**
     * A ContextServiceDefinition with minimal attributes configured
     * clears transaction context and propagates other types.
     */
	@Test
    public void testContextServiceDefinitionDefaults() throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(contextURL).withPaths("ContextServiceDefinitionServlet").withTestName(testName);
		runTest(requestURL);
    }
	
    /**
     * A ContextServiceDefinition defined in an EJB with minimal attributes configured
     * clears transaction context and propagates other types.
     */
	@Test(groups = JAKARTAEE_FULL, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testContextServiceDefinitionFromEJBDefaults(@ArquillianResource(ContextServiceDefinitionFromEJBServlet.class)
	URL ejbContextURL) throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(ejbContextURL).withPaths("ContextServiceDefinitionFromEJBServlet").withTestName(testName);
		runTest(requestURL);
    }

    /**
     * A ContextServiceDefinition can specify a third-party context type to be propagated/cleared/ignored.
     * This test uses 2 ContextServiceDefinitions:
     * ContextA with IntContext propagated and StringContext cleared.
     * ContextB with IntContext unchanged and StringContext propagated (per ALL_REMAINING).
     */
	@Test
    public void testContextServiceDefinitionWithThirdPartyContext() throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(contextURL).withPaths("ContextServiceDefinitionServlet").withTestName(testName);
		runTest(requestURL);
    }

    /**
     * A ContextService contextualizes a Consumer, which can be supplied as a dependent stage action
     * to an unmanaged CompletableFuture. The dependent stage action runs with the thread context of
     * the thread that contextualizes the Consumer, per the configuration of the ContextServiceDefinition.
     */
	@Test
    public void testContextualConsumer() throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(contextURL).withPaths("ContextServiceDefinitionServlet").withTestName(testName);
		runTest(requestURL);
    }

    /**
     * A ContextService contextualizes a Function, which can be supplied as a dependent stage action
     * to an unmanaged CompletableFuture. The dependent stage action runs with the thread context of
     * the thread that contextualizes the Function, per the configuration of the ContextServiceDefinition.
     */
	@Test
    public void testContextualFunction() throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(contextURL).withPaths("ContextServiceDefinitionServlet").withTestName(testName);
		runTest(requestURL);
    }

    /**
     * A ContextService contextualizes a Supplier, which can be supplied as a dependent stage action
     * to an unmanaged CompletableFuture. The dependent stage action runs with the thread context of
     * the thread that contextualizes the Supplier, per the configuration of the ContextServiceDefinition.
     */
	@Test
    public void testContextualSupplier() throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(contextURL).withPaths("ContextServiceDefinitionServlet").withTestName(testName);
		runTest(requestURL);
    }
	
    /**
     * A ContextService contextualizes a Supplier, which can be supplied as a dependent stage action
     * to an unmanaged CompletableFuture. The dependent stage action runs with the thread context of
     * the thread that contextualizes the Supplier, per the configuration of the ContextServiceDefinition.
     */
	@Test(groups = JAKARTAEE_FULL, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testContextualSupplierEJB(@ArquillianResource(ContextServiceDefinitionFromEJBServlet.class)
	URL ejbContextURL) throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(ejbContextURL).withPaths("ContextServiceDefinitionFromEJBServlet").withTestName("testContextualSupplier");
        runTest(requestURL);
    }

    /**
     * ContextService can create a contextualized copy of an unmanaged CompletableFuture.
     */
	@Test
    public void testCopyWithContextCapture() throws Throwable {
		URLBuilder requestURL = URLBuilder.get().withBaseURL(contextURL).withPaths("ContextServiceDefinitionServlet").withTestName(testName);
		runTest(requestURL);
    }
}
