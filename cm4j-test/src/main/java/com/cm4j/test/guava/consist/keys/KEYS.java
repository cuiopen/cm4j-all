package com.cm4j.test.guava.consist.keys;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/**
 * 缓存key
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:30:27
 * 
 */
public abstract class KEYS {

	public abstract String key();

	public static String[] getKeyInfo(String key) {
		Preconditions.checkArgument(StringUtils.isNotBlank(key), "key is illegal");
		return StringUtils.splitPreserveAllTokens(key, "_");
	}

	public static final class JOINER extends KEYS {
		private String key;

		public JOINER(String prefix, Object... args) {
			Preconditions.checkArgument(StringUtils.isNotBlank(prefix), "key's prefix is illegal");
			Object[] all = ArrayUtils.addAll(new Object[] { prefix }, args);
			this.key = Joiner.on("_").join(all);
		}

		@Override
		public String key() {
			return this.key;
		}
	}
}
