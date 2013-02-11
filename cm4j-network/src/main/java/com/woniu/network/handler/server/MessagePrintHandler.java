package com.woniu.network.handler.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 直接按照协议规则打印协议内容
 * 
 * @author yang.hao
 * @since 2011-4-29 上午10:13:09
 * 
 */
public class MessagePrintHandler extends SimpleChannelHandler {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		ChannelBuffer buffer = (ChannelBuffer) e.getMessage();

		logger.debug("server Buffer content:{}", buffer.array());

		logger.debug("收到消息总长度：{}", buffer.readableBytes());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("参数：{}\n", buffer.readInt());

		logger.debug("类型：{}", buffer.readByte());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("类型：{}", buffer.readByte());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("类型：{}", buffer.readByte());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("类型：{}", buffer.readByte());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("类型：{}", buffer.readByte());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("类型：{}", buffer.readByte());
		logger.debug("参数：{}", buffer.readInt());
		logger.debug("类型：{}", buffer.readByte());
		String str = buffer.toString(CharsetUtil.UTF_8);
		if ('\u0000' != str.charAt(str.length() - 1)) {
			logger.error("结果不一致");
		}
		logger.debug("参数：{}", str.substring(0, str.length() - 1));
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		logger.debug("server channel closed:{}", ctx.getChannel());
	}
}
