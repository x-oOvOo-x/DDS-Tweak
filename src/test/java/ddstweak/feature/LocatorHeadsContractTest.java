package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Contracts for a client-only, optional HUD replacement: no fake-player/packet dependency. */
final class LocatorHeadsContractTest {
    private static String file(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/ddstweak/").resolve(name));
    }

    /** Match the missing-alpha guard without depending on trace statements or line endings. */
    private static void assertSkipsMissingAlpha(String source){
        assertTrue(java.util.regex.Pattern.compile(
                "if\\s*\\(alpha\\s*==\\s*null\\)\\s*\\{[^}]*return\\s+null\\s*;")
                .matcher(source).find());
    }

    @Test void headSizeUsesFineGrainedPercentages() {
        assertEquals(0.45f,ddstweak.feature.locatorheads.LocatorHeadsFeature.headScale(50),0.001f);
        assertEquals(0.81f,ddstweak.feature.locatorheads.LocatorHeadsFeature.headScale(90),0.001f);
        assertEquals(0.9f,ddstweak.feature.locatorheads.LocatorHeadsFeature.headScale(100),0.001f);
        assertEquals(0.909f,ddstweak.feature.locatorheads.LocatorHeadsFeature.headScale(101),0.001f);
        assertEquals(1.35f,ddstweak.feature.locatorheads.LocatorHeadsFeature.headScale(150),0.001f);
    }

    @Test void nameFiltersMatchWholeNamesCaseInsensitively() {
        var names=java.util.List.of(" Alice, BOB ", " Carol;Dave ", "eve:Frank.Gina");
        assertTrue(ddstweak.feature.locatorheads.LocatorHeadsFeature.matches(names,"alice"));
        assertTrue(ddstweak.feature.locatorheads.LocatorHeadsFeature.matches(names,"bob"));
        assertTrue(ddstweak.feature.locatorheads.LocatorHeadsFeature.matches(names,"gina"));
        assertFalse(ddstweak.feature.locatorheads.LocatorHeadsFeature.matches(names,"ali"));
        assertFalse(ddstweak.feature.locatorheads.LocatorHeadsFeature.matches(names,"nobody"));
        assertFalse(ddstweak.feature.locatorheads.LocatorHeadsFeature.matches(names,null));
        assertFalse(ddstweak.feature.locatorheads.LocatorHeadsFeature.matches(null,"alice"));
    }

    @Test void compassAnglesWrapAcrossNorth() {
        assertEquals(20f,ddstweak.feature.locatorheads.LocatorHeadsFeature.relativeAngle(350f,10f),0.001f);
        assertEquals(-20f,ddstweak.feature.locatorheads.LocatorHeadsFeature.relativeAngle(10f,350f),0.001f);
        assertEquals(90f,ddstweak.feature.locatorheads.LocatorHeadsFeature.relativeAngle(0f,90f),0.001f);
        assertEquals(-90f,ddstweak.feature.locatorheads.LocatorHeadsFeature.relativeAngle(0f,270f),0.001f);
    }

    @Test void fullyTransparentSkinProducesNoOutline() {
        var face=new boolean[64];
        var hat=new boolean[64];
        var mask=ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,true,1f);
        assertFalse(ddstweak.feature.locatorheads.LocatorContourMask.hasAlpha(mask));
        hat[0]=true;
        var visible=ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,true,1f);
        assertTrue(ddstweak.feature.locatorheads.LocatorContourMask.hasAlpha(visible));
        assertFalse(ddstweak.feature.locatorheads.LocatorContourMask.hasAlpha(
                ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,false,1f)));
    }

    @Test void nameFadeReversalPreservesOpacityAndCanBeCleared() {
        var id=java.util.UUID.randomUUID();
        long t=1_000_000_000L;
        ddstweak.feature.locatorheads.LocatorNameFades.clear();
        assertEquals(0f,ddstweak.feature.locatorheads.LocatorNameFades.ease(id,true,t),0.001f);
        assertEquals(0.875f,ddstweak.feature.locatorheads.LocatorNameFades.ease(id,true,t+75_000_000L),0.001f);
        assertEquals(0.875f,ddstweak.feature.locatorheads.LocatorNameFades.ease(id,false,t+75_000_000L),0.001f);
        assertEquals(0f,ddstweak.feature.locatorheads.LocatorNameFades.ease(id,false,t+150_000_000L),0.001f);
        assertEquals(0f,ddstweak.feature.locatorheads.LocatorNameFades.ease(id,false,t+300_000_000L),0.001f);
        ddstweak.feature.locatorheads.LocatorNameFades.clear();
        assertEquals(0f,ddstweak.feature.locatorheads.LocatorNameFades.ease(id,false,t+450_000_000L),0.001f);
    }

    @Test void nameFadeLifecycleFollowsSessionEvenWhenHudIsDisabled() throws Exception {
        String fades=file("feature/locatorheads/LocatorNameFades.java");
        String entry=file("DDSTweak.java");
        String hud=file("mixin/feature/locatorheads/LocatorBarMixin.java");
        assertTrue(entry.contains("LocatorNameFades.initialize();"));
        assertTrue(fades.contains("ClientTickEvents.END_CLIENT_TICK.register(LocatorNameFades::refresh)"));
        assertTrue(fades.contains("connection!=client.getConnection()||level!=client.level"));
        assertTrue(fades.contains("!LocatorHeadsFeature.enabled()&&!FADES.isEmpty()"));
        assertTrue(hud.contains("LocatorNameFades.ease(ddstweak$uuid,visible,System.nanoTime())"));
        assertFalse(hud.contains("ddstweak$fades"));
    }

    @Test void locatorDisclosureUsesGlobalConfigGroups() throws Exception {
        String configs=file("config/Configs.java");
        String gui=file("gui/DDSTweakConfigScreen.java");
        assertTrue(gui.contains("new GroupedConfigList("));
        assertTrue(gui.contains("new GroupedConfigOption("));
        assertTrue(gui.contains("group(parents,Configs.Visual.LOCATOR_HEADS,"));
        assertTrue(gui.contains("isGroupParent(config)"));
        assertTrue(gui.contains("MaLiLibIcons.ARROW_UP:MaLiLibIcons.ARROW_DOWN"));
        assertTrue(gui.contains("screen.toggleGroup(config)"));
        assertTrue(gui.contains("EXPANDED.contains(config)?MaLiLibIcons.ARROW_UP:MaLiLibIcons.ARROW_DOWN"));
        assertTrue(gui.contains("x+labelWidth+10+LABEL_TO_CONTROL_GAP,y+1,20,20"));
        assertTrue(gui.contains("LABEL_TO_CONTROL_GAP=8"));
        assertTrue(gui.contains("width-disclosure-LABEL_TO_CONTROL_GAP,config"));
        assertFalse(gui.contains("new ButtonGeneric(x+1,y+1"));
        assertFalse(gui.contains("if(Configs.Visual.LOCATOR_BORDER.getBooleanValue())"));
        assertFalse(gui.contains("if(Configs.Visual.LOCATOR_COMPASS.getBooleanValue())"));
        assertTrue(gui.contains("getListWidget().refreshEntries()"));
        assertTrue(gui.contains("rows(!getFilterText().isEmpty())"));
        assertTrue(gui.contains("ConfigRowGrouping.visible(configsFor(tab),PARENTS,EXPANDED,searching)"));
        for(String field:new String[]{
                "LOCATOR_HEADS","LOCATOR_OUTER_LAYER","LOCATOR_HEAD_SIZE","LOCATOR_NAME_MODE",
                "LOCATOR_BORDER","LOCATOR_BORDER_STYLE","LOCATOR_BORDER_THICKNESS","LOCATOR_BORDER_COLOR",
                "LOCATOR_COMPASS","LOCATOR_COMPASS_COLOR","LOCATOR_COMPASS_SHADOW",
                "LOCATOR_ALWAYS_XP","LOCATOR_NEARBY_ONLY","LOCATOR_PLAYER_FILTER",
                "LOCATOR_INCLUDE","LOCATOR_EXCLUDE"}) {
            assertTrue(configs.contains(" "+field+"="),"Missing persistent config: "+field);
            assertTrue(gui.contains("c.add(Configs.Visual."+field+");"),"Missing Visual option: "+field);
        }
        assertFalse(configs.contains("LOCATOR_RENDER_HEADS="));
        assertFalse(configs.contains("LOCATOR_COORDINATES="));
        assertFalse(gui.contains("c.add(Configs.Visual.LOCATOR_RENDER_HEADS)"));
        assertFalse(gui.contains("c.add(Configs.Visual.LOCATOR_COORDINATES)"));
    }

    @Test void nativeColorEditorsAndLegacyMigrationArePresent() throws Exception {
        String configs=file("config/Configs.java");
        assertTrue(configs.contains("MalilibCompat.integer(\"locatorHeadSize\",100,50,150,true,VISUAL_KEY)"));
        assertTrue(configs.contains("MalilibCompat.color(\"locatorBorderColor\",\"#FFFFFFFF\",VISUAL_KEY)"));
        assertTrue(configs.contains("MalilibCompat.color(\"locatorCompassColor\",\"#FFFFFFFF\",VISUAL_KEY)"));
        assertTrue(configs.contains("migrateLocatorConfigs(r)"));
        assertTrue(configs.contains("LocatorConfigMigration.migrate(root)"));
        String migration=file("feature/locatorheads/LocatorConfigMigration.java");
        assertTrue(migration.contains("migrateColor(visual,\"locatorBorderColor\")"));
        assertTrue(migration.contains("migrateColor(visual,\"locatorCompassColor\")"));
        assertTrue(file("platform/malilib/MalilibCompat.java").contains("ConfigColor color("));
    }

    @Test void doesNotConflateFakeIdentityWithWaypoints() throws Exception {
        String mixin=file("mixin/feature/locatorheads/LocatorBarMixin.java");
        assertTrue(mixin.contains("getPlayerInfo(uuid)"));
        assertTrue(mixin.contains("id().left()"));
        assertTrue(mixin.contains("getPlayerByUUID(uuid)"));
        assertFalse(mixin.contains("PlayerIdentityResolver"));
        assertFalse(mixin.contains("FakePlayerIdentity"));
        assertTrue(mixin.contains("original.call(graphics,pipeline,sprite,x,y,width,height,depth)"));
        assertTrue(mixin.contains("1f/8,2f/8,1f/8,2f/8"));
        assertTrue(mixin.contains("5f/8,6f/8,1f/8,2f/8"));
        assertTrue(mixin.contains("int overhang="));
    }

    @Test void missingSkinAlphaSkipsSquareOutlineAndKeepsTeamFallback() throws Exception {
        String cache=file("feature/locatorheads/LocatorContourCache.java");
        String mixin=file("mixin/feature/locatorheads/LocatorBarMixin.java");
        assertSkipsMissingAlpha(cache);
        assertFalse(cache.contains("FALLBACK_ALPHA"));
        assertTrue(cache.contains("ALPHAS.replace(id.toString(),new LocatorContourMask.SkinAlpha(base,outer))"));
        assertTrue(mixin.contains("if(LocatorHeadsFeature.borderEnabled())"));
        assertTrue(mixin.contains("LocatorHeadsFeature.fixedBorderArgb()"));
        assertTrue(mixin.contains("Math.min(w,h)/8f"));
        assertTrue(mixin.contains("if(LocatorHeadsFeature.outerLayer())"));
        assertFalse(mixin.contains("LocatorHeadsFeature.borderStyle()==LocatorOptions.BorderStyle.STATIC||ddstweak$teamColor!=null"));
    }

    @Test void outerLayerPixelsAppearOutsideBaseFaceMask() {
        boolean[] face=new boolean[64];
        java.util.Arrays.fill(face,true);
        boolean[] hat=new boolean[64];
        hat[0]=true;
        byte[] without=ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,false,1);
        byte[] with=ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,true,1);
        assertFalse(java.util.Arrays.equals(without,with));
        int center=ddstweak.feature.locatorheads.LocatorContourMask.SIZE/2;
        assertEquals(0,with[center*ddstweak.feature.locatorheads.LocatorContourMask.SIZE+center]);
    }

    @Test void locatorSpriteInterceptionReplacesTailOverlay() throws Exception {
        String source=file("mixin/feature/locatorheads/LocatorBarMixin.java");
        assertTrue(source.contains("@WrapOperation(method=\"method_70870\""));
        assertTrue(source.contains("@WrapOperation(method=\"lambda$extractRenderState$1\""));
        assertTrue(source.contains("@Redirect(method=\"lambda$extractRenderState$1\""));
        assertTrue(source.contains("@Inject(method=\"lambda$extractRenderState$1\""));
        assertFalse(source.contains("@Redirect(method=\"*\""));
        assertFalse(source.contains("@Inject(method=\"*\""));
        assertTrue(source.contains("private void ddstweak$capture(Level world,GuiGraphics graphics,int index,TrackedWaypoint waypoint,CallbackInfo ci)"));
        assertTrue(source.contains("Entity viewer=minecraft.getCameraEntity();"));
        assertTrue(source.contains("original.call(graphics,pipeline,sprite,x,y,width,height,depth)"));
        assertTrue(source.contains("ddstweak$renderHead(graphics,x,y,width,height)"));
        assertTrue(source.contains("ddstweak$filtered"));
        assertTrue(source.contains("}finally{"));
        assertFalse(source.contains("ddstweak$overlayWaypoints"));
        assertFalse(source.contains("getWaypointManager().forEachWaypoint(camera,waypoint->"));
        assertTrue(source.contains("Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"));
    }

    @Test void borderAndCompassRetainConfiguredAlpha() throws Exception {
        String feature=file("feature/locatorheads/LocatorHeadsFeature.java");
        String face=file("mixin/feature/locatorheads/LocatorBarMixin.java");
        String compass=file("feature/locatorheads/LocatorCompass.java");
        assertTrue(feature.contains("fixedBorderArgb(){return Configs.Visual.LOCATOR_BORDER_COLOR.getIntegerValue();}"));
        assertTrue(feature.contains("compassArgb(){return Configs.Visual.LOCATOR_COMPASS_COLOR.getIntegerValue();}"));
        assertTrue(face.contains("LocatorHeadsFeature.fixedBorderArgb()"));
        assertTrue(compass.contains("(configured>>>24)"));
        assertTrue(compass.contains("if(alpha<=0)return;"));
    }

    @Test void faceLayersRenderIndependentlyAndRespectMasterSwitch() throws Exception {
        String source=file("mixin/feature/locatorheads/LocatorBarMixin.java").replace("\r\n","\n");
        assertTrue(source.contains("ddstweak$pendingArrowOutcome=LocatorSpriteDecision.decide(enabled,ddstweak$filtered,drewHead)"));
        assertTrue(source.contains("ddstweak$pendingArrowOutcome=LocatorSpriteDecision.Outcome.ORIGINAL;"));
        assertTrue(source.contains("ddstweak$filterPitchArrow("));
        assertTrue(source.contains("if(ddstweak$pendingArrowOutcome!=LocatorSpriteDecision.Outcome.HIDDEN)"));
        assertTrue(source.contains("blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"));
        assertTrue(source.contains("original.call(graphics,pipeline,arrow,x,y,width,height)"));
        assertTrue(source.contains("enabled&&!ddstweak$filtered&&ddstweak$renderHead(graphics,x,y,width,height)"));
        assertTrue(source.contains("if(!LocatorHeadsFeature.enabled()||ddstweak$waypoint==null||ddstweak$texture==null)return false;"));
        assertTrue(source.contains("graphics.blit(ddstweak$texture,left,top,left+w,top+h,1f/8,2f/8,1f/8,2f/8);"));
        assertTrue(source.contains("if(LocatorHeadsFeature.outerLayer()){\n            graphics.blit(ddstweak$texture,left-overhang"));
        assertTrue(source.contains("5f/8,6f/8,1f/8,2f/8"));
        assertFalse(source.contains("PlayerFaceExtractor.extractRenderState("));
        assertTrue(source.contains("original.call(graphics,pipeline,sprite,x,y,width,height,depth);"));
    }

    @Test void cachedMaskUsesAlphaRatherThanSkinRgb() throws Exception {
        String mixin=file("mixin/feature/locatorheads/LocatorBarMixin.java");
        String cache=file("feature/locatorheads/LocatorContourCache.java");
        assertTrue(mixin.contains("LocatorContourCache.get(ddstweak$texture"));
        assertFalse(mixin.contains("ddstweak$drawContour("));
        assertTrue(cache.contains("pixels.getPixel(x+8,y+8)>>>24"));
        assertTrue(cache.contains("pixels.getPixel(x+40,y+8)>>>24"));
        assertTrue(cache.contains("new DynamicTexture("));
        assertTrue(cache.contains("client.getTextureManager().release("));
        assertTrue(cache.contains("0xFFFFFFFF:0"));
        assertTrue(cache.contains("MAX_TEXTURES=128"));
        assertTrue(file("mixin/feature/locatorheads/LocatorSkinDownloadMixin.java").contains("registerTextureInManager"));
        assertTrue(Files.readString(Path.of("src/main/resources/dds-tweak.mixins.json")).contains("LocatorSkinDownloadMixin"));
    }

    @Test void cacheLifecycleDoesNotDependOnRenderingWhileEnabled() throws Exception {
        String cache=file("feature/locatorheads/LocatorContourCache.java");
        String client=file("DDSTweak.java");
        assertTrue(client.contains("LocatorContourCache.initialize();"));
        assertTrue(cache.contains("ClientTickEvents.END_CLIENT_TICK.register(client->{"));
        assertTrue(cache.contains("            refresh(client);"));
        assertTrue(cache.contains("clearTextures(client);"));
        assertTrue(cache.contains("ALPHAS.clear();"));
        assertTrue(cache.contains("connection=client.getConnection();"));
        assertTrue(cache.contains("resourceManager=client.getResourceManager();"));
        assertTrue(cache.contains("if(reloadGeneration.get()==appliedReloadGeneration&&"));
        assertTrue(cache.contains("(connection!=client.getConnection()||resourceManager!=client.getResourceManager()))reset(client);"));
        assertTrue(cache.contains("private static final AtomicInteger reloadGeneration=new AtomicInteger();"));
        assertTrue(cache.contains("ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener("));
        assertTrue(cache.contains("onResourceManagerReload(ResourceManager manager)"));
        assertTrue(cache.contains("locator_contour_reload"));
        assertTrue(cache.contains("client.execute(()->clearReloadedTextures(client,generation));"));
        assertTrue(cache.contains("int generation=reloadGeneration.incrementAndGet();"));
        assertTrue(cache.contains("if(generation!=reloadGeneration.get())return;"));
        assertTrue(cache.contains("appliedReloadGeneration=generation;"));
        int reload=cache.indexOf("public static void clear(){");
        int invalidation=cache.indexOf("ALPHAS.invalidateBundled();",reload);
        int dispatch=cache.indexOf("client.execute(()->clearReloadedTextures(client,generation));",reload);
        int helper=cache.indexOf("private static void clearReloadedTextures(",reload);
        assertTrue(reload>=0&&invalidation>reload&&dispatch>invalidation&&helper>dispatch);
        assertFalse(cache.substring(helper).contains("ALPHAS.clear();"),
                "Late GPU cleanup must not discard newly uploaded skin alpha");
    }

    @Test void optionalCacheDiagnosticsCountAllDynamicTextureReleasePaths() throws Exception {
        String cache=file("feature/locatorheads/LocatorContourCache.java");
        assertTrue(cache.contains("Boolean.getBoolean(\"ddstweak.locatorHeads.trace\")"));
        assertTrue(cache.contains("if(TRACE&&++traceTicks%200==0&&client.getConnection()!=null)"));
        assertTrue(cache.contains("LOGGER.info(\"LocatorHeads alpha:"));
        int tickHook=cache.indexOf("ClientTickEvents.END_CLIENT_TICK.register(client->{");
        int refresh=cache.indexOf("private static void refresh(Minecraft client)");
        int counter=cache.indexOf("++traceTicks");
        assertTrue(tickHook>=0&&tickHook<counter&&counter<refresh,
                "Periodic logging must run on client ticks, not on every HUD get()");
        assertEquals(counter,cache.lastIndexOf("++traceTicks"),
                "Multiple render-path increments make trace cadence player-count-dependent");
        assertTrue(cache.contains("LOGGER.info(\"LocatorHeads reset:"));
        assertTrue(cache.contains("if(TRACE&&(hadSession||previousMasks>0))"));
        assertTrue(cache.contains("LOGGER.info(\"LocatorHeads reload:"));
        assertTrue(cache.contains("sessionChanged?\"session\":\"resources\""));
        assertTrue(cache.contains("if(TRACE)CAPTURES.incrementAndGet();"));
        assertTrue(cache.contains("if(TRACE&&alpha!=null)recovered++;"));
        assertTrue(cache.contains("if(TRACE&&alpha!=null)bundled++;"));
        assertTrue(cache.contains("if(TRACE)uploads++;"));
        assertTrue(cache.contains("if(old!=null)release(client,old);"));
        assertTrue(cache.contains("if(removed!=null)release(client,removed);"));
        assertTrue(cache.contains("for(Entry entry:MASKS.values())release(client,entry);"));
        assertTrue(cache.contains("client.getTextureManager().release(entry.texture());"));
        assertTrue(cache.contains("if(TRACE)releases++;"));
    }

    @Test void missingAlphaRecoversRegisteredDynamicSkinWithoutGpuReadback() throws Exception {
        String cache=file("feature/locatorheads/LocatorContourCache.java");
        assertTrue(cache.contains("new LocatorAlphaSnapshots(512)"));
        assertTrue(cache.contains("ALPHAS.replace(id.toString(),new LocatorContourMask.SkinAlpha(base,outer))"));
        assertTrue(cache.contains("ALPHAS.invalidateBundled();"));
        assertTrue(cache.contains("if(alpha==null)alpha=readRegisteredSkin(client,skin);"));
        assertTrue(cache.contains("client.getTextureManager().getTexture(skin) instanceof DynamicTexture texture"));
        assertTrue(cache.contains("NativeImage pixels=texture.getPixels();"));
        assertTrue(cache.contains("accept(skin,pixels);"));
        assertSkipsMissingAlpha(cache);
        assertFalse(cache.contains("FALLBACK_ALPHA"));
        assertFalse(cache.contains("trimAlphas("));
        assertFalse(cache.contains("readPixels("));
    }

    @Test void hairSilhouetteExtendsBeyondOpaqueFaceSquare() {
        var face=new boolean[64];
        java.util.Arrays.fill(face,true);
        var hat=new boolean[64];
        for(int x=1;x<7;x++)hat[x]=true; // visible hair above the face
        var plain=ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,false,1f);
        var layered=ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,true,1f);
        int size=ddstweak.feature.locatorheads.LocatorContourMask.SIZE;
        assertEquals(0,plain[42*size+52]);
        assertNotEquals(0,layered[42*size+52]);
        assertEquals(0,layered[64*size+64]); // never paint across the visible face
    }

    @Test void transparentHatAffectsOnlyOuterContour() {
        var empty=new boolean[64];
        var face=new boolean[64];
        java.util.Arrays.fill(face,true);
        var hat=new boolean[64];
        hat[0]=true;
        byte[] plain=ddstweak.feature.locatorheads.LocatorContourMask.create(face,empty,true,1f);
        byte[] enabled=ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,true,1f);
        byte[] disabled=ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,false,1f);
        assertArrayEquals(plain,disabled);
        assertFalse(java.util.Arrays.equals(enabled,disabled));
        int start=ddstweak.feature.locatorheads.LocatorContourMask.FACE_START;
        int size=ddstweak.feature.locatorheads.LocatorContourMask.SIZE;
        for(int y=start;y<start+32;y++)for(int x=start;x<start+32;x++)
            assertEquals(0,enabled[y*size+x],"No border may cover the face");
    }

    @Test void contourWidthsAreMonotonic() {
        var face=new boolean[64];
        java.util.Arrays.fill(face,true);
        var empty=new boolean[64];
        int last=0;
        for(float thickness:new float[]{0.3f,0.5f,1f,2f}){
            byte[] mask=ddstweak.feature.locatorheads.LocatorContourMask.create(face,empty,false,thickness);
            int count=0;
            for(byte value:mask)if(value!=0)count++;
            assertTrue(count>last,"Thicker outlines must cover more outer pixels");
            last=count;
        }
    }

    @Test void outlineKeepsGuiThicknessAcrossHeadSizesAndDistances() {
        for(float thickness:new float[]{0.3f,0.5f,1f,2f}){
            for(float width:new float[]{2.5f,3.75f,4f,5f,6f,8f,12f}){
                int radius=ddstweak.feature.locatorheads.LocatorContourMask.radius(thickness,width);
                float resulting=radius*width/ddstweak.feature.locatorheads.LocatorContourMask.FACE_SIZE;
                assertEquals(thickness,resulting,width/64f,
                        "Outline thickness drift at face width "+width);
            }
        }
        assertThrows(IllegalArgumentException.class,()->
                ddstweak.feature.locatorheads.LocatorContourMask.radius(1f,0f));
    }

    @Test void smallHeadThickOutlineFitsMaskPaddingAndLeavesFaceTransparent() {
        var face=new boolean[64];
        var hat=new boolean[64];
        java.util.Arrays.fill(face,true);
        java.util.Arrays.fill(hat,true);
        int radius=ddstweak.feature.locatorheads.LocatorContourMask.radius(2f,2.5f);
        int size=ddstweak.feature.locatorheads.LocatorContourMask.SIZE;
        byte[] outline=ddstweak.feature.locatorheads.LocatorContourMask.create(face,hat,true,radius);
        assertEquals(size*size,outline.length);
        assertNotEquals(0,outline[(44-radius)*size+44],"Outer hair edge must not clip");
        assertEquals(0,outline[64*size+64],"Face remains transparent in contour mask");
        for(int i=0;i<size;i++){
            assertEquals(0,outline[i],"No top-edge clipping");
            assertEquals(0,outline[(size-1)*size+i],"No bottom-edge clipping");
            assertEquals(0,outline[i*size],"No left-edge clipping");
            assertEquals(0,outline[i*size+size-1],"No right-edge clipping");
        }
    }

    @Test void keepsHudAndVersionBoundariesExplicit() throws Exception {
        String mixin=file("mixin/feature/locatorheads/LocatorBarMixin.java");
        String xp=file("mixin/feature/locatorheads/LocatorExperienceMixin.java");
        String compass=file("feature/locatorheads/LocatorCompass.java");
        assertTrue(mixin.contains("//#if MC >= 260100"));
        assertTrue(mixin.contains("//#if MC >= 260200"));
        assertTrue(mixin.contains("//#if MC >= 12111"));
        assertTrue(mixin.contains("Identifier"));
        assertTrue(mixin.contains("public abstract class LocatorBarMixin"));
        assertTrue(xp.contains("LocatorHeadsFeature.alwaysXp()"));
        assertTrue(mixin.contains("LocatorHudTrace.background(false)"));
        assertTrue(mixin.contains("LocatorHudTrace.foreground()"));
        assertTrue(mixin.contains("LocatorHudTrace.capture()"));
        assertTrue(mixin.contains("LocatorHudTrace.outcome(ddstweak$pendingArrowOutcome)"));
        assertTrue(mixin.contains("LocatorHudTrace.arrow("));
        assertTrue(xp.contains("LocatorHudTrace.background(true)"));
        assertTrue(xp.contains("LocatorHudTrace.xpReturn(overlay)"));
        assertTrue(file("mixin/feature/locatorheads/LocatorPriorityMixin.java").contains("LocatorHudTrace.priority(force)"));
        assertTrue(file("feature/locatorheads/LocatorContourCache.java").contains("LocatorHudTrace.tick(client.getConnection()!=null)"));
        assertTrue(compass.contains("HudElementRegistry.attachElementAfter(VanillaHudElements.INFO_BAR,"));
        assertFalse(compass.contains("HudElementRegistry.addLast("));
        assertTrue(compass.contains("HudRenderCallback.EVENT.register"));
        assertTrue(compass.contains("isFlashbackReplay()"));
        assertTrue(file("mixin/DdsTweakMixinConfigPlugin.java").contains("isModLoaded(\"locator-heads\")"));
    }
}
