package com.woniu.network.handler.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.woniu.network.bootstrap.client.AbstractClient;
import com.woniu.network.protocol.IProtocol;

/**
 * 在channel连接上时启动线程池从 {@link BlockingQueue}
 * 
 * @author yang.hao
 * @since 2011-11-18 下午3:43:57
 */
public class ChannelWriteHandler extends SimpleChannelUpstreamHandler {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private BlockingQueue<IProtocol> sendQueue;
	private boolean closeAfterWrite;
	private boolean shutdown = false;
	private final ThreadGroup threadGroup = new ThreadGroup("channelwriteGroup");
	private final AtomicInteger counter = new AtomicInteger(); 

	public ChannelWriteHandler(BlockingQueue<IProtocol> sendQueue, boolean closeAfterWrite) {
		this.sendQueue = sendQueue;
		this.closeAfterWrite = closeAfterWrite;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		startWriteChannel(ctx.getChannel());
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		// 关闭当前允许线程
		stopWriteChannel();
		super.channelClosed(ctx, e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		logger.error("channelWriteHandler exception", e.getCause());
//		Channels.close(ctx.getChannel());
	}

	public void startWriteChannel(Channel channel) {
		Thread t = new Thread(threadGroup, new R(channel), "Channel write handler #" + counter.incrementAndGet());
		t.start();
	}

	public void stopWriteChannel() {
		this.shutdown = true;
		try {
			if (threadGroup.activeCount() > 0) {
				logger.debug("destroy thread group,size:{}", threadGroup.activeCount());
				threadGroup.interrupt();
			}
		} catch (Exception e) {
			logger.error("stop threadGroup error", e);
		}
	}

	class R implements Runnable {
		private Channel channel;

		public R(Channel channel) {
			this.channel = channel;
		}

		private boolean stopped = false;

		@Override
		public void run() {
			while (!stopped) {
				try {
					if (shutdown) {
						stopped = true;
						if (closeAfterWrite) {
							// 关闭连接
							Channels.close(channel);
						}
						continue;
					}
					
					if (! channel.isWritable()){
						continue;
					}

					IProtocol constructedMessage = sendQueue.poll(AbstractClient.timeout, TimeUnit.MILLISECONDS);
					if (constructedMessage == null) {
						continue;
					} else {
						// 写入数据
						ChannelFuture writeFuture = channel.write(constructedMessage);

						// 等待1500ms
						writeFuture.awaitUninterruptibly(AbstractClient.timeout);

						logger.debug("client write result:{} => {}",writeFuture.isSuccess(), writeFuture.getChannel());

						if (closeAfterWrite) {
							// 关闭连接
							Channels.close(channel);
						}
					}
				} catch (InterruptedException e) {
					logger.error("write to channel error", e);
				}
			}
		}
	}
}
