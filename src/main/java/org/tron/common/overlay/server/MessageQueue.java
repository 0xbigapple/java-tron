/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.overlay.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.PingMessage;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.common.overlay.message.StaticMessages;

/**
 * This class contains the logic for sending messages in a queue
 *
 * Messages open by send and answered by receive of appropriate message
 *      PING by PONG
 *      GET_PEERS by PEERS
 *      GET_TRANSACTIONS by TRANSACTIONS
 *      GET_BLOCK_HASHES by BLOCK_HASHES
 *      GET_BLOCKS by BLOCKS
 *
 * The following messages will not be answered:
 *      PONG, PEERS, HELLO, STATUS, TRANSACTIONS, BLOCKS
 *
 * @author Roman Mandeleil
 */
@Component
@Scope("prototype")
public class MessageQueue {

  private static final Logger logger = LoggerFactory.getLogger("MessageQueue");

  private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(4, new ThreadFactory() {
    private AtomicInteger cnt = new AtomicInteger(0);

    public Thread newThread(Runnable r) {
      return new Thread(r, "MessageQueueTimer-" + cnt.getAndIncrement());
    }
  });

  private Queue<MessageRoundtrip> requestQueue = new ConcurrentLinkedQueue<>();
  private Queue<MessageRoundtrip> respondQueue = new ConcurrentLinkedQueue<>();
  private ChannelHandlerContext ctx = null;

//  @Autowired
//  EthereumListener ethereumListener;
  boolean hasPing = false;
  private ScheduledFuture<?> timerTask;
  private Channel channel;

  public MessageQueue() {
  }

  public void activate(ChannelHandlerContext ctx) {
    this.ctx = ctx;
    timerTask = timer.scheduleAtFixedRate(() -> {
      try {
        nudgeQueue();
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 10, 10, TimeUnit.MILLISECONDS);
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public void sendMessage(Message msg) {
    if (msg instanceof PingMessage) {
      if (hasPing) return;
      hasPing = true;
    }

    if (msg.getAnswerMessage() != null)
      requestQueue.add(new MessageRoundtrip(msg));
    else
      respondQueue.add(new MessageRoundtrip(msg));
  }

  public void disconnect() {
    disconnect(StaticMessages.DISCONNECT_MESSAGE);
  }

  public void disconnect(ReasonCode reason) {
    disconnect(new DisconnectMessage(reason));
  }

  private void disconnect(DisconnectMessage msg) {
    ctx.writeAndFlush(msg);
    ctx.close();
  }

  public void receivedMessage(Message msg) throws InterruptedException {

    if (requestQueue.peek() != null) {
      MessageRoundtrip messageRoundtrip = requestQueue.peek();
      Message waitingMessage = messageRoundtrip.getMsg();

      if (waitingMessage instanceof PingMessage) hasPing = false;

      if (waitingMessage.getAnswerMessage() != null
          && msg.getClass() == waitingMessage.getAnswerMessage()) {
        messageRoundtrip.answer();
      }
    }
  }

  private void removeAnsweredMessage(MessageRoundtrip messageRoundtrip) {
    if (messageRoundtrip != null && messageRoundtrip.isAnswered())
      requestQueue.remove();
  }

  private void nudgeQueue() {
    // remove last answered message on the queue
    removeAnsweredMessage(requestQueue.peek());
    // Now send the next message
    sendToWire(respondQueue.poll());
    sendToWire(requestQueue.peek());
  }

  private void sendToWire(MessageRoundtrip messageRoundtrip) {

    if (messageRoundtrip != null && messageRoundtrip.getRetryTimes() == 0) {
      // TODO: retry logic || messageRoundtrip.hasToRetry()){

      Message msg = messageRoundtrip.getMsg();

      //TODO#p2p#peerDel : let node know
      logger.info(msg.toString());

      ctx.writeAndFlush(msg.getSendData())
          .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

      if (msg.getAnswerMessage() != null) {
        messageRoundtrip.incRetryTimes();
        messageRoundtrip.saveTime();
      }
    }
  }

  public void close() {
    if (!timerTask.isCancelled()) {
      timerTask.cancel(false);
    }
  }
}