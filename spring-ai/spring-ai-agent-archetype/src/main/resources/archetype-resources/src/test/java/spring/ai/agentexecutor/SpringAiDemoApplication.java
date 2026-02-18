#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agentexecutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringAiDemoApplication  {

	/**
  * The entry point of the application. This method is executed when the application starts.
  *
  * @param args An array of strings that are arguments passed to the application.
  */
 public static void main(String[] args) {
		SpringApplication.run(SpringAiDemoApplication.class, args);
	}

}