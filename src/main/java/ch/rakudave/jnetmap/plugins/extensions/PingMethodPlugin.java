package ch.rakudave.jnetmap.plugins.extensions;

import ch.rakudave.jnetmap.net.status.PingMethod;
import org.pf4j.ExtensionPoint;

/**
 * Marker-Interface
 *
 * @author rakudave
 */
public interface PingMethodPlugin extends ExtensionPoint, PingMethod {
    // Marker
}
