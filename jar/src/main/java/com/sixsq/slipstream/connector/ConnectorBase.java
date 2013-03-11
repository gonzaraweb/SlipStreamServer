package com.sixsq.slipstream.connector;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.NotImplementedException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ExtraDisk;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

public abstract class ConnectorBase implements Connector {

	private static Logger log = Logger
			.getLogger(ConnectorBase.class.toString());

	protected static Logger getLog() {
		return log;
	}

	protected static String ORCHESTRATOR_INSTANCE_ID_NAME = Run.ORCHESTRATOR_NAME_PREFIX
			+ RuntimeParameter.INSTANCE_ID_KEY;
	private static String ORCHESTRATOR_INSTANCE_ID_DESCRIPTION = "Orchestrator instance id";
	protected static String ORCHESTRATOR_INSTANCE_HOSTNAME = Run.ORCHESTRATOR_NAME_PREFIX
			+ RuntimeParameter.HOSTNAME_KEY;
	private static String ORCHESTRATOR_INSTANCE_HOSTNAME_DESCRIPTION = "Orchestrator instance hostname/IP";

	private static final String MACHINE_INSTANCE_ID_NAME = Run.MACHINE_NAME_PREFIX
			+ RuntimeParameter.INSTANCE_ID_KEY;
	private static String MACHINE_INSTANCE_ID_DESCRIPTION = "Machine instance id";
	protected static String MACHINE_INSTANCE_HOSTNAME = Run.MACHINE_NAME_PREFIX
			+ RuntimeParameter.HOSTNAME_KEY;
	private static String MACHINE_INSTANCE_HOSTNAME_DESCRIPTION = "Machine instance hostname/IP";

	protected static final String SLIPSTREAM_REPORT_DIR = "/tmp/slipstream/reports";

	protected Map<String, Map<String, String>> extraDisksInfo = new HashMap<String, Map<String, String>>();

	protected static final String EXTRADISK_KEY_REGEX = "regex";
	protected static final String EXTRADISK_KEY_REGEXERROR = "regexError";
	protected static final String EXTRADISK_KEY_DESCRIPTION = "description";

	private File tempSshKeyFile;

	private final String instanceName;

	public ConnectorBase(String instanceName) {
		this.instanceName = instanceName;
	}

	protected static Credentials getCredentialsObject(User user)
			throws ConfigurationException, ValidationException {
		return ConnectorFactory.getCurrentConnector(user).getCredentials(user);
	}

	protected String getImageId(Run run)
			throws SlipStreamClientException, ConfigurationException {

		String imageId;

		if (run.getType() == RunType.Orchestration) {
			imageId = getOrchestratorImageId();
		} else {
			imageId = ((ImageModule) run.getModule()).extractBaseImageId(run
	                   .getCloudServiceName());
		}
		return imageId;
	}

	abstract protected String getOrchestratorImageId()
			throws ConfigurationException, ValidationException;

	protected String getDefaultCloudServiceName(User user)
			throws ValidationException {
		return user.getDefaultCloudService();
	}

	@Override
	public String getCloudServiceName() {
		return "unknown";
	}

	public String getConnectorInstanceName() {
		return instanceName;
	}

	public void abort(Run run, User user)
			throws ServerExecutionEnginePluginException {
		return;
	}

	public void checkCredentials(Credentials credentials)
			throws InvalidElementException {
		if (credentials.getKey() == null) {
			throw (new InvalidElementException("Missing Cloud account key."));
		}
		if (credentials.getSecret() == null) {
			throw (new InvalidElementException("Missing Cloud account secret."));
		}
	}

	protected Run updateInstanceIdAndIpOnRun(Run run, String instanceId,
			String ipAddress) throws NotFoundException, ValidationException,
			ServerExecutionEnginePluginException {
		return updateInstanceIdAndIpOnRun(run, instanceId, ipAddress,
				getOrchestratorName(run));
	}

	protected Run updateInstanceIdAndIpOnRun(Run run, String instanceId,
			String ipAddress, String orchestratorName)
			throws NotFoundException, ValidationException,
			ServerExecutionEnginePluginException {

		if (run.getType() == RunType.Orchestration) {
			updateOrchestratorInstanceIdOnRun(run, instanceId, orchestratorName);
			updateOrchestratorInstanceIpOnRun(run, ipAddress, orchestratorName);
		} else {
			updateMachineInstanceIdOnRun(run, instanceId);
			updateMachineInstanceIpOnRun(run, ipAddress);
		}
		return run;
	}

	private void updateOrchestratorInstanceIdOnRun(Run run, String instanceId,
			String orchestratorName) throws NotFoundException,
			ValidationException {

		String orchestratorInstanceIdName = orchestratorName
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
				+ RuntimeParameter.INSTANCE_ID_KEY;

		try {
			run.assignRuntimeParameter(orchestratorInstanceIdName, instanceId,
					ORCHESTRATOR_INSTANCE_ID_DESCRIPTION);
		} catch (ValidationException ex) {
			run.updateRuntimeParameter(orchestratorInstanceIdName, instanceId);
		}
	}

	private void updateMachineInstanceIdOnRun(Run run, String instanceId)
			throws NotFoundException, ValidationException {
		try {
			run.assignRuntimeParameter(MACHINE_INSTANCE_ID_NAME, instanceId,
					MACHINE_INSTANCE_ID_DESCRIPTION);
		} catch (ValidationException ex) {
			run.updateRuntimeParameter(MACHINE_INSTANCE_ID_NAME, instanceId);
		}
	}

	private void updateOrchestratorInstanceIpOnRun(Run run,
			String instanceHostname, String orchestratorName)
			throws NotFoundException, ValidationException {

		String machineInstanceHostname = orchestratorName
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
				+ RuntimeParameter.HOSTNAME_KEY;

		try {
			run.assignRuntimeParameter(machineInstanceHostname,
					instanceHostname,
					ORCHESTRATOR_INSTANCE_HOSTNAME_DESCRIPTION);
		} catch (ValidationException ex) {
			run.updateRuntimeParameter(machineInstanceHostname,
					instanceHostname);
		}
	}

	private void updateMachineInstanceIpOnRun(Run run, String instanceHostname)
			throws NotFoundException, ValidationException {
		try {
			run.assignRuntimeParameter(MACHINE_INSTANCE_HOSTNAME,
					instanceHostname, MACHINE_INSTANCE_HOSTNAME_DESCRIPTION);
		} catch (ValidationException ex) {
			run.updateRuntimeParameter(MACHINE_INSTANCE_HOSTNAME,
					instanceHostname);
		}
	}

	protected void defineExtraDisk(String name, String description,
			String regex, String regexError) {
		Map<String, String> diskInfo = new HashMap<String, String>();
		diskInfo.put(EXTRADISK_KEY_DESCRIPTION, description);
		diskInfo.put(EXTRADISK_KEY_REGEX, regex);
		diskInfo.put(EXTRADISK_KEY_REGEXERROR, regexError);

		extraDisksInfo.put(name, Collections.unmodifiableMap(diskInfo));
	}

	public List<ExtraDisk> getExtraDisks() {
		List<ExtraDisk> disks = new ArrayList<ExtraDisk>();

		for (Map.Entry<String, Map<String, String>> entry : extraDisksInfo
				.entrySet()) {
			String name = entry.getKey();
			Map<String, String> valuesList = entry.getValue();
			ExtraDisk disk = new ExtraDisk(name,
					valuesList.get(EXTRADISK_KEY_DESCRIPTION), this);
			disks.add(disk);
		}

		return disks;
	}

	public void validateExtraDiskParameter(String name, String param)
			throws ValidationException {
		if (param == null || param.isEmpty()) {
			return;
		}
		Map<String, String> diskInfo = extraDisksInfo.get(name);
		if (!param.matches(diskInfo.get(EXTRADISK_KEY_REGEX)))
			throw (new ValidationException(
					diskInfo.get(EXTRADISK_KEY_REGEXERROR)));
	}

	protected String getPublicSshKeyFileName(Run run, User user)
			throws IOException, ValidationException {
		String publicSshKey;
		if (run.getType() == RunType.Machine) {
			File tempSshKeyFile = File.createTempFile("sshkey", ".tmp");
			BufferedWriter out = new BufferedWriter(new FileWriter(
					tempSshKeyFile));
			String sshPublicKey = user
					.getParameter(
							constructKey(UserParametersFactoryBase.SSHKEY_PARAMETER_NAME))
					.getValue();
			out.write(sshPublicKey);
			out.close();
			publicSshKey = tempSshKeyFile.getPath();
		} else {
			publicSshKey = Configuration.getInstance().getProperty(
					"cloud.connector.security.publicsshkey");
		}
		return publicSshKey;
	}

	protected void deleteTempSshKeyFile() {
		if (tempSshKeyFile != null) {
			tempSshKeyFile.delete();
		}
	}

	protected List<String> getCloudNodeInstanceIds(Run run)
			throws NotFoundException, ValidationException {
		List<String> ids = new ArrayList<String>();

		for (String nodeName : run.getNodeNameList()) {
			nodeName = nodeName.trim();

			String idKey = nodeName + RuntimeParameter.NODE_PROPERTY_SEPARATOR
					+ RuntimeParameter.INSTANCE_ID_KEY;

			String cloudServiceKey = nodeName
					+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
					+ RuntimeParameter.CLOUD_SERVICE_NAME;

			String id = (String) run.getRuntimeParameterValueIgnoreAbort(idKey);
			String cloudService = run
					.getRuntimeParameterValueIgnoreAbort(cloudServiceKey);

			if (id != null
					&& this.getConnectorInstanceName().equals(cloudService)) {
				ids.add(id);
			}
		}
		return ids;
	}

	public Map<String, UserParameter> getUserParametersTemplate()
			throws ValidationException {
		throw (new NotImplementedException());
	}

	public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate()
			throws ValidationException {
		throw (new NotImplementedException());
	}

	public Map<String, ModuleParameter> getImageParametersTemplate()
			throws ValidationException {
		return new HashMap<String, ModuleParameter>();
	}

	protected String generateCookie(String identifier) {
		return CookieUtils.getCookieName() + "="
				+ CookieUtils.createCookieValue("local", identifier)
				+ "; Path:/";
	}

	protected String getCookieForEnvironmentVariable(String identifier) {
		return "\"" + generateCookie(identifier) + "\"";
	}

	protected abstract String constructKey(String key)
			throws ValidationException;

	protected String getVerboseParameterValue(User user)
			throws ValidationException {
		return user
				.getParameterValue(
						new ExecutionControlUserParametersFactory()
								.constructKey(ExecutionControlUserParametersFactory.VERBOSITY_LEVEL),
						ExecutionControlUserParametersFactory.VERBOSITY_LEVEL_DEFAULT);
	}

	public String getOrchestratorName(Run run) {
		String orchestratorName = Run.ORCHESTRATOR_NAME;

		if (run.getType() == RunType.Orchestration
				&& run.getCategory() == ModuleCategory.Deployment) {
			orchestratorName += "-" + getConnectorInstanceName();
		}

		return orchestratorName;
	}

	protected String getInstanceType() {
		// TODO Auto-generated method stub
		return null;
	}

	protected String getInstanceType(ImageModule image)
			throws ValidationException {
		return getParameterValue(ImageModule.INSTANCE_TYPE_KEY, image);
	}

	protected String getCpu(ImageModule image) throws ValidationException {
		return getParameterValue(ImageModule.CPU_KEY, image);
	}

	protected String getRam(ImageModule image) throws ValidationException {
		return getParameterValue(ImageModule.RAM_KEY, image);
	}

	protected String getParameterValue(String parameterName, ImageModule image)
			throws ValidationException {
		ModuleParameter instanceTypeParameter = image
				.getParameter(constructKey(parameterName));
		return instanceTypeParameter == null ? null : instanceTypeParameter
				.getValue();
	}

	protected String getKey(User user) {
		try {
			return getCredentials(user).getKey();
		} catch (InvalidElementException e) {
			return null;
		} catch (ConfigurationException e) {
			return null;
		}
	}

	protected String getSecret(User user) {
		try {
			return getCredentials(user).getSecret();
		} catch (InvalidElementException e) {
			return null;
		} catch (ConfigurationException e) {
			return null;
		}
	}

	protected String getLoginUsername(Run run) throws SlipStreamClientException {
		if (run.getType() == RunType.Orchestration) {
			return getOrchestratorImageLoginUsername();
		} else {
			return getMachineImageLoginUsername(run);
		}
	}

	private String getOrchestratorImageLoginUsername()
			throws ConfigurationException, ValidationException {
		return Configuration.getInstance().getRequiredProperty(
				constructKey("cloud.connector.orchestrator.ssh.username"));
	}

	private String getMachineImageLoginUsername(Run run)
			throws SlipStreamClientException {

		if (run.getType() == RunType.Machine) {
			return "\"\"";
		}

		ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
		String username = machine.getLoginUser();
		if (username == null) {
			throw (new SlipStreamClientException("Module " + machine.getName()
					+ " is missing login username"));
		}
		return username;
	}

	protected String getLoginPassword(Run run) throws ConfigurationException,
			SlipStreamClientException {
		if (run.getType() == RunType.Orchestration) {
			return getOrchestratorImageLoginPassword();
		} else {
			return getMachineImageLoginPassword(run);
		}
	}

	private String getOrchestratorImageLoginPassword()
			throws ConfigurationException, ValidationException {
		return Configuration.getInstance().getRequiredProperty(
				constructKey("cloud.connector.orchestrator.ssh.password"));
	}

	private String getMachineImageLoginPassword(Run run)
			throws SlipStreamClientException {

		if (run.getType() == RunType.Machine) {
			return "\"\"";
		}

		ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
		String password = machine.getParameterValue(
				constructKey(ImageModule.LOGINPASSWORD_KEY), null);
		if (password == null) {
			throw (new SlipStreamClientException("Module " + machine.getName()
					+ " is missing ssh login password"));
		}
		return password;
	}
}