package com.sixsq.slipstream.statemachine;

import com.sixsq.slipstream.persistence.RunType;

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


public class SendingFinalReportState extends SynchronizedState {

	public SendingFinalReportState(ExtrinsicState extrinsicState) {
		super(extrinsicState);
		if( extrinsicState.getRun().getType() == RunType.Run){
			nextState = States.Detached;
		}else{
			nextState = States.Finalizing;
		}
	}
	
	@Override
	public States getState() {
		return States.SendingFinalReport;
	}
	
	@Override
	public boolean mustSynchronizeOnFailure() {
		return true;
	}

}
