/*
Copyright 2008 WebAtlas
Authors : Mathieu Bastian, Mathieu Jacomy, Julian Bilcke
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gephi.plugins.layout.geo;

import javax.swing.Icon;
import javax.swing.JPanel;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutUI;
import org.gephi.plugins.layout.geo.GeoLayout;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Alexis Jacomy
 */
@ServiceProvider(service = LayoutBuilder.class)
public class GeoLayoutBuilder implements LayoutBuilder {

    private GeoLayoutUI ui = new GeoLayoutUI();

    public String getName() {
        return "Geo Layout";
    }

    public LayoutUI getUI() {
        return ui;
    }

    public Layout buildLayout() {
        return new GeoLayout(this);
    }

    private static class GeoLayoutUI implements LayoutUI {

        public String getDescription() {
            return "Layout for latitude/longitude coordinates";
        }

        public Icon getIcon() {
            return null;
        }

        public JPanel getSimplePanel(Layout layout) {
            return null;
        }

        public int getQualityRank() {
            return -1;
        }

        public int getSpeedRank() {
            return -1;
        }
    }
}
