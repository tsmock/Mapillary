// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;

import org.openstreetmap.josm.plugins.mapillary.utils.TestUtil;

/**
 * Abstract class for tests that require JOSM's preferences running.
 *
 * @author nokutu
 *
 */
@Ignore
public abstract class AbstractTest {

  /**
   * Initiates the basic parts of JOSM.
   * @throws IOException
   */
  @BeforeClass
  public static void setUpBeforeClass() throws IOException {
    TestUtil.initPlugin();
  }
}
