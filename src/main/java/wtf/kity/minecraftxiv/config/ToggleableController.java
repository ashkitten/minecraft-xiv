package wtf.kity.minecraftxiv.config;

import dev.isxander.yacl3.api.Binding;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.StateManager;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.YACLTooltipPositioner;
import dev.isxander.yacl3.gui.controllers.TickBoxController;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

//? >=1.21.11
import net.minecraft.client.gui.components.WidgetTooltipHolder;

//? >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//? }

public class ToggleableController<T> implements Controller<T> {
    public final Option<Boolean> enabled;
    public final Controller<T> inner;
    private final Option<T> option;
    private final Supplier<Component> tickBoxTooltipFunction;

    public ToggleableController(
            Option<T> option,
            Function<Option<T>, ControllerBuilder<T>> inner,
            Binding<Boolean> availableBinding,
            Supplier<Component> tickBoxTooltipFunction
    ) {
        this.option = option;
        this.inner = inner.apply(option).build();
        this.tickBoxTooltipFunction = tickBoxTooltipFunction;

        this.enabled = Option
                .<Boolean>createBuilder()
                .name(Component.empty())
                .stateManager(StateManager.createInstant(availableBinding))
                .controller(TickBoxControllerBuilder::create)
                .addListener((opt, event) -> this.option.setAvailable(opt.pendingValue()))
                .available(option.available())
                .build();
    }

    @Override
    public Option<T> option() {
        return this.option;
    }

    @Override
    public Component formatValue() {
        return null;
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new ToggleableControllerWidget<>(this, screen, widgetDimension);
    }

    public static class ToggleableControllerWidget<T> extends AbstractWidget implements ContainerEventHandler {
        private final ToggleableController<T> control;
        private final TickBoxController.TickBoxControllerElement tickBox;
        private final AbstractWidget inner;
        private YACLScreen screen;
        //? >=1.21.11
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

        public ToggleableControllerWidget(ToggleableController<T> control, YACLScreen screen, Dimension<Integer> dim) {
            super(dim);
            this.control = control;
            this.screen = screen;
            this.tickBox = (TickBoxController.TickBoxControllerElement) control.enabled
                    .controller()
                    .provideWidget(screen, dim);
            this.inner = control.inner.provideWidget(screen, dim);
            this.setDimension(dim);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            this.tickBox.render(graphics, mouseX, mouseY, delta);
            this.inner.render(graphics, mouseX, mouseY, delta);
            //? <1.21.11 {
            /*screen.setTooltipForNextRenderPass(
                    Tooltip.create(this.control.tickBoxTooltipFunction.get()),
                    new YACLTooltipPositioner(this.tickBox),
                    this.tickBox.isHovered()
            );
            *///? } else {
                this.tooltip.set(Tooltip.create(this.control.tickBoxTooltipFunction.get()));
                this.tooltip.refreshTooltipForNextRenderPass(
                        graphics,
                        mouseX,
                        mouseY,
                        this.tickBox.isHovered(),
                        this.tickBox.isFocused(),
                        this.tickBox.getRectangle()
                );
            //? }
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return List.of(this.tickBox, this.inner);
        }

        @Override
        public boolean isDragging() {
            return false;
        }

        @Override
        public void setDragging(boolean dragging) {

        }

        @Override
        public @Nullable GuiEventListener getFocused() {
            if (this.tickBox.isFocused()) {
                return tickBox;
            } else if (this.inner.isFocused()) {
                return inner;
            }
            return null;
        }

        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
            if (focused instanceof AbstractWidget) {
                focused.setFocused(true);
            }
        }

        @Override
        public boolean canReset() {
            return this.tickBox.canReset() || this.inner.canReset();
        }

        @Override
        public void setDimension(Dimension<Integer> dim) {
            super.setDimension(dim);
            this.tickBox.setDimension(Dimension.ofInt(dim.x(), dim.y(),
                    // Square
                    dim.height(), dim.height()
            ));
            this.inner.setDimension(Dimension.ofInt(
                    dim.x() + dim.height(),
                    dim.y(),
                    dim.width() - dim.height(),
                    dim.height()
            ));
        }

        @Override
        public void unfocus() {
            this.tickBox.unfocus();
            this.inner.unfocus();
        }

        @Override
        public boolean matchesSearch(String query) {
            return this.inner.matchesSearch(query);
        }

        @Override
        public void updateNarration(NarrationElementOutput builder) {
            this.tickBox.updateNarration(builder);
            this.inner.updateNarration(builder);
        }

        @Override
        //? >=1.21.9 {
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            return ContainerEventHandler.super.mouseClicked(mouseButtonEvent, doubleClick);
        //? } else {
        /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return ContainerEventHandler.super.mouseClicked(mouseX, mouseY, button);
        *///? }
        }

        @Override
        //? >=1.21.9 {
        public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
            return ContainerEventHandler.super.mouseReleased(mouseButtonEvent);
        //? } else {
        /*public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return ContainerEventHandler.super.mouseReleased(mouseX, mouseY, button);
        *///? }
        }

        @Override
        //? >=1.21.9 {
        public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dx, double dy) {
            return ContainerEventHandler.super.mouseDragged(mouseButtonEvent, dx, dy);
        //? } else {
        /*public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
            return ContainerEventHandler.super.mouseDragged(mouseX, mouseY, button, dx, dy);
        *///? }
        }

        @Override
        //? >=1.21.9 {
        public boolean keyPressed(KeyEvent keyEvent) {
            return ContainerEventHandler.super.keyPressed(keyEvent);
        //? } else {
        /*public boolean keyPressed(int keycode, int scancode, int modifiers) {
            return ContainerEventHandler.super.keyPressed(keycode, scancode, modifiers);
        *///? }
        }

        @Override
        //? >=1.21.9 {
        public boolean keyReleased(KeyEvent keyEvent) {
            return ContainerEventHandler.super.keyReleased(keyEvent);
        //? } else {
        /*public boolean keyReleased(int keycode, int scancode, int modifiers) {
            return ContainerEventHandler.super.keyReleased(keycode, scancode, modifiers);
        *///? }
        }

        @Override
        //? >=1.21.9 {
        public boolean charTyped(CharacterEvent characterEvent) {
            return ContainerEventHandler.super.charTyped(characterEvent);
        //? } else {
        /*public boolean charTyped(char codePoint, int modifiers) {
            return ContainerEventHandler.super.charTyped(codePoint, modifiers);
        *///? }
        }
    }
}
