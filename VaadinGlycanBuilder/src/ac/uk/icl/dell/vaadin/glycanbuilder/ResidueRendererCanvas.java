/*
*   EuroCarbDB, a framework for carbohydrate bioinformatics
*
*   Copyright (c) 2006-2011, Eurocarb project, or third-party contributors as
*   indicated by the @author tags or express copyright attribution
*   statements applied by the authors.  
*
*   This copyrighted material is made available to anyone wishing to use, modify,
*   copy, or redistribute it subject to the terms and conditions of the GNU
*   Lesser General Public License, as published by the Free Software Foundation.
*   A copy of this license accompanies this distribution in the file LICENSE.txt.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
*   for more details.
*   
*   @author David R. Damerell (david@nixbioinf.org)
*/
package ac.uk.icl.dell.vaadin.glycanbuilder;

import static org.eurocarbdb.application.glycanbuilder.Geometry.angle;
import static org.eurocarbdb.application.glycanbuilder.Geometry.center;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.Icon;

import org.eurocarbdb.application.glycanbuilder.AbstractResidueRenderer;
import org.eurocarbdb.application.glycanbuilder.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.Paintable;
import org.eurocarbdb.application.glycanbuilder.ResAngle;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyle;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.vaadin.damerell.canvas.BasicCanvas;
import org.vaadin.damerell.canvas.BasicCanvas.TextInfo;

import ac.uk.icl.dell.vaadin.canvas.shapes.Bracket;
import ac.uk.icl.dell.vaadin.canvas.shapes.Circle;
import ac.uk.icl.dell.vaadin.canvas.shapes.Diamond;
import ac.uk.icl.dell.vaadin.canvas.shapes.End;
import ac.uk.icl.dell.vaadin.canvas.shapes.HatDiamond;
import ac.uk.icl.dell.vaadin.canvas.shapes.Heptagon;
import ac.uk.icl.dell.vaadin.canvas.shapes.Hexagon;
import ac.uk.icl.dell.vaadin.canvas.shapes.Pentagon;
import ac.uk.icl.dell.vaadin.canvas.shapes.RHatDiamond;
import ac.uk.icl.dell.vaadin.canvas.shapes.Rhombus;
import ac.uk.icl.dell.vaadin.canvas.shapes.Square;
import ac.uk.icl.dell.vaadin.canvas.shapes.SquareBracket;
import ac.uk.icl.dell.vaadin.canvas.shapes.Star;
import ac.uk.icl.dell.vaadin.canvas.shapes.Triangle;

public class ResidueRendererCanvas extends AbstractResidueRenderer{
	public ResidueRendererCanvas() {
    	super();
    }

    public ResidueRendererCanvas(GlycanRendererCanvas src) {
    	super(src);
    }
	
	
    
	
	@Override
	public Icon getIcon(ResidueType type, int max_y_size) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Image getImage(ResidueType type, int max_y_size) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void paint(Paintable paintable, Residue node, boolean selected,
			boolean active, boolean on_border, Rectangle par_bbox,
			Rectangle cur_bbox, Rectangle sup_bbox, ResAngle orientation){
		
		Point pp = ( par_bbox!=null ) ?center(par_bbox) :center(cur_bbox);
		
    	@SuppressWarnings("unused")
		Point pc = center(cur_bbox);
    	
    	Point ps = ( sup_bbox!=null ) ?center(sup_bbox) :center(cur_bbox);
		
		BasicCanvas canvas=(BasicCanvas)paintable.getObject();
		
		ResidueStyle style=theResidueStyleDictionary.getStyle(node); 
		
		boolean painted=false;
		
		double x=cur_bbox.x;
	    double y=cur_bbox.y;
	    double nodeSize=cur_bbox.width;
	    
	    double w=cur_bbox.width;
	    double h=cur_bbox.height;
	    
	    canvas.saveContext();
	    canvas.setLineWidth(2.0);
		if(style.getShape()!=null){
			if(style.getShape().equals("square")){
				new Square(x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("circle")){
				new Circle(x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("triangle")){
				new Triangle(angle(pp,ps),x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("diamond")){
				new Diamond(x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("hexagon")){
				new Hexagon(x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("heptagon")){
				new Heptagon(x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("star")){
				new Star(x,y,nodeSize,nodeSize,style,canvas,5,selected).paint();
				painted=true;
			}else if(style.getShape().equals("sixstar")){
				new Star(x,y,nodeSize,nodeSize,style,canvas,6,selected).paint();
				painted=true;
			}else if(style.getShape().equals("sevenstar")){
				new Star(x,y,nodeSize,nodeSize,style,canvas,7,selected).paint();
				painted=true;
			}else if(style.getShape().equals("pentagon")){
				new Pentagon(x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("hatdiamond")){
				new HatDiamond(x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("rhatdiamond")){
				new RHatDiamond(x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("rhombus")){
				new Rhombus(x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("end")){
				new End(angle(pp,ps),x,y,nodeSize,nodeSize,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("bracket")){
				new Bracket(orientation.opposite().getAngle(),x,y,w,h,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("startrep")){
				new SquareBracket(orientation.opposite().getAngle(),x,y,w,h,style,canvas,selected).paint();
				painted=true;
			}else if(style.getShape().equals("endrep")){
				new SquareBracket(orientation.getAngle(),x,y,w,h,style,canvas,selected).paint();
				painted=true;
			}else{
				//System.err.println("Style: "+style.getShape());
			}
		}	
		
		if(!painted || style.getText()!=null){
			String text=getText(node,false);
			
			if(!painted){
				canvas.saveContext();
				//Color colour=Color.CYAN;
				//canvas.setFillStyle(colour.getRed(),colour.getGreen(), colour.getBlue());
				
				
				
				canvas.setFillStyle("#CCF");
				canvas.fillRect(x, y, cur_bbox.width,cur_bbox.height);
				canvas.restoreContext();
			}
			
			if(text!=null){
				float scale=.3f;
				
				if(!painted && theGraphicOptions.NOTATION==GraphicOptions.NOTATION_TEXT){
					scale=0.5f;
				}
				
				canvas.saveContext();
				canvas.setLineWidth(2.0);
				canvas.beginPath();
				
				TextInfo info=canvas.getRequiredScale(scale, 0.05, cur_bbox.width,cur_bbox.width, text);
				ResAngle angle=theGraphicOptions.getOrientationAngle();
				
				double rotate=0;
				if( !(angle.equals(0) || orientation.equals(180))){
					rotate=-Math.PI/2.0; 
					info=canvas.getRequiredScale(scale, 0.05, cur_bbox.height,cur_bbox.height, text);
					canvas.renderText(text, x+((cur_bbox.width)/2)-(12*info.scale)-2, y+((cur_bbox.height+info.height)/2.)-2, rotate, info.scale);
					//canvas.renderText(text, x+((cur_bbox.height-info.width+2)/2)+(12*info.scale), y+(cur_bbox.width/2.)-(12*info.scale), rotate, info.scale);
				}else{
					canvas.renderText(text, x+((cur_bbox.width-info.width+2)/2), y+(cur_bbox.height/2.)-(12*info.scale), rotate, info.scale);
				}
			
				Color colour=style.getTextColor();
				canvas.setStrokeStyle(colour.getRed(),colour.getGreen(), colour.getBlue());
				
				canvas.stroke();
				canvas.restoreContext();
			}
		}
		
		if(style.getShape()==null && selected){
			canvas.saveContext();
			canvas.setGlobalAlpha(0.4);
			new Square(x,y,cur_bbox.width,cur_bbox.height,style,canvas,selected).paint();
			canvas.restoreContext();
		}
		
		canvas.restoreContext();
	}
}
