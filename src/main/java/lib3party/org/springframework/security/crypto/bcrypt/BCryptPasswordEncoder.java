/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package lib3party.org.springframework.security.crypto.bcrypt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of PasswordEncoder that uses the BCrypt strong hashing function. Clients
 * can optionally supply a "version" ($2a, $2b, $2y) and a "strength" (a.k.a. log rounds in BCrypt)
 * and a SecureRandom instance. The larger the strength parameter the more work will have to be done
 * (exponentially) to hash the passwords. The default value is 10.
 *
 * @author Dave Syer
 */
public class BCryptPasswordEncoder implements PasswordEncoder {
	private Pattern BCRYPT_PATTERN = Pattern
			.compile("\\A\\$2(a|y|b)?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}");
	private final Log logger = LogFactory.getLog(getClass());

	private final int strength;
	private final BCryptVersion version;

	private final SecureRandom random;


	public BCryptPasswordEncoder() {
		this(-1);
	}

	/**
	 * @param strength the log rounds to use, between 4 and 31
	 */
	public BCryptPasswordEncoder(int strength) {
		this(strength, null);
	}

	/**
	 * @param version the version of bcrypt, can be 2a,2b,2y
	 */
	public BCryptPasswordEncoder(BCryptVersion version) {
		this(version, null);
	}

	/**
	 * @param version the version of bcrypt, can be 2a,2b,2y
	 * @param random  the secure random instance to use
	 */
	public BCryptPasswordEncoder(BCryptVersion version, SecureRandom random) {
		this(version, -1, random);
	}

	/**
	 * @param strength the log rounds to use, between 4 and 31
	 * @param random   the secure random instance to use
	 */
	public BCryptPasswordEncoder(int strength, SecureRandom random) {
		this(BCryptVersion.$2A, strength, random);
	}

	/**
	 * @param version  the version of bcrypt, can be 2a,2b,2y
	 * @param strength the log rounds to use, between 4 and 31
	 */
	public BCryptPasswordEncoder(BCryptVersion version, int strength) {
		this(version, strength, null);
	}

	/**
	 * @param version  the version of bcrypt, can be 2a,2b,2y
	 * @param strength the log rounds to use, between 4 and 31
	 * @param random   the secure random instance to use
	 */
	public BCryptPasswordEncoder(BCryptVersion version, int strength, SecureRandom random) {
		if (strength != -1 && (strength < BCrypt.MIN_LOG_ROUNDS || strength > BCrypt.MAX_LOG_ROUNDS)) {
			throw new IllegalArgumentException("Bad strength");
		}
		this.version = version;
		this.strength = strength == -1 ? 10 : strength;
		this.random = random;
	}

	public String encode(CharSequence rawPassword) {
		String salt;
		if (random != null) {
			salt = BCrypt.gensalt(version.getVersion(), strength, random);
		} else {
			salt = BCrypt.gensalt(version.getVersion(), strength);
		}
		return BCrypt.hashpw(rawPassword.toString(), salt);
	}

	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if (encodedPassword == null || encodedPassword.length() == 0) {
			logger.warn("Empty encoded password");
			return false;
		}

		if (!BCRYPT_PATTERN.matcher(encodedPassword).matches()) {
			logger.warn("Encoded password does not look like BCrypt");
			return false;
		}

		return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
	}

	@Override
	public boolean upgradeEncoding(String encodedPassword) {
		if (encodedPassword == null || encodedPassword.length() == 0) {
			logger.warn("Empty encoded password");
			return false;
		}

		Matcher matcher = BCRYPT_PATTERN.matcher(encodedPassword);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Encoded password does not look like BCrypt: " + encodedPassword);
		}
		else {
			int strength = Integer.parseInt(matcher.group(2));
			return strength < this.strength;
		}
	}

	/**
	 * Stores the default bcrypt version for use in configuration.
	 *
	 * @author Lin Feng
	 */
	public enum BCryptVersion {
		$2A("$2a"),
		$2Y("$2y"),
		$2B("$2b");

		private final String version;

		BCryptVersion(String version) {
			this.version = version;
		}

		public String getVersion() {
			return this.version;
		}
	}
}
