package com.sixsq.slipstream.util;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration.RequiredParameters;

public abstract class CommonTestUtil {

	protected static final String PASSWORD = "password";

	// Need to set cloudServiceName before the status user is
	// created, since the createUser method uses it
	public static final String cloudServiceName = new LocalConnector()
			.getCloudServiceName();

	public static User createTestUser() throws ConfigurationException,
			ValidationException {
		return createUser("test", PASSWORD);
	}

	public static User createUser(String name) throws ConfigurationException,
			ValidationException {
		return CommonTestUtil.createUser(name, null);
	}

	public static User createUser(String name, String password)
			throws ConfigurationException, ValidationException {
		User user = User.loadByName(name);
		if (user != null) {
			try {
				user.remove();
			} catch (Exception ex) {

			}
		}

		try {
			user = new User(name);
		} catch (ValidationException e) {
			e.printStackTrace();
			throw (new SlipStreamRuntimeException(e));
		}
		try {
			user.hashAndSetPassword(password);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			fail();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}

		try {
			user.setDefaultCloudServiceName(cloudServiceName);
		} catch (ValidationException e) {
			throw (new SlipStreamRuntimeException(e));
		}

		return user.store();
	}

	public static void deleteUser(User user) {
		if (user != null) {
			user.remove();
		}
	}

	public static void addSshKeys(User user) throws ValidationException {
		UserParameter userKey = new UserParameter(
				UserParametersFactoryBase.getPublicKeyParameterName(), "xxx",
				"xxx");
		user.setParameter(userKey);

		String publicSshKey = "cloud.connector.orchestrator.publicsshkey";
		Configuration config = Configuration.getInstance();

		config.getParameters().setParameter(
				new ServiceConfigurationParameter(publicSshKey, "/dev/null"));
		config.store();
	}

	public static DeploymentModule createDeployment()
			throws ValidationException, NotFoundException {

		ImageModule imageForDeployment1 = new ImageModule("imagefordeployment1");
		imageForDeployment1
				.setParameter(new ModuleParameter("pi1", "pi1 init value",
						"pi1 parameter desc", ParameterCategory.Input));
		imageForDeployment1.setParameter(new ModuleParameter("po1",
				"po1 init value", "po1 parameter desc",
				ParameterCategory.Output));

		imageForDeployment1.setIsBase(true);
		imageForDeployment1.setImageId("123", CommonTestUtil.cloudServiceName);
		imageForDeployment1 = imageForDeployment1.store();

		ImageModule imageForDeployment2 = new ImageModule("imagefordeployment2");
		imageForDeployment2
				.setParameter(new ModuleParameter("pi2", "pi2 init value",
						"pi2 parameter desc", ParameterCategory.Input));
		imageForDeployment2.setParameter(new ModuleParameter("po2",
				"po2 init value", "po2 parameter desc",
				ParameterCategory.Output));
		imageForDeployment2.setImageId("123", CommonTestUtil.cloudServiceName);
		imageForDeployment2 = imageForDeployment2.store();

		DeploymentModule deployment = new DeploymentModule("test/deployment");

		Node node;

		node = new Node("node1", imageForDeployment1);
		deployment.getNodes().put(node.getName(), node);

		node = new Node("node2", imageForDeployment2);
		deployment.getNodes().put(node.getName(), node);

		return deployment.store();
	}

	public static void deleteDeployment(DeploymentModule deployment) {
		for (Node n : deployment.getNodes().values()) {
			n.getImage().remove();
		}
		deployment.remove();
	}

	public static void resetAndLoadConnector(
			Class<? extends Connector> connectorClass, String instanceName)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException, ConfigurationException, ValidationException {

		// Instantiate the configuration force loading from disk
		// This is required otherwise the first loading from
		// disk will reset the connectors
		Configuration.getInstance();

		Map<String, Connector> connectors = new HashMap<String, Connector>();
		Connector connector = ConnectorFactory
				.instantiateConnectorFromName(connectorClass.getName());

		String connectorName = instanceName == null ? connector
				.getCloudServiceName() : instanceName;

		connectors.put(connectorName, connector);
		ConnectorFactory.setConnectors(connectors);
	}

	public static void resetAndLoadConnector(
			Class<? extends Connector> connectorClass)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException, ConfigurationException, ValidationException {

		resetAndLoadConnector(connectorClass, null);
	}

	public static void cleanupModules() {
		List<Module> modules = Module.listAll();
		for (Module m : modules) {
			try {
				m.remove();
			} catch (Exception ex) {
			}
		}
	}

	// FIME: duplicate from ResourceTestBase
	public static void setCloudConnector(String connectorClassName)
			throws ConfigurationException {
		Configuration configuration = null;
		try {
			configuration = Configuration.getInstance();
		} catch (ValidationException e) {
			fail();
		}

		ServiceConfiguration sc = configuration.getParameters();
		try {
			sc.setParameter(new ServiceConfigurationParameter(
					RequiredParameters.CLOUD_CONNECTOR_CLASS.getName(),
					connectorClassName));
		} catch (ValidationException e) {
			fail();
		}
		sc.store();
		ConnectorFactory.resetConnectors();
	}

	// Only static methods. Ensure no instances are created.
	public CommonTestUtil() {

	}

}
