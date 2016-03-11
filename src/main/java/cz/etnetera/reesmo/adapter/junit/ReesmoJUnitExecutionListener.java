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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.etnetera.reesmo.writer.Property;
import cz.etnetera.reesmo.writer.model.result.Result;
import cz.etnetera.reesmo.writer.model.result.TestStatus;
import cz.etnetera.reesmo.writer.model.result.TestType;
import cz.etnetera.reesmo.writer.storage.Storage;
import cz.etnetera.reesmo.writer.storage.StorageException;

public class ReesmoJUnitExecutionListener extends RunListener {

	protected Map<String, Suite> suitesMap = new ConcurrentHashMap<>();

	protected Map<String, Result> resultsMap = new ConcurrentHashMap<>();

	protected Map<String, List<Object>> attachmentsMap = new ConcurrentHashMap<>();

	public void initTest(ReesmoJUnitTest testClassInstance, FrameworkMethod testMethod) {
		String resultKey = getResultKey(testClassInstance.getClass(), testMethod.getName());
		testClassInstance.registerReesmoBridge(new ReesmoJUnitBridge(resultKey, this));
	}
	
	@Override
	public void testStarted(Description description) throws Exception {
		Result result = getResult(description);
		result.setName(getResultName(description));
		result.setStartedAt(new Date());
		result.setAutomated(true);
		Suite suite = getSuite(description);
		result.setSuite(suite.getName());
		result.setSuiteId(suite.getId());
	}

	@Override
	public void testFinished(Description description) throws Exception {
		Result result = getResult(description);
		result.setProjectId((String) Property.PROJECT_ID.get(getConfigurations(description)));
		result.setEndedAt(new Date());
		// do not override status, it can be set in error method already
		if (result.getStatus() == null) {
			result.setStatus(TestStatus.PASSED);
		}
		result.addType(TestType.JUNIT);
		storeResult(description, result);
	}

	/**
	 * This one is called before
	 * {@link ReesmoJUnitExecutionListener#testFinished(Description)}.
	 */
	@Override
	public void testFailure(Failure failure) throws Exception {
		if (failure.getDescription().isTest()) {
			Result result = getResult(failure.getDescription());
			result.setStatus(TestStatus.FAILED);
			result.addError(failure.getException());
		} else {
			// TODO fire fake test case
		}
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		try {
			testFailure(failure);
		} catch (Exception e) {
			getLogger().error("Error storing test assumption failure", e);
		}
	}

	/**
	 * This one is called standalone. No call to
	 * {@link ReesmoJUnitExecutionListener#testStarted(Description)} or
	 * {@link ReesmoJUnitExecutionListener#testFinished(Description)}
	 * is done.
	 */
	@Override
	public void testIgnored(Description description) throws Exception {
		Result result = getResult(description);
		result.setName(getResultName(description));
		result.setStartedAt(new Date());
		result.setEndedAt(result.getEndedAt());
		result.setAutomated(true);
		Suite suite = getSuite(description);
		result.setSuite(suite.getName());
		result.setSuiteId(suite.getId());
		result.setStatus(TestStatus.SKIPPED);
		storeResult(description, result);
	}

	protected Result getResult(Description description) {
		return getResult(getResultKey(description));
	}
	
	protected Result getResult(String resultKey) {
		Result result = resultsMap.get(resultKey);
		if (result == null) {
			result = new Result();
			resultsMap.put(resultKey, result);
		}
		return result;
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

	protected List<Object> getAttachments(String resultKey) {
		List<Object> attachments = attachmentsMap.get(resultKey);
		if (attachments == null) {
			attachments = new ArrayList<Object>();
			attachmentsMap.put(resultKey, attachments);
		}
		return attachments;
	}

	protected String getResultKey(Description description) {
		return getResultKey(description.getTestClass(), description.getMethodName());
	}

	protected String getResultKey(Class<?> testClass, String methodName) {
		return testClass.getName() + "." + methodName;
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

	protected void storeResult(Description description, Result result) {
		try {
			getStorage(description).addResult(getConfigurations(description), result,
					getAttachments(getResultKey(description)));
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
