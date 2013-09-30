package com.cm4j.test.tcp_ip.node_chain;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicNodePipelineTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void testAddLast() throws InterruptedException {
		logger.debug("DynamicNodePipeline.testAddLast()开始");
		DynamicNodePipeline pipeline = new DynamicNodePipeline();

		for (int i = 0; i < 10000000; i++) {
			NodeHandler handler = new NodeHandler() {
			};
			pipeline.addLast(handler);
			// DynamicNodePipeline.Node node = pipeline.getNode(handler);

		}
		logger.debug("DynamicNodePipeline.testAddLast()结束...");
	}

	@Test
	public void testGetNode() {
		DynamicNodePipeline pipeline = new DynamicNodePipeline();
		NodeHandler handler = new NodeHandler() {
		};
		pipeline.addLast(handler);

		Assert.assertNotNull(pipeline.getNode(handler));
	}
}
