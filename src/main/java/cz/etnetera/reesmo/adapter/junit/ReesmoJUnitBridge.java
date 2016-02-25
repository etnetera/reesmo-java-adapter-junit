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

import cz.etnetera.reesmo.writer.storage.FileWithPath;

/**
 * Bridge which allows to add attachments for test
 * and store them into Reesmo.
 */
public class ReesmoJUnitBridge {

	protected String resultKey;
	
	protected ReesmoJUnitExecutionListener listener;

	public ReesmoJUnitBridge(String resultKey, ReesmoJUnitExecutionListener listener) {
		this.resultKey = resultKey;
		this.listener = listener;
	}

	/**
	 * Adds a file as an attachment for test.
	 *  
	 * @param file
	 */
	public void addAttachment(File file) {
		listener.getAttachments(resultKey).add(file);
	}
	
	/**
	 * Adds a file with path as an attachment for test.
	 *  
	 * @param file
	 */
	public void addAttachment(FileWithPath file) {
		listener.getAttachments(resultKey).add(file);
	}
	
}
