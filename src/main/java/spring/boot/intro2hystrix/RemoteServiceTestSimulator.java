package spring.boot.intro2hystrix;

/**
 * Introduction to Hystrix
 * https://www.baeldung.com/introduction-to-hystrix
 *
 * represents a service on a remote server.
 * It has a method which responds with a message after the given period of time.
 * We can imagine that this wait is a simulation of a time consuming process at
 * the remote system resulting in a delayed response to the calling service
 *
 *
 *
 *
 *
 *
 */
public class RemoteServiceTestSimulator
{
  private long wait;

  RemoteServiceTestSimulator(long wait) throws InterruptedException
  {
    this.wait = wait;
  }

  String execute() throws InterruptedException
  {
    Thread.sleep(wait);
    return "Success";
  }
}
