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

import java.beans.PropertyEditorSupport;
import org.gephi.plugins.layout.geo.GeoLayout;

/**
 *
 * @author Alexis Jacomy
 */
abstract class AbstractProjectionPropertyEditor extends PropertyEditorSupport {

    protected AbstractProjectionPropertyEditor() {
    }
    
    private String selectedRow;

    @Override
    public String[] getTags() {
        return GeoLayout.rows;
    }

    @Override
    public Object getValue() {
        return selectedRow;
    }

    @Override
    public void setValue(Object value) {
        for(int i=0;i<GeoLayout.rows.length;i++){
            if(GeoLayout.rows[i].equals((String)value)){
                selectedRow = GeoLayout.rows[i];
                break;
            }
        }
    }

    @Override
    public String getAsText() {
        return (String)getValue();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(text);
    }

    public boolean isNumberColumn(String column) {
        return false;
    }

    public boolean isStringColumn(String column) {
        return true;
    }
}
