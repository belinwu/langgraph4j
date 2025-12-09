//DEPS org.bsc.langgraph4j:langgraph4j-javelit:1.7-SNAPSHOT

import io.javelit.core.Jt;
import org.bsc.javelit.SpinnerComponent;

public class JtSpinnerTestApp {


    public static void main(String[] args) {

        var app = new JtSpinnerTestApp();

        app.view();
    }

    public void view() {
        Jt.title("JtSpinner test App").use();

        var overlay = Jt.toggle("overlay").value(false).use();

        Jt.divider("hr1").use();



        SpinnerComponent.builder()
                .message("**this is the spinner test**")
                .showTime(true)
                .onStart( () -> {
                    try {
                        Thread.sleep(1000 * 5 );
                    } catch (InterruptedException e) {
                        Jt.error( "interrupted exception");
                    }
                    return "my result";
                })
                .onComplete( (result, elapsed ) ->
                    Jt.info("**Completed in** %ds".formatted(elapsed.toSeconds())))
                .overlay( overlay )
                .use();

    }
}
