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

import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.ParametersFactory;
import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Metadata;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.resource.ParameterizedResource;
import com.sixsq.slipstream.run.RunFactory;
import com.sixsq.slipstream.util.ModuleUriUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.XmlUtil;

/**
 * Unit test see
 * 
 * @see ModuleResourceTest
 * 
 */
public class ModuleResource extends ParameterizedResource<Module> {

	private ModuleCategory category = null;
	public static final String COPY_SOURCE_FORM_PARAMETER_NAME = "source_uri";
	public static final String COPY_TARGET_FORM_PARAMETER_NAME = "target_name";

	@Get("xml")
	public Representation toXml() {
		checkCanGet();

		String result = XmlUtil.normalize(getParameterized());
		return new StringRepresentation(result);
	}

	@Post("form")
	public void copyTo(Representation entity) throws ValidationException {
		Form form = new Form(entity);

		if (!isExisting()) {
			throwClientForbiddenError("Target project doesn't exist: "
					+ getParameterized().getName());
		}

		String sourceUri = form.getFirstValue(COPY_SOURCE_FORM_PARAMETER_NAME);
		if (sourceUri == null) {
			throwClientBadRequest("Missing source uri form parameter");
		}

		String targetName = form.getFirstValue(COPY_TARGET_FORM_PARAMETER_NAME);
		if (targetName == null) {
			throwClientBadRequest("Missing target name form parameter");
		}

		Module source = Module.load(sourceUri);
		if (source == null) {
			throwClientBadRequest("Unknown source module: " + sourceUri);
		}

		if (!source.getAuthz().canGet(getUser())) {
			throwClientForbiddenError("You do not have read rights on the source module: "
					+ source.getName());
		}

		String targetFullName = getParameterized().getName() + "/" + targetName;
		String targetUri = Module.constructResourceUri(targetFullName);

		Module target = Module.load(targetUri);
		if (target != null) {
			throwClientForbiddenError("Target module already exists: "
					+ targetUri);
		}

		if (!getParameterized().getAuthz().canCreateChildren(getUser())) {
			throwClientForbiddenError("You do not have rights to create modules in this project");
		}

		target = source.copy();
		target.getAuthz().setUser(getUser().getName());
		target.getAuthz().clear();
		target.setName(targetFullName);
		target.store();

		getResponse().redirectSeeOther("/" + target.getResourceUri());
	}

	@Delete
	public void deleteModule() {

		super.deleteResource();
		Module latest = null;
		latest = Module.loadLatest(getParameterized().getResourceUri());
		try {
			if (latest == null) {
				redirectToParent();
			} else {
				redirectToLatest(latest);
			}
		} catch (ValidationException e) {
			throwClientConflicError(e.getMessage());
		}
	}

	private void redirectToParent() throws ValidationException {
		String resourceUri = getParameterized().getResourceUri();
		String parentResourceUri = ModuleUriUtil
				.extractParentUriFromResourceUri(resourceUri);
		String location = getRequest().getRootRef().toString() + "/"
				+ parentResourceUri;
		getResponse().setLocationRef(location);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	private void redirectToLatest(Module latest) {
		String location = getRequest().getRootRef().toString() + "/"
				+ latest.getResourceUri();
		getResponse().setLocationRef(location);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Put("form")
	public void updateOrCreateFromForm(Representation entity)
			throws ResourceException {

		Module module = null;
		try {
			module = (Module) processEntityAsForm(entity);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		updateOrCreate(module);
		
		setResponseOkAndViewLocation(module.getResourceUri());
	}

	@Put("multipart")
	public void updateOrCreateFromXml(Representation entity)
			throws ResourceException {

		Module module = xmlToModule();

		updateOrCreate(module);

		setResponseRedirect("/" + module.getResourceUri());
	}

	private void updateOrCreate(Module module) {

		checkCanPut();

		try {
			module.validate();
		} catch (ValidationException ex) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, ex);
		}

		String moduleUri = null;
		String targetUri = null;
		try {
			moduleUri = ModuleUriUtil.extractVersionLessResourceUri(module
					.getResourceUri());
			targetUri = ModuleUriUtil
					.extractVersionLessResourceUri(targetParameterizeUri);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		try {
			checkConsistentModule(moduleUri, targetUri);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		module.store();

	}

	private void checkConsistentModule(String moduleUri, String targetUri) throws ValidationException {
		// Only check that the form contains the module uri corresponding
		// to the target uri if the module is not new. In the case the module
		// is new, the name is defined by the put request and the target
		// is always 'new'
		if(!isNew()) {
			if (!targetUri.equals(moduleUri)) {
				throwClientBadRequest("The uploaded module does not correspond to the target module uri");
			}
		}
		
		// Check that the new proposed module doesn't already exists.
		// We need to do this here since the standard AA process runs before
		// the module name is extracted from the request.
		if(isNew()) {
			if(loadModule(moduleUri) != null) {
				throwClientForbiddenError("Cannot create this resource. Does it already exist?");
			}
		}
	}

	private Module xmlToModule() {
		Module module = null;
		try {
			module = (Module) SerializationUtil.fromXml(extractXml(),
					ImageModule.class);
		} catch (SlipStreamClientException e) {
			e.printStackTrace();
			throwClientBadRequest("Invalid xml module: " + e.getMessage());
		}
		return module;
	}

	private String extractXml() {

		RestletFileUpload upload = new RestletFileUpload(
				new DiskFileItemFactory());

		List<FileItem> items;

		Request request = getRequest();
		try {
			items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getMessage());
		}

		String module = null;
		for (FileItem fi : items) {
			if (fi.getName() != null) {
				module = getContent(fi);
			}
		}
		if (module == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"the file is empty");
		}

		return XmlUtil.denormalize(module);
	}

	protected String getContent(FileItem fi) {
		return fi.getString();
	}

	@Override
	protected Module loadParameterized(String targetParameterizedUri)
			throws ValidationException {

		Module module = loadModule(targetParameterizedUri);

		resolveImageIdIfAppropriate(module);

		return module;

	}

	private void resolveImageIdIfAppropriate(Module module)
			throws ConfigurationException, ValidationException {
		RunFactory.resolveImageIdIfAppropriate(module, getUser());
	}

	private Module loadModule(String targetParameterizedUri)
			throws ValidationException {
		Module module = Module.load(targetParameterizedUri);
		if (module != null) {
			if (module.getCategory() == ModuleCategory.Project) {
				((ProjectModule) module).setChildren(Module.viewList(module
						.getResourceUri()));
			}
		}
		return module;
	}

	@Override
	protected void setIsEdit() {
		super.setIsEdit();

		if (isExisting()) {
			// Add connector information for the transformation
			List<String> serviceCloudNames = ConnectorFactory
					.getCloudServiceNamesList();
			serviceCloudNames.add(CloudImageIdentifier.DEFAULT_CLOUD_SERVICE);
			getParameterized().setCloudNames(
					serviceCloudNames.toArray(new String[0]));
		}
	}

	private Metadata processEntityAsForm(Representation entity)
			throws ValidationException {

		// Add the default module parameters if the module is not new,
		// to ensure that all mandatory parameters are present.
		// This is required to avoid inconsistent modules, for example
		// when connectors are added in the configuration
		Module module = getParameterized();
		if (module != null) {
			try {
				ParametersFactory.addParametersForEditing(module);
			} catch (ValidationException e) {
				throwClientConflicError(e.getMessage());
			} catch (ConfigurationException e) {
				throwServerError(e.getMessage());
			}
		}

		Form form = extractFormFromEntity(entity);
		ModuleFormProcessor processor = ModuleFormProcessor
				.createFormProcessorInstance(getCategory(form), getUser());

		try {
			processor.processForm(form);
		} catch (BadlyFormedElementException e) {
			throwClientError(e);
		} catch (SlipStreamClientException e) {
			throwClientError(e);
		}

		module = processor.getParametrized();

		category = module.getCategory();

		module = resetMandatoryParameters(module);

		return module;
	}

	private Module resetMandatoryParameters(Module module)
			throws ValidationException {
		for (ModuleParameter referenceParameter : createParameterized(
				"reference").getParameterList()) {
			ModuleParameter p = module.getParameter(referenceParameter
					.getName());
			if (p == null) {
				throw (new ValidationException("Missing mandatory parameter: "
						+ referenceParameter.getName()));
			}
			p.setCategory(referenceParameter.getCategory());
			p.setDescription(referenceParameter.getDescription());
			p.setEnumValues(referenceParameter.getEnumValues());
			p.setInstructions(referenceParameter.getInstructions());
			p.setMandatory(referenceParameter.isMandatory());
			p.setReadonly(referenceParameter.isReadonly());
			p.setType(referenceParameter.getType());
		}
		return module;
	}

	@Override
	protected String extractTargetUriFromRequest() {

		String module = (String) getRequest().getAttributes().get("module");

		int version = extractVersion();
		String moduleName = (version == Module.DEFAULT_VERSION ? module
				: module + "/" + version);
		return Module.constructResourceUri(moduleName);
	}

	private int extractVersion() {
		String v = (String) getRequest().getAttributes().get("version");
		return (v == null) ? -1 : Integer.parseInt(v);
	}

	@Override
	protected void authorize() {

		setCanPut(authorizePut());

		if (isExisting()) {
			setCanDelete(authorizeDelete());
		}

		if (isExisting()) {
			setCanGet(authorizeGet());
		}

	}

	private boolean authorizeGet() {
		if (getUser().isSuper() || isNew()) {
			return true;
		}
		return getParameterized().getAuthz().canGet(getUser());
	}

	private boolean authorizeDelete() {
		if (getUser().isSuper()) {
			return true;
		}
		return getParameterized().getAuthz().canDelete(getUser());
	}

	protected boolean authorizePut() {

		if (getUser().isSuper()) {
			return true;
		}
		if (isNew()) {
			// check parent
			String parentResourceUri = null;
			try {
				parentResourceUri = ModuleUriUtil
						.extractParentUriFromResourceUri(targetParameterizeUri);
			} catch (ValidationException e) {
				return false;
			}
			Module parent = Module.load(parentResourceUri);
			if (parent == null) {
				// this is the root module. All can put on it (for now)
				return true;
			} else {
				return parent.getAuthz().canCreateChildren(getUser());
			}
		}
		boolean isExisting = isExisting(); // also true for isNew

		return isExisting ? Module
				.loadLatest(getParameterized().getResourceUri()).getAuthz()
				.canPut(getUser()) : true;
	}

	@Override
	protected Module createParameterized(String name)
			throws ValidationException {
		Module module = null;
		switch (getCategory()) {
		case Project:
			module = new ProjectModule(name);
			break;
		case Image:
			module = new ImageModule(name);
			break;
		case Deployment:
			module = new DeploymentModule(name);
			break;
		default:
			throwClientError("Unknown category");
		}
		module.setAuthz(new Authz(getUser().getName(), module));
		ParametersFactory.addParametersForEditing(module);
		return module;
	}

	private ModuleCategory getCategory(Form form) {
		return getCategory(form.getFirstValue("category"));
	}

	private ModuleCategory getCategory() {
		if (category == null) {
			String c = (String) getRequest().getAttributes().get("category");
			category = (c == null) ? ModuleCategory.Project : getCategory(c);
		}
		return category;
	}

	private ModuleCategory getCategory(String category) {
		ModuleCategory c = null;
		try {
			c = ModuleCategory.valueOf(category);
		} catch (NullPointerException e) {
			throwClientError("Missing category attribute");
		} catch (IllegalArgumentException e) {
			throwClientError("Invalid category attribute, got: " + category);
		}
		return c;
	}

	@Override
	protected String getViewStylesheet() {
		return "module.xsl";
	}

	@Override
	protected String getEditStylesheet() {
		return "module-editor.xsl";
	}

	@Override
	protected ModuleCategory getChooser() {
		String c = (String) getRequest().getAttributes().get("choosertype");
		return (c == null) ? null : getCategory(c);
	}

	@Override
	protected void addParametersForEditing() throws ValidationException,
			ConfigurationException {

		ParametersFactory.addParametersForEditing(getParameterized());
	}

}