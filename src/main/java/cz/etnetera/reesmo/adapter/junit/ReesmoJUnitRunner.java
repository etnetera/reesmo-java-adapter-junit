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

import java.util.List;

import org.junit.Before;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class ReesmoJUnitRunner extends BlockJUnit4ClassRunner {
	
	protected ReesmoJUnitExecutionListener listener;
	
	public ReesmoJUnitRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}

	@Override
	public synchronized void run(RunNotifier notifier) {
		listener = new ReesmoJUnitExecutionListener();
		notifier.addListener(listener);
		super.run(notifier);
	}
	
	@Override
	protected Statement withBefores(FrameworkMethod method, Object target,
            Statement statement) {
        List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(
                Before.class);
        return befores.isEmpty() ? statement : new RunBefores(method, statement,
                befores, target);
    }
	
	public class RunBefores extends Statement {
		
		protected final FrameworkMethod method;
		
	    protected final Statement next;

	    protected final Object target;

	    protected final List<FrameworkMethod> befores;

	    public RunBefores(FrameworkMethod method, Statement next, List<FrameworkMethod> befores, Object target) {
	    	this.method = method;
	        this.next = next;
	        this.befores = befores;
	        this.target = target;
	    }

	    @Override
	    public void evaluate() throws Throwable {
	        for (FrameworkMethod before : befores) {
	            before.invokeExplosively(target);
	        }
	        if (target instanceof ReesmoJUnitTest) {
	        	listener.initTest((ReesmoJUnitTest) target, method);
	        }
	        next.evaluate();
	    }
	}

}
