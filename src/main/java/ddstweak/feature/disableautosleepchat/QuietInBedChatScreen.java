package ddstweak.feature.disableautosleepchat;

//#if MC >= 260102
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
//#if MC >= 11904
import net.minecraft.client.gui.components.Renderable;
//#else
//$$ import net.minecraft.client.gui.components.Widget;
//#endif
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.InBedChatScreen;
//#if MC >= 12109
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.input.KeyEvent;
//#endif

/**
 * The normal in-bed screen without the chat UI.
 * The vanilla leave-bed button and Escape behavior are kept intact.
 */
public final class QuietInBedChatScreen extends InBedChatScreen {
    //#if MC >= 12109
    public QuietInBedChatScreen(String initial, boolean isDraft) {
        super(initial, isDraft);
    }
    //#else
    //$$ public QuietInBedChatScreen() {
    //$$     super();
    //$$ }
    //#endif

    @Override
    protected void init() {
        super.init();
        this.input.active=false;
        this.input.visible=false;
    }

    //#if MC >= 260102
    //$$ @Override
    //$$ public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
    //$$     for (GuiEventListener child : this.children()) {
    //$$         if (child != this.input && child instanceof Renderable) {
    //$$             ((Renderable)child).extractRenderState(graphics,mouseX,mouseY,partialTick);
    //$$         }
    //$$     }
    //$$ }
    //#elseif MC >= 12000
    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        for (GuiEventListener child : this.children()) {
            if (child != this.input && child instanceof Renderable) {
                ((Renderable)child).render(graphics,mouseX,mouseY,partialTick);
            }
        }
    }
    //#elseif MC >= 11904
    //$$ @Override
    //$$ public void render(PoseStack graphics,int mouseX,int mouseY,float partialTick) {
    //$$     for (GuiEventListener child : this.children()) {
    //$$         if (child != this.input && child instanceof Renderable) {
    //$$             ((Renderable)child).render(graphics,mouseX,mouseY,partialTick);
    //$$         }
    //$$     }
    //$$ }
    //#elseif MC >= 11600
    //$$ @Override
    //$$ public void render(PoseStack graphics,int mouseX,int mouseY,float partialTick) {
    //$$     for (GuiEventListener child : this.children()) {
    //$$         if (child != this.input && child instanceof Widget) {
    //$$             ((Widget)child).render(graphics,mouseX,mouseY,partialTick);
    //$$         }
    //$$     }
    //$$ }
    //#else
    //$$ @Override
    //$$ public void render(int mouseX,int mouseY,float partialTick) {
    //$$     for (GuiEventListener child : this.children()) {
    //$$         if (child != this.input && child instanceof Widget) {
    //$$             ((Widget)child).render(mouseX,mouseY,partialTick);
    //$$         }
    //$$     }
    //$$ }
    //#endif

    //#if MC >= 12109
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.minecraft.options.keyChat.matches(event)) {
            this.ddstweak$openFullChat();
            return true;
        }
        if (event.isEscape()) {
            return super.keyPressed(event);
        }
        return false;
    }
    //#else
    //$$ @Override
    //$$ public boolean keyPressed(int keyCode,int scanCode,int modifiers) {
    //$$     if (this.minecraft.options.keyChat.matches(keyCode,scanCode)) {
    //$$         this.ddstweak$openFullChat();
    //$$         return true;
    //$$     }
    //$$     if (keyCode == 256) {
    //$$         return super.keyPressed(keyCode,scanCode,modifiers);
    //$$     }
    //$$     return false;
    //$$ }
    //#endif

    //#if MC >= 12109
    private void ddstweak$openFullChat() {
        //#if MC >= 260200
        //$$ this.minecraft.gui.hud.getChat().openScreen(ChatComponent.ChatMethod.MESSAGE,InBedChatScreen::new);
        //#else
        this.minecraft.gui.getChat().openScreen(ChatComponent.ChatMethod.MESSAGE,InBedChatScreen::new);
        //#endif
    }
    //#else
    //$$ private void ddstweak$openFullChat() {
    //$$     this.minecraft.setScreen(new InBedChatScreen());
    //$$ }
    //#endif
}

