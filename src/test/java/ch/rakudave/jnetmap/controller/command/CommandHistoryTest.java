package ch.rakudave.jnetmap.controller.command;

import ch.rakudave.jnetmap.util.Settings;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandHistoryTest {
	private int origSize;
	private CommandHistory history;
	Command c1 = new Command() {
        @Override
        public Object redo() {
            return "do1";
        }
        @Override
        public Object undo() {
            return "undo1";
        }
    }, c2 = new Command() {
        @Override
        public Object redo() {
            return "do2";
        }
        @Override
        public Object undo() {
            return "undo2";
        }
    }, c3 = new Command() {
        @Override
        public Object redo() {
            return "do3";
        }
        @Override
        public Object undo() {
            return "undo3";
        }
    };

	@Before
	public void setup() {
		origSize = Settings.getInt("commandhistory.size", 20);
		Settings.put("commandhistory.size", 2);
		history = new CommandHistory();
	}

    @Test
    public void executeTest() {
        assertEquals("do1", history.execute(c1));
        assertTrue(history.getUndoSize() == 1);
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
        assertEquals("do2", history.execute(c2));
        assertTrue(history.getUndoSize() == 2);
    }

    @Test
    public void undoTest() {
        history.execute(c1);
        history.execute(c2);
        assertTrue(history.canUndo());
        assertEquals("undo2", history.undo());
        assertTrue(history.canUndo());
        assertEquals("undo1", history.undo());
        assertFalse(history.canUndo());
    }

    @Test
    public void redoTest() {
        assertEquals("do1", history.execute(c1));
        assertEquals("do2", history.execute(c2));
        assertFalse(history.canRedo());
        history.undo();
        history.undo();
        assertTrue(history.canRedo());
        assertEquals("do1", history.redo());
        assertEquals("do2", history.redo());
        assertFalse(history.canRedo());
    }

    @Test
    public void capacityTest() {
        assertEquals("do1", history.execute(c1));
        assertEquals("do2", history.execute(c2));
        assertEquals("do3", history.execute(c3));
        assertEquals(2, history.getUndoSize());
        assertEquals("undo3", history.undo());
        assertEquals("undo2", history.undo());
        assertFalse(history.canUndo());
    }

	@After
	public void teardown() {
		Settings.put("commandhistory.size", origSize);
	}
}
