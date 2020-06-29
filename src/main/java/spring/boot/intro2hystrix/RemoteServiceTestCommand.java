package spring.boot.intro2hystrix;

import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommand;

class RemoteServiceTestCommand extends HystrixCommand<String>
{
  private RemoteServiceTestSimulator remoteService;

  RemoteServiceTestCommand( Setter config, RemoteServiceTestSimulator remoteService)
  {
    super( config);
    this.remoteService = remoteService;
  }

  @Override
  protected String run() throws Exception
  {
    return remoteService.execute();
  }
}