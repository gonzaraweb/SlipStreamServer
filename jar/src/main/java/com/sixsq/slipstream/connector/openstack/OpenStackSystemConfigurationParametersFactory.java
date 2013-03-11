package com.sixsq.slipstream.connector.openstack;

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

import com.sixsq.slipstream.connector.SystemConfigurationParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;

public class OpenStackSystemConfigurationParametersFactory extends
		SystemConfigurationParametersFactoryBase {

	public OpenStackSystemConfigurationParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		super.initReferenceParameters();

		putMandatoryParameter(constructKey("orchestrator.instance.type"),
				"OpenStack Flavor for the orchestrator. " +
				"The actual image should support the desired Flavor.");

		putMandatoryParameter(constructKey("orchestrator.imageid"),
				"Image id to use for orchestrator");

		putMandatoryParameter(constructKey("service.type"), 
				"Type-name of the service who provide the instances functionality. ");
		
		putMandatoryParameter(constructKey("service.name"), 
				"Name of the service who provide the instances functionality" +
				"('nova' for OpenStack essex and 'Compute' for HP Cloud)");
		
		putMandatoryParameter(constructKey("service.region"), 
				"Region used by this cloud connector");

	}

}