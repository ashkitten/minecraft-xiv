package wtf.kity.minecraftxiv.config;

import com.google.common.collect.ImmutableSet;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.impl.SafeBinding;
import dev.isxander.yacl3.impl.utils.YACLConstants;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ToggleableOption<T> implements Option<Pair<Boolean, T>> {
    private final Text name;
    private OptionDescription description;
    private final Controller<Pair<Boolean, T>> controller;
    private final Binding<Pair<Boolean, T>> binding;

    private final ImmutableSet<OptionFlag> flags;

    public Option<Boolean> enabled;
    public Option<T> inner;

    private final List<BiConsumer<Option<Pair<Boolean, T>>, Pair<Boolean, T>>> listeners;
    private int listenerTriggerDepth = 0;

    public ToggleableOption(
            @NotNull Text name,
            @NotNull Function<Pair<Boolean, T>, OptionDescription> descriptionFunction,
            @NotNull Function<Option<Pair<Boolean, T>>, Controller<Pair<Boolean, T>>> controlGetter,
            @NotNull Binding<Pair<Boolean, T>> binding,
            boolean available,
            ImmutableSet<OptionFlag> flags,
            @NotNull Collection<BiConsumer<Option<Pair<Boolean, T>>, Pair<Boolean, T>>> listeners,
            @NotNull Function<Option<T>, ControllerBuilder<T>> innerControlGetter
    ) {
        this.name = name;
        this.binding = new SafeBinding<>(binding);
        this.flags = flags;
        this.listeners = new ArrayList<>(listeners);

        this.inner = Option.<T>createBuilder()
                .name(name)
                .binding(
                        this.binding.getValue().getRight(),
                        () -> this.binding.getValue().getRight(),
                        (val) -> this.binding.setValue(new Pair<>(this.binding.getValue().getLeft(), val))
                )
                .controller(innerControlGetter)
                .flags(flags)
                .listener((opt, val) -> this.triggerListeners(false))
                .build();
        this.enabled = Option.<Boolean>createBuilder()
                .name(Text.empty())
                .description(OptionDescription.of(Text.literal("Allow players on this server to use this feature")))
                .binding(
                        this.binding.getValue().getLeft(),
                        () -> this.binding.getValue().getLeft(),
                        (val) -> this.binding.setValue(new Pair<>(val, this.binding.getValue().getRight()))
                )
                .available(available)
                .listener((opt, val) -> this.inner.setAvailable(val))
                .listener((opt, val) -> this.triggerListeners(false))
                .controller(TickBoxControllerBuilder::create)
                .build();
        this.controller = controlGetter.apply(this);

        addListener((opt, pending) -> description = descriptionFunction.apply(pending));
        triggerListeners(true);
    }

    @Override
    public @NotNull Text name() {
        return name;
    }

    @Override
    public @NotNull OptionDescription description() {
        return this.description;
    }

    @Override
    public @NotNull Text tooltip() {
        return description.text();
    }

    @Override
    public @NotNull Controller<Pair<Boolean, T>> controller() {
        return controller;
    }

    @Override
    public @NotNull Binding<Pair<Boolean, T>> binding() {
        return binding;
    }

    @Override
    public boolean available() {
        return this.enabled.available();
    }

    @Override
    public void setAvailable(boolean available) {
        this.enabled.setAvailable(available);
    }

    @Override
    public @NotNull ImmutableSet<OptionFlag> flags() {
        return flags;
    }

    @Override
    public boolean changed() {
        return !binding().getValue().equals(this.pendingValue());
    }

    @Override
    public @NotNull Pair<Boolean, T> pendingValue() {
        return new Pair<>(this.enabled.pendingValue(), this.inner.pendingValue());
    }

    @Override
    public void requestSet(@NotNull Pair<Boolean, T> value) {
        Validate.notNull(value, "`value` cannot be null");

        this.enabled.requestSet(value.getLeft());
        this.inner.requestSet(value.getRight());

        this.triggerListeners(true);
    }

    @Override
    public boolean applyValue() {
        if (changed()) {
            binding.setValue(pendingValue());
            return true;
        }
        return false;
    }

    @Override
    public void forgetPendingValue() {
        requestSet(binding().getValue());
    }

    @Override
    public void requestSetDefault() {
        requestSet(binding().defaultValue());
    }

    @Override
    public boolean isPendingValueDefault() {
        return binding().defaultValue().equals(pendingValue());
    }

    @Override
    public void addListener(BiConsumer<Option<Pair<Boolean, T>>, Pair<Boolean, T>> changedListener) {
        this.listeners.add(changedListener);
    }

    private void triggerListeners(boolean bypass) {
        if (bypass || listenerTriggerDepth == 0) {
            if (listenerTriggerDepth > 10) {
                throw new IllegalStateException("Listener trigger depth exceeded 10! This means a listener triggered a listener etc etc 10 times deep. This is likely a bug in the mod using YACL!");
            }

            this.listenerTriggerDepth++;

            for (BiConsumer<Option<Pair<Boolean, T>>, Pair<Boolean, T>> listener : listeners) {
                try {
                    listener.accept(this, this.pendingValue());
                } catch (Exception e) {
                    YACLConstants.LOGGER.error("Exception whilst triggering listener for option '%s'".formatted(name.getString()), e);
                }
            }

            this.listenerTriggerDepth--;
        }
    }

    public static class ToggleableOptionBuilder<T> implements Builder<Pair<Boolean, T>> {
        private Text name = Text.literal("Name not specified!").formatted(Formatting.RED);

        private Function<Pair<Boolean, T>, OptionDescription> descriptionFunction = pending -> OptionDescription.EMPTY;

        private Function<Option<Pair<Boolean, T>>, Controller<Pair<Boolean, T>>> controlGetter;

        private Binding<Pair<Boolean, T>> binding;

        private boolean available = true;

        private boolean instant = false;

        private final Set<OptionFlag> flags = new HashSet<>();

        private final List<BiConsumer<Option<Pair<Boolean, T>>, Pair<Boolean, T>>> listeners = new ArrayList<>();

        private Function<Option<T>, ControllerBuilder<T>> innerControlGetter;

        @Override
        public Builder<Pair<Boolean, T>> name(@NotNull Text name) {
            Validate.notNull(name, "`name` cannot be null");

            this.name = name;
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> description(@NotNull OptionDescription description) {
            return description(opt -> description);
        }

        @Override
        public Builder<Pair<Boolean, T>> description(@NotNull Function<Pair<Boolean, T>, OptionDescription> descriptionFunction) {
            this.descriptionFunction = descriptionFunction;
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> controller(@NotNull Function<Option<Pair<Boolean, T>>, ControllerBuilder<Pair<Boolean, T>>> controllerBuilder) {
            Validate.notNull(controllerBuilder, "`controllerBuilder` cannot be null");

            return customController(opt -> controllerBuilder.apply(opt).build());
        }

        @Override
        public Builder<Pair<Boolean, T>> customController(@NotNull Function<Option<Pair<Boolean, T>>, Controller<Pair<Boolean, T>>> control) {
            Validate.notNull(control, "`control` cannot be null");

            this.controlGetter = control;
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> binding(@NotNull Binding<Pair<Boolean, T>> binding) {
            Validate.notNull(binding, "`binding` cannot be null");

            this.binding = binding;
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> binding(@NotNull Pair<Boolean, T> def, @NotNull Supplier<@NotNull Pair<Boolean, T>> getter, @NotNull Consumer<@NotNull Pair<Boolean, T>> setter) {
            Validate.notNull(def, "`def` must not be null");
            Validate.notNull(getter, "`getter` must not be null");
            Validate.notNull(setter, "`setter` must not be null");

            this.binding = Binding.generic(def, getter, setter);
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> available(boolean available) {
            this.available = available;
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> flag(@NotNull OptionFlag... flag) {
            Validate.notNull(flag, "`flag` must not be null");

            this.flags.addAll(Arrays.asList(flag));
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> flags(@NotNull Collection<? extends OptionFlag> flags) {
            Validate.notNull(flags, "`flags` must not be null");

            this.flags.addAll(flags);
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> instant(boolean instant) {
            this.instant = instant;
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> listener(@NotNull BiConsumer<Option<Pair<Boolean, T>>, Pair<Boolean, T>> listener) {
            this.listeners.add(listener);
            return this;
        }

        @Override
        public Builder<Pair<Boolean, T>> listeners(@NotNull Collection<BiConsumer<Option<Pair<Boolean, T>>, Pair<Boolean, T>>> listeners) {
            this.listeners.addAll(listeners);
            return this;
        }

        public ToggleableOptionBuilder<T> innerControl(@NotNull Function<Option<T>, ControllerBuilder<T>> innerControlGetter) {
            this.innerControlGetter = innerControlGetter;
            return this;
        }

        @Override
        public Option<Pair<Boolean, T>> build() {
            Validate.notNull(controlGetter, "`control` must not be null when building `Option`");
            Validate.notNull(binding, "`binding` must not be null when building `Option`");
            Validate.isTrue(!instant || flags.isEmpty(), "instant application does not support option flags");

            if (instant) {
                listeners.add((opt, pendingValue) -> opt.applyValue());
            }

            return new ToggleableOption<>(name, descriptionFunction, controlGetter, binding, available, ImmutableSet.copyOf(flags), listeners, innerControlGetter);
        }
    }
}

