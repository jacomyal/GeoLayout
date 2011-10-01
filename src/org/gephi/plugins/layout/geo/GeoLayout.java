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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.spi.LayoutData;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutProperty;
import org.gephi.ui.propertyeditor.NodeColumnNumbersEditor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.gephi.dynamic.api.*;
import org.gephi.data.attributes.type.*;
import org.gephi.data.attributes.api.*;

/**
 *
 * @author Alexis Jacomy
 */
public class GeoLayout implements Layout {

    private GeoLayoutBuilder builder;
    private GraphModel graphModel;
    private boolean cancel;
    //Params
    private double focal = 150;
    private double scale = 1000;
    private boolean centered = true;
    private AttributeColumn latitude;
    private AttributeColumn longitude;
    private boolean radian = false;
    private String projection = "Mercator";
    public static String[] rows = {"Mercator","Transverse Mercator","Miller cylindrical","Gall–Peters","Sinusoidal","Lambert cylindrical","Equirectangular","Winkel tripel"};

    public GeoLayout(GeoLayoutBuilder builder) {
        this.builder = builder;
        resetPropertiesValues();
    }

    public void resetPropertiesValues() {
        AttributeModel attModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        for (AttributeColumn c : attModel.getNodeTable().getColumns()) {
            if (c.getId().equalsIgnoreCase("latitude")
                    || c.getId().equalsIgnoreCase("lat")
                    || c.getTitle().equalsIgnoreCase("latitude")
                    || c.getTitle().equalsIgnoreCase("lat")) {
                latitude = c;
            } else if (c.getId().equalsIgnoreCase("longitude")
                    || c.getId().equalsIgnoreCase("lon")
                    || c.getTitle().equalsIgnoreCase("longitude")
                    || c.getTitle().equalsIgnoreCase("lon")) {
                longitude = c;
            }
        }
    }

    public void initAlgo() {
        cancel = false;
    }

    public void goAlgo() {
        double lon = 0;
        double lat = 0;
        float nodeX = 0;
        float nodeY = 0;
        float averageX = 0;
        float averageY = 0;
        Graph gr = graphModel.getGraph();
        
        // try to handle dynamics
        DynamicController dc = Lookup.getDefault().lookup(DynamicController.class);
        DynamicModel dm = dc.getModel();
        boolean isDynamic = dm.isDynamicGraph();
        Graph graph = null;
        Estimator estimator = null;
        TimeInterval timeInt = null;
        Interval currentInt = null;
        if ( isDynamic ) {
            DynamicGraph dg = dm.createDynamicGraph(gr);
            timeInt = dm.getVisibleInterval();
            dg.setInterval(timeInt);
            // Presumably the graph at the given time interval
            graph = dg.getSnapshotGraph(timeInt.getLow(), timeInt.getHigh());
            estimator = dm.getEstimator();
            // Handy for converting DynamicDouble to appropriate primitive
            currentInt = new Interval(timeInt.getLow(), timeInt.getHigh());
        } else {
            graph = gr;
        }
            
        Node[] nodes = graph.getNodes().toArray();
        Vector<Node> validNodes = new Vector<Node>();
        Vector<Node> unvalidNodes = new Vector<Node>();

        // Set valid and non valid nodes:
        for(Node n: nodes){
            AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
            if(row.getValue(latitude)!=null && row.getValue(longitude)!=null){
                validNodes.add(n);
            }else{
                unvalidNodes.add(n);
            }
        }

        // Mercantor
        if(projection.equals("Mercator")){
            double lambda0 = 0;

            //determine lambda0:
            for(Node n: validNodes){
                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                if ( isDynamic ) {
                    lon = ((DynamicDouble)row.getValue(longitude)).getValue(currentInt, estimator);
                } else {
                    lon = ((Number) row.getValue(longitude)).doubleValue();
                }
                lambda0 += lon;
            }

            lambda0 = lambda0/validNodes.size();
            lambda0 = Math.toRadians(lambda0);

            //apply the formula:
            for(Node n: validNodes){
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof GeoLayoutData)) {
                    n.getNodeData().setLayoutData(new GeoLayoutData());
                }

                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                if ( isDynamic ) {
                    lat = ((DynamicDouble)row.getValue(latitude)).getValue(currentInt, estimator);
                    lon = ((DynamicDouble)row.getValue(longitude)).getValue(currentInt, estimator);
                } else {
                    lat = ((Number) row.getValue(latitude)).doubleValue();
                    lon = ((Number) row.getValue(longitude)).doubleValue();
                }
                
                lat = Math.toRadians(lat);
                lon = Math.toRadians(lon);

                nodeX = (float)((lon-lambda0)*scale);
                nodeY = (float)((Math.log(Math.tan(Math.PI/4+lat/2)))*scale);

                averageX += nodeX;
                averageY += nodeY;

                n.getNodeData().setX(nodeX);
                n.getNodeData().setY(nodeY);
            }

            averageX = averageX/validNodes.size();
            averageY = averageY/validNodes.size();
        }

        // Transverse Mercantor
        else if(projection.equals("Transverse Mercator")){
            //apply the formula:
            for(Node n: validNodes){
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof GeoLayoutData)) {
                    n.getNodeData().setLayoutData(new GeoLayoutData());
                }

                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                if ( isDynamic ) {
                    lat = ((DynamicDouble)row.getValue(latitude)).getValue(currentInt, estimator);
                    lon = ((DynamicDouble)row.getValue(longitude)).getValue(currentInt, estimator);
                } else {
                    lat = ((Number) row.getValue(latitude)).doubleValue();
                    lon = ((Number) row.getValue(longitude)).doubleValue();
                }

                lat = Math.toRadians(lat);
                lon = Math.toRadians(lon);

                nodeX = (float)(lon*scale);
                nodeY = (float)(scale/2*Math.log((1+Math.sin(lat))/(1-Math.sin(lat))));

                averageX += nodeX;
                averageY += nodeY;

                n.getNodeData().setX(nodeX);
                n.getNodeData().setY(nodeY);
            }

            averageX = averageX/validNodes.size();
            averageY = averageY/validNodes.size();
        }

        // Miller cylindrical
        else if(projection.equals("Miller cylindrical")){
            //apply the formula:
            for(Node n: validNodes){
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof GeoLayoutData)) {
                    n.getNodeData().setLayoutData(new GeoLayoutData());
                }

                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                if ( isDynamic ) {
                    lat = ((DynamicDouble)row.getValue(latitude)).getValue(currentInt, estimator);
                    lon = ((DynamicDouble)row.getValue(longitude)).getValue(currentInt, estimator);
                } else {
                    lat = ((Number) row.getValue(latitude)).doubleValue();
                    lon = ((Number) row.getValue(longitude)).doubleValue();
                }

                lat = Math.toRadians(lat);
                lon = Math.toRadians(lon);

                nodeX = (float)(lon*scale);
                nodeY = (float)(Math.log(Math.tan(Math.PI/4+2*lat/5))*scale*5/4);

                averageX += nodeX;
                averageY += nodeY;

                n.getNodeData().setX(nodeX);
                n.getNodeData().setY(nodeY);
            }

            averageX = averageX/validNodes.size();
            averageY = averageY/validNodes.size();
        }

        // Gall–Peters
        else if(projection.equals("Gall–Peters")){
            //apply the formula:
            for(Node n: validNodes){
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof GeoLayoutData)) {
                    n.getNodeData().setLayoutData(new GeoLayoutData());
                }

                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                if ( isDynamic ) {
                    lat = ((DynamicDouble)row.getValue(latitude)).getValue(currentInt, estimator);
                    lon = ((DynamicDouble)row.getValue(longitude)).getValue(currentInt, estimator);
                } else {
                    lat = ((Number) row.getValue(latitude)).doubleValue();
                    lon = ((Number) row.getValue(longitude)).doubleValue();
                }

                lat = Math.toRadians(lat);
                lon = Math.toRadians(lon);

                nodeX = (float)(lon*scale);
                nodeY = (float)(2*scale*Math.sin(lat));

                averageX += nodeX;
                averageY += nodeY;

                n.getNodeData().setX(nodeX);
                n.getNodeData().setY(nodeY);
            }

            averageX = averageX/validNodes.size();
            averageY = averageY/validNodes.size();
        }

        // Sinusoidal
        else if(projection.equals("Sinusoidal")){
            double lambda0 = 0;

            //determine lambda0:
            for(Node n: validNodes){
                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                lon = ((Number) row.getValue(longitude)).doubleValue();
                lambda0 += lon;
            }

            lambda0 = lambda0/validNodes.size();
            lambda0 = Math.toRadians(lambda0);

            //apply the formula:
            for(Node n: validNodes){
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof GeoLayoutData)) {
                    n.getNodeData().setLayoutData(new GeoLayoutData());
                }

                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                if ( isDynamic ) {
                    lat = ((DynamicDouble)row.getValue(latitude)).getValue(currentInt, estimator);
                    lon = ((DynamicDouble)row.getValue(longitude)).getValue(currentInt, estimator);
                } else {
                    lat = ((Number) row.getValue(latitude)).doubleValue();
                    lon = ((Number) row.getValue(longitude)).doubleValue();
                }

                lat = Math.toRadians(lat);
                lon = Math.toRadians(lon);

                nodeX = (float)((lon-lambda0)*Math.cos(lat)*scale);
                nodeY = (float)(scale*lat);

                averageX += nodeX;
                averageY += nodeY;

                n.getNodeData().setX(nodeX);
                n.getNodeData().setY(nodeY);
            }

            averageX = averageX/validNodes.size();
            averageY = averageY/validNodes.size();
        }

        // Lambert cylindrical equal-area
        else if(projection.equals("Lambert cylindrical")){
            double lambda0 = 0;
            double phi0 = 0;

            //determine lambda0:
            for(Node n: validNodes){
                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                lat = ((Number) row.getValue(latitude)).doubleValue();
                lon = ((Number) row.getValue(longitude)).doubleValue();
                lambda0 += lon;
                phi0 += lat;
            }

            lambda0 = lambda0/validNodes.size();
            phi0 = phi0/validNodes.size();

            lambda0 = Math.toRadians(lambda0);
            phi0 = Math.toRadians(phi0);

            //apply the formula:
            for(Node n: validNodes){
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof GeoLayoutData)) {
                    n.getNodeData().setLayoutData(new GeoLayoutData());
                }

                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                if ( isDynamic ) {
                    lat = ((DynamicDouble)row.getValue(latitude)).getValue(currentInt, estimator);
                    lon = ((DynamicDouble)row.getValue(longitude)).getValue(currentInt, estimator);
                } else {
                    lat = ((Number) row.getValue(latitude)).doubleValue();
                    lon = ((Number) row.getValue(longitude)).doubleValue();
                }

                lat = Math.toRadians(lat);
                lon = Math.toRadians(lon);

                nodeX = (float)((lon-lambda0)*Math.cos(phi0)*scale);
                nodeY = (float)(scale*Math.sin(lat)/Math.cos(phi0));

                averageX += nodeX;
                averageY += nodeY;

                n.getNodeData().setX(nodeX);
                n.getNodeData().setY(nodeY);
            }

            averageX = averageX/validNodes.size();
            averageY = averageY/validNodes.size();
        }

        // Equirectangular
        else if(projection.equals("Equirectangular")){

            //apply the formula:
            for(Node n: validNodes){
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof GeoLayoutData)) {
                    n.getNodeData().setLayoutData(new GeoLayoutData());
                }

                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                if ( isDynamic ) {
                    lat = ((DynamicDouble)row.getValue(latitude)).getValue(currentInt, estimator);
                    lon = ((DynamicDouble)row.getValue(longitude)).getValue(currentInt, estimator);
                } else {
                    lat = ((Number) row.getValue(latitude)).doubleValue();
                    lon = ((Number) row.getValue(longitude)).doubleValue();
                }

                lat = Math.toRadians(lat);
                lon = Math.toRadians(lon);

                nodeX = (float)(scale*lon);
                nodeY = (float)(scale*lat);

                averageX += nodeX;
                averageY += nodeY;

                n.getNodeData().setX(nodeX);
                n.getNodeData().setY(nodeY);
            }

            averageX = averageX/validNodes.size();
            averageY = averageY/validNodes.size();
        }

        // Winkel tripel
        else if(projection.equals("Winkel tripel")){
            double alpha = 0;

            //apply the formula:
            for(Node n: validNodes){
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof GeoLayoutData)) {
                    n.getNodeData().setLayoutData(new GeoLayoutData());
                }

                AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
                lat = ((Number) row.getValue(latitude)).doubleValue();
                lon = ((Number) row.getValue(longitude)).doubleValue();

                lat = Math.toRadians(lat);
                lon = Math.toRadians(lon);

                alpha = Math.acos(Math.cos(lon/2)*2/Math.PI);

                nodeX = (float)(scale*((lon*2/Math.PI)+(2*Math.cos(lat)*Math.sin(lon/2)*alpha/Math.sin(alpha))));
                nodeY = (float)(scale*(lat+Math.sin(lat)*alpha/Math.sin(alpha)));

                averageX += nodeX;
                averageY += nodeY;

                n.getNodeData().setX(nodeX);
                n.getNodeData().setY(nodeY);
            }

            averageX = averageX/validNodes.size();
            averageY = averageY/validNodes.size();
        }

        if(validNodes.size()>0 && unvalidNodes.size()>0){
            Node tempNode = validNodes.elementAt(0);
            double xMin = tempNode.getNodeData().x();
            double xMax = tempNode.getNodeData().x();
            double yMin = tempNode.getNodeData().y();
            double xTemp = 0;
            double yTemp = 0;

            for(Node n: validNodes){
                xTemp = n.getNodeData().x();
                yTemp = n.getNodeData().y();

                if(xTemp<xMin) xMin = xTemp;
                if(xTemp>xMax) xMax = xTemp;
                if(yTemp<yMin) yMin = yTemp;
            }

            if(unvalidNodes.size()>1){
                double i=0;
                double step=(xMax-xMin)/(unvalidNodes.size()-1);
                for(Node n: unvalidNodes){
                    n.getNodeData().setX((float) (xMin+i*step));
                    n.getNodeData().setY((float) (yMin-step));
                    i++;
                }
            }else{
                tempNode = unvalidNodes.elementAt(0);
                tempNode.getNodeData().setX(10000);
                tempNode.getNodeData().setY(10000);
            }
        }

        //recenter the graph
        if(centered==true){
            for(Node n: nodes){
                nodeX = n.getNodeData().x() - averageX;
                nodeY = n.getNodeData().y() - averageY;

                n.getNodeData().setX(nodeX);
                n.getNodeData().setY(nodeY);
            }
        }

        cancel = true;
    }

    public void endAlgo() {
    }

    @Override
    public boolean canAlgo() {
        return !cancel && latitude != null && longitude != null;
    }

    public LayoutProperty[] getProperties() {
        List<LayoutProperty> properties = new ArrayList<LayoutProperty>();
        final String GEOLAYOUT = "Geo Layout";

        try {
            properties.add(LayoutProperty.createProperty(
                    this, Double.class,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.scale.name"),
                    GEOLAYOUT,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.scale.desc"),
                    "getScale", "setScale"));
            properties.add(LayoutProperty.createProperty(
                    this, AttributeColumn.class,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.latitude.name"),
                    GEOLAYOUT,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.latitude.desc"),
                    "getLatitude", "setLatitude", NodeColumnNumbersEditor.class));
            properties.add(LayoutProperty.createProperty(
                    this, AttributeColumn.class,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.longitude.name"),
                    GEOLAYOUT,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.longitude.desc"),
                    "getLongitude", "setLongitude", NodeColumnNumbersEditor.class));
            properties.add(LayoutProperty.createProperty(
                    this, String.class,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.projection.name"),
                    GEOLAYOUT,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.projection.desc"),
                    "getProjection", "setProjection", CustomComboBoxEditor.class));
            properties.add(LayoutProperty.createProperty(
                    this, Boolean.class,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.centered.name"),
                    GEOLAYOUT,
                    NbBundle.getMessage(GeoLayout.class, "GeoLayout.centered.desc"),
                    "isCentered", "setCentered"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return properties.toArray(new LayoutProperty[0]);
    }

    public Boolean isCentered() {
        return centered;
    }

    public void setCentered(Boolean centered) {
        this.centered = centered;
    }

    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

    public void setGraphModel(GraphModel graphModel) {
        this.graphModel = graphModel;
    }

    public LayoutBuilder getBuilder() {
        return builder;
    }

    public Double getScale() {
        return scale;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }

    public AttributeColumn getLatitude() {
        return latitude;
    }

    public void setLatitude(AttributeColumn latitude) {
        this.latitude = latitude;
    }

    public AttributeColumn getLongitude() {
        return longitude;
    }

    public void setLongitude(AttributeColumn longitude) {
        this.longitude = longitude;
    }

    private static class GeoLayoutData implements LayoutData {

        //Data
        public double x = 0f;
        public double y = 0f;
    }
}
