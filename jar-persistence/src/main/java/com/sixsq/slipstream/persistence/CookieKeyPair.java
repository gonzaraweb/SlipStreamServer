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


import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
@SuppressWarnings("serial")
public class CookieKeyPair implements Serializable {

	private static Long ID = 1L;
	
	@Id
	Long id = ID;

	@Lob
	private String privateKey = null;
	@Lob
	private String publicKey = null;

	@SuppressWarnings("unused")
	private CookieKeyPair() {
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public CookieKeyPair(String privateKey, String publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}
	
	public static CookieKeyPair load() {
		EntityManager em = PersistenceUtil.createEntityManager();
		return em.find(CookieKeyPair.class, ID);
	}

	public CookieKeyPair store() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		CookieKeyPair obj = em.merge(this);
		transaction.commit();
		em.close();
		return obj;
	}

}
