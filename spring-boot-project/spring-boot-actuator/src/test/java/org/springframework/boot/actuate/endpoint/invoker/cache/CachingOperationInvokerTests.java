/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.invoker.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link CachingOperationInvoker}.
 *
 * @author Stephane Nicoll
 */
public class CachingOperationInvokerTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void createInstanceWithTtlSetToZero() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TimeToLive");
		new CachingOperationInvoker(mock(OperationInvoker.class), 0);
	}

	@Test
	public void cacheInTtlRangeWithNoParameter() {
		assertCacheIsUsed(Collections.emptyMap());
	}

	@Test
	public void cacheInTtlWithNullParameters() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("first", null);
		parameters.put("second", null);
		assertCacheIsUsed(parameters);
	}

	private void assertCacheIsUsed(Map<String, Object> parameters) {
		OperationInvoker target = mock(OperationInvoker.class);
		Object expected = new Object();
		given(target.invoke(parameters)).willReturn(expected);
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, 500L);
		Object response = invoker.invoke(parameters);
		assertThat(response).isSameAs(expected);
		verify(target, times(1)).invoke(parameters);
		Object cachedResponse = invoker.invoke(parameters);
		assertThat(cachedResponse).isSameAs(response);
		verifyNoMoreInteractions(target);
	}

	@Test
	public void targetAlwaysInvokedWithArguments() {
		OperationInvoker target = mock(OperationInvoker.class);
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("test", "value");
		parameters.put("something", null);
		given(target.invoke(parameters)).willReturn(new Object());
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, 500L);
		invoker.invoke(parameters);
		invoker.invoke(parameters);
		invoker.invoke(parameters);
		verify(target, times(3)).invoke(parameters);
	}

	@Test
	public void targetInvokedWhenCacheExpires() throws InterruptedException {
		OperationInvoker target = mock(OperationInvoker.class);
		Map<String, Object> parameters = new HashMap<>();
		given(target.invoke(parameters)).willReturn(new Object());
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, 50L);
		invoker.invoke(parameters);
		Thread.sleep(55);
		invoker.invoke(parameters);
		verify(target, times(2)).invoke(parameters);
	}

}
