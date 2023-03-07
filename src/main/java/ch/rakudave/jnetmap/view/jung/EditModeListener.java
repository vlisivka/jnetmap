package ch.rakudave.jnetmap.view.jung;

import edu.uci.ics.jung.visualization.control.ModalGraphMouse;

public interface EditModeListener {
    void editModeChanged(ModalGraphMouse.Mode mode);
}
