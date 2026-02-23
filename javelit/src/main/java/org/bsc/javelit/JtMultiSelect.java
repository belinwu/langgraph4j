package org.bsc.javelit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javelit.core.JtComponent;
import io.javelit.core.JtComponentBuilder;
import jakarta.annotation.Nonnull;

import java.io.StringWriter;
import java.util.Set;

public class JtMultiSelect extends JtComponent<Set<String>> {

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("MultiSelect.register.html.mustache");
        renderTemplate = mf.compile("MultiSelect.render.html.mustache");
    }

    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<Set<String>, JtMultiSelect, Builder> {

        private Set<String> items;
        private boolean disabled;

        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder items( Set<String> items ) {
            this.items = items;
            return this;
        }

        @Override
        public JtMultiSelect build() {
            return new JtMultiSelect(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    final Set<String> initialItems;
    final boolean disabled;

    protected JtMultiSelect(@Nonnull Builder builder) {
        super(builder, Set.of(), null);

        this.initialItems = builder.items;
        this.disabled = builder.disabled;
    }

    public String getItemsJson() {
        return toJson(initialItems);
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
    protected TypeReference<Set<String>> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
