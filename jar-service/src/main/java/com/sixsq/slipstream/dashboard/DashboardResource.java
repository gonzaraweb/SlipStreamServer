package com.sixsq.slipstream.dashboard;

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

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class DashboardResource extends BaseResource {

	@Get("xml")
	public Representation toXml() {

		String metadata = SerializationUtil.toXmlString(computeDashboard());
		return new StringRepresentation(metadata, MediaType.APPLICATION_XML);

	}

	@Get("html")
	public Representation toHtml() {

		String html = HtmlUtil.toHtml(computeDashboard(),
				getPageRepresentation(), getUser());
		
		return new StringRepresentation(html, MediaType.TEXT_HTML);
	}

	private Dashboard computeDashboard() {
	
		Dashboard dashboard = new Dashboard();
		try {
			dashboard.populate(getUser());
		} catch (SlipStreamClientException e) {
			throwClientConflicError(e.getMessage());
		} catch (SlipStreamException e) {
			throwClientConflicError(e.getMessage());
		}
	
		return dashboard;
	}

	@Override
	protected String getPageRepresentation() {
		return "dashboard";
	}

}
