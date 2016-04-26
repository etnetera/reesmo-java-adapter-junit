/* Copyright 2016 Etnetera a.s.
 *
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
 */
package cz.etnetera.reesmo.adapter.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.etnetera.reesmo.writer.Property;
import cz.etnetera.reesmo.writer.model.result.Result;
import cz.etnetera.reesmo.writer.model.result.TestStatus;
import cz.etnetera.reesmo.writer.model.result.TestType;
import cz.etnetera.reesmo.writer.storage.ExtendedFile;
import cz.etnetera.reesmo.writer.storage.Storage;
import cz.etnetera.reesmo.writer.storage.StorageException;

public class ReesmoJUnitAdapter extends TestWatcher {

	protected static Map<String, Suite> suitesMap = new ConcurrentHashMap<>();

	protected Result result = new Result();

	protected List<Object> attachments = new ArrayList<>();

	/**
	 * Adds a file as an attachment for test.
	 *  
	 * @param file
	 */
	public void addAttachment(File file) {
		attachments.add(file);
	}
	
	/**
	 * Adds a file with path as an attachment for test.
	 *  
	 * @param file
	 */
	public void addAttachment(ExtendedFile file) {
		attachments.add(file);
	}
	
	/**
	 * Returns instance of Result which will be stored
	 * in Reesmo. Feel free to add specific values,
	 * i.e. notes, labels, categories, types, links etc.
	 * 
	 * Only attachments need to be added separately using 
	 * {@link ReesmoJUnitAdapter#addAttachment(File)} like methods.
	 * 
	 * @return
	 */
	public Result getResult() {
		return result;
	}
	
	@Override
	protected void starting(Description description) {
		result.setName(getResultName(description));
		result.setStartedAt(new Date());
		result.setAutomated(true);
		Suite suite = getSuite(description);
		result.setSuite(suite.getName());
		result.setSuiteId(suite.getId());
		result.addType(TestType.JUNIT);
	}

	@Override
	protected void finished(Description description) {
		if (result.getProjectId() == null)
			result.setProjectId((String) Property.PROJECT_ID.get(getConfigurations(description)));
		if (result.getEndedAt() == null)
			result.setEndedAt(new Date());
		storeResult(description);
	}

	@Override
	protected void succeeded(Description description) {
		result.setStatus(TestStatus.PASSED);
	}

	@Override
	protected void failed(Throwable e, Description description) {
		result.setStatus(TestStatus.FAILED);
		result.addError(e);
	}

	@Override
	protected void skipped(AssumptionViolatedException e, Description description) {
		result.setStatus(TestStatus.SKIPPED);
		result.addError(e);
	}

	protected Suite getSuite(Description description) {
		String key = getSuiteKey(description);
		Suite suite = suitesMap.get(key);
		if (suite == null) {
			suite = new Suite(description);
			suitesMap.put(key, suite);
		}
		return suite;
	}

	protected String getResultName(Description description) {
		return getResultName(description.getTestClass(), description.getMethodName());
	}

	protected String getResultName(Class<?> testClass, String methodName) {
		return testClass.getSimpleName() + "." + methodName;
	}

	protected String getSuiteKey(Description description) {
		return description.getClassName();
	}

	protected List<Object> getConfigurations(Description description) {
		List<Object> sources = new ArrayList<>();
		sources.add(description.getTestClass());
		description.getAnnotations().forEach(a -> sources.add(a));
		return sources;
	}

	protected void storeResult(Description description) {
		try {
			getStorage(description).addResult(getConfigurations(description), result, attachments);
		} catch (StorageException e) {
			getLogger().error("Error when storing result", e);
		}
	}

	protected Storage getStorage(Description description) throws StorageException {
		return Storage.newInstance(getConfigurations(description));
	}

	protected Logger getLogger() {
		return LoggerFactory.getLogger(getClass());
	}

	protected class Suite {

		protected String name;

		protected String id;

		public Suite(Description description) {
			name = description.getTestClass().getSimpleName();
			id = String.valueOf(new Date().getTime());
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

	}

}
