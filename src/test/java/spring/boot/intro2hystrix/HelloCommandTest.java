package spring.boot.intro2hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Assertions;

/**
 * https://www.baeldung.com/introduction-to-hystrix
 */
public class HelloCommandTest
{
//  @Rule JUnit 4
//  public ExpectedException thrown= ExpectedException.none();

  @Test
  public void testHelloCommand()
  {
    assertThat( new HelloCommand("Bob").execute(), equalTo("Hello Bob!"));
  }

  @Test
  public void testSuccess() throws InterruptedException
  {

    HystrixCommand.Setter config = HystrixCommand.Setter.
                                                  withGroupKey( HystrixCommandGroupKey.
                                                                Factory.
                                                                asKey( "RemoteServiceGroupTest4" ) );

    HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
    commandProperties.withExecutionTimeoutInMilliseconds( 10_000 ); // 10 000 > 500 -> success
    config.andCommandPropertiesDefaults( commandProperties );

    assertThat( new RemoteServiceTestCommand( config, new RemoteServiceTestSimulator( 500 ) ).execute(), equalTo( "Success" ) );
  }

  @Test( /*expected = HystrixRuntimeException.class*/ )
  public void testServiceFailure() throws InterruptedException
  {
    HystrixCommand.Setter config = HystrixCommand.Setter
                                                 .withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceGroupTest5"));

    HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
    commandProperties.withExecutionTimeoutInMilliseconds( 5_000); // 5 000 < 15 000 -> HystrixRuntimeException
    config.andCommandPropertiesDefaults( commandProperties);

    Assertions.assertThrows( HystrixRuntimeException.class, () -> new RemoteServiceTestCommand( config, new RemoteServiceTestSimulator(15_000)).execute());
  }

  /**
   * 5.2. Defensive Programming With Limited Thread Pool
   * Mivel timeout eseten a hivo felek tovabb sulyosbitjak a helyzetet tovabbi hivasokkal -> ThreadPool limitet adunk meg a szerver oldalon.
   * @throws InterruptedException
   */
  @Test
  public void testHystrixWithLimitedThreadpool() throws InterruptedException
  {
    HystrixCommand.Setter config = HystrixCommand.Setter
                                                 .withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceGroupThreadPool"));

    HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
    commandProperties.withExecutionTimeoutInMilliseconds(10_000);

    config.andCommandPropertiesDefaults( commandProperties);
    config.andThreadPoolPropertiesDefaults( HystrixThreadPoolProperties.Setter()
                                                                       .withMaxQueueSize(10) // 10 felett visszautasitja az ujabb kereseket
                                                                       .withCoreSize(3)
                                                                       .withQueueSizeRejectionThreshold(10));

    assertThat( new RemoteServiceTestCommand( config, new RemoteServiceTestSimulator(500)).execute(), equalTo("Success"));
  }

  /**
   *
   * The CircuitBreakerSleepWindow which is set to 4,000 ms.
   * This configures the circuit breaker window and defines the time interval after which the request to the remote service will be resumed
   * The CircuitBreakerRequestVolumeThreshold which is set to 1 and defines the minimum number of requests needed before the failure rate will be considered
   *
   * With the above settings in place, our HystrixCommand will now trip open after two failed request.
   * The third request will not even hit the remote service even though we have set the service delay to be 500 ms,
   * Hystrix will short circuit and our method will return null as the response.
   *
   * We will subsequently add a Thread.sleep(5000) in order to cross the limit of the sleep window that we have set.
   * This will cause Hystrix to close the circuit and the subsequent requests will flow through successfully.
   */
  @Test
  public void givenCircuitBreakerSetup_whenRemoteSvcCmdExecuted_thenReturnSuccess()
          throws InterruptedException {

    HystrixCommand.Setter config = HystrixCommand
            .Setter
            .withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceGroupCircuitBreaker"));

    HystrixCommandProperties.Setter properties = HystrixCommandProperties.Setter();
    properties.withExecutionTimeoutInMilliseconds(1000);
    properties.withCircuitBreakerSleepWindowInMilliseconds(4000);
    properties.withExecutionIsolationStrategy
            (HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
    properties.withCircuitBreakerEnabled(true);
    properties.withCircuitBreakerRequestVolumeThreshold(1);

    config.andCommandPropertiesDefaults(properties);
    config.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
            .withMaxQueueSize(1)
            .withCoreSize(1)
            .withQueueSizeRejectionThreshold(1));

    assertThat(this.invokeRemoteService(config, 10_000), equalTo(null));
    assertThat(this.invokeRemoteService(config, 10_000), equalTo(null));
    assertThat(this.invokeRemoteService(config, 10_000), equalTo(null));

    Thread.sleep(5000);

    assertThat(new RemoteServiceTestCommand(config, new RemoteServiceTestSimulator(500)).execute(),
            equalTo("Success"));

    assertThat(new RemoteServiceTestCommand(config, new RemoteServiceTestSimulator(500)).execute(),
            equalTo("Success"));

    assertThat(new RemoteServiceTestCommand(config, new RemoteServiceTestSimulator(500)).execute(),
            equalTo("Success"));
  }

  public String invokeRemoteService(HystrixCommand.Setter config, int timeout) throws InterruptedException
  {
    String response = null;

    try
    {
      response = new RemoteServiceTestCommand(config, new RemoteServiceTestSimulator(timeout)).execute();
    }
    catch (HystrixRuntimeException ex)
    {
      System.out.println( "ex = " + ex);
    }

    return response;
  }
}
