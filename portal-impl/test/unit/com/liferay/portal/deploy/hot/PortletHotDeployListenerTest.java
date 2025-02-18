/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.deploy.hot;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.model.PortletApp;
import com.liferay.portal.kernel.resource.bundle.ResourceBundleLoader;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.ServletContext;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.mockito.Mockito;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Dante Wang
 */
public class PortletHotDeployListenerTest {

	@ClassRule
	public static LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testResourceBundle() {
		PortletHotDeployListener portletHotDeployListener =
			new PortletHotDeployListener();

		Portlet portlet = Mockito.mock(Portlet.class);

		Class<?> clazz = getClass();

		Mockito.when(
			portlet.getContextName()
		).thenReturn(
			clazz.getName()
		);

		PortletApp portletApp = _mockPortletApp(clazz);

		Mockito.when(
			portlet.getPortletApp()
		).thenReturn(
			portletApp
		);

		Mockito.when(
			portlet.getPortletId()
		).thenReturn(
			"TestPortlet"
		);

		Mockito.when(
			portlet.getResourceBundle()
		).thenReturn(
			clazz.getPackageName() + ".dependencies.TestPortlet"
		);

		portletHotDeployListener.checkResourceBundles(
			clazz.getClassLoader(), portlet);

		Map<String, ServiceRegistration<ResourceBundleLoader>>
			resourceBundleLoaderServiceRegistrations =
				ReflectionTestUtil.getFieldValue(
					portletHotDeployListener,
					"_resourceBundleLoaderServiceRegistrations");

		Assert.assertNull(
			resourceBundleLoaderServiceRegistrations.get("FakePortlet"));
		Assert.assertNotNull(
			resourceBundleLoaderServiceRegistrations.get("TestPortlet"));

		Map<String, Set<ServiceRegistration<ResourceBundle>>>
			resourceBundleServiceRegistrations =
				ReflectionTestUtil.getFieldValue(
					portletHotDeployListener,
					"_resourceBundleServiceRegistrations");

		Set<ServiceRegistration<ResourceBundle>> serviceRegistrations =
			resourceBundleServiceRegistrations.get(clazz.getName());

		Assert.assertNotNull(serviceRegistrations);
		Assert.assertEquals(
			serviceRegistrations.toString(), 1, serviceRegistrations.size());

		ServiceRegistration<?>[] serviceRegistrationsArray =
			serviceRegistrations.toArray(new ServiceRegistration<?>[0]);

		ServiceRegistration<?> serviceRegistration =
			serviceRegistrationsArray[0];

		ServiceReference<?> serviceReference =
			serviceRegistration.getReference();

		Assert.assertEquals(
			"en_US", serviceReference.getProperty("language.id"));
	}

	private PortletApp _mockPortletApp(Class<?> clazz) {
		PortletApp portletApp = Mockito.mock(PortletApp.class);

		ServletContext servletContext = Mockito.mock(ServletContext.class);

		String path = StringUtil.replace(
			"/WEB-INF/classes/" + clazz.getPackageName() +
				".dependencies.TestPortlet",
			CharPool.PERIOD, CharPool.SLASH);

		String resourcePath = path.substring(
			0, path.lastIndexOf(StringPool.SLASH));

		Mockito.when(
			servletContext.getResourcePaths(Mockito.anyString())
		).thenReturn(
			Set.of(
				resourcePath + "/FakePortlet_Fake_Resource_Name.bin",
				resourcePath + "/FakePortlet_zh_CN.properties",
				resourcePath + "/TestPortlet_en_US.properties")
		);

		Mockito.when(
			portletApp.getServletContext()
		).thenReturn(
			servletContext
		);

		return portletApp;
	}

}