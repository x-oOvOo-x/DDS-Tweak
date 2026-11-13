package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleNavigationTest {
    private static CarpetRuleCatalog.RemoteRule rule(String manager,String name){
        return new CarpetRuleCatalog.RemoteRule(new CarpetRuleCatalog.RuleKey(manager,name),"false",true);
    }

    @Test void fallbackGroupsCoverEveryRuleWithoutCallingManagerAnAddon(){
        CarpetRuleCatalog.RemoteRule carpet=rule("carpet","nativeRule");
        CarpetRuleCatalog.RemoteRule other=rule("cca","extensionRule");
        CarpetRuleMetadata metadata=new CarpetRuleMetadata();
        metadata.put(carpet.key(),new CarpetRuleMetadata.Description("Native","",
                Collections.singleton("gamerule"),"test"));
        CarpetRulePreferences prefs=new CarpetRulePreferences();
        prefs.toggleFavorite(other.key());
        prefs.createGroup("常用");
        prefs.setGroupMembership("常用",other.key(),true);
        List<CarpetRuleSidebar.Entry> entries=CarpetRuleSidebar.entries(
                Arrays.asList(carpet,other),metadata,prefs);
        assertFalse(entries.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.CARPET
                &&e.count()>0));
        assertTrue(entries.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.UNKNOWN
                &&e.count()==2));
        assertFalse(entries.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.OWNER));
        assertTrue(entries.stream().anyMatch(e->e.matches(CarpetRuleSidebar.Kind.FAVORITES,"")&&e.count()==1));
        assertTrue(entries.stream().anyMatch(e->e.matches(CarpetRuleSidebar.Kind.GAME_RULES,"")&&e.count()==-1));
        assertTrue(entries.stream().anyMatch(e->e.matches(CarpetRuleSidebar.Kind.SAVED_DEFAULTS,"")&&!e.available()));
        assertTrue(CarpetRuleSidebar.matches(CarpetRuleSidebar.Kind.GROUP,"常用",other,metadata,prefs));
        assertFalse(CarpetRuleSidebar.matches(CarpetRuleSidebar.Kind.CARPET,"",other,metadata,prefs));
    }

    @Test void trustedAddonIsSeparatedEvenWhenRulesShareCarpetManager(){
        CarpetRuleCatalog.RemoteRule core=rule("carpet","core");
        CarpetRuleCatalog.RemoteRule ams=rule("carpet","ams");
        CarpetRuleCatalog.RemoteRule tis=rule("carpet","tis");
        CarpetRuleCatalog.RemoteRule unverified=rule("carpet","unverified");
        CarpetRuleCatalog.RemoteRule extra=rule("cca","extra");
        CarpetRuleCatalog.RemoteRule unknown=rule("Carpet","caseSensitive");
        List<CarpetRuleCatalog.RemoteRule> rules=Arrays.asList(core,ams,tis,unverified,extra,unknown);
        java.util.Map<CarpetRuleCatalog.RuleKey,String> trusted=new java.util.HashMap<>();
        trusted.put(core.key(),"carpet");
        trusted.put(ams.key(),"carpet-ams-addition");
        trusted.put(tis.key(),"carpet-tis-addition");
        trusted.put(extra.key(),"cca");
        CarpetRuleMetadata meta=new CarpetRuleMetadata();
        CarpetRulePreferences prefs=new CarpetRulePreferences();
        List<CarpetRuleSidebar.Entry> sources=CarpetRuleSidebar.entries(rules,meta,prefs,trusted);
        List<CarpetRuleSidebar.Entry> buckets=new java.util.ArrayList<>();
        for(CarpetRuleSidebar.Entry entry:sources)
            if(entry.kind()==CarpetRuleSidebar.Kind.CARPET
                    ||entry.kind()==CarpetRuleSidebar.Kind.OWNER
                    ||entry.kind()==CarpetRuleSidebar.Kind.UNKNOWN)buckets.add(entry);
        assertEquals(6,buckets.stream().mapToInt(CarpetRuleSidebar.Entry::count).sum());
        assertTrue(buckets.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.CARPET
                &&e.count()==1&&!e.containsUnverified()));
        assertTrue(buckets.stream().anyMatch(e->e.matches(CarpetRuleSidebar.Kind.UNKNOWN,"")
                &&e.count()==2));
        assertTrue(buckets.stream().anyMatch(e->e.matches(CarpetRuleSidebar.Kind.OWNER,
                "carpet-ams-addition")&&e.count()==1));
        assertTrue(buckets.stream().anyMatch(e->e.matches(CarpetRuleSidebar.Kind.OWNER,
                "carpet-tis-addition")&&e.count()==1));
        for(CarpetRuleCatalog.RemoteRule r:rules){
            int membership=0;
            for(CarpetRuleSidebar.Entry e:buckets)
                if(CarpetRuleSidebar.matches(e.kind(),e.id(),r,meta,prefs,trusted.get(r.key())))
                    membership++;
            assertEquals(1,membership);
        }
        assertFalse(CarpetRuleSidebar.matches(CarpetRuleSidebar.Kind.CARPET,"",ams,meta,prefs,
                "carpet-ams-addition"));
    }

    @Test void localDefaultsOnlyMarkConfiguredRuleKeysWhenSnapshotIsKnown(){
        CarpetRuleCatalog.RemoteRule base=rule("carpet","base");
        CarpetRuleCatalog.RemoteRule addon=rule("carpet","addon");
        java.util.Set<CarpetRuleCatalog.RuleKey> defaults=
                Collections.singleton(base.key());
        List<CarpetRuleSidebar.Entry> local=CarpetRuleSidebar.entries(
                Arrays.asList(base,addon),new CarpetRuleMetadata(),new CarpetRulePreferences(),
                Collections.emptyMap(),defaults,true);
        assertTrue(local.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.SAVED_DEFAULTS
                &&e.available()&&e.count()==1));
        List<CarpetRuleSidebar.Entry> remote=CarpetRuleSidebar.entries(
                Arrays.asList(base,addon),new CarpetRuleMetadata(),new CarpetRulePreferences());
        assertTrue(remote.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.SAVED_DEFAULTS
                &&!e.available()));
        assertFalse(CarpetRuleSidebar.matches(CarpetRuleSidebar.Kind.GAME_RULES,"",
                base,new CarpetRuleMetadata(),new CarpetRulePreferences()));
    }

    @Test void remoteCarpetManagerOnlyNeverInventsRealAddon(){
        CarpetRuleCatalog.RemoteRule a=rule("carpet","a"),b=rule("carpet","b");
        List<CarpetRuleSidebar.Entry> entries=CarpetRuleSidebar.entries(
                Arrays.asList(a,b),new CarpetRuleMetadata(),new CarpetRulePreferences());
        assertFalse(entries.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.CARPET
                &&e.count()>0));
        assertFalse(entries.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.OWNER));
        assertTrue(entries.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.UNKNOWN
                &&e.count()==2));
    }

    @Test void sourceAndGroupSelectionsKeepTheirOwnAccordionOpen(){
        assertEquals(CarpetRuleSidebar.Section.EXTENSIONS,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.OWNER));
        assertEquals(CarpetRuleSidebar.Section.EXTENSIONS,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.CARPET));
        assertEquals(CarpetRuleSidebar.Section.EXTENSIONS,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.UNKNOWN));
        assertEquals(CarpetRuleSidebar.Section.GROUPS,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.GROUP));
        assertEquals(CarpetRuleSidebar.Section.CATEGORIES,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.CATEGORY));
        assertEquals("survival",CarpetRuleSidebar.Entry.category("survival",4).id());
        assertEquals(4,CarpetRuleSidebar.Entry.category("survival",4).count());
        assertEquals(CarpetRuleSidebar.Section.NONE,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.FAVORITES));
        assertEquals(CarpetRuleSidebar.Section.NONE,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.ALL));

        int top=78,bottom=530;
        assertEquals(bottom-118,CarpetRuleSidebar.quickHeaderY(bottom));
        // The previous eight-row cap wasted the lower sidebar area.
        assertEquals(12,CarpetRuleSidebar.sourceRows(54,900,12));
        assertEquals(26,CarpetRuleSidebar.sourceRows(54,900,26));
        assertEquals(11,CarpetRuleSidebar.sourceRows(78,530,20));
        int rows=CarpetRuleSidebar.sourceRows(top,bottom,7);
        int expanded=CarpetRuleSidebar.groupHeaderY(top,bottom,true,rows);
        int collapsed=CarpetRuleSidebar.groupHeaderY(top,bottom,false,rows);
        assertEquals(top+62,collapsed);
        assertEquals(top+62+rows*22+5,expanded);
        assertEquals(top+62,CarpetRuleSidebar.choiceTop(top,bottom,
                CarpetRuleSidebar.Section.EXTENSIONS,expanded));
        assertEquals(expanded-5,CarpetRuleSidebar.choiceBottom(top,bottom,
                CarpetRuleSidebar.Section.EXTENSIONS,expanded));
        assertEquals(expanded+26,CarpetRuleSidebar.choiceTop(top,bottom,
                CarpetRuleSidebar.Section.GROUPS,expanded));
        assertEquals(bottom-126,CarpetRuleSidebar.choiceBottom(top,bottom,
                CarpetRuleSidebar.Section.GROUPS,expanded));
        assertEquals(253,CarpetRuleSidebar.groupAddX(100,200));
    }

    @Test void cardMetadataDoesNotRepeatVerifiedModLabelOrInventUnverifiedSources(){
        assertEquals("TIS · feature",CarpetRuleSidebar.cardMetadata(
                "carpet-tis-addition",Arrays.asList("TIS","feature")));
        assertEquals("GCA",CarpetRuleSidebar.cardMetadata(
                "gugle-carpet-addition",Collections.singletonList("GCA")));
        assertEquals("feature",CarpetRuleSidebar.cardMetadata(
                "gugle-carpet-addition",Arrays.asList("gugle-carpet-addition","feature")));
        assertEquals("IGRY · command",CarpetRuleSidebar.cardMetadata(
                null,Arrays.asList("IGRY","command")));
        assertEquals("debug",CarpetRuleSidebar.cardMetadata(
                "magiclib-26_3",Collections.singletonList("debug")));
        assertEquals("",CarpetRuleSidebar.cardMetadata("carpet",Collections.emptyList()));
    }

    @Test void aVerifiedLibraryIsNotAutomaticallyACarpetAddon(){
        CarpetRuleCatalog.RemoteRule library=rule("carpet","magicRule");
        CarpetRuleCatalog.RemoteRule addon=rule("carpet","amsRule");
        CarpetRuleCatalog.RemoteRule unverified=rule("carpet","other");
        java.util.Map<CarpetRuleCatalog.RuleKey,String> owners=new java.util.HashMap<>();
        owners.put(library.key(),"magiclib-26_3");
        owners.put(addon.key(),"carpet-ams-addition");
        List<CarpetRuleSidebar.Entry> entries=CarpetRuleSidebar.entries(
                Arrays.asList(library,addon,unverified),new CarpetRuleMetadata(),
                new CarpetRulePreferences(),owners);
        assertTrue(entries.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.UNKNOWN
                &&e.count()==1));
        assertTrue(entries.stream().anyMatch(e->e.matches(CarpetRuleSidebar.Kind.OWNER,
                "magiclib-26_3")&&e.count()==1));
        assertFalse(CarpetRuleSidebar.matches(CarpetRuleSidebar.Kind.UNKNOWN,"",
                library,new CarpetRuleMetadata(),new CarpetRulePreferences(),"magiclib-26_3"));
        assertFalse(CarpetRuleSidebar.matches(CarpetRuleSidebar.Kind.CARPET,"",
                library,new CarpetRuleMetadata(),new CarpetRulePreferences(),"magiclib-26_3"));
        assertEquals("AMS",CarpetRuleSidebar.addonLabel("carpet-ams-addition"));
    }

    @Test void compactSourceLabelsAndHiddenCompatibilityRules(){
        assertEquals("DDS",CarpetRuleSidebar.sourceShortLabel(
                "carpet-dds-addition","Carpet DDS Addition"));
        assertEquals("IGNY",CarpetRuleSidebar.sourceShortLabel(
                "carpet-igny-addition","Carpet IGNY Addition"));
        assertEquals("Org",CarpetRuleSidebar.sourceShortLabel(
                "carpet-org-addition","Carpet Org Addition"));
        assertEquals("Other",CarpetRuleSidebar.sourceShortLabel(
                "other-id","Carpet Other Addition"));
        assertTrue(CarpetRuleSidebar.hiddenCompatibilityRule(
                rule("magiclib","booleanTest"),"magiclib-26_3"));
        assertFalse(CarpetRuleSidebar.hiddenCompatibilityRule(
                rule("carpet","other"),"magiclib-26_3"));
        assertFalse(CarpetRuleSidebar.hiddenCompatibilityRule(
                rule("magiclib","booleanTest"),"another-mod"));
    }

    @Test void fullSourceNamesAreDisplayOnlyAndUnknownNamesFallBackToIds(){
        assertEquals("DDS（Carpet DDS Addition）",
                CarpetRuleSidebar.sourceLabel("carpet-dds-addition","Carpet DDS Addition"));
        assertEquals("AMS（Carpet AMS Addition）",
                CarpetRuleSidebar.sourceLabel("carpet-ams-addition","Carpet AMS Addition"));
        assertEquals("MagicLib",CarpetRuleSidebar.sourceLabel("magiclib-26_3","MagicLib"));
        assertEquals("other-mod",CarpetRuleSidebar.sourceLabel("other-mod",null));
        assertEquals("DDS（carpet-dds-addition）",
                CarpetRuleSidebar.sourceLabel("carpet-dds-addition",null));
        assertEquals("Carpet",CarpetRuleSidebar.sourceLabel("carpet","Fabric Carpet"));
    }

    @Test void verifiedDdsAndLibrariesStayInDistinctSourceGroups(){
        CarpetRuleCatalog.RemoteRule dds=rule("carpet-dds-addition","ddsNetworkProtocol");
        CarpetRuleCatalog.RemoteRule lib=rule("carpet","libraryRule");
        CarpetRuleCatalog.RemoteRule unknown=rule("carpet","unverified");
        java.util.Map<CarpetRuleCatalog.RuleKey,String> trusted=new java.util.HashMap<>();
        trusted.put(dds.key(),"carpet-dds-addition");
        trusted.put(lib.key(),"magiclib-26_3");
        List<CarpetRuleSidebar.Entry> groups=CarpetRuleSidebar.entries(
                Arrays.asList(dds,lib,unknown),new CarpetRuleMetadata(),
                new CarpetRulePreferences(),trusted);
        assertTrue(groups.stream().anyMatch(e->e.matches(CarpetRuleSidebar.Kind.OWNER,
                "carpet-dds-addition")&&e.count()==1));
        assertTrue(groups.stream().anyMatch(e->e.matches(CarpetRuleSidebar.Kind.OWNER,
                "magiclib-26_3")&&e.count()==1));
        assertTrue(groups.stream().anyMatch(e->e.kind()==CarpetRuleSidebar.Kind.UNKNOWN&&e.count()==1));
        assertEquals(0,groups.stream().filter(e->e.kind()==CarpetRuleSidebar.Kind.CARPET)
                .mapToInt(CarpetRuleSidebar.Entry::count).sum());
    }

    @Test void ruleIdDisplayDropsOnlyNativeCarpetPrefix(){
        assertEquals("testRule",CarpetRuleSidebar.displayRuleId(rule("carpet","testRule").key()));
        assertEquals("cca:testRule",CarpetRuleSidebar.displayRuleId(rule("cca","testRule").key()));
        assertEquals("Carpet:testRule",CarpetRuleSidebar.displayRuleId(rule("Carpet","testRule").key()));
    }

    @Test void saveActionAndFavoriteUseNonOverlappingRightHandHitBoxes(){
        int right=420;
        assertEquals(right-25,CarpetRuleListScroll.favoriteX(right));
        assertEquals(right-49,CarpetRuleListScroll.saveX(right));
        assertEquals(right-93,CarpetRuleListScroll.switchX(right));
        assertEquals(right-128,CarpetRuleListScroll.choiceX(right));
        assertEquals(CarpetRuleListScroll.switchX(right)+CarpetRuleListScroll.SWITCH_WIDTH,
                CarpetRuleListScroll.choiceX(right)+CarpetRuleListScroll.CHOICE_WIDTH);
        assertEquals(68,CarpetRuleListScroll.CHOICE_WIDTH);
        assertTrue(CarpetRuleListScroll.choiceX(right)+CarpetRuleListScroll.CHOICE_WIDTH
                <CarpetRuleListScroll.saveX(right));
        assertTrue(CarpetRuleListScroll.switchX(right)+CarpetRuleListScroll.SWITCH_WIDTH
                <CarpetRuleListScroll.saveX(right));
        assertTrue(CarpetRuleListScroll.saveX(right)+20
                <=CarpetRuleListScroll.favoriteX(right));
        assertTrue(CarpetRuleListScroll.favoriteX(right)+CarpetRuleListScroll.STAR_WIDTH<right);
        assertEquals(33,CarpetRuleListScroll.SWITCH_WIDTH);
        assertEquals(14,CarpetRuleListScroll.SWITCH_HEIGHT);
        assertEquals(36,CarpetRuleListScroll.CARD_HEIGHT);
        assertEquals(42,CarpetRuleListScroll.STRIDE);
    }

    @Test void ruleListScrollbarMatchesWheelScrollingAndAllowsClickJump(){
        CarpetRuleListScroll scroll=new CarpetRuleListScroll();
        assertEquals(200,CarpetRuleListScroll.thumbHeight(3,200,200));
        assertTrue(CarpetRuleListScroll.thumbHeight(100,240,300)<300);
        assertEquals(40,scroll.thumbTop(40,100,240,300));
        scroll.scrollToFraction(1,100,240);
        assertEquals(CarpetRuleListScroll.max(100,240),scroll.position());
        assertEquals(40+300-CarpetRuleListScroll.thumbHeight(100,240,300),
                scroll.thumbTop(40,100,240,300));
        scroll.scrollToFraction(0,100,240);
        assertEquals(0,scroll.position());
    }

    @Test void cardGapIsNeverClickableAndScrollUsesSameCoordinatesAsRender(){
        CarpetRuleListScroll scroll=new CarpetRuleListScroll();
        int viewportTop=100;
        assertEquals(0,scroll.hit(121,viewportTop,40,270));
        assertEquals(-1,scroll.hit(viewportTop+CarpetRuleListScroll.CARD_HEIGHT+2,viewportTop,40,270));
        assertEquals(1,scroll.hit(viewportTop+CarpetRuleListScroll.STRIDE+2,viewportTop,40,270));
        scroll.wheel(-4,40,270);
        for(int i=0;i<90;i++)scroll.animate(40,270);
        for(int index=0;index<40;index++){
            int y=scroll.top(viewportTop,index);
            if(y>=viewportTop&&y+CarpetRuleListScroll.CARD_HEIGHT<=viewportTop+270)
                assertEquals(index,scroll.hit(y+5,viewportTop,40,270));
        }
        assertTrue(scroll.position()>0);
        scroll.reset();
        assertEquals(0,scroll.position());
    }
}
