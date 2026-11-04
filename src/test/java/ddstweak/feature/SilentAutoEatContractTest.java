package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class SilentAutoEatContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();

    private static String read(String path) throws Exception {
        Path file=ROOT.resolve(path);
        return Files.exists(file)?Files.readString(file):"";
    }

    @Test void configIsRegisteredAndVisible() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        assertTrue(configs.contains("AUTO_EAT=MalilibCompat.bool(\"autoEat\",false"));
        assertTrue(configs.contains("AUTO_EAT_HUNGER_THRESHOLD"));
        assertTrue(configs.contains("AUTO_EAT_NO_SLOWDOWN=MalilibCompat.bool(\"autoEatNoSlowdown\",false"));
        assertTrue(configs.contains("AUTO_EAT_FOOD_BLACKLIST"));
        assertTrue(configs.contains("\"minecraft:pufferfish\""));
        assertTrue(configs.contains("\"minecraft:golden_apple\""));
        assertTrue(screen.contains("c.add(Configs.Tweaks.AUTO_EAT)"));
        assertTrue(screen.contains("c.add(Configs.Tweaks.AUTO_EAT_OFFHAND_POLICY)"));
        assertTrue(screen.contains("c.add(Configs.Tweaks.AUTO_EAT_HUNGER_THRESHOLD)"));
        assertTrue(screen.contains("c.add(Configs.Tweaks.AUTO_EAT_NO_SLOWDOWN)"));
        assertTrue(screen.contains("c.add(Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST)"));
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String bindings=read("src/main/java/ddstweak/gui/itempicker/ItemPickerConfigBindings.java");
        String pickerMixin=read("src/main/java/ddstweak/mixin/feature/dropconfirmation/DropConfirmStringListButtonMixin.java");
        assertTrue(picker.contains("ItemPickerKind.FOOD"));
        assertTrue(picker.contains("SilentAutoEatFoodRules.isEdible(item)"));
        assertTrue(bindings.contains("register(Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST, ItemPickerKind.FOOD,"));
        assertTrue(pickerMixin.contains("ItemPickerConfigBindings.open(GuiUtils.getCurrentScreen(),config)"));
    }

    @Test void offhandPolicyIsPersistedTranslatedAndAppliedOnlyAtPlanning() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String policy=read("src/main/java/ddstweak/feature/silentautoeat/AutoEatOffhandPolicy.java");

        assertTrue(configs.contains("AUTO_EAT_OFFHAND_POLICY=MalilibCompat.optionList(\"autoEatOffhandPolicy\",AutoEatOffhandPolicy.PROTECT_CRITICAL,TWEAKS_KEY)"));
        assertTrue(configs.contains("AUTO_EAT,AUTO_EAT_OFFHAND_POLICY,AUTO_EAT_HUNGER_THRESHOLD"));
        assertTrue(screen.contains("c.add(Configs.Tweaks.AUTO_EAT_OFFHAND_POLICY)"));
        assertTrue(screen.contains("Configs.Tweaks.AUTO_EAT_OFFHAND_POLICY,Configs.Tweaks.AUTO_EAT_HUNGER_THRESHOLD"));
        assertTrue(feature.contains("Configs.Tweaks.AUTO_EAT_OFFHAND_POLICY.getOptionListValue()"));
        assertTrue(feature.contains("policy,isEligibleFood(offhand)"));
        assertTrue(feature.contains("private static final boolean STAGED_SERVER_ACK_READY=false;"));
        // An active transaction is not re-planned halfway through use.
        String preparation=feature.substring(feature.indexOf("private static void prepare("),
                feature.indexOf("private static boolean preferSwapFreeHotbar("));
        assertEquals(1,preparation.split("getOptionListValue\\(", -1).length-1);
        assertTrue(policy.contains("PROTECT_CRITICAL(\"protect_critical\")"));
        assertTrue(policy.contains("NEVER_REPLACE(\"never_replace\")"));
        assertTrue(policy.contains("ALLOW_REPLACE(\"allow_replace\")"));
        for(String lang:new String[]{"en_us","zh_cn","zh_tw"}){
            String json=read("src/main/resources/assets/dds-tweak/lang/"+lang+".json");
            for(String field:new String[]{"name","prettyName","comment"})
                assertTrue(json.contains("dds-tweak.config.tweaks."+field+".autoEatOffhandPolicy"),lang);
            for(String mode:new String[]{"protect_critical","never_replace","allow_replace"})
                assertTrue(json.contains("dds-tweak.auto_eat.offhand_policy."+mode),lang);
        }
        // User-excluded feature additions remain out of scope.
        assertFalse(configs.contains("AUTO_EAT_TARGET_HUNGER"));
        assertFalse(configs.contains("AUTO_EAT_FOOD_SCORING"));
    }

    @Test void alreadyHeldFoodHasPriorityAndNeverCausesArtificialSlotSwap() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String planner=read("src/main/java/ddstweak/feature/silentautoeat/AutoEatExecutionPlanner.java");
        int begin=feature.indexOf("private static int findFood(");
        int end=feature.indexOf("private static boolean isProtectedOffhand(",begin);
        String finder=feature.substring(begin,end);
        int held=finder.indexOf("isEligibleFood(inventory.getItem(selected)))return selected;");
        int offhand=finder.indexOf("if(isEligibleFood(offhand))return -1;");
        int rest=finder.indexOf("AutoEatExecutionPlanner.selectFoodSource(");
        assertTrue(begin>=0&&held>=0&&offhand>held&&rest>offhand,
                "Held edible mainhand must win over offhand and every other food slot");
        assertTrue(finder.contains("SilentAutoEatCompat.menuSlot(player,selected)>=0"));
        assertTrue(planner.contains("sourceIndex==selectedIndex"));
        assertTrue(planner.indexOf("return Path.MAIN_HAND_HOTBAR;") <
                planner.indexOf("if(offhandIsAllowedFood)return Path.USE_OFFHAND_DIRECT;"));
        int select=feature.indexOf("private static void selectMainhand(");
        int finish=feature.indexOf("private static boolean mainhandSelectionOwned()",select);
        String main=feature.substring(select,finish);
        assertTrue(main.contains("mainhandSelectedByAuto=mainhandAutoSelected!=mainhandOriginalSelected;"));
        assertTrue(main.contains("if(mainhandSelectedByAuto)"));
        assertTrue(main.contains("SilentAutoEatCompat.selectHotbar(inventory,mainhandAutoSelected);"));
        assertTrue(feature.contains("SilentAutoEatCompat.useMainhand(minecraft,owner);"));
        // Unavailable server ACK must still block inventory staging.
        assertTrue(feature.contains("private static final boolean STAGED_SERVER_ACK_READY=false;"));
    }

    @Test void v263PrefersNoRealOffhandSwapForAnyEligibleHotbarFood() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String planner=read("src/main/java/ddstweak/feature/silentautoeat/AutoEatExecutionPlanner.java");
        String rules=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatRules.java");

        assertTrue(feature.contains("private static boolean preferSwapFreeHotbar(AutoEatOffhandPolicy policy)"));
        assertTrue(feature.contains("//#if MC >= 260300"));
        assertTrue(feature.contains("//$$ return policy!=AutoEatOffhandPolicy.ALLOW_REPLACE;"));
        assertTrue(feature.contains("return false;"));
        assertTrue(feature.contains("boolean preferHotbar=preferSwapFreeHotbar(policy);"));
        assertTrue(feature.contains("findFood(player,inventory,offhand,preferHotbar)"));
        assertTrue(feature.contains("isProtectedOffhand(offhand),candidate,SilentAutoEatCompat.selectedSlot(inventory),"));
        assertTrue(planner.contains("sourceIndex==selectedIndex||preferMainhandHotbar"));
        assertTrue(planner.contains("return Path.MAIN_HAND_HOTBAR;"));
        assertTrue(rules.contains("if(preferHotbar)"));
        assertTrue(rules.contains("for(int i=0;i<9;i++)if(i!=selected&&eligible.test(i))return i;"));
        // The source is still validated by the real menu / existing blacklist.
        assertTrue(feature.contains("SilentAutoEatCompat.menuSlot(player,index)>=0&&isEligibleFood(inventory.getItem(index))"));
        int begin=feature.indexOf("private static void selectMainhand(");
        int end=feature.indexOf("private static void swapToOffhand(",begin);
        String mainhand=feature.substring(begin,end);
        assertFalse(mainhand.contains("swapWithOffhand("));
        assertTrue(mainhand.contains("SilentAutoEatCompat.useMainhand(minecraft,owner);"));
        assertTrue(mainhand.contains("mainhandSelectedByAuto&&mainhandSelectionOwned()"));
        assertTrue(feature.contains("private static final boolean STAGED_SERVER_ACK_READY=false;"));
    }

    @Test void protectedOffhandHotbarFallbackUsesOneOwnedMainhandPath() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String compat=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatCompat.java");
        String planner=read("src/main/java/ddstweak/feature/silentautoeat/AutoEatExecutionPlanner.java");
        String rules=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatRules.java");
        assertTrue(feature.contains("AutoEatExecutionPlanner.plan("));
        assertTrue(feature.contains("Configs.Tweaks.AUTO_EAT_OFFHAND_POLICY.getOptionListValue()"));
        assertTrue(feature.contains("policy,isEligibleFood(offhand)"));
        assertTrue(feature.contains("private static final boolean STAGED_SERVER_ACK_READY=false;"));
        assertTrue(feature.contains("path==AutoEatExecutionPlanner.Path.MAIN_HAND_STAGED"));
        assertTrue(feature.contains("&&!STAGED_SERVER_ACK_READY&&!experimental26StagingEnabled()"));
        assertTrue(feature.contains("?AutoEatScratchSelection.selected(selected,"));
        assertTrue(feature.contains(":AutoEatScratchSelection.choose(selected,"));
        assertTrue(feature.contains("state=State.STAGED_SWAP;"));
        assertTrue(feature.contains("mainhandOriginalSelected=selected;"));
        assertTrue(feature.contains("state=State.MAIN_HAND_SELECT;"));
        assertTrue(feature.contains("case MAIN_HAND_SELECT:selectMainhand(minecraft);break;"));
        assertTrue(feature.contains("case MAIN_HAND_START_USE:startMainhandUse(minecraft);break;"));
        assertTrue(feature.contains("case MAIN_HAND_USING:watchMainhandUse(minecraft);break;"));
        assertTrue(feature.contains("mainhandSelectedByAuto&&mainhandSelectionOwned()"));
        assertTrue(feature.contains("SilentAutoEatCompat.useMainhand(minecraft,owner);"));
        assertTrue(feature.contains("owner.getUsedItemHand()==InteractionHand.MAIN_HAND"));
        assertTrue(feature.contains("if(mainhandPath()){"));
        assertTrue(feature.contains("finishMainhand(minecraft,true);"));
        assertTrue(compat.contains("inventory.setSelectedSlot(slot);"));
        assertTrue(compat.contains("inventory.selected=slot;"));
        assertTrue(compat.contains("InteractionHand.MAIN_HAND"));
        assertTrue(planner.contains("MAIN_HAND_HOTBAR"));
        assertTrue(planner.contains("MAIN_HAND_STAGED"));
        // Existing source precedence, hunger/blacklist and physical offhand restore stay intact.
        assertTrue(feature.contains("AutoEatExecutionPlanner.selectFoodSource("));
        assertTrue(feature.contains("AUTO_EAT_HUNGER_THRESHOLD"));
        assertTrue(feature.contains("AUTO_EAT_FOOD_BLACKLIST"));
        assertTrue(feature.contains("state=State.RESTORE_OFFHAND"));
        assertTrue(feature.contains("SilentAutoEatCompat.swapWithOffhand("));
        assertTrue(rules.contains("sourceSelectionConflicts("));
        // The selected hotbar path must not borrow offhand SWAP or open a
        // second inventory item transaction before staged ACK exists.
        int start=feature.indexOf("private static void selectMainhand(");
        int end=feature.indexOf("private static void swapToOffhand(",start);
        String main=feature.substring(start,end);
        assertFalse(main.contains("swapWithOffhand("));
        assertFalse(main.contains("handleInventoryMouseClick("));
        assertFalse(main.contains("handleContainerInput("));
        assertFalse(main.contains("setItem("));
    }

    @Test void protectedOffhandMainhandEatingUsesSilentFirstPersonPoseOnly() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String renderer=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");
        assertTrue(feature.contains("public static boolean isAutomatedMainhandUse(LocalPlayer player)"));
        assertTrue(feature.contains("owner==player&&state==State.MAIN_HAND_USING"));
        assertTrue(feature.contains("player.isUsingItem()&&player.getUsedItemHand()==InteractionHand.MAIN_HAND"));
        assertTrue(feature.contains("return isPresentationActive(player)||isAutomatedMainhandUse(player);"));
        // The first-person consumer hides the pose, not actual LocalPlayer use.
        assertTrue(renderer.contains("SilentAutoEat.shouldHideUseAnimation((LocalPlayer)player)"));
        assertTrue(renderer.contains("SilentAutoEat.shouldHideUseAnimation(player)?false:original.call(player)"));
        assertTrue(renderer.contains("if(player!=null&&SilentAutoEat.isMainhandVisualSourceActive(player))"));
        int modern=renderer.indexOf("//$$     if(SilentAutoEat.isMainhandVisualSourceActive(player)){");
        int suppress=renderer.indexOf("//$$         state.useItemRemainingTicks=0;",modern);
        int visual=renderer.indexOf("SilentAutoEatPresentation.projectMainhand(player,state.mainHandItem)",suppress);
        int model=renderer.indexOf("updateForLiving(",visual);
        int done=renderer.indexOf("//$$         return;",model);
        int projection=renderer.indexOf("//$$     boolean active=SilentAutoEat.isPresentationActive(player)",done);
        assertTrue(modern>=0&&suppress>modern&&visual>suppress&&model>visual
                &&done>model&&projection>done);
        assertTrue(renderer.substring(modern,done).contains("state.mainHandItem=visual;"));
        assertFalse(renderer.substring(modern,done).contains("state.offHandItem="));
        // Actual use and hunger must retain vanilla semantics.
        assertTrue(feature.contains("SilentAutoEatCompat.useMainhand(minecraft,owner);"));
        assertTrue(feature.contains("state=State.MAIN_HAND_USING;"));
        assertTrue(feature.contains("owner.getUsedItemHand()==InteractionHand.MAIN_HAND"));
    }

    @Test void mainhandFallbackKeepsOriginalVisualItemWithoutEquipAnimation() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String projection=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        String renderer=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");

        assertTrue(feature.contains("public static boolean isMainhandVisualSourceActive(LocalPlayer player)"));
        assertTrue(feature.contains("owner!=player||mainhandOriginalSelected<0||mainhandOriginalSelected>=9"));
        assertTrue(feature.contains("state!=State.MAIN_HAND_START_USE&&state!=State.MAIN_HAND_USING"));
        assertTrue(feature.contains("selectedSlot(SilentAutoEatCompat.inventory(player))==mainhandAutoSelected"));
        assertTrue(feature.contains("return SilentAutoEatCompat.inventory(player).getItem(mainhandOriginalSelected);"));
        assertTrue(projection.contains("if(SilentAutoEat.isMainhandVisualSourceActive(player))"));
        assertTrue(projection.contains("return SilentAutoEat.mainhandOriginalVisual(player);"));
        // No change to the real mainhand food source or vanilla use transaction.
        assertTrue(feature.contains("SilentAutoEatCompat.useMainhand(minecraft,owner);"));
        assertTrue(feature.contains("SilentAutoEatCompat.selectHotbar(inventory,mainhandAutoSelected);"));
        assertTrue(feature.contains("SilentAutoEatCompat.selectHotbar(SilentAutoEatCompat.inventory(owner),"));
        // Legacy renderer repairs its own visual cache only. No inventory writes.
        assertTrue(renderer.contains("//$$ this.mainHandItem=SilentAutoEatPresentation.projectMainhand(player,player.getMainHandItem());"));
        assertTrue(renderer.contains("this.mainHandHeight=1.0F;"));
        assertTrue(renderer.contains("this.oMainHandHeight=1.0F;"));
        // 1.21.11 final hand renderer argument uses the same projection.
        assertTrue(renderer.contains("method=\"renderArmWithItem\""));
        assertTrue(renderer.contains("?SilentAutoEatPresentation.projectMainhand(player,upstream)"));
        // 26.3 updates final extracted render models but not vanilla caches.
        int modern=renderer.indexOf("//$$     if(SilentAutoEat.isMainhandVisualSourceActive(player)){");
        int end=renderer.indexOf("//$$         return;",modern);
        String branch=renderer.substring(modern,end);
        assertTrue(branch.contains("state.useItemRemainingTicks=0;"));
        assertTrue(branch.contains("state.mainHandItem=visual;"));
        assertTrue(branch.contains("updateForLiving("));
        assertFalse(branch.contains("this.mainHandItem="));
        assertFalse(branch.contains("state.offHandItem="));
    }

    @Test void wheelScrollCancelsAndSuppressesAutoEatBeforeAnyRestart() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String mixins=read("src/main/resources/dds-tweak.mixins.json");
        String wheel=read("src/main/java/ddstweak/mixin/feature/silentautoeat/MouseHandlerScrollMixin.java");
        assertTrue(mixins.contains("feature.silentautoeat.MouseHandlerScrollMixin"));
        assertTrue(wheel.contains("@Mixin(MouseHandler.class)"));
        assertTrue(wheel.contains("@Inject(method=\"onScroll(JDD)V\",at=@At(\"HEAD\"))"));
        assertTrue(wheel.contains("SilentAutoEat.onManualHotbarScroll(Minecraft.getInstance(),vertical);"));
        assertFalse(wheel.contains("ci.cancel()"));
        assertFalse(wheel.contains("swapWithHotbar("));
        assertTrue(feature.contains("pendingHotbarScrolls.incrementAndGet();"));
        assertTrue(feature.contains("pendingHotbarScrolls.getAndSet(0)"));
        assertTrue(feature.contains("MinecraftCompat.screen(minecraft)!=null)return;"));
        int consume=feature.indexOf("private static boolean consumeManualHotbarScroll(");
        int next=feature.indexOf("public static void onExplicitPlayerAction(",consume);
        String handler=feature.substring(consume,next);
        int relinquish=handler.indexOf("mainhandSelectedByAuto=false;");
        int cancel=handler.indexOf("onExplicitPlayerAction(minecraft);",relinquish);
        assertTrue(consume>=0&&next>consume&&relinquish>=0&&cancel>relinquish);
        assertTrue(handler.contains("!Configs.Tweaks.AUTO_EAT.getBooleanValue()"));
        int start=feature.indexOf("private static void beforeTick(");
        int tick=feature.indexOf("private static void tick(",start);
        int end=feature.indexOf("private static boolean shouldStart(",tick);
        String pre=feature.substring(start,tick);
        String post=feature.substring(tick,end);
        assertTrue(pre.indexOf("consumeManualHotbarScroll(minecraft)") <
                pre.indexOf("if(state==State.IDLE"));
        assertTrue(post.indexOf("consumeManualHotbarScroll(minecraft)") <
                post.indexOf("if(state==State.IDLE"));
        assertTrue(feature.contains("overrideDeadlineTick=clientTicks+USER_OVERRIDE_TICKS;"));
        assertTrue(feature.contains("isWithinUserOverride(clientTicks,overrideDeadlineTick)"));
        int stop=feature.indexOf("private static void finishMainhand(");
        int stopEnd=feature.indexOf("private static boolean mainhandPath(",stop);
        String stopOwnedUse=feature.substring(stop,stopEnd);
        assertTrue(stopOwnedUse.contains("interrupted&&state==State.MAIN_HAND_USING&&owner.isUsingItem()"));
        assertTrue(stopOwnedUse.contains("owner.getUsedItemHand()==InteractionHand.MAIN_HAND"));
        assertTrue(stopOwnedUse.contains("minecraft.gameMode.releaseUsingItem(owner);"));
        assertFalse(stopOwnedUse.contains("interrupted&&mainhandSelectionOwned()"),
                "Cancel the old Auto Eat use even when vanilla wheel already changed selected slot");
        // Input observation is passive; normal inventory/selection remains vanilla-owned.
        assertFalse(wheel.contains("selectHotbar("));
        assertFalse(wheel.contains("releaseUsingItem("));
    }

    @Test void explicitHotbarSelectionWinsEvenWhenSlotDoesNotChange() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        int start=feature.indexOf("private static void beforeTick(");
        int tick=feature.indexOf("private static void tick(",start);
        int afterTick=feature.indexOf("private static boolean shouldStart(",tick);
        String preTick=feature.substring(start,tick);
        String endTick=feature.substring(tick,afterTick);
        assertTrue(preTick.contains("if(relinquishOnPlayerHotbarIntent(minecraft))return;"));
        assertTrue(endTick.contains("if(relinquishOnPlayerHotbarIntent(minecraft))return;"));
        int helper=feature.indexOf("private static boolean relinquishOnPlayerHotbarIntent(");
        int nextHelper=feature.indexOf("private static boolean hasExplicitHotbarIntent(",helper);
        String intent=feature.substring(helper,nextHelper);
        int check=intent.indexOf("hasExplicitHotbarIntent(minecraft)");
        int relinquish=intent.indexOf("mainhandSelectedByAuto=false;",check);
        int cancel=intent.indexOf("onExplicitPlayerAction(minecraft);",relinquish);
        assertTrue(helper>=0&&nextHelper>helper&&check>=0&&relinquish>check&&cancel>relinquish,
                "Player intent must relinquish selected-slot ownership before cancelling");
        assertTrue(feature.contains("minecraft.options.keyPickItem.isDown()"));
        assertTrue(feature.contains("minecraft.options.keyHotbarSlots[i].isDown()"));
        assertTrue(feature.contains("if(!(mainhandPath()||stagingSwapSent)||!hasExplicitHotbarIntent(minecraft))"));
        assertTrue(feature.contains("if(mainhandSelectedByAuto&&mainhandSelectionOwned())"));
        assertTrue(feature.contains("||hasExplicitConflict(minecraft)||hasExplicitHotbarIntent(minecraft))return false;"));
        assertTrue(feature.contains("||hasExplicitHotbarIntent(minecraft)){"));
        // No changed inventory ItemStack or slot-index guess is needed.
        assertFalse(preTick.contains("swapWithOffhand("));
        assertFalse(preTick.contains("swapWithHotbar("));
    }

    @Test void stagingUsesTwoOwnedSlotsAndNeverBlindlyRetriesSwaps() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String compat=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatCompat.java");
        String rules=read("src/main/java/ddstweak/feature/silentautoeat/AutoEatScratchSelection.java");
        String evidenceSource=read("src/main/java/ddstweak/feature/silentautoeat/StagingServerSlotEvidence.java");
        assertTrue(rules.contains("i!=selected&&isEmpty.test(i)"));
        assertTrue(rules.contains("i!=selected&&distinctFromFood.test(i)"));
        assertTrue(feature.contains("case STAGED_SWAP:stageToHotbar(minecraft);break;"));
        assertTrue(feature.contains("case STAGED_WAIT_ACK:waitForStagingAck(minecraft);break;"));
        assertTrue(feature.contains("case STAGED_RESTORE:restoreStagedHotbar(minecraft);break;"));
        assertTrue(feature.contains("!ItemStack.matches(owner.getOffhandItem(),originalOffhand)"));
        assertTrue(feature.contains("stagingSwapSent=true;"));
        assertTrue(feature.contains("stagingRestoreSent=true;"));
        assertTrue(feature.contains("stagedSourceOwned()"));
        assertTrue(feature.contains("hasStagingAck()"));
        assertTrue(feature.contains("stagedScratchRecoverable()"));
        assertTrue(feature.contains("hasConfirmedStageRestore()"));
        assertTrue(feature.contains("if(!stagingConfirmed){"));
        assertTrue(feature.contains("else{expireStagedTransaction();return;}"));
        assertTrue(feature.contains("if(!SilentAutoEatCompat.isSafeAutomationContext(minecraft,owner))"));
        assertTrue(feature.contains("stagingRestoreDeadline=clientTicks+RESTORE_GRACE_TICKS;"));
        assertTrue(feature.contains("if(state!=State.STAGED_RESTORE)"));
        assertTrue(feature.contains("restoreReadyTick=Math.max(restoreReadyTick,clientTicks+1);"));
        assertTrue(feature.contains("failedStagingOwner=owner;"));
        assertTrue(feature.contains("hasConfirmedStagingRecovery(player)"));
        assertTrue(feature.contains("index==sourceIndex||index==scratchIndex"));
        assertTrue(feature.contains("||state!=State.IDLE&&stagingSwapSent"));
        assertTrue(feature.contains("if(stagingSwapSent||state==State.STAGED_SWAP){"));
        assertTrue(feature.contains("requestStageRestore(minecraft,true);"));
        assertTrue(feature.contains("if(mainhandSelectedByAuto&&mainhandSelectionOwned())"));
        assertTrue(feature.contains("if(stagingSwapSent&&completed)completionObserved=true;"));
        assertTrue(feature.contains("stagingSwapSent&&(!stagedSourceOwned()||!stagedScratchRecoverable())"));
        assertTrue(feature.contains("if(stagingSwapSent&&serverSlotEvidence.hasConflict())return false;"));
        assertTrue(feature.contains("if(serverSlotEvidence.hasConflict()){"));
        assertTrue(feature.contains("expireStagedTransaction();return;"));
        // Preserve source-slot server corrections while permitting the food
        // stack to change count during vanilla consumption.
        assertTrue(feature.contains("serverSlotEvidence.retainSourceDuringConsumption();"));
        assertTrue(evidenceSource.contains("void retainSourceDuringConsumption(){"));
        assertTrue(compat.contains("ContainerInput.SWAP"));
        assertTrue(compat.contains("menu.containerId,sourceMenuSlot,scratchHotbar,ClickType.SWAP,player"));
        assertTrue(compat.contains("!MinecraftCompat.carried(menu,player).isEmpty()"));
        int stage=feature.indexOf("private static boolean preferSwapFreeHotbar(");
        int send=feature.indexOf("stagingSwapSent=true;",stage);
        int ack=feature.indexOf("if(hasStagingAck())",send);
        int select=feature.indexOf("state=State.MAIN_HAND_SELECT;",ack);
        assertTrue(stage>=0&&send>stage&&ack>send&&select>ack);
        String restoration=feature.substring(feature.indexOf("private static void restoreStagedHotbar("),
                feature.indexOf("private static void expireStagedTransaction("));
        // Every restore has a single sent flag, an observed-slot gate and
        // no code path that loops or sends another SWAP after its request.
        assertTrue(restoration.indexOf("if(stagingRestoreSent){") <
                restoration.indexOf("swapWithHotbar("));
        assertTrue(restoration.indexOf("if(!stagedScratchRecoverable())") <
                restoration.indexOf("swapWithHotbar("));
        assertEquals(1,restoration.split("swapWithHotbar\\(", -1).length-1);
        // The original offhand remains physically untouched by staging.
        String stageCode=feature.substring(stage,feature.indexOf("private static void selectMainhand(",stage));
        assertFalse(stageCode.contains("swapWithOffhand("));
    }

    @Test void serverSlotEvidenceIsPassiveAndCannotUnlockStaging() throws Exception {
        String mixins=read("src/main/resources/dds-tweak.mixins.json");
        String observer=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ClientPacketListenerSlotEvidenceMixin.java");
        String evidence=read("src/main/java/ddstweak/feature/silentautoeat/StagingServerSlotEvidence.java");
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        assertTrue(mixins.contains("feature.silentautoeat.ClientPacketListenerSlotEvidenceMixin"));
        assertTrue(observer.contains("@Inject(method=\"handleContainerSetSlot\",at=@At(\"TAIL\"))"));
        assertTrue(observer.contains("SilentAutoEat.observeServerSlot(packet.getContainerId(),packet.getSlot(),packet.getItem())"));
        assertTrue(observer.contains("@Inject(method=\"handleContainerContent\",at=@At(\"TAIL\"))"));
        assertTrue(observer.contains("SilentAutoEat.observeServerSnapshot(packet.containerId(),packet.items())"));
        assertTrue(observer.contains("SilentAutoEat.observeServerSnapshot(packet.getContainerId(),packet.getItems())"));
        assertFalse(observer.contains("ci.cancel()"));
        assertFalse(observer.contains("@Redirect("));
        assertTrue(evidence.contains("packetMenuId!=menuId"));
        assertTrue(evidence.contains("sourceSeen&&scratchSeen&&!conflicting"));
        assertTrue(feature.contains("serverSlotEvidence.observe(menuId,menuSlot,stack)"));
        assertTrue(feature.contains("serverSlotEvidence.observeSnapshot(menuId,stacks)"));
        assertTrue(feature.contains("serverSlotEvidence.arm(transactionContainerId,sourceMenuSlot,"));
        assertTrue(feature.contains("serverSlotEvidence.clear()"));
        assertTrue(feature.contains("STAGING_EVIDENCE_LOG.isDebugEnabled()"));
        assertTrue(feature.contains("both slots appeared in S2C updates (not a click ACK)"));
        int restore=feature.indexOf("private static void restoreStagedHotbar(");
        int snapshot=feature.indexOf("ItemStack expectedSource=",restore);
        int click=feature.indexOf("swapWithHotbar(",snapshot);
        int report=feature.indexOf("serverSlotEvidence.arm(",click);
        assertTrue(restore>=0&&snapshot>restore&&click>snapshot&&report>click,
                "Expected server-reported restored food must be captured before local click prediction");
        // A server packet does not identify a client click, and successful
        // predicted vanilla swaps may receive no per-slot correction packet.
        assertTrue(feature.contains("private static final boolean STAGED_SERVER_ACK_READY=false;"));
        assertTrue(feature.contains("&&!STAGED_SERVER_ACK_READY&&!experimental26StagingEnabled()"));
        // Paired observations may be used for diagnostics, but never in the
        // staging state-transition or restore SWAP authorization branches.
        int stage=feature.indexOf("private static boolean preferSwapFreeHotbar(");
        int stagesEnd=feature.indexOf("private static void selectMainhand(",stage);
        assertTrue(stage>=0&&stagesEnd>stage);
        assertFalse(feature.substring(stage,stagesEnd).contains("serverSlotEvidence.pairedMatchingUpdatesSeen()"));
        int restoreEnd=feature.indexOf("private static void expireStagedTransaction(",restore);
        assertTrue(restore>=0&&restoreEnd>restore);
        assertFalse(feature.substring(restore,restoreEnd).contains("serverSlotEvidence.pairedMatchingUpdatesSeen()"));
    }

    @Test void experimentalStagingIsHiddenDefaultOffAndSelectedSlotOnly() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(feature.contains("private static final boolean STAGED_SERVER_ACK_READY=false;"));
        assertTrue(feature.contains("private static boolean experimental26StagingEnabled(){"));
        String devGate=feature.substring(
                feature.indexOf("private static boolean experimental26StagingEnabled(){"),
                feature.indexOf("private static LocalPlayer restoredVisualOwner;"));
        assertTrue(devGate.contains("//#if MC == 260300"));
        assertTrue(devGate.contains("//#else"));
        assertTrue(devGate.contains("return false;"));
        assertTrue(feature.contains("Boolean.getBoolean(\"ddstweak.experimental.silentAutoEatStaging26\")"));
        assertTrue(feature.contains("&&!STAGED_SERVER_ACK_READY&&!experimental26StagingEnabled()"));
        assertTrue(feature.contains("!STAGED_SERVER_ACK_READY&&!experimental26StagingEnabled()"));
        assertTrue(feature.contains("&&!originalScratch.isEmpty()"));
        assertTrue(feature.contains("if(scratchIndex<0){clear();return;}"));
        assertFalse(configs.contains("EXPERIMENTAL_AUTO_EAT_STAGING"));
        assertFalse(feature.contains("STAGED_SERVER_ACK_READY=true"));
        assertFalse(feature.contains("ServerboundPingRequestPacket"));
    }

    @Test void settledStagingWatchesLateCorrectionsWithoutReplayingSwaps() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String evidence=read("src/main/java/ddstweak/feature/silentautoeat/StagingServerSlotEvidence.java");
        assertTrue(feature.contains("STAGED_SERVER_SETTLE_TICKS=40"));
        assertTrue(feature.contains("private static final StagingServerSlotEvidence<ItemStack> settledStageEvidence="));
        assertTrue(feature.contains("if(settledStageOwner!=null){"));
        assertTrue(feature.contains("settledStageEvidence.observe(menuId,menuSlot,stack);"));
        assertTrue(feature.contains("settledStageEvidence.observeSnapshot(menuId,stacks);"));
        assertTrue(feature.contains("if(settledStageEvidence.hasConflict())rejectSettledStage();"));
        assertTrue(feature.contains("if(!isSettledStageContextValid()||clientTicks>settledStageDeadline)"));
        assertTrue(feature.contains("else return false;"));
        assertTrue(feature.contains("clearSettledStage();"));
        assertTrue(feature.contains("clearHotbarPopVisual();"));
        int stage=feature.indexOf("private static void restoreStagedHotbar(");
        int guard=feature.indexOf("private static void observeSettledStage(",stage);
        int fail=feature.indexOf("private static void rejectSettledStage(",guard);
        int end=feature.indexOf("private static void clearSettledStage(",fail);
        assertTrue(stage>=0&&guard>stage&&fail>guard&&end>fail);
        assertTrue(feature.substring(stage,guard).contains("observeSettledStage();"));
        assertTrue(feature.substring(stage,guard).indexOf("observeSettledStage();")
                <feature.substring(stage,guard).lastIndexOf("clear();"));
        String failed=feature.substring(fail,end);
        assertTrue(failed.contains("failedStagingOwner=settledStageOwner;"));
        assertTrue(failed.contains("failedStagingStableSince=-1;"));
        assertFalse(failed.contains("swapWithHotbar("));
        assertFalse(failed.contains("swapWithOffhand("));
        assertTrue(feature.contains("&&minecraft.level==settledStageLevel"));
        assertTrue(feature.contains("&&settledStageOwner.containerMenu==settledStageOwner.inventoryMenu"));
        assertTrue(feature.contains("&&MinecraftCompat.screen(minecraft)==null"));
        assertTrue(feature.contains("&&clientTicks<=settledStageDeadline"));
        assertTrue(evidence.contains("packetMenuId!=menuId"));
        assertTrue(feature.contains("STAGED_SERVER_ACK_READY=false"));
    }

    @Test void stagingNeverSendsStatusPacketsIntoPlayProtocol() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String observer=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ClientPacketListenerSlotEvidenceMixin.java");
        assertTrue(feature.contains("private static final boolean STAGED_SERVER_ACK_READY=false;"));
        assertTrue(feature.contains("if(hasStagingAck())"));
        assertTrue(feature.contains("if(hasConfirmedStageRestore()){"));
        assertTrue(feature.contains("rememberStagedMainhandVisual();"));
        assertTrue(feature.contains("if(stagingSwapSent&&serverSlotEvidence.hasConflict())return false;"));
        assertTrue(feature.contains("if(serverSlotEvidence.hasConflict()){"));
        assertTrue(observer.contains("SilentAutoEat.observeServerInventorySlot(packet.slot(),packet.contents())"));
        assertTrue(feature.contains("if(inventoryIndex!=source&&inventoryIndex!=scratch)return;"));
        assertTrue(feature.contains("?transactionContainerId:settledStageMenuId,slot,stack)"));
        assertFalse(feature.contains("ServerboundPingRequestPacket"));
        assertFalse(feature.contains("stageRoundTrip"));
        assertFalse(observer.contains("handlePongResponse"));
        assertFalse(observer.contains("ClientboundPongResponsePacket"));
        assertFalse(observer.contains("ci.cancel()"));
        assertTrue(feature.contains("&&!STAGED_SERVER_ACK_READY&&!experimental26StagingEnabled()"));
    }

    @Test void initializationAndMixinsAreRegistered() throws Exception {
        String init=read("src/main/java/ddstweak/DDSTweakInitHandler.java");
        String mixins=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(init.contains("SilentAutoEat.initialize()"));
        assertTrue(mixins.contains("feature.silentautoeat.MinecraftInputMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.LocalPlayerNoSlowdownMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.ContainerScreenInputMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.ContainerScreenRenderMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.ItemInHandRendererMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.GuiMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.InventoryPreviewMixin"));
    }

    @Test void stateMachineUsesVanillaInventoryAndUsePaths() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String presentation=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        String compat=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatCompat.java");
        assertTrue(feature.contains("owner=minecraft.player;"));
        assertTrue(feature.contains("ownerLevel=minecraft.level;"));
        assertTrue(feature.indexOf("owner=minecraft.player;")<feature.indexOf("state=State.PREPARE;"));
        assertTrue(feature.contains("SWAP_TO_OFFHAND"));
        assertTrue(feature.contains("WAIT_SWAP"));
        assertTrue(feature.contains("START_USE"));
        assertTrue(feature.contains("USING"));
        assertTrue(feature.contains("RESTORE_OFFHAND"));
        assertTrue(feature.contains("swapWithOffhand"));
        assertTrue(compat.contains("ContainerInput.SWAP"));
        assertTrue(compat.contains("ClickType.SWAP"));
        assertTrue(compat.contains("handleContainerInput"));
        assertTrue(compat.contains("handleInventoryMouseClick"));
        assertTrue(compat.contains("isSafeInventoryContext("));
        assertTrue(compat.contains("screen.getClass()==InventoryScreen.class"));
        assertTrue(compat.contains("SlotContainerIndexAccessor"));
        assertTrue(compat.contains("menuSlot(LocalPlayer player,int inventoryIndex)"));
        assertTrue(compat.contains("inventoryIndex(LocalPlayer player,Slot slot)"));
        assertFalse(feature.contains("visualContainerSlot"));
        assertTrue(presentation.contains("projectContainerStack"));
        assertTrue(presentation.contains("ItemStack upstream"));
        String containerRender=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ContainerScreenRenderMixin.java");
        assertTrue(containerRender.contains("ddstweak$captureRenderSlot(Slot slot)"));
        assertTrue(containerRender.contains("ddstweak$projectRenderedStack(ItemStack upstream)"));
        assertTrue(containerRender.contains("ddstweak$projectExtractedSlotStack("));
        assertTrue(containerRender.contains("SilentAutoEatPresentation.projectContainerStack(player,slot,upstream)"));
        assertTrue(containerRender.contains("ddstweak$projectTooltipStack"));
        assertTrue(containerRender.contains("@ModifyExpressionValue("));
        assertTrue(containerRender.contains("method=\"extractTooltip\""));
        assertTrue(containerRender.contains("method=\"renderTooltip\""));
        assertTrue(containerRender.contains("//#if MC >= 12111"));
        assertFalse(containerRender.contains("method={\"renderSlot\",\"renderTooltip\",\"extractSlot\",\"extractTooltip\"}"));
        assertTrue(feature.contains("START_CLIENT_TICK"));
        assertTrue(feature.contains("keyUse.isDown()"));
        assertTrue(feature.contains("keyAttack.isDown()"));
        assertTrue(feature.contains("keySwapOffhand.isDown()"));
        assertTrue(feature.contains("SilentAutoEatCompat.isSafeAutomationContext"));
        assertTrue(feature.contains("transactionMenu"));
        assertTrue(feature.contains("sourceMenuSlot"));
        assertTrue(feature.contains("completionObserved"));
        // Restoration now uses an explicit ownership predicate with a bounded fail-closed path.
        // The truth table is covered by SilentAutoEatRulesTest#recoveryRejectsForeignOffhandState.
        assertTrue(feature.contains("canRestoreOwnedOffhand()"));
        assertTrue(feature.contains("SilentAutoEatRules.canRestoreOffhand("));
        assertTrue(feature.contains("expireUnrestorableTransaction()"));
        assertTrue(feature.contains("hasConfirmedSwap()"));
        assertTrue(feature.contains("hasConfirmedFailedRestore(minecraft,player)"));
        assertTrue(feature.contains("restoreRequestSent=true"));
        assertTrue(feature.contains("if(hasConfirmedRestore()){"));
        assertTrue(feature.contains("rememberRestoredMainhandVisual();"));
        assertTrue(feature.contains("clearFailedRestore()"));
        assertTrue(feature.contains("shouldGuardSlotClick("));
        assertTrue(feature.contains("failedRestoreStableSince"));
        assertTrue(feature.contains("FAILURE_RECOVERY_TICKS"));
        assertTrue(feature.contains("restoreDeadlineTick"));
        assertTrue(feature.contains("releaseUsingItem"));
        assertTrue(compat.contains("gameMode.useItem"));
        assertFalse(feature.contains("setFoodLevel"));
        assertFalse(feature.contains("setSaturation"));
        assertFalse(feature.contains("ServerboundSetCreativeModeSlotPacket"));
    }

    @Test void inventoryPreviewOnlyProjectsRenderStateForModernLocalPlayer() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/silentautoeat/InventoryPreviewMixin.java");
        String build=read("common.gradle");
        assertTrue(mixin.contains("//#if MC >= 12111"));
        assertTrue(build.contains("The preview adapter has an explicit preprocessed implementation"));
        assertFalse(build.contains("unavailable.add('feature.silentautoeat.InventoryPreviewMixin')"));
        assertTrue(build.contains("boolean previewExpected = true"));
        assertTrue(build.contains("incorrect inventory preview Mixin registration"));
        assertTrue(mixin.contains("entity!=player||!SilentAutoEat.isPresentationActive(player)"));
        assertTrue(mixin.contains("CallbackInfoReturnable<EntityRenderState>"));
        assertTrue(mixin.contains("ItemDisplayContext.THIRD_PERSON_RIGHT_HAND"));
        assertTrue(mixin.contains("ItemDisplayContext.THIRD_PERSON_LEFT_HAND"));
        assertTrue(mixin.contains("SilentAutoEatPresentation.projectMainhand"));
        assertTrue(mixin.contains("SilentAutoEatPresentation.projectOffhand"));
        assertFalse(mixin.contains("swapWithOffhand"));
        assertFalse(mixin.contains("setItem("));
    }

    @Test void modernContainerRenderingProjectsAtStackConsumerAndTooltipLocal() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ContainerScreenRenderMixin.java");
        String preprocessorMarker="/" + "/" + "$" + "$";
        String baseSource=java.util.Arrays.stream(mixin.split("\\R"))
                .filter(line->!line.stripLeading().startsWith(preprocessorMarker))
                .collect(java.util.stream.Collectors.joining(""));
        assertFalse(baseSource.contains("@ModifyArgs("));
        assertTrue(baseSource.contains("method=\"renderSlot\""));
        assertFalse(baseSource.contains("method=\"extractSlot\""));
        assertFalse(baseSource.contains("renderSlotContents(Lnet/minecraft/client/gui/GuiGraphics;"));
        assertTrue(baseSource.contains("ddstweak$captureRenderSlot(Slot slot)"));
        assertTrue(baseSource.contains("ddstweak$projectRenderedStack(ItemStack upstream)"));
        assertTrue(baseSource.contains("@Unique private Slot ddstweak$silentAutoEatRenderSlot"));
        assertTrue(baseSource.contains("at=@At(\"HEAD\")"));
        assertTrue(baseSource.contains("argsOnly=true"));
        assertTrue(mixin.contains(preprocessorMarker + "         method=\"extractSlot\","));
        assertTrue(mixin.contains(preprocessorMarker + "         at=@At(value=\"STORE\",ordinal=0)"));
        assertTrue(mixin.contains(preprocessorMarker + " private ItemStack ddstweak$projectExtractedSlotStack("));
        assertTrue(mixin.contains(preprocessorMarker + "         Slot slot, int mouseX, int mouseY)"));
        assertFalse(mixin.contains("renderSlotContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;"));
        assertTrue(baseSource.contains("@ModifyVariable("));
        assertTrue(baseSource.contains("method=\"renderTooltip\""));
        assertFalse(baseSource.contains("method=\"extractTooltip\""));
        assertTrue(baseSource.contains("at=@At(value=\"STORE\",ordinal=0)"));
        assertTrue(baseSource.contains("projectTooltipHasItem"));
        assertTrue(baseSource.contains("ddstweak$silentAutoEatRenderSlot"));
        assertFalse(baseSource.contains("@Redirect("));
    }

    @Test void modernHotbarOffhandProjectsResolvedStackLocal() throws Exception {
        String gui=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        String marker="/" + "/" + "$" + "$";
        String baseSource=java.util.Arrays.stream(gui.split("\\R"))
                .filter(line->!line.stripLeading().startsWith(marker))
                .collect(java.util.stream.Collectors.joining(""));
        assertTrue(baseSource.contains("@ModifyVariable("));
        assertTrue(baseSource.contains("method=\"renderItemHotbar\""));
        assertTrue(baseSource.contains("@Local Player player"));
        assertTrue(baseSource.contains("projectOffhand((LocalPlayer)player,upstream)"));
        assertFalse(baseSource.contains("ddstweak$silentAutoEatLegacyHotbar"));
        assertFalse(baseSource.contains("ddstweak$silentAutoEatLegacyItemHotbar"));
        assertTrue(gui.contains("//#elseif MC >= 260100"));
        assertTrue(gui.contains("//#elseif MC >= 260200"));
        assertTrue(gui.contains("//#if MC >= 260300"));
        // 26.3 adds a required source-stack getter alongside the two offhand hooks.
        assertEquals(5,gui.split(java.util.regex.Pattern.quote(
                marker + "         method=\"extractItemHotbar\","),-1).length);
        assertTrue(gui.contains(marker + "         method=\"extractItemHotbar\","));
        assertTrue(gui.contains(marker + " private ItemStack ddstweak$silentAutoEatLegacyHotbar"));
        assertTrue(gui.contains("projectHotbarInventoryStack"));
    }

    @Test void hotbarInventoryGetterProjectionRequiresLocalInventoryIdentity() throws Exception {
        String gui=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        // 26.3's strict wrapper and earlier composable wrappers must share this guard.
        assertEquals(1,gui.split(java.util.regex.Pattern.quote("projectHotbarInventoryStack(player,inventory,index,upstream)"),-1).length-1);
        assertTrue(gui.contains("SilentAutoEatPresentation.projectHudSource26(player,inventory,index,upstream)"));
        assertTrue(read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java")
                .contains("inventory==SilentAutoEatCompat.inventory(player)"));
    }

    @Test void everySupportedEraHasAnInventoryAvatarPreviewAdapter() throws Exception {
        String preview=read("src/main/java/ddstweak/mixin/feature/silentautoeat/InventoryPreviewMixin.java");
        String build=read("common.gradle");
        assertTrue(preview.contains("//#if MC >= 12111"));
        assertTrue(preview.contains("//#elseif MC >= 12104"));
        assertTrue(preview.contains("//#elseif MC == 12103"));
        assertTrue(preview.contains("@Mixin(ArmedEntityRenderState.class)"));
        assertTrue(preview.contains("@Mixin(PlayerRenderer.class)"));
        assertTrue(preview.contains("@Mixin(ItemInHandLayer.class)"));
        assertTrue(preview.contains("SilentAutoEatPresentation.projectOffhand"));
        assertTrue(preview.contains("SilentAutoEatPresentation.projectMainhand"));
        assertTrue(preview.contains("minecraft.screen instanceof InventoryScreen"));
        assertTrue(build.contains("boolean previewExpected = true"));
        assertTrue(build.contains("previewOffhandReads"));
        assertTrue(build.contains("previewPlayerExtract"));
        assertTrue(build.contains("previewHandFields"));
        assertTrue(build.contains("previewArmedExtract"));
        assertTrue(build.contains("previewLivingResolver"));
        assertTrue(build.contains("mcVersion == 1_21_04 ? 'Z' : ''"));
        assertTrue(preview.contains("state.rightHandItem=main"));
        assertTrue(preview.contains("state.leftHandItem=off"));
        assertFalse(preview.contains("state.rightHandStack="));
        assertFalse(preview.contains("state.leftHandStack="));
        assertTrue(preview.contains("//#if MC == 12104"));
        assertTrue(preview.contains("!mainRight,player"));
        assertTrue(preview.contains("mainRight,player"));
        assertTrue(preview.contains("ddstweak$projectLegacyPreviewMainhand"));
        assertTrue(preview.contains("ddstweak$projectLegacyPreviewOffhand"));
        assertTrue(preview.contains("ddstweak$projectPreviewHandModels"));
        assertTrue(preview.contains("ddstweak$projectLegacy12103PreviewHands"));
    }

    @Test void manualRightClickCannotExecuteAgainstTemporarySwappedItems() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String input=read("src/main/java/ddstweak/mixin/feature/silentautoeat/MinecraftInputMixin.java");
        String rules=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatRules.java");
        assertTrue(input.contains("@Inject(method=\"startUseItem\",at=@At(\"HEAD\"),cancellable=true)"));
        assertTrue(input.contains("if(SilentAutoEat.onExplicitUse((Minecraft)(Object)this))ci.cancel();"));
        int method=feature.indexOf("public static boolean onExplicitUse(");
        int cancel=feature.indexOf("onExplicitPlayerAction(minecraft);",method);
        int returnGate=feature.indexOf("return defer;",cancel);
        assertTrue(method>=0&&cancel>method&&returnGate>cancel);
        assertTrue(feature.substring(method,cancel).contains("shouldDeferExplicitUse(state!=State.IDLE,swapped,sourceIndex)"));
        assertTrue(rules.contains("active&&swapped&&sourceIndex>=0&&sourceIndex<36"));
        assertTrue(feature.contains("if(hasConfirmedRestore()){"));
        assertTrue(feature.contains("rememberRestoredMainhandVisual();"));
        assertTrue(feature.contains("restoreRequestSent=true;"));
    }

    @Test void interruptedRestoreWaitsForSafeMenuWithoutBlindSwap() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String restore=feature.substring(feature.indexOf("private static void restoreAndClear("),
                feature.indexOf("private static boolean hasConfirmedRestore()"));
        int ownership=restore.indexOf("if(!sourceStillOwnsOriginal()||!canRestoreOwnedOffhand())");
        int safeMenu=restore.indexOf("if(!SilentAutoEatCompat.isSafeAutomationContext(minecraft,owner))");
        int pausedDeadline=restore.indexOf("restoreDeadlineTick=clientTicks+RESTORE_GRACE_TICKS;",safeMenu);
        int menuResolution=restore.indexOf("AbstractContainerMenu menu=owner.containerMenu;",safeMenu);
        int swap=restore.indexOf("SilentAutoEatCompat.swapWithOffhand(",menuResolution);
        assertTrue(ownership>=0&&ownership<safeMenu);
        assertTrue(safeMenu<pausedDeadline&&pausedDeadline<menuResolution&&menuResolution<swap);
        int useRelease=restore.indexOf("SilentAutoEatRules.canSendRestoreSwap(",safeMenu);
        assertTrue(safeMenu<pausedDeadline&&pausedDeadline<useRelease&&useRelease<menuResolution,
                "An unsafe foreign menu must pause an unsent restore before the use-release timeout");
        // Only the unsafe-menu branch must not expire. Once safe, the
        // use-release and missing-slot guards must still fail closed.
        assertFalse(restore.substring(safeMenu,useRelease).contains("expireUnrestorableTransaction()"));
        assertTrue(restore.substring(useRelease,menuResolution).contains("expireUnrestorableTransaction()"));
        assertTrue(restore.contains("if(restoreRequestSent){"));
        assertTrue(restore.contains("if(hasConfirmedRestore()){"));
        assertTrue(restore.contains("rememberRestoredMainhandVisual();"));
        assertTrue(feature.contains("restoreReadyTick=clientTicks+1;"));
        assertTrue(feature.contains("if(!releasingUse)restoreAndClear(minecraft);"));
        assertTrue(feature.contains("failedRestoreOwner=owner;"));
        assertTrue(feature.contains("hasConfirmedFailedRestore(minecraft,player)"));
    }

    @Test void v263InterruptedRecoveryEmitsDeduplicatedDiagnostics() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        assertTrue(feature.contains("//#if MC == 260300"));
        assertTrue(feature.contains("diagnoseFailedRestore(minecraft,player)"));
        assertTrue(feature.contains("if(!snapshot.equals(recoveryDiagnostic))"));
        assertTrue(feature.contains("auto-eat restore expired: requestSent="));
        assertTrue(feature.contains("blocked auto-eat restart after interrupted restore:"));
    }

    @Test void legacy11404UsesGuardedVanillaPickupSequenceInsteadOfOffhandSwapButton() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatCompat.java");
        assertTrue(compat.contains("//#elseif MC == 11404"));
        assertTrue(compat.contains("if(inventoryIndex(player,menu.slots.get(i))==40)"));
        assertTrue(compat.contains("!MinecraftCompat.carried(menu,player).isEmpty()"));
        assertTrue(compat.contains("menu.containerId,menuSlot,0,ClickType.PICKUP,player)"));
        assertTrue(compat.contains("menu.containerId,offhandSlot,0,ClickType.PICKUP,player)"));
        assertTrue(compat.contains("menu.containerId,menuSlot,40,ClickType.SWAP,player"));
        assertFalse(compat.contains("inventory(player).setItem("));
    }
    @Test void legacy12001InventoryAvatarPreviewGetsOffhandProjection() throws Exception {
        String preview=read("src/main/java/ddstweak/mixin/feature/silentautoeat/InventoryPreviewMixin.java");
        String gradle=read("common.gradle");
        assertTrue(preview.contains("//#if MC == 12001"));
        assertTrue(preview.contains("@Mixin(ItemInHandLayer.class)"));
        assertTrue(preview.contains("ddstweak$projectLegacyPreviewOffhand"));
        assertTrue(preview.contains("ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V"));
        assertTrue(preview.contains("minecraft.screen instanceof InventoryScreen"));
        assertTrue(preview.contains("original.call(entity)"));
        assertTrue(preview.contains("SilentAutoEatPresentation.projectOffhand"));
        assertTrue(gradle.contains("legacy12001PreviewOffhandCalls"));
        assertTrue(gradle.contains("count { it.calls > 0 } != 1"));
        assertTrue(gradle.contains("missing/ambiguous avatar preview offhand render"));
    }

    @Test void legacyVersionsHaveSpecificRuntimeDiagnostics() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String render=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ContainerScreenRenderMixin.java");
        String presentation=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        assertTrue(feature.contains("ddstweak$traceLegacy11404Swap(\"ack-timeout\")"));
        assertTrue(feature.contains("sourceMatchesOriginal="));
        assertTrue(feature.contains("offhandMatchesFood="));
        assertTrue(render.contains("traceLegacy12001OffhandSlot"));
        assertTrue(presentation.contains("inventory offhand slot"));
        assertTrue(presentation.contains("playerOwned="));
        assertTrue(presentation.contains("menuContains="));
    }

    @Test void legacy12001InventoryProjectionUsesComposableSlotGetter() throws Exception {
        String render=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ContainerScreenRenderMixin.java");
        String gradle=read("common.gradle");
        assertTrue(render.contains("//#elseif MC == 12001"));
        assertTrue(render.contains("ddstweak$projectLegacy12001SlotStack"));
        assertTrue(render.contains("Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"));
        assertTrue(render.contains("ItemStack upstream=original.call(slot)"));
        assertTrue(render.contains("projectContainerStack(player,slot,upstream)"));
        assertTrue(gradle.contains("legacy12001RenderSlotGetItemCalls"));
        assertTrue(gradle.contains("renderSlot Slot#getItem target missing"));
    }

    @Test void legacy11404DiagnosticsReportTriggerGatesWithoutChangingPolicy() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        assertTrue(feature.contains("//#if MC == 11404"));
        assertTrue(feature.contains("diagnoseLegacy11404(minecraft)"));
        assertTrue(feature.contains("client tick callbacks registered"));
        assertTrue(feature.contains("hunger="));
        assertTrue(feature.contains("menuIsInventory="));
        assertTrue(feature.contains("eligibleSource="));
        assertTrue(feature.contains("snapshot.equals(legacy11404DiagnosticSnapshot)"));
    }

    @Test void legacyDiagnosticsAreDisabledAtNormalInfoLevel() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String projection=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        assertTrue(feature.contains("LEGACY_LOG.isDebugEnabled()"));
        assertTrue(feature.contains("LEGACY_LOG.debug("));
        assertFalse(feature.contains("LEGACY_LOG.info("));
        assertTrue(projection.contains("LEGACY_12001_LOG.isDebugEnabled()"));
        assertTrue(projection.contains("LEGACY_12001_LOG.debug("));
        assertFalse(projection.contains(".info(\"[1.20.1] inventory offhand slot"));
        // Real ownership failures must still retain an actionable warning in 26.3.
        assertTrue(feature.contains("RECOVERY_LOG.warn("));
    }

    @Test void legacyHotbarKeepsComposableOffhandFallback() throws Exception {
        String gui=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        assertFalse(gui.contains("silentAutoEatLegacyOffhandLocal"));
        assertFalse(gui.contains("//#elseif MC == 12001"));
        assertTrue(gui.contains("ddstweak$silentAutoEatLegacyHotbar"));
        assertTrue(gui.contains("ddstweak$silentAutoEatLegacyItemHotbar"));
        assertTrue(gui.contains("original.call(player)"));
    }

    @Test void vanillaAuditReportsHotbarAndHandFinalConsumerCandidates() throws Exception {
        String gradle=read("common.gradle");
        assertTrue(gradle.contains("def summarizeRenderMethod"));
        assertTrue(gradle.contains("hotbarRenderAudit: hotbarCandidates.collect"));
        assertTrue(gradle.contains("handRenderAudit: handCandidates.collect"));
        assertTrue(gradle.contains("nextStoreLocal: nextStoreLocal, nextStoreOrdinal: nextStoreOrdinal"));
        assertTrue(gradle.contains("Integer nextStoreOrdinal = null"));
        assertTrue(gradle.contains("storeInstructions << [instruction: i, local: insn.var]"));
        assertTrue(gradle.contains("nonNullListGet"));
        assertTrue(gradle.contains("getOffhandItem"));
        assertTrue(gradle.contains("itemStackRenderStateClear"));
    }

    @Test void legacyTooltipVanillaGetterTargetsCannotSilentlyDisappear() throws Exception {
        String gradle=read("common.gradle");
        assertTrue(gradle.contains("tasks.register('verifySilentAutoEatVanillaTargets')"));
        assertTrue(gradle.contains("report.legacyTooltips.size() != 1"));
        assertTrue(gradle.contains("report.legacyTooltips.first().getItemCalls != 1"));
        assertTrue(gradle.contains("report.legacyTooltips.first().hasItemCalls != 1"));
        assertTrue(gradle.contains("legacy renderTooltip Slot getters changed"));
        assertFalse(gradle.contains("logger.warn('Silent Auto Eat ' + project.name + ': no legacy tooltip target found"));
    }

    @Test void presentationOnlyHooksUseComposableOperations() throws Exception {
        for(String mixinName:new String[]{
                "ContainerScreenRenderMixin","GuiMixin","ItemInHandRendererMixin","InventoryPreviewMixin"}){
            String mixin=read("src/main/java/ddstweak/mixin/feature/silentautoeat/"+mixinName+".java");
            assertTrue(mixin.contains("@WrapOperation("),mixinName);
            assertFalse(mixin.contains("@Redirect("),mixinName);
            assertTrue(mixin.contains("original.call("),mixinName);
        }
        // The input redirect is deliberately not a visual presentation hook.
        String input=read("src/main/java/ddstweak/mixin/feature/silentautoeat/MinecraftInputMixin.java");
        assertTrue(input.contains("@Redirect("));
        assertTrue(input.contains("method=\"handleKeybinds\""));
        assertTrue(input.contains("releaseUsingItem"));
        String projection=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        assertTrue(projection.contains("isPresentedContainerSlot("));
    }

    @Test void renderLayerHidesOnlyLocalPresentation() throws Exception {
        String input=read("src/main/java/ddstweak/mixin/feature/silentautoeat/MinecraftInputMixin.java");
        String hand=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");
        String gui=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        String presentation=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        assertTrue(input.contains("method=\"handleKeybinds\""));
        assertTrue(input.contains("SilentAutoEat.ownsUse"));
        assertTrue(hand.contains("SilentAutoEatPresentation.projectOffhand"));
        assertTrue(hand.contains("SilentAutoEat.isPresentationActive"));
        assertTrue(hand.contains("renderArmWithItem"));
        assertTrue(hand.contains("FirstPersonHandsAndItems"));
        assertTrue(hand.contains("FirstPersonHandsAndItemsRenderState"));
        assertTrue(hand.contains("extractRenderState"));
        assertTrue(hand.contains("state.mainHandItem=main;"));
        assertTrue(hand.contains("state.offHandItem=off;"));
        assertTrue(hand.contains("state.mainHandRenderState,main"));
        assertTrue(hand.contains("state.offHandRenderState,off"));
        assertTrue(hand.contains("state.useItemRemainingTicks=0"));
        assertTrue(hand.contains("ddstweak$projectFirstPersonHands"));
        assertTrue(hand.contains("//#if MC >= 260300"));
        assertTrue(gui.contains("renderHotbar"));
        assertTrue(gui.contains("renderItemHotbar"));
        assertTrue(gui.contains("extractItemHotbar"));
        assertTrue(gui.contains("MC >= 260200"));
        assertTrue(gui.contains("SilentAutoEatPresentation.projectOffhand"));
        assertTrue(gui.contains("SilentAutoEatPresentation.projectHotbarInventoryStack"));
        assertTrue(presentation.contains("projectOffhand"));
        assertTrue(presentation.contains("projectInventoryStack"));
        assertTrue(presentation.contains("projectContainerStack"));
    }

    @Test void interruptionKeepsOwnedOffhandVisualUntilRestoreAcrossMenus() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String view=feature.substring(feature.indexOf("public static boolean isPresentationActive("),
                feature.indexOf("static int presentationSourceIndex()"));
        int pending=view.indexOf("if(state==State.RESTORE_OFFHAND)");
        int owned=view.indexOf("sourceStillOwnsOriginal(),canRestoreOwnedOffhand()");
        assertTrue(pending>=0&&owned>pending);
        assertFalse(view.substring(pending,owned).contains("isSafeAutomationContext"));
        assertTrue(view.contains("SilentAutoEatRules.shouldMaskPendingRestore("));
        assertTrue(view.contains("return SilentAutoEatCompat.isSafeAutomationContext("));

        String projection=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        assertTrue(projection.contains("!SilentAutoEatCompat.isSafeInventoryPresentation("));
        assertFalse(projection.contains("!SilentAutoEatCompat.isSafeAutomationContext("));
        assertTrue(projection.contains("projectContainerStack(LocalPlayer player,Slot slot,ItemStack upstream)"));
    }

    @Test void carriedItemAndQcaScreenKeepOnlyLocalPlayerSwapSlotsProjected() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatCompat.java");
        String render=compat.substring(compat.indexOf("static boolean isSafeInventoryPresentation("),
                compat.indexOf("static int menuSlot(",compat.indexOf("static boolean isSafeInventoryPresentation(")));
        assertTrue(render.contains("player.containerMenu!=null"));
        assertTrue(render.contains("screen(minecraft) instanceof AbstractContainerScreen"));
        assertTrue(render.contains("hasPlayerInventorySlots(player)"));
        assertFalse(render.contains("player.containerMenu==player.inventoryMenu"));
        assertFalse(render.contains("carried("));
        assertTrue(compat.contains("if(!MinecraftCompat.carried(player.containerMenu,player).isEmpty())return false;"));

        String presentation=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        assertEquals(2,presentation.split(java.util.regex.Pattern.quote("isSafeInventoryPresentation("),-1).length-1);
        assertTrue(presentation.contains("!player.containerMenu.slots.contains(slot)"));
        assertTrue(presentation.contains("inventoryIndex==40"));
        assertTrue(presentation.contains("isOwnedSourceIndex(inventoryIndex,SilentAutoEat.presentationSourceIndex())"));
        assertTrue(compat.contains("slot.container!=inventory(player)"));

        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String guard=feature.substring(feature.indexOf("public static boolean shouldGuardSlotClick("),
                feature.indexOf("public static void onExplicitPlayerAction("));
        assertTrue(guard.contains("owner.containerMenu.slots.contains(slot)"));
        assertTrue(guard.contains("SilentAutoEatCompat.inventoryIndex(owner,slot)"));
        assertTrue(guard.contains("SilentAutoEatRules.isTransactionSwapSlot(index,sourceIndex)"));
        assertFalse(guard.contains("owner.containerMenu!=owner.inventoryMenu"));

        // Cancel source/offhand input before QCA can act on the physically swapped stack.
        String input=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ContainerScreenInputMixin.java");
        String qca=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");
        assertTrue(input.contains("@Mixin(value=AbstractContainerScreen.class,priority=1100)"));
        assertTrue(input.contains("if(guard)cir.setReturnValue(true)"));
        assertTrue(qca.contains("@Mixin(AbstractContainerScreen.class)"));
    }

    @Test void modernInventoryAvatarUsesProjectedArmPoseInsteadOfInvisibleFoodUse() throws Exception {
        String preview=read("src/main/java/ddstweak/mixin/feature/silentautoeat/InventoryPreviewMixin.java");
        String modern=preview.substring(preview.indexOf("//#if MC >= 12111"),
                preview.indexOf("//#elseif MC >= 12104"));
        assertTrue(modern.contains("HumanoidModel.ArmPose.EMPTY"));
        assertTrue(modern.contains("HumanoidModel.ArmPose.ITEM"));
        assertTrue(modern.contains("state.rightArmPose=visualOffhand.isEmpty()"));
        assertTrue(modern.contains("state.leftArmPose=visualOffhand.isEmpty()"));
        // ArmedEntityRenderState has no isUsingItem field in 1.21.11+;
        // a projected preview must alter only hand pose and item-model state.
        assertFalse(modern.contains("state.isUsingItem"));
        assertTrue(modern.contains("state.rightArmPose=visualMain.isEmpty()"));
        assertTrue(modern.contains("state.leftArmPose=visualMain.isEmpty()"));
        assertTrue(modern.contains("entity!=player||!SilentAutoEat.isPresentationActive(player)"));
        assertFalse(modern.contains("player.getInventory().setItem("));
    }

    @Test void v12111FirstPersonRendersProjectedHandArgumentNotOnlyTickCache() throws Exception {
        String hand=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");
        String audit=read("common.gradle");
        int start=hand.indexOf("//#if MC == 12111",hand.indexOf("private void ddstweak$pinVisualOffhand("));
        int end=hand.indexOf("//#endif",start);
        assertTrue(start>=0&&end>start);
        String hook=hand.substring(start,end);
        assertTrue(hook.contains("@ModifyVariable("));
        assertTrue(hook.contains("method=\"renderArmWithItem\""));
        assertTrue(hook.contains("at=@At(\"HEAD\")"));
        assertTrue(hook.contains("argsOnly=true"));
        assertTrue(hook.contains("ordinal=0"));
        assertTrue(hook.contains("require=1"));
        assertTrue(hook.contains("@Local(argsOnly=true) AbstractClientPlayer entity"));
        assertTrue(hook.contains("@Local(argsOnly=true) InteractionHand hand"));
        assertTrue(hook.contains("hand==InteractionHand.MAIN_HAND"));
        assertTrue(hook.contains("SilentAutoEatPresentation.projectMainhand(player,upstream)"));
        assertTrue(hook.contains("SilentAutoEatPresentation.projectOffhand(player,upstream)"));
        assertFalse(hook.contains("this.mainHandItem="));
        assertFalse(hook.contains("this.offHandItem="));
        assertTrue(audit.contains("v12111RenderHandStackArgument"));
        // In 1.21.11 vanilla the ItemStack is followed by an equip-progress float.
        assertTrue(audit.contains("'Lnet/minecraft/world/item/ItemStack;F' +"));
        assertTrue(audit.contains("first-person ItemStack argument changed"));
    }

    @Test void v263ProjectsBothFirstPersonModelsWithoutContaminatingVanillaHandCaches() throws Exception {
        String hand=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");
        // Search directive boundaries independently of CRLF/LF checkout settings.
        // The pinning helper now has its own 26.3-only guard. Find the
        // extractRenderState injector itself, not the first preceding #if.
        int injector=hand.indexOf("//$$ @Inject(method=\"extractRenderState\"");
        int start=hand.lastIndexOf("//#if MC >= 260300",injector);
        int end=hand.indexOf("//#else",injector);
        assertTrue(injector>=0&&start>=0&&end>injector,
                "missing 26.3-specific extraction branch");
        String modern=hand.substring(injector,end);
        assertTrue(hand.contains("@Mixin(FirstPersonHandsAndItems.class)"));
        assertTrue(hand.contains("//#if MC < 260300"));
        assertTrue(hand.contains("this.offHandItem=SilentAutoEatPresentation.projectOffhand(player,player.getOffhandItem());"));
        assertTrue(hand.contains("this.mainHandItem=visualMainhand;"));
        assertTrue(modern.contains("@Inject(method=\"extractRenderState\",at=@At(\"TAIL\"),require=1)"));
        assertTrue(modern.contains("state.mainHandItem=main;"));
        assertTrue(modern.contains("state.offHandItem=off;"));
        assertTrue(modern.contains("||SilentAutoEat.isOwned26HudSwap(player);"));
        assertTrue(hand.contains("//$$ active|=SilentAutoEat.isOwned26HudSwap(player);"));
        assertTrue(modern.contains("state.mainHandRenderState,main"));
        assertTrue(modern.contains("state.offHandRenderState,off"));
        assertTrue(modern.contains("mainRight?ItemDisplayContext.FIRST_PERSON_LEFT_HAND:ItemDisplayContext.FIRST_PERSON_RIGHT_HAND"));
        assertTrue(modern.contains("state.useItemRemainingTicks=0"));
        assertTrue(modern.contains("if(!active&&!SilentAutoEat.isRestoredMainhandVisualActive(player))return;"));
        assertTrue(modern.contains("active?player.getMainHandItem():state.mainHandItem"));
        assertTrue(modern.contains("if(!active){"));
        assertTrue(modern.contains("if(ItemStack.matches(state.mainHandItem,main))return;"));
        assertFalse(modern.contains("this.mainHandItem="));
        assertFalse(modern.contains("this.offHandItem="));
        assertFalse(modern.contains("ddstweak$pinBeforeExtractingHands"));
    }

    @Test void v263HotbarSelectionRendersActualSourceFoodNotSwappedOriginalOffhand() throws Exception {
        String gui=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        String audit=read("common.gradle");
        assertTrue(gui.contains("//#if MC >= 260300"));
        assertTrue(gui.contains("method=\"extractItemHotbar\""));
        assertTrue(gui.contains("require=1"));
        assertTrue(gui.contains("SilentAutoEatPresentation.projectHudSource26(player,inventory,index,upstream)"));
        assertTrue(read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java")
                .contains("inventory==SilentAutoEatCompat.inventory(player)"));
        assertTrue(audit.contains("v263HotbarInventoryReads"));
        assertTrue(audit.contains("v263HandStateFields"));
        assertTrue(audit.contains("v263Resolver"));
    }

    @Test void selectedMainhandFoodIsEligibleAndVisuallyPreserved() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String rules=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatRules.java");
        String projection=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        String hand=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");
        assertTrue(feature.contains("AutoEatExecutionPlanner.selectFoodSource("));
        assertFalse(feature.contains("i!=selected&&SilentAutoEatCompat.menuSlot"));
        assertTrue(feature.contains("selectedMainhandSource=false"));
        assertTrue(feature.contains("sourceSelectionConflicts(inventory)"));
        assertTrue(rules.contains("return selected>=0&&selected<9&&eligible.test(selected)"));
        assertTrue(projection.contains("projectMainhand(LocalPlayer player,ItemStack upstream)"));
        assertTrue(hand.contains("SilentAutoEatPresentation.projectMainhand"));
    }

    @Test void ownedOffhandSwapMasksOnlyHudPopNotRealInventory() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String hud=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        assertTrue(hud.contains("method=\"renderSlot\""));
        assertTrue(hud.contains("//$$ method=\"extractSlot\""));
        assertTrue(hud.contains("getPopTime()I"));
        assertTrue(hud.contains("require=1"));
        assertTrue(hud.contains("private int ddstweak$silentAutoEatHideOwnedHotbarPop("));
        assertTrue(hud.contains("@Local(argsOnly=true,ordinal=2) int seed"));
        assertTrue(hud.contains("int slot=seed==10?40:seed-1;"));
        assertTrue(hud.contains("SilentAutoEat.shouldSuppressHotbarPop((LocalPlayer)player,slot,stack)"));
        assertTrue(hud.contains("return original.call(stack);"));
        // 26.3 may clear popTime on a display-only snapshot, never the real stack.
        assertTrue(hud.contains("ItemStack renderOnly=upstream.copy();"));
        assertTrue(hud.contains("renderOnly.setPopTime(0);"));
        assertFalse(hud.contains("upstream.setPopTime("));
        assertFalse(hud.contains("ci.cancel()"));

        int begin=feature.indexOf("public static boolean shouldSuppressHotbarPop(");
        int end=feature.indexOf("public static boolean isRestoredMainhandVisualActive(",begin);
        String mask=feature.substring(begin,end);
        assertTrue(mask.contains("player!=Minecraft.getInstance().player"));
        assertTrue(mask.contains("(index!=40&&(index<0||index>=9))"));
        assertTrue(mask.contains("swapped&&sourceIndex>=0&&sourceIndex<9&&!originalOffhand.isEmpty()"));
        assertTrue(mask.contains("ownerLevel==Minecraft.getInstance().level"));
        assertTrue(mask.contains("index==sourceIndex"));
        assertTrue(mask.contains("ItemStack.matches(rendered,originalOffhand)"));
        assertTrue(mask.contains("isRestoredFoodOrRemainder(rendered,selectedFood,completionObserved)"));
        assertTrue(mask.contains("state==State.RESTORE_OFFHAND&&restoreRequestSent"));
        assertTrue(mask.contains("hasConfirmedRestore()"));
        assertTrue(mask.contains("state!=State.IDLE||hotbarPopOwner!=player"));
        assertTrue(mask.contains("clientTicks>hotbarPopUntilTick"));
        assertTrue(mask.contains("index==hotbarPopSource"));

        int restore=feature.indexOf("private static void restoreAndClear(");
        int restoreEnd=feature.indexOf("private static boolean hasConfirmedRestore()",restore);
        String restoreMethod=feature.substring(restore,restoreEnd);
        int confirm=restoreMethod.indexOf("if(hasConfirmedRestore())");
        int remember=restoreMethod.indexOf("rememberHotbarPopVisual();",confirm);
        int clear=restoreMethod.indexOf("clear();",remember);
        assertTrue(confirm>=0&&remember>confirm&&clear>remember);
        assertTrue(feature.contains("hotbarPopUntilTick=clientTicks+7;"));
        assertTrue(feature.contains("if(owner==null||sourceIndex<0||sourceIndex>=9||originalOffhand.isEmpty())return;"));
        assertTrue(feature.contains("clearHotbarPopVisual();"));
        assertFalse(feature.contains(".setPopTime("));
        // The offhand SWAP and the server-owned inventory contents stay unchanged.
        assertTrue(feature.contains("SilentAutoEatCompat.swapWithOffhand(minecraft,owner,menu,menuSlot);"));
    }

    @Test void v263ExtractedHudUsesCloneWithoutChangingRealPopTime() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String hud=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        int begin=hud.indexOf("//#if MC >= 260300",hud.indexOf("ddstweak$silentAutoEatLegacySourceSlot"));
        int end=hud.indexOf("//#endif",begin);
        String modern=hud.substring(begin,end);
        assertTrue(modern.contains("//$$ @ModifyVariable("));
        assertTrue(modern.contains("//$$         method=\"extractSlot\","));
        assertTrue(modern.contains("//$$         at=@At(\"HEAD\"),"));
        assertTrue(modern.contains("//$$         argsOnly=true,"));
        assertTrue(modern.contains("//$$         ordinal=0,"));
        assertTrue(modern.contains("//$$         require=1"));
        assertTrue(modern.contains("ddstweak$silentAutoEatHideExtractedHotbarPop("));
        assertTrue(modern.contains("@Local(argsOnly=true,ordinal=2) int seed"));
        assertTrue(modern.contains("int index=seed==10?40:seed-1;"));
        assertTrue(modern.contains("SilentAutoEat.shouldSuppressExtractedHotbarPop((LocalPlayer)player,index)"));
        assertTrue(modern.contains("ItemStack renderOnly=upstream.copy();"));
        assertTrue(modern.contains("renderOnly.setPopTime(0);"));
        assertTrue(modern.contains("return renderOnly;"));
        assertFalse(modern.contains("upstream.setPopTime("));
        assertFalse(modern.contains("original.call("));

        int scope=feature.indexOf("public static boolean shouldSuppressExtractedHotbarPop(");
        int scopeEnd=feature.indexOf("public static boolean isRestoredMainhandVisualActive(",scope);
        String predicate=feature.substring(scope,scopeEnd);
        assertTrue(scope>=0&&scopeEnd>scope);
        assertTrue(predicate.contains("player!=Minecraft.getInstance().player"));
        assertTrue(predicate.contains("isOwned26HudSwap(player)"));
        assertTrue(predicate.contains("isOwned26HudRestore(player,index)"));
        assertTrue(predicate.contains("index==40||sourceIndex<9&&index==sourceIndex"));
        assertTrue(predicate.contains("isOwned26HudSwap(player)"));
        assertTrue(feature.contains("ItemStack.matches(source,originalOffhand)"));
        assertTrue(feature.contains("ItemStack.matches(offhand,originalOffhand)"));
        assertTrue(feature.contains("hotbarPopOwner!=player"));
        assertTrue(feature.contains("hotbarPopLevel!=Minecraft.getInstance().level"));
        assertTrue(feature.contains("ItemStack.matches(player.getOffhandItem(),hotbarPopOriginalOffhand)"));
        assertTrue(feature.contains("isRestoredFoodOrRemainder("));
        assertTrue(feature.contains("hotbarPopUntilTick=clientTicks+12;"));
        assertTrue(feature.contains("        hotbarPopUntilTick=clientTicks+7;"));
        assertTrue(feature.contains("//$$ hotbarPopUntilTick=clientTicks+12;"));
        assertFalse(feature.contains(".setPopTime("));
        // The 1.21.11 getPopTime wrapper remains unchanged.
        assertTrue(hud.contains("method=\"renderSlot\""));
        assertTrue(hud.contains("private int ddstweak$silentAutoEatHideOwnedHotbarPop("));
    }

    @Test void v263OffhandEmptyHudSpriteIsHiddenBeforeVanillaVisibilityCheck() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String hud=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        String presentation=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");

        int newBlock=hud.indexOf("//#if MC >= 260300");
        int oldVersion=hud.indexOf("//#elseif MC >= 260200",newBlock);
        String version=hud.substring(newBlock,oldVersion);
        assertTrue(version.contains("@WrapOperation("));
        assertTrue(version.contains("method=\"extractItemHotbar\""));
        assertTrue(version.contains("Player;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"));
        assertTrue(version.contains("require=1"));
        assertTrue(version.contains("original.call(player)"));
        assertTrue(version.contains("projectHudOffhand26((LocalPlayer)player,upstream)"));
        assertFalse(version.contains("at=@At(value=\"STORE\""));
        assertTrue(hud.contains("SilentAutoEatPresentation.projectHudSource26(player,inventory,index,upstream)"));
        assertTrue(presentation.contains("if(SilentAutoEat.isOwned26HudSwap(player)"));
        assertTrue(presentation.contains("return projectOffhand(player,upstream);"));
        assertTrue(presentation.contains("SilentAutoEat.presentationOriginalOffhand():upstream"));
        assertTrue(presentation.contains("return SilentAutoEat.hudFood26(player);"));
        assertTrue(presentation.contains("index==SilentAutoEat.presentationSourceIndex()"));
        assertTrue(feature.contains("!swapped||state==State.IDLE||sourceIndex<0||sourceIndex>=36"));
        assertTrue(feature.contains("ItemStack.matches(source,originalOffhand)"));
        assertTrue(feature.contains("isRestoredFoodOrRemainder(offhand,selectedFood,completed)"));
        assertTrue(feature.contains("isRestoredFoodOrRemainder(source,selectedFood,completed)"));
        // Initially empty offhand is an allowed owned transaction, not a reason
        // to skip 26.3 HUD protection.
        assertFalse(feature.contains("&&sourceIndex<9\n    //$$             &&!originalOffhand.isEmpty()"));
        assertTrue(feature.contains("//$$ if(owner==null||sourceIndex<0||sourceIndex>=9)return;"));
        assertTrue(feature.contains("if(owner==null||sourceIndex<0||sourceIndex>=9||originalOffhand.isEmpty())return;"));
        // The old version's known-good render path remains separate.
        assertTrue(hud.contains("//#elseif MC >= 260200"));
        assertTrue(hud.contains("method=\"renderItemHotbar\""));
        assertTrue(hud.contains("method=\"renderSlot\""));
        assertTrue(feature.contains("STAGED_SERVER_ACK_READY=false"));
    }

    @Test void v263InventorySourceMasksLastBiteWhileRestoreCacheRemainsHotbarOnly() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String view=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        String hud=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        String hand=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");

        int ownerStart=feature.indexOf("public static boolean isOwned26HudSwap(LocalPlayer player)");
        int ownerEnd=feature.indexOf("public static boolean isOwned26HudRestore(",ownerStart);
        assertTrue(ownerStart>=0&&ownerEnd>ownerStart);
        String owner=feature.substring(ownerStart,ownerEnd);
        // Inventory sources 9..35 may still own the live food/offhand layout,
        // including the last-bite frame before the tick observer runs.
        assertTrue(owner.contains("sourceIndex>=36"));
        assertTrue(owner.contains("boolean finishingUse=state==State.USING"));
        assertTrue(owner.contains("isRestoredFoodOrRemainder(offhand,selectedFood,completed)"));
        assertTrue(feature.contains("return index==40||sourceIndex<9&&index==sourceIndex;"));
        // Do not extend the settled pop cache to backpack slots.
        assertTrue(feature.contains("hotbarPopSource<0||hotbarPopSource>=9"));
        assertTrue(feature.contains("//$$ if(owner==null||sourceIndex<0||sourceIndex>=9)return;"));
        assertTrue(feature.contains("if(owner==null||sourceIndex<0||sourceIndex>=9||originalOffhand.isEmpty())return;"));
        assertTrue(view.contains("return projectOffhand(player,upstream);"));
        assertTrue(hud.contains("projectHudOffhand26((LocalPlayer)player,upstream)"));

        int injector=hand.indexOf("//$$ @Inject(method=\"extractRenderState\"");
        int end=hand.indexOf("//#else",injector);
        assertTrue(injector>=0&&end>injector);
        String modern=hand.substring(injector,end);
        assertTrue(modern.contains("||SilentAutoEat.isOwned26HudSwap(player);"));
        assertTrue(modern.contains("state.offHandHeight=1.0F;"));
        assertTrue(modern.contains("state.oldOffHandHeight=1.0F;"));
        assertFalse(modern.contains("this.offHandItem="));
        assertTrue(feature.contains("SilentAutoEatCompat.swapWithOffhand(minecraft,owner,menu,menuSlot);"));
    }

    @Test void v263StagedMainhandKeepsSelectedSlotAndRendersOriginalWithoutEquipDip() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String view=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        String hand=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");
        String scratch=read("src/main/java/ddstweak/feature/silentautoeat/AutoEatScratchSelection.java");
        assertTrue(scratch.contains("static int selected(int selected,IntPredicate distinctFromFood)"));
        assertTrue(feature.contains("?AutoEatScratchSelection.selected(selected,"));
        assertTrue(feature.contains("mainhandOriginalSelected=selected;"));
        assertTrue(feature.contains("mainhandAutoSelected=scratchIndex;"));
        assertTrue(feature.contains("mainhandSelectedByAuto=mainhandAutoSelected!=mainhandOriginalSelected;"));
        assertTrue(feature.contains("if(mainhandSelectedByAuto)"));
        assertTrue(feature.contains("stagingRestoreSent&&hasConfirmedStageRestore()"));
        assertTrue(feature.contains("rememberStagedMainhandVisual();"));
        assertTrue(feature.contains("stagedVisualDeadline=clientTicks+HAND_CACHE_SETTLE_TICKS;"));
        assertTrue(feature.contains("selected==scratchIndex"));
        assertTrue(view.contains("if(SilentAutoEat.isStagedMainhandVisualActive(player))"));
        assertTrue(view.contains("return SilentAutoEat.stagedMainhandVisual(player);"));
        assertTrue(view.contains("index==SilentAutoEat.stagedMainhandSlot(player)"));
        assertTrue(hand.contains("if(player!=null&&SilentAutoEat.isStagedMainhandVisualActive(player))"));
        assertTrue(hand.contains("state.mainHandHeight=1.0F;"));
        assertTrue(hand.contains("state.oldMainHandHeight=1.0F;"));
        assertTrue(hand.contains("state.useItemRemainingTicks=0;"));
        assertTrue(hand.contains("state.mainHandRenderState,visual"));
        assertTrue(feature.contains("private static final boolean STAGED_SERVER_ACK_READY=false;"));
        assertFalse(feature.contains("STAGED_SERVER_ACK_READY=true"));
    }

    @Test void restoredSelectedMainhandDoesNotFlashPreviousOffhandCache() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String presentation=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        String hand=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");
        String rules=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatRules.java");
        String restore=feature.substring(feature.indexOf("private static void restoreAndClear("),
                feature.indexOf("private static boolean hasConfirmedRestore()"));
        int ack=restore.indexOf("if(hasConfirmedRestore())");
        int remember=restore.indexOf("rememberRestoredMainhandVisual();",ack);
        int clear=restore.indexOf("clear();",remember);
        assertTrue(ack>=0&&remember>ack&&clear>remember);
        assertTrue(feature.contains("private static final int HAND_CACHE_SETTLE_TICKS=10;"));
        assertTrue(feature.contains("clientTicks<=restoredVisualUntilTick"));
        assertTrue(feature.contains("selected==restoredVisualSource"));
        assertTrue(feature.contains("restoredVisualOwner!=player"));
        assertTrue(feature.contains("restoredVisualLevel!=Minecraft.getInstance().level"));
        assertTrue(feature.contains("SilentAutoEatFoodRules.sameStackIgnoringCount(actual,restoredVisualFood)"));
        assertTrue(feature.contains("&&sourceIndex>=0&&sourceIndex<9&&selected==sourceIndex"));
        assertTrue(feature.contains("return hasConfirmedRestore();"));
        assertTrue(feature.contains("?originalOffhand:restoredVisualOriginalOffhand"));
        assertTrue(feature.contains("ItemStack.matches(upstream,oldOffhand)"));
        assertTrue(feature.contains("!ItemStack.matches(upstream,player.getMainHandItem())"));
        assertTrue(rules.contains("keepRestoredMainhandVisual("));
        assertTrue(presentation.contains("SilentAutoEat.isStaleRestoredMainhandStack(player,upstream)"));
        assertTrue(presentation.contains("?player.getMainHandItem():upstream"));
        assertTrue(feature.contains("public static boolean isRestoredMainhandVisualActive(LocalPlayer player)"));
        assertTrue(hand.contains("SilentAutoEat.isRestoredMainhandVisualActive(player)"));
        assertTrue(hand.contains("this.oMainHandHeight=1.0F;"));
        // 26.3 must not rewrite persistent vanilla hand caches.
        assertFalse(hand.contains("//$$ this.mainHandItem=main;"));
        assertFalse(hand.contains("//$$ this.offHandItem=off;"));
    }

    @Test void switchingToFoodSourceDuringEatProjectsFoodUntilOwnedRestore() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String presentation=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        String selector=feature.substring(feature.indexOf("static boolean presentationUsesSelectedMainhand("),
                feature.indexOf("static ItemStack presentationOriginalOffhand()"));
        assertTrue(selector.contains("SilentAutoEatCompat.selectedSlot(SilentAutoEatCompat.inventory(player))"));
        assertTrue(selector.contains("SilentAutoEatRules.isOwnedSourceIndex(selected,sourceIndex)"));
        assertFalse(selector.contains("return selectedMainhandSource;"));
        assertTrue(presentation.contains("SilentAutoEat.presentationUsesSelectedMainhand(player)"));
        assertFalse(presentation.contains("SilentAutoEat.presentationSelectedFood()"));
        // Mainhand, hotbar source and container source must all reflect the
        // actual in-flight food count or vanilla remainder, not the initial copy.
        assertTrue(presentation.split(java.util.regex.Pattern.quote("player.getOffhandItem()"),-1).length>=4);
        // Initial selection is retained for conflict detection: no transaction semantics change.
        assertTrue(feature.contains("selectedMainhandSource=candidate==SilentAutoEatCompat.selectedSlot(inventory)"));
        assertTrue(feature.contains("sourceIndex,selectedMainhandSource,SilentAutoEatCompat.selectedSlot(inventory)"));
        assertTrue(feature.contains("sourceSelectionConflicts(inventory)"));
    }

    @Test void containerScrollMixinOnlyTargetsDeclaredOverride() throws Exception {
        String input=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ContainerScreenInputMixin.java");
        int gate=input.indexOf("//#if MC >= 12102");
        int scroll=input.indexOf("@Inject(method=\"mouseScrolled\"",gate);
        int end=input.indexOf("//#endif",gate);
        assertTrue(gate>=0&&scroll>gate&&end>scroll);
        assertFalse(input.contains("//#if MC >= 12002"));
        assertFalse(input.contains("double amount,CallbackInfoReturnable<Boolean>"));
        assertTrue(input.contains("ddstweak$silentAutoEatContainerClick"));
        assertTrue(input.contains("ddstweak$guardSourceClick"));
        assertTrue(input.contains("if(guard)cir.setReturnValue(true)"));
        assertTrue(input.contains("ddstweak$silentAutoEatContainerKey"));
    }

    @Test void noSlowdownOnlyChangesOwnedMovementInput() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String mixin=read("src/main/java/ddstweak/mixin/feature/silentautoeat/LocalPlayerNoSlowdownMixin.java");
        assertTrue(feature.contains("AUTO_EAT_NO_SLOWDOWN"));
        assertTrue(feature.contains("ownsUse(player)"));
        assertTrue(mixin.contains("method=\"itemUseSpeedMultiplier\""));
        assertTrue(mixin.contains("method=\"isSlowDueToUsingItem\""));
        assertTrue(mixin.contains("method=\"modifyInput\""));
        assertTrue(mixin.contains("method=\"aiStep\""));
        for(String lang:new String[]{"en_us","zh_cn","zh_tw"}){
            String json=read("src/main/resources/assets/dds-tweak/lang/"+lang+".json");
            for(String field:new String[]{"name","prettyName","comment"})
                assertTrue(json.contains("dds-tweak.config.tweaks."+field+".autoEatNoSlowdown"));
        }
    }

    @Test void versionDifferencesStayInCompat() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatCompat.java");
        assertTrue(compat.contains("//#if MC >= 12105"));
        assertTrue(compat.contains("//#if MC >= 12102"));
        assertTrue(compat.contains("DataComponents.CONSUMABLE"));
        assertTrue(compat.contains("//#elseif MC >= 12005"));
        assertTrue(compat.contains("//#if MC >= 11903"));
        assertTrue(compat.contains("//#if MC >= 11900"));
        assertTrue(compat.contains("//#if MC >= 260100"));
        assertFalse(compat.contains("//$ "));
    }
}
