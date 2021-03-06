package com.sixsq.slipstream.module;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.restlet.data.Form;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.Commit;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.user.FormProcessor;

public abstract class ModuleFormProcessor extends
		FormProcessor<Module, ModuleParameter> {

	private List<String> illegalNames = new ArrayList<String>(
			(Arrays.asList(("new"))));

	public ModuleFormProcessor(User user) {
		super(user);
	}

	static public ModuleFormProcessor createFormProcessorInstance(
			ModuleCategory category, User user) {

		ModuleFormProcessor processor = null;

		switch (category) {
		case Project:
			processor = new ProjectFormProcessor(user);
			break;
		case Image:
			processor = new ImageFormProcessor(user);
			break;
		case BlockStore:
			break;
		case Deployment:
			processor = new DeploymentFormProcessor(user);
			break;
		default:
			String msg = "Unknown category: " + category.toString();
			throw new IllegalArgumentException(msg);
		}

		return processor;

	}

	@Override
	protected void parseForm() throws ValidationException, NotFoundException {
		super.parseForm();

		String name = parseName();
		setParametrized(getOrCreateParameterized(name));
		getParametrized().setDescription(parseDescription());
		getParametrized().setCommit(parseCommit());
	}

	private String parseName() throws ValidationException {
		String parent = getForm().getFirstValue("parentmodulename", "");
		String name = getForm().getFirstValue("name");

		validateName(name);

		return ("".equals(parent)) ? name : parent + "/" + name;
	}

	private String parseDescription() throws ValidationException {
		return getForm().getFirstValue("description");
	}

	private Commit parseCommit() throws ValidationException {
		return new Commit(getUser().getName(), getForm().getFirstValue("comment"));
	}

	private void validateName(String name) throws ValidationException {
		for (String illegal : illegalNames) {
			if (illegal.equals(name)) {
				throw (new ValidationException("Illegal name: " + name));
			}
		}
		return;
	}

	protected void parseAuthz() {

		// Save authz section
		Module module = getParametrized();
		Authz authz = new Authz(getUser().getName(), module);
		authz.clear();

		Form form = getForm();

		// ownerGet: can't be changed because owner would lose access
		authz.setOwnerPost(getBooleanValue(form, "ownerPost"));
		authz.setOwnerDelete(getBooleanValue(form, "ownerDelete"));

		authz.setGroupGet(getBooleanValue(form, "groupGet"));
		authz.setGroupPut(getBooleanValue(form, "groupPut"));
		authz.setGroupPost(getBooleanValue(form, "groupPost"));
		authz.setGroupDelete(getBooleanValue(form, "groupDelete"));

		authz.setPublicGet(getBooleanValue(form, "publicGet"));
		authz.setPublicPut(getBooleanValue(form, "publicPut"));
		authz.setPublicPost(getBooleanValue(form, "publicPost"));
		authz.setPublicDelete(getBooleanValue(form, "publicDelete"));

		authz.setGroupMembers(form.getFirstValue("groupmembers", ""));
		authz.setInheritedGroupMembers(getBooleanValue(form,
				"inheritedGroupMembers"));

		if (module.getCategory() == ModuleCategory.Project) {
			authz.setOwnerCreateChildren(getBooleanValue(form,
					"ownerCreateChildren"));
			authz.setGroupCreateChildren(getBooleanValue(form,
					"groupCreateChildren"));
			authz.setPublicCreateChildren(getBooleanValue(form,
					"publicCreateChildren"));
		}

		getParametrized().setAuthz(authz);

	}

	protected boolean getBooleanValue(Form form, String parameter) {

		Object value = form.getFirstValue(parameter);
		if (value != null && "on".equals(value.toString())) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected ModuleParameter createParameter(String name, String value,
			String description) throws SlipStreamClientException {
		return new ModuleParameter(name, value, description);
	}

	public void adjustModule(Module older) throws ValidationException {
		getParametrized().setCreation(older.getCreation());
		getParametrized().getAuthz().setUser(older.getOwner());
	}
}
