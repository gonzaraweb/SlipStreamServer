package com.sixsq.slipstream.action;

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

import java.io.IOException;
import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.persistence.OneShotAction;
import com.sixsq.slipstream.resource.SimpleRepresentationBaseResource;

public class ActionResource extends SimpleRepresentationBaseResource {

	private String uuid;

	private OneShotAction action;

	@Override
	protected void doInit() throws ResourceException {
		super.doInit();

		Map<String, Object> attributes = getRequest().getAttributes();

		uuid = attributes.get("uuid").toString();

		action = OneShotAction.load(uuid);

		if (action == null) {
			Util.throwNotFoundResource();
		}

	}

	@Get("txt")
	public Representation toText() {
		try {
			return action.doAction(getRequest());
		} catch (SlipStreamRuntimeException e) {
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
	}

	@Get("html")
	public Representation toHtml() {

		try {
			setMessage(toText().getText());
		} catch (IOException e) {
			handleError(e);
		}
		
		return new StringRepresentation(generateHtml(), MediaType.TEXT_HTML);
	}

	@Override
	protected String getPageRepresentation() {
		return "action";
	}
}