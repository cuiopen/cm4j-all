package com.cm4j.taobao.api.googds.postages;

import java.util.List;

import org.junit.Test;

import com.cm4j.taobao.TestContext;
import com.cm4j.taobao.api.common.APICaller;
import com.taobao.api.ApiException;
import com.taobao.api.domain.Postage;

public class PostagesServiceTest extends TestContext {

	private PostagesAPI postagesService = new PostagesAPI();

	@Test
	public void getTest() throws ApiException {
		List<Postage> result = postagesService.get(null, TAOBAO_SESSION_KEY);
		logger.debug("taobao.postages.get:{}", APICaller.jsonBinder.toJson(result));
	}
}