package com.woniu.network.bootstrap.client;

import org.junit.Test;

import com.woniu.network.protocol.ConstructedMessage;
import com.woniu.network.protocol.factory.ProtocolFactory;

public class LongConnectClientTest {

	@Test
	public void send() {

		Client client = new LongConnectClient("127.0.0.1:2012");
		client.connect();

		for (int i = 0; i < 100000; i++) {
//			param.setAccountId(100000 + i);
//			ConstructedMessage protocol = ((Object) ProtocolFactory.Holder.instance).createProtocol();
//
//			client.sendProtocol(protocol);
		}

		try {
			// 看是不是junit关闭了客户端
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
