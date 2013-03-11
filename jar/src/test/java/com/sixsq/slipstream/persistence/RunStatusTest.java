package com.sixsq.slipstream.persistence;

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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sixsq.slipstream.statemachine.States;

public class RunStatusTest {

	@Test
	public void verifyRunningSuccess() {

		assertEquals(new RunStatus(States.Running, false).toString(), States.Running.toString());
	}

	@Test
	public void verifyRunningWhileAborting() {

		assertEquals(new RunStatus(States.Running, true).toString(), RunStatus.FAILING);
	}

	@Test
	public void verifyFinalStateWithSuccess() {

		assertEquals(new RunStatus(States.Terminal, false).toString(), RunStatus.SUCCESS);
	}

	@Test
	public void verifyFinalStateAndAborted() {

		assertEquals(new RunStatus(States.Terminal, true).toString(), RunStatus.FAILED);
	}

}