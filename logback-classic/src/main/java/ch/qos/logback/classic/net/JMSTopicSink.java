/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * 
 * Copyright (C) 1999-2006, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */

package ch.qos.logback.classic.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.util.ContextInitializer;

/**
 * A simple application that consumes logging events sent by a {@link
 * JMSTopicAppender}.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class JMSTopicSink implements javax.jms.MessageListener {

  private Logger logger = (Logger)LoggerFactory.getLogger(JMSTopicSink.class);

  static public void main(String[] args) throws Exception {
    if (args.length != 4) {
      usage("Wrong number of arguments.");
    }

    String tcfBindingName = args[0];
    String topicBindingName = args[1];
    String username = args[2];
    String password = args[3];

    LoggerContext loggerContext = (LoggerContext) LoggerFactory
        .getILoggerFactory();
    ContextInitializer.autoConfig(loggerContext);

    new JMSTopicSink(tcfBindingName, topicBindingName, username, password);

    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    // Loop until the word "exit" is typed
    System.out.println("Type \"exit\" to quit JMSSink.");
    while (true) {
      String s = stdin.readLine();
      if (s.equalsIgnoreCase("exit")) {
        System.out.println("Exiting. Kill the application if it does not exit "
            + "due to daemon threads.");
        return;
      }
    }
  }

  public JMSTopicSink(String tcfBindingName, String topicBindingName,
      String username, String password) {

    try {
      Properties env = new Properties();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
      env.put(Context.PROVIDER_URL, "tcp://localhost:61616");
      Context ctx = new InitialContext(env);
      TopicConnectionFactory topicConnectionFactory;
      topicConnectionFactory = (TopicConnectionFactory) lookup(ctx,
          tcfBindingName);
      System.out.println("Topic Cnx Factory found");
      Topic topic = (Topic) ctx.lookup(topicBindingName);
      System.out.println("Topic found: " + topic.getTopicName());

      TopicConnection topicConnection = topicConnectionFactory
          .createTopicConnection();
      System.out.println("Topic Connection created");
      
      TopicSession topicSession = topicConnection.createTopicSession(false,
          Session.AUTO_ACKNOWLEDGE);

      TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic);

      topicSubscriber.setMessageListener(this);
      
      topicConnection.start();
      System.out.println("Topic Connection started");
      
    } catch (Exception e) {
      logger.error("Could not read JMS message.", e);
    }
  }

  public void onMessage(javax.jms.Message message) {
    LoggingEvent event;
    System.out.println("xxxx onMessage called");
    try {
      if (message instanceof ObjectMessage) {
        ObjectMessage objectMessage = (ObjectMessage) message;
        event = (LoggingEvent) objectMessage.getObject();
        logger.callAppenders(event);
      } else {
        logger.warn("Received message is of type " + message.getJMSType()
            + ", was expecting ObjectMessage.");
      }
    } catch (JMSException jmse) {
      logger.error("Exception thrown while processing incoming message.", jmse);
    }
  }

  protected Object lookup(Context ctx, String name)
      throws NamingException {
    try {
      return ctx.lookup(name);
    } catch (NameNotFoundException e) {
      logger.error("Could not find name [" + name + "].");
      throw e;
    }
  }

  static void usage(String msg) {
    System.err.println(msg);
    System.err
        .println("Usage: java "
            + JMSTopicSink.class.getName()
            + " TopicConnectionFactoryBindingName TopicBindingName username password");
    System.exit(1);
  }
}