package de.braintags.vertx;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;

public class TestMail {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    MailClient mailClient = MailClient.createShared(vertx, initMailClientSettings(), "exampleclient");

    MailMessage message = new MailMessage();
    message.setFrom("xxx@braintags.de");
    message.setTo("to schmitti <rschmitt@braintags.de>");
    message.setCc("cc michael <michael.remme@braintags.de>");
    message.setBcc("bcc home <home@braintags.de>");
    message.setText("this is the plain message text");

    mailClient.sendMail(message, result -> {
      if (result.succeeded()) {
        System.out.println("###" + result.result());
        mailClient.close();
        vertx.close();
      } else {
        result.cause().printStackTrace();
      }
    });

  }

  private static MailConfig initMailClientSettings() {
    MailConfig config = new MailConfig();
    String mailUserName = System.getProperty("username");
    if (mailUserName != null && mailUserName.hashCode() != 0) {
      config.setUsername(mailUserName);
    } else
      throw new NullPointerException();

    String mailClientPassword = System.getProperty("password");
    if (mailClientPassword != null && mailClientPassword.hashCode() != 0) {
      config.setPassword(mailClientPassword);
    } else
      throw new NullPointerException();
    String mailClientHost = System.getProperty("host");
    if (mailClientHost != null && mailClientHost.hashCode() != 0) {
      config.setHostname(mailClientHost);
    } else
      throw new NullPointerException();

    String mailClientPort = System.getProperty("port");
    if (mailClientPort != null && mailClientPort.hashCode() != 0) {
      config.setPort(Integer.parseInt(mailClientPort));
    } else
      throw new NullPointerException();
    config.setSsl(false).setTrustAll(true).setStarttls(StartTLSOptions.DISABLED).setKeepAlive(true);
    return config;

  }

}
