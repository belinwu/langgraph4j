package org.bsc.javelit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javelit.core.JtComponent;
import io.javelit.core.JtComponentBuilder;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.Language;

import java.io.StringWriter;

public class JtSpinner extends JtComponent<Boolean> {

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("spinner.register.html.mustache");
        renderTemplate = mf.compile("spinner.render.html.mustache");
    }

    // visible to the template engine
    final boolean loading;
    final String message;
    final boolean showTime;
    final boolean overlay;

    public static class Builder extends JtComponentBuilder<Boolean, JtSpinner, Builder> {
        private @Language("markdown") String message;
        private Boolean loading = false;
        private Boolean showTime = false;
        private Boolean overlay = false;

        /**
         * The error message content to display. Markdown is supported, see {@link io.javelit.core.Jt#markdown(String)} for more details.
         */
        public Builder message(final @Language("markdown") @Nonnull String message) {
            this.message = message;
            return this;
        }

        public Builder loading( boolean loading) {
            this.loading = loading;
            return this;
        }

        public Builder showTime( boolean showTime) {
            this.showTime = showTime;
            return this;
        }

        public Builder overlay( boolean overlay) {
            this.overlay = overlay;
            return this;
        }

        @Override
        public JtSpinner build() {
            return new JtSpinner(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private JtSpinner( Builder builder ) {
        super(builder, builder.loading, ( value )-> {
            System.out.printf( "CALLBACK %s%n", value );
        });
        this.loading = builder.loading;
        this.message = markdownToHtml(builder.message, true);
        this.showTime = builder.showTime;
        this.overlay = builder.overlay;

    }

    @Override
    protected String register() {
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected String render() {
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected TypeReference<Boolean> getTypeReference() {
        return new TypeReference<>() {
        };
    }

}
