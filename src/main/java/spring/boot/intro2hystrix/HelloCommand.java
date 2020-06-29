package spring.boot.intro2hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class HelloCommand extends HystrixCommand< String >
{
  private String name;

  HelloCommand(String name)
  {
    super( HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));

    this.name=name;
  }

  @Override
  protected String run(){
    return"Hello "+name+"!";
  }
}