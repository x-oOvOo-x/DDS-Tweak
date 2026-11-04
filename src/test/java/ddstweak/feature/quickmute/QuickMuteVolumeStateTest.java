package ddstweak.feature.quickmute;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class QuickMuteVolumeStateTest {
    @Test void startupMigrationPersistsRecoveredVolumeBeforeVanillaCanSaveZero(){
        QuickMuteVolumeState legacy=new QuickMuteVolumeState();
        float[] durable={Float.NaN};
        legacy.prepareMute(0.37F,true,()->durable[0]=legacy.saved());
        assertEquals(0.37F,durable[0]);
        // Simulate a restart immediately after vanilla saves its temporary zero.
        QuickMuteVolumeState restarted=new QuickMuteVolumeState();
        restarted.load(durable[0]);
        restarted.prepareMute(0F,true,()->durable[0]=restarted.saved());
        assertEquals(0.37F,restarted.restore(0F));
    }

    @Test void aMutedRestartKeepsTheDurableOriginalVolume(){
        QuickMuteVolumeState first=new QuickMuteVolumeState();
        first.capture(0.37F,false);
        QuickMuteVolumeState restarted=new QuickMuteVolumeState();
        restarted.load(first.saved());
        restarted.capture(0F,true);
        assertEquals(0.37F,restarted.restore(0F));
    }

    @Test void deliberateZeroAndManualChangesAreRespected(){
        QuickMuteVolumeState state=new QuickMuteVolumeState();
        state.load(0.7F);
        state.capture(0F,false);
        assertEquals(0F,state.restore(0F));
        state.capture(0.4F,false);
        assertEquals(0.8F,state.restore(0.8F));
        assertEquals(0.8F,state.saved());
    }

    @Test void invalidMetadataCannotCreateAnInvalidVolume(){
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,1.1}){
            QuickMuteVolumeState state=new QuickMuteVolumeState();
            state.load(invalid);
            assertFalse(state.known());
            state.capture(0.42F,true);
            assertEquals(0.42F,state.restore(0F));
        }
    }

    @Test void alreadyLostLegacyVolumeIsNotInvented(){
        QuickMuteVolumeState state=new QuickMuteVolumeState();
        state.capture(0F,true);
        assertEquals(0F,state.restore(0F));
    }
}
