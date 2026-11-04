package ddstweak.integration.dds.qca;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CreativeStorageSessionTest {
    @Test void onlyTheOriginalConnectionPlayerScreenAndMenuMayComplete(){
        Object connection=new Object(),player=new Object(),screen=new Object(),menu=new Object();
        CreativeStorageSession<Object,Object,Object,Object> session=
                new CreativeStorageSession<>(connection,player,screen,menu);
        assertTrue(session.matches(connection,player,screen,menu));
        assertFalse(session.matches(new Object(),player,screen,menu));
        assertFalse(session.matches(connection,new Object(),screen,menu));
        assertFalse(session.matches(connection,player,new Object(),menu));
        assertFalse(session.matches(connection,player,screen,new Object()));
        assertFalse(session.matches(null,player,screen,menu));
        assertSame(menu,session.menu());
    }

    @Test void closingAndReopeningTheSameMenuDoesNotReviveTheOldGesture(){
        Object connection=new Object(),player=new Object(),menu=new Object();
        Object firstScreen=new Object();
        CreativeStorageSession<Object,Object,Object,Object> session=
                new CreativeStorageSession<>(connection,player,firstScreen,menu);
        assertFalse(session.matches(connection,player,null,menu));
        assertFalse(session.matches(connection,player,new Object(),menu));
    }
}
